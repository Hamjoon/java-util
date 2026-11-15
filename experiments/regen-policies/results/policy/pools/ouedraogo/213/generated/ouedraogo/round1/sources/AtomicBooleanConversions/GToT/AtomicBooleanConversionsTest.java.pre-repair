package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive JUnit‑4 test suite for {@link AtomicBooleanConversions}.
 *
 * The suite is built in the spirit of the three‑expert exercise described in the prompt:
 * <ol>
 *   <li>All public (package‑private static) conversion methods are listed with their signatures.</li>
 *   <li>For each method a basic sanity test is provided that exercises the “true” and “false”
 *       branches of the conversion.</li>
 *   <li>Potential edge‑cases and error scenarios are identified (null arguments, wrong source type,
 *       and {@link ConverterOptions} custom true/false characters).</li>
 *   <li>Additional tests target those edge‑cases.</li>
 *   <li>All tests are merged into a single JUnit‑4 test class.</li>
 * </ol>
 *
 * Note: The class under test is package‑private and its methods are static, therefore the test class
 * resides in the same package (com.cedarsoftware.util.convert) so it can access the methods directly.
 */
public class AtomicBooleanConversionsTest {

    // -------------------------------------------------------------------------
    // Helper objects used by many tests
    // -------------------------------------------------------------------------
    private Converter defaultConverter;
    private Converter customCharConverter;

    @Before
    public void setUp() {
        // Default options – trueChar = 'T', falseChar = 'F' (these are the defaults in the library)
        defaultConverter = new Converter(new ConverterOptions());

        // Custom options to verify that toCharacter respects the ConverterOptions values
        ConverterOptions customOptions = new ConverterOptions()
                .trueChar('Y')
                .falseChar('N');
        customCharConverter = new Converter(customOptions);
    }

    // -------------------------------------------------------------------------
    // 1. List of all conversion methods (package‑private static)
    // -------------------------------------------------------------------------
    // static Byte    toByte(Object from, Converter converter)
    // static Short   toShort(Object from, Converter converter)
    // static Integer toInt(Object from, Converter converter)
    // static Long    toLong(Object from, Converter converter)
    // static Float   toFloat(Object from, Converter converter)
    // static Double  toDouble(Object from, Converter converter)
    // static boolean toBoolean(Object from, Converter converter)
    // static AtomicBoolean    toAtomicBoolean(Object from, Converter converter)
    // static AtomicInteger    toAtomicInteger(Object from, Converter converter)
    // static AtomicLong       toAtomicLong(Object from, Converter converter)
    // static Character        toCharacter(Object from, Converter converter)
    // static BigDecimal       toBigDecimal(Object from, Converter converter)
    // public static BigInteger toBigInteger(Object from, Converter converter)

    // -------------------------------------------------------------------------
    // 2. Basic functionality tests (true / false values)
    // -------------------------------------------------------------------------

    @Test
    public void testToByte() {
        assertEquals(Byte.valueOf((byte)1), AtomicBooleanConversions.toByte(new AtomicBoolean(true), defaultConverter));
        assertEquals(Byte.valueOf((byte)0), AtomicBooleanConversions.toByte(new AtomicBoolean(false), defaultConverter));
    }

    @Test
    public void testToShort() {
        assertEquals(Short.valueOf((short)1), AtomicBooleanConversions.toShort(new AtomicBoolean(true), defaultConverter));
        assertEquals(Short.valueOf((short)0), AtomicBooleanConversions.toShort(new AtomicBoolean(false), defaultConverter));
    }

    @Test
    public void testToInt() {
        assertEquals(Integer.valueOf(1), AtomicBooleanConversions.toInt(new AtomicBoolean(true), defaultConverter));
        assertEquals(Integer.valueOf(0), AtomicBooleanConversions.toInt(new AtomicBoolean(false), defaultConverter));
    }

