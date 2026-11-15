package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toLocalTime_Test {

    @Mock
    private Converter // Converter is only needed for the fallback path
    mockConverter;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for each test.
        mocks = MockitoAnnotations.openMocks(this);
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the Mockito session.
        mocks.close();
        mocks.close();
    }

    private static Stream<Arguments> directParsingProvider() {
        return Stream.of(// valid ISO‑8601 local time with seconds
        Arguments.of("23:59:59", LocalTime.of(23, 59, 59)), // nanosecond fraction
        Arguments.of("12:34:56.789", LocalTime.of(12, 34, 56, 789_000_000)), // hour‑minute only (seconds omitted)
        Arguments.of("23:59", LocalTime.of(23, 59)), // simple valid time used in dummy‑converter test
        Arguments.of("13:45:00", LocalTime.of(13, 45, 0)));
    }

    private static Stream<Arguments> fallbackParsingProvider() {
        return Stream.of(// full ZonedDateTime with zone ID
        Arguments.of("2022-01-01T08:15:30+01:00[Europe/Paris]", LocalTime.of(8, 15, 30)), // offset‑only time
        Arguments.of("10:15:30+02:00", LocalTime.of(10, 15, 30)), // offset‑date‑time without zone ID
        Arguments.of("2022-01-01T08:15:30+01:00", LocalTime.of(8, 15, 30)));
    }

    private static Stream<Arguments> failureProvider() {
        return Stream.of(// unparsable string – both parsers fail
        Arguments.of("not-a-date-time"), // whitespace‑only string – direct parse fails, fallback returns null
        Arguments.of(" 12:00:00 "), // empty string
        Arguments.of(""), // bare zone ID
        Arguments.of("Europe/Paris"));
    }

    /**
     * When the input string is a valid {@link LocalTime}, the method should return the parsed value
     * without invoking the fallback {@code toZonedDateTime} conversion.
     */
    @Test
    void shouldParseValidLocalTimeString() {
        // GIVEN – a valid LocalTime string (converter is irrelevant)
        // WHEN
        LocalTime result = StringConversions.toLocalTime(input, mockConverter);
        // THEN
        assertNotNull(result, "Result must not be null for a valid LocalTime string");
        assertEquals(expected, result);
        // Verify that the fallback static method is never called
        try (MockedStatic<StringConversions> mocked = mockStatic(StringConversions.class, CALLS_REAL_METHODS)) {
            mocked.verify(() -> StringConversions.toZonedDateTime(any(), any()), never());
        }
    }

    /**
     * When the input string cannot be parsed as a {@link LocalTime} but is a valid
     * {@link ZonedDateTime} representation, the method should fall back to {@code toZonedDateTime}
     * and return the {@link LocalTime} component of that {@link ZonedDateTime}.
     */
    @Test
    void shouldFallbackToZonedDateTimeAndExtractLocalTime() {
        // GIVEN – mock the static fallback method to return a suitable ZonedDateTime
        ZonedDateTime mockedZdt = ZonedDateTime.of(// date part is irrelevant for the local time extraction
        LocalDate.of(1970, 1, 1), expected, ZoneOffset.UTC);
        try (MockedStatic<StringConversions> mocked = mockStatic(StringConversions.class, CALLS_REAL_METHODS)) {
            // When toZonedDateTime is called with the exact input, return the mocked ZonedDateTime
            mocked.when(() -> StringConversions.toZonedDateTime(eq(input), any())).thenReturn(mockedZdt);
            // WHEN
            LocalTime result = StringConversions.toLocalTime(input, mockConverter);
            // THEN
            assertNotNull(result, "Result must not be null when fallback succeeds");
            assertEquals(expected, result);
        }
    }

    /**
     * When the input string is neither a valid {@link LocalTime} nor a valid {@link ZonedDateTime},
     * the method should return {@code null}.
     */
    @Test
    void shouldReturnNullWhenBothParsersFail() {
        try (MockedStatic<StringConversions> mocked = mockStatic(StringConversions.class, CALLS_REAL_METHODS)) {
            // Force the fallback to return null for the given input
            mocked.when(() -> StringConversions.toZonedDateTime(eq(input), any())).thenReturn(null);
            // WHEN
            LocalTime result = StringConversions.toLocalTime(input, mockConverter);
            // THEN
            assertNull(result, "Result should be null for an unparseable string");
        }
    }

    @Test
    void shouldThrowNullPointerExceptionWhenFromIsNull() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toLocalTime(nullInput, mockConverter), "Casting a null to String should throw NullPointerException");
    }

    @Test
    void shouldThrowClassCastExceptionWhenFromIsNotString() {
        // GIVEN
        Integer nonString = 12345;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> StringConversions.toLocalTime(nonString, mockConverter), "Providing a non‑String argument should cause a ClassCastException");
    }

    @Test
    void converterShouldNotBeUsedWhenDirectParseSucceeds() {
        // GIVEN
        String validTime = "13:45:00";
        // WHEN
        LocalTime result = StringConversions.toLocalTime(validTime, mockConverter);
        // THEN
        assertEquals(LocalTime.of(13, 45, 0), result);
        // Verify that the mock converter never received any interaction
        verifyNoInteractions(mockConverter);
    }
}
