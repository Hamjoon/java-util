package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.mockito.MockedStatic;
import java.util.Date;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class StringConversions_toDate_Test {

    @Mock
    Converter // a mock Converter – its behaviour is not needed for these tests
    mockConverter;

    private static Stream<Arguments> successfulConversionsProvider() {
        return Stream.of(// case 1 – arbitrary string, mock converter returns a concrete ZDT
        Arguments.of("anyString", ZonedDateTime.ofInstant(Instant.parse("2023-05-10T12:30:00Z"), ZoneOffset.UTC), 1683719400000L, "When the Converter returns a non‑null ZonedDateTime, toDate should return a Date built from that instant."), // case 2 – source already a ZonedDateTime, identity converter
        Arguments.of(ZonedDateTime.of(2021, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC")), ZonedDateTime.ofInstant(Instant.parse("2021-12-31T23:59:59Z"), ZoneOffset.UTC), 1640995199000L, "If the source object is already a ZonedDateTime and the Converter simply returns it, toDate must convert it to the equivalent Date."), // case 3 – source is a java.util.Date, mock converter returns same instant
        Arguments.of(Date.from(Instant.parse("1999-01-01T00:00:00Z")), ZonedDateTime.ofInstant(Instant.parse("1999-01-01T00:00:00Z"), ZoneOffset.UTC), 915148800000L, "When the Converter converts a java.util.Date to a ZonedDateTime with the same instant, toDate should return a Date equal to the original."));
    }

    private static Stream<Arguments> additionalScenariosProvider() {
        return Stream.of(// null input – already covered by a dedicated test, but kept for completeness
        Arguments.of(null, null, null, null, "When the source value is null the method should return null regardless of the converter."), // non‑null input, converter returns null
        Arguments.of("anyString", null, null, null, "If the supplied Converter returns null for a non‑null input, toDate must propagate the null result."), // Timestamp conversion
        Arguments.of(Timestamp.valueOf("2022-03-15 10:20:30.123"), ZonedDateTime.ofInstant(Instant.parse("2022-03-15T10:20:30.123Z"), ZoneOffset.UTC), 1647349230123L, null, "A java.sql.Timestamp converted via a mock converter should yield a matching Date."), // Long epoch millis conversion
        Arguments.of(1622505600000L, ZonedDateTime.ofInstant(Instant.ofEpochMilli(1622505600000L), ZoneOffset.UTC), 1622505600000L, null, "When the source is a Long containing epoch milliseconds, the resulting Date must have the identical epoch value."), // Calendar conversion (GMT)
        Arguments.of(createCalendar(2020, Calendar.JANUARY, 2, 3, 4, 5, 0, "GMT"), ZonedDateTime.ofInstant(Instant.parse("2020-01-02T03:04:05Z"), ZoneOffset.UTC), 1577958245000L, null, "A Calendar set to a known GMT instant is converted; the resulting Date must equal that instant."), // Instant identity conversion
        Arguments.of(Instant.parse("2023-07-04T12:00:00Z"), ZonedDateTime.ofInstant(Instant.parse("2023-07-04T12:00:00Z"), ZoneOffset.UTC), 1688472000000L, null, "An Instant passed directly should be turned into a Date representing the same instant."), // ZonedDateTime with non‑UTC zone
        Arguments.of(ZonedDateTime.of(2022, 10, 5, 15, 30, 0, 0, ZoneId.of("America/New_York")), ZonedDateTime.ofInstant(Instant.parse("2022-10-05T19:30:00Z"), ZoneOffset.UTC), 1664998200000L, null, "A ZonedDateTime with a non‑UTC zone is converted; the Date must reflect the same instant in UTC."), // ISO‑8601 string with offset parsed by mock converter
        Arguments.of("2021-12-25T10:15:30+02:00", ZonedDateTime.ofInstant(Instant.parse("2021-12-25T08:15:30Z"), ZoneOffset.UTC), 1640420130000L, null, "An ISO‑8601 string with an offset parsed by the mock converter must yield the corresponding UTC Date."), // ZonedDateTime with nanosecond precision (truncation to millis)
        Arguments.of(ZonedDateTime.ofInstant(Instant.ofEpochSecond(1609459200, 123_456_789), ZoneOffset.UTC), ZonedDateTime.ofInstant(Instant.ofEpochSecond(1609459200, 123_456_789), ZoneOffset.UTC), 1609459200123L, null, "A ZonedDateTime containing nanosecond precision is converted; java.util.Date stores only milliseconds, so the nanoseconds are truncated."), // Unsupported type (UUID) – converter returns null
        Arguments.of(UUID.randomUUID(), null, null, null, "An unsupported source type for which the converter returns null should result in a null Date."));
    }

    /**
     * Helper to create a Calendar instance with the supplied fields and time‑zone.
     */
    private static Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, String tzId) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(tzId));
        cal.setLenient(false);
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    @Test
    void testToDate_NullInput_ReturnsNull() {
        // GIVEN
        Object input = null;
        // WHEN
        Date result = StringConversions.toDate(input, mockConverter);
        // THEN
        assertNull(result, "toDate should return null when the source object is null");
    }

    @Test
    void testToDate_ValidZonedDateTime_ReturnsEquivalentDate() {
        // GIVEN
        // an arbitrary instant
        long expectedEpochMilli = 1_617_283_945_123L;
        ZonedDateTime mockedZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(expectedEpochMilli), ZoneOffset.UTC);
        // the actual value is irrelevant because we mock the static call
        Object dummyInput = new Object();
        // Mock the static method StringConversions.toZonedDateTime(...)
        try (MockedStatic<StringConversions> staticMock = Mockito.mockStatic(StringConversions.class)) {
            staticMock.when(() -> StringConversions.toZonedDateTime(any(), any())).thenReturn(mockedZdt);
            // WHEN
            Date result = StringConversions.toDate(dummyInput, mockConverter);
            // THEN
            assertNotNull(result, "Resulting Date should not be null");
            assertEquals(expectedEpochMilli, result.getTime(), "The Date returned by toDate must represent the same instant as the ZonedDateTime supplied by toZonedDateTime");
            // Verify that the static method was indeed called with the supplied arguments
            staticMock.verify(() -> StringConversions.toZonedDateTime(dummyInput, mockConverter), times(1));
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("successfulConversionsProvider")
    void testToDate_successfulConversions(Object from, ZonedDateTime mockedZdt, Long expectedEpochMillis, String description) {
        // Mock static call toStringConversions.toZonedDateTime(...)
        try (MockedStatic<StringConversions> staticMock = mockStatic(StringConversions.class, CALLS_REAL_METHODS)) {
            staticMock.when(() -> StringConversions.toZonedDateTime(any(), any())).thenReturn(mockedZdt);
            // WHEN
            Date result = StringConversions.toDate(from, mockConverter);
            // THEN
            assertNotNull(result, "Resulting Date should not be null");
            assertEquals(expectedEpochMillis.longValue(), result.getTime(), "Date epoch millis must match the ZonedDateTime supplied by the mocked converter");
            // Verify the static method was called exactly once with the supplied arguments
            staticMock.verify(() -> StringConversions.toZonedDateTime(from, mockConverter), times(1));
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("additionalScenariosProvider")
    void testToDate_additionalScenarios(Object from, ZonedDateTime mockedZdt, Long expectedEpochMillis, Class<? extends Throwable> expectedException, String description) {
        try (MockedStatic<StringConversions> staticMock = mockStatic(StringConversions.class, CALLS_REAL_METHODS)) {
            if (expectedException != null) {
                // Stub to throw the required exception
                staticMock.when(() -> StringConversions.toZonedDateTime(any(), any())).thenThrow(expectedException.getDeclaredConstructor(String.class).newInstance("mocked exception"));
            } else {
                // Normal stub (may be null to simulate a null conversion)
                staticMock.when(() -> StringConversions.toZonedDateTime(any(), any())).thenReturn(mockedZdt);
            }
            // WHEN / THEN
            if (expectedException != null) {
                assertThrows(expectedException, () -> StringConversions.toDate(from, mockConverter), description);
            } else {
                Date result = StringConversions.toDate(from, mockConverter);
                if (mockedZdt == null) {
                    assertNull(result, description);
                } else {
                    assertNotNull(result, description);
                    assertEquals(expectedEpochMillis.longValue(), result.getTime(), description);
                }
            }
            // Verify that the static method was invoked (unless we expected an NPE before the call)
            staticMock.verify(() -> StringConversions.toZonedDateTime(from, mockConverter), times(1));
        } catch (ReflectiveOperationException e) {
            fail("Failed to instantiate expected exception: " + e.getMessage());
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToDate_nullConverter_throwsNPE() {
        Object from = "2023-01-01T00:00:00Z";
        Converter nullConverter = null;
        assertThrows(NullPointerException.class, () -> StringConversions.toDate(from, nullConverter), "When the supplied Converter instance is null, the method should throw NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToDate_invalidStringParsing_propagatesDateTimeParseException() {
        Object from = "not-a-date";
        try (MockedStatic<StringConversions> staticMock = mockStatic(StringConversions.class, CALLS_REAL_METHODS)) {
            staticMock.when(() -> StringConversions.toZonedDateTime(any(), any())).thenThrow(new DateTimeParseException("Invalid format", "not-a-date", 0));
            assertThrows(DateTimeParseException.class, () -> StringConversions.toDate(from, mockConverter), "An unparsable ISO‑8601 string should cause a DateTimeParseException to be thrown");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToDate_epochMillisOverflow_propagatesDateTimeException() {
        // 9223372036854775807L
        Object from = Long.MAX_VALUE;
        try (MockedStatic<StringConversions> staticMock = mockStatic(StringConversions.class, CALLS_REAL_METHODS)) {
            staticMock.when(() -> StringConversions.toZonedDateTime(any(), any())).thenThrow(new DateTimeException("Epoch millis overflow"));
            assertThrows(DateTimeException.class, () -> StringConversions.toDate(from, mockConverter), "A Long value exceeding Instant range should cause DateTimeException");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToDate_converterThrowsRuntimeException_propagatesException() {
        Object from = "2022-12-31T23:59:59Z";
        try (MockedStatic<StringConversions> staticMock = mockStatic(StringConversions.class, CALLS_REAL_METHODS)) {
            staticMock.when(() -> StringConversions.toZonedDateTime(any(), any())).thenThrow(new RuntimeException("converter failure"));
            RuntimeException ex = assertThrows(RuntimeException.class, () -> StringConversions.toDate(from, mockConverter), "RuntimeException from the converter must be propagated unchanged");
            assertEquals("converter failure", ex.getMessage());
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToDate_unsupportedTypeIllegalArgument_propagatesException() {
        Object from = new Object();
        try (MockedStatic<StringConversions> staticMock = mockStatic(StringConversions.class, CALLS_REAL_METHODS)) {
            staticMock.when(() -> StringConversions.toZonedDateTime(any(), any())).thenThrow(new IllegalArgumentException("Unsupported type"));
            assertThrows(IllegalArgumentException.class, () -> StringConversions.toDate(from, mockConverter), "Passing an unsupported object type should result in IllegalArgumentException");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testToDate_localDateWithoutZone_propagatesDateTimeException() {
        Object from = LocalDate.of(2023, 3, 15);
        try (MockedStatic<StringConversions> staticMock = mockStatic(StringConversions.class, CALLS_REAL_METHODS)) {
            staticMock.when(() -> StringConversions.toZonedDateTime(any(), any())).thenThrow(new DateTimeException("Missing zone information"));
            assertThrows(DateTimeException.class, () -> StringConversions.toDate(from, mockConverter), "Converting a LocalDate without zone information should raise DateTimeException");
        }
    }
}
