package com.wizzardo.http.request;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wizzardo on 06.01.16.
 */
public class BlockReaderTest {

    @Test
    public void test_findPart() {
        byte[] separator = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        byte[] data = new byte[separator.length];

        BlockReader br = new BlockReader(separator, null);

        for (int i = 0; i < separator.length; i++) {
            Arrays.fill(data, (byte) 0);
            System.arraycopy(separator, 0, data, i, separator.length - i);
//            System.out.println(Arrays.toString(data));
            Assert.assertEquals(i, br.findPart(data, 0, data.length, separator));
        }
    }

    @Test
    public void test_startsWith() {
        byte[] separator = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        byte[] data = new byte[separator.length];

        BlockReader br = new BlockReader(separator, null);

        for (int i = 0; i < separator.length; i++) {
            Arrays.fill(data, (byte) 0);
            System.arraycopy(separator, i, data, 0, separator.length - i);
//            System.out.println(Arrays.toString(data));
            Assert.assertTrue(br.startsWith(data, 0, data.length, separator, i, separator.length - i));
        }
    }

    @Test
    public void test_search() {
        byte[] separator = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        byte[] data = new byte[separator.length];
        byte[] buffer = new byte[separator.length];

        BlockReader br = new BlockReader(separator, null);

        for (int i = 0; i <= separator.length; i++) {
            Arrays.fill(data, (byte) 0);
            Arrays.fill(buffer, (byte) 0);

            System.arraycopy(separator, 0, buffer, i, separator.length - i);
            System.arraycopy(separator, separator.length - i, data, 0, i);
//            System.out.println(Arrays.toString(buffer) + " " + Arrays.toString(data));
            Assert.assertEquals(i, br.search(separator, buffer, buffer.length, data, 0, data.length, br.bm));
        }
    }

