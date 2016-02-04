package com.wizzardo.http;

import com.wizzardo.http.filter.TokenFilter;
import com.wizzardo.http.request.Request;

import java.io.File;

/**
 * Created by wizzardo on 23.02.15.
 */
public class TokenizedFileTreeHandler<T extends TokenizedFileTreeHandler.HandlerContextWithToken> extends FileTreeHandler<T> {
    protected TokenFilter tokenFilter;

    public TokenizedFileTreeHandler(File workDir, String prefix, TokenFilter tokenFilter, String name) {
        super(workDir, prefix, name);
        this.tokenFilter = tokenFilter;
    }

    public TokenizedFileTreeHandler(String workDir, String prefix, TokenFilter tokenFilter, String name) {
        super(workDir, prefix, name);
        this.tokenFilter = tokenFilter;
    }

    @Override
    protected String generateUrl(File file, T handlerContext) {
        return super.generateUrl(file, handlerContext) + "?token=" + handlerContext.token;
    }

    @Override
    protected T createHandlerContext(String path, Request request) {
        return (T) new HandlerContextWithToken(path, request);
    }

    protected class HandlerContextWithToken extends HandlerContext {
        protected final String token;

        public HandlerContextWithToken(String path, Request request) {
            super(path);
            token = tokenFilter.generateToken(request);
        }
    }
}
