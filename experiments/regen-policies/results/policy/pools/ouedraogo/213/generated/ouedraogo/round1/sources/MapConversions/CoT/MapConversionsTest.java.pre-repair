package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.*;
import java.util.*;

public class MapConversionsTest {

    /* ----------------------------------------------------------------------
     * Helper to create a real Converter instance (uses the library's
     * ConverterOptions class via reflection – the class exists in the same
     * library and has a no‑arg constructor).
     * ---------------------------------------------------------------------- */
    private static Converter createConverter() {
        try {
            Class<?> optsClass = Class.forName("com.cedarsoftware.util.convert.ConverterOptions");
            Object opts = optsClass.getDeclaredConstructor().newInstance();
            return new Converter(opts);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create Converter instance", e);
        }
    }

    /* ----------------------------------------------------------------------
     * 1.  BASIC FUNCTIONALITY TESTS – each conversion method is exercised with
     *     a map that contains the expected key/value pair.
     * ---------------------------------------------------------------------- */
    @Test
    public void testToByte() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", (byte) 7);
        Byte result = MapConversions.toByte(map, createConverter());
        Assert.assertEquals(Byte.valueOf((byte) 7), result);
    }

    @Test
    public void testToShort() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", (short) 12);
        Short result = MapConversions.toShort(map, createConverter());
        Assert.assertEquals(Short.valueOf((short) 12), result);
    }

    @Test
    public void testToInt() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", 42);
        Integer result = MapConversions.toInt(map, createConverter());
        Assert.assertEquals(Integer.valueOf(42), result);
    }

    @Test
    public void testToLong() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", 123456789L);
        Long result = MapConversions.toLong(map, createConverter());
        Assert.assertEquals(Long.valueOf(123456789L), result);
    }

    @Test
    public void testToFloat() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", 3.14f);
        Float result = MapConversions.toFloat(map, createConverter());
        Assert.assertEquals(Float.valueOf(3.14f), result);
    }

    @Test
    public void testToDouble() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", 2.71828);
        Double result = MapConversions.toDouble(map, createConverter());
        Assert.assertEquals(Double.valueOf(2.71828), result);
    }

    @Test
    public void testToBoolean() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", Boolean.TRUE);
        Boolean result = MapConversions.toBoolean(map, createConverter());
        Assert.assertTrue(result);
    }

    @Test
    public void testToBigDecimal() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", new BigDecimal("123.45"));
        BigDecimal result = MapConversions.toBigDecimal(map, createConverter());
        Assert.assertEquals(new BigDecimal("123.45"), result);
    }

    @Test
    public void testToBigInteger() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", new BigInteger("987654321"));
        BigInteger result = MapConversions.toBigInteger(map, createConverter());
        Assert.assertEquals(new BigInteger("987654321"), result);
    }

    @Test
    public void testToString() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "hello");
        String result = MapConversions.toString(map, createConverter());
        Assert.assertEquals("hello", result);
    }

    @Test
    public void testToStringBuilder() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "builder");
        StringBuilder result = MapConversions.toStringBuilder(map, createConverter());
        Assert.assertEquals("builder", result.toString());
    }

    @Test
    public void testToCharacter() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", 'X');
        Character result = MapConversions.toCharacter(map, createConverter());
        Assert.assertEquals(Character.valueOf('X'), result);
    }

    @Test
    public void testToAtomicInteger() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", 99);
        AtomicInteger result = MapConversions.toAtomicInteger(map, createConverter());
        Assert.assertEquals(99, result.get());
    }

    @Test
    public void testToAtomicLong() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", 123L);
        AtomicLong result = MapConversions.toAtomicLong(map, createConverter());
        Assert.assertEquals(123L, result.get());
    }

    @Test
    public void testToAtomicBoolean() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", Boolean.FALSE);
        AtomicBoolean result = MapConversions.toAtomicBoolean(map, createConverter());
        Assert.assertFalse(result.get());
    }

    @Test
    public void testToPattern() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "a*b");
        java.util.regex.Pattern result = MapConversions.toPattern(map, createConverter());
        Assert.assertEquals("a*b", result.pattern());
    }

    @Test
    public void testToCurrency() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", Currency.getInstance("USD"));
        Currency result = MapConversions.toCurrency(map, createConverter());
        Assert.assertEquals(Currency.getInstance("USD"), result);
    }

    @Test
    public void testToSqlDate() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", new java.sql.Date(0L));
        java.sql.Date result = MapConversions.toSqlDate(map, createConverter());
        Assert.assertEquals(new java.sql.Date(0L), result);
    }

    @Test
    public void testToDate() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", new Date(1_000L));
        Date result = MapConversions.toDate(map, createConverter());
        Assert.assertEquals(new Date(1_000L), result);
    }

    @Test
    public void testToTimestamp() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", new Timestamp(2_000L));
        Timestamp result = MapConversions.toTimestamp(map, createConverter());
        Assert.assertEquals(new Timestamp(2_000L), result);
    }

    @Test
    public void testToTimeZone() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", TimeZone.getTimeZone("UTC"));
        TimeZone result = MapConversions.toTimeZone(map, createConverter());
        Assert.assertEquals(TimeZone.getTimeZone("UTC"), result);
    }

    @Test
    public void testToCalendar() {
        Map<String, Object> map = new HashMap<>();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        map.put("_v", cal);
        Calendar result = MapConversions.toCalendar(map, createConverter());
        Assert.assertEquals(cal, result);
    }

    @Test
    public void testToLocale() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", Locale.CANADA_FRENCH);
        Locale result = MapConversions.toLocale(map, createConverter());
        Assert.assertEquals(Locale.CANADA_FRENCH, result);
    }

    @Test
    public void testToLocalDate() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", LocalDate.of(2023, 3, 14));
        LocalDate result = MapConversions.toLocalDate(map, createConverter());
        Assert.assertEquals(LocalDate.of(2023, 3, 14), result);
    }

    @Test
    public void testToLocalTime() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", LocalTime.of(12, 30, 45));
        LocalTime result = MapConversions.toLocalTime(map, createConverter());
        Assert.assertEquals(LocalTime.of(12, 30, 45), result);
    }

    @Test
    public void testToLocalDateTime() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", LocalDateTime.of(2021, 12, 31, 23, 59, 59));
        LocalDateTime result = MapConversions.toLocalDateTime(map, createConverter());
        Assert.assertEquals(LocalDateTime.of(2021, 12, 31, 23, 59, 59), result);
    }

    @Test
    public void testToOffsetTime() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", OffsetTime.of(10, 15, 30, 0, ZoneOffset.UTC));
        OffsetTime result = MapConversions.toOffsetTime(map, createConverter());
        Assert.assertEquals(OffsetTime.of(10, 15, 30, 0, ZoneOffset.UTC), result);
    }

    @Test
    public void testToOffsetDateTime() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", OffsetDateTime.of(2022, 5, 20, 8, 0, 0, 0, ZoneOffset.ofHours(2)));
        OffsetDateTime result = MapConversions.toOffsetDateTime(map, createConverter());
        Assert.assertEquals(OffsetDateTime.of(2022, 5, 20, 8, 0, 0, 0, ZoneOffset.ofHours(2)), result);
    }

    @Test
    public void testToZonedDateTime() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Paris")));
        ZonedDateTime result = MapConversions.toZonedDateTime(map, createConverter());
        Assert.assertEquals(ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Paris")), result);
    }

    @Test
    public void testToClass() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", String.class);
        Class<?> result = MapConversions.toClass(map, createConverter());
        Assert.assertEquals(String.class, result);
    }

    @Test
    public void testToDuration() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", Duration.ofMinutes(5));
        Duration result = MapConversions.toDuration(map, createConverter());
        Assert.assertEquals(Duration.ofMinutes(5), result);
    }

    @Test
    public void testToInstant() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", Instant.ofEpochSecond(1000));
        Instant result = MapConversions.toInstant(map, createConverter());
        Assert.assertEquals(Instant.ofEpochSecond(1000), result);
    }

    @Test
    public void testToMonthDay() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", MonthDay.of(12, 25));
        MonthDay result = MapConversions.toMonthDay(map, createConverter());
        Assert.assertEquals(MonthDay.of(12, 25), result);
    }

    @Test
    public void testToYearMonth() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", YearMonth.of(2024, 2));
        YearMonth result = MapConversions.toYearMonth(map, createConverter());
        Assert.assertEquals(YearMonth.of(2024, 2), result);
    }

    @Test
    public void testToPeriod() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", Period.ofMonths(6));
        Period result = MapConversions.toPeriod(map, createConverter());
        Assert.assertEquals(Period.ofMonths(6), result);
    }

    @Test
    public void testToZoneId() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", ZoneId.of("America/New_York"));
        ZoneId result = MapConversions.toZoneId(map, createConverter());
        Assert.assertEquals(ZoneId.of("America/New_York"), result);
    }

    @Test
    public void testToZoneOffset() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", ZoneOffset.ofHours(-5));
        ZoneOffset result = MapConversions.toZoneOffset(map, createConverter());
        Assert.assertEquals(ZoneOffset.ofHours(-5), result);
    }

    @Test
    public void testToYear() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", Year.of(1999));
        Year result = MapConversions.toYear(map, createConverter());
        Assert.assertEquals(Year.of(1999), result);
    }

    @Test
    public void testToURL() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("value", new URL("http://example.com"));
        URL result = MapConversions.toURL(map, createConverter());
        Assert.assertEquals(new URL("http://example.com"), result);
    }

    @Test
    public void testToURI() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", new URI("urn:test"));
        URI result = MapConversions.toURI(map, createConverter());
        Assert.assertEquals(new URI("urn:test"), result);
    }

    @Test
    public void testToUUIDFromBits() {
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", 0x1234567890ABCDEFL);
        map.put("leastSigBits", 0x0FEDCBA987654321L);
        Object result = MapConversions.toUUID(map, createConverter());
        Assert.assertTrue(result instanceof UUID);
        Assert.assertEquals(new UUID(0x1234567890ABCDEFL, 0x0FEDCBA987654321L), result);
    }

    @Test
    public void testToByteBufferValid() {
        byte[] original = new byte[]{10, 20, 30, 40};
        String base64 = Base64.getEncoder().encodeToString(original);
        Map<String, Object> map = new HashMap<>();
        map.put("value", base64);
        ByteBuffer buffer = MapConversions.toByteBuffer(map, createConverter());
        Assert.assertArrayEquals(original, buffer.array());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByteBufferMissingValue() {
        Map<String, Object> map = new HashMap<>();
        // No "value" or "_v"
        MapConversions.toByteBuffer(map, createConverter());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByteBufferNonStringValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", 12345); // not a String
        MapConversions.toByteBuffer(map, createConverter());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToByteBufferInvalidBase64() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "!!! not base64 !!!");
        MapConversions.toByteBuffer(map, createConverter());
    }

    @Test
    public void testInitMap() {
        String original = "testValue";
        Map<String, ?> result = MapConversions.initMap(original, createConverter());
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.containsKey("_v"));
        Assert.assertEquals(original, result.get("_v"));
    }

    /* ----------------------------------------------------------------------
     * 2.  EDGE CASES – missing keys cause an IllegalArgumentException from
     *     the private fromMap() method.  We verify the exception is thrown and
     *     that the message contains the expected key list.
     * ---------------------------------------------------------------------- */
    @Test
    public void testMissingKeyExceptionMessage() {
        Map<String, Object> map = new HashMap<>(); // empty map
        try {
            MapConversions.toInt(map, createConverter());
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            // The message should mention the required keys "value" or "_v"
            Assert.assertTrue(msg.contains("value"));
            Assert.assertTrue(msg.contains("_v"));
        }
    }

    /* ----------------------------------------------------------------------
     * 3.  TESTING toThrowable – normal reconstruction and edge cases.
     * ---------------------------------------------------------------------- */
    @Test
    public void testToThrowableSimple() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", "java.lang.Exception");
        map.put("message", "top level");
        map.put("cause", "java.lang.IllegalArgumentException");
        map.put("causeMessage", "inner cause");

        Throwable t = MapConversions.toThrowable(map, createConverter(), Throwable.class);
        Assert.assertTrue(t instanceof Exception);
        Assert.assertEquals("top level", t.getMessage());
        Assert.assertNotNull(t.getCause());
        Assert.assertTrue(t.getCause() instanceof IllegalArgumentException);
        Assert.assertEquals("inner cause", t.getCause().getMessage());

        // Stack trace should be cleared
        Assert.assertEquals(0, t.getStackTrace().length);
    }

    @Test
    public void testToThrowableMissingClassUsesTarget() {
        Map<String, Object> map = new HashMap<>();
        // No "class" entry – should fall back to the supplied target (RuntimeException)
        map.put("message", "fallback test");

        Throwable t = MapConversions.toThrowable(map, createConverter(), RuntimeException.class);
        Assert.assertTrue(t instanceof RuntimeException);
        Assert.assertEquals("fallback test", t.getMessage());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToThrowableInvalidClassName() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", "non.existent.ClassName");
        map.put("message", "won't matter");
        MapConversions.toThrowable(map, createConverter(), Throwable.class);
    }

    /* ----------------------------------------------------------------------
     * 4.  PRIVATE helper methods are not directly accessible, but we can
     *     indirectly test their behaviour via public APIs (already covered).
     * ---------------------------------------------------------------------- */
}
