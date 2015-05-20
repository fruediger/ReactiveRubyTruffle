# Copyright (c) 2007-2014, Evan Phoenix and contributors
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of Rubinius nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

# Only part of Rubinius' kernel.rb

module Kernel

  def Array(obj)
    ary = Rubinius::Type.check_convert_type obj, Array, :to_ary

    return ary if ary

    if array = Rubinius::Type.check_convert_type(obj, Array, :to_a)
      array
    else
      [obj]
    end
  end
  module_function :Array

  def Complex(*args)
    Rubinius.privately do
      Complex.convert(*args)
    end
  end
  module_function :Complex
  
  def Rational(a, b = 1)
    Rubinius.privately do
      Rational.convert a, b
    end
  end
  module_function :Rational

  def Float(obj)
    case obj
    when String
      Rubinius::Type.coerce_string_to_float obj, true
    else
      Rubinius::Type.coerce_object_to_float obj
    end
  end
  module_function :Float

  ##
  # MRI uses a macro named NUM2DBL which has essentially the same semantics as
  # Float(), with the difference that it raises a TypeError and not a
  # ArgumentError. It is only used in a few places (in MRI and Rubinius).
  #--
  # If we can, we should probably get rid of this.

  def FloatValue(obj)
    exception = TypeError.new 'no implicit conversion to float'

    case obj
    when String
      raise exception
    else
      begin
        Rubinius::Type.coerce_object_to_float obj
      rescue
        raise exception
      end
    end
  end
  private :FloatValue

  def Hash(obj)
    return {} if obj.nil? || obj == []

    if hash = Rubinius::Type.check_convert_type(obj, Hash, :to_hash)
      return hash
    end

    raise TypeError, "can't convert #{obj.class} into Hash"
  end
  module_function :Hash

  def Integer(obj, base=nil)
    if obj.kind_of? String
      if obj.empty?
        raise ArgumentError, "invalid value for Integer: (empty string)"
      else
        base ||= 0
        return obj.to_inum(base, true)
      end
    end

    if base
      raise ArgumentError, "base is only valid for String values"
    end

    case obj
      when Integer
        obj
      when Float
        if obj.nan? or obj.infinite?
          raise FloatDomainError, "unable to coerce #{obj} to Integer"
        else
          obj.to_int
        end
      when NilClass
        raise TypeError, "can't convert nil into Integer"
      else
        # Can't use coerce_to or try_convert because I think there is an
        # MRI bug here where it will return the value without checking
        # the return type.
        if obj.respond_to? :to_int
          if val = obj.to_int
            return val
          end
        end

        Rubinius::Type.coerce_to obj, Integer, :to_i
    end
  end
  module_function :Integer

  ##
  # MRI uses a macro named StringValue which has essentially the same
  # semantics as obj.coerce_to(String, :to_str), but rather than using that
  # long construction everywhere, we define a private method similar to
  # String().
  #
  # Another possibility would be to change String() as follows:
  #
  #   String(obj, sym=:to_s)
  #
  # and use String(obj, :to_str) instead of StringValue(obj)

  def StringValue(obj)
    Rubinius::Type.coerce_to obj, String, :to_str
  end
  module_function :StringValue

  def autoload(name, file)
    Object.autoload(name, file)
  end
  private :autoload

  def autoload?(name)
    Object.autoload?(name)
  end
  private :autoload?

  def define_singleton_method(*args, &block)
    singleton_class.send(:define_method, *args, &block)
  end

  def extend(*modules)
    raise ArgumentError, "wrong number of arguments (0 for 1+)" if modules.empty?

    # Disabled for JRuby+Truffle. The frozen check should be done in `object_extend`.
    # Having the check here breaks nil.extend().
    #Rubinius.check_frozen

    modules.reverse_each do |mod|
      Rubinius.privately do
        mod.extend_object self
      end

      Rubinius.privately do
        mod.extended self
      end
    end
    self
  end

  def itself
    self
  end

  def object_id
    Rubinius.primitive :object_id
    raise PrimitiveFailure, "Kernel#object_id primitive failed"
  end

  def print(*args)
    args.each do |obj|
      $stdout.write obj.to_s
    end
    nil
  end
  module_function :print

  def trap(sig, prc=nil, &block)
    Signal.trap(sig, prc, &block)
  end
  module_function :trap

  def warn(*messages)
    $stderr.puts(*messages) if !$VERBOSE.nil? && !messages.empty?
    nil
  end
  module_function :warn

  def srand(seed=undefined)
    if undefined.equal? seed
      seed = Thread.current.randomizer.generate_seed
    end
    seed = Rubinius::Type.coerce_to seed, Integer, :to_int
    Thread.current.randomizer.swap_seed seed
  end
  module_function :srand

  def tap
    yield self
    self
  end

  def test(cmd, file1, file2=nil)
    case cmd
      when ?d
        File.directory? file1
      when ?e
        File.exist? file1
      when ?f
        File.file? file1
      when ?l
        File.symlink? file1
      when ?r
        File.readable? file1
      when ?R
        File.readable_real? file1
      when ?w
        File.writable? file1
      when ?W
        File.writable_real? file1
      when ?A
        File.atime file1
      when ?C
        File.ctime file1
      when ?M
        File.mtime file1
      else
        raise NotImplementedError, "command ?#{cmd.chr} not implemented"
    end
  end
  module_function :test

  def open(obj, *rest, &block)
    if obj.respond_to?(:to_open)
      obj = obj.to_open(*rest)

      if block_given?
        return yield(obj)
      else
        return obj
      end
    end

    path = Rubinius::Type.coerce_to_path obj

    if path.kind_of? String and path.prefix? '|'
      return IO.popen(path[1..-1], *rest, &block)
    end

    File.open(path, *rest, &block)
  end
  module_function :open

  def p(*a)
    return nil if a.empty?
    a.each { |obj| $stdout.puts obj.inspect }
    $stdout.flush

    a.size == 1 ? a.first : a
  end
  module_function :p

  def putc(int)
    $stdout.putc(int)
  end
  module_function :putc

  def puts(*a)
    $stdout.puts(*a)
    nil
  end
  module_function :puts

  def loop
    return to_enum(:loop) unless block_given?

    begin
      while true
        yield
      end
    rescue StopIteration
    end
  end
  module_function :loop

end
