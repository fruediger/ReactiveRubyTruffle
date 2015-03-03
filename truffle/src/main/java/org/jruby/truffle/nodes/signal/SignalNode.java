package org.jruby.truffle.nodes.signal;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Created by me on 25.02.15.
 */
public class SignalNode extends RubyNode{


    public SignalNode(RubyContext context, SourceSection sourceSection, Object body) {
        super(context, sourceSection);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return null;
    }
}
