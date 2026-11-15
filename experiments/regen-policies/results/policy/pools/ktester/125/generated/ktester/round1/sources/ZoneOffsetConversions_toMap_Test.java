package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.MapConversions;
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
import java.time.ZoneId;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ZoneOffsetConversions_toMap_Test {

    @Mock
    private Converter // the method does not use the converter, a mock is sufficient
    mockConverter;

    private static Stream<org.junit.jupiter.params.provider.Arguments> validOffsetProvider() {
        return Stream.of(// Zero offset (UTC)
        arguments("Zero offset (UTC)", ZoneOffset.UTC, Map.of(HOURS, 0, MINUTES, 0)), // Positive offset with seconds
        arguments("Positive offset with seconds", ZoneOffset.ofHoursMinutesSeconds(5, 30, 45), Map.of(HOURS, 5, MINUTES, 30, SECONDS, 45)), // Negative offset with seconds
        arguments("Negative offset with seconds", ZoneOffset.ofHoursMinutesSeconds(-5, -30, -45), Map.of(HOURS, -5, MINUTES, -30, SECONDS, -45)), // Positive offset without seconds (hour only)
        arguments("Positive offset hour‑only", ZoneOffset.ofHours(2), Map.of(HOURS, 2, MINUTES, 0)), // Negative offset without seconds (hour only)
        arguments("Negative offset hour‑only", ZoneOffset.ofHours(-3), Map.of(HOURS, -3, MINUTES, 0)), // Maximum allowed offset (+18:00)
        arguments("Maximum allowed offset", ZoneOffset.MAX, Map.of(HOURS, 18, MINUTES, 0)), // Minimum allowed offset (-18:00)
        arguments("Minimum allowed offset", ZoneOffset.MIN, Map.of(HOURS, -18, MINUTES, 0)), // Offset with only minutes (positive)
        arguments("Offset with only minutes (positive)", // +00:45:00
        ZoneOffset.ofTotalSeconds(45 * 60), Map.of(HOURS, 0, MINUTES, 45)), // Offset with only minutes (negative)
        arguments("Offset with only minutes (negative)", // -00:30:00
        ZoneOffset.ofTotalSeconds(-30 * 60), Map.of(HOURS, 0, MINUTES, -30)));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidSourceProvider() {
        return Stream.of(arguments("String \"UTC\"", "UTC"), arguments("ZoneId.of(\"UTC\")", ZoneId.of("UTC")), arguments("Integer 0", Integer.valueOf(0)), arguments("Plain Object", new Object()));
    }

    @Test
    void toMap_withoutSeconds_returnsHoursAndMinutesOnly() {
        // given
        // 5:30, 0 seconds
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(5, 30);
        // when
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, mockConverter);
        // then
        assertNotNull(result, "Result map should not be null");
        assertEquals(2, result.size(), "Map should contain only hours and minutes");
        assertEquals(5, result.get(HOURS), "Hours value is incorrect");
        assertEquals(30, result.get(MINUTES), "Minutes value is incorrect");
        assertFalse(result.containsKey(SECONDS), "Seconds key should not be present when seconds are zero");
    }

    @Test
    void toMap_withSeconds_includesSecondsKey() {
        // given
        // 2:15:45
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(2, 15, 45);
        // when
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, mockConverter);
        // then
        assertNotNull(result, "Result map should not be null");
        assertEquals(3, result.size(), "Map should contain hours, minutes, and seconds");
        assertEquals(2, result.get(HOURS), "Hours value is incorrect");
        assertEquals(15, result.get(MINUTES), "Minutes value is incorrect");
        assertEquals(45, result.get(SECONDS), "Seconds value is incorrect");
    }

    @Test
    void toMap_negativeOffset_includesCorrectValues() {
        // given
        // -03:20:15
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(-3, -20, -15);
        // when
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(-3, result.get(HOURS));
        assertEquals(-20, result.get(MINUTES));
        assertEquals(-15, result.get(SECONDS));
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for all *valid* ZoneOffset inputs
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validOffsetProvider")
    void toMap_validOffsets(String description, ZoneOffset offset, Map<String, Object> expected) {
        // when
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, mockConverter);
        // then
        assertNotNull(result, "Result map must not be null");
        assertEquals(expected.size(), result.size(), "Map size differs – expected keys: " + expected.keySet());
        // verify each expected entry
        expected.forEach((k, v) -> assertEquals(v, result.get(k), () -> "Incorrect value for key '" + k + "'"));
        // ensure no unexpected SECONDS entry when seconds are zero
        if (!expected.containsKey(SECONDS)) {
            assertFalse(result.containsKey(SECONDS), "SECONDS key should be absent when seconds are zero");
        }
    }

    @Test
    void toMap_nullSource_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class, () -> ZoneOffsetConversions.toMap(null, mockConverter), "Passing null as the source should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] Invalid source type: {0}")
    @MethodSource("invalidSourceProvider")
    void toMap_invalidSource_throwsClassCastException(String description, Object invalidSource) {
        // when / then
        assertThrows(ClassCastException.class, () -> ZoneOffsetConversions.toMap(invalidSource, mockConverter), "Invalid source (" + description + ") should cause ClassCastException");
    }
}
