package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toLong_Test {

    /**
     * Shared {@link Converter} instance – the method under test does not use it,
     *  but the signature requires it.
     */
    private static Converter converter;

    /**
     * A fixed {@link ZonedDateTime} used for the happy‑path test.
     */
    private ZonedDateTime fixedZonedDateTime;

    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a public no‑arg constructor, so we can use the default options.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @BeforeEach
    void setupBeforeEach() {
        // 2023‑01‑01T12:34:56.789+02:00[Europe/Paris]
        fixedZonedDateTime = ZonedDateTime.of(2023, 1, 1, 12, 34, 56, 789_000_000, ZoneId.of("Europe/Paris"));
    }

    @AfterEach
    void teardownAfterEach() {
        fixedZonedDateTime = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    static Stream<ValidCase> validConversionProvider() {
        return Stream.of(new ValidCase(ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 0, ZoneId.of("UTC")), 1683808245000L, "Standard UTC ZonedDateTime should be converted to its exact epoch milli value."), new ValidCase(ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 0, ZoneId.of("America/New_York")), 1683822645000L, "ZonedDateTime with a non‑UTC zone (America/New_York, UTC‑4 in May) should be converted to the same instant as UTC, resulting in a larger epoch milli."), new ValidCase(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC")), 0L, "The epoch instant (1970‑01‑01T00:00:00Z) should map to 0 milliseconds."), new ValidCase(ZonedDateTime.of(3000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), 32503593600000L, "A far‑future UTC ZonedDateTime (year 3000) should be converted correctly; the expected value is pre‑computed."));
    }

    static Stream<InvalidTypeCase> invalidTypeProvider() {
        return Stream.of(new InvalidTypeCase(LocalDate.now(), ClassCastException.class, "Passing a LocalDate (not a ZonedDateTime) should cause a ClassCastException during the cast."), new InvalidTypeCase("some string", ClassCastException.class, "Passing a String instead of a ZonedDateTime should trigger a ClassCastException."), new InvalidTypeCase(new Date(), ClassCastException.class, "Passing a java.util.Date (not a ZonedDateTime) should result in a ClassCastException."), new InvalidTypeCase(Instant.now(), ClassCastException.class, "Passing an Instant (not a ZonedDateTime) should raise a ClassCastException."), new InvalidTypeCase(OffsetDateTime.now(), ClassCastException.class, "Passing an OffsetDateTime (different type) should cause a ClassCastException."));
    }

    @Test
    void toLong_returnsCorrectEpochMilli() {
        // GIVEN
        long expected = fixedZonedDateTime.toInstant().toEpochMilli();
        // WHEN
        long actual = ZonedDateTimeConversions.toLong(fixedZonedDateTime, converter);
        // THEN
        assertEquals(expected, actual, "toLong should return the epoch‑millisecond representation of the ZonedDateTime");
    }

    @Test
    void toLong_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toLong(nullInput, converter), "Passing null should result in a NullPointerException because the method casts without checking");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toLong_validConversions(ValidCase tc) {
        // GIVEN – the test case already contains a fully‑constructed ZonedDateTime.
        // WHEN
        long actual = ZonedDateTimeConversions.toLong(tc.from(), null);
        // THEN
        assertEquals(tc.expected(), actual, tc.description());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidTypeProvider")
    void toLong_invalidSourceTypes(InvalidTypeCase tc) {
        assertThrows(tc.expectedException(), () -> ZonedDateTimeConversions.toLong(tc.from(), null), tc.description());
    }

    @Test
    void toLong_nullSourceObject_throwsNullPointerException() {
        // This mirrors the JSON “Null source object handling” group.
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toLong(null, null), "Passing a null source should cause a NullPointerException because the method casts the argument to ZonedDateTime before any null‑check.");
    }

    @Test
    void toLong_overflow_throwsArithmeticException() {
        // Instant.MAX is far beyond Long.MAX_VALUE milliseconds.
        ZonedDateTime overflowZdt = ZonedDateTime.ofInstant(Instant.MAX, ZoneId.of("UTC"));
        assertThrows(ArithmeticException.class, () -> ZonedDateTimeConversions.toLong(overflowZdt, null), "Converting a ZonedDateTime whose instant exceeds Long.MAX_VALUE milliseconds should throw an ArithmeticException due to overflow.");
    }
}
