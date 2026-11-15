package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toLocalDateTime_Test {

    /**
     * The mocked {@link Converter} that will be passed to the method under test.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock for every test to avoid cross‑test interference.
        converter = mock(Converter.class);
    }

    private static Stream<Arguments> provideValidConversionCases() {
        return Stream.of(// 1. Direct LocalDateTime – should be returned unchanged.
        Arguments.of(LocalDateTime.of(2023, 5, 17, 13, 45, 30, 123_000_000), null, LocalDateTime.of(2023, 5, 17, 13, 45, 30, 123_000_000), "Pass a LocalDateTime instance directly; method should return the same value."), // 2. ZonedDateTime (UTC) – offset stripped.
        Arguments.of(ZonedDateTime.of(2023, 5, 17, 13, 45, 30, 0, ZoneId.of("UTC")), null, LocalDateTime.of(2023, 5, 17, 13, 45, 30), "Convert a ZonedDateTime in UTC to a LocalDateTime; time zone offset should be stripped."), // 3. OffsetDateTime with negative offset – offset ignored.
        Arguments.of(OffsetDateTime.of(2023, 5, 17, 13, 45, 30, 0, ZoneOffset.ofHours(-5)), null, LocalDateTime.of(2023, 5, 17, 13, 45, 30), "Convert an OffsetDateTime with negative offset; offset is ignored in the resulting LocalDateTime."), // 4. Instant (UTC) – via ZonedDateTime conversion.
        Arguments.of(Instant.parse("2023-05-17T13:45:30Z"), null, LocalDateTime.of(2023, 5, 17, 13, 45, 30), "Convert an Instant (UTC) to LocalDateTime via ZonedDateTime conversion."), // 5. java.util.Date (UTC) – via ZonedDateTime conversion.
        Arguments.of(Date.from(Instant.parse("2023-05-17T13:45:30Z")), null, LocalDateTime.of(2023, 5, 17, 13, 45, 30), "Convert a java.util.Date (representing UTC instant) to LocalDateTime."), // 6. java.sql.Timestamp – preserve nanoseconds.
        Arguments.of(Timestamp.valueOf("2023-05-17 13:45:30.123456789"), null, LocalDateTime.of(2023, 5, 17, 13, 45, 30, 123_456_789), "Convert a java.sql.Timestamp preserving nanoseconds."), // 7. java.sql.Date – start of day.
        Arguments.of(java.sql.Date.valueOf("2023-05-17"), null, LocalDateTime.of(2023, 5, 17, 0, 0), "Convert a java.sql.Date (date only) to LocalDateTime at start of day."), // 8. LocalDate – start of day.
        Arguments.of(LocalDate.of(2023, 5, 17), null, LocalDateTime.of(2023, 5, 17, 0, 0), "Convert a LocalDate to LocalDateTime at start of day."), // 9. LocalTime – uses current date.
        Arguments.of(LocalTime.of(13, 45, 30), null, LocalDateTime.of(LocalDate.now(), LocalTime.of(13, 45, 30)), "Convert a LocalTime to LocalDateTime using the current date (as per toZonedDateTime implementation)."), // 10. ISO‑8601 String – parsed.
        Arguments.of("2023-05-17T13:45:30", null, LocalDateTime.of(2023, 5, 17, 13, 45, 30), "String ISO-8601 representation should be parsed to LocalDateTime."), // 11. Null input – should return null.
        Arguments.of(null, null, null, "Null input should result in null LocalDateTime."));
    }

    private static Stream<Arguments> provideExceptionCases() {
        // Helper converter that throws a RuntimeException when its convert method is invoked.
        Converter faultyConverter = mock(Converter.class);
        when(faultyConverter.convert(any(), any())).thenThrow(new RuntimeException("forced failure"));
        return Stream.of(// 1. Unsupported numeric type (Integer)
        Arguments.of(12345, null, IllegalArgumentException.class, "Pass an unsupported numeric type (Integer). The conversion should fail because Integer cannot be mapped to a ZonedDateTime."), // 2. Unsupported Boolean type
        Arguments.of(true, null, IllegalArgumentException.class, "Pass an unsupported Boolean type. The method should throw IllegalArgumentException."), // 3. Malformed ISO‑8601 string
        Arguments.of("not-a-date", null, java.time.format.DateTimeParseException.class, "Pass a malformed ISO‑8601 string that cannot be parsed."), // 4. Empty string
        Arguments.of("", null, java.time.format.DateTimeParseException.class, "Pass an empty string. Parsing fails and a DateTimeParseException is expected."), // 5. Generic Object with no conversion rule
        Arguments.of(new Object(), null, IllegalArgumentException.class, "Pass a generic Object that has no conversion rule."), // 6. Faulty Converter that throws RuntimeException
        Arguments.of(LocalDateTime.of(2023, 5, 17, 13, 45, 30), faultyConverter, RuntimeException.class, "Supply a custom Converter implementation whose convert method throws a RuntimeException."));
    }

    /**
     * Verifies that {@code BigIntegerConversions.toLocalDateTime(Object, Converter)}
     * delegates to {@code BigIntegerConversions.toZonedDateTime(Object, Converter)}
     * and returns the {@link ZonedDateTime}'s {@link LocalDateTime} component.
     */
    @Test
    void testToLocalDateTime_DelegatesCorrectly() {
        // Arrange – a deterministic ZonedDateTime that we expect to be returned.
        ZonedDateTime expectedZdt = ZonedDateTime.parse("2023-08-15T12:34:56+02:00[Europe/Paris]");
        // Mock the static method toZonedDateTime so that it returns the expected value.
        try (MockedStatic<BigIntegerConversions> mocked = Mockito.mockStatic(BigIntegerConversions.class)) {
            mocked.when(() -> BigIntegerConversions.toZonedDateTime(any(), eq(converter))).thenReturn(expectedZdt);
            // Act – call the method under test.
            Object dummySource = "any source object";
            LocalDateTime result = BigIntegerConversions.toLocalDateTime(dummySource, converter);
            // Assert – the result should be the LocalDateTime part of the stubbed ZonedDateTime.
            assertEquals(expectedZdt.toLocalDateTime(), result, "The LocalDateTime returned should match the ZonedDateTime's local part.");
            // Verify that the static delegation happened exactly once with the supplied arguments.
            mocked.verify(() -> BigIntegerConversions.toZonedDateTime(dummySource, converter), times(1));
        }
    }

    /**
     * Ensures that a {@code null} source object results in a {@code null}
     * {@link LocalDateTime} when the delegated {@code toZonedDateTime} also returns {@code null}.
     */
    @Test
    void testToLocalDateTime_NullSourceReturnsNull() {
        try (MockedStatic<BigIntegerConversions> mocked = Mockito.mockStatic(BigIntegerConversions.class)) {
            mocked.when(() -> BigIntegerConversions.toZonedDateTime(isNull(), eq(converter))).thenReturn(null);
            LocalDateTime result = BigIntegerConversions.toLocalDateTime(null, converter);
            assertNull(result, "When the source is null, the method should return null.");
            mocked.verify(() -> BigIntegerConversions.toZonedDateTime(null, converter), times(1));
        }
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideValidConversionCases")
    void testToLocalDateTime_Valid(Object from, Converter conv, LocalDateTime expected, String description) {
        // Act
        LocalDateTime actual = BigIntegerConversions.toLocalDateTime(from, conv);
        // Assert
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideExceptionCases")
    void testToLocalDateTime_Exceptions(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        // Act & Assert
        assertThrows(expectedException, () -> BigIntegerConversions.toLocalDateTime(from, conv), description);
    }
}
