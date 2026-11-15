package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.DATE;
import static com.cedarsoftware.util.convert.MapConversions.TIME;
import static com.cedarsoftware.util.convert.MapConversions.ZONE;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toBigInteger_Test {

    /**
     * A mock {@link Converter} that is passed through the call chain.
     */
    private Converter mockConverter;

    /**
     * A reusable {@link Instant} used as the stubbed result of {@code toInstant}.
     */
    private Instant stubInstant;

    /**
     * The expected {@link BigInteger} result of the whole conversion.
     */
    private BigInteger expectedBigInteger;

    @BeforeEach
    void setupBeforeEach() {
        mockConverter = mock(Converter.class);
        stubInstant = Instant.parse("2023-01-01T12:34:56.789Z");
        expectedBigInteger = new BigInteger("12345678901234567890");
    }

    private static Stream<Arguments> validConversionProvider() {
        Instant instant = Instant.parse("2023-05-17T12:30:45Z");
        long epochMilli = instant.toEpochMilli();
        return Stream.of(// ZonedDateTime (UTC)
        Arguments.of("ZonedDateTime UTC", ZonedDateTime.of(2023, 5, 17, 12, 30, 45, 0, ZoneId.of("UTC")), BigInteger.valueOf(epochMilli)), // OffsetDateTime (+02:00)
        Arguments.of("OffsetDateTime +02:00", OffsetDateTime.of(2023, 5, 17, 12, 30, 45, 0, ZoneOffset.ofHours(2)), BigInteger.valueOf(epochMilli)), // java.util.Date
        Arguments.of("java.util.Date", Date.from(instant), BigInteger.valueOf(epochMilli)), // Calendar (UTC)
        Arguments.of("Calendar UTC", createUtcCalendar(instant), BigInteger.valueOf(epochMilli)), // java.sql.Timestamp
        Arguments.of("java.sql.Timestamp", Timestamp.from(instant), BigInteger.valueOf(epochMilli)), // Long epoch milliseconds
        Arguments.of("Long epoch millis", Long.valueOf(epochMilli), BigInteger.valueOf(epochMilli)), // ISO‑8601 string
        Arguments.of("ISO‑8601 string", "2023-05-17T12:30:45Z", BigInteger.valueOf(epochMilli)), // Non‑UTC ZonedDateTime (America/New_York)
        Arguments.of("ZonedDateTime America/New_York", ZonedDateTime.of(2023, 5, 17, 12, 30, 45, 0, ZoneId.of("America/New_York")), BigInteger.valueOf(Instant.parse("2023-05-17T16:30:45Z").toEpochMilli())), // LocalDate (start of day UTC)
        Arguments.of("LocalDate start of day UTC", LocalDate.of(2023, 5, 17), BigInteger.valueOf(Instant.parse("2023-05-17T00:00:00Z").toEpochMilli())), // LocalDateTime (treated as UTC)
        Arguments.of("LocalDateTime as UTC", LocalDateTime.of(2023, 5, 17, 12, 30, 45), BigInteger.valueOf(Instant.parse("2023-05-17T12:30:45Z").toEpochMilli())), // AtomicLong containing epoch millis
        Arguments.of("AtomicLong epoch millis", new AtomicLong(epochMilli), BigInteger.valueOf(epochMilli)), // Double with integral epoch millis
        Arguments.of("Double integral epoch millis", Double.valueOf(epochMilli), BigInteger.valueOf(epochMilli)), // Map with DATE, TIME, ZONE keys
        Arguments.of("Map with DATE/TIME/ZONE", createDateTimeMap("2023-05-17", "12:30:45", "UTC"), BigInteger.valueOf(epochMilli)), // BigInteger identity conversion
        Arguments.of("BigInteger identity", BigInteger.valueOf(123456789L), BigInteger.valueOf(123456789L)));
    }

    private static Calendar createUtcCalendar(Instant instant) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(instant.toEpochMilli());
        return cal;
    }

    private static Map<String, Object> createDateTimeMap(String date, String time, String zone) {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put(DATE, date);
        map.put(TIME, time);
        map.put(ZONE, zone);
        return map;
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(// Null source
        Arguments.of("Null source → NPE", null, NullPointerException.class), // Invalid ISO‑8601 string
        Arguments.of("Malformed string → DateTimeParseException", "not-a-date", DateTimeParseException.class), // Empty string
        Arguments.of("Empty string → DateTimeParseException", "", DateTimeParseException.class), // LocalTime unsupported
        Arguments.of("LocalTime unsupported → UnsupportedOperationException", LocalTime.of(12, 30, 45), UnsupportedOperationException.class), // Generic Object unsupported
        Arguments.of("Unsupported object type → UnsupportedOperationException", new Object(), UnsupportedOperationException.class));
    }

    @Test
    void toBigInteger_delegatesToInstantAndInstantConversions() {
        // Arrange – mock the two static methods that the implementation relies on.
        try (MockedStatic<ZonedDateTimeConversions> zonedMock = Mockito.mockStatic(ZonedDateTimeConversions.class);
            MockedStatic<InstantConversions> instantMock = Mockito.mockStatic(InstantConversions.class)) {
            // Stub: ZonedDateTimeConversions.toInstant(any, any) → stubInstant
            zonedMock.when(() -> ZonedDateTimeConversions.toInstant(any(), any())).thenReturn(stubInstant);
            // Stub: InstantConversions.toBigInteger(stubInstant, mockConverter) → expectedBigInteger
            instantMock.when(() -> InstantConversions.toBigInteger(stubInstant, mockConverter)).thenReturn(expectedBigInteger);
            // Act – invoke the method under test.
            // the actual value is irrelevant because we mock the static call
            Object dummySource = new Object();
            BigInteger result = ZonedDateTimeConversions.toBigInteger(dummySource, mockConverter);
            // Assert – the result should be the stubbed BigInteger.
            assertEquals(expectedBigInteger, result, "ZonedDateTimeConversions.toBigInteger should return the value produced by InstantConversions.toBigInteger");
            // Verify that the static methods were called exactly once with the expected arguments.
            zonedMock.verify(() -> ZonedDateTimeConversions.toInstant(eq(dummySource), eq(mockConverter)), times(1));
            instantMock.verify(() -> InstantConversions.toBigInteger(eq(stubInstant), eq(mockConverter)), times(1));
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversionProvider")
    void toBigInteger_validConversions(String description, Object source, BigInteger expected) {
        // Use a real (non‑mock) converter – the conversion logic does not depend on it.
        Converter converter = mock(Converter.class);
        BigInteger result = ZonedDateTimeConversions.toBigInteger(source, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionConversionProvider")
    void toBigInteger_exceptionScenarios(String description, Object source, Class<? extends Throwable> expectedException) {
        Converter converter = mock(Converter.class);
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toBigInteger(source, converter), description);
    }
}
