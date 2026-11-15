package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toLocalDate_Test {

    /**
     * Mocked {@link Converter} that will be passed to the method under test.
     */
    @Mock
    Converter mockConverter;

    /**
     * A {@link MockedStatic} instance that allows us to stub the static
     *  {@code BigIntegerConversions.toZonedDateTime(...)} call used inside the
     *  method under test.
     */
    private MockedStatic<BigIntegerConversions> staticMock;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the static mock before each test so that each test gets a clean stub.
        staticMock = mockStatic(BigIntegerConversions.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock to avoid side‑effects on other tests.
        if (staticMock != null) {
            staticMock.close();
        }
    }

    /**
     * Provides arguments for the conversion‑success scenarios.
     *
     * Each argument consists of:
     *   - a description (used only for readability)
     *   - the source object to pass as the first parameter
     *   - the expected {@link LocalDate} result
     */
    private static Stream<Arguments> successCases() {
        return Stream.of(// Conversion from ZonedDateTime
        Arguments.of("ZonedDateTime source", ZonedDateTime.of(2023, 5, 17, 10, 30, 0, 0, ZoneId.of("UTC")), LocalDate.of(2023, 5, 17)), // Conversion from java.util.Date
        Arguments.of("java.util.Date source", Date.from(Instant.parse("2023-05-17T10:30:00Z")), LocalDate.of(2023, 5, 17)));
    }

    /**
     * Provides arguments for the scenarios that must result in a {@link NullPointerException}.
     */
    private static Stream<Arguments> nullPointerCases() {
        return Stream.of(// Null source object
        Arguments.of("null source object", null, // any non‑null converter
        mock(Converter.class)), // Null converter argument
        Arguments.of("null converter argument", ZonedDateTime.now(), null));
    }

    /**
     * Provides arguments for unsupported source types that must trigger an
     * {@link IllegalArgumentException}.
     */
    private static Stream<Arguments> illegalArgumentCases() {
        return Stream.of(Arguments.of("Unsupported Boolean source", Boolean.TRUE, IllegalArgumentException.class), Arguments.of("Unsupported arbitrary Object source", new Object(), IllegalArgumentException.class));
    }

    /**
     * Provides arguments for malformed string inputs that should cause a
     * {@link DateTimeParseException}.
     */
    private static Stream<Arguments> dateTimeParseCases() {
        return Stream.of(Arguments.of("Malformed date string \"2023-13-40\"", "2023-13-40", DateTimeParseException.class), Arguments.of("Non‑date string \"not-a-date\"", "not-a-date", DateTimeParseException.class));
    }

    /**
     * Provides arguments for epoch‑second values that are outside the range
     * supported by {@link Instant}, which should result in a {@link DateTimeException}.
     */
    private static Stream<Arguments> outOfRangeEpochCases() {
        return Stream.of(Arguments.of("Epoch seconds > Long.MAX_VALUE", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), DateTimeException.class), Arguments.of("Epoch seconds < Long.MIN_VALUE", BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE), DateTimeException.class));
    }

    /**
     * Verifies that {@code toLocalDate} returns the {@link java.time.LocalDate}
     * part of the {@link java.time.ZonedDateTime} produced by the internal
     * {@code toZonedDateTime} conversion.
     */
    @Test
    void testToLocalDate_DelegatesToZonedDateTime() {
        // GIVEN
        // the actual value is irrelevant because we stub the static call
        Object input = new Object();
        ZonedDateTime expectedZdt = ZonedDateTime.of(2023, 5, 17, 12, 34, 56, 0, ZoneOffset.UTC);
        // Stub the static method used inside the method under test.
        staticMock.when(() -> BigIntegerConversions.toZonedDateTime(eq(input), eq(mockConverter))).thenReturn(expectedZdt);
        // WHEN
        LocalDate result = BigIntegerConversions.toLocalDate(input, mockConverter);
        // THEN
        assertNotNull(result, "Resulting LocalDate should not be null");
        assertEquals(LocalDate.of(2023, 5, 17), result, "The LocalDate should match the date component of the stubbed ZonedDateTime");
        // Verify that the static method was called exactly once with the expected arguments.
        staticMock.verify(() -> BigIntegerConversions.toZonedDateTime(eq(input), eq(mockConverter)), times(1));
    }

    /**
     * Verifies the behaviour when the underlying {@code toZonedDateTime}
     * conversion returns {@code null}. According to the current implementation,
     * a {@link NullPointerException} is expected because the method immediately
     * dereferences the result to call {@code toLocalDate()}.
     */
    @Test
    void testToLocalDate_WhenZonedDateTimeIsNull_ThrowsNPE() {
        // GIVEN
        Object input = new Object();
        staticMock.when(() -> BigIntegerConversions.toZonedDateTime(eq(input), eq(mockConverter))).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toLocalDate(input, mockConverter), "Calling toLocalDate when toZonedDateTime returns null should throw NPE");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successCases")
    void testToLocalDate_SuccessfulConversions(String description, Object source, LocalDate expected) {
        // GIVEN – stub the static conversion to return a ZonedDateTime with the expected date
        ZonedDateTime stubZdt = ZonedDateTime.of(expected.getYear(), expected.getMonthValue(), expected.getDayOfMonth(), 0, 0, 0, 0, ZoneOffset.UTC);
        staticMock.when(() -> BigIntegerConversions.toZonedDateTime(eq(source), eq(mockConverter))).thenReturn(stubZdt);
        // WHEN
        LocalDate actual = BigIntegerConversions.toLocalDate(source, mockConverter);
        // THEN
        assertEquals(expected, actual, () -> "Expected LocalDate " + expected + " for source " + description);
        staticMock.verify(() -> BigIntegerConversions.toZonedDateTime(eq(source), eq(mockConverter)), times(1));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullPointerCases")
    void testToLocalDate_NullPointerScenarios(String description, Object source, Converter converter) {
        // No stubbing needed – the method should fail before delegating.
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toLocalDate(source, converter), () -> "Expected NPE for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("illegalArgumentCases")
    void testToLocalDate_UnsupportedSource(String description, Object source, Class<? extends Throwable> expectedException) {
        // Stub the static method to throw the expected exception.
        staticMock.when(() -> BigIntegerConversions.toZonedDateTime(eq(source), eq(mockConverter))).thenThrow(expectedException);
        // Verify that the exception propagates.
        assertThrows(expectedException, () -> BigIntegerConversions.toLocalDate(source, mockConverter), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("dateTimeParseCases")
    void testToLocalDate_InvalidString(String description, String source, Class<? extends Throwable> expectedException) {
        // Stub the static conversion to throw the parsing exception.
        staticMock.when(() -> BigIntegerConversions.toZonedDateTime(eq(source), eq(mockConverter))).thenThrow(expectedException);
        assertThrows(expectedException, () -> BigIntegerConversions.toLocalDate(source, mockConverter), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("outOfRangeEpochCases")
    void testToLocalDate_OutOfRangeEpoch(String description, BigInteger source, Class<? extends Throwable> expectedException) {
        // Stub the static conversion to throw the range‑related exception.
        staticMock.when(() -> BigIntegerConversions.toZonedDateTime(eq(source), eq(mockConverter))).thenThrow(expectedException);
        assertThrows(expectedException, () -> BigIntegerConversions.toLocalDate(source, mockConverter), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}
