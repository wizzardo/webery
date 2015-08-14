package com.wizzardo.http.http2;

import com.wizzardo.http.http2.huffman.HuffmanTree;
import com.wizzardo.http.http2.huffman.Leaf;
import com.wizzardo.http.http2.huffman.Node;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wizzardo on 14.08.15.
 */
public class HuffmanTreeTest {

    public void test_1() {
        List<Leaf> leafs = new ArrayList<>();
        leafs.add(new Leaf('A', 0b0, 1));
        leafs.add(new Leaf('B', 0b100, 3));
        leafs.add(new Leaf('C', 0b1010, 4));
        leafs.add(new Leaf('D', 0b1011, 4));
        leafs.add(new Leaf('E', 0b1100, 4));
        leafs.add(new Leaf('F', 0b1101, 4));
        leafs.add(new Leaf('G', 0b1110, 4));
        leafs.add(new Leaf('H', 0b1111, 4));

        HuffmanTree tree = new HuffmanTree();
        for (Leaf leaf : leafs) {
            tree.add(leaf);
        }

        byte[] data = new byte[]{(byte) 0b10001010, 0b01011011, 0b00011010, (byte) 0b10010000, 0b01110011, (byte) 0b11000000};

        StringBuilder sb = new StringBuilder();
        Node node = tree;
        int bit;
        int i = 0;
        int n = 5 * 8 + 2;
        while (i < n) {
            byte b = data[i / 8];
            int k = i % 8;
            bit = (b >> 7 - k) & 1;
            node = node.get(bit);
            if (node.isEnd()) {
                sb.append(node.get());
                node = tree;
            }
            i++;
        }

        Assert.assertEquals("BACADAEAFABBAAAGAH", sb.toString());
    }
}
