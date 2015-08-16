package com.wizzardo.http.http2.huffman;

import java.util.Arrays;

/**
 * Created by wizzardo on 27.07.15.
 */
public class HuffmanTree extends Node {

    protected Leaf[] leafs;

    public void add(Leaf leaf) {
        addLeafToArray(leaf);
        int bits = leaf.bits;
        Node node = this;
        for (int i = 0; i < leaf.length - 1; i++) {
            int bit = (bits >> leaf.length - i - 1) & 1;

            Node next = bit == 0 ? node.left : node.right;
            if (next == null) {
                if (bit == 0)
                    next = node.left = new Node();
                else
                    next = node.right = new Node();
            }

            node = next;
        }

        int bit = bits & 1;
        if (bit == 0)
            node.left = leaf;
        else
            node.right = leaf;
    }

    protected void addLeafToArray(Leaf leaf) {
        if (leafs == null || leafs.length <= leaf.value) {
            Leaf[] temp = new Leaf[leaf.value + 1];
            if (leafs != null)
                System.arraycopy(leafs, 0, temp, 0, leafs.length);
            leafs = temp;
        }
        leafs[leaf.value] = leaf;
    }

    public int decode(byte[] data, int offset, CharConsumer consumer) {
        Node node = this;
        int bit;
        int k;
        byte b;
        int i = offset << 3;
        int length = data.length << 3;
        while (i < length) {
            b = data[i >> 3];
            k = i - ((i >> 3) << 3);
            bit = (b >> 7 - k) & 1;
            node = node.get(bit);
            i++;
            if (node.isEnd()) {
                if (!consumer.consume(node.get()))
                    break;
                node = this;
            }
        }
        return i;
    }

    public int encode(byte[] data, int offset, String s) {
        return encode(data, offset, s.toCharArray());
    }

    public int encode(byte[] data, int offset, char[] chars) {
        return encode(data, offset, chars, 0, chars.length);
    }

    public int encode(byte[] data, int offset, char[] chars, int from, int to) {
        int k;
        byte b;
        int i = offset << 3;

        for (int j = from; j < to; j++) {
            Leaf leaf = leafs[chars[j]];
            int l = leaf.length;
            k = 8 - i + ((i >> 3) << 3);
            if (l <= k) {
                if (k == 8)
                    data[i >> 3] = (byte) (leaf.bits << k - l);
                else {
                    b = (byte) (data[i >> 3] & (0xff << k));
                    b = (byte) (b | (leaf.bits << k - l));
                    data[i >> 3] = b;
                }
                i += l;
            } else {
                int ll = l;
                b = (byte) (data[i >> 3] & (0xff << k));
                b = (byte) (b | (leaf.bits >> ll - k));
                data[i >> 3] = b;

                ll -= k;
                i += k;
                while (ll >= 8) {
                    ll -= 8;
                    data[i >> 3] = (byte) (leaf.bits >> ll);
                    i += 8;
                }
                if (ll > 0) {
                    data[i >> 3] = (byte) (leaf.bits << 8 - ll);
                    i += ll;
                }
            }
        }

        return i;
    }

    public interface CharConsumer {
        boolean consume(char ch);
    }
}
