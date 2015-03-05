/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import java.util.concurrent.locks.ReentrantLock;

import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyMutex;
import org.jruby.truffle.runtime.core.RubyThread;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingActionWithoutGlobalLock;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

@CoreClass(name = "Mutex")
public abstract class MutexNodes {

    @CoreMethod(names = "lock")
    public abstract static class LockNode extends UnaryCoreMethodNode {

        public LockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LockNode(LockNode prev) {
            super(prev);
        }

        @Specialization
        public RubyMutex lock(RubyMutex mutex) {
            final ReentrantLock lock = mutex.getReentrantLock();

            if (lock.isHeldByCurrentThread()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().threadError("deadlock; recursive locking", this));
            }

            final RubyThread thread = getContext().getThreadManager().getCurrentThread();

            getContext().getThreadManager().runUntilResult(new BlockingActionWithoutGlobalLock<Boolean>() {
                @Override
                public Boolean block() throws InterruptedException {
                    lock.lockInterruptibly();
                    thread.acquiredLock(lock);
                    return SUCCESS;
                }
            });

            return mutex;
        }

    }

    @CoreMethod(names = "locked?")
    public abstract static class IsLockedNode extends UnaryCoreMethodNode {

        public IsLockedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IsLockedNode(IsLockedNode prev) {
            super(prev);
        }

        @Specialization
        public boolean isLocked(RubyMutex mutex) {
            final ReentrantLock lock = mutex.getReentrantLock();

            return lock.isLocked();
        }

    }

    @CoreMethod(names = "owned?")
    public abstract static class IsOwnedNode extends UnaryCoreMethodNode {

        public IsOwnedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IsOwnedNode(IsOwnedNode prev) {
            super(prev);
        }

        @Specialization
        public boolean isOwned(RubyMutex mutex) {
            final ReentrantLock lock = mutex.getReentrantLock();

            return lock.isHeldByCurrentThread();
        }

    }

    @CoreMethod(names = "try_lock")
    public abstract static class TryLockNode extends UnaryCoreMethodNode {

        public TryLockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TryLockNode(TryLockNode prev) {
            super(prev);
        }

        @Specialization
        public boolean tryLock(RubyMutex mutex) {
            final ReentrantLock lock = mutex.getReentrantLock();

            if (lock.isHeldByCurrentThread()) {
                return false;
            }

            if (lock.tryLock()) {
                RubyThread thread = getContext().getThreadManager().getCurrentThread();
                thread.acquiredLock(lock);
                return true;
            } else {
                return false;
            }
        }

    }

    @CoreMethod(names = "unlock")
    public abstract static class UnlockNode extends UnaryCoreMethodNode {

        public UnlockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UnlockNode(UnlockNode prev) {
            super(prev);
        }

        @Specialization
        public RubyMutex unlock(RubyMutex mutex) {
            final ReentrantLock lock = mutex.getReentrantLock();
            final RubyThread thread = getContext().getThreadManager().getCurrentThread();

            try {
                lock.unlock();
            } catch (IllegalMonitorStateException e) {
                if (!lock.isLocked()) {
                    throw new RaiseException(getContext().getCoreLibrary().threadError("Attempt to unlock a mutex which is not locked", this));
                } else {
                    throw new RaiseException(getContext().getCoreLibrary().threadError("Attempt to unlock a mutex which is locked by another thread", this));
                }
            }

            thread.releasedLock(lock);

            return mutex;
        }

    }

}
