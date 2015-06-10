package org.jruby.truffle.nodes.core.behavior;


import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.nodes.core.behavior.init.InitFold;
import org.jruby.truffle.nodes.core.behavior.utility.BehaviorOption;
import org.jruby.truffle.nodes.core.behavior.utility.DependencyStaticScope;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.BehaviorObject;

@CoreClass(name = "BehaviorCore")
public class BehaviorModule {

    @CoreMethod(names = "fold", isModuleFunction = true, required = 1, needsBlock = true  )
    public abstract static class FoldExprNode extends BinaryCoreMethodNode {

        @Child
        DependencyStaticScope extractDeps;
        @Child
        InitFold initFold;

        public FoldExprNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            extractDeps = new DependencyStaticScope();
            initFold = new InitFold(context);
        }

        @Specialization
        public BehaviorObject fold(VirtualFrame frame, int value, RubyProc proc){
            BehaviorObject[] deps = extractDeps.execute(frame,proc);
            return initFold.execute(frame,deps,value,proc);
        }
        @Specialization
        public BehaviorObject fold(VirtualFrame frame, double value, RubyProc proc){
            BehaviorObject[] deps = extractDeps.execute(frame,proc);
            return initFold.execute(frame,deps,value,proc);        }
        @Specialization
        public BehaviorObject fold(VirtualFrame frame, Object value, RubyProc proc){
            BehaviorObject[] deps = extractDeps.execute(frame,proc);
            return initFold.execute(frame,deps,value,proc);
        }
    }

    @CoreMethod(names = {"behavior","signal","map"}, isModuleFunction = true, needsBlock = true)
    public abstract static class BehaviorExprNode extends UnaryCoreMethodNode {
        @Child
        private WriteHeadObjectFieldNode writeSignalExpr;
        @Child
        HandleBehaviorExprInitializationNode execSignalExpr;
        @Child
        DependencyStaticScope extractDeps;

        public BehaviorExprNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            writeSignalExpr = new WriteHeadObjectFieldNode(BehaviorOption.SIGNAL_EXPR);
            execSignalExpr = new HandleBehaviorExprInitializationNode(context, sourceSection);
            extractDeps = new DependencyStaticScope();
        }

        @Specialization
        BehaviorObject map(VirtualFrame frame, RubyProc block) {
            BehaviorObject self = newSignal();
            BehaviorObject[] dependsOn = extractDeps.execute(frame,block);
            self.setupPropagationDep(dependsOn);
            writeSignalExpr.execute(self, block);
            execSignalExpr.execute(frame, self, dependsOn);
            return self;
        }

        @CompilerDirectives.TruffleBoundary
        private BehaviorObject newSignal() {
            return (BehaviorObject) (new BehaviorObject.SignalRuntimeAllocator()).allocate(getContext(), getContext().getCoreLibrary().getBehaviorClass(), null);
        }
    }


    @CoreMethod(names = "source", isModuleFunction = true, required = 1)
    public abstract static class SourceNode extends UnaryCoreMethodNode {
        @Child
        CallDispatchHeadNode callInit;


        public SourceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            callInit = DispatchHeadNodeFactory.createMethodCall(context, true);
        }

        @Specialization
        BehaviorObject source(VirtualFrame frame, int value) {
            final BehaviorObject self = newSignal();
            return (BehaviorObject) callInit.call(frame, newSignal(), "initialize", null, value);
        }

        @Specialization
        BehaviorObject source(VirtualFrame frame, Object value) {
            final BehaviorObject self = newSignal();
            return (BehaviorObject) callInit.call(frame, newSignal(), "initialize", null, value);
        }

        @CompilerDirectives.TruffleBoundary
        private BehaviorObject newSignal() {
            return (BehaviorObject) (new BehaviorObject.SignalRuntimeAllocator()).allocate(getContext(), getContext().getCoreLibrary().getBehaviorSourceClass(), null);
        }

    }

}
