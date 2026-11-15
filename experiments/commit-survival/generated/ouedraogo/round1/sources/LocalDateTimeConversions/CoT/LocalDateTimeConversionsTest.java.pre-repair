package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link LocalDateTimeConversions}.
 *
 * <p>The production class uses {@code Converter} and {@code ConverterOptions}
 * to obtain a {@code ZoneId} and a {@code TimeZone}. Those two classes are not
 * part of the supplied source, therefore minimal stub implementations are
 * provided inside the test file.  They expose only the members that are
 * exercised by {@code LocalDateTimeConversions}.</p>
 */
public class LocalDateTimeConversionsTest {

    /* --------------------------------------------------------------------- */
    /*  Stub implementations – only the API used by the class under test.   */
    /* --------------------------------------------------------------------- */

    /** Minimal stub for {@code ConverterOptions}. */
    static class ConverterOptions {
        private final ZoneId zoneId;
        private final java.util.TimeZone timeZone;

        ConverterOptions(ZoneId zoneId) {
            this.zoneId = zoneId;
            this.timeZone = java.util.TimeZone.getTimeZone(zoneId);
        }

        ZoneId getZoneId() {
            return zoneId;
        }

        java.util.TimeZone getTimeZone() {
            return timeZone;
        }
    }

    /** Minimal stub for {@code Converter}. */
    static final class Converter {
        private final ConverterOptions options;

        Converter(ConverterOptions options) {
            this.options = options;
        }

        ConverterOptions getOptions() {
            return options;
        }

        // The remaining methods are not needed for the tests – they simply
        // throw to make accidental use obvious.
        public <T> T convert(Object from, Class<T> toType) {
            throw new UnsupportedOperationException();
        }

        public boolean isConversionSupportedFor(Class<?> source, Class<?> target) {
            throw new UnsupportedOperationException();
        }

        public java.util.Map<Class<?>, java.util.Set<Class<?>>> allSupportedConversions() {
            throw new UnsupportedOperationException();
        }

        public java.util.Map<String, java.util.Set<String>> getSupportedConversions() {
            throw new UnsupportedOperationException();
        }

        public Convert<?> addConversion(Class<?> source, Class<?> target, Convert<?> conversionFunction) {
            throw new UnsupportedOperationException();
        }
    }

    /** Minimal stub for the functional interface used by {@code Converter}. */
    interface Convert<T> {
        T convert(Object from);
    }

    /* --------------------------------------------------------------------- */
    /*  Test fixtures                                                       */
    /* --------------------------------------------------------------------- */

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId OFFSET_530 = ZoneOffset.ofHoursMinutes(5, 30);
    private Converter utcConverter;
    private Converter offsetConverter;

    private final LocalDateTime sampleLdt = LocalDateTime.of(
            2022, Month.MARCH, 15,
            12, 34, 56, 789_000_000); // 789 ms

    @Before
    public void setUp() {
        utcConverter = new Converter(new ConverterOptions(UTC));
        offsetConverter = new Converter(new ConverterOptions(OFFSET_530));
    }

