package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class BooleanConversionsTest {

    // -------------------------------------------------------------------------
    // 1. List of all static conversion methods in BooleanConversions
    // -------------------------------------------------------------------------
    // static Byte            toByte(Object from, Converter converter)
    // static Short           toShort(Object from, Converter converter)
    // static Integer         toInt(Object from, Converter converter)
    // static AtomicInteger   toAtomicInteger(Object from, Converter converter)
    // static AtomicLong      toAtomicLong(Object from, Converter converter)
    // static AtomicBoolean   toAtomicBoolean(Object from, Converter converter)
    // static Long            toLong(Object from, Converter converter)
    // static BigDecimal      toBigDecimal(Object from, Converter converter)
    // static BigInteger      toBigInteger(Object from, Converter converter)
    // static Float           toFloat(Object from, Converter converter)
    // static Double          toDouble(Object from, Converter converter)
    // static char            toCharacter(Object from, Converter converter)

    private Converter defaultConverter;

    @Before
    public void setUp() {
        // Assuming ConverterOptions has a public no‑arg constructor that sets
        // trueChar() = 'T' and falseChar() = 'F'. If the real library uses
        // different defaults, the tests will still pass because we only verify
        // the mapping logic, not the exact characters.
        ConverterOptions options = new ConverterOptions(); // default options
        defaultConverter = new Converter(options);
    }

    // -------------------------------------------------------------------------
    // 2. Basic functionality tests for each conversion method
    // -------------------------------------------------------------------------

    @Test
    public void testToByteTrue() {
        Byte result = BooleanConversions.toByte(Boolean.TRUE, defaultConverter);
        assertEquals(CommonValues.BYTE_ONE, result);
    }

    @Test
    public void testToByteFalse() {
        Byte result = BooleanConversions.toByte(Boolean.FALSE, defaultConverter);
        assertEquals(CommonValues.BYTE_ZERO, result);
    }

    @Test
    public void testToShortTrue() {
        Short result = BooleanConversions.toShort(Boolean.TRUE, defaultConverter);
        assertEquals(CommonValues.SHORT_ONE, result);
    }

    @Test
    public void testToShortFalse() {
        Short result = BooleanConversions.toShort(Boolean.FALSE, defaultConverter);
        assertEquals(CommonValues.SHORT_ZERO, result);
    }

    @Test
    public void testToIntTrue() {
        Integer result = BooleanConversions.toInt(Boolean.TRUE, defaultConverter);
        assertEquals(CommonValues.INTEGER_ONE, result);
    }

    @Test
    public void testToIntFalse() {
        Integer result = BooleanConversions.toInt(Boolean.FALSE, defaultConverter);
        assertEquals(CommonValues.INTEGER_ZERO, result);
    }

    @Test
    public void testToAtomicIntegerTrue() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.TRUE, defaultConverter);
        assertEquals(1, ai.get());
    }

    @Test
    public void testToAtomicIntegerFalse() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.FALSE, defaultConverter);
        assertEquals(0, ai.get());
    }

    @Test
    public void testToAtomicLongTrue() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.TRUE, defaultConverter);
        assertEquals(1L, al.get());
    }

    @Test
    public void testToAtomicLongFalse() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.FALSE, defaultConverter);
        assertEquals(0L, al.get());
    }

    @Test
    public void testToAtomicBooleanTrue() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.TRUE, defaultConverter);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBooleanFalse() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.FALSE, defaultConverter);
        assertFalse(ab.get());
    }

    @Test
    public void testToLongTrue() {
        Long result = BooleanConversions.toLong(Boolean.TRUE, defaultConverter);
        assertEquals(CommonValues.LONG_ONE, result);
    }

    @Test
    public void testToLongFalse() {
        Long result = BooleanConversions.toLong(Boolean.FALSE, defaultConverter);
        assertEquals(CommonValues.LONG_ZERO, result);
    }

    @Test
    public void testToBigDecimalTrue() {
        BigDecimal bd = BooleanConversions.toBigDecimal(Boolean.TRUE, defaultConverter);
        assertEquals(BigDecimal.ONE, bd);
    }

    @Test
    public void testToBigDecimalFalse() {
        BigDecimal bd = BooleanConversions.toBigDecimal(Boolean.FALSE, defaultConverter);
        assertEquals(BigDecimal.ZERO, bd);
    }

    @Test
    public void testToBigIntegerTrue() {
        BigInteger bi = BooleanConversions.toBigInteger(Boolean.TRUE, defaultConverter);
        assertEquals(BigInteger.ONE, bi);
    }

    @Test
    public void testToBigIntegerFalse() {
        BigInteger bi = BooleanConversions.toBigInteger(Boolean.FALSE, defaultConverter);
        assertEquals(BigInteger.ZERO, bi);
    }

    @Test
    public void testToFloatTrue() {
        Float f = BooleanConversions.toFloat(Boolean.TRUE, defaultConverter);
        assertEquals(CommonValues.FLOAT_ONE, f);
    }

    @Test
    public void testToFloatFalse() {
        Float f = BooleanConversions.toFloat(Boolean.FALSE, defaultConverter);
        assertEquals(CommonValues.FLOAT_ZERO, f);
    }

    @Test
    public void testToDoubleTrue() {
        Double d = BooleanConversions.toDouble(Boolean.TRUE, defaultConverter);
        assertEquals(CommonValues.DOUBLE_ONE, d);
    }

    @Test
    public void testToDoubleFalse() {
        Double d = BooleanConversions.toDouble(Boolean.FALSE, defaultConverter);
        assertEquals(CommonValues.DOUBLE_ZERO, d);
    }

    @Test
    public void testToCharacterTrue() {
        char ch = BooleanConversions.toCharacter(Boolean.TRUE, defaultConverter);
        assertEquals(defaultConverter.getOptions().trueChar(), ch);
    }

    @Test
    public void testToCharacterFalse() {
        char ch = BooleanConversions.toCharacter(Boolean.FALSE, defaultConverter);
        assertEquals(defaultConverter.getOptions().falseChar(), ch);
    }

    // -------------------------------------------------------------------------
    // 3. Edge‑case and exception handling tests
    // -------------------------------------------------------------------------

    // Helper to assert that a method throws a specific exception when passed null
    private interface Conversion {
        Object apply(Object from);
    }

    private void assertThrowsNullPointer(Conversion conversion) {
        try {
            conversion.apply(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    private void assertThrowsClassCast(Conversion conversion) {
        try {
            conversion.apply(Integer.valueOf(42));
            fail("Expected ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }
    }

    @Test
    public void testNullInputThrowsNullPointerException() {
        assertThrowsNullPointer(from -> BooleanConversions.toByte(from, defaultConverter));
        assertThrowsNullPointer(from -> BooleanConversions.toShort(from, defaultConverter));
        assertThrowsNullPointer(from -> BooleanConversions.toInt(from, defaultConverter));
        assertThrowsNullPointer(from -> BooleanConversions.toAtomicInteger(from, defaultConverter));
        assertThrowsNullPointer(from -> BooleanConversions.toAtomicLong(from, defaultConverter));
        assertThrowsNullPointer(from -> BooleanConversions.toAtomicBoolean(from, defaultConverter));
        assertThrowsNullPointer(from -> BooleanConversions.toLong(from, defaultConverter));
        assertThrowsNullPointer(from -> BooleanConversions.toBigDecimal(from, defaultConverter));
        assertThrowsNullPointer(from -> BooleanConversions.toBigInteger(from, defaultConverter));
        assertThrowsNullPointer(from -> BooleanConversions.toFloat(from, defaultConverter));
        assertThrowsNullPointer(from -> BooleanConversions.toDouble(from, defaultConverter));
        assertThrowsNullPointer(from -> BooleanConversions.toCharacter(from, defaultConverter));
    }

    @Test
    public void testInvalidTypeThrowsClassCastException() {
        assertThrowsClassCast(from -> BooleanConversions.toByte(from, defaultConverter));
        assertThrowsClassCast(from -> BooleanConversions.toShort(from, defaultConverter));
        assertThrowsClassCast(from -> BooleanConversions.toInt(from, defaultConverter));
        assertThrowsClassCast(from -> BooleanConversions.toAtomicInteger(from, defaultConverter));
        assertThrowsClassCast(from -> BooleanConversions.toAtomicLong(from, defaultConverter));
        assertThrowsClassCast(from -> BooleanConversions.toAtomicBoolean(from, defaultConverter));
        assertThrowsClassCast(from -> BooleanConversions.toLong(from, defaultConverter));
        assertThrowsClassCast(from -> BooleanConversions.toBigDecimal(from, defaultConverter));
        assertThrowsClassCast(from -> BooleanConversions.toBigInteger(from, defaultConverter));
        assertThrowsClassCast(from -> BooleanConversions.toFloat(from, defaultConverter));
        assertThrowsClassCast(from -> BooleanConversions.toDouble(from, defaultConverter));
        assertThrowsClassCast(from -> BooleanConversions.toCharacter(from, defaultConverter));
    }

    @Test
    public void testToCharacterRespectsCustomOptions() {
        // Create custom options where trueChar = 'Y' and falseChar = 'N'
        ConverterOptions customOptions = new ConverterOptions() {
            @Override
            public char trueChar() { return 'Y'; }
            @Override
            public char falseChar() { return 'N'; }
        };
        Converter customConverter = new Converter(customOptions);

        char trueChar = BooleanConversions.toCharacter(Boolean.TRUE, customConverter);
        char falseChar = BooleanConversions.toCharacter(Boolean.FALSE, customConverter);

        assertEquals('Y', trueChar);
        assertEquals('N', falseChar);
    }
}
