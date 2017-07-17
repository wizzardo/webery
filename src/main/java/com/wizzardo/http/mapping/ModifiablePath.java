package com.wizzardo.http.mapping;

import java.util.List;

/**
 * Created by Mikhail Bobrutskov on 14.07.17.
 */
public class ModifiablePath extends Path {
    public ModifiablePath() {
    }

    public ModifiablePath(int size) {
        super(size);
    }

    @Override
    public Path add(String part) {
        add(this, part);
        return this;
    }

    public void set(int i, String part) {
        parts.set(i, part);
    }

    @Override
    public List<String> parts() {
        return super.parts();
    }
}
