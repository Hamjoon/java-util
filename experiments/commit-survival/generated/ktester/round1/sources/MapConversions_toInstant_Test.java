package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toInstant_Test {

    @Mock
    Converter // Mockito will create a mock Converter
    mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to avoid cross‑test contamination.
        reset(mockConverter);
    }

    /**
     * -----------------------------------------------------------------
     *  Test case 1 – map contains both "seconds" and "nanos".
     *  The method should delegate to the Converter for both values and
     *  construct the correct Instant via Instant.ofEpochSecond.
     *  -----------------------------------------------------------------
     */
    @Test
    void toInstant_WithSecondsAndNanos_ReturnsCorrectInstant() {
        // GIVEN
        // an arbitrary epoch second
        long expectedSeconds = 1_600_000_000L;
        // arbitrary nanosecond adjustment
        long expectedNanos = 123_456_789L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, expectedSeconds);
        map.put(MapConversions.NANOS, expectedNanos);
        // Stub the converter to return the supplied values when asked for a long.
        when(mockConverter.convert(expectedSeconds, long.class)).thenReturn(expectedSeconds);
        when(mockConverter.convert(expectedNanos, long.class)).thenReturn(expectedNanos);
        // WHEN
        Instant result = MapConversions.toInstant(map, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Instant must not be null");
        assertEquals(Instant.ofEpochSecond(expectedSeconds, expectedNanos), result, "Instant should be built from the provided seconds and nanos");
        // Verify that the converter was called exactly once for each key.
        verify(mockConverter, times(1)).convert(expectedSeconds, long.class);
        verify(mockConverter, times(1)).convert(expectedNanos, long.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * -----------------------------------------------------------------
     *  Test case 2 – map contains "seconds" but no "nanos".
     *  The method should still call the converter for the missing entry;
     *  the stub returns 0 for a null value, resulting in an Instant with
     *  zero nanos.
     *  -----------------------------------------------------------------
     */
    @Test
    void toInstant_WithSecondsOnly_NanosDefaultedToZero() {
        // GIVEN
        long expectedSeconds = 1_600_000_001L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, expectedSeconds);
        // "nanos" key intentionally omitted
        when(mockConverter.convert(expectedSeconds, long.class)).thenReturn(expectedSeconds);
        // Converter is asked for a null value; we simulate it returning 0.
        when(mockConverter.convert(null, long.class)).thenReturn(0L);
        // WHEN
        Instant result = MapConversions.toInstant(map, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(Instant.ofEpochSecond(expectedSeconds, 0L), result, "When nanos are absent they should default to 0");
        verify(mockConverter, times(1)).convert(expectedSeconds, long.class);
        verify(mockConverter, times(1)).convert(null, long.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toInstant_WithStringNanos_ConverterParsesString() {
        long seconds = 42L;
        String nanosStr = "123456789";
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, seconds);
        map.put(MapConversions.NANOS, nanosStr);
        when(mockConverter.convert(seconds, long.class)).thenReturn(seconds);
        when(mockConverter.convert(nanosStr, long.class)).thenReturn(123_456_789L);
        Instant result = MapConversions.toInstant(map, mockConverter);
        assertEquals(Instant.ofEpochSecond(seconds, 123_456_789L), result);
        verify(mockConverter).convert(seconds, long.class);
        verify(mockConverter).convert(nanosStr, long.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toInstant_WithNegativeValues() {
        long seconds = -10L;
        long nanos = -500L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, seconds);
        map.put(MapConversions.NANOS, nanos);
        when(mockConverter.convert(seconds, long.class)).thenReturn(seconds);
        when(mockConverter.convert(nanos, long.class)).thenReturn(nanos);
        Instant result = MapConversions.toInstant(map, mockConverter);
        assertEquals(Instant.ofEpochSecond(seconds, nanos), result);
        verify(mockConverter).convert(seconds, long.class);
        verify(mockConverter).convert(nanos, long.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toInstant_WithNanosOverflow_NormalisedByInstant() {
        long seconds = 1L;
        // 1.5 seconds in nanos
        long nanos = 1_500_000_000L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, seconds);
        map.put(MapConversions.NANOS, nanos);
        when(mockConverter.convert(seconds, long.class)).thenReturn(seconds);
        when(mockConverter.convert(nanos, long.class)).thenReturn(nanos);
        Instant result = MapConversions.toInstant(map, mockConverter);
        // Instant.ofEpochSecond normalises overflow: 1s + 1.5s => 2s + 0.5s
        assertEquals(Instant.ofEpochSecond(2L, 500_000_000L), result);
        verify(mockConverter).convert(seconds, long.class);
        verify(mockConverter).convert(nanos, long.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /* -----------------------------------------------------------------
     *  Group: seconds key present – exceptional paths
     * ----------------------------------------------------------------- */
    @Test
    void toInstant_SecondsStringConversionThrows_NumberFormatException() {
        String badSeconds = "not-a-number";
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, badSeconds);
        map.put(MapConversions.NANOS, 0L);
        when(mockConverter.convert(badSeconds, long.class)).thenThrow(new NumberFormatException("Invalid number"));
        // nanos conversion is not reached, but we stub it anyway
        when(mockConverter.convert(0L, long.class)).thenReturn(0L);
        assertThrows(NumberFormatException.class, () -> MapConversions.toInstant(map, mockConverter));
        verify(mockConverter).convert(badSeconds, long.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toInstant_ConverterReturnsNullForSeconds_NullPointerException() {
        long seconds = 5L;
        long nanos = 0L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, seconds);
        map.put(MapConversions.NANOS, nanos);
        // Return null for seconds – unboxing to primitive long triggers NPE
        when(mockConverter.convert(seconds, long.class)).thenReturn(null);
        // nanos conversion would not be reached, but stub it
        when(mockConverter.convert(nanos, long.class)).thenReturn(nanos);
        assertThrows(NullPointerException.class, () -> MapConversions.toInstant(map, mockConverter));
        verify(mockConverter).convert(seconds, long.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toInstant_ConverterReturnsNonLongForSeconds_ClassCastException() {
        long seconds = 7L;
        long nanos = 0L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, seconds);
        map.put(MapConversions.NANOS, nanos);
        // Return a Double instead of Long
        when(mockConverter.convert(seconds, long.class)).thenReturn(7.0);
        when(mockConverter.convert(nanos, long.class)).thenReturn(nanos);
        assertThrows(ClassCastException.class, () -> MapConversions.toInstant(map, mockConverter));
        verify(mockConverter).convert(seconds, long.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toInstant_NanosConversionThrows_NumberFormatException() {
        long seconds = 12L;
        String badNanos = "not-a-number";
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, seconds);
        map.put(MapConversions.NANOS, badNanos);
        when(mockConverter.convert(seconds, long.class)).thenReturn(seconds);
        when(mockConverter.convert(badNanos, long.class)).thenThrow(new NumberFormatException("Invalid nanos"));
        assertThrows(NumberFormatException.class, () -> MapConversions.toInstant(map, mockConverter));
        verify(mockConverter).convert(seconds, long.class);
        verify(mockConverter).convert(badNanos, long.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toInstant_NanosMissing_ConverterThrowsOnNull_IllegalArgumentException() {
        long seconds = 20L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, seconds);
        // NANOS omitted => null passed to converter
        when(mockConverter.convert(seconds, long.class)).thenReturn(seconds);
        when(mockConverter.convert(null, long.class)).thenThrow(new IllegalArgumentException("Nanos cannot be null"));
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toInstant(map, mockConverter));
        verify(mockConverter).convert(seconds, long.class);
        verify(mockConverter).convert(null, long.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /* -----------------------------------------------------------------
     *  Group: seconds key missing – fallback to fromMap
     * ----------------------------------------------------------------- */
    @Test
    void toInstant_SecondsMissing_EpochMillisLong_ReturnsInstantOfMilli() {
        // 2021‑01‑01T00:00:00.123Z
        long epochMillis = 1_609_459_200_123L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, epochMillis);
        when(mockConverter.convert(epochMillis, long.class)).thenReturn(epochMillis);
        Instant result = MapConversions.toInstant(map, mockConverter);
        assertEquals(Instant.ofEpochMilli(epochMillis), result);
        verify(mockConverter).convert(epochMillis, long.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toInstant_SecondsMissing_EpochMillisString_ConverterParsesString() {
        String epochStr = "1609459200123";
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, epochStr);
        when(mockConverter.convert(epochStr, long.class)).thenReturn(1_609_459_200_123L);
        Instant result = MapConversions.toInstant(map, mockConverter);
        assertEquals(Instant.ofEpochMilli(1_609_459_200_123L), result);
        verify(mockConverter).convert(epochStr, long.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toInstant_SecondsMissing_EpochMillisConversionThrows_NumberFormatException() {
        String badEpoch = "invalid-number";
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, badEpoch);
        when(mockConverter.convert(badEpoch, long.class)).thenThrow(new NumberFormatException("Cannot parse epochMillis"));
        assertThrows(NumberFormatException.class, () -> MapConversions.toInstant(map, mockConverter));
        verify(mockConverter).convert(badEpoch, long.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toInstant_SecondsMissing_UnrecognizableKeys_IllegalArgumentException() {
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");
        // The internal fromMap implementation is expected to throw an
        // IllegalArgumentException (or a subclass) when it cannot build
        // an Instant from the supplied map.
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toInstant(map, mockConverter));
        // No interaction with the converter is expected because the
        // method never reaches the conversion step.
        verifyNoInteractions(mockConverter);
    }

    /* -----------------------------------------------------------------
     *  Group: null arguments
     * ----------------------------------------------------------------- */
    @Test
    void toInstant_NullFrom_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toInstant(null, mockConverter));
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toInstant_NullConverter_ThrowsNullPointerException() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, 10L);
        map.put(MapConversions.NANOS, 0L);
        assertThrows(NullPointerException.class, () -> MapConversions.toInstant(map, null));
        // No interaction because the method fails before using the converter.
    }
}
