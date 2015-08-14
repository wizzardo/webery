package com.wizzardo.http.http2.huffman;

/**
 * Created by wizzardo on 28.07.15.
 */
public class Leaf extends Node implements Comparable<Leaf> {
    final char value;
    final int bits;
    final int length;

    public Leaf(char value, int bits, int length) {
        this.value = value;
        this.bits = bits;
        this.length = length;
    }

    @Override
    public boolean isEnd() {
        return true;
    }

    @Override
    public char get() {
        return value;
    }

    @Override
    public int compareTo(Leaf o) {
        return Integer.compare(length, o.length);
    }

    @Override
    public String toString() {
        return "Leaf{" +
                "value=" + value +
                ", bits=" + bits +
                ", length=" + length +
                '}';
    }
}
