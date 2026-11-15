package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toLocalTime_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure deterministic behaviour for Date/Calendar/Long conversions that depend on the system zone.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
        converter = null;
    }

    @Test
    void toLocalTime_fromString_returnsExpectedLocalTime() {
        // GIVEN
        // ISO‑8601 offset date‑time
        String source = "2023-08-15T13:45:30+02:00";
        // WHEN
        LocalTime result = OffsetDateTimeConversions.toLocalTime(source, converter);
        // THEN
        assertEquals(LocalTime.of(13, 45, 30), result, "The LocalTime extracted from the ISO‑8601 string should match the time component.");
    }

    @Test
    void toLocalTime_fromOffsetDateTime_returnsExpectedLocalTime() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2021, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        // WHEN
        LocalTime result = OffsetDateTimeConversions.toLocalTime(odt, converter);
        // THEN
        assertEquals(LocalTime.of(23, 59, 59), result, "The LocalTime extracted from an OffsetDateTime should be its time component.");
    }

    @Test
    void toLocalTime_fromZonedDateTime_returnsExpectedLocalTime() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2022, 5, 10, 08, 30, 15, 0, ZoneOffset.ofHours(-5));
        // WHEN
        LocalTime result = OffsetDateTimeConversions.toLocalTime(zdt, converter);
        // THEN
        assertEquals(LocalTime.of(8, 30, 15), result, "The LocalTime extracted from a ZonedDateTime should be its time component.");
    }

    @Test
    void toLocalTime_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toLocalTime(nullSource, converter), "Passing a null source should result in a NullPointerException because the underlying " + "toZonedDateTime conversion returns null and .toLocalTime() is invoked on it.");
    }

    // ---------- OffsetDateTime ----------
    @Test
    void toLocalTime_FromOffsetDateTime() {
        OffsetDateTime src = OffsetDateTime.parse("2023-03-15T10:15:30+02:00");
        LocalTime result = OffsetDateTimeConversions.toLocalTime(src, null);
        assertEquals(LocalTime.of(10, 15, 30), result, "Converts an OffsetDateTime with a positive offset to its LocalTime component, ignoring the offset.");
    }

    // ---------- ZonedDateTime ----------
    @Test
    void toLocalTime_FromZonedDateTime() {
        ZonedDateTime src = ZonedDateTime.parse("2023-03-15T23:45:01.123456789-05:00[America/New_York]");
        LocalTime result = OffsetDateTimeConversions.toLocalTime(src, null);
        assertEquals(LocalTime.of(23, 45, 1, 123_456_789), result, "Converts a ZonedDateTime with a negative offset and nanosecond precision to LocalTime, preserving the wall‑clock time.");
    }

    // ---------- LocalDateTime ----------
    @Test
    void toLocalTime_FromLocalDateTime() {
        LocalDateTime src = LocalDateTime.of(2023, 12, 31, 0, 0, 0);
        LocalTime result = OffsetDateTimeConversions.toLocalTime(src, null);
        assertEquals(LocalTime.of(0, 0), result, "Converts a LocalDateTime at midnight to LocalTime.");
    }

    // ---------- java.util.Date ----------
    @Test
    void toLocalTime_FromDate() {
        Date src = Date.from(Instant.parse("2023-03-15T12:30:45Z"));
        LocalTime result = OffsetDateTimeConversions.toLocalTime(src, null);
        assertEquals(LocalTime.of(12, 30, 45), result, "Converts a java.util.Date representing an instant in UTC to LocalTime (system default zone is UTC for the test).");
    }

    // ---------- Calendar ----------
    @Test
    void toLocalTime_FromCalendar() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(2023, Calendar.MARCH, 15, 8, 20, 55);
        calendar.set(Calendar.MILLISECOND, 250);
        LocalTime result = OffsetDateTimeConversions.toLocalTime(calendar, null);
        assertEquals(LocalTime.of(8, 20, 55, 250_000_000), result, "Converts a Calendar set to UTC with millisecond precision to LocalTime, preserving the milliseconds.");
    }

    // ---------- Long epoch millis ----------
    @Test
    void toLocalTime_FromLongEpochMillis() {
        // 2023‑03‑15T00:00:00Z
        long epochMillis = 1678886400000L;
        LocalTime result = OffsetDateTimeConversions.toLocalTime(epochMillis, null);
        assertEquals(LocalTime.MIDNIGHT, result, "Converts a long representing epoch milliseconds to LocalTime; default zone is UTC, so result is midnight.");
    }

    // ---------- ISO‑8601 String ----------
    @Test
    void toLocalTime_FromString_ISO8601() {
        String src = "2023-03-15T14:05:06.789+03:00";
        LocalTime result = OffsetDateTimeConversions.toLocalTime(src, null);
        assertEquals(LocalTime.of(14, 5, 6, 789_000_000), result, "Converts an ISO‑8601 string with offset to LocalTime, discarding the offset.");
    }

    // ---------- Custom pattern String with custom Converter ----------
    @Test
    void toLocalTime_FromString_CustomPattern() {
        // custom converter that parses "dd-MM-yyyy HH:mm:ss" into a LocalDateTime
        Converter customConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                DateTimeFormatter f = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                return LocalDateTime.parse((String) src, f);
            }
        };
        String src = "15-03-2023 22:59:59";
        LocalTime result = OffsetDateTimeConversions.toLocalTime(src, customConverter);
        assertEquals(LocalTime.of(22, 59, 59), result, "Uses a custom Converter that parses a non‑ISO string to LocalDateTime, then extracts the time component.");
    }

    // ---------- Null input ----------
    @Test
    void toLocalTime_NullInput() {
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toLocalTime(null, null), "Passing null as the source object should result in a NullPointerException.");
    }

    // ---------- Unsupported type ----------
    @Test
    void toLocalTime_UnsupportedType() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toLocalTime(Boolean.TRUE, null), "An unsupported source type (Boolean) should cause an IllegalArgumentException.");
    }

    // ---------- Invalid ISO‑8601 string ----------
    @Test
    void toLocalTime_InvalidISOString() {
        String src = "2023-13-40T25:61:61+02:00";
        assertThrows(DateTimeParseException.class, () -> OffsetDateTimeConversions.toLocalTime(src, null), "An ISO‑8601 string with an impossible date and time should cause a parsing exception.");
    }

    // ---------- Unsupported string without custom converter ----------
    @Test
    void toLocalTime_UnsupportedStringWithoutConverter() {
        String src = "15/03/2023 22:59:59";
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toLocalTime(src, null), "A non‑ISO string that cannot be parsed by the default logic and no custom Converter is supplied should result in an IllegalArgumentException.");
    }

    // ---------- Converter returns null ----------
    @Test
    void toLocalTime_ConverterReturnsNull() {
        Converter nullReturningConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                // deliberately return null
                return null;
            }
        };
        Object src = new Object();
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toLocalTime(src, nullReturningConverter), "When the provided Converter returns null, a NullPointerException should be thrown when .toLocalTime() is invoked.");
    }

    // ---------- Converter throws exception ----------
    @Test
    void toLocalTime_ConverterThrowsException() {
        Converter explodingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new IllegalStateException("conversion failed");
            }
        };
        Object src = new Object();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> OffsetDateTimeConversions.toLocalTime(src, explodingConverter), "If the custom Converter throws a runtime exception, it should propagate out of toLocalTime.");
        assertEquals("conversion failed", ex.getMessage());
    }

    // ---------- Null converter with a type that requires conversion ----------
    @Test
    void toLocalTime_NullConverterWithCustomObject() {
        BigDecimal src = new BigDecimal("123.45");
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toLocalTime(src, null), "A source type (BigDecimal) that is not directly handled by toZonedDateTime and requires a Converter results in IllegalArgumentException when the converter is null.");
    }
}
