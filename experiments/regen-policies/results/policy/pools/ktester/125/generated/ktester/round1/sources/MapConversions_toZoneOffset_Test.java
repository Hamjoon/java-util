package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DateTimeException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toZoneOffset_Test {

    /**
     * The Converter used in the tests – it is mocked so we can verify the
     *  exact interactions performed by {@code MapConversions.toZoneOffset}.
     */
    @Mock
    Converter converter;

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// Map only provides the required HOURS entry. MINUTES and SECONDS default to 0.
        Arguments.of(mapOf(MapConversions.HOURS, 5), ZoneOffset.ofHours(5), "Map only provides HOURS – minutes/seconds default to 0"), // Map provides HOURS and MINUTES. SECONDS defaults to 0.
        Arguments.of(mapOf(MapConversions.HOURS, -3, MapConversions.MINUTES, 30), ZoneOffset.ofHoursMinutes(-3, 30), "Map provides HOURS and MINUTES – seconds default to 0"), // All three components are present.
        Arguments.of(mapOf(MapConversions.HOURS, 2, MapConversions.MINUTES, 15, MapConversions.SECONDS, 45), ZoneOffset.ofHoursMinutesSeconds(2, 15, 45), "All components present – full precision offset"));
    }

    private static Stream<Arguments> fallbackErrorProvider() {
        return Stream.of(Arguments.of(mapOf(MapConversions.MINUTES, 30, MapConversions.SECONDS, 15), "Missing HOURS key and also missing required singular hour"), Arguments.of(mapOf(), "Empty map – required hour key missing"));
    }

    private static Stream<Arguments> stringValueProvider() {
        return Stream.of(Arguments.of(mapOf(MapConversions.HOURS, "2", MapConversions.MINUTES, "45", MapConversions.SECONDS, "10"), ZoneOffset.ofHoursMinutesSeconds(2, 45, 10), "All components supplied as strings"), Arguments.of(mapOf(MapConversions.HOURS, "-5", MapConversions.MINUTES, "15"), ZoneOffset.ofHoursMinutes(-5, 15), "Negative hour and minute supplied as strings"));
    }

    private static Stream<Arguments> hourOutOfRangeProvider() {
        return Stream.of(Arguments.of(mapOf(MapConversions.HOURS, 20), "Hour 20 exceeds +18 limit"), Arguments.of(mapOf(MapConversions.HOURS, -19, MapConversions.MINUTES, 0), "Hour -19 exceeds -18 limit"));
    }

    private static Stream<Arguments> minuteSecondOutOfRangeProvider() {
        return Stream.of(Arguments.of(mapOf(MapConversions.HOURS, 5, MapConversions.MINUTES, 70), "Invalid minute value 70"), Arguments.of(mapOf(MapConversions.HOURS, -3, MapConversions.MINUTES, 15, MapConversions.SECONDS, 80), "Invalid second value 80"));
    }

    private static Stream<Arguments> nullOptionalProvider() {
        return Stream.of(Arguments.of(mapOf(MapConversions.HOURS, 3, MapConversions.MINUTES, null), ZoneOffset.ofHours(3), "Minute null – defaults to 0"), Arguments.of(mapOf(MapConversions.HOURS, -2, MapConversions.SECONDS, null), ZoneOffset.ofHours(-2), "Second null – defaults to 0 (minutes also default to 0)"));
    }

    /**
     * Verifies the branch that is taken when the source map contains the key
     * {@code "hours"} (the plural form).  The method should call the converter
     * three times (hours, minutes, seconds) and build the {@link ZoneOffset}
     * using {@code ZoneOffset.ofHoursMinutesSeconds}.
     */
    @Test
    void toZoneOffset_usesHoursKey() {
        // GIVEN – a map containing the plural keys and a mocked Converter
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOURS, 5);
        map.put(MapConversions.MINUTES, 30);
        map.put(MapConversions.SECONDS, 15);
        // Stub the converter to simply return the supplied Integer as int
        when(converter.convert(map.get(MapConversions.HOURS), int.class)).thenReturn(5);
        when(converter.convert(map.get(MapConversions.MINUTES), int.class)).thenReturn(30);
        when(converter.convert(map.get(MapConversions.SECONDS), int.class)).thenReturn(15);
        // WHEN – the method under test is invoked
        ZoneOffset result = MapConversions.toZoneOffset(map, converter);
        // THEN – the correct ZoneOffset is produced and the converter is called as expected
        assertEquals(ZoneOffset.ofHoursMinutesSeconds(5, 30, 15), result);
        verify(converter).convert(map.get(MapConversions.HOURS), int.class);
        verify(converter).convert(map.get(MapConversions.MINUTES), int.class);
        verify(converter).convert(map.get(MapConversions.SECONDS), int.class);
        verifyNoMoreInteractions(converter);
    }

    /**
     * Verifies the fallback path when the source map does **not** contain the
     * {@code "hours"} key.  In this case the method delegates to the private
     * {@code fromMap} helper, which ultimately extracts the singular {@code "hour"}
     * key (and optional minute/second keys).  The test supplies only an hour
     * value and expects the resulting {@link ZoneOffset} to have minutes and
     * seconds defaulted to zero.
     */
    @Test
    void toZoneOffset_usesHourKeyFallback() {
        // GIVEN – a map that lacks the plural "hours" key but contains the singular "hour"
        Map<String, Object> map = new HashMap<>();
        // negative offset is allowed
        map.put(MapConversions.HOUR, -3);
        // The real Converter implementation correctly handles Integer → int conversion,
        // so we can use the real class for this branch to avoid stubbing every optional key.
        Converter realConverter = new Converter(new ConverterOptions());
        // WHEN
        ZoneOffset result = MapConversions.toZoneOffset(map, realConverter);
        // THEN – minutes and seconds default to 0
        assertEquals(ZoneOffset.ofHours(-3), result);
    }

    /**
     * Verifies that when the map contains the singular {@code "hour"} key together
     * with optional minute and second entries, those values are honoured.
     */
    @Test
    void toZoneOffset_usesHourKeyWithOptionalMinutesAndSeconds() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 2);
        map.put(MapConversions.MINUTE, 45);
        map.put(MapConversions.SECOND, 10);
        Converter realConverter = new Converter(new ConverterOptions());
        // WHEN
        ZoneOffset result = MapConversions.toZoneOffset(map, realConverter);
        // THEN
        assertEquals(ZoneOffset.ofHoursMinutesSeconds(2, 45, 10), result);
    }

    /**
     * Normal conversions where the map contains the plural {@code "hours"} key.
     * The {@link Converter} simply returns the supplied value (or parses a string).
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("normalConversionProvider")
    void toZoneOffset_normalConversions_usingHoursKey(Map<String, Object> map, ZoneOffset expected, String description) {
        // Mock a tolerant converter that returns the raw integer (or parses a string) and
        // treats null as 0 – this mirrors the behaviour of the simple TestConverter.
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(int.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg == null)
                return 0;
            if (arg instanceof Integer)
                return arg;
            if (arg instanceof String)
                return Integer.parseInt((String) arg);
            throw new IllegalArgumentException("Unsupported type");
        });
        ZoneOffset result = MapConversions.toZoneOffset(map, mock);
        assertEquals(expected, result, description);
    }

    /**
     * Fallback path when the plural {@code "hours"} key is missing.
     * The method should delegate to {@code fromMap} and throw an {@link IllegalArgumentException}
     * if the required singular {@code "hour"} key is absent.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("fallbackErrorProvider")
    void toZoneOffset_fallback_missingHoursKey_throwsIllegalArgumentException(Map<String, Object> map, String description) {
        Converter realConverter = new Converter(new ConverterOptions());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toZoneOffset(map, realConverter), description);
        assertTrue(ex.getMessage().toLowerCase().contains("hour"), "Exception message should mention missing 'hour' key");
    }

    /**
     * Conversion where the map supplies numeric values as {@link String}s.
     * The mocked {@link Converter} parses the strings to {@code int}.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("stringValueProvider")
    void toZoneOffset_stringValues_parsedByConverter(Map<String, Object> map, ZoneOffset expected, String description) {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(int.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg == null)
                return 0;
            if (arg instanceof String)
                return Integer.parseInt((String) arg);
            if (arg instanceof Integer)
                return arg;
            throw new IllegalArgumentException("Unsupported type");
        });
        ZoneOffset result = MapConversions.toZoneOffset(map, mock);
        assertEquals(expected, result, description);
    }

    /**
     * Hour values outside the allowed {@code ZoneOffset} range (-18 … +18) should
     * cause a {@link DateTimeException}.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("hourOutOfRangeProvider")
    void toZoneOffset_hourOutOfRange_throwsDateTimeException(Map<String, Object> map, String description) {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(int.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof Integer)
                return arg;
            if (arg instanceof String)
                return Integer.parseInt((String) arg);
            throw new IllegalArgumentException("Unsupported type");
        });
        assertThrows(DateTimeException.class, () -> MapConversions.toZoneOffset(map, mock), description);
    }

    /**
     * Minute or second values outside the 0‑59 range should also result in a
     * {@link DateTimeException}.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("minuteSecondOutOfRangeProvider")
    void toZoneOffset_minuteSecondOutOfRange_throwsDateTimeException(Map<String, Object> map, String description) {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(int.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof Integer)
                return arg;
            if (arg instanceof String)
                return Integer.parseInt((String) arg);
            throw new IllegalArgumentException("Unsupported type");
        });
        assertThrows(DateTimeException.class, () -> MapConversions.toZoneOffset(map, mock), description);
    }

    /**
     * Null values for optional keys (minute / second) should be treated as 0
     * when a null‑tolerant converter is used.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("nullOptionalProvider")
    void toZoneOffset_nullOptionalValues_treatedAsZero(Map<String, Object> map, ZoneOffset expected, String description) {
        // Null‑tolerant mock: returns 0 for null arguments.
        Converter tolerantMock = mock(Converter.class);
        when(tolerantMock.convert(any(), eq(int.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg == null)
                return 0;
            if (arg instanceof Integer)
                return arg;
            if (arg instanceof String)
                return Integer.parseInt((String) arg);
            throw new IllegalArgumentException("Unsupported type");
        });
        ZoneOffset result = MapConversions.toZoneOffset(map, tolerantMock);
        assertEquals(expected, result, description);
    }

    /**
     * Maps that contain unrelated keys should be ignored; only the relevant
     * hour/minute/second entries are used.
     */
    @Test
    void toZoneOffset_extraUnrelatedKeys_areIgnored() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOURS, 4);
        map.put(MapConversions.MINUTES, 20);
        map.put("foo", "bar");
        map.put("baz", 123);
        // Simple mock that just returns the integer values.
        when(converter.convert(any(), eq(int.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof Integer)
                return arg;
            throw new IllegalArgumentException("Unexpected value");
        });
        ZoneOffset result = MapConversions.toZoneOffset(map, converter);
        assertEquals(ZoneOffset.ofHoursMinutes(4, 20), result);
    }

    /**
     * When the {@code Converter} itself throws a {@link NumberFormatException}
     * (e.g., parsing a non‑numeric string), the exception should propagate.
     */
    @Test
    void toZoneOffset_converterThrowsNumberFormatException() {
        Map<String, Object> map = mapOf(MapConversions.HOURS, "notANumber");
        when(converter.convert(any(), eq(int.class))).thenThrow(NumberFormatException.class);
        assertThrows(NumberFormatException.class, () -> MapConversions.toZoneOffset(map, converter), "Converter should propagate NumberFormatException");
    }

    /**
     * When the {@code Converter} throws an {@link IllegalArgumentException}
     * for an invalid minute value, the exception should propagate.
     */
    @Test
    void toZoneOffset_converterThrowsIllegalArgumentExceptionForMinute() {
        Map<String, Object> map = mapOf(MapConversions.HOURS, 3, MapConversions.MINUTES, "abc");
        // Hour parses fine, minute triggers the exception.
        when(converter.convert(eq(3), eq(int.class))).thenReturn(3);
        when(converter.convert(eq("abc"), eq(int.class))).thenThrow(IllegalArgumentException.class);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toZoneOffset(map, converter), "Converter should propagate IllegalArgumentException for minute");
    }

    /**
     * A null hour value with a non‑tolerant converter should result in an
     * {@link IllegalArgumentException} from the converter.
     */
    @Test
    void toZoneOffset_nullHourWithStrictConverter_throwsIllegalArgumentException() {
        Map<String, Object> map = mapOf(MapConversions.HOURS, null);
        when(converter.convert(isNull(), eq(int.class))).thenThrow(IllegalArgumentException.class);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toZoneOffset(map, converter), "Strict converter should reject null hour");
    }

    /**
     * Passing a {@code null} source map should result in a {@link NullPointerException}
     * because the method attempts to cast and call {@code containsKey} on it.
     */
    @Test
    void toZoneOffset_nullFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toZoneOffset(null, converter), "Null source map should cause NPE");
    }

    /**
     * Passing a non‑Map object as the source argument should cause a
     * {@link ClassCastException} during the initial cast.
     */
    @Test
    void toZoneOffset_nonMapFrom_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> MapConversions.toZoneOffset("just a string", converter), "Non‑Map source should cause ClassCastException");
    }

    /**
     * Passing a {@code null} converter should cause a {@link NullPointerException}
     * when the method attempts to invoke {@code converter.convert(...)}.
     */
    @Test
    void toZoneOffset_nullConverter_throwsNullPointerException() {
        Map<String, Object> map = mapOf(MapConversions.HOURS, 2);
        assertThrows(NullPointerException.class, () -> MapConversions.toZoneOffset(map, null), "Null converter should cause NPE");
    }

    /**
     * Convenient var‑args map builder used by the parameterised tests.
     *
     * @param entries alternating key/value pairs
     * @return a mutable {@link HashMap} containing the supplied entries
     */
    @SafeVarargs
    private static Map<String, Object> mapOf(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Even number of arguments required (key/value pairs)");
        }
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put((String) entries[i], entries[i + 1]);
        }
        return map;
    }
}
