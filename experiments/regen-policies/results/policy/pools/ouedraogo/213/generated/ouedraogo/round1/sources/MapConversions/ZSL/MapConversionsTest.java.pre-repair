package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Comprehensive unit tests for {@link MapConversions}.
 *
 * The tests are written against the package‑private static methods, therefore the
 * test class lives in the same package as the class under test.
 */
public class MapConversionsTest {

    // Helper to create a Converter with default options (null is acceptable for the constructor)
    private static Converter newConverter() {
        return new Converter(null);
    }

    // -------------------------------------------------------------------------
    // formatKeys()
    // -------------------------------------------------------------------------

    @Test
    public void testFormatKeysSingle() {
        String[] keys = {"value"};
        String formatted = invokeFormatKeys(keys);
        Assert.assertEquals("[value]", formatted);
    }

    @Test
    public void testFormatKeysTwo() {
        String[] keys = {"value", "_v"};
        String formatted = invokeFormatKeys(keys);
        Assert.assertEquals("[value] or [_v]", formatted);
    }

    @Test
    public void testFormatKeysThreeOrMore() {
        String[] keys = {"a", "b", "c", "d"};
        String formatted = invokeFormatKeys(keys);
        Assert.assertEquals("[a], [b], [c], or [d]", formatted);
    }

    // -------------------------------------------------------------------------
    // getValue() and dispatch() via the conversion helpers
    // -------------------------------------------------------------------------

