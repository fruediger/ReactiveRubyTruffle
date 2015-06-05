/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.BreakException;
import org.jruby.truffle.translator.TranslatorEnvironment.BreakID;

/**
 * Catch a {@code break} from a call with a block containing a break
 * or inside a while/until loop.
 */
public class CatchBreakNode extends RubyNode {

    @Child private RubyNode body;

    private final BreakID breakID;

    private final BranchProfile breakProfile = BranchProfile.create();
    private final ConditionProfile matchingBreakProfile = ConditionProfile.createCountingProfile();

    public CatchBreakNode(RubyContext context, SourceSection sourceSection, RubyNode body, BreakID breakID) {
        super(context, sourceSection);
        this.body = body;
        this.breakID = breakID;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return body.execute(frame);
        } catch (BreakException e) {
            breakProfile.enter();

            if (matchingBreakProfile.profile(e.getBreakID() == breakID)) {
                return e.getResult();
            } else {
                throw e;
            }
        }
    }

}
