/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.SymbolNodes;
import org.jruby.truffle.nodes.objects.SingletonClassNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.methods.InternalMethod;

import java.util.concurrent.Callable;

public final class UnresolvedDispatchNode extends DispatchNode {

    private int depth = 0;

    private final boolean ignoreVisibility;
    private final boolean indirect;
    private final MissingBehavior missingBehavior;

    @Child private SingletonClassNode singletonClassNode;

    public UnresolvedDispatchNode(
            RubyContext context,
            boolean ignoreVisibility,
            boolean indirect,
            MissingBehavior missingBehavior,
            DispatchAction dispatchAction) {
        super(context, dispatchAction);
        this.ignoreVisibility = ignoreVisibility;
        this.indirect = indirect;
        this.missingBehavior = missingBehavior;
    }

    @Override
    protected boolean guard(Object methodName, Object receiver) {
        return false;
    }

    @Override
    public Object executeDispatch(
            final VirtualFrame frame,
            final Object receiverObject,
            final Object methodName,
            Object blockObject,
            final Object argumentsObjects) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        final DispatchNode dispatch = atomic(new Callable<DispatchNode>() {
            @Override
            public DispatchNode call() throws Exception {
                final DispatchNode first = getHeadNode().getFirstDispatchNode();

                // First try to see if we did not a miss a specialization added by another thread.

                DispatchNode lookupDispatch = first;
                while (lookupDispatch != null) {
                    if (lookupDispatch.guard(methodName, receiverObject)) {
                        // This one worked, no need to rewrite anything.
                        return lookupDispatch;
                    }
                    lookupDispatch = lookupDispatch.getNext();
                }

                // We need a new node to handle this case.

                final DispatchNode newDispathNode;

                if (depth == DISPATCH_POLYMORPHIC_MAX) {
                    newDispathNode = new UncachedDispatchNode(getContext(), ignoreVisibility, getDispatchAction(), missingBehavior);
                } else {
                    depth++;
                    if (receiverObject instanceof RubyBasicObject) {
                        newDispathNode = doRubyBasicObject(frame, first, receiverObject, methodName, argumentsObjects);
                    }
                    else if (RubyGuards.isForeignObject(receiverObject)) {
                        newDispathNode = createForeign(argumentsObjects, first, methodName);
                    } else {
                        newDispathNode = doUnboxedObject(frame, first, receiverObject, methodName);
                    }
                }

                first.replace(newDispathNode);
                return newDispathNode;
            }
        });

        return dispatch.executeDispatch(frame, receiverObject, methodName, blockObject, argumentsObjects);
    }

    private DispatchNode createForeign(Object argumentsObjects, DispatchNode first, Object methodName) {
        Object[] args = (Object[]) argumentsObjects;
        return new CachedForeignDispatchNode(getContext(), first, methodName, args.length);
    }

    private DispatchNode doUnboxedObject(
            VirtualFrame frame,
            DispatchNode first,
            Object receiverObject,
            Object methodName) {
        final RubyClass callerClass;

        if (ignoreVisibility) {
            callerClass = null;
        } else {
            callerClass = getContext().getCoreLibrary().getMetaClass(RubyArguments.getSelf(frame.getArguments()));
        }

        final String methodNameString = toString(methodName);

        final InternalMethod method = lookup(callerClass, receiverObject, methodNameString, ignoreVisibility);

        if (method == null) {
            return createMethodMissingNode(first, methodName, receiverObject);
        }

        if (receiverObject instanceof Boolean) {
            final Assumption falseUnmodifiedAssumption =
                    getContext().getCoreLibrary().getFalseClass().getUnmodifiedAssumption();

            final InternalMethod falseMethod =
                    lookup(callerClass, false, methodNameString,
                            ignoreVisibility);

            final Assumption trueUnmodifiedAssumption =
                    getContext().getCoreLibrary().getTrueClass().getUnmodifiedAssumption();

            final InternalMethod trueMethod =
                    lookup(callerClass, true, methodNameString,
                            ignoreVisibility);

            if ((falseMethod == null) && (trueMethod == null)) {
                throw new UnsupportedOperationException();
            }

            return new CachedBooleanDispatchNode(getContext(),
                    methodName, first,
                    falseUnmodifiedAssumption, null, falseMethod,
                    trueUnmodifiedAssumption, null, trueMethod, indirect, getDispatchAction());
        } else {
            return new CachedUnboxedDispatchNode(getContext(),
                    methodName, first, receiverObject.getClass(),
                    getContext().getCoreLibrary().getLogicalClass(receiverObject).getUnmodifiedAssumption(), method, indirect, getDispatchAction());
        }
    }

    private DispatchNode doRubyBasicObject(
            VirtualFrame frame,
            DispatchNode first,
            Object receiverObject,
            Object methodName,
            Object argumentsObjects) {
        final RubyClass callerClass = ignoreVisibility ? null : getContext().getCoreLibrary().getMetaClass(RubyArguments.getSelf(frame.getArguments()));

        final InternalMethod method = lookup(callerClass, receiverObject, toString(methodName), ignoreVisibility);

        if (method == null) {
            return createMethodMissingNode(first, methodName, receiverObject);
        }

        if (RubyGuards.isRubySymbol(receiverObject)) {
            return new CachedBoxedSymbolDispatchNode(getContext(), methodName, first, method, indirect, getDispatchAction());
        } else {
            return new CachedBoxedDispatchNode(getContext(), methodName, first,
                    getContext().getCoreLibrary().getMetaClass(receiverObject), method, indirect, getDispatchAction());
        }
    }

    private String toString(Object methodName) {
        if (methodName instanceof String) {
            return (String) methodName;
        } else if (RubyGuards.isRubyString(methodName)) {
            return methodName.toString();
        } else if (RubyGuards.isRubySymbol(methodName)) {
            return SymbolNodes.getString((RubyBasicObject) methodName);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private DispatchNode createMethodMissingNode(
            DispatchNode first,
            Object methodName,
            Object receiverObject) {
        switch (missingBehavior) {
            case RETURN_MISSING: {
                return new CachedBoxedReturnMissingDispatchNode(getContext(), methodName, first,
                        getContext().getCoreLibrary().getMetaClass(receiverObject), indirect, getDispatchAction());
            }

            case CALL_METHOD_MISSING: {
                final InternalMethod method = lookup(null, receiverObject, "method_missing", true);

                if (method == null) {
                    throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                            receiverObject.toString() + " didn't have a #method_missing", this));
                }

                if (DISPATCH_METAPROGRAMMING_ALWAYS_UNCACHED) {
                    return new UncachedDispatchNode(getContext(), ignoreVisibility, getDispatchAction(), missingBehavior);
                }

                return new CachedBoxedMethodMissingDispatchNode(getContext(), methodName, first,
                        getContext().getCoreLibrary().getMetaClass(receiverObject), method, DISPATCH_METAPROGRAMMING_ALWAYS_INDIRECT, getDispatchAction());
            }

            default: {
                throw new UnsupportedOperationException(missingBehavior.toString());
            }
        }
    }

}
