/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.IsTaintedNode;
import org.jruby.truffle.nodes.objects.IsTaintedNodeGen;
import org.jruby.truffle.nodes.objects.TaintNode;
import org.jruby.truffle.nodes.objects.TaintNodeGen;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public class TaintResultNode extends RubyNode {

    private final boolean taintFromSelf;
    private final int taintFromParameter;
    private final ConditionProfile taintProfile = ConditionProfile.createBinaryProfile();

    @Child private RubyNode method;
    @Child private IsTaintedNode isTaintedNode;
    @Child private TaintNode taintNode;

    public TaintResultNode(boolean taintFromSelf, int taintFromParameter, RubyNode method) {
        super(method.getContext(), method.getEncapsulatingSourceSection());
        this.taintFromSelf = taintFromSelf;
        this.taintFromParameter = taintFromParameter;
        this.method = method;
        this.isTaintedNode = IsTaintedNodeGen.create(getContext(), getSourceSection(), null);
    }

    public TaintResultNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        this.taintFromSelf = false;
        this.taintFromParameter = -1;
        this.isTaintedNode = IsTaintedNodeGen.create(getContext(), getSourceSection(), null);
    }

    public Object maybeTaint(RubyBasicObject source, RubyBasicObject result) {
        if (taintProfile.profile(isTaintedNode.isTainted(source))) {
            if (taintNode == null) {
                CompilerDirectives.transferToInterpreter();
                taintNode = insert(TaintNodeGen.create(getContext(), getSourceSection(), null));
            }

            taintNode.taint(result);
        }

        return result;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final RubyBasicObject result;

        try {
            result = method.executeRubyBasicObject(frame);
        } catch (DoNotTaint e) {
            return e.getResult();
        } catch (UnexpectedResultException e) {
            throw new UnsupportedOperationException(e);
        }

        if (result != nil()) {
            if (taintFromSelf) {
                maybeTaint((RubyBasicObject) RubyArguments.getSelf(frame.getArguments()), result);
            }

            // It's possible the taintFromParameter value was misconfigured by the user, but the far more likely
            // scenario is that the argument at that position is an UndefinedPlaceholder, which doesn't take up
            // a space in the frame.
            if (taintFromParameter < RubyArguments.getUserArgumentsCount(frame.getArguments())) {
                final Object argument = RubyArguments.getUserArgument(frame.getArguments(), taintFromParameter);

                if (argument instanceof RubyBasicObject) {
                    final RubyBasicObject taintSource = (RubyBasicObject) argument;
                    maybeTaint(taintSource, result);
                }
            }
        }

        return result;
    }

    public static class DoNotTaint extends ControlFlowException {
        private static final long serialVersionUID = 5321304910918469059L;

        private final Object result;

        public DoNotTaint(Object result) {
            this.result = result;
        }

        public Object getResult() {
            return result;
        }
    }
}
