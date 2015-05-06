/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import jnr.constants.platform.Errno;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyException;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyString;

/**
 * Rubinius primitives associated with the Ruby {@code Exception} class.
 */
public abstract class ExceptionPrimitiveNodes {

    @RubiniusPrimitive(name = "exception_errno_error", needsSelf = false)
    public static abstract class ExceptionErrnoErrorPrimitiveNode extends RubiniusPrimitiveNode {

        protected final int ENOENT = Errno.ENOENT.intValue();
        protected final int EBADF = Errno.EBADF.intValue();
        protected final int EEXIST = Errno.EEXIST.intValue();

        public ExceptionErrnoErrorPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "errno == ENOENT")
        public RubyException enoent(RubyString message, int errno) {
            return getContext().getCoreLibrary().fileNotFoundError(message.toString(), this);
        }

        @Specialization(guards = "errno == ENOENT")
        public RubyException enoent(RubyNilClass message, int errno) {
            return getContext().getCoreLibrary().fileNotFoundError("nil", this);
        }

        @Specialization(guards = "errno == EBADF")
        public RubyException ebadf(RubyNilClass message, int errno) {
            return getContext().getCoreLibrary().badFileDescriptor(this);
        }

        @Specialization(guards = "errno == EEXIST")
        public RubyException eexist(RubyString message, int errno) {
            return getContext().getCoreLibrary().fileExistsError(message.toString(), this);
        }

        @Specialization(guards = "errno == EEXIST")
        public RubyException eexist(RubyNilClass message, int errno) {
            return getContext().getCoreLibrary().fileExistsError("nil", this);
        }


        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "!isExceptionSupported(errno)")
        public RubyException unsupported(RubyString message, int errno) {
            final Errno errnoObject = Errno.valueOf(errno);

            if (errnoObject == null) {
                throw new UnsupportedOperationException("errno: " + errno + " " + message);
            } else {
                throw new UnsupportedOperationException("errno: " + errnoObject.name());
            }
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "!isExceptionSupported(errno)")
        public RubyException unsupported(RubyNilClass message, int errno) {
            final Errno errnoObject = Errno.valueOf(errno);

            if (errnoObject == null) {
                throw new UnsupportedOperationException("errno: " + errno + " nil");
            } else {
                throw new UnsupportedOperationException("errno: " + errnoObject.name());
            }
        }

        public static boolean isExceptionSupported(int errno) {
            return Errno.ENOENT.intValue() == errno || Errno.EBADF.intValue() == errno || Errno.EEXIST.intValue() == errno;
        }

    }

}