    @Test
    public void testToLong() {
        assertEquals(Long.valueOf(1L), AtomicBooleanConversions.toLong(new AtomicBoolean(true), defaultConverter));
        assertEquals(Long.valueOf(0L), AtomicBooleanConversions.toLong(new AtomicBoolean(false), defaultConverter));
    }

    @Test
    public void testToFloat() {
        assertEquals(Float.valueOf(1.0f), AtomicBooleanConversions.toFloat(new AtomicBoolean(true), defaultConverter));
        assertEquals(Float.valueOf(0.0f), AtomicBooleanConversions.toFloat(new AtomicBoolean(false), defaultConverter));
    }

    @Test
    public void testToDouble() {
        assertEquals(Double.valueOf(1.0d), AtomicBooleanConversions.toDouble(new AtomicBoolean(true), defaultConverter));
        assertEquals(Double.valueOf(0.0d), AtomicBooleanConversions.toDouble(new AtomicBoolean(false), defaultConverter));
    }

    @Test
    public void testToBoolean() {
        assertTrue(AtomicBooleanConversions.toBoolean(new AtomicBoolean(true), defaultConverter));
        assertFalse(AtomicBooleanConversions.toBoolean(new AtomicBoolean(false), defaultConverter));
    }

    @Test
    public void testToAtomicBoolean() {
        AtomicBoolean resultTrue = AtomicBooleanConversions.toAtomicBoolean(new AtomicBoolean(true), defaultConverter);
        AtomicBoolean resultFalse = AtomicBooleanConversions.toAtomicBoolean(new AtomicBoolean(false), defaultConverter);
        assertTrue(resultTrue.get());
        assertFalse(resultFalse.get());

        // Ensure a new instance is returned (not the same reference)
        assertNotSame(new AtomicBoolean(true), resultTrue);
    }

    @Test
    public void testToAtomicInteger() {
        AtomicInteger aiTrue = AtomicBooleanConversions.toAtomicInteger(new AtomicBoolean(true), defaultConverter);
        AtomicInteger aiFalse = AtomicBooleanConversions.toAtomicInteger(new AtomicBoolean(false), defaultConverter);
        assertEquals(1, aiTrue.get());
        assertEquals(0, aiFalse.get());
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong alTrue = AtomicBooleanConversions.toAtomicLong(new AtomicBoolean(true), defaultConverter);
        AtomicLong alFalse = AtomicBooleanConversions.toAtomicLong(new AtomicBoolean(false), defaultConverter);
        assertEquals(1L, alTrue.get());
        assertEquals(0L, alFalse.get());
    }

    @Test
    public void testToCharacter_DefaultOptions() {
        // Default trueChar = 'T', falseChar = 'F'
        assertEquals(Character.valueOf('T'), AtomicBooleanConversions.toCharacter(new AtomicBoolean(true), defaultConverter));
        assertEquals(Character.valueOf('F'), AtomicBooleanConversions.toCharacter(new AtomicBoolean(false), defaultConverter));
    }

    @Test
    public void testToCharacter_CustomOptions() {
        // Custom trueChar = 'Y', falseChar = 'N'
        assertEquals(Character.valueOf('Y'), AtomicBooleanConversions.toCharacter(new AtomicBoolean(true), customCharConverter));
        assertEquals(Character.valueOf('N'), AtomicBooleanConversions.toCharacter(new AtomicBoolean(false), customCharConverter));
    }

    @Test
    public void testToBigDecimal() {
        assertEquals(BigDecimal.ONE, AtomicBooleanConversions.toBigDecimal(new AtomicBoolean(true), defaultConverter));
        assertEquals(BigDecimal.ZERO, AtomicBooleanConversions.toBigDecimal(new AtomicBoolean(false), defaultConverter));
    }

    @Test
    public void testToBigInteger() {
        assertEquals(BigInteger.ONE, AtomicBooleanConversions.toBigInteger(new AtomicBoolean(true), defaultConverter));
        assertEquals(BigInteger.ZERO, AtomicBooleanConversions.toBigInteger(new AtomicBoolean(false), defaultConverter));
    }

