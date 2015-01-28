package com.wizzardo.http.request;


/**
 * @author: wizzardo
 * Date: 7/30/14
 */
public class ByteTree {

    private Node root;

    public ByteTree() {
    }

    public Node getRoot() {
        return root;
    }

    public ByteTree(String s) {
        byte[] bytes = s.getBytes();

        root = new SingleNode();
        Node temp = root;

        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            temp = temp.append(b).next(b);
        }
        temp.setValue(s);
    }

    public ByteTree append(String s) {
        return append(s.getBytes(), s);
    }

    public ByteTree append(byte[] bytes, String s) {
        if (root == null)
            root = new SingleNode();

        byte b = bytes[0];
        root = root.append(b);
        Node temp = root.next(b);
        Node prev = root;
        byte p = b;
        for (int i = 1; i < bytes.length; i++) {
            b = bytes[i];
            Node next = temp.append(b);
            prev.set(p, next);
            prev = next;
            temp = next.next(b);
            p = b;
        }
        temp.setValue(s);

        return this;
    }

    public ByteTree appendIgnoreCase(String s) {
        if (root == null)
            root = new ArrayNode(0);
        return appendIgnoreCase(s.getBytes(), s, 0, root, null, (byte) 0);
    }

    private ByteTree appendIgnoreCase(byte[] bytes, String s, int i, Node current, Node prev, byte p) {
        if (i == bytes.length) {
            current.setValue(s);
            return this;
        }

        byte b = bytes[i];
        byte upper = toUpperCase(b);
        byte lower = toLowerCase(b);
        current = current.append(upper);
        current = current.append(lower);
        if (prev != null)
            prev.set(p, current);

        appendIgnoreCase(bytes, s, i + 1, current.next(upper), current, upper);
        appendIgnoreCase(bytes, s, i + 1, current.next(lower), current, lower);

        return this;
    }

    private byte toUpperCase(byte b) {
        return (byte) String.valueOf((char) b).toUpperCase().charAt(0);
    }

    private byte toLowerCase(byte b) {
        return (byte) String.valueOf((char) b).toLowerCase().charAt(0);
    }

    public boolean contains(String name) {
        if (root == null)
            return false;

        return root.get(name.getBytes()) != null;
    }

    public static abstract class Node {
        protected String value;

        public abstract Node next(byte b);

        public abstract Node append(byte b);

        public abstract Node set(byte b, Node node);

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String get(byte[] bytes) {
            return get(bytes, 0, bytes.length);
        }

        public String get(byte[] bytes, int offset, int length) {
            Node node = getNode(bytes, offset, length);
            return node == null ? null : node.value;
        }

        public Node getNode(byte[] bytes, int offset, int length) {
            Node node = this;
            for (int i = offset; i < offset + length && node != null; i++) {
                node = node.next(bytes[i]);
            }
            return node;
        }
    }

    public static class ArrayNode extends Node {
        private Node[] nodes;

        public ArrayNode(int size) {
            increase(size);
        }

        @Override
        public Node next(byte b) {
            int i = b & 0xff;
            if (i >= nodes.length)
                return null;

            return nodes[i];
        }

        @Override
        public Node append(byte b) {
            int i = b & 0xff;
            increase(i + 1);

            if (nodes[i] == null)
                nodes[i] = new SingleNode();

            return this;
        }

        @Override
        public Node set(byte b, Node node) {
            int i = b & 0xff;
            increase(i + 1);

            nodes[i] = node;

            return this;
        }

        private void increase(int size) {
            if (nodes == null)
                nodes = new Node[size];
            else if (nodes.length < size) {
                Node[] temp = new Node[size];
                System.arraycopy(nodes, 0, temp, 0, nodes.length);
                nodes = temp;
            }
        }
    }

    public static class SingleNode extends Node {
        private byte b;
        private Node next;

        @Override
        public Node next(byte b) {
            if (b == this.b)
                return next;
            return null;
        }

        @Override
        public Node append(byte b) {
            if (next != null && this.b != b) {
                ArrayNode node = new ArrayNode(Math.max(this.b & 0xff, b & 0xff));
                node.set(this.b, next);
                node.append(b);
                return node;
            } else if (this.b == b)
                return this;
            else {
                this.b = b;
                next = new SingleNode();
                return this;
            }
        }

        @Override
        public Node set(byte b, Node n) {
            if (next != null && this.b != b) {
                ArrayNode node = new ArrayNode(Math.max(this.b & 0xff, b & 0xff));
                node.set(this.b, next);
                node.set(b, n);
                return node;
            } else if (this.b == b) {
                next = n;
                return this;
            } else {
                this.b = b;
                next = n;
                return this;
            }
        }

        @Override
        public String toString() {
            return "single " + b;
        }
    }
}
