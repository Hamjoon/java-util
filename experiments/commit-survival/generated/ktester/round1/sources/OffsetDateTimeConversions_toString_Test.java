package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toString_Test {

    /**
     * Mocked {@link Converter} – the method under test does not use it, but it must be supplied.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A dummy implementation of {@link Converter} – does nothing.
     */
    private static class DummyConverter implements Converter {
        // No methods to implement for this test; the interface is a marker in the library.
    }

    /**
     * A Converter that throws if any method is invoked – proves the method under test ignores it.
     */
    private static class ThrowingConverter implements Converter {
        // All methods (if any) would throw; left empty because the test never calls them.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the mock (MockitoExtension does this automatically, but we keep the method for symmetry).
        MockitoAnnotations.openMocks(this);
        // MockitoExtension already initialises @Mock fields, this method is kept for symmetry.
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to a clean state for the next test.
        reset(mockConverter);
        reset(mockConverter);
    }

    /* --------------------------------------------------------------------- *
     *  Parameter source for successful conversions
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> provideValidOffsetDateTimes() {
        return Stream.of(// UTC (zero offset) – expected trailing 'Z'
        Arguments.of(OffsetDateTime.parse("2023-01-01T12:34:56Z"), null, "2023-01-01T12:34:56Z", "Valid UTC OffsetDateTime (zero offset)"), // Positive hour offset
        Arguments.of(OffsetDateTime.parse("2023-01-01T12:34:56+02:00"), null, "2023-01-01T12:34:56+02:00", "Positive offset (+02:00)"), // Negative hour‑minute offset
        Arguments.of(OffsetDateTime.parse("2023-01-01T12:34:56-05:30"), null, "2023-01-01T12:34:56-05:30", "Negative offset (-05:30)"), // Leap‑day, zero offset, using a non‑null Converter (ignored)
        Arguments.of(OffsetDateTime.parse("2024-02-29T23:59:59+00:00"), new DummyConverter(), "2024-02-29T23:59:59Z", "Leap‑day UTC with non‑null Converter"), // Fractional seconds (milliseconds) with positive offset
        Arguments.of(OffsetDateTime.parse("2023-03-15T08:30:45.123+01:00"), null, "2023-03-15T08:30:45.123+01:00", "Fractional seconds, positive offset"), // Maximum positive offset (+18:00)
        Arguments.of(OffsetDateTime.parse("2022-12-31T23:59:59+18:00"), null, "2022-12-31T23:59:59+18:00", "Maximum positive offset"), // Maximum negative offset (‑18:00)
        Arguments.of(OffsetDateTime.parse("2022-01-01T00:00:00-18:00"), null, "2022-01-01T00:00:00-18:00", "Maximum negative offset"), // Non‑hour‑aligned offset (+05:45)
        Arguments.of(OffsetDateTime.parse("2021-06-30T12:00:00+05:45"), null, "2021-06-30T12:00:00+05:45", "Non‑hour‑aligned offset"), // Full nanosecond precision on a leap day
        Arguments.of(OffsetDateTime.parse("2020-02-29T23:59:59.999999999Z"), null, "2020-02-29T23:59:59.999999999Z", "Maximum nanosecond precision"), // Valid UTC with a Converter that would throw if used (method ignores it)
        Arguments.of(OffsetDateTime.parse("2023-01-01T00:00:00Z"), new ThrowingConverter(), "2023-01-01T00:00:00Z", "UTC with throwing Converter"));
    }

    /* --------------------------------------------------------------------- *
     *  Parameter source for exceptional cases
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> provideExceptionalCases() {
        return Stream.of(// Null source – cast yields NullPointerException when format() is invoked
        Arguments.of(null, null, NullPointerException.class, "Null source should cause NPE"), // Wrong type – explicit cast triggers ClassCastException
        Arguments.of("2023-01-01T12:34:56", null, ClassCastException.class, "Incompatible type (String) should cause CCE"));
    }

    @Test
    @DisplayName("toString returns ISO‑8601 representation for a valid OffsetDateTime")
    void testToStringReturnsIsoFormat() {
        // given
        OffsetDateTime offsetDateTime = OffsetDateTime.of(LocalDateTime.of(2023, 5, 15, 10, 15, 30, 0), ZoneOffset.ofHours(2));
        // when
        String result = OffsetDateTimeConversions.toString(offsetDateTime, mockConverter);
        // then
        assertEquals("2023-05-15T10:15:30+02:00", result, "The string representation should match DateTimeFormatter.ISO_OFFSET_DATE_TIME");
    }

    @Test
    @DisplayName("toString throws NullPointerException when the source object is null")
    void testToStringWithNullThrowsNPE() {
        // given
        Object nullObject = null;
        // when / then
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toString(nullObject, mockConverter), "A null source should result in a NullPointerException due to the cast");
    }

    @Test
    @DisplayName("toString throws ClassCastException when the source is not an OffsetDateTime")
    void testToStringWithInvalidTypeThrowsClassCastException() {
        // given
        Object notAnOffsetDateTime = "2023-05-15T10:15:30+02:00";
        // when / then
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toString(notAnOffsetDateTime, mockConverter), "A non‑OffsetDateTime source should cause a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideValidOffsetDateTimes")
    void testToStringValidCases(OffsetDateTime from, Converter converter, String expected, String description) {
        // When the converter argument is null we pass null directly; otherwise we use the supplied instance.
        String result = OffsetDateTimeConversions.toString(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideExceptionalCases")
    void testToStringExceptionalCases(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toString(from, converter), description);
    }
}
