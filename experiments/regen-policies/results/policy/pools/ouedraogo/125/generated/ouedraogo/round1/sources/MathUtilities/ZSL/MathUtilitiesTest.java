package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class MathUtilitiesTest {

    /*--------------------------------------------------------------------
     *  Primitive long minimum / maximum
     *--------------------------------------------------------------------*/

    @Test
    public void testLongMinimumSimple() {
        assertEquals(1L, MathUtilities.minimum(5L, 3L, 1L, 9L));
    }

    @Test
    public void testLongMaximumSimple() {
        assertEquals(9L, MathUtilities.maximum(5L, 3L, 1L, 9L));
    }

    @Test
    public void testLongMinimumWithNegativesAndZero() {
        assertEquals(-10L, MathUtilities.minimum(-10L, 0L, 5L, -3L));
    }

    @Test
    public void testLongMaximumWithNegativesAndZero() {
        assertEquals(7L, MathUtilities.maximum(-10L, 0L, 5L, 7L));
    }

    @Test
    public void testLongMinimumSingleElement() {
        assertEquals(Long.MAX_VALUE, MathUtilities.minimum(Long.MAX_VALUE));
    }

    @Test
    public void testLongMaximumSingleElement() {
        assertEquals(Long.MIN_VALUE, MathUtilities.maximum(Long.MIN_VALUE));
    }

    /*--------------------------------------------------------------------
     *  Primitive double minimum / maximum
     *--------------------------------------------------------------------*/

    @Test
    public void testDoubleMinimumSimple() {
        assertEquals(1.1, MathUtilities.minimum(5.5, 3.3, 1.1, 9.9), 0.0);
    }

    @Test
    public void testDoubleMaximumSimple() {
        assertEquals(9.9, MathUtilities.maximum(5.5, 3.3, 1.1, 9.9), 0.0);
    }

    @Test
    public void testDoubleMinimumWithNegativesAndNaN() {
        // NaN should be ignored because Math.min treats NaN as the other value
        assertEquals(-10.0, MathUtilities.minimum(-10.0, Double.NaN, 5.0), 0.0);
    }

    @Test
    public void testDoubleMaximumWithNegativesAndNaN() {
        assertEquals(5.0, MathUtilities.maximum(-10.0, Double.NaN, 5.0), 0.0);
    }

    @Test
    public void testDoubleMinimumSingleElement() {
        assertEquals(Double.MIN_VALUE, MathUtilities.minimum(Double.MIN_VALUE), 0.0);
    }

    @Test
    public void testDoubleMaximumSingleElement() {
        assertEquals(Double.MAX_VALUE, MathUtilities.maximum(Double.MAX_VALUE), 0.0);
    }

    /*--------------------------------------------------------------------
     *  BigInteger minimum / maximum
     *--------------------------------------------------------------------*/

    @Test
    public void testBigIntegerMinimumSimple() {
        BigInteger a = new BigInteger("12345678901234567890");
        BigInteger b = new BigInteger("-12345678901234567890");
        BigInteger c = BigInteger.ZERO;
        assertEquals(b, MathUtilities.minimum(a, b, c));
    }

    @Test
    public void testBigIntegerMaximumSimple() {
        BigInteger a = new BigInteger("12345678901234567890");
        BigInteger b = new BigInteger("-12345678901234567890");
        BigInteger c = BigInteger.ZERO;
        assertEquals(a, MathUtilities.maximum(a, b, c));
    }

    @Test
    public void testBigIntegerSingleElement() {
        BigInteger val = new BigInteger("42");
        assertSame(val, MathUtilities.minimum(val));
        assertSame(val, MathUtilities.maximum(val));
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testBigIntegerMinimumNullEntryThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot passed null BigInteger entry to minimum()");
        MathUtilities.minimum(BigInteger.ONE, null, BigInteger.TEN);
    }

    @Test
    public void testBigIntegerMaximumNullEntryThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot passed null BigInteger entry to maximum()");
        MathUtilities.maximum(null, BigInteger.ONE);
    }

    /*--------------------------------------------------------------------
     *  BigDecimal minimum / maximum
     *--------------------------------------------------------------------*/

    @Test
    public void testBigDecimalMinimumSimple() {
        BigDecimal a = new BigDecimal("1.0001");
        BigDecimal b = new BigDecimal("-2.5");
        BigDecimal c = new BigDecimal("0");
        assertEquals(b, MathUtilities.minimum(a, b, c));
    }

    @Test
    public void testBigDecimalMaximumSimple() {
        BigDecimal a = new BigDecimal("1.0001");
        BigDecimal b = new BigDecimal("-2.5");
        BigDecimal c = new BigDecimal("0");
        assertEquals(a, MathUtilities.maximum(a, b, c));
    }

    @Test
    public void testBigDecimalSingleElement() {
        BigDecimal val = new BigDecimal("3.14159");
        assertSame(val, MathUtilities.minimum(val));
        assertSame(val, MathUtilities.maximum(val));
    }

    @Test
    public void testBigDecimalMinimumNullEntryThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot passed null BigDecimal entry to minimum()");
        MathUtilities.minimum(new BigDecimal("1"), null);
    }

    @Test
    public void testBigDecimalMaximumNullEntryThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot pass null BigDecimal entry to maximum()");
        MathUtilities.maximum(null, new BigDecimal("2"));
    }

    /*--------------------------------------------------------------------
     *  parseToMinimalNumericType
     *--------------------------------------------------------------------*/

    @Test
    public void testParseLongWithinRange() {
        Number n = MathUtilities.parseToMinimalNumericType("12345");
        assertTrue(n instanceof Long);
        assertEquals(12345L, n);
    }

    @Test
    public void testParseLongNegative() {
        Number n = MathUtilities.parseToMinimalNumericType("-9876543210");
        assertTrue(n instanceof Long);
        assertEquals(-9876543210L, n);
    }

    @Test
    public void testParseBigIntegerBeyondLong() {
        String big = "9223372036854775808"; // Long.MAX_VALUE + 1
        Number n = MathUtilities.parseToMinimalNumericType(big);
        assertTrue(n instanceof BigInteger);
        assertEquals(new BigInteger(big), n);
    }

    @Test
    public void testParseDoubleSimple() {
        Number n = MathUtilities.parseToMinimalNumericType("3.14");
        assertTrue(n instanceof Double);
        assertEquals(3.14, (Double) n, 0.0);
    }

    @Test
    public void testParseDoubleWithExponentWithinDoubleRange() {
        Number n = MathUtilities.parseToMinimalNumericType("1.23e2");
        assertTrue(n instanceof Double);
        assertEquals(123.0, (Double) n, 0.0);
    }

    @Test
    public void testParseBigDecimalWhenPrecisionExceedsDouble() {
        // mantissa > 16 digits forces BigDecimal
        String val = "12345678901234567"; // 17 digits, no exponent
        Number n = MathUtilities.parseToMinimalNumericType(val);
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal(val), n);
    }

    @Test
    public void testParseBigDecimalWhenExponentTooLarge() {
        // exponent >= 308 forces BigDecimal
        String val = "1e309";
        Number n = MathUtilities.parseToMinimalNumericType(val);
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal(val), n);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseInvalidNumberThrows() {
        MathUtilities.parseToMinimalNumericType("not-a-number");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseNullThrows() {
        MathUtilities.parseToMinimalNumericType(null);
    }

    /*--------------------------------------------------------------------
     *  nextPermutation
     *--------------------------------------------------------------------*/

    @Test
    public void testNextPermutationBasic() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<List<Integer>> expected = Arrays.asList(
                Arrays.asList(1, 2, 3),
                Arrays.asList(1, 3, 2),
                Arrays.asList(2, 1, 3),
                Arrays.asList(2, 3, 1),
                Arrays.asList(3, 1, 2),
                Arrays.asList(3, 2, 1)
        );

        List<List<Integer>> actual = new ArrayList<>();
        do {
            actual.add(new ArrayList<>(list));
        } while (MathUtilities.nextPermutation(list));

        assertEquals(expected, actual);
    }

    @Test
    public void testNextPermutationSingleElement() {
        List<String> list = new ArrayList<>(Arrays.asList("only"));
        assertFalse(MathUtilities.nextPermutation(list));
        assertEquals(Arrays.asList("only"), list);
    }

    @Test
    public void testNextPermutationAlreadyHighest() {
        List<Integer> list = new ArrayList<>(Arrays.asList(3, 2, 1));
        assertFalse(MathUtilities.nextPermutation(list));
        // list should remain unchanged
        assertEquals(Arrays.asList(3, 2, 1), list);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNextPermutationNullListThrows() {
        MathUtilities.nextPermutation(null);
    }

    /*--------------------------------------------------------------------
     *  Additional edge cases for nextPermutation with duplicate values
     *--------------------------------------------------------------------*/

    @Test
    public void testNextPermutationWithDuplicates() {
        // Duplicates are allowed; algorithm still produces lexicographic order
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 1, 2));
        List<List<Integer>> expected = Arrays.asList(
                Arrays.asList(1, 1, 2),
                Arrays.asList(1, 2, 1),
                Arrays.asList(2, 1, 1)
        );

        List<List<Integer>> actual = new ArrayList<>();
        do {
            actual.add(new ArrayList<>(list));
        } while (MathUtilities.nextPermutation(list));

        assertEquals(expected, actual);
    }
}
