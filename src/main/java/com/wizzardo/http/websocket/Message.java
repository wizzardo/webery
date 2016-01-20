package com.wizzardo.http.websocket;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: wizzardo
 * Date: 06.10.14
 */
public class Message {
    private List<Frame> frames = new ArrayList<>();

    public Message() {
    }

    public Message(String message) {
        append(message);
    }

    public Message(byte[] message) {
        append(message);
    }

    public Message(byte[] message, int offset, int length) {
        append(message, offset, length);
    }

    public boolean isComplete() {
        if (frames.isEmpty())
            return false;

        Frame frame = frames.get(frames.size() - 1);
        return frame.isFinalFrame() && frame.isComplete();
    }

    void add(Frame frame) {
        if (!frames.isEmpty())
            frames.get(frames.size() - 1).setIsFinalFrame(false);
        frames.add(frame);
    }

    public Message append(String s) {
        return append(s.getBytes(StandardCharsets.UTF_8));
    }

    public Message append(byte[] bytes) {
        return append(bytes, 0, bytes.length);
    }

    public Message append(byte[] bytes, int offset, int length) {
        add(new Frame(bytes, offset, length));
        return this;
    }

    public String asString() {
        return new String(asBytes(), StandardCharsets.UTF_8);
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

    List<Frame> getFrames() {
        return frames;
    }
}
