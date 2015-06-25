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


import org.jruby.truffle.nodes.rubinius.PointerNodes;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public class PointerGuards {

    public static boolean isNullPointer(RubyBasicObject pointer) {
        return PointerNodes.getPointer(pointer) == PointerNodes.NULL_POINTER;
    }

}

