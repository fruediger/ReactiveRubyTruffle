package org.jruby.truffle.nodes.core.behavior.functionality;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.interop.messages.Write;
import org.jruby.truffle.nodes.core.behavior.utility.BehaviorOption;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.signalRuntime.BehaviorObject;

/**
 * Created by me on 14.05.15.
 */
public class TakeNode extends  Functionality {

    @Child
    ReadHeadObjectFieldNode readLastNodeValue ;
    @Child
    WriteHeadObjectFieldNode writeValue;
    @Child
    WriteHeadObjectFieldNode writeTakeValue;
    @Child
    ReadHeadObjectFieldNode readTakeValue ;

    TakeNode(RubyContext context) {
        super(context);
        readLastNodeValue = new ReadHeadObjectFieldNode(BehaviorOption.VALUE_VAR);
        readTakeValue = new ReadHeadObjectFieldNode(BehaviorOption.TAKE_NUM);
        writeValue = new WriteHeadObjectFieldNode(BehaviorOption.VALUE_VAR);
        writeTakeValue = new WriteHeadObjectFieldNode(BehaviorOption.TAKE_NUM);
    }

    @Override
    public boolean execute(VirtualFrame frame, BehaviorObject self, BehaviorObject lastNode,long sourceID) {

        try {
            int takeValue = 0;
            takeValue = readTakeValue.executeInteger(self);
            if(takeValue > 0){
                writeTakeValue.execute(self,takeValue-1);
                writeValue.execute(self,readLastNodeValue.execute(lastNode));
                return true;
            }else{
                return false;
            }
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new RuntimeException("take value must be an int");
        }

    }
}
