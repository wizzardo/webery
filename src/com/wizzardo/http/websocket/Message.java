package com.wizzardo.http.websocket;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: wizzardo
 * Date: 06.10.14
 */
public class Message {
    protected List<Frame> frames = new ArrayList<>();

    public boolean isComplete() {
        if (frames.isEmpty())
            return false;

        Frame frame = frames.get(frames.size() - 1);
        return frame.isFinalFrame() && frame.isComplete();
    }

    public void add(Frame frame) {
        frames.add(frame);
    }

    public String asString() {
        return new String(asBytes());
    }

    public byte[] asBytes() {
        int length = 0;
        for (Frame frame : frames)
            length += frame.getLength();

        int offset = 0;
        byte[] data = new byte[length];

        for (Frame frame : frames) {
            System.arraycopy(frame.getData(), frame.getOffset(), data, offset, frame.getLength());
            offset += frame.getLength();
        }

        return data;
    }
}
