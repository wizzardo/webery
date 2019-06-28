package com.wizzardo.http.extra;

import com.wizzardo.http.BytesConsumer;
import com.wizzardo.http.BytesProducer;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Lazy;
import com.wizzardo.tools.misc.Pair;
import com.wizzardo.tools.misc.Unchecked;

import java.io.*;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipBytesProducer implements BytesProducer {

    protected Iterator<Pair<ZipEntry, Lazy<InputStream>>> entries;
    protected byte[] buffer = new byte[16 * 1024];
    protected DirectByteArrayOutputStream out;
    protected ZipOutputStream zipout;
    protected volatile InputStream currentInput;

    public ZipBytesProducer(Iterator<Pair<ZipEntry, Lazy<InputStream>>> entries) {
        this.entries = entries;
        out = new DirectByteArrayOutputStream(buffer.length);
        zipout = new ZipOutputStream(out);
        zipout.setMethod(ZipOutputStream.DEFLATED);
        zipout.setLevel(0);
    }

    public ZipBytesProducer(File folder) {
        this(FileTools.listRecursive(folder).stream()
                .map(file -> Pair.of(
                        new ZipEntry(file.getAbsolutePath().substring(folder.getParentFile().getAbsolutePath().length() + 1)),
                        Lazy.of(() -> Unchecked.call(() -> (InputStream) new FileInputStream(file))))
                )
                .collect(Collectors.toList()).iterator());
    }

    @Override
    public void produceTo(BytesConsumer consumer) throws IOException {
        if (currentInput == null) {
            zipout.closeEntry();

            if (entries.hasNext()) {
                Pair<ZipEntry, Lazy<InputStream>> pair = entries.next();
                currentInput = pair.value.get();
                zipout.putNextEntry(pair.key);
                produceTo(consumer);
            } else {
                zipout.close();
                if (out.length() > 0)
                    consumer.consume(out.bytes(), 0, out.length());
                consumer.consume(new byte[0], 0, 0);
            }
        } else {
            try {
                int read = currentInput.read(buffer);
                if (read > 0) {
                    zipout.write(buffer, 0, read);
                    if (out.length() > 0) {
                        consumer.consume(out.bytes(), 0, out.length());
                        out.reset();
                    } else {
                        produceTo(consumer);
                    }
                } else {
                    IOTools.close(currentInput);
                    currentInput = null;
                    produceTo(consumer);
                }
            } catch (IOException e) {
                IOTools.close(currentInput);
                throw Unchecked.rethrow(e);
            }
        }
    }

    static class DirectByteArrayOutputStream extends ByteArrayOutputStream {
        public DirectByteArrayOutputStream(int size) {
            super(size);
        }

        byte[] bytes() {
            return buf;
        }

        int length() {
            return count;
        }
    }
}
