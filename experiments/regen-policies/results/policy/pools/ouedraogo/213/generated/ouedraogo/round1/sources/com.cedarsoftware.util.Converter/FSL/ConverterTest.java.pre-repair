package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.convert.Convert;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link Converter}.
 *
 * The tests cover:
 * <ul>
 *   <li>Null handling for all primitive‑wrapper conversion helpers</li>
 *   <li>Typical conversions (String → numeric/boolean/char, Date ↔ LocalDate, etc.)</li>
 *   <li>String‑to‑String helpers (empty vs. null handling)</li>
 *   <li>BigDecimal / BigInteger special handling for {@code null} and empty strings</li>
 *   <li>Atomic type helpers with null defaults</li>
 *   <li>Support‑query APIs {@code isConversionSupportedFor} and {@code isSimpleTypeConversionSupported}</li>
 *   <li>Adding and using a custom conversion via {@code addConversion}</li>
 *   <li>Deprecated epoch‑millis helpers for LocalDate / LocalDateTime / ZonedDateTime</li>
 * </ul>
 */
public class ConverterTest {

    /* --------------------------------------------------------------------- */
    /*  Null handling for primitive‑wrapper conversion helper methods        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testNullHandlingForPrimitiveHelpers() {
        // char / Character
        assertEquals((char) 0, Converter.convert2char(null));
        assertNull(Converter.convertToCharacter(null));

        // byte / Byte
        assertEquals((byte) 0, Converter.convert2byte(null));
        assertNull(Converter.convertToByte(null));

        // short / Short
        assertEquals((short) 0, Converter.convert2short(null));
        assertNull(Converter.convertToShort(null));

        // int / Integer
        assertEquals(0, Converter.convert2int(null));
        assertNull(Converter.convertToInteger(null));

        // long / Long
        assertEquals(0L, Converter.convert2long(null));
        assertNull(Converter.convertToLong(null));

        // float / Float
        assertEquals(0.0f, Converter.convert2float(null), 0.0f);
        assertNull(Converter.convertToFloat(null));

        // double / Double
        assertEquals(0.0d, Converter.convert2double(null), 0.0d);
        assertNull(Converter.convertToDouble(null));

        // boolean / Boolean
        assertFalse(Converter.convert2boolean(null));
        assertNull(Converter.convertToBoolean(null));
    }

    /* --------------------------------------------------------------------- */
    /*  Null handling for String‑to‑String helpers                            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testStringHelpersNullHandling() {
        assertEquals("", Converter.convert2String(null));
        assertNull(Converter.convertToString(null));
    }

    /* --------------------------------------------------------------------- */
    /*  BigDecimal / BigInteger handling (null & empty string)             */
    /* --------------------------------------------------------------------- */
    @Test
    public void testBigDecimalConversions() {
        // null → ZERO (convert2)
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(null));

        // empty string → ZERO (convertTo)
        assertEquals(BigDecimal.ZERO, Converter.convertToBigDecimal(""));

