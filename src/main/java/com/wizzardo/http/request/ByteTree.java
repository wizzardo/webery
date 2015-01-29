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
            root = new ArrayNode();
        return appendIgnoreCase(s.toLowerCase().getBytes(), s.toUpperCase().getBytes(), s, 0, root, null, (byte) 0);
    }

    private ByteTree appendIgnoreCase(byte[] lower, byte[] upper, String s, int i, Node current, Node prev, byte p) {
        if (i == lower.length) {
            current.setValue(s);
            return this;
        }

        byte u = upper[i];
        byte l = lower[i];
        current = current.append(u);
        current = current.append(l);
        if (prev != null)
            prev.set(p, current);

        appendIgnoreCase(lower, upper, s, i + 1, current.next(u), current, u);
        current.set(l, current.next(u));
        return this;
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
        int shift = -1;

        public ArrayNode() {
        }

        public ArrayNode(byte b, Node node, byte b2, Node node2) {
            int i1 = b & 0xff;
            int i2 = b2 & 0xff;

            shift = Math.min(i1, i2);
            increase(Math.max(i1, i2) - shift + 1);

            nodes[i1 - shift] = node;
            nodes[i2 - shift] = node2;
        }

        @Override
        public Node next(byte b) {
            int i = (b & 0xff) - shift;
            if (i >= nodes.length || i < 0)
                return null;

            return nodes[i];
        }

        @Override
        public Node append(byte b) {
            set((b & 0xff), new SingleNode(), true);
            return this;
        }

        @Override
        public Node set(byte b, Node node) {
            set(b & 0xff, node);
            return this;
        }

        public Node set(int i, Node node) {
            return set(i, node, false);
        }

        public Node set(int i, Node node, boolean ifNull) {
            if (shift == -1)
                shift = i;
            if (i - shift < 0)
                increase(i - shift);
            else
                increase(i + 1 - shift);


            if (ifNull) {
                if (nodes[i - shift] == null)
                    nodes[i - shift] = node;
            } else
                nodes[i - shift] = node;

            return this;
        }

        private void increase(int size) {
            if (size == 0)
                size = -1;

            if (nodes == null)
                nodes = new Node[size];
            else if (size < 0) {
                Node[] temp = new Node[nodes.length - size];
                System.arraycopy(nodes, 0, temp, -size, nodes.length);
                nodes = temp;
                shift += size;
            } else if (nodes.length < size) {
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
                return new ArrayNode(this.b, next, b, new SingleNode());
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
                return new ArrayNode(this.b, next, b, n);
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
