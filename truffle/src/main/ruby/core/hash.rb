# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

class Hash

  def invert
    inverted = {}
    each_pair { |key, value|
      inverted[value] = key
    }
    inverted
  end

  def to_hash
    self
  end

  # Implementation of a fundamental Rubinius method that allows their Hash
  # implementation to work. We probably want to remove uses of this in the long
  # term, as it creates objects which already exist for them and we have to
  # create, but for now it allows us to use more Rubinius code unmodified.

  class KeyValue

    attr_reader :key
    attr_reader :value

    def initialize(key, value)
      @key = key
      @value = value
    end

  end

  def each_item
    each do |key, value|
      yield KeyValue.new(key, value)
    end
    nil
  end

  def find_item(key)
    value = _get_or_undefined(key)
    if undefined.equal?(value)
      nil
    else
      # TODO CS 7-Mar-15 maybe we should return the stored key?
      KeyValue.new(key, value)
    end
  end

  def merge_fallback(other, &block)
    merge(Rubinius::Type.coerce_to other, Hash, :to_hash, &block)
  end

  # Rubinius' Hash#reject taints but we don't want this

  def reject(&block)
    return to_enum(:reject) unless block_given?
    copy = dup
    copy.untaint
    copy.delete_if(&block)
    copy
  end

end
