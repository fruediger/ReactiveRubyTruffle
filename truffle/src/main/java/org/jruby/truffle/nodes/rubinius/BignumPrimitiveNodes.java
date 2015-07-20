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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.core.BignumNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

/**
 * Rubinius primitives associated with the Ruby {@code Bignum} class.
 */
public abstract class BignumPrimitiveNodes {

    @RubiniusPrimitive(name = "bignum_pow")
    public static abstract class BignumPowPrimitiveNode extends RubiniusPrimitiveNode {

        private final ConditionProfile negativeProfile = ConditionProfile.createBinaryProfile();

        public BignumPowPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject pow(RubyBasicObject a, int b) {
            return pow(a, (long) b);
        }

        @Specialization
        public RubyBasicObject pow(RubyBasicObject a, long b) {
            if (negativeProfile.profile(b < 0)) {
                return null; // Primitive failure
            } else {
                // TODO CS 15-Feb-15 what about this cast?
                return BignumNodes.createRubyBignum(getContext().getCoreLibrary().getBignumClass(), BignumNodes.getBigIntegerValue(a).pow((int) b));
            }
        }

        @TruffleBoundary
        @Specialization
        public double pow(RubyBasicObject a, double b) {
            return Math.pow(BignumNodes.getBigIntegerValue(a).doubleValue(), b);
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Void pow(RubyBasicObject a, RubyBasicObject b) {
            throw new UnsupportedOperationException();
        }

    }

}
