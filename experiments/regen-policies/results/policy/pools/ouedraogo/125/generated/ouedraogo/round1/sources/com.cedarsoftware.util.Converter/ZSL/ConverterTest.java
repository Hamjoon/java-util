package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.CommonValues;
import com.cedarsoftware.util.convert.Convert;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import com.cedarsoftware.util.convert.Converter;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unit tests for {@link Converter} static façade.
 * The tests cover:
 * <ul>
 *   <li>Basic primitive and wrapper conversions</li>
 *   <li>Null handling for primitive‑specific helper methods</li>
 *   <li>String conversion helpers</li>
 *   <li>Support‑query methods (isConversionSupportedFor, isDirectConversionSupported, isSimpleTypeConversionSupported)</li>
 *   <li>Custom conversion registration via {@code addConversion}</li>
 * </ul>
 */
public class ConverterTest {

    /** Simple conversion from String to Integer using the main {@code convert} API. */
    @Test
    public void testConvertStringToInteger() {
        Integer result = Converter.convert("42", Integer.class);
        Assert.assertEquals(Integer.valueOf(42), result);
    }

    /** Null handling for object return types – should return null. */
    @Test
    public void testConvertNullToObject() {
        String s = Converter.convert(null, String.class);
        Assert.assertNull(s);
    }

    /** Null handling for primitive return types – should return default primitive values. */
    @Test
    public void testConvertNullToPrimitives() {
        Assert.assertEquals(0, (int) Converter.convert(null, int.class));
        Assert.assertEquals(0L, (long) Converter.convert(null, long.class));
        Assert.assertEquals(0.0f, (float) Converter.convert(null, float.class), 0.0f);
        Assert.assertEquals(0.0d, (double) Converter.convert(null, double.class), 0.0d);
        Assert.assertEquals((byte) 0, (byte) Converter.convert(null, byte.class));
        Assert.assertEquals((short) 0, (short) Converter.convert(null, short.class));
        Assert.assertEquals(false, (boolean) Converter.convert(null, boolean.class));
        Assert.assertEquals('\u0000', (char) Converter.convert(null, char.class));
    }

    /** Helper method convert2String – null input should yield empty string. */
    @Test
    public void testConvert2StringNull() {
        Assert.assertEquals("", Converter.convert2String(null));
    }

    /** Helper method convert2String – non‑null input should be converted to its string form. */
    @Test
    public void testConvert2StringNonNull() {
        Assert.assertEquals("123", Converter.convert2String(123));
    }

    /** Helper method convertToString – null input should yield null (different contract from convert2String). */
    @Test
    public void testConvertToStringNull() {
        Assert.assertNull(Converter.convertToString(null));
    }

