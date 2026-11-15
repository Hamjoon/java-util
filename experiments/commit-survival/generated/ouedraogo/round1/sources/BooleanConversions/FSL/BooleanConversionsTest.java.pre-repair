package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * Unit tests for {@link BooleanConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Correct numeric/atomic conversion for {@code true} and {@code false}.</li>
 *   <li>Correct {@link BigDecimal}/{@link BigInteger} conversion.</li>
 *   <li>Correct {@link Character} conversion based on {@link ConverterOptions}.</li>
 *   <li>Behaviour when the supplied {@code Object} is {@code null} or not a {@link Boolean}.</li>
 * </ul>
 *
 * Only the public API of {@link Converter} and the classes used inside
 * {@link BooleanConversions} are exercised – no mocking or subclassing of library
 * internals is performed.
 */
public class BooleanConversionsTest {

    // ----------------------------------------------------------------------
    // Helper to create a Converter with known true/false characters
    // ----------------------------------------------------------------------
    private static Converter createConverter(char trueChar, char falseChar) {
        // ConverterOptions is assumed to have a public constructor and mutators
        // for the true/false characters. If the real API differs, adjust accordingly.
        ConverterOptions options = new ConverterOptions();
        options.setTrueChar(trueChar);
        options.setFalseChar(falseChar);
        return new Converter(options);
    }

    // ----------------------------------------------------------------------
    // Byte conversion
    // ----------------------------------------------------------------------
    @Test
    public void testToByte() {
        Converter converter = createConverter('T', 'F');
        assertEquals(Byte.valueOf((byte) 1), BooleanConversions.toByte(Boolean.TRUE, converter));
        assertEquals(Byte.valueOf((byte) 0), BooleanConversions.toByte(Boolean.FALSE, converter));
    }

    // ----------------------------------------------------------------------
    // Short conversion
    // ----------------------------------------------------------------------
    @Test
    public void testToShort() {
        Converter converter = createConverter('Y', 'N');
        assertEquals(Short.valueOf((short) 1), BooleanConversions.toShort(Boolean.TRUE, converter));
        assertEquals(Short.valueOf((short) 0), BooleanConversions.toShort(Boolean.FALSE, converter));
    }

    // ----------------------------------------------------------------------
    // Integer conversion
    // ----------------------------------------------------------------------
    @Test
    public void testToInt() {
        Converter converter = createConverter('1', '0');
        assertEquals(Integer.valueOf(1), BooleanConversions.toInt(Boolean.TRUE, converter));
        assertEquals(Integer.valueOf(0), BooleanConversions.toInt(Boolean.FALSE, converter));
    }

    // ----------------------------------------------------------------------
    // AtomicInteger conversion
    // ----------------------------------------------------------------------
    @Test
    public void testToAtomicInteger() {
        Converter converter = createConverter('T', 'F');
        AtomicInteger aiTrue = BooleanConversions.toAtomicInteger(Boolean.TRUE, converter);
        AtomicInteger aiFalse = BooleanConversions.toAtomicInteger(Boolean.FALSE, converter);
        assertEquals(1, aiTrue.get());
        assertEquals(0, aiFalse.get());
    }

    // ----------------------------------------------------------------------
    // AtomicLong conversion
    // ----------------------------------------------------------------------
    @Test
    public void testToAtomicLong() {
        Converter converter = createConverter('Y', 'N');
        AtomicLong alTrue = BooleanConversions.toAtomicLong(Boolean.TRUE, converter);
        AtomicLong alFalse = BooleanConversions.toAtomicLong(Boolean.FALSE, converter);
        assertEquals(1L, alTrue.get());
        assertEquals(0L, alFalse.get());
    }

    // ----------------------------------------------------------------------
    // AtomicBoolean conversion
    // ----------------------------------------------------------------------
    @Test
    public void testToAtomicBoolean() {
        Converter converter = createConverter('T', 'F');
        AtomicBoolean abTrue = BooleanConversions.toAtomicBoolean(Boolean.TRUE, converter);
        AtomicBoolean abFalse = BooleanConversions.toAtomicBoolean(Boolean.FALSE, converter);
        assertTrue(abTrue.get());
        assertFalse(abFalse.get());
    }

    // ----------------------------------------------------------------------
    // Long conversion
    // ----------------------------------------------------------------------
    @Test
    public void testToLong() {
        Converter converter = createConverter('Y', 'N');
        assertEquals(Long.valueOf(1L), BooleanConversions.toLong(Boolean.TRUE, converter));
        assertEquals(Long.valueOf(0L), BooleanConversions.toLong(Boolean.FALSE, converter));
    }

    // ----------------------------------------------------------------------
    // BigDecimal conversion
    // ----------------------------------------------------------------------
    @Test
    public void testToBigDecimal() {
        Converter converter = createConverter('T', 'F');
        assertEquals(BigDecimal.ONE, BooleanConversions.toBigDecimal(Boolean.TRUE, converter));
        assertEquals(BigDecimal.ZERO, BooleanConversions.toBigDecimal(Boolean.FALSE, converter));
    }

    // ----------------------------------------------------------------------
    // BigInteger conversion
    // ----------------------------------------------------------------------
    @Test
    public void testToBigInteger() {
        Converter converter = createConverter('Y', 'N');
        assertEquals(BigInteger.ONE, BooleanConversions.toBigInteger(Boolean.TRUE, converter));
        assertEquals(BigInteger.ZERO, BooleanConversions.toBigInteger(Boolean.FALSE, converter));
    }

    // ----------------------------------------------------------------------
    // Float conversion
    // ----------------------------------------------------------------------
    @Test
    public void testToFloat() {
        Converter converter = createConverter('T', 'F');
        assertEquals(Float.valueOf(1.0f), BooleanConversions.toFloat(Boolean.TRUE, converter));
        assertEquals(Float.valueOf(0.0f), BooleanConversions.toFloat(Boolean.FALSE, converter));
    }

    // ----------------------------------------------------------------------
    // Double conversion
    // ----------------------------------------------------------------------
    @Test
    public void testToDouble() {
        Converter converter = createConverter('Y', 'N');
        assertEquals(Double.valueOf(1.0d), BooleanConversions.toDouble(Boolean.TRUE, converter));
        assertEquals(Double.valueOf(0.0d), BooleanConversions.toDouble(Boolean.FALSE, converter));
    }

    // ----------------------------------------------------------------------
    // Character conversion (uses ConverterOptions)
    // ----------------------------------------------------------------------
    @Test
    public void testToCharacter() {
        char trueChar = 'X';
        char falseChar = 'Z';
        Converter converter = createConverter(trueChar, falseChar);

        assertEquals(trueChar, BooleanConversions.toCharacter(Boolean.TRUE, converter));
        assertEquals(falseChar, BooleanConversions.toCharacter(Boolean.FALSE, converter));
    }

    // ----------------------------------------------------------------------
    // Null handling – all conversion methods should throw NullPointerException
    // ----------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testToByte_Null() {
        BooleanConversions.toByte(null, createConverter('T', 'F'));
    }

    @Test(expected = NullPointerException.class)
    public void testToShort_Null() {
        BooleanConversions.toShort(null, createConverter('T', 'F'));
    }

    @Test(expected = NullPointerException.class)
    public void testToInt_Null() {
        BooleanConversions.toInt(null, createConverter('T', 'F'));
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_Null() {
        BooleanConversions.toAtomicInteger(null, createConverter('T', 'F'));
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_Null() {
        BooleanConversions.toAtomicLong(null, createConverter('T', 'F'));
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBoolean_Null() {
        BooleanConversions.toAtomicBoolean(null, createConverter('T', 'F'));
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_Null() {
        BooleanConversions.toLong(null, createConverter('T', 'F'));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_Null() {
        BooleanConversions.toBigDecimal(null, createConverter('T', 'F'));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_Null() {
        BooleanConversions.toBigInteger(null, createConverter('T', 'F'));
    }

    @Test(expected = NullPointerException.class)
    public void testToFloat_Null() {
        BooleanConversions.toFloat(null, createConverter('T', 'F'));
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_Null() {
        BooleanConversions.toDouble(null, createConverter('T', 'F'));
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacter_Null() {
        BooleanConversions.toCharacter(null, createConverter('T', 'F'));
    }

    // ----------------------------------------------------------------------
    // Wrong type handling – ClassCastException should be thrown
    // ----------------------------------------------------------------------
    @Test(expected = ClassCastException.class)
    public void testToByte_WrongType() {
        BooleanConversions.toByte("not a boolean", createConverter('T', 'F'));
    }

    @Test(expected = ClassCastException.class)
    public void testToCharacter_WrongType() {
        BooleanConversions.toCharacter(Integer.valueOf(1), createConverter('T', 'F'));
    }
}
