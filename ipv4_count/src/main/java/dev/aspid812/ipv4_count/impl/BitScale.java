package dev.aspid812.ipv4_count.impl;

import java.util.Arrays;


public class BitScale {

    public static final long BITS_PER_WORD = Long.SIZE;
    public static final long SIZE_LIMIT = Integer.MAX_VALUE * BITS_PER_WORD;

    private final long size;
    private final long[] words;

    public BitScale(long size) {
        if (size < 0 || size > SIZE_LIMIT) {
            throw new IllegalArgumentException("size = " + size);
        }

        this.size = size;
        this.words = new long[wordIndex(size - 1) + 1];
    }

    static int wordIndex(long bit) {
        return (int) (bit / BITS_PER_WORD);
    }

    public void witness(long bit) {
        if (bit < 0 || bit >= size) {
            throw new IndexOutOfBoundsException(bit);
        }

        words[wordIndex(bit)] |= 1L << bit;
    }

    public long count() {
        return Arrays.stream(words)
                .map(Long::bitCount)
                .sum();
    }
}