    @Test
    public void testDispatchUsesFirstNonEmptyValue() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.VALUE, "");               // empty string – should be ignored
        map.put(MapConversions.V, 42);                  // valid integer
        Converter converter = newConverter();

        Integer result = MapConversions.toInt(map, converter);
        Assert.assertEquals(Integer.valueOf(42), result);
    }

    @Test
    public void testDispatchWhenAllValuesMissingThrows() {
        Map<String, Object> empty = new LinkedHashMap<>();
        Converter converter = newConverter();

        try {
            MapConversions.toBoolean(empty, converter);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // Message must contain the formatted list of keys for Boolean conversion
            String expected = "value] or [_v";
            Assert.assertTrue("Message should mention required keys", ex.getMessage().contains(expected));
        }
    }

    // -------------------------------------------------------------------------
    // toUUID()
    // -------------------------------------------------------------------------

    @Test
    public void testToUUIDFromMostAndLeastSigBits() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.MOST_SIG_BITS, 0x1234567890ABCDEFL);
        map.put(MapConversions.LEAST_SIG_BITS, 0x0FEDCBA098765432L);
        Converter converter = newConverter();

        Object result = MapConversions.toUUID(map, converter);
        Assert.assertTrue(result instanceof UUID);
        UUID uuid = (UUID) result;
        Assert.assertEquals(0x1234567890ABCDEFL, uuid.getMostSignificantBits());
        Assert.assertEquals(0x0FEDCBA098765432L, uuid.getLeastSignificantBits());
    }

    @Test
    public void testToUUIDFromValueKey() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.VALUE, UUID.randomUUID().toString()); // a String representation
        Converter converter = newConverter();

        Object result = MapConversions.toUUID(map, converter);
        Assert.assertTrue(result instanceof UUID);
        // The converter should have turned the String into a UUID – we only verify the type
    }

    // -------------------------------------------------------------------------
    // toByteBuffer()
    // -------------------------------------------------------------------------

    @Test
    public void testToByteBufferValidBase64() {
        String original = "hello world";
        String base64 = Base64.getEncoder().encodeToString(original.getBytes());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.VALUE, base64);
        Converter converter = newConverter();

        ByteBuffer buffer = MapConversions.toByteBuffer(map, converter);
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Assert.assertArrayEquals(original.getBytes(), bytes);
    }

    @Test
    public void testToByteBufferMissingValueThrows() {
        Map<String, Object> map = new LinkedHashMap<>();
        Converter converter = newConverter();

        try {
            MapConversions.toByteBuffer(map, converter);
            Assert.fail("Expected IllegalArgumentException for missing value");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("Missing or null 'value'"));
        }
    }

    @Test
    public void testToByteBufferNonStringValueThrows() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.VALUE, 12345); // not a String
        Converter converter = newConverter();

        try {
            MapConversions.toByteBuffer(map, converter);
            Assert.fail("Expected IllegalArgumentException for non‑String value");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("Value must be a Base64-encoded String"));
        }
    }

    @Test
    public void testToByteBufferInvalidBase64Throws() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.VALUE, "!!! not base64 !!!");
        Converter converter = newConverter();

        try {
            MapConversions.toByteBuffer(map, converter);
            Assert.fail("Expected IllegalArgumentException for invalid Base64");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("Invalid Base64 encoding"));
            Assert.assertTrue(ex.getCause() instanceof IllegalArgumentException);
        }
    }

    // -------------------------------------------------------------------------
    // toThrowable()
    // -------------------------------------------------------------------------

    @Test
    public void testToThrowableSimpleException() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.CLASS, IllegalArgumentException.class.getName());
        map.put(MapConversions.MESSAGE, "bad argument");
        Converter converter = newConverter();

        Throwable t = MapConversions.toThrowable(map, converter, Throwable.class);
        Assert.assertTrue(t instanceof IllegalArgumentException);
        Assert.assertEquals("bad argument", t.getMessage());
        Assert.assertNull(t.getCause());
        // Stack trace must be cleared
        Assert.assertEquals(0, t.getStackTrace().length);
    }

    @Test
    public void testToThrowableWithCause() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.CLASS, RuntimeException.class.getName());
        map.put(MapConversions.MESSAGE, "outer");
        map.put(MapConversions.CAUSE, NullPointerException.class.getName());
        map.put(MapConversions.CAUSE_MESSAGE, "inner cause");
        Converter converter = newConverter();

        Throwable t = MapConversions.toThrowable(map, converter, Throwable.class);
        Assert.assertTrue(t instanceof RuntimeException);
        Assert.assertEquals("outer", t.getMessage());
        Assert.assertNotNull(t.getCause());
        Assert.assertTrue(t.getCause() instanceof NullPointerException);
        Assert.assertEquals("inner cause", t.getCause().getMessage());
    }

    // -------------------------------------------------------------------------
    // initMap()
    // -------------------------------------------------------------------------

    @Test
    public void testInitMapCreatesMapWithVKey() {
        String original = "payload";
        Converter converter = newConverter();

        Map<String, ?> result = MapConversions.initMap(original, converter);
        Assert.assertTrue(result.containsKey(MapConversions.V));
        Assert.assertEquals(original, result.get(MapConversions.V));
    }

    // -------------------------------------------------------------------------
    // Simple conversion helpers (toInt, toBoolean, etc.)
    // -------------------------------------------------------------------------

    @Test
    public void testSimpleConversionHelpersPassThroughIdentity() {
        Converter converter = newConverter();

        Map<String, Object> mapInt = new LinkedHashMap<>();
        mapInt.put(MapConversions.VALUE, 7);
        Assert.assertEquals(Integer.valueOf(7), MapConversions.toInt(mapInt, converter));

        Map<String, Object> mapBool = new LinkedHashMap<>();
        mapBool.put(MapConversions.VALUE, Boolean.TRUE);
        Assert.assertEquals(Boolean.TRUE, MapConversions.toBoolean(mapBool, converter));

        Map<String, Object> mapString = new LinkedHashMap<>();
        mapString.put(MapConversions.VALUE, "text");
        Assert.assertEquals("text", MapConversions.toString(mapString, converter));
    }

    // -------------------------------------------------------------------------
    // Date/Time conversion helpers – they delegate to dispatch, we verify that a
    // correctly typed object is returned when the map already holds the target type.
    // -------------------------------------------------------------------------

    @Test
    public void testDateTimeHelpersReturnSuppliedInstances() {
        Converter converter = newConverter();

        // LocalDate
        LocalDate ld = LocalDate.of(2021, 3, 14);
        Map<String, Object> mapLd = new LinkedHashMap<>();
        mapLd.put(MapConversions.VALUE, ld);
        Assert.assertEquals(ld, MapConversions.toLocalDate(mapLd, converter));

        // Duration
        Duration dur = Duration.ofMinutes(15);
        Map<String, Object> mapDur = new LinkedHashMap<>();
        mapDur.put(MapConversions.VALUE, dur);
        Assert.assertEquals(dur, MapConversions.toDuration(mapDur, converter));

        // ZoneId
        ZoneId zid = ZoneId.of("UTC");
        Map<String, Object> mapZid = new LinkedHashMap<>();
        mapZid.put(MapConversions.VALUE, zid);
        Assert.assertEquals(zid, MapConversions.toZoneId(mapZid, converter));
    }

    // -------------------------------------------------------------------------
    // Helper to invoke the private static formatKeys method via reflection.
    // -------------------------------------------------------------------------
    private static String invokeFormatKeys(String[] keys) {
        try {
            java.lang.reflect.Method m = MapConversions.class.getDeclaredMethod("formatKeys", String[].class);
            m.setAccessible(true);
            return (String) m.invoke(null, (Object) keys);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
