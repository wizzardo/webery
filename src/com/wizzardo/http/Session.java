package com.wizzardo.http;

import com.wizzardo.tools.cache.Cache;
import com.wizzardo.tools.security.MD5;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: wizzardo
 * Date: 9/2/14
 */
public class Session extends ConcurrentHashMap {

    private static volatile Cache<String, Session> cache;
    private static Random random = new Random();

    private String id;

    private Session() {
    }

    private Session(String id) {
        this.id = id;
    }

    static void createSessionsHolder(long ttl) {
        cache = new Cache<>(ttl, id -> new Session(id));
    }

    public static Session find(String id) {
        return cache.get(id, true);
    }

    public static Session create() {
        Session session = new Session();
        String id;
        do {
            id = MD5.getMD5AsString(String.valueOf(random.nextInt(Integer.MAX_VALUE)));
        } while (!cache.putIfAbsent(id, session));

        session.id = id;
        return session;
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