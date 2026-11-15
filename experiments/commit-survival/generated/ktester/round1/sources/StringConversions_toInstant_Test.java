package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toInstant_Test {

    /**
     * The {@link Converter} used by the tests.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Most conversions work out‑of‑the‑box with the default options.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector – not strictly necessary but keeps the pattern consistent.
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        // 1. Date representing epoch (0 ms)
        Date epochDate = new Date(0L);
        Instant epochInstant = Instant.ofEpochMilli(0L);
        // 2. ISO‑8601 string with +02:00 offset – should be normalized to UTC.
        String isoWithOffset = "2023-03-15T12:30:45+02:00";
        // Parse manually to obtain the expected UTC instant.
        ZonedDateTime zdt = ZonedDateTime.parse(isoWithOffset);
        Instant expectedInstant = zdt.toInstant();
        return Stream.of(Arguments.of(epochDate, epochInstant), Arguments.of(isoWithOffset, expectedInstant));
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        // Malformed ISO‑8601 string
        String malformed = "not-a-date";
        // Unsupported type (Boolean)
        Boolean unsupported = Boolean.TRUE;
        // Epoch millisecond overflow – Long.MAX_VALUE
        Long overflow = Long.MAX_VALUE;
        // Invalid zone identifier in string
        String invalidZone = "2023-03-15T12:30:45[Invalid/Zone]";
        return Stream.of(// DateTimeParseException for malformed string
        Arguments.of(malformed, DateTimeParseException.class), // IllegalArgumentException for unsupported source type
        Arguments.of(unsupported, IllegalArgumentException.class), // DateTimeException for overflow value
        Arguments.of(overflow, DateTimeException.class), // DateTimeException for invalid zone id
        Arguments.of(invalidZone, DateTimeException.class));
    }

    /**
     * Verify that a {@link java.util.Date} is correctly turned into an {@link Instant}.
     */
    @Test
    void toInstant_convertsDateToInstant() {
        // given
        // 2021‑06‑01T12:00:00Z
        long epochMilli = 1_622_548_800_000L;
        Date date = new Date(epochMilli);
        Instant expected = Instant.ofEpochMilli(epochMilli);
        // when
        Instant actual = StringConversions.toInstant(date, converter);
        // then
        assertEquals(expected, actual, "The Instant produced from a Date should match the Date's epoch value");
    }

    /**
     * Verify that a {@code null} source value results in a {@code null} Instant.
     */
    @Test
    void toInstant_nullInputReturnsNull() {
        // given
        Object source = null;
        // when
        Instant result = StringConversions.toInstant(source, converter);
        // then
        assertNull(result, "Converting a null source should return null");
    }

    @Test
    void toInstant_nullSourceAndNullConverter_returnsNull() {
        // Both parameters are null – the implementation should safely return null.
        assertNull(StringConversions.toInstant(null, null), "When both source and converter are null, the result should be null");
    }

    @Test
    void toInstant_nullSourceWithNonNullConverter_returnsNull() {
        // Only the source is null – conversion should yield null without NPE.
        assertNull(StringConversions.toInstant(null, converter), "When source is null, the result should be null regardless of converter");
    }

    @Test
    void toInstant_nonNullSourceWithNullConverter_throwsNullPointerException() {
        // The method expects a non‑null Converter for non‑null sources.
        Date now = new Date();
        assertThrows(NullPointerException.class, () -> StringConversions.toInstant(now, null), "A null Converter with a non‑null source should cause NPE");
    }

    @ParameterizedTest(name = "[{index}] from={0} => expected={1}")
    @MethodSource("validConversionProvider")
    void toInstant_validConversions(Object from, Instant expected) {
        // when
        Instant actual = StringConversions.toInstant(from, converter);
        // then
        assertEquals(expected, actual, () -> "Conversion of " + from + " should produce Instant " + expected);
    }

    @ParameterizedTest(name = "[{index}] from={0} => throws {1}")
    @MethodSource("exceptionConversionProvider")
    void toInstant_invalidConversions(Object from, Class<? extends Throwable> expectedException) {
        // then
        assertThrows(expectedException, () -> StringConversions.toInstant(from, converter), () -> "Converting " + from + " should throw " + expectedException.getSimpleName());
    }
}
