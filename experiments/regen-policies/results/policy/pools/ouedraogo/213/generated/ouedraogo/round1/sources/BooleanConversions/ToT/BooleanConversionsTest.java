package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JUnit 4 test suite for {@link BooleanConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical conversions for {@code true} and {@code false} values.</li>
 *   <li>Edge cases such as {@code null} input (expected {@link NullPointerException}).</li>
 *   <li>Invalid input types (expected {@link ClassCastException}).</li>
 *   <li>Special handling of {@code toCharacter} which depends on {@link ConverterOptions}.</li>
 * </ul>
 *
 * No mocking frameworks are used – all objects are created via their public constructors
 * as required by the problem statement.
 */
public class BooleanConversionsTest {

    /** Helper to create a {@link Converter} with default {@link ConverterOptions}. */
    private static Converter createDefaultConverter() {
        // The library provides a public constructor Converter(ConverterOptions options).
        // ConverterOptions is assumed to have a public no‑arg constructor.
        return new Converter(new ConverterOptions());
    }

    /* ---------------------------------------------------------------------- */
    /*  Boolean -> Byte                                                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToByte() {
        Converter converter = createDefaultConverter();

        // true -> 1
        assertEquals(Byte.valueOf((byte)1), BooleanConversions.toByte(Boolean.TRUE, converter));

        // false -> 0
        assertEquals(Byte.valueOf((byte)0), BooleanConversions.toByte(Boolean.FALSE, converter));

        // null input should throw NPE
        try {
            BooleanConversions.toByte(null, converter);
            fail("Expected NullPointerException for null input");
        } catch (NullPointerException expected) { /* ok */ }

        // non‑Boolean input should throw ClassCastException
        try {
            BooleanConversions.toByte("not a boolean", converter);
            fail("Expected ClassCastException for non‑Boolean input");
        } catch (ClassCastException expected) { /* ok */ }
    }

    /* ---------------------------------------------------------------------- */
    /*  Boolean -> Short                                                      */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToShort() {
        Converter converter = createDefaultConverter();

        assertEquals(Short.valueOf((short)1), BooleanConversions.toShort(Boolean.TRUE, converter));
        assertEquals(Short.valueOf((short)0), BooleanConversions.toShort(Boolean.FALSE, converter));

        try {
            BooleanConversions.toShort(null, converter);
            fail("Expected NullPointerException for null input");
        } catch (NullPointerException ignored) {}

        try {
            BooleanConversions.toShort(123, converter);
            fail("Expected ClassCastException for non‑Boolean input");
        } catch (ClassCastException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  Boolean -> Integer                                                    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToInt() {
        Converter converter = createDefaultConverter();

        assertEquals(Integer.valueOf(1), BooleanConversions.toInt(Boolean.TRUE, converter));
        assertEquals(Integer.valueOf(0), BooleanConversions.toInt(Boolean.FALSE, converter));

        try {
            BooleanConversions.toInt(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {}

        try {
            BooleanConversions.toInt(new Object(), converter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  Boolean -> AtomicInteger                                              */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToAtomicInteger() {
        Converter converter = createDefaultConverter();

        assertEquals(new AtomicInteger(1), BooleanConversions.toAtomicInteger(Boolean.TRUE, converter));
        assertEquals(new AtomicInteger(0), BooleanConversions.toAtomicInteger(Boolean.FALSE, converter));

        try {
            BooleanConversions.toAtomicInteger(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {}

        try {
            BooleanConversions.toAtomicInteger(Boolean.TRUE.toString(), converter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  Boolean -> AtomicLong                                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToAtomicLong() {
        Converter converter = createDefaultConverter();

        assertEquals(new AtomicLong(1L), BooleanConversions.toAtomicLong(Boolean.TRUE, converter));
        assertEquals(new AtomicLong(0L), BooleanConversions.toAtomicLong(Boolean.FALSE, converter));

        try {
            BooleanConversions.toAtomicLong(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {}

        try {
            BooleanConversions.toAtomicLong(Boolean.FALSE, null);
            // The method does not dereference the converter for this conversion,
            // so a null converter is allowed – we just verify the value.
            // No exception expected here.
        } catch (Exception e) {
            fail("Did not expect exception when converter is null: " + e);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Boolean -> AtomicBoolean                                              */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToAtomicBoolean() {
        Converter converter = createDefaultConverter();

        assertEquals(new AtomicBoolean(true), BooleanConversions.toAtomicBoolean(Boolean.TRUE, converter));
        assertEquals(new AtomicBoolean(false), BooleanConversions.toAtomicBoolean(Boolean.FALSE, converter));

        try {
            BooleanConversions.toAtomicBoolean(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {}

        try {
            BooleanConversions.toAtomicBoolean(0, converter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  Boolean -> Long                                                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToLong() {
        Converter converter = createDefaultConverter();

        assertEquals(Long.valueOf(1L), BooleanConversions.toLong(Boolean.TRUE, converter));
        assertEquals(Long.valueOf(0L), BooleanConversions.toLong(Boolean.FALSE, converter));

        try {
            BooleanConversions.toLong(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {}

        try {
            BooleanConversions.toLong(new Object(), converter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  Boolean -> BigDecimal                                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToBigDecimal() {
        Converter converter = createDefaultConverter();

        assertEquals(BigDecimal.ONE, BooleanConversions.toBigDecimal(Boolean.TRUE, converter));
        assertEquals(BigDecimal.ZERO, BooleanConversions.toBigDecimal(Boolean.FALSE, converter));

        try {
            BooleanConversions.toBigDecimal(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {}

        try {
            BooleanConversions.toBigDecimal(Boolean.TRUE.toString(), converter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  Boolean -> BigInteger                                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToBigInteger() {
        Converter converter = createDefaultConverter();

        assertEquals(BigInteger.ONE, BooleanConversions.toBigInteger(Boolean.TRUE, converter));
        assertEquals(BigInteger.ZERO, BooleanConversions.toBigInteger(Boolean.FALSE, converter));

        try {
            BooleanConversions.toBigInteger(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {}

        try {
            BooleanConversions.toBigInteger(Boolean.FALSE, null);
            // The method does not use the converter, so null is acceptable.
        } catch (Exception e) {
            fail("Did not expect exception when converter is null: " + e);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Boolean -> Float                                                      */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToFloat() {
        Converter converter = createDefaultConverter();

        assertEquals(Float.valueOf(1.0f), BooleanConversions.toFloat(Boolean.TRUE, converter));
        assertEquals(Float.valueOf(0.0f), BooleanConversions.toFloat(Boolean.FALSE, converter));

        try {
            BooleanConversions.toFloat(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {}

        try {
            BooleanConversions.toFloat(Boolean.TRUE, null);
            // No use of converter inside, so null is fine.
        } catch (Exception e) {
            fail("Unexpected exception with null converter: " + e);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Boolean -> Double                                                     */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToDouble() {
        Converter converter = createDefaultConverter();

        assertEquals(Double.valueOf(1.0d), BooleanConversions.toDouble(Boolean.TRUE, converter));
        assertEquals(Double.valueOf(0.0d), BooleanConversions.toDouble(Boolean.FALSE, converter));

        try {
            BooleanConversions.toDouble(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {}

        try {
            BooleanConversions.toDouble(Integer.valueOf(1), converter);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*  Boolean -> Character                                                  */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToCharacter() {
        // Custom options to verify that the conversion respects the configured chars.
        ConverterOptions customOptions = new ConverterOptions()
                .withTrueChar('Y')
                .withFalseChar('N');
        Converter customConverter = new Converter(customOptions);

        assertEquals('Y', BooleanConversions.toCharacter(Boolean.TRUE, customConverter));
        assertEquals('N', BooleanConversions.toCharacter(Boolean.FALSE, customConverter));

        // Default options (the exact default chars are implementation‑specific;
        // we verify that they are consistent with the options object).
        Converter defaultConverter = createDefaultConverter();
        ConverterOptions defaults = defaultConverter.getOptions();
        assertEquals(defaults.trueChar(), BooleanConversions.toCharacter(Boolean.TRUE, defaultConverter));
        assertEquals(defaults.falseChar(), BooleanConversions.toCharacter(Boolean.FALSE, defaultConverter));

        try {
            BooleanConversions.toCharacter(null, defaultConverter);
            fail("Expected NullPointerException for null input");
        } catch (NullPointerException ignored) {}

        try {
            BooleanConversions.toCharacter("true", defaultConverter);
            fail("Expected ClassCastException for non‑Boolean input");
        } catch (ClassCastException ignored) {}
    }
}