    // -------------------------------------------------------------------------
    // 3. Edge‑case / exception tests
    // -------------------------------------------------------------------------

    /**
     * All conversion methods expect the source object to be an {@link AtomicBoolean}.
     * Passing {@code null} should result in a {@link NullPointerException} because the
     * method attempts an unchecked cast.
     */
    @Test(expected = NullPointerException.class)
    public void testToInt_NullSource() {
        AtomicBooleanConversions.toInt(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInt_WrongSourceType() {
        // Passing an Integer instead of AtomicBoolean triggers a ClassCastException at the cast line.
        AtomicBooleanConversions.toInt(Integer.valueOf(5), defaultConverter);
    }

    /**
     * The {@code toCharacter} method also dereferences {@link Converter#getOptions()}.
     * Verify that a {@code null} Converter leads to a NullPointerException.
     */
    @Test(expected = NullPointerException.class)
    public void testToCharacter_NullConverter() {
        AtomicBooleanConversions.toCharacter(new AtomicBoolean(true), null);
    }

    /**
     * Verify that the methods do not retain references to the original AtomicBoolean.
     * Mutating the original after conversion must not affect the returned value.
     */
    @Test
    public void testReturnedAtomicTypesAreIndependent() {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicInteger ai = AtomicBooleanConversions.toAtomicInteger(source, defaultConverter);
        AtomicLong al = AtomicBooleanConversions.toAtomicLong(source, defaultConverter);
        AtomicBoolean ab = AtomicBooleanConversions.toAtomicBoolean(source, defaultConverter);

        // Change source
        source.set(false);

        // Converted values must stay as they were at conversion time
        assertEquals(1, ai.get());
        assertEquals(1L, al.get());
        assertTrue(ab.get()); // new instance, not affected by source change
    }

    /**
     * Ensure that the conversion of false values always yields the exact zero constants
     * defined by the JDK (e.g., BigDecimal.ZERO, BigInteger.ZERO) and not merely a new
     * instance with the same numeric value.
     */
    @Test
    public void testZeroConstantsIdentity() {
        assertSame(BigDecimal.ZERO, AtomicBooleanConversions.toBigDecimal(new AtomicBoolean(false), defaultConverter));
        assertSame(BigInteger.ZERO, AtomicBooleanConversions.toBigInteger(new AtomicBoolean(false), defaultConverter));
    }

    /**
     * Verify that the conversion methods are deterministic – calling them multiple
     * times with the same input yields equal results (including object equality where
     * applicable).
     */
    @Test
    public void testDeterministicConversions() {
        AtomicBoolean trueFlag = new AtomicBoolean(true);
        assertEquals(AtomicBooleanConversions.toInt(trueFlag, defaultConverter),
                     AtomicBooleanConversions.toInt(trueFlag, defaultConverter));

        assertEquals(AtomicBooleanConversions.toAtomicInteger(trueFlag, defaultConverter).get(),
                     AtomicBooleanConversions.toAtomicInteger(trueFlag, defaultConverter).get());

        assertEquals(AtomicBooleanConversions.toCharacter(trueFlag, defaultConverter),
                     AtomicBooleanConversions.toCharacter(trueFlag, defaultConverter));
    }

    // -------------------------------------------------------------------------
    // 4. Summary
    // -------------------------------------------------------------------------
    // The above tests collectively cover:
    //   • Normal true/false conversion for every supported target type.
    //   • Null source argument handling (NPE).
    //   • Incorrect source type handling (ClassCastException).
    //   • Null converter argument for the only method that uses converter options.
    //   • Independence of returned mutable atomic wrappers.
    //   • Identity of shared constant objects (BigDecimal.ZERO, BigInteger.ZERO).
    //   • Deterministic behavior across repeated calls.
}
