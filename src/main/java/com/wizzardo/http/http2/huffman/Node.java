package com.wizzardo.http.http2.huffman;

/**
 * Created by wizzardo on 28.07.15.
 */
public class Node {

    Node left;
    Node right;

    public Node(Node left, Node right) {
        this.left = left;
        this.right = right;
    }

    public Node() {
    }

    public boolean isEnd() {
        return false;
    }

    public Node get(int bit) {
        if (bit == 0)
            return left;
        else if (bit == 1)
            return right;
        else
            throw new IllegalArgumentException("parameter 'bit' must be '1' or '0'");
    }

    public char get() {
        return 0;
    }

    public Node left() {
        return left;
    }

    public Node right() {
        return right;
    }
}
