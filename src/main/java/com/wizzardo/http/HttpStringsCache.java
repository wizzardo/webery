package com.wizzardo.http;

import com.wizzardo.http.request.ByteTree;

public class HttpStringsCache {
    protected ByteTree tree = new ByteTree();

    public ByteTree getTree() {
        return tree;
    }
}
