# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../../ruby/spec_helper'

describe "Truffle::Primitive.assert_constant" do
  
  it "raises a RuntimeError when called dynamically" do
    lambda{ Truffle::Primitive.send(:assert_constant, 14 + 2) }.should raise_error(RuntimeError)
  end

  unless Truffle.graal?
    it "returns nil" do
      Truffle::Primitive.assert_constant(14 + 2).should be_nil
    end
  end

end
