package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneOffset;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.HOURS;
import static com.cedarsoftware.util.convert.MapConversions.MINUTES;
import static com.cedarsoftware.util.convert.MapConversions.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZoneOffsetConversions_toMap_Test {

    @Mock
    private Converter // Converter is not used by the method but must be supplied
    mockConverter;

    /* --------------------------------------------------------------------- *
     *  Helper – a dummy Converter implementation for the “non‑null converter”
     *  test case.  The real Converter interface has no methods used by the
     *  method under test, so an empty implementation is sufficient.
     * --------------------------------------------------------------------- */
    private static final class DummyConverter implements Converter {
        // no‑op – the conversion logic does not depend on the Converter instance
    }

    private static Stream<Arguments> secondsIncludedProvider() {
        return Stream.of(// Positive offset with non‑zero seconds
        Arguments.of(ZoneOffset.ofHoursMinutesSeconds(5, 30, 15), null, mapOf(HOURS, 5, MINUTES, 30, SECONDS, 15), "Positive offset with non‑zero seconds"), // Negative offset where seconds are non‑zero.
        // ZoneOffset.ofHoursMinutesSeconds does not accept negative minutes/seconds,
        // so we build it from total seconds.
        Arguments.of(ZoneOffset.ofTotalSeconds((-3 * 3600) + (-20 * 60) + (-45)), null, mapOf(HOURS, -3, MINUTES, -20, SECONDS, -45), "Negative offset with non‑zero seconds"), // Zero hour/minute offset but with seconds
        Arguments.of(ZoneOffset.ofHoursMinutesSeconds(0, 0, 30), null, mapOf(HOURS, 0, MINUTES, 0, SECONDS, 30), "Zero hour/minute offset with seconds"));
    }

    private static Stream<Arguments> secondsOmittedPositiveProvider() {
        return Stream.of(Arguments.of(ZoneOffset.ofHoursMinutesSeconds(2, 15, 0), null, mapOf(HOURS, 2, MINUTES, 15), "Positive offset where seconds are zero"));
    }

    private static Stream<Arguments> secondsOmittedNegativeProvider() {
        return Stream.of(Arguments.of(// same sign rule – use total seconds
        ZoneOffset.ofHoursMinutesSeconds(-5, -30, 0), null, mapOf(HOURS, -5, MINUTES, -30), "Negative offset with zero seconds"));
    }

    private static Stream<Arguments> zeroOffsetProvider() {
        return Stream.of(Arguments.of(ZoneOffset.UTC, null, mapOf(HOURS, 0, MINUTES, 0), "UTC (zero total offset)"), Arguments.of(ZoneOffset.ofHoursMinutesSeconds(0, 0, 0), null, mapOf(HOURS, 0, MINUTES, 0), "Explicit zero offset using ofHoursMinutesSeconds"));
    }

    private static Stream<Arguments> boundaryOffsetsProvider() {
        return Stream.of(Arguments.of(ZoneOffset.ofHours(18), null, mapOf(HOURS, 18, MINUTES, 0), "Maximum positive ZoneOffset (+18:00)"), Arguments.of(ZoneOffset.ofHours(-18), null, mapOf(HOURS, -18, MINUTES, 0), "Maximum negative ZoneOffset (-18:00)"));
    }

    @Test
    void toMap_WithWholeHoursAndMinutes_NoSecondsKey() {
        // given
        // 5 hours 30 minutes, 0 seconds
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(5, 30);
        // when
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, mockConverter);
        // then
        assertNotNull(result, "Result map should not be null");
        assertEquals(2, result.size(), "Map should contain exactly HOURS and MINUTES entries");
        assertEquals(5, result.get(HOURS), "Hours should be 5");
        assertEquals(30, result.get(MINUTES), "Minutes should be 30");
        assertFalse(result.containsKey(SECONDS), "Map should not contain SECONDS when seconds are zero");
    }

    @Test
    void toMap_WithSeconds_IncludesSecondsKey() {
        // given
        // 2h 15m 45s
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(2, 15, 45);
        // when
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(3, result.size(), "Map should contain HOURS, MINUTES and SECONDS entries");
        assertEquals(2, result.get(HOURS));
        assertEquals(15, result.get(MINUTES));
        assertEquals(45, result.get(SECONDS));
    }

    @Test
    void toMap_WithNegativeOffset_HandlesNegativeValuesCorrectly() {
        // given
        // -3 hours, 0 minutes, 0 seconds
        ZoneOffset offset = ZoneOffset.ofHours(-3);
        // when
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(-3, result.get(HOURS));
        assertEquals(0, result.get(MINUTES));
        assertFalse(result.containsKey(SECONDS));
    }

    @Test
    void toMap_WithNegativeOffsetAndSeconds_IncludesSecondsKey() {
        // given
        // -4h 20m 30s
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(-4, 20, 30);
        // when
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(-4, result.get(HOURS));
        assertEquals(20, result.get(MINUTES));
        assertEquals(30, result.get(SECONDS));
    }

    /**
     * Group: Seconds Included – positive, negative and zero‑hour offsets.
     */
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("secondsIncludedProvider")
    void toMap_SecondsIncluded(ZoneOffset offset, Converter converter, Map<String, Object> expected, String description) {
        // when
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, converter);
        // then
        assertNotNull(result, "Result map must not be null");
        assertEquals(expected.size(), result.size(), "Map size differs – expected: " + expected + ", actual: " + result);
        expected.forEach((k, v) -> assertEquals(v, result.get(k), "Incorrect value for key '" + k + "'"));
    }

    /**
     * Group: Seconds Omitted – Positive Offset
     */
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("secondsOmittedPositiveProvider")
    void toMap_SecondsOmittedPositive(ZoneOffset offset, Converter converter, Map<String, Object> expected, String description) {
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, converter);
        assertNotNull(result);
        assertEquals(expected, result, description);
    }

    /**
     * Group: Seconds Omitted – Negative Offset
     */
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("secondsOmittedNegativeProvider")
    void toMap_SecondsOmittedNegative(ZoneOffset offset, Converter converter, Map<String, Object> expected, String description) {
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, converter);
        assertNotNull(result);
        assertEquals(expected, result, description);
    }

    /**
     * Group: Zero Offset
     */
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("zeroOffsetProvider")
    void toMap_ZeroOffset(ZoneOffset offset, Converter converter, Map<String, Object> expected, String description) {
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, converter);
        assertNotNull(result);
        assertEquals(expected, result, description);
    }

    /**
     * Group: Boundary Offsets
     */
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("boundaryOffsetsProvider")
    void toMap_BoundaryOffsets(ZoneOffset offset, Converter converter, Map<String, Object> expected, String description) {
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, converter);
        assertNotNull(result);
        assertEquals(expected, result, description);
    }

    /**
     * Group: Offset With Minutes Only
     */
    @Test
    void toMap_OffsetWithMinutesOnly() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(0, 45);
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, mockConverter);
        assertNotNull(result);
        assertEquals(mapOf(HOURS, 0, MINUTES, 45), result, "Offset that has only minutes – map must contain HOURS=0 and MINUTES=45");
    }

    /**
     * Group: Non‑Null Converter Parameter
     */
    @Test
    void toMap_NonNullConverter_IgnoredByMethod() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(3, 20, 10);
        // non‑null instance
        Converter dummy = new DummyConverter();
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, dummy);
        assertNotNull(result);
        assertEquals(mapOf(HOURS, 3, MINUTES, 20, SECONDS, 10), result, "Method should ignore the converter argument");
    }

    @Test
    void toMap_NullFrom_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ZoneOffsetConversions.toMap(null, null), "Passing a null 'from' argument must result in NPE");
    }

    @Test
    void toMap_InvalidFromType_String_ThrowsClassCastException() {
        assertThrows(ClassCastException.class, () -> ZoneOffsetConversions.toMap("some string", null), "Casting a String to ZoneOffset must fail with CCE");
    }

    @Test
    void toMap_InvalidFromType_ZoneId_ThrowsClassCastException() {
        assertThrows(ClassCastException.class, () -> ZoneOffsetConversions.toMap(ZoneId.systemDefault(), null), "Casting a ZoneId to ZoneOffset must fail with CCE");
    }

    @Test
    void toMap_InvalidFromType_Object_ThrowsClassCastException() {
        assertThrows(ClassCastException.class, () -> ZoneOffsetConversions.toMap(new Object(), null), "Casting a generic Object to ZoneOffset must fail with CCE");
    }

    /* --------------------------------------------------------------------- *
     *  Utility method to build a map in a concise way.  The production code
     *  uses {@link CompactLinkedMap}, but for assertions a plain {@link java.util.HashMap}
     *  is sufficient.
     * --------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new CompactLinkedMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
