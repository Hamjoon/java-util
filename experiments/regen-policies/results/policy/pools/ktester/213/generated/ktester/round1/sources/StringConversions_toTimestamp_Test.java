package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.time.*;
import java.time.format.DateTimeParseException;
import static org.mockito.ArgumentMatchers.any;

class StringConversions_toTimestamp_Test {

    private static ConverterOptions options;

    private Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Shared options for all tests – default configuration is sufficient.
        options = new ConverterOptions();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Converter for each test to avoid cross‑test state.
        converter = new Converter(options);
    }

    /**
     * When the source object is {@code null}, {@code toTimestamp} should return {@code null}.
     */
    @Test
    void shouldReturnNullWhenSourceIsNull() {
        // given
        Object source = null;
        // when
        Timestamp result = StringConversions.toTimestamp(source, converter);
        // then
        assertNull(result, "Result should be null when source is null");
    }

    /**
     * A normal conversion from an {@link Instant} that is within the supported range
     * should produce a matching {@link Timestamp}.
     */
    @Test
    void shouldConvertInstantToTimestamp() {
        // given
        // always after year 0001
        Instant now = Instant.now();
        // when
        Timestamp ts = StringConversions.toTimestamp(now, converter);
        // then
        assertNotNull(ts, "Timestamp should not be null for a valid Instant");
        assertEquals(Timestamp.from(now), ts, "Timestamp should represent the same instant");
    }

    /**
     * Converting an {@link Instant} that represents a date before year 0001 must throw
     * {@link IllegalArgumentException} with the expected message.
     */
    @Test
    void shouldThrowWhenInstantIsBeforeYearOne() {
        // given
        // Epoch second for 0000‑12‑31T23:59:59Z is -62135596801L (one second before 0001‑01‑01)
        Instant beforeYearOne = Instant.ofEpochSecond(-62135596801L);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toTimestamp(beforeYearOne, converter));
        assertTrue(ex.getMessage().contains("Cannot convert to Timestamp"), "Exception message should indicate conversion problem");
        assertTrue(ex.getMessage().contains("year before 0001"), "Exception message should mention the year restriction");
    }

    /**
     * When the supplied {@code Converter} instance is {@code null},
     * the internal call to {@code StringConversions.toInstant} will
     * dereference it and throw a {@link NullPointerException}.
     * The exception must propagate unchanged.
     */
    @Test
    void shouldThrowNullPointerExceptionWhenConverterIsNull() {
        // given
        Object source = "2020-01-01T00:00:00Z";
        Converter nullConverter = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringConversions.toTimestamp(source, nullConverter), "A null Converter should cause NullPointerException");
    }

    /**
     * If {@code toInstant} throws a {@link DateTimeParseException}
     * (e.g., when parsing an invalid ISO‑8601 string), {@code toTimestamp}
     * must let the exception bubble up unchanged.
     */
    @Test
    void shouldPropagateDateTimeParseExceptionFromToInstant() {
        // given
        String badIso = "not-a-date";
        // Mock the static call to StringConversions.toInstant to throw the exception.
        try (MockedStatic<StringConversions> mocked = Mockito.mockStatic(StringConversions.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> StringConversions.toInstant(eq(badIso), any())).thenThrow(new DateTimeParseException("Invalid format", badIso, 0));
            // when / then
            DateTimeParseException ex = assertThrows(DateTimeParseException.class, () -> StringConversions.toTimestamp(badIso, converter), "DateTimeParseException should be propagated");
            assertTrue(ex.getMessage().contains("Invalid format"));
        }
    }

    /**
     * If {@code toInstant} throws an {@link IllegalArgumentException}
     * because the source type is unsupported, {@code toTimestamp}
     * must propagate that exception.
     */
    @Test
    void shouldPropagateIllegalArgumentExceptionFromToInstant() {
        // given
        Object unsupported = new Object();
        // Mock the static call to StringConversions.toInstant to throw IllegalArgumentException.
        try (MockedStatic<StringConversions> mocked = Mockito.mockStatic(StringConversions.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> StringConversions.toInstant(eq(unsupported), any())).thenThrow(new IllegalArgumentException("Unsupported type"));
            // when / then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toTimestamp(unsupported, converter), "IllegalArgumentException should be propagated");
            assertEquals("Unsupported type", ex.getMessage());
        }
    }

    /**
     * Converting an {@link Instant} that is too large for {@link Timestamp}
     * (its epoch milli value overflows) must result in an
     * {@link ArithmeticException} from {@code Timestamp.from}.
     */
    @Test
    void shouldPropagateArithmeticExceptionWhenInstantIsTooLarge() {
        // given
        // overflow when converting to millis
        Instant tooLarge = Instant.ofEpochSecond(Long.MAX_VALUE);
        // when / then
        assertThrows(ArithmeticException.class, () -> StringConversions.toTimestamp(tooLarge, converter), "Overflowing instant should cause ArithmeticException");
    }

    /**
     * The JSON test case where a string representing a date before year 0001
     * is supplied together with a converter that parses the string to an
     * {@link Instant}.  The resulting instant is before the supported range,
     * so {@code toTimestamp} must throw {@link IllegalArgumentException}.
     *
     * <p>We achieve this by mocking {@code StringConversions.toInstant} to
     * return the pre‑year‑1 instant.</p>
     */
    @Test
    void shouldThrowWhenStringConvertsToInstantBeforeYearOne() {
        // given
        String preYearOneString = "0000-12-31T23:59:59Z";
        // one second before 0001‑01‑01
        Instant preYearOneInstant = Instant.ofEpochSecond(-62135596801L);
        // Mock static toInstant to return the pre‑year‑1 instant.
        try (MockedStatic<StringConversions> mocked = Mockito.mockStatic(StringConversions.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> StringConversions.toInstant(eq(preYearOneString), any())).thenReturn(preYearOneInstant);
            // when / then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toTimestamp(preYearOneString, converter), "Instant before year 0001 should cause IllegalArgumentException");
            assertTrue(ex.getMessage().contains("year before 0001"));
        }
    }
}
