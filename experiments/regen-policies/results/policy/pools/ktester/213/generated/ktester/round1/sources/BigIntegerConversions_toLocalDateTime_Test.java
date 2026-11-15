package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

class BigIntegerConversions_toLocalDateTime_Test {

    /**
     * Shared Converter instance used by the real‑conversion tests.
     */
    private static Converter sharedConverter;

    /**
     * Preserve the original default time‑zone so that we can restore it after all tests.
     */
    private static TimeZone originalDefaultTZ;

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve original default TZ.
        originalDefaultTZ = TimeZone.getDefault();
        // Force UTC – many expected results are based on UTC.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Use default options – the Converter constructor requires a non‑null options object.
        sharedConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default TZ.
        TimeZone.setDefault(originalDefaultTZ);
        // Release the shared Converter reference.
        sharedConverter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        // ZonedDateTime (UTC)
        ZonedDateTime zdtUtc = ZonedDateTime.of(2023, 5, 17, 14, 30, 0, 0, ZoneId.of("UTC"));
        // Instant (epoch seconds for 2022‑01‑01T00:00:00Z)
        Instant instant = Instant.ofEpochSecond(1_640_995_200L);
        // java.util.Date (epoch)
        Date epochDate = new Date(0L);
        // Calendar (UTC) set to the same instant as above
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(1_640_995_200_000L);
        // Long representing epoch milliseconds
        Long epochMillis = 1_640_995_200_000L;
        // BigInteger representing epoch seconds
        BigInteger epochSeconds = BigInteger.valueOf(1_640_995_200L);
        // ISO‑8601 string
        String isoString = "2023-08-15T12:45:30";
        // Direct LocalDateTime
        LocalDateTime directLdt = LocalDateTime.of(2021, 12, 31, 23, 59, 59);
        return Stream.of(Arguments.of(zdtUtc, LocalDateTime.of(2023, 5, 17, 14, 30, 0)), Arguments.of(instant, LocalDateTime.of(2022, 1, 1, 0, 0, 0)), Arguments.of(epochDate, LocalDateTime.of(1970, 1, 1, 0, 0, 0)), Arguments.of(utcCal, LocalDateTime.of(2022, 1, 1, 0, 0, 0)), Arguments.of(epochMillis, LocalDateTime.of(2022, 1, 1, 0, 0, 0)), Arguments.of(epochSeconds, LocalDateTime.of(2022, 1, 1, 0, 0, 0)), Arguments.of(isoString, LocalDateTime.of(2023, 8, 15, 12, 45, 30)), Arguments.of(directLdt, directLdt));
    }

    /**
     * Verify that {@code toLocalDateTime} simply delegates to {@code toZonedDateTime}
     * and extracts the {@link LocalDateTime} part.
     */
    @Test
    void testToLocalDateTime_DelegatesToZonedDateTime() {
        // Arrange – a dummy source object and a known ZonedDateTime result.
        Object dummySource = new Object();
        ZonedDateTime expectedZoned = ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 0, ZoneId.systemDefault());
        // Mock the static method toZonedDateTime to return the known value.
        try (MockedStatic<BigIntegerConversions> mocked = mockStatic(BigIntegerConversions.class)) {
            // The real toLocalDateTime implementation will be executed, but the
            // call to toZonedDateTime will be intercepted.
            mocked.when(() -> BigIntegerConversions.toZonedDateTime(dummySource, sharedConverter)).thenReturn(expectedZoned);
            // Act – invoke the method under test.
            LocalDateTime result = BigIntegerConversions.toLocalDateTime(dummySource, sharedConverter);
            // Assert – the result must be the LocalDateTime part of the mocked ZonedDateTime.
            assertEquals(expectedZoned.toLocalDateTime(), result);
            // Verify that the static method was called exactly once.
            mocked.verify(() -> BigIntegerConversions.toZonedDateTime(dummySource, sharedConverter), times(1));
        }
    }

    /**
     * When the source object is already a {@link LocalDateTime}, the conversion should
     * return an equal {@code LocalDateTime} (the system default zone is applied internally,
     * but the local date‑time component stays the same).
     */
    @Test
    void testToLocalDateTime_WithLocalDateTimeInput() {
        // Arrange
        LocalDateTime source = LocalDateTime.of(2022, 12, 31, 23, 59, 59);
        // Act
        LocalDateTime result = BigIntegerConversions.toLocalDateTime(source, sharedConverter);
        // Assert
        assertEquals(source, result);
    }

    /**
     * The conversion contract for a {@code null} source is defined by the underlying
     * {@code toZonedDateTime} implementation. In the current library version it returns
     * {@code null}, which would cause a {@link NullPointerException} when calling
     * {@code toLocalDateTime()} on it. This test documents that behaviour.
     */
    @Test
    void testToLocalDateTime_NullInput_ThrowsNPE() {
        // Arrange
        Object nullSource = null;
        // Act & Assert
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toLocalDateTime(nullSource, sharedConverter));
    }

    @ParameterizedTest(name = "[{index}] from={0} => expected={1}")
    @MethodSource("validConversionProvider")
    void testToLocalDateTime_ValidConversions(Object from, LocalDateTime expected) {
        // Act
        LocalDateTime actual = BigIntegerConversions.toLocalDateTime(from, sharedConverter);
        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testToLocalDateTime_NullSource_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toLocalDateTime(null, sharedConverter));
    }

    @Test
    void testToLocalDateTime_UnsupportedSource_ThrowsConversionException() {
        Object unsupported = new Object();
        assertThrows(ConversionException.class, () -> BigIntegerConversions.toLocalDateTime(unsupported, sharedConverter));
    }

    @Test
    void testToLocalDateTime_MalformedString_ThrowsDateTimeParseException() {
        String badString = "not-a-date-time";
        assertThrows(DateTimeParseException.class, () -> BigIntegerConversions.toLocalDateTime(badString, sharedConverter));
    }

    @Test
    void testToLocalDateTime_NullConverter_ThrowsNPE() {
        String iso = "2023-01-01T00:00:00";
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toLocalDateTime(iso, null));
    }

    @Test
    void testToLocalDateTime_BigIntegerOverflow_ThrowsDateTimeException() {
        // Long.MAX_VALUE seconds + 1 exceeds the range of Instant.
        BigInteger overflow = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        assertThrows(DateTimeException.class, () -> BigIntegerConversions.toLocalDateTime(overflow, sharedConverter));
    }

    @Test
    void testToLocalDateTime_YearBeyondSupportedRange_ThrowsDateTimeException() {
        String farFuture = "10000-01-01T00:00:00";
        assertThrows(DateTimeException.class, () -> BigIntegerConversions.toLocalDateTime(farFuture, sharedConverter));
    }

    @Test
    void testToLocalDateTime_InvalidHourInString_ThrowsDateTimeParseException() {
        String invalidHour = "2023-01-01T25:00:00";
        assertThrows(DateTimeParseException.class, () -> BigIntegerConversions.toLocalDateTime(invalidHour, sharedConverter));
    }

    @Test
    void testToLocalDateTime_UnsupportedDouble_ThrowsConversionException() {
        Double unsupported = Double.valueOf(12345.0);
        assertThrows(ConversionException.class, () -> BigIntegerConversions.toLocalDateTime(unsupported, sharedConverter));
    }
}
