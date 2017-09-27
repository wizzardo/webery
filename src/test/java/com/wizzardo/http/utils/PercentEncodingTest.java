package com.wizzardo.http.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Mikhail Bobrutskov on 20.08.17.
 */
public class PercentEncodingTest {

    @Test
    public void getHexValueTest() {
        assertEquals(0, PercentEncoding.getHexValue('0'));
        assertEquals(1, PercentEncoding.getHexValue('1'));
        assertEquals(2, PercentEncoding.getHexValue('2'));
        assertEquals(3, PercentEncoding.getHexValue('3'));
        assertEquals(4, PercentEncoding.getHexValue('4'));
        assertEquals(5, PercentEncoding.getHexValue('5'));
        assertEquals(6, PercentEncoding.getHexValue('6'));
        assertEquals(7, PercentEncoding.getHexValue('7'));
        assertEquals(8, PercentEncoding.getHexValue('8'));
        assertEquals(9, PercentEncoding.getHexValue('9'));

        assertEquals(10, PercentEncoding.getHexValue('a'));
        assertEquals(11, PercentEncoding.getHexValue('b'));
        assertEquals(12, PercentEncoding.getHexValue('c'));
        assertEquals(13, PercentEncoding.getHexValue('d'));
        assertEquals(14, PercentEncoding.getHexValue('e'));
        assertEquals(15, PercentEncoding.getHexValue('f'));

        assertEquals(10, PercentEncoding.getHexValue('A'));
        assertEquals(11, PercentEncoding.getHexValue('B'));
        assertEquals(12, PercentEncoding.getHexValue('C'));
        assertEquals(13, PercentEncoding.getHexValue('D'));
        assertEquals(14, PercentEncoding.getHexValue('E'));
        assertEquals(15, PercentEncoding.getHexValue('F'));
    }

    @Test
    public void decodeTest() {
        byte[] bytes = "%2b".getBytes();

        int length = PercentEncoding.decode(bytes, 0, bytes.length);
        assertEquals(1, length);
        assertEquals("+", new String(bytes, 0, length));
    }

    @Test
    public void decodeTest_2() {
        byte[] bytes = "%2Bqwerty%23".getBytes();

        int length = PercentEncoding.decode(bytes, 0, bytes.length);
        assertEquals(8, length);
        assertEquals("+qwerty#", new String(bytes, 0, length));
    }
}