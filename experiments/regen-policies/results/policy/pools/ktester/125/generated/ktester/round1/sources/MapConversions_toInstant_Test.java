package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toInstant_Test {

    /* --------------------------------------------------------------------- */
    private static Converter createLenientLongConverter() {
        Converter converter = mock(Converter.class);
        when(converter.convert(any(), eq(long.class))).thenAnswer(invocation -> {
            Object src = invocation.getArgument(0);
            if (src == null) {
                return 0L;
            }
            if (src instanceof Number) {
                return ((Number) src).longValue();
            }
            if (src instanceof String) {
                return Long.parseLong((String) src);
            }
            throw new IllegalArgumentException("Unsupported source type: " + src);
        });
        return converter;
    }

    /**
     * Verifies that when both {@code seconds} and {@code nanos} keys are present,
     * the method returns the exact {@link Instant} built from those values.
     */
    @Test
    void toInstant_WithSecondsAndNanos_ReturnsCorrectInstant() {
        // GIVEN
        // 2021‑06‑01T12:00:00Z
        long seconds = 1_620_548_800L;
        long nanos = 123_456_789L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, seconds);
        map.put(MapConversions.NANOS, nanos);
        Converter converter = mock(Converter.class);
        when(converter.convert(seconds, long.class)).thenReturn(seconds);
        when(converter.convert(nanos, long.class)).thenReturn(nanos);
        // WHEN
        Instant result = MapConversions.toInstant(map, converter);
        // THEN
        assertEquals(Instant.ofEpochSecond(seconds, nanos), result, "Instant should be built from provided seconds and nanos");
        verify(converter).convert(seconds, long.class);
        verify(converter).convert(nanos, long.class);
        verifyNoMoreInteractions(converter);
    }

    /**
     * Verifies that when only {@code seconds} is supplied, the missing {@code nanos}
     * entry is treated as {@code 0} (the {@code Converter} returns 0 for a null source
     * when converting to a primitive {@code long}).
     */
    @Test
    void toInstant_WithSecondsOnly_NanosDefaultZero() {
        // GIVEN
        // 2021‑06‑01T12:00:00Z
        long seconds = 1_620_548_800L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, seconds);
        // No "nanos" entry
        Converter converter = mock(Converter.class);
        when(converter.convert(seconds, long.class)).thenReturn(seconds);
        // Converter should receive a null for the nanos key and return 0
        when(converter.convert(null, long.class)).thenReturn(0L);
        // WHEN
        Instant result = MapConversions.toInstant(map, converter);
        // THEN
        assertEquals(Instant.ofEpochSecond(seconds, 0L), result, "Instant should be built from seconds with nanos defaulting to 0");
        verify(converter).convert(seconds, long.class);
        verify(converter).convert(null, long.class);
        verifyNoMoreInteractions(converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toInstant_WithIntegerValues_ReturnsCorrectInstant() {
        // 2021‑01‑01T00:00:00Z
        Integer seconds = 1_609_459_200;
        Integer nanos = 123_456_789;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, seconds);
        map.put(MapConversions.NANOS, nanos);
        Converter converter = createLenientLongConverter();
        Instant result = MapConversions.toInstant(map, converter);
        assertEquals(Instant.ofEpochSecond(seconds.longValue(), nanos.longValue()), result);
        verify(converter).convert(seconds, long.class);
        verify(converter).convert(nanos, long.class);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toInstant_WithIntegerSecondsOnly_NanosDefaultZero() {
        Integer seconds = 1_609_459_200;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, seconds);
        Converter converter = createLenientLongConverter();
        Instant result = MapConversions.toInstant(map, converter);
        assertEquals(Instant.ofEpochSecond(seconds.longValue(), 0L), result);
        verify(converter).convert(seconds, long.class);
        verify(converter).convert(null, long.class);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toInstant_WithStringValues_ReturnsCorrectInstant() {
        String seconds = "1609459200";
        String nanos = "123456789";
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, seconds);
        map.put(MapConversions.NANOS, nanos);
        Converter converter = createLenientLongConverter();
        Instant result = MapConversions.toInstant(map, converter);
        assertEquals(Instant.ofEpochSecond(Long.parseLong(seconds), Long.parseLong(nanos)), result);
        verify(converter).convert(seconds, long.class);
        verify(converter).convert(nanos, long.class);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toInstant_WithStringSecondsOnly_NanosDefaultZero() {
        String seconds = "1609459200";
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, seconds);
        Converter converter = createLenientLongConverter();
        Instant result = MapConversions.toInstant(map, converter);
        assertEquals(Instant.ofEpochSecond(Long.parseLong(seconds), 0L), result);
        verify(converter).convert(seconds, long.class);
        verify(converter).convert(null, long.class);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toInstant_Fallback_EpochMillisCreatesInstant() {
        // 2021‑01‑01T00:00:00.123Z
        long epochMillis = 1_609_459_200_123L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, epochMillis);
        Converter converter = mock(Converter.class);
        when(converter.convert(epochMillis, long.class)).thenReturn(epochMillis);
        Instant result = MapConversions.toInstant(map, converter);
        assertEquals(Instant.ofEpochMilli(epochMillis), result);
        verify(converter).convert(epochMillis, long.class);
        verifyNoMoreInteractions(converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toInstant_EmptyMap_ThrowsIllegalArgumentException() {
        Map<String, Object> emptyMap = new HashMap<>();
        Converter converter = mock(Converter.class);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toInstant(emptyMap, converter));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toInstant_SecondsKeyNull_FallbackToEpochMillis() {
        long epochMillis = 1_609_459_200_123L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, null);
        map.put(MapConversions.EPOCH_MILLIS, epochMillis);
        Converter converter = mock(Converter.class);
        when(converter.convert(epochMillis, long.class)).thenReturn(epochMillis);
        Instant result = MapConversions.toInstant(map, converter);
        assertEquals(Instant.ofEpochMilli(epochMillis), result);
        verify(converter).convert(epochMillis, long.class);
        verifyNoMoreInteractions(converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toInstant_WithExtraKeys_IgnoresUnrelatedData() {
        long seconds = 1_609_459_200L;
        long nanos = 0L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, seconds);
        map.put(MapConversions.NANOS, nanos);
        map.put("randomKey", "ignoreMe");
        // should be ignored
        map.put(MapConversions.EPOCH_MILLIS, 9_999_999_999_999L);
        Converter converter = createLenientLongConverter();
        Instant result = MapConversions.toInstant(map, converter);
        assertEquals(Instant.ofEpochSecond(seconds, nanos), result);
        verify(converter).convert(seconds, long.class);
        verify(converter).convert(nanos, long.class);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toInstant_NanosOverflow_NormalisesToNextSecond() {
        long seconds = 0L;
        // 1.5 seconds in nanos
        long nanos = 1_500_000_000L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, seconds);
        map.put(MapConversions.NANOS, nanos);
        Converter converter = createLenientLongConverter();
        Instant result = MapConversions.toInstant(map, converter);
        assertEquals(Instant.ofEpochSecond(1, 500_000_000L), result);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toInstant_ConverterThrowsRuntimeException_Propagates() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, 1_609_459_200L);
        map.put(MapConversions.NANOS, 0L);
        Converter converter = mock(Converter.class);
        when(converter.convert(any(), eq(long.class))).thenThrow(new RuntimeException("Conversion failed"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> MapConversions.toInstant(map, converter));
        assertEquals("Conversion failed", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toInstant_NonMapFrom_ThrowsClassCastException() {
        String notAMap = "I am not a map";
        Converter converter = mock(Converter.class);
        assertThrows(ClassCastException.class, () -> MapConversions.toInstant(notAMap, converter));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toInstant_NullFrom_ThrowsNullPointerException() {
        Converter converter = mock(Converter.class);
        assertThrows(NullPointerException.class, () -> MapConversions.toInstant(null, converter));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toInstant_ConverterThrowsNumberFormatException_Propagates() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, "notANumber");
        map.put(MapConversions.NANOS, "123");
        Converter converter = mock(Converter.class);
        when(converter.convert("notANumber", long.class)).thenThrow(new NumberFormatException("For input string: \"notANumber\""));
        // nanos conversion would not be reached, but we can stub it anyway
        when(converter.convert("123", long.class)).thenReturn(123L);
        assertThrows(NumberFormatException.class, () -> MapConversions.toInstant(map, converter));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toInstant_InstantOverflow_ThrowsDateTimeException() {
        // far beyond Instant range
        long overflowSeconds = Long.MAX_VALUE;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, overflowSeconds);
        map.put(MapConversions.NANOS, 0L);
        Converter converter = createLenientLongConverter();
        assertThrows(DateTimeException.class, () -> MapConversions.toInstant(map, converter));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toInstant_EpochMillisConversionFailure_ThrowsNumberFormatException() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, "invalid");
        Converter converter = mock(Converter.class);
        when(converter.convert("invalid", long.class)).thenThrow(new NumberFormatException("For input string: \"invalid\""));
        assertThrows(NumberFormatException.class, () -> MapConversions.toInstant(map, converter));
    }
}