    @Test
    public void test_process_1() {
        byte[] separator = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        AtomicInteger counter = new AtomicInteger();
        BlockReader br = new BlockReader(separator, (end, bytes, offset, length) -> {
            Assert.assertEquals(true, end);
            Assert.assertArrayEquals(new byte[]{1, 2, 3}, Arrays.copyOfRange(bytes, offset, length));
            counter.incrementAndGet();
        });

        br.process(new byte[]{1, 2, 3, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        br.end();

        Assert.assertEquals(1, counter.get());
    }

    @Test
    public void test_process_2() {
        byte[] separator = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        AtomicInteger counter = new AtomicInteger();
        BlockReader br = new BlockReader(separator, (end, bytes, offset, length) -> {
            if (counter.get() == 0) {
                Assert.assertEquals(true, end);
                Assert.assertEquals(0, length);
            } else if (counter.get() == 1) {
                Assert.assertEquals(true, end);
                Assert.assertArrayEquals(new byte[]{1, 2, 3}, Arrays.copyOfRange(bytes, offset, length));
            }
            counter.incrementAndGet();
        });

        br.process(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3});
        br.end();

        Assert.assertEquals(2, counter.get());
    }

    @Test
    public void test_process_3() {
        byte[] separator = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        AtomicInteger counter = new AtomicInteger();
        BlockReader br = new BlockReader(separator, (end, bytes, offset, length) -> {
            if (counter.get() == 0) {
                Assert.assertEquals(true, end);
                Assert.assertEquals(0, length);
            } else if (counter.get() == 1) {
                Assert.assertEquals(true, end);
                Assert.assertArrayEquals(new byte[]{1, 2, 3}, Arrays.copyOfRange(bytes, offset, offset + length));
            }
            counter.incrementAndGet();
        });

        br.process(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                1, 2, 3,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        br.end();

        Assert.assertEquals(2, counter.get());
    }

    @Test
    public void test_process_4() {
        byte[] separator = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        AtomicInteger counter = new AtomicInteger();
        BlockReader br = new BlockReader(separator, (end, bytes, offset, length) -> {
            Assert.assertArrayEquals(new byte[]{1, 2, 3}, Arrays.copyOfRange(bytes, offset, offset + length));
            counter.incrementAndGet();
        });

        br.process(new byte[]{
                1, 2, 3,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                1, 2, 3,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                1, 2, 3
        });
        br.end();

        Assert.assertEquals(3, counter.get());
    }

    @Test
    public void test_process_5() {
        byte[] separator = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        AtomicInteger counter = new AtomicInteger();
        BlockReader br = new BlockReader(separator, (end, bytes, offset, length) -> {
            Assert.assertArrayEquals(new byte[]{1, 2, 3}, Arrays.copyOfRange(bytes, offset, length));
            counter.incrementAndGet();
        });

        br.process(new byte[]{1, 2, 3});
        br.process(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        br.process(new byte[]{1, 2, 3});
        br.process(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        br.process(new byte[]{1, 2, 3});
        br.end();

        Assert.assertEquals(3, counter.get());
    }

    @Test
    public void test_process_6() {
        byte[] separator = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        AtomicInteger counter = new AtomicInteger();
        BlockReader br = new BlockReader(separator, (end, bytes, offset, length) -> {
            Assert.assertArrayEquals(new byte[]{1, 2, 3}, Arrays.copyOfRange(bytes, offset, length));
            counter.incrementAndGet();
        });

        br.process(new byte[]{1});
        br.process(new byte[]{2, 3});

        br.process(new byte[]{0, 1, 2, 3});
        br.process(new byte[]{4, 5, 6, 7, 8, 9});

        br.process(new byte[]{1});
        br.process(new byte[]{2});
        br.process(new byte[]{3});

        br.process(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        br.process(new byte[]{1, 2, 3});
        br.end();

        Assert.assertEquals(3, counter.get());
    }

    @Test
    public void test_process_7() {
        byte[] separator = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        AtomicInteger counter = new AtomicInteger();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BlockReader br = new BlockReader(separator, (end, bytes, offset, length) -> {
            out.write(bytes, offset, length);
            if (end) {
                Assert.assertArrayEquals(new byte[]{1, 2, 3}, out.toByteArray());
                out.reset();
                counter.incrementAndGet();
            }
        });

        byte[] data = {
                1, 2, 3,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                1, 2, 3,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                1, 2, 3
        };
        for (int i = 0; i < data.length; i++) {
            br.process(Arrays.copyOfRange(data, i, i + 1));
        }
        br.end();

        Assert.assertEquals(3, counter.get());
    }

    @Test
    public void test_process_8() {
        byte[] separator = new byte[]{0};

        AtomicInteger counter = new AtomicInteger();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BlockReader br = new BlockReader(separator, (end, bytes, offset, length) -> {
            out.write(bytes, offset, length);
            if (end) {
                Assert.assertArrayEquals(new byte[]{1, 2, 3}, out.toByteArray());
                out.reset();
                counter.incrementAndGet();
            }
        });

        byte[] data = {
                1, 2, 3,
                0,
                1, 2, 3,
                0,
                1, 2, 3
        };
        for (int i = 0; i < data.length; i++) {
            br.process(Arrays.copyOfRange(data, i, i + 1));
        }
        br.end();

        Assert.assertEquals(3, counter.get());
    }

    @Test
    public void test_process_9() {
        byte[] separator = new byte[]{0};

        AtomicInteger counter = new AtomicInteger();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BlockReader br = new BlockReader(separator, (end, bytes, offset, length) -> {
            out.write(bytes, offset, length);
            if (end) {
                Assert.assertArrayEquals(new byte[]{1, 2, 3}, out.toByteArray());
                out.reset();
                counter.incrementAndGet();
            }
        });

        byte[] data = {
                1, 2, 3,
                0,
                1, 2, 3,
                0,
                1, 2, 3
        };
        br.process(data);
        br.end();

        Assert.assertEquals(3, counter.get());
    }

    @Test
    public void test_process_10() {
        byte[] separator = new byte[]{0};

        AtomicInteger counter = new AtomicInteger();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BlockReader br = new BlockReader(separator, (end, bytes, offset, length) -> {
            out.write(bytes, offset, length);
            if (end) {
                Assert.assertArrayEquals(new byte[]{1, 2, 3}, out.toByteArray());
                out.reset();
                counter.incrementAndGet();
            }
        });

        br.process(new byte[]{1, 2});
        br.process(new byte[]{3, 0, 1, 2, 3, 0});
        br.process(new byte[]{1, 2, 3});
        br.end();

        Assert.assertEquals(3, counter.get());
    }

    @Test
    public void test_process_11() {
        byte[] separator = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        AtomicInteger counter = new AtomicInteger();
        BlockReader br = new BlockReader(separator, (end, bytes, offset, length) -> {
            if (counter.get() == 0) {
                Assert.assertEquals(false, end);
                Assert.assertArrayEquals(new byte[]{1, 2, 3}, Arrays.copyOfRange(bytes, offset, offset + length));
            } else if (counter.get() == 1) {
                Assert.assertEquals(true, end);
                Assert.assertArrayEquals(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, -1}, Arrays.copyOfRange(bytes, offset, offset + length));
            }
            counter.incrementAndGet();
        });

        br.process(new byte[]{1, 2, 3});
        br.process(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, -1});
        br.process(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        br.end();

        Assert.assertEquals(2, counter.get());
    }

}