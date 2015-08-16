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

    public interface CharConsumer {
        boolean consume(char ch);
    }
}
