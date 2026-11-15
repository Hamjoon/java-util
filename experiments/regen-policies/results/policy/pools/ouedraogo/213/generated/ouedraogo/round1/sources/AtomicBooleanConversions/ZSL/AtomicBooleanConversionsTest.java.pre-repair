package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class AtomicBooleanConversionsTest {

    private Converter converter;
    private ConverterOptions options;

    @Before
    public void setUp() {
        // Assuming ConverterOptions has a public no‑arg constructor.
        options = new ConverterOptions();
        converter = new Converter(options);
    }

    @Test
    public void testToByte() {
        AtomicBoolean trueAb = new AtomicBoolean(true);
        AtomicBoolean falseAb = new AtomicBoolean(false);

        assertEquals(Byte.valueOf((byte) 1), AtomicBooleanConversions.toByte(trueAb, converter));
        assertEquals(Byte.valueOf((byte) 0), AtomicBooleanConversions.toByte(falseAb, converter));
    }

    @Test
    public void testToShort() {
        AtomicBoolean trueAb = new AtomicBoolean(true);
        AtomicBoolean falseAb = new AtomicBoolean(false);

        assertEquals(Short.valueOf((short) 1), AtomicBooleanConversions.toShort(trueAb, converter));
        assertEquals(Short.valueOf((short) 0), AtomicBooleanConversions.toShort(falseAb, converter));
    }

    @Test
    public void testToInt() {
        AtomicBoolean trueAb = new AtomicBoolean(true);
        AtomicBoolean falseAb = new AtomicBoolean(false);

        assertEquals(Integer.valueOf(1), AtomicBooleanConversions.toInt(trueAb, converter));
        assertEquals(Integer.valueOf(0), AtomicBooleanConversions.toInt(falseAb, converter));
    }

    @Test
    public void testToLong() {
        AtomicBoolean trueAb = new AtomicBoolean(true);
        AtomicBoolean falseAb = new AtomicBoolean(false);

        assertEquals(Long.valueOf(1L), AtomicBooleanConversions.toLong(trueAb, converter));
        assertEquals(Long.valueOf(0L), AtomicBooleanConversions.toLong(falseAb, converter));
    }

    @Test
    public void testToFloat() {
        AtomicBoolean trueAb = new AtomicBoolean(true);
        AtomicBoolean falseAb = new AtomicBoolean(false);

        assertEquals(Float.valueOf(1.0f), AtomicBooleanConversions.toFloat(trueAb, converter));
        assertEquals(Float.valueOf(0.0f), AtomicBooleanConversions.toFloat(falseAb, converter));
    }

    @Test
    public void testToDouble() {
        AtomicBoolean trueAb = new AtomicBoolean(true);
        AtomicBoolean falseAb = new AtomicBoolean(false);

        assertEquals(Double.valueOf(1.0d), AtomicBooleanConversions.toDouble(trueAb, converter));
        assertEquals(Double.valueOf(0.0d), AtomicBooleanConversions.toDouble(falseAb, converter));
    }

    @Test
    public void testToBoolean() {
        AtomicBoolean trueAb = new AtomicBoolean(true);
        AtomicBoolean falseAb = new AtomicBoolean(false);

        assertTrue(AtomicBooleanConversions.toBoolean(trueAb, converter));
        assertFalse(AtomicBooleanConversions.toBoolean(falseAb, converter));
    }

    @Test
    public void testToAtomicBoolean() {
        AtomicBoolean trueAb = new AtomicBoolean(true);
        AtomicBoolean falseAb = new AtomicBoolean(false);

        AtomicBoolean resultTrue = AtomicBooleanConversions.toAtomicBoolean(trueAb, converter);
        AtomicBoolean resultFalse = AtomicBooleanConversions.toAtomicBoolean(falseAb, converter);

        assertNotSame(trueAb, resultTrue); // should be a new instance
        assertTrue(resultTrue.get());

        assertNotSame(falseAb, resultFalse);
        assertFalse(resultFalse.get());
    }

    @Test
    public void testToAtomicInteger() {
        AtomicBoolean trueAb = new AtomicBoolean(true);
        AtomicBoolean falseAb = new AtomicBoolean(false);

        AtomicInteger intTrue = AtomicBooleanConversions.toAtomicInteger(trueAb, converter);
        AtomicInteger intFalse = AtomicBooleanConversions.toAtomicInteger(falseAb, converter);

        assertEquals(1, intTrue.get());
        assertEquals(0, intFalse.get());
    }

    @Test
    public void testToAtomicLong() {
        AtomicBoolean trueAb = new AtomicBoolean(true);
        AtomicBoolean falseAb = new AtomicBoolean(false);

        AtomicLong longTrue = AtomicBooleanConversions.toAtomicLong(trueAb, converter);
        AtomicLong longFalse = AtomicBooleanConversions.toAtomicLong(falseAb, converter);

        assertEquals(1L, longTrue.get());
        assertEquals(0L, longFalse.get());
    }

    @Test
    public void testToCharacter() {
        AtomicBoolean trueAb = new AtomicBoolean(true);
        AtomicBoolean falseAb = new AtomicBoolean(false);

        char expectedTrue = options.trueChar();
        char expectedFalse = options.falseChar();

        assertEquals(expectedTrue, AtomicBooleanConversions.toCharacter(trueAb, converter).charValue());
        assertEquals(expectedFalse, AtomicBooleanConversions.toCharacter(falseAb, converter).charValue());
    }

    @Test
    public void testToBigDecimal() {
        AtomicBoolean trueAb = new AtomicBoolean(true);
        AtomicBoolean falseAb = new AtomicBoolean(false);

        assertEquals(BigDecimal.ONE, AtomicBooleanConversions.toBigDecimal(trueAb, converter));
        assertEquals(BigDecimal.ZERO, AtomicBooleanConversions.toBigDecimal(falseAb, converter));
    }

    @Test
    public void testToBigInteger() {
        AtomicBoolean trueAb = new AtomicBoolean(true);
        AtomicBoolean falseAb = new AtomicBoolean(false);

        assertEquals(BigInteger.ONE, AtomicBooleanConversions.toBigInteger(trueAb, converter));
        assertEquals(BigInteger.ZERO, AtomicBooleanConversions.toBigInteger(falseAb, converter));
    }

    @Test(expected = ClassCastException.class)
    public void testInvalidSourceThrowsClassCastException() {
        // Passing a non‑AtomicBoolean should result in a ClassCastException
        AtomicInteger notAnAtomicBoolean = new AtomicInteger(1);
        AtomicBooleanConversions.toByte(notAnAtomicBoolean, converter);
    }
}
