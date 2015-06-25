# Copyright (c) 2007-2015, Evan Phoenix and contributors
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

##
#--
# NOTE do not define to_sym or id2name. It's been deprecated for 5 years and
# we've decided to remove it.
#++

class Fixnum < Integer

  def self.induced_from(obj)
    case obj
    when Fixnum
      return obj
    when Float, Bignum
      value = obj.to_i
      if value.is_a? Bignum
        raise RangeError, "Object is out of range for a Fixnum"
      else
        return value
      end
    else
      value = Rubinius::Type.coerce_to(obj, Integer, :to_int)
      return self.induced_from(value)
    end
  end

  #--
  # see README-DEVELOPERS regarding safe math compiler plugin
  #++

  Truffle.omit(":divide is a Rubinius internal detail. We define :/ directly in Java") do
    alias_method :/, :divide
  end

  alias_method :modulo, :%

  def fdiv(n)
    if n.kind_of?(Fixnum)
      to_f / n
    else
      redo_coerced :fdiv, n
    end
  end

  def imaginary
    0
  end

  Truffle.omit(":divide is a Rubinius internal detail. We define :/ directly in Java") do
    # Must be it's own method, so that super calls the correct method
    # on Numeric
    def div(o)
      if o.is_a?(Float) && o == 0.0
        raise ZeroDivisionError, "division by zero"
      end
      divide(o).floor
    end
  end

  def **(o)
    Rubinius.primitive :fixnum_pow

    if o.is_a?(Float) && self < 0 && o != o.round
      return Complex.new(self, 0) ** o
    elsif o.is_a?(Integer) && o < 0
      return Rational.new(self, 1) ** o
    end

    redo_coerced :**, o
  end
end
