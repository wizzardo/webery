package com.wizzardo.http;

import com.wizzardo.http.filter.BaseAuthTokenFilter;
import com.wizzardo.http.request.Request;

import java.io.File;

/**
 * Created by wizzardo on 23.02.15.
 */
public class TokenizedFileTreeHandler extends FileTreeHandler {
    protected BaseAuthTokenFilter tokenFilter;

    public TokenizedFileTreeHandler(File workDir, String prefix, BaseAuthTokenFilter tokenFilter) {
        super(workDir, prefix);
        this.tokenFilter = tokenFilter;
    }

    public TokenizedFileTreeHandler(String workDir, String prefix, BaseAuthTokenFilter tokenFilter) {
        super(workDir, prefix);
        this.tokenFilter = tokenFilter;
    }

    @Override
    protected String generateUrl(String path, File file, Request request) {
        return super.generateUrl(path, file, request) + "?token=" + tokenFilter.generateToken(request);
    }
}
