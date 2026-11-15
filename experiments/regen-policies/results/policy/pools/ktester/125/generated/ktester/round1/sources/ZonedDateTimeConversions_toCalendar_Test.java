package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toCalendar_Test {

    /**
     * Shared options for all tests.
     */
    private static ConverterOptions options;

    /**
     * Converter instance created before each test.
     */
    private Converter converter;

    /**
     * Shared options for the default tests (UTC).
     */
    private static ConverterOptions defaultOptions;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise ConverterOptions once for the whole test suite.
        options = new ConverterOptions();
        // Use a deterministic time‑zone so that the expected Calendar time‑zone is known.
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void setupBeforeEach() {
        // The normal, non‑stubbed converter used by the simple happy‑path test.
        converter = new Converter(defaultOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release references – not strictly required but keeps the pattern clear.
        converter = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Clean up static resources if any (none in this case).
        options = null;
    }

    /**
     * Asserts that a {@link Calendar} contains the expected date‑time fields.
     */
    private static void assertCalendarFields(Calendar cal, int year, int month, int day, int hour, int minute, int second, int milli) {
        assertAll("Calendar fields", () -> assertEquals(year, cal.get(Calendar.YEAR), "YEAR"), () -> assertEquals(month, cal.get(Calendar.MONTH), "MONTH"), () -> assertEquals(day, cal.get(Calendar.DAY_OF_MONTH), "DAY_OF_MONTH"), () -> assertEquals(hour, cal.get(Calendar.HOUR_OF_DAY), "HOUR_OF_DAY"), () -> assertEquals(minute, cal.get(Calendar.MINUTE), "MINUTE"), () -> assertEquals(second, cal.get(Calendar.SECOND), "SECOND"), () -> assertEquals(milli, cal.get(Calendar.MILLISECOND), "MILLISECOND"));
    }

    /**
     * Creates a stub {@link Converter} whose {@code getOptions()} returns an {@link ConverterOptions}
     *  with the supplied {@link TimeZone}.
     */
    private static Converter converterWithTimeZone(ZoneId zoneId) {
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone(zoneId));
        return mockConverter;
    }

    /**
     * Stub that returns {@code null} from {@code getOptions()}.
     */
    private static Converter converterWithNullOptions() {
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(null);
        return mockConverter;
    }

    /**
     * Stub whose {@code getOptions()} returns an options object with a {@code null} time‑zone.
     */
    private static Converter converterWithNullTimeZone() {
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(null);
        return mockConverter;
    }

    /**
     * Stub whose {@code getOptions()} throws the supplied exception.
     */
    private static Converter converterWithOptionsThrowing(RuntimeException ex) {
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenThrow(ex);
        return mockConverter;
    }

    @Test
    void toCalendar_ShouldReturnCalendarWithSameInstantAndTimeZone() {
        // GIVEN: a ZonedDateTime at a known instant and the Converter configured with UTC.
        ZonedDateTime zdt = ZonedDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        10, // hour, minute, second, nano
        12, // hour, minute, second, nano
        30, // hour, minute, second, nano
        45, // hour, minute, second, nano
        0, // original zone – will be ignored for the instant value
        ZoneId.of("America/New_York"));
        // WHEN: converting to Calendar via the static method.
        Calendar result = ZonedDateTimeConversions.toCalendar(zdt, converter);
        // THEN: the Calendar reflects the same epoch‑millis as the ZonedDateTime.
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTimeInMillis(), "Calendar should represent the same instant as the source ZonedDateTime");
        // AND: the Calendar uses the time‑zone supplied in ConverterOptions (UTC).
        assertEquals(options.getTimeZone(), result.getTimeZone(), "Calendar time‑zone must match the ConverterOptions time‑zone");
    }

    @Test
    void toCalendar_ShouldRespectConverterTimeZoneAndInstant() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 15, 10, 30, 45, 123_000_000, // original zone is irrelevant for the instant
        ZoneId.of("America/New_York"));
        Converter stub = converterWithTimeZone(ZoneId.of("Europe/Paris"));
        // WHEN
        Calendar cal = ZonedDateTimeConversions.toCalendar(source, stub);
        // THEN – same instant
        assertEquals(source.toInstant().toEpochMilli(), cal.getTimeInMillis(), "Calendar must represent the same instant as the source ZonedDateTime");
        // THEN – time‑zone from the converter
        assertEquals(TimeZone.getTimeZone("Europe/Paris"), cal.getTimeZone(), "Calendar time‑zone must match the Converter's configured zone");
        // THEN – individual fields (Paris is UTC+2 on 2023‑05‑15)
        assertCalendarFields(cal, 2023, Calendar.MAY, 15, 14, 30, 45, 123);
    }

    @Test
    void toCalendar_EpochStart_ShouldProduce1970_01_01_UTC() {
        // GIVEN
        ZonedDateTime epoch = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.of("UTC"));
        Converter stub = converterWithTimeZone(ZoneId.of("UTC"));
        // WHEN
        Calendar cal = ZonedDateTimeConversions.toCalendar(epoch, stub);
        // THEN
        assertEquals(0L, cal.getTimeInMillis(), "Epoch start should have 0 milliseconds");
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone(), "Time‑zone must be UTC");
        assertCalendarFields(cal, 1970, Calendar.JANUARY, 1, 0, 0, 0, 0);
    }

    @Test
    void toCalendar_FractionalNanosecondsTruncation() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 15, 10, 30, 45, 999_999_999, ZoneId.of("UTC"));
        Converter stub = converterWithTimeZone(ZoneId.of("UTC"));
        // WHEN
        Calendar cal = ZonedDateTimeConversions.toCalendar(source, stub);
        // THEN – nanoseconds are truncated to milliseconds (999 ms)
        // 2023‑05‑15T10:30:45.999Z
        long expectedEpoch = 1684233045999L;
        assertEquals(expectedEpoch, cal.getTimeInMillis(), "Nanoseconds beyond milliseconds must be truncated");
        assertCalendarFields(cal, 2023, Calendar.MAY, 15, 10, 30, 45, 999);
    }

    @Test
    void toCalendar_NullFrom_ShouldThrowNullPointerException() {
        // GIVEN
        Converter stub = converterWithTimeZone(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toCalendar(null, stub), "Passing a null 'from' argument must result in NPE");
    }

    @Test
    void toCalendar_InvalidSourceType_ShouldThrowClassCastException() {
        // GIVEN
        LocalDateTime wrongType = LocalDateTime.of(2023, 5, 15, 10, 30);
        Converter stub = converterWithTimeZone(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toCalendar(wrongType, stub), "A non‑ZonedDateTime source must cause a ClassCastException");
    }

    @Test
    void toCalendar_NullConverter_ShouldThrowNullPointerException() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toCalendar(source, null), "A null Converter should cause NPE when the method dereferences it");
    }

    @Test
    void toCalendar_NullOptions_ShouldThrowNullPointerException() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC"));
        Converter stub = converterWithNullOptions();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toCalendar(source, stub), "Null options from Converter must cause NPE");
    }

    @Test
    void toCalendar_NullTimeZone_ShouldThrowNullPointerException() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        Converter stub = converterWithNullTimeZone();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toCalendar(source, stub), "A null TimeZone inside ConverterOptions must cause NPE");
    }

    @Test
    void toCalendar_OptionsMethodThrows_ShouldPropagateException() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.now();
        RuntimeException cause = new RuntimeException("Options failure");
        Converter stub = converterWithOptionsThrowing(cause);
        // WHEN / THEN
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ZonedDateTimeConversions.toCalendar(source, stub), "Exception from getOptions() should be propagated");
        assertSame(cause, thrown, "The original exception must be re‑thrown unchanged");
    }
}