        // regular conversion
        assertEquals(new BigDecimal("123.45"), Converter.convert2BigDecimal("123.45"));
    }

    @Test
    public void testBigIntegerConversions() {
        // null → ZERO (convert2)
        assertEquals(BigInteger.ZERO, Converter.convert2BigInteger(null));

        // empty string → ZERO (convertTo)
        assertEquals(BigInteger.ZERO, Converter.convertToBigInteger(""));

        // regular conversion
        assertEquals(new BigInteger("9876543210"), Converter.convert2BigInteger("9876543210"));
    }

    /* --------------------------------------------------------------------- */
    /*  Simple numeric / boolean / char conversions from String              */
    /* --------------------------------------------------------------------- */
    @Test
    public void testStringToNumericAndBooleanConversions() {
        assertEquals(Integer.valueOf(42), Converter.convert("42", Integer.class));
        assertEquals(Long.valueOf(123456789L), Converter.convert("123456789", Long.class));
        assertEquals(Double.valueOf(3.1415), Converter.convert("3.1415", Double.class));
        assertEquals(Boolean.TRUE, Converter.convert("true", Boolean.class));
        assertEquals(Character.valueOf('Z'), Converter.convert("Z", Character.class));
    }

    /* --------------------------------------------------------------------- */
    /*  Date / Time conversions                                                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testSqlDateToUtilDateConversion() {
        java.sql.Date sqlDate = new java.sql.Date(Instant.parse("2024-01-01T00:00:00Z").toEpochMilli());
        Date utilDate = Converter.convertToDate(sqlDate);
        assertEquals(sqlDate.getTime(), utilDate.getTime());
    }

    @Test
    public void testUtilDateToLocalDateConversion() {
        // epoch 0 → 1970‑01‑01
        Date epochDate = new Date(0L);
        LocalDate ld = Converter.convertToLocalDate(epochDate);
        assertEquals(LocalDate.of(1970, 1, 1), ld);
    }

    @Test
    public void testStringToLocalDateConversion() {
        // ISO‑8601 format is supported by the underlying converter
        LocalDate expected = LocalDate.of(2024, 4, 27);
        LocalDate actual = Converter.convert("2024-04-27", LocalDate.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testStringToZonedDateTimeConversion() {
        ZonedDateTime expected = ZonedDateTime.parse("2024-04-27T12:34:56+02:00[Europe/Paris]");
        ZonedDateTime actual = Converter.convert("2024-04-27T12:34:56+02:00[Europe/Paris]", ZonedDateTime.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testTimestampConversion() {
        Timestamp ts = Timestamp.from(Instant.now());
        Timestamp roundTrip = Converter.convertToTimestamp(ts);
        assertEquals(ts, roundTrip);
    }

    /* --------------------------------------------------------------------- */
    /*  Atomic helpers – null defaults                                         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testAtomicHelpersNullHandling() {
        assertEquals(new AtomicInteger(0), Converter.convert2AtomicInteger(null));
        assertNull(Converter.convertToAtomicInteger(null));

        assertEquals(new AtomicLong(0L), Converter.convert2AtomicLong(null));
        assertNull(Converter.convertToAtomicLong(null));

        assertEquals(new AtomicBoolean(false), Converter.convert2AtomicBoolean(null));
        assertNull(Converter.convertToAtomicBoolean(null));
    }

    /* --------------------------------------------------------------------- */
    /*  Support‑query APIs                                                    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testIsConversionSupportedFor() {
        // Simple known conversion
        assertTrue(Converter.isConversionSupportedFor(String.class, Integer.class));

        // Array‑to‑array conversion (String[] → Integer[])
        assertTrue(Converter.isConversionSupportedFor(String[].class, Integer[].class));

        // Unsupported conversion (String → java.util.UUID) – should be false
        assertFalse(Converter.isConversionSupportedFor(String.class, java.util.UUID.class));
    }

    @Test
    public void testIsSimpleTypeConversionSupported() {
        // Simple conversion – true
        assertTrue(Converter.isSimpleTypeConversionSupported(String.class, Double.class));

        // Collection conversion – false
        assertFalse(Converter.isSimpleTypeConversionSupported(java.util.List.class, java.util.Set.class));

        // Same type – true
        assertTrue(Converter.isSimpleTypeConversionSupported(LocalDate.class, LocalDate.class));
    }

    /* --------------------------------------------------------------------- */
    /*  Custom conversion registration                                        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testAddCustomConversion() {
        // Simple POJO used only for this test
        class Dummy {
            private final String value;

            Dummy(String v) { this.value = v; }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Dummy)) return false;
                Dummy d = (Dummy) o;
                return value.equals(d.value);
            }

            @Override
            public int hashCode() {
                return value.hashCode();
            }

            @Override
            public String toString() {
                return "Dummy[" + value + "]";
            }
        }

        // Register conversion: String → Dummy
        Convert<Dummy> stringToDummy = (from, conv) -> new Dummy((String) from);
        Converter.addConversion(String.class, Dummy.class, stringToDummy);

        // Verify conversion works
        Dummy expected = new Dummy("test-value");
        Dummy actual = Converter.convert("test-value", Dummy.class);
        assertEquals(expected, actual);
    }

    /* --------------------------------------------------------------------- */
    /*  Deprecated epoch‑millis helpers                                        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testDeprecatedEpochHelpers() {
        // LocalDate epoch (1970‑01‑01) → 0
        assertEquals(0L, Converter.localDateToMillis(LocalDate.ofEpochDay(0)));

        // LocalDateTime epoch (1970‑01‑01T00:00) → 0
        assertEquals(0L, Converter.localDateTimeToMillis(LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)));

        // ZonedDateTime epoch → 0
        assertEquals(0L, Converter.zonedDateTimeToMillis(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)));
    }

    /* --------------------------------------------------------------------- */
    /*  General convert method – sanity check for a few types                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGeneralConvertMethod() {
        // String → Long (primitive wrapper)
        Long l = Converter.convert("555", Long.class);
        assertEquals(Long.valueOf(555L), l);

        // Integer → String
        String s = Converter.convert(777, String.class);
        assertEquals("777", s);

        // Boolean → Integer (true → 1, false → 0) – typical behaviour of the library
        Integer trueAsInt = Converter.convert(Boolean.TRUE, Integer.class);
        Integer falseAsInt = Converter.convert(Boolean.FALSE, Integer.class);
        assertEquals(Integer.valueOf(1), trueAsInt);
        assertEquals(Integer.valueOf(0), falseAsInt);
    }

    /* --------------------------------------------------------------------- */
    /*  allSupportedConversions / getSupportedConversions sanity check      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testSupportedConversionsMapsAreNotNull() {
        assertNotNull(Converter.allSupportedConversions());
        assertNotNull(Converter.getSupportedConversions());
    }
}
