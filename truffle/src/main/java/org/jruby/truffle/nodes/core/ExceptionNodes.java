/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyException;

@CoreClass(name = "Exception")
public abstract class ExceptionNodes {

    @CoreMethod(names = "initialize", optional = 1)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject initialize(RubyException exception, NotProvided message) {
            exception.initialize(nil());
            return exception;
        }

        @Specialization(guards = "wasProvided(message)")
        public RubyBasicObject initialize(RubyException exception, Object message) {
            exception.initialize(message);
            return exception;
        }

    }

    @CoreMethod(names = "backtrace")
    public abstract static class BacktraceNode extends CoreMethodArrayArgumentsNode {

        @Child ReadHeadObjectFieldNode readCustomBacktrace;

        public BacktraceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readCustomBacktrace = new ReadHeadObjectFieldNode("@custom_backtrace");
        }

        @Specialization
        public Object backtrace(RubyException exception) {
            if (readCustomBacktrace.isSet(exception)) {
                return readCustomBacktrace.execute(exception);
            } else if (exception.getBacktrace() != null) {
                return exception.asRubyStringArray();
            } else {
                return nil();
            }
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "capture_backtrace!", optional = 1)
    public abstract static class CaptureBacktraceNode extends CoreMethodArrayArgumentsNode {

        public CaptureBacktraceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject captureBacktrace(RubyException exception, NotProvided offset) {
            return captureBacktrace(exception, 1);
        }

        @Specialization
        public RubyBasicObject captureBacktrace(RubyException exception, int offset) {
            Backtrace backtrace = RubyCallStack.getBacktrace(this, offset);
            exception.setBacktrace(backtrace);
            return nil();
        }

    }

    @CoreMethod(names = "message")
    public abstract static class MessageNode extends CoreMethodArrayArgumentsNode {

        public MessageNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object message(RubyException exception) {
            return exception.getMessage();
        }

    }

}
