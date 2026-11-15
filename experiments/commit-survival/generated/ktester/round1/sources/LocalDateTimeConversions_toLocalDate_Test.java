package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toLocalDate_Test {

    /**
     * The mock Converter that will be passed to the static conversion methods.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Mocked Converter that always throws a RuntimeException – used to verify exception propagation.
     */
    private Converter throwingConverter;

    /**
     * Dummy class used to test custom conversion via a supplied {@link Converter}.
     */
    private static final class CustomTemporal {
        // No fields or behavior – just a marker type.
    }

    /**
     * Placeholder for the library's {@code ConversionException}.
     * In real code this would be imported from the library; we declare a minimal version
     * to keep the test compilation self‑contained.
     */
    @SuppressWarnings("serial")
    private static class ConversionException extends RuntimeException {

        public ConversionException(String message) {
            super(message);
        }

        public ConversionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @BeforeAll
    static void beforeAll() {
        // Force the JVM default time‑zone to UTC so that Date/Calendar based tests are deterministic.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void beforeEach() {
        // Create a throwing converter for the specific test case.
        throwingConverter = mock(Converter.class);
        when(throwingConverter.convert(any(), any())).thenThrow(new RuntimeException("conversion failed"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid any accidental interaction leakage between tests.
        // This is defensive; the current tests do not interact with the mock.
        org.mockito.Mockito.reset(mockConverter);
        // Reset mocks to avoid cross‑test interference.
        reset(mockConverter, throwingConverter);
    }

    private static Stream<Arguments> successfulConversionProvider() {
        // Helper values
        ZonedDateTime utcZdt = ZonedDateTime.of(LocalDateTime.of(2023, 5, 10, 15, 30, 0), ZoneOffset.UTC);
        ZonedDateTime offsetZdt = ZonedDateTime.of(LocalDateTime.of(2023, 5, 10, 15, 30, 0), ZoneOffset.ofHours(2));
        Instant instant = Instant.parse("2023-05-10T13:30:00Z");
        Date utilDate = new Date(instant.toEpochMilli());
        Timestamp timestamp = Timestamp.valueOf("2023-05-10 15:30:00");
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.set(2023, Calendar.MAY, 10, 15, 30, 0);
        utcCal.set(Calendar.MILLISECOND, 0);
        Calendar nyCal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        nyCal.set(2023, Calendar.MAY, 10, 20, 30, 0);
        nyCal.set(Calendar.MILLISECOND, 0);
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2023, 5, 10, 15, 30, 0, 0, ZoneOffset.ofHours(2));
        java.sql.Date sqlDate = java.sql.Date.valueOf("2023-05-10");
        LocalDate expected = LocalDate.of(2023, 5, 10);
        LocalDate preEpoch = LocalDate.of(1969, 12, 31);
        LocalDate customDate = LocalDate.of(2022, 12, 31);
        // Custom converter for CustomTemporal → ZonedDateTime
        Converter customConverter = mock(Converter.class);
        CustomTemporal customTemporal = new CustomTemporal();
        when(customConverter.convert(eq(customTemporal), eq(ZonedDateTime.class))).thenReturn(ZonedDateTime.of(customDate.atStartOfDay(), ZoneOffset.UTC));
        return Stream.of(// Null source is **not** allowed – handled in exception tests.
        // 1. Direct LocalDate
        arguments(LocalDate.of(2023, 5, 10), null, LocalDate.of(2023, 5, 10)), // 2. LocalDateTime
        arguments(LocalDateTime.of(2023, 5, 10, 15, 30, 45), null, expected), // 3. ZonedDateTime with offset
        arguments(offsetZdt, null, expected), // 4. Instant (UTC)
        arguments(instant, null, expected), // 5. java.util.Date (system default UTC)
        arguments(utilDate, null, expected), // 6. java.sql.Timestamp
        arguments(timestamp, null, expected), // 7. Calendar (UTC)
        arguments(utcCal, null, expected), // 8. Long epoch‑millis
        arguments(Long.valueOf(1683718200000L), null, expected), // 9. ISO‑8601 date string
        arguments("2023-05-10", null, expected), // 10. ISO‑8601 datetime string (no offset)
        arguments("2023-05-10T15:30:00", null, expected), // 11. OffsetDateTime
        arguments(offsetDateTime, null, expected), // 12. ISO‑8601 datetime with offset
        arguments("2023-05-10T15:30:00+02:00", null, expected), // 13. java.sql.Date
        arguments(sqlDate, null, expected), // 14. Calendar with non‑UTC zone (America/New_York)
        arguments(nyCal, null, expected), // 15. Instant at day‑end UTC
        arguments(Instant.parse("2023-05-10T23:59:59Z"), null, expected), // 16. Negative epoch millis (day before epoch)
        arguments(Long.valueOf(-86400000L), null, preEpoch), // 17. BigDecimal epoch‑millis
        arguments(BigDecimal.valueOf(1683718200000L), null, expected), // 18. AtomicLong containing epoch‑millis
        arguments(new AtomicLong(1683718200000L), null, expected), // 19. CustomTemporal with a supplied custom converter
        arguments(customTemporal, customConverter, customDate));
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        // Custom temporal without a converter → IllegalArgumentException
        CustomTemporal customTemporal = new CustomTemporal();
        // Object that will cause the supplied converter to throw
        Object throwingObject = new Object();
        return Stream.of(// 1. Null source → IllegalArgumentException
        arguments(null, null, IllegalArgumentException.class), // 2. Unsupported Boolean
        arguments(Boolean.TRUE, null, ConversionException.class), // 3. LocalTime (no date component)
        arguments(LocalTime.of(15, 30), null, ConversionException.class), // 4. Invalid date string format
        arguments("10-05-2023", null, ConversionException.class), // 5. Empty string
        arguments("", null, ConversionException.class), // 6. Whitespace only string
        arguments("   ", null, ConversionException.class), // 7. Malformed ISO‑8601 datetime with garbage
        arguments("2023-05-10T15:30:00XYZ", null, ConversionException.class), // 8. Numeric string not representing a date
        arguments("12345", null, ConversionException.class), // 9. Double value
        arguments(Double.valueOf(12345.0), null, ConversionException.class), // 10. Float value
        arguments(Float.valueOf(12345f), null, ConversionException.class), // 11. YearMonth (no day)
        arguments(YearMonth.of(2023, 5), null, ConversionException.class), // 12. java.sql.Time (time‑only)
        arguments(Time.valueOf("15:30:00"), null, ConversionException.class), // 13. Generic Map
        arguments(Map.of("key", "value"), null, ConversionException.class), // 14. AtomicInteger (not accepted)
        arguments(new AtomicInteger(1683718200), null, ConversionException.class), // 15. BigInteger (not handled)
        arguments(BigInteger.valueOf(1683718200000L), null, ConversionException.class), // 16. CustomTemporal without a converter → IllegalArgumentException
        arguments(customTemporal, null, IllegalArgumentException.class), // 17. Converter throws during conversion → ConversionException
        // we will pass the throwingConverter in the test body
        arguments(throwingObject, null, ConversionException.class));
    }

    /**
     * Verifies that {@code LocalDateTimeConversions.toLocalDate(Object, Converter)}
     * correctly extracts the {@link java.time.LocalDate} part from a {@link ZonedDateTime}
     * instance supplied as the source object.
     */
    @Test
    void toLocalDate_givenZonedDateTime_returnsCorrespondingLocalDate() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(LocalDateTime.of(2023, 5, 17, 10, 15, 30), ZoneOffset.UTC);
        LocalDate expected = LocalDate.of(2023, 5, 17);
        // WHEN
        LocalDate actual = LocalDateTimeConversions.toLocalDate(source, mockConverter);
        // THEN
        assertEquals(expected, actual, "The LocalDate extracted from the ZonedDateTime should match the expected date.");
    }

    /**
     * Verifies that a {@link java.time.LocalDate} supplied directly is also handled
     * correctly. The internal implementation routes through {@code toZonedDateTime},
     * which should treat a {@code LocalDate} as a date at the start of the day in the
     * system default zone.
     */
    @Test
    void toLocalDate_givenLocalDate_returnsSameLocalDate() {
        // GIVEN
        LocalDate source = LocalDate.of(1999, 12, 31);
        // The expected result is the same LocalDate; the conversion path should preserve it.
        LocalDate expected = source;
        // WHEN
        LocalDate actual = LocalDateTimeConversions.toLocalDate(source, mockConverter);
        // THEN
        assertEquals(expected, actual, "When a LocalDate is supplied, the conversion should return the identical LocalDate.");
    }

    /**
     * Verifies that passing {@code null} as the source object returns {@code null}
     * (the Converter utility follows this convention for most reference types).
     */
    @Test
    void toLocalDate_givenNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        LocalDate actual = LocalDateTimeConversions.toLocalDate(source, mockConverter);
        // THEN
        assertNull(actual, "Converting a null source should result in a null LocalDate.");
    }

    /* --------------------------------------------------------------
     *  Successful conversion test cases
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("successfulConversionProvider")
    void toLocalDate_successful(Object source, Converter converter, LocalDate expected) {
        // WHEN
        LocalDate actual = LocalDateTimeConversions.toLocalDate(source, converter);
        // THEN
        assertEquals(expected, actual);
    }

    /* --------------------------------------------------------------
     *  Exception handling test cases
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} expects={2}")
    @MethodSource("exceptionConversionProvider")
    void toLocalDate_exceptions(Object source, Converter converter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> LocalDateTimeConversions.toLocalDate(source, converter));
    }

    @Test
    void toLocalDate_converterThrows_isWrappedInConversionException() {
        // GIVEN
        // any object that forces the converter to be used
        Object source = new Object();
        // WHEN / THEN
        ConversionException ex = assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toLocalDate(source, throwingConverter));
        assertTrue(ex.getCause() instanceof RuntimeException, "The underlying cause should be the RuntimeException thrown by the mock converter.");
    }
}
