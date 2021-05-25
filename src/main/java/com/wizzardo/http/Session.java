package com.wizzardo.http;

import com.wizzardo.http.request.Request;
import com.wizzardo.tools.cache.Cache;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: wizzardo
 * Date: 9/2/14
 */
public class Session extends ConcurrentHashMap {

    private static volatile Cache<String, Session> cache;
    private static volatile SessionIdGenerator sessionIdGenerator = new MD5SessionIdGenerator();

    private String id;

    private Session(String id) {
        this.id = id;
    }

    static void createSessionsHolder(long ttl) {
        cache = new Cache<>("sessions", ttl);
    }

    public static Session find(String id) {
        return cache.get(id, true);
    }

    public static Session create(Request request) {
        String id = sessionIdGenerator.generate(request);
        return cache.get(id, Session::new);
    }

    public static void setSessionIdGenerator(SessionIdGenerator sessionIdGenerator) {
        Session.sessionIdGenerator = sessionIdGenerator;
    }

    public String getId() {
        return id;
    }

    public void setTTL(long ttl) {
        cache.put(id, this, ttl);
    }

    public long getTTL() {
        return cache.getTTL(id);
    }

    public void invalidate() {
        cache.remove(id);
        clear();
    }
}