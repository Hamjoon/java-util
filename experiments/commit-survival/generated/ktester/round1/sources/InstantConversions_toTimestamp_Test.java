package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toTimestamp_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    mockConverter;

    private static Stream<Arguments> validInstantProvider() {
        return Stream.of(Arguments.of("Standard instant conversion", "2023-01-01T12:34:56Z", "2023-01-01 12:34:56.0"), Arguments.of("Nanosecond‑precision instant conversion", "2023-01-01T12:34:56.123456789Z", "2023-01-01 12:34:56.123456789"), Arguments.of("Unix epoch instant conversion", "1970-01-01T00:00:00Z", "1970-01-01 00:00:00.0"));
    }

    private static Stream<Arguments> outOfRangeInstantProvider() {
        return Stream.of(Arguments.of("Far future beyond Timestamp.max", "292278994-08-17T07:12:55.807Z"), Arguments.of("Far past before Timestamp.min", "-292275055-01-27T08:29:52.681Z"));
    }

    @Test
    void toTimestamp_WithValidInstant_ReturnsCorrespondingTimestamp() {
        // given
        Instant now = Instant.now();
        // when
        Timestamp result = InstantConversions.toTimestamp(now, mockConverter);
        // then
        assertNotNull(result, "Resulting Timestamp should not be null");
        assertEquals(Timestamp.from(now), result, "Timestamp should match the one created from the same Instant");
        assertEquals(now.toEpochMilli(), result.getTime(), "Epoch milliseconds should be equal");
    }

    @Test
    void toTimestamp_WithNonInstantObject_ThrowsClassCastException() {
        // given
        Object notAnInstant = "2023-01-01T00:00:00Z";
        // when / then
        assertThrows(ClassCastException.class, () -> InstantConversions.toTimestamp(notAnInstant, mockConverter), "Casting a non‑Instant object should result in ClassCastException");
    }

    @Test
    void toTimestamp_WithNullObject_ThrowsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> InstantConversions.toTimestamp(nullObj, mockConverter), "Passing null should cause NullPointerException when Timestamp.from is invoked");
    }

    /**
     * Valid {@link Instant} values should be converted to a {@link Timestamp}
     * that matches {@link Timestamp#valueOf(String)} for the expected string.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validInstantProvider")
    void toTimestamp_WithValidInstant_ReturnsExpectedTimestamp(String description, String instantIso, String expectedTimestampString) {
        // given
        Instant instant = Instant.parse(instantIso);
        Timestamp expected = Timestamp.valueOf(expectedTimestampString);
        // when
        Timestamp result = InstantConversions.toTimestamp(instant, mockConverter);
        // then
        assertNotNull(result, "Resulting Timestamp should not be null");
        assertEquals(expected, result, () -> "Timestamp should match expected value for case: " + description);
        assertEquals(expected.getTime(), result.getTime(), () -> "Epoch milliseconds should be equal for case: " + description);
    }

    @Test
    void toTimestamp_WithConverterArgumentIgnored_ReturnsSameResult() {
        // given
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        Timestamp expected = Timestamp.valueOf("2023-01-01 12:34:56.0");
        // when: pass a different (non‑null) converter – the mock is sufficient
        Timestamp result = InstantConversions.toTimestamp(instant, mockConverter);
        // then
        assertEquals(expected, result, "The Converter argument should have no effect on the conversion result");
    }

    @ParameterizedTest(name = "[{index}] Out‑of‑range instant: {0}")
    @MethodSource("outOfRangeInstantProvider")
    void toTimestamp_WithOutOfRangeInstant_ThrowsIllegalArgumentException(String description, String instantIso) {
        // given
        Instant instant = Instant.parse(instantIso);
        // when / then
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toTimestamp(instant, mockConverter), () -> "Instant " + instantIso + " should cause IllegalArgumentException (" + description + ")");
    }
}
