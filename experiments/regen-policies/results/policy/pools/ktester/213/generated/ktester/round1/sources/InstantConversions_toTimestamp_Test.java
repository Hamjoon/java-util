package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DateTimeException;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toTimestamp_Test {

    /**
     * Shared {@link Converter} instance used by the tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; using the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared Converter reference.
        converter = null;
    }

    private static Stream<Arguments> validInstantProvider() {
        return Stream.of(Arguments.of("Epoch Instant (1970‑01‑01T00:00:00Z)", Instant.EPOCH, Timestamp.from(Instant.EPOCH)), Arguments.of("Typical Instant 2023‑03‑15T12:34:56.789Z", Instant.parse("2023-03-15T12:34:56.789Z"), Timestamp.from(Instant.parse("2023-03-15T12:34:56.789Z"))), Arguments.of("Maximum epoch‑millis Instant (Long.MAX_VALUE)", Instant.ofEpochMilli(Long.MAX_VALUE), Timestamp.from(Instant.ofEpochMilli(Long.MAX_VALUE))));
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of("String value \"2023-01-01\"", "2023-01-01"), Arguments.of("java.util.Date instance", new java.util.Date()));
    }

    private static Stream<Arguments> negativeEpochProvider() {
        return Stream.of(Arguments.of("Instant one second before the epoch", Instant.ofEpochSecond(-1), -1000L), Arguments.of("Instant one day before the epoch", Instant.ofEpochSecond(-86400), -86_400_000L));
    }

    private static Stream<Arguments> nanosecondPrecisionProvider() {
        return Stream.of(Arguments.of("Instant with explicit nanos (1622547800s + 123456789ns)", Instant.ofEpochSecond(1622547800, 123_456_789), 1_622_547_800_123L, 123_456_789), Arguments.of("Instant at 2022‑01‑01T00:00:00.999999999Z", Instant.parse("2022-01-01T00:00:00.999999999Z"), 1_640_995_200_999L, 999_999_999));
    }

    private static Stream<Arguments> outOfRangeProvider() {
        return Stream.of(Arguments.of("Instant.ofEpochMilli(Long.MIN_VALUE) – far below Timestamp range", Instant.ofEpochMilli(Long.MIN_VALUE)), Arguments.of("Instant.MAX – far above Timestamp range", Instant.MAX));
    }

    @Test
    void toTimestamp_convertsInstantCorrectly() {
        // GIVEN
        Instant now = Instant.now();
        // WHEN
        Timestamp ts = InstantConversions.toTimestamp(now, converter);
        // THEN
        assertNotNull(ts, "Resulting Timestamp should not be null");
        assertEquals(Timestamp.from(now), ts, "Timestamp should represent the same instant as the source Instant");
    }

    @Test
    void toTimestamp_nullInputReturnsNull() {
        // GIVEN
        Object nullObj = null;
        // WHEN
        Timestamp ts = InstantConversions.toTimestamp(nullObj, converter);
        // THEN
        assertNull(ts, "Converting a null Instant should return null");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validInstantProvider")
    void toTimestamp_validInstants(String description, Instant input, Timestamp expected) {
        // WHEN
        Timestamp actual = InstantConversions.toTimestamp(input, converter);
        // THEN
        assertNotNull(actual, "Result should never be null for a valid Instant");
        assertEquals(expected, actual, "Timestamp does not match expected value");
    }

    @Test
    void toTimestamp_nullInputThrowsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toTimestamp(nullObj, converter), "Converting a null Instant should propagate NullPointerException");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidTypeProvider")
    void toTimestamp_invalidTypes(String description, Object invalidInput) {
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> InstantConversions.toTimestamp(invalidInput, converter), "Expected ClassCastException when passing a non‑Instant value");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("negativeEpochProvider")
    void toTimestamp_negativeEpochs(String description, Instant input, long expectedMillis) {
        Timestamp ts = InstantConversions.toTimestamp(input, converter);
        assertEquals(expectedMillis, ts.getTime(), "Timestamp.getTime() should reflect the negative epoch milliseconds");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nanosecondPrecisionProvider")
    void toTimestamp_nanosecondPrecision(String description, Instant input, long expectedMillis, int expectedNanos) {
        Timestamp ts = InstantConversions.toTimestamp(input, converter);
        assertEquals(expectedMillis, ts.getTime(), "Milliseconds part should match");
        assertEquals(expectedNanos, ts.getNanos(), "Nanoseconds part should be preserved");
    }

    @Test
    void toTimestamp_ignoresConverterArgument() {
        // GIVEN
        Instant instant = Instant.parse("2023-03-15T12:34:56.789Z");
        // not used
        Converter dummyConverter = new Converter(new ConverterOptions());
        // WHEN
        Timestamp ts = InstantConversions.toTimestamp(instant, dummyConverter);
        // THEN
        assertEquals(Timestamp.from(instant), ts, "Method must ignore the converter argument and produce correct Timestamp");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("outOfRangeProvider")
    void toTimestamp_outOfRangeInstants(String description, Instant input) {
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toTimestamp(input, converter), "Converting an out‑of‑range Instant should throw IllegalArgumentException");
    }
}
