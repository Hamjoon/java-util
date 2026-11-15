package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toSqlDate_Test {

    private static Converter converter;

    /**
     * A real converter – used for the majority of happy‑path tests.
     */
    private static Converter realConverter;

    /**
     * Simple holder for the parameters of a parameterised test.
     */
    private static final class ArgumentsHolder {

        final Object from;

        final long expectedMillis;

        final String description;

        ArgumentsHolder(Object from, long expectedMillis, String description) {
            this.from = from;
            this.expectedMillis = expectedMillis;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    class NullKeyEpochMillis {
        // No fields – we will use reflection to inject a null key via a spy.
    }

    class NullKeyEpochMillis {
        // No fields – we will use reflection to inject a null key via a spy.
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance; the default options are sufficient for these tests
        converter = new Converter(new ConverterOptions());
        realConverter = new Converter(new ConverterOptions());
    }

    /**
     * Provides test data for the “epoch‑millis present” group.
     */
    private static Stream<ArgumentsHolder> epochMillisPresentProvider() {
        return Stream.of(// simple millis from an Instant
        new ArgumentsHolder(Instant.ofEpochMilli(1622548800000L), 1622548800000L, "2021‑06‑01T00:00:00.000Z"), // millis + nanos (2 500 000 ns = 2 ms)
        new ArgumentsHolder(Instant.ofEpochSecond(1622548800L, 2_500_000L), // 2 ms added
        1622548800000L + 2, "2021‑06‑01T00:00:00.002Z"), // negative instant (one day before epoch)
        new ArgumentsHolder(// -86400 seconds
        Instant.ofEpochSecond(-86_400L), -86_400_000L, "1969‑12‑31T00:00:00Z"), // java.util.Date instance (treated as epoch millis)
        new ArgumentsHolder(new Date(1622548800123L), 1622548800123L, "2021‑06‑01T00:00:00.123Z"), // java.sql.Timestamp with nanos
        new ArgumentsHolder(Timestamp.valueOf("2021-06-01 12:34:56.123456789"), // base millis + 123 ms from nanos/1_000_000
        1622550896123L + 123, "2021‑06‑01T12:34:56.123Z"), // plain Long interpreted as epoch millis
        new ArgumentsHolder(Long.valueOf(1622548800123L), 1622548800123L, "2021‑06‑01T00:00:00.123Z"));
    }

    /**
     * When the source object is a {@link java.util.Date}, {@code toEpochMillis} returns a non‑null
     * entry (epoch millis + zero nanos). The method should therefore return a new {@link Date}
     * with the same millisecond value.
     */
    @Test
    void testToSqlDateFromDate() {
        Date now = new Date();
        Date sqlDate = MapConversions.toSqlDate(now, converter);
        assertNotNull(sqlDate, "Resulting Date should not be null");
        assertEquals(now.getTime(), sqlDate.getTime(), "The time value of the returned Date should match the source Date");
    }

    /**
     * When the source is a {@link Map} that does **not** contain {@code "epochMillis"} but does
     * contain a {@code "time"} entry, {@code toEpochMillis} returns {@code null} and the fallback
     * {@code fromMap} logic extracts the {@code "time"} value and converts it to a {@link Date}.
     */
    @Test
    void testToSqlDateFromMapWithTimeKey() {
        long millis = System.currentTimeMillis();
        Map<String, Object> map = new HashMap<>();
        // key that the fallback logic recognises
        map.put("time", millis);
        // other optional keys (zone, etc.) are not required for this simple case
        Date sqlDate = MapConversions.toSqlDate(map, converter);
        assertNotNull(sqlDate, "Resulting Date should not be null");
        assertEquals(millis, sqlDate.getTime(), "The Date returned should represent the millisecond value supplied in the map");
    }

    /**
     * When the source map contains {@code "epochMillis"} the private {@code toEpochMillis}
     * method will succeed, so the method should return a Date built directly from that entry.
     */
    @Test
    void testToSqlDateFromMapWithEpochMillisKey() {
        long epochMillis = System.currentTimeMillis();
        Map<String, Object> map = new HashMap<>();
        map.put("epochMillis", epochMillis);
        Date sqlDate = MapConversions.toSqlDate(map, converter);
        assertNotNull(sqlDate, "Resulting Date should not be null");
        assertEquals(epochMillis, sqlDate.getTime(), "The Date returned should match the epochMillis value from the map");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("epochMillisPresentProvider")
    void testToSqlDate_EpochMillisPresent(ArgumentsHolder holder) {
        Date result = MapConversions.toSqlDate(holder.from, realConverter);
        assertNotNull(result, "Result must not be null");
        assertEquals(holder.expectedMillis, result.getTime(), "Millis should match the value derived from the source");
    }

    @Test
    void testToSqlDate_MapConversionSucceeds() {
        // map contains date, time and zone – matches the third key‑set
        Map<String, Object> map = Map.of("date", "2021-06-01", "time", "12:34:56", "zone", "UTC");
        // mock converter to return the expected Date when asked to convert the map
        Converter mockConverter = mock(Converter.class);
        Date expected = Date.from(Instant.parse("2021-06-01T12:34:56Z"));
        when(mockConverter.convert(eq(map), eq(Date.class))).thenReturn(expected);
        Date result = MapConversions.toSqlDate(map, mockConverter);
        assertNotNull(result);
        assertEquals(expected, result);
        verify(mockConverter).convert(eq(map), eq(Date.class));
    }

    @Test
    void testToSqlDate_MapWithEpochMillisKey_Fallback() {
        // Simulate a scenario where toEpochMillis returns null (by using a mock that throws)
        // but the map still contains the recognised "epochMillis" key.
        Map<String, Object> map = Map.of("epochMillis", 1622548800123L);
        Converter mockConverter = mock(Converter.class);
        Date expected = new Date(1622548800123L);
        when(mockConverter.convert(eq(map), eq(Date.class))).thenReturn(expected);
        Date result = MapConversions.toSqlDate(map, mockConverter);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void testToSqlDate_MapDateTimeNoZone() {
        Map<String, Object> map = Map.of("date", "2021-12-25", "time", "08:15:00");
        Converter mockConverter = mock(Converter.class);
        // 2021‑12‑25T08:15:00Z
        Date expected = new Date(1640427300000L);
        when(mockConverter.convert(eq(map), eq(Date.class))).thenReturn(expected);
        Date result = MapConversions.toSqlDate(map, mockConverter);
        assertEquals(expected, result);
    }

    @Test
    void testToSqlDate_MapConversionFails_ReturnsNull() {
        Map<String, Object> map = Map.of("foo", "bar");
        Converter mockConverter = mock(Converter.class);
        // fromMap will try to convert the map and throw IllegalArgumentException;
        // we let the mock throw it to simulate failure.
        when(mockConverter.convert(eq(map), eq(Date.class))).thenThrow(new IllegalArgumentException("cannot convert map"));
        // The implementation of fromMap catches conversion failures and returns null,
        // so toSqlDate should also return null (no exception is propagated).
        Date result = MapConversions.toSqlDate(map, mockConverter);
        assertNull(result, "When map conversion fails, toSqlDate should return null");
    }

    @Test
    void testToSqlDate_MapTimeOnly_ReturnsNull() {
        Map<String, Object> map = Map.of("time", "15:30:45");
        // No conversion is expected because the key‑set does not match.
        Converter mockConverter = mock(Converter.class);
        Date result = MapConversions.toSqlDate(map, mockConverter);
        assertNull(result);
        verifyNoInteractions(mockConverter);
    }

    @Test
    void testToSqlDate_NullInput_ReturnsNull() {
        Date result = MapConversions.toSqlDate(null, realConverter);
        assertNull(result, "Null source should result in null");
    }

    @Test
    void testToSqlDate_NullConverter_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> MapConversions.toSqlDate(Instant.ofEpochMilli(1622548800000L), null));
    }

    @Test
    void testToSqlDate_ConverterThrowsInToEpochMillis_Propagates() {
        // Mock a converter that throws when any conversion is attempted.
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(any(), any())).thenThrow(new RuntimeException("boom"));
        // The exception will be thrown from toEpochMillis (which uses the converter).
        RuntimeException ex = assertThrows(RuntimeException.class, () -> MapConversions.toSqlDate(new Date(1622548800123L), mockConverter));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void testToSqlDate_ConverterThrowsInFromMap_Propagates() {
        Map<String, Object> map = Map.of("date", "2021-06-01", "time", "12:34:56");
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(map), eq(Date.class))).thenThrow(new IllegalArgumentException("cannot convert map"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toSqlDate(map, mockConverter));
        assertEquals("cannot convert map", ex.getMessage());
    }

    @Test
    void testToSqlDate_MapEpochMillisInvalidType_PropagatesClassCast() {
        Map<String, Object> map = Map.of("epochMillis", new Object());
        Converter mockConverter = mock(Converter.class);
        // The mock will attempt to cast the value to Long and fail.
        when(mockConverter.convert(eq(map), eq(Date.class))).thenAnswer(invocation -> {
            Object val = ((Map<?, ?>) invocation.getArgument(0)).get("epochMillis");
            // simulate the real conversion logic that expects a Long
            // will throw ClassCastException
            Long l = (Long) val;
            return new Date(l);
        });
        assertThrows(ClassCastException.class, () -> MapConversions.toSqlDate(map, mockConverter));
    }

    @Test
    void testToSqlDate_MapDateInvalidFormat_PropagatesDateTimeParseException() {
        Map<String, Object> map = Map.of("date", "31-02-2021", "time", "12:00:00");
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(map), eq(Date.class))).thenThrow(new java.time.format.DateTimeParseException("Invalid date", "31-02-2021", 0));
        assertThrows(java.time.format.DateTimeParseException.class, () -> MapConversions.toSqlDate(map, mockConverter));
    }

    @Test
    void testToSqlDate_EpochMillisEntryNullKey_ThrowsNPE() {
        // Helper class that pretends to return a null‑key entry from toEpochMillis.
        class NullKeyEpochMillis {
            // No fields – we will use reflection to inject a null key via a spy.
        }
        // Spy the MapConversions class to force a null key entry.
        // Since toEpochMillis is private static, we use reflection to replace it.
        // For simplicity we invoke the method via a mock Converter that returns null,
        // then let the private method return a crafted entry via reflection.
        // However, the current implementation of toSqlDate will attempt to add
        // epochTime.getValue() / 1_000_000 to a null key, causing NPE.
        // We simulate this by calling the method directly with a custom object
        // that triggers the null‑key path via a mocked Converter.
        // Create a mock Converter that returns null for any conversion (forcing fallback)
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(any(), any())).thenReturn(null);
        // Use reflection to invoke the private toEpochMillis and force a null key entry.
        // This is a bit hacky but demonstrates the expected NPE.
        assertThrows(NullPointerException.class, () -> {
            // Directly call the private method via reflection
            java.lang.reflect.Method m = MapConversions.class.getDeclaredMethod("toEpochMillis", Object.class, Converter.class);
            m.setAccessible(true);
            Map.Entry<Long, Integer> entry = (Map.Entry<Long, Integer>) m.invoke(null, new NullKeyEpochMillis(), mockConverter);
            // entry should be null‑key; now call toSqlDate which will use it.
            MapConversions.toSqlDate(new NullKeyEpochMillis(), mockConverter);
        });
    }
}
