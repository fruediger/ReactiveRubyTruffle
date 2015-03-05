/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.signal;

import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyThread;
import org.jruby.truffle.runtime.subsystems.SafepointAction;
import org.jruby.truffle.runtime.util.Consumer;

import sun.misc.Signal;
import sun.misc.SignalHandler;

@SuppressWarnings("restriction")
public class ProcSignalHandler implements SignalHandler {

    private final RubyContext context;
    private final RubyProc proc;

    public ProcSignalHandler(RubyContext context, RubyProc proc) {
        this.context = context;
        this.proc = proc;
    }

    @Override
    public void handle(Signal signal) {
        // TODO: just make this a normal Ruby thread once we don't have the global lock anymore
        context.getSafepointManager().pauseAllThreadsAndExecuteFromNonRubyThread(null, new SafepointAction() {

            @Override
            public void run(RubyThread thread, Node currentNode) {
                if (thread == context.getThreadManager().getRootThread()) {
                    context.getThreadManager().enterGlobalLock(thread);
                    try {
                        // assumes this proc does not re-enter the SafepointManager.
                        proc.rootCall();
                    } finally {
                        context.getThreadManager().leaveGlobalLock();
                    }
                }
            }

        });
    }

}
