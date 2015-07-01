package org.jruby.truffle.nodes.core.behavior.functionality;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.BehaviorObject;

import static org.jruby.truffle.runtime.core.BehaviorObject.*;


//TODO i should move the behavior functionality inside different methods.
public class HandleBehaviorFunctionality extends Node {

    @Child
    AbstractFunctionality functionality;

    public HandleBehaviorFunctionality(RubyContext context, SourceSection sourceSection) {
        functionality = new UninitializedFunctionality(context);
    }

    public static HandleBehaviorFunctionality createHandleBehaviorExprNode(RubyContext context, SourceSection section) {
        return new HandleBehaviorFunctionality(context, section);
    }


    public boolean execute(VirtualFrame frame, BehaviorObject self, BehaviorObject lastNode, long sourceID) {
        return functionality.execute(frame, self, lastNode, sourceID);
    }

}

abstract class AbstractFunctionality extends Node {

    protected RubyContext context;


    AbstractFunctionality(RubyContext context) {
        this.context = context;
    }

    /**
     * This method handles the behavior "expression". For special behaviors like e.g. fold the behavior expression is
     * replaced by a predefined expression
     * <p/>
     * This method returns true if the value of the behavior changed
     *
     * @param frame
     * @param self
     * @param lastNode
     * @return
     */
    abstract public boolean execute(VirtualFrame frame, BehaviorObject self, BehaviorObject lastNode, long sourceID);

    protected HandleBehaviorFunctionality getHeadNode() {
        return NodeUtil.findParent(this, HandleBehaviorFunctionality.class);
    }
}

//TODO this node is not nice i really need to move the behavior functionality into methods!
class AllFunctionality extends AbstractFunctionality {
    @Child
    TakeNode take;
    @Child
    NormalBehavior normal;
    @Child
    FoldNode fold;
    @Child
    FilterNode filter;
    @Child
    MapNode map;
    @Child
    MergeNode merge;
    @Child
    SkipNode skip;
    @Child
    MapNNode mapN;
    @Child
    SampleOnNode sampleOn;

    AllFunctionality(RubyContext context) {
        super(context);
        normal = new NormalBehavior(context, null);
        fold = new FoldNode(context);
        filter = new FilterNode(context);
        map = new MapNode(context);
        merge = new MergeNode(context);
        take = new TakeNode(context);
        skip = new SkipNode(context);
        mapN = new MapNNode(context);
        sampleOn = new SampleOnNode(context);
    }

    @Override
    public boolean execute(VirtualFrame frame, BehaviorObject self, BehaviorObject lastNode, long sourceID) {
        if (self.isNormal()) {
            return normal.execute(frame, self, lastNode, sourceID);
        } else if (self.isFold()) {
            return fold.execute(frame, self, lastNode, sourceID);
        } else if (self.isFilter()) {
            return filter.execute(frame, self, lastNode, sourceID);
        } else if (self.getType() == TYPE_MAP) {
            return map.execute(frame, self, lastNode, sourceID);
        } else if (self.getType() == TYPE_MERGE) {
            return merge.execute(frame, self, lastNode, sourceID);
        } else if (self.getType() == TYPE_TAKE) {
            return take.execute(frame, self, lastNode, sourceID);
        } else if (self.getType() == TYPE_SKIP) {
            return skip.execute(frame, self, lastNode, sourceID);
        } else if (self.getType() == TYPE_MAPN) {
            return mapN.execute(frame, self, lastNode, sourceID);
        } else if (self.getType() == TYPE_SAMPLEON) {
            return sampleOn.execute(frame, self, lastNode, sourceID);
        }

        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new RuntimeException("the type of the BehaviorObject is unknown: " + self.getType());
    }
}

class CachedFunctionality extends AbstractFunctionality {
    private final int type;
    @Child
    AbstractFunctionality next;
    @Child
    Functionality functionality;


    CachedFunctionality(RubyContext context, Functionality functionality, int type, AbstractFunctionality next) {
        super(context);
        this.next = next;
        this.functionality = functionality;
        this.type = type;
    }

    @Override
    public boolean execute(VirtualFrame frame, BehaviorObject self, BehaviorObject lastNode, long sourceID) {
        if (self.getType() == type) {
            return functionality.execute(frame, self, lastNode, sourceID);
        } else {
            return next.execute(frame, self, lastNode, sourceID);
        }
    }
}

class UninitializedFunctionality extends AbstractFunctionality {
    static final int MAX_CHAIN_SIZE = 3;
    private int depth = 0;

    UninitializedFunctionality(RubyContext context) {
        super(context);
    }

    @Override
    public boolean execute(VirtualFrame frame, BehaviorObject self, BehaviorObject lastNode, long sourceID) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        AbstractFunctionality propNode = getHeadNode().functionality;
        AbstractFunctionality newFunctionality;
        if (depth >= MAX_CHAIN_SIZE) {
            newFunctionality = new AllFunctionality(context);
        } else {
            if (self.isNormal()) {
                newFunctionality = new CachedFunctionality(context, new NormalBehavior(context, null), BehaviorObject.TYPE_NORMAL, propNode);
            } else if (self.isFold()) {
                newFunctionality = new CachedFunctionality(context, new FoldNode(context), BehaviorObject.TYPE_FOLD, propNode);
            } else if (self.isFilter()) {
                newFunctionality = new CachedFunctionality(context, new FilterNode(context), BehaviorObject.TYPE_FILTER, propNode);
            } else if (self.isMerge()) {
                newFunctionality = new CachedFunctionality(context, new MergeNode(context), BehaviorObject.TYPE_MERGE, propNode);
            } else if (self.getType() == TYPE_MAP) {
                newFunctionality = new CachedFunctionality(context, new MapNode(context), TYPE_MAP, propNode);
            } else if (self.getType() == TYPE_TAKE) {
                newFunctionality = new CachedFunctionality(context, new TakeNode(context), TYPE_TAKE, propNode);
            } else if (self.getType() == TYPE_SKIP) {
                newFunctionality = new CachedFunctionality(context, new SkipNode(context), TYPE_SKIP, propNode);
            } else if (self.getType() == TYPE_MAPN) {
                newFunctionality = new CachedFunctionality(context, new MapNNode(context), TYPE_MAPN, propNode);
            } else if (self.getType() == TYPE_SAMPLEON) {
                newFunctionality = new CachedFunctionality(context, new SampleOnNode(context), TYPE_SAMPLEON, propNode);
            } else {
                throw new RuntimeException("unkown behavior type");
            }

            depth += 1;
        }
        propNode.replace(newFunctionality);
        return newFunctionality.execute(frame, self, lastNode, sourceID);
    }


}