# Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require "mock-logger"

require "chunky_png/color"
require "psd/color"
require "psd/util"
require "psd/image_modes/rgb"

if ENV.include? 'BENCH_9000_NATIVE'
  require "oily_png/oily_png"
  require "psd_native/psd_native"
end

WIDTH = 2000
HEIGHT = 2000

CHANNEL_DATA = [128] * WIDTH * HEIGHT * 4

class MockImage
  include PSD::ImageMode::RGB

  if ENV.include? 'BENCH_9000_NATIVE'
    include PSDNative::ImageMode::RGB
  end

  public :combine_rgb_channel

  def initialize
    @num_pixels = WIDTH * HEIGHT
    @channels_info = [{id: 0}, {id: 1}, {id: 2}, {id: -1}]
    @channel_length = @num_pixels
    @channel_data = CHANNEL_DATA
    @pixel_data = []
  end

  def pixel_step
    1
  end

  def pixel_data
    @pixel_data
  end
end

def harness_input
  MockImage.new
end

def harness_sample(input)
  input.combine_rgb_channel
  input
end

def harness_verify(output)
  output.pixel_data[3838] == 0x80808080
end

require 'bench/harness'
