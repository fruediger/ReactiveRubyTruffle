require 'bigdecimal'

module BigMath
  module_function
  
  # call-seq:
  #   log(decimal, numeric) -> BigDecimal
  #
  # Computes the natural logarithm of +decimal+ to the specified number of
  # digits of precision, +numeric+.
  #
  # If +decimal+ is zero of negative raise Math::DomainError.
  #
  # If +decimal+ is positive infinity, returns Infinity.
  #
  # If +decimal+ is NaN, returns NaN.
  #
  #   BigMath::log(BigMath::E(10), 10).to_s
  #   #=> "1.000000000000"
  #
  def log(x, precision)
    raise ArgumentError if x.nil?
    raise Math::DomainError if x.is_a?(Complex)
    raise Math::DomainError if x <= 0
    raise ArgumentError unless precision.is_a?(Integer)
    raise ArgumentError if precision < 1
    return BigDecimal::INFINITY if x == BigDecimal::INFINITY
    return BigDecimal::NAN if x.is_a?(BigDecimal) && x.nan?
    return BigDecimal::NAN if x.is_a?(Float) && x.nan?

    # this uses the series expansion of the Arctangh (Arc tangens hyperbolicus)
    # http://en.wikipedia.org/wiki/Area_hyperbolic_tangent
    # where ln(x) = 2 * artanh ((x - 1) / (x + 1))
    # d are the elements in the series (getting smaller and smaller)

    x = x.to_d
    rmpd_double_figures = 16 # from MRI ruby
    n = precision + rmpd_double_figures

    # offset the calculation to the efficient (0.1)...(10) window
    expo = x.exponent
    use_window = (x > 10) || (expo < 0) # allow up to 10 itself
    if use_window
      offset = BigDecimal.new("1E#{-expo}")
      x = x.mult(offset, n)
    end

    z = (x - 1).div((x + 1), n)
    z2 = z.mult(z, n)
    series_sum = z
    series_element = z

    i = 1
    while series_element != 0 do
      sum_exponent = series_sum.exponent
      element_exponent = series_element.exponent
      remaining_precision = n - (sum_exponent - element_exponent).abs
      break if remaining_precision < 0
      z = z.mult(z2, n)
      i += 2
      series_element = z.div(i, n)
      series_sum += series_element
    end

    window_result = series_sum * 2

    # reset the result back to the original value if needed
    if use_window
      log10 = log(10, n)
      window_result + log10.mult(expo, n)
    else
      window_result
    end
  end
end