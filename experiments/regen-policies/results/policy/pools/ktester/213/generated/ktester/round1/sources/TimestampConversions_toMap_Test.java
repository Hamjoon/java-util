package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toMap_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // DefaultConverterOptions is the standard options class used throughout the library.
        converter = new Converter(new DefaultConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to clean up; method kept for symmetry with the template.
        converter = null;
    }

    /**
     * Helper that creates a {@link Timestamp} from an ISO‑date string and a time component.
     */
    private static Timestamp createTimestamp(String isoDate, int hour, int minute, int second, int nanos) {
        return java.sql.Timestamp.from(java.time.LocalDate.parse(isoDate).atTime(hour, minute, second, nanos).atZone(java.time.ZoneId.systemDefault()).toInstant());
    }

    @Test
    void testToMapWithTimestamp() {
        java.sql.Timestamp ts = createTimestamp("2023-06-15", 12, 30, 45, 123_456_789);
        // Expected string representation – the same conversion that TimestampConversions.toString uses.
        String expectedString = converter.convert(ts, String.class);
        // Invoke the method under test.
        Map<String, Object> result = TimestampConversions.toMap(ts, converter);
        // Verify map structure.
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map must contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.TIMESTAMP), "Map must contain the TIMESTAMP key");
        // Verify the stored value.
        assertEquals(expectedString, result.get(MapConversions.TIMESTAMP), "Map value must match the string conversion of the source object");
    }

    @Test
    void testToMapWithNull() {
        // When the source object is null, TimestampConversions.toString returns null,
        // therefore the map should contain a null value.
        Map<String, Object> result = TimestampConversions.toMap(null, converter);
        assertNotNull(result, "Resulting map must not be null even when source is null");
        assertEquals(1, result.size(), "Map must contain exactly one entry for a null source");
        assertTrue(result.containsKey(MapConversions.TIMESTAMP), "Map must contain the TIMESTAMP key");
        assertNull(result.get(MapConversions.TIMESTAMP), "Value for a null source must be null");
    }

    @Test
    void testToMapWithVariousDateTypes() {
        // Verify that the method works for a few different date‑time classes supported by the Converter.
        Object[] sources = new Object[] { // java.sql.Timestamp
        createTimestamp("1970-01-01", 0, 0, 0, 0), // java.util.Date
        java.util.Date.from(java.time.Instant.now()), // java.time.LocalDateTime
        java.time.LocalDateTime.now(), // java.time.OffsetDateTime
        java.time.OffsetDateTime.now() };
        for (Object src : sources) {
            String expected = converter.convert(src, String.class);
            Map<String, Object> map = TimestampConversions.toMap(src, converter);
            assertNotNull(map, "Map must not be null for source: " + src);
            assertEquals(1, map.size(), "Map must contain exactly one entry for source: " + src);
            assertEquals(expected, map.get(MapConversions.TIMESTAMP), "Incorrect string value for source: " + src);
        }
    }

    /**
     * Verify that a {@link java.util.Date} representing the epoch is correctly converted
     * to an ISO‑8601 UTC string when the {@code converter} argument is {@code null}.
     */
    @Test
    void testToMapWithDateAndNullConverter() {
        // 1970‑01‑01T00:00:00Z
        Date epoch = new Date(0L);
        // The method under test should still produce a map with the expected ISO‑8601 string.
        Map<String, Object> result = TimestampConversions.toMap(epoch, null);
        assertNotNull(result, "Result map must not be null");
        assertEquals(1, result.size(), "Result map must contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.TIMESTAMP), "Result map must contain the TIMESTAMP key");
        // Expected ISO‑8601 representation of the epoch in UTC.
        String expected = "1970-01-01T00:00:00Z";
        assertEquals(expected, result.get(MapConversions.TIMESTAMP), "Timestamp string does not match the expected ISO‑8601 UTC value");
    }

    /**
     * When both the source object and the converter are {@code null}, the internal
     * {@code toString} call should throw a {@link NullPointerException},
     * which propagates out of {@code toMap}.
     */
    @Test
    void testToMapWithNullSourceAndNullConverter() {
        assertThrows(NullPointerException.class, () -> TimestampConversions.toMap(null, null), "Expected NullPointerException when both source and converter are null");
    }

    /**
     * Passing an object of a type that {@code TimestampConversions} does not know how to
     * convert should result in an {@link IllegalArgumentException} bubbling up from
     * the internal {@code toString} call.
     */
    @Test
    void testToMapWithUnsupportedType() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> TimestampConversions.toMap(unsupported, null), "Expected IllegalArgumentException for an unsupported source type");
    }

    /**
     * If the supplied {@link Converter} throws an exception during conversion,
     * {@code toMap} must propagate that exception unchanged.
     */
    @Test
    void testToMapWithConverterThatThrows() {
        // Create a mock Converter that always throws IllegalStateException.
        Converter badConverter = mock(Converter.class);
        when(badConverter.convert(any(), eq(String.class))).thenThrow(new IllegalStateException("converter failure"));
        Date someDate = new Date(0L);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> TimestampConversions.toMap(someDate, badConverter), "Expected IllegalStateException to be propagated from the converter");
        assertEquals("converter failure", thrown.getMessage(), "Exception message should match the one thrown by the mock converter");
    }
}