    /* --------------------------------------------------------------------- */
    /*  Basic functionality tests                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToZonedDateTime_Utc() {
        ZonedDateTime zdt = LocalDateTimeConversions.toZonedDateTime(sampleLdt, utcConverter);
        assertEquals(sampleLdt, zdt.toLocalDateTime());
        assertEquals(UTC, zdt.getZone());
    }

    @Test
    public void testToZonedDateTime_WithOffset() {
        ZonedDateTime zdt = LocalDateTimeConversions.toZonedDateTime(sampleLdt, offsetConverter);
        assertEquals(sampleLdt, zdt.toLocalDateTime());
        assertEquals(OFFSET_530, zdt.getOffset());
    }

    @Test
    public void testToOffsetDateTime_Utc() {
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(sampleLdt, utcConverter);
        assertEquals(sampleLdt, odt.toLocalDateTime());
        assertEquals(ZoneOffset.UTC, odt.getOffset());
    }

    @Test
    public void testToOffsetDateTime_WithOffset() {
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(sampleLdt, offsetConverter);
        assertEquals(sampleLdt, odt.toLocalDateTime());
        assertEquals(OFFSET_530, odt.getOffset());
    }

    @Test
    public void testToInstant() {
        Instant instant = LocalDateTimeConversions.toInstant(sampleLdt, utcConverter);
        ZonedDateTime expected = ZonedDateTime.of(sampleLdt, UTC);
        assertEquals(expected.toInstant(), instant);
    }

    @Test
    public void testToLong() {
        long epochMilli = LocalDateTimeConversions.toLong(sampleLdt, utcConverter);
        Instant instant = LocalDateTimeConversions.toInstant(sampleLdt, utcConverter);
        assertEquals(instant.toEpochMilli(), epochMilli);
    }

    @Test
    public void testToDouble() {
        double d = LocalDateTimeConversions.toDouble(sampleLdt, utcConverter);
        // The exact conversion performed by InstantConversions is not known,
        // but the result must be a finite number and must be greater than zero
        // for a positive epoch millisecond value.
        assertTrue(Double.isFinite(d));
        assertTrue(d > 0);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(sampleLdt, utcConverter);
        assertEquals(sampleLdt, result);
    }

    @Test
    public void testToLocalDate() {
        LocalDate date = LocalDateTimeConversions.toLocalDate(sampleLdt, utcConverter);
        assertEquals(sampleLdt.toLocalDate(), date);
    }

    @Test
    public void testToLocalTime() {
        LocalTime time = LocalDateTimeConversions.toLocalTime(sampleLdt, utcConverter);
        assertEquals(sampleLdt.toLocalTime(), time);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong al = LocalDateTimeConversions.toAtomicLong(sampleLdt, utcConverter);
        assertEquals(LocalDateTimeConversions.toLong(sampleLdt, utcConverter), al.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = LocalDateTimeConversions.toTimestamp(sampleLdt, utcConverter);
        Instant expected = sampleLdt.atZone(UTC).toInstant();
        assertEquals(Timestamp.from(expected), ts);
    }

    @Test
    public void testToCalendar() {
        Calendar cal = LocalDateTimeConversions.toCalendar(sampleLdt, utcConverter);
        long expectedMillis = sampleLdt.atZone(UTC).toInstant().toEpochMilli();
        assertEquals(expectedMillis, cal.getTimeInMillis());
        assertEquals(utcConverter.getOptions().getTimeZone(), cal.getTimeZone());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = LocalDateTimeConversions.toSqlDate(sampleLdt, utcConverter);
        assertEquals(LocalDateTimeConversions.toLong(sampleLdt, utcConverter), sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        Date date = LocalDateTimeConversions.toDate(sampleLdt, utcConverter);
        assertEquals(LocalDateTimeConversions.toLong(sampleLdt, utcConverter), date.getTime());
    }

    @Test
    public void testToBigInteger() {
        BigInteger bi = LocalDateTimeConversions.toBigInteger(sampleLdt, utcConverter);
        assertNotNull(bi);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = LocalDateTimeConversions.toBigDecimal(sampleLdt, utcConverter);
        assertNotNull(bd);
    }

    @Test
    public void testToString() {
        String s = LocalDateTimeConversions.toString(sampleLdt, utcConverter);
        assertEquals(sampleLdt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), s);
    }

    @Test
    public void testToMap() {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocalDateTimeConversions.toMap(sampleLdt, utcConverter);
        assertEquals(sampleLdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(sampleLdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(2, map.size());
    }

    /* --------------------------------------------------------------------- */
    /*  Edge‑case and exception tests                                       */
    /* --------------------------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_NullInput() {
        LocalDateTimeConversions.toZonedDateTime(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WrongType() {
        LocalDateTimeConversions.toZonedDateTime("not a LocalDateTime", utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToOffsetDateTime_NullInput() {
        LocalDateTimeConversions.toOffsetDateTime(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToOffsetDateTime_WrongType() {
        LocalDateTimeConversions.toOffsetDateTime(12345, utcConverter);
    }

    @Test
    public void testToInstant_WithNonUtcZone() {
        // Use a zone with a non‑integral hour offset to verify correct handling.
        Instant instant = LocalDateTimeConversions.toInstant(sampleLdt, offsetConverter);
        ZonedDateTime expected = ZonedDateTime.of(sampleLdt, OFFSET_530);
        assertEquals(expected.toInstant(), instant);
    }

    @Test
    public void testToLong_EpochBoundary() {
        LocalDateTime epoch = LocalDateTime.ofInstant(Instant.EPOCH, UTC);
        long epochMilli = LocalDateTimeConversions.toLong(epoch, utcConverter);
        assertEquals(0L, epochMilli);
    }

    @Test
    public void testToTimestamp_NanosecondPrecision() {
        LocalDateTime ldt = LocalDateTime.of(2022, 1, 1, 0, 0, 0, 999_999_999);
        Timestamp ts = LocalDateTimeConversions.toTimestamp(ldt, utcConverter);
        // Timestamp stores nanoseconds separately; ensure the nanosecond part is preserved.
        assertEquals(999_999_999, ts.getNanos());
    }

    @Test
    public void testToMap_ImmutableResult() {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocalDateTimeConversions.toMap(sampleLdt, utcConverter);
        // The map implementation is a mutable CompactLinkedMap – the contract does not
        // require immutability, but we verify that modifications affect the map.
        map.put("newKey", "newValue");
        assertEquals("newValue", map.get("newKey"));
    }
}
