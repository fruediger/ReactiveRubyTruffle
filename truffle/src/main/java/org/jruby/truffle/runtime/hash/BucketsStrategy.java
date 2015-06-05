/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.hash;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import org.jruby.truffle.nodes.core.hash.HashNodes;
import org.jruby.truffle.runtime.core.RubyBasicObject;

import java.util.Arrays;

public abstract class BucketsStrategy {

    public static final double LOAD_FACTOR = 0.75;

    public static final int SIGN_BIT_MASK = ~(1 << 31);

    private static final int[] CAPACITIES = Arrays.copyOf(org.jruby.RubyHash.MRI_PRIMES, org.jruby.RubyHash.MRI_PRIMES.length - 1);

    public static int capacityGreaterThan(int size) {
        for (int capacity : CAPACITIES) {
            if (capacity > size) {
                return capacity;
            }
        }

        return CAPACITIES[CAPACITIES.length - 1];
    }

    public static int getBucketIndex(int hashed, int bucketsCount) {
        return (hashed & SIGN_BIT_MASK) % bucketsCount;
    }

    public static void addNewEntry(RubyBasicObject hash, int hashed, Object key, Object value) {
        assert HashNodes.getStore(hash) instanceof Entry[];

        final Entry[] buckets = (Entry[]) HashNodes.getStore(hash);

        final Entry entry = new Entry(hashed, key, value);

        if (HashNodes.getFirstInSequence(hash) == null) {
            HashNodes.setFirstInSequence(hash, entry);
        } else {
            HashNodes.getLastInSequence(hash).setNextInSequence(entry);
            entry.setPreviousInSequence(HashNodes.getLastInSequence(hash));
        }

        HashNodes.setLastInSequence(hash, entry);

        final int bucketIndex = BucketsStrategy.getBucketIndex(hashed, buckets.length);

        Entry previousInLookup = buckets[bucketIndex];

        if (previousInLookup == null) {
            buckets[bucketIndex] = entry;
        } else {
            while (previousInLookup.getNextInLookup() != null) {
                previousInLookup = previousInLookup.getNextInLookup();
            }

            previousInLookup.setNextInLookup(entry);
        }

        HashNodes.setSize(hash, HashNodes.getSize(hash) + 1);

        assert HashOperations.verifyStore(hash);
    }

    @TruffleBoundary
    public static void resize(RubyBasicObject hash) {
        assert HashOperations.verifyStore(hash);

        final int bucketsCount = capacityGreaterThan(HashNodes.getSize(hash)) * 2;
        final Entry[] newEntries = new Entry[bucketsCount];

        Entry entry = HashNodes.getFirstInSequence(hash);

        while (entry != null) {
            final int bucketIndex = getBucketIndex(entry.getHashed(), bucketsCount);
            Entry previousInLookup = newEntries[bucketIndex];

            if (previousInLookup == null) {
                newEntries[bucketIndex] = entry;
            } else {
                while (previousInLookup.getNextInLookup() != null) {
                    previousInLookup = previousInLookup.getNextInLookup();
                }

                previousInLookup.setNextInLookup(entry);
            }

            entry.setNextInLookup(null);
            entry = entry.getNextInSequence();
        }

        HashNodes.setStore(hash, newEntries, HashNodes.getSize(hash), HashNodes.getFirstInSequence(hash), HashNodes.getLastInSequence(hash));

        assert HashOperations.verifyStore(hash);
    }

}
