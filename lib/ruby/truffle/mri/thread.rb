# Truffle: Last version of lib/thread.rb in MRI @ r42801 (324df61e).
# Truffle: we shim Thread.handle_interrupt as we do not support it yet.
def Thread.handle_interrupt(h)
  yield
end

#
#               thread.rb - thread support classes
#                       by Yukihiro Matsumoto <matz@netlab.co.jp>
#
# Copyright (C) 2001  Yukihiro Matsumoto
# Copyright (C) 2000  Network Applied Communication Laboratory, Inc.
# Copyright (C) 2000  Information-technology Promotion Agency, Japan
#

unless defined? Thread
  raise "Thread not available for this ruby interpreter"
end

unless defined? ThreadError
  class ThreadError < StandardError
  end
end

if $DEBUG
  Thread.abort_on_exception = true
end

# Truffle: ConditionVariable is defined in Java.

#
# This class provides a way to synchronize communication between threads.
#
# Example:
#
#   require 'thread'
#
#   queue = Queue.new
#
#   producer = Thread.new do
#     5.times do |i|
#       sleep rand(i) # simulate expense
#       queue << i
#       puts "#{i} produced"
#     end
#   end
#
#   consumer = Thread.new do
#     5.times do |i|
#       value = queue.pop
#       sleep rand(i/2) # simulate expense
#       puts "consumed #{value}"
#     end
#   end
#
#   consumer.join
#
class Queue
  #
  # Creates a new queue.
  #
  def initialize
    @que = []
    @que.taint          # enable tainted communication
    @num_waiting = 0
    self.taint
    @mutex = Mutex.new
    @cond = ConditionVariable.new
  end

  #
  # Pushes +obj+ to the queue.
  #
  def push(obj)
    Thread.handle_interrupt(StandardError => :on_blocking) do
      @mutex.synchronize do
        @que.push obj
        @cond.signal
      end
      self
    end
  end

  #
  # Alias of push
  #
  alias << push

  #
  # Alias of push
  #
  alias enq push

  #
  # Retrieves data from the queue.  If the queue is empty, the calling thread is
  # suspended until data is pushed onto the queue.  If +non_block+ is true, the
  # thread isn't suspended, and an exception is raised.
  #
  def pop(non_block=false)
    Thread.handle_interrupt(StandardError => :on_blocking) do
      @mutex.synchronize do
        while true
          if @que.empty?
            if non_block
              raise ThreadError, "queue empty"
            else
              begin
                @num_waiting += 1
                @cond.wait @mutex
              ensure
                @num_waiting -= 1
              end
            end
          else
            return @que.shift
          end
        end
      end
    end
  end

  #
  # Alias of pop
  #
  alias shift pop

  #
  # Alias of pop
  #
  alias deq pop

  #
  # Returns +true+ if the queue is empty.
  #
  def empty?
    @que.empty?
  end

  #
  # Removes all objects from the queue.
  #
  def clear
    @que.clear
    self
  end

  #
  # Returns the length of the queue.
  #
  def length
    @que.length
  end

  #
  # Alias of length.
  #
  alias size length

  #
  # Returns the number of threads waiting on the queue.
  #
  def num_waiting
    @num_waiting
  end
end

#
# This class represents queues of specified size capacity.  The push operation
# may be blocked if the capacity is full.
#
# See Queue for an example of how a SizedQueue works.
#
class SizedQueue < Queue
  #
  # Creates a fixed-length queue with a maximum size of +max+.
  #
  def initialize(max)
    max = Rubinius::Type.num2long(max)
    raise ArgumentError, "queue size must be positive" unless max > 0
    @max = max
    @enque_cond = ConditionVariable.new
    @num_enqueue_waiting = 0
    super()
  end

  #
  # Returns the maximum size of the queue.
  #
  def max
    @max
  end

  #
  # Sets the maximum size of the queue.
  #
  def max=(max)
    max = Rubinius::Type.num2long(max)
    raise ArgumentError, "queue size must be positive" unless max > 0

    @mutex.synchronize do
      if max <= @max
        @max = max
      else
        diff = max - @max
        @max = max
        diff.times do
          @enque_cond.signal
        end
      end
    end
    max
  end

  #
  # Pushes +obj+ to the queue.  If there is no space left in the queue, waits
  # until space becomes available.
  #
  def push(obj, non_block=false)
    Thread.handle_interrupt(RuntimeError => :on_blocking) do
      @mutex.synchronize do
        while true
          break if @que.length < @max
          raise ThreadError, "queue full" if non_block
          @num_enqueue_waiting += 1
          begin
            @enque_cond.wait @mutex
          ensure
            @num_enqueue_waiting -= 1
          end
        end

        @que.push obj
        @cond.signal
      end
      self
    end
  end

  #
  # Alias of push
  #
  alias << push

  #
  # Alias of push
  #
  alias enq push

  #
  # Retrieves data from the queue and runs a waiting thread, if any.
  #
  def pop(*args)
    retval = super
    @mutex.synchronize do
      if @que.length < @max
        @enque_cond.signal
      end
    end
    retval
  end

  #
  # Alias of pop
  #
  alias shift pop

  #
  # Alias of pop
  #
  alias deq pop

  #
  # Returns the number of threads waiting on the queue.
  #
  def num_waiting
    @num_waiting + @num_enqueue_waiting
  end
end

# Documentation comments:
#  - How do you make RDoc inherit documentation from superclass?

# Truffle: define marshal_dump as MRI tests expect it
[ConditionVariable, Queue].each do |klass|
  klass.class_exec do
    def marshal_dump
      raise TypeError, "can't dump #{self.class}"
    end
  end
end
