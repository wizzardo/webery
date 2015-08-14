package com.wizzardo.http.http2.huffman;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wizzardo on 27.07.15.
 */
public class HuffmanTree extends Node {

    public void add(Leaf leaf) {
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

}