    /** Helper method convert2BigDecimal – null input yields BigDecimal.ZERO. */
    @Test
    public void testConvert2BigDecimalNull() {
        Assert.assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(null));
    }

    /** Helper method convert2BigDecimal – numeric string conversion. */
    @Test
    public void testConvert2BigDecimalFromString() {
        BigDecimal bd = Converter.convert2BigDecimal("12.34");
        Assert.assertEquals(new BigDecimal("12.34"), bd);
    }

    /** Helper method convert2BigInteger – null input yields BigInteger.ZERO. */
    @Test
    public void testConvert2BigIntegerNull() {
        Assert.assertEquals(BigInteger.ZERO, Converter.convert2BigInteger(null));
    }

    /** Helper method convert2BigInteger – numeric string conversion. */
    @Test
    public void testConvert2BigIntegerFromString() {
        BigInteger bi = Converter.convert2BigInteger("12345");
        Assert.assertEquals(new BigInteger("12345"), bi);
    }

    /** Primitive‑specific conversion helpers – ensure they return defaults on null. */
    @Test
    public void testPrimitiveHelpersNullDefaults() {
        Assert.assertEquals(0, Converter.convert2int(null));
        Assert.assertEquals(0L, Converter.convert2long(null));
        Assert.assertEquals(0.0f, Converter.convert2float(null), 0.0f);
        Assert.assertEquals(0.0d, Converter.convert2double(null), 0.0d);
        Assert.assertEquals((short) 0, Converter.convert2short(null));
        Assert.assertEquals((byte) 0, Converter.convert2byte(null));
        Assert.assertEquals((char) 0, Converter.convert2char(null));
        Assert.assertEquals(false, Converter.convert2boolean(null));
    }

    /** Primitive‑specific conversion helpers – actual conversion from String. */
    @Test
    public void testPrimitiveHelpersConversion() {
        Assert.assertEquals(7, Converter.convert2int("7"));
        Assert.assertEquals(7L, Converter.convert2long("7"));
        Assert.assertEquals(7.5f, Converter.convert2float("7.5"), 0.0f);
        Assert.assertEquals(7.5d, Converter.convert2double("7.5"), 0.0d);
        Assert.assertEquals((short) 9, Converter.convert2short("9"));
        Assert.assertEquals((byte) 4, Converter.convert2byte("4"));
        Assert.assertEquals('A', Converter.convert2char("A"));
        Assert.assertEquals(true, Converter.convert2boolean("true"));
    }

    /** Atomic helper methods – null yields freshly created atomic with zero/false. */
    @Test
    public void testAtomicHelpersNull() {
        Assert.assertEquals(0, Converter.convert2AtomicInteger(null).get());
        Assert.assertEquals(0L, Converter.convert2AtomicLong(null).get());
        Assert.assertEquals(false, Converter.convert2AtomicBoolean(null).get());
    }

    /** Atomic helper methods – conversion from numeric/string values. */
    @Test
    public void testAtomicHelpersConversion() {
        Assert.assertEquals(5, Converter.convert2AtomicInteger("5").get());
        Assert.assertEquals(12L, Converter.convert2AtomicLong("12").get());
        Assert.assertEquals(true, Converter.convert2AtomicBoolean("true").get());
    }

    /** Verify that isConversionSupportedFor correctly reports known simple conversions. */
    @Test
    public void testIsConversionSupportedFor() {
        Assert.assertTrue(Converter.isConversionSupportedFor(String.class, Integer.class));
        Assert.assertTrue(Converter.isConversionSupportedFor(Integer.class, String.class));
        // Unsupported conversion (e.g., String to UUID without a map) – should be false
        Assert.assertFalse(Converter.isConversionSupportedFor(String.class, UUID.class));
    }

    /** Verify that isDirectConversionSupported matches simple direct conversions and rejects unknown ones. */
    @Test
    public void testIsDirectConversionSupported() {
        Assert.assertTrue(Converter.isDirectConversionSupported(String.class, Integer.class));
        Assert.assertFalse(Converter.isDirectConversionSupported(String.class, UUID.class));
    }

    /** Verify that isSimpleTypeConversionSupported returns true for non‑collection simple types. */
    @Test
    public void testIsSimpleTypeConversionSupported() {
        Assert.assertTrue(Converter.isSimpleTypeConversionSupported(String.class, Double.class));
        Assert.assertFalse(Converter.isSimpleTypeConversionSupported(List.class, Set.class));
        Assert.assertFalse(Converter.isSimpleTypeConversionSupported(String[].class, Integer[].class));
    }

    /** Register a custom conversion and verify it is used. */
    @Test
    public void testAddCustomConversion() {
        // Custom conversion: from String to UUID by parsing the string
        Convert<UUID> stringToUuid = (from, conv) -> UUID.fromString((String) from);
        Converter.addConversion(String.class, UUID.class, stringToUuid);

        UUID expected = UUID.nameUUIDFromBytes("test".getBytes());
        UUID actual = Converter.convert(expected.toString(), UUID.class);
        Assert.assertEquals(expected, actual);
    }

    /** Ensure that after adding a custom conversion, isConversionSupportedFor reflects the new capability. */
    @Test
    public void testIsConversionSupportedAfterCustom() {
        Convert<Integer> doubleToInt = (from, conv) -> ((Double) from).intValue();
        Converter.addConversion(Double.class, Integer.class, doubleToInt);

        Assert.assertTrue(Converter.isConversionSupportedFor(Double.class, Integer.class));
        Integer result = Converter.convert(12.7, Integer.class);
        Assert.assertEquals(Integer.valueOf(12), result);
    }

    /** Test conversion of a Map to a known supported type (BigDecimal) using the “value” key convention. */
    @Test
    public void testMapToBigDecimal() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "123.45");
        BigDecimal bd = Converter.convert(map, BigDecimal.class);
        Assert.assertEquals(new BigDecimal("123.45"), bd);
    }

    /** Test that converting a Date to a LocalDate works (date -> epoch millis -> LocalDate). */
    @Test
    public void testDateToLocalDate() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2020, Calendar.JANUARY, 15, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        LocalDate localDate = Converter.convertToLocalDate(date);
        Assert.assertEquals(LocalDate.of(2020, 1, 15), localDate);
    }

    /** Test that converting a LocalDate to a long (epoch millis) works via the deprecated helper. */
    @Test
    public void testLocalDateToMillisDeprecated() {
        LocalDate ld = LocalDate.of(1970, 1, 2); // one day after epoch
        long millis = Converter.localDateToMillis(ld);
        Assert.assertEquals(24L * 60 * 60 * 1000, millis);
    }
}
