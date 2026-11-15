package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DateConversions_toLocalDate_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the library supplies a no‑arg constructor for options.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference (helps GC in long‑running test suites).
        converter = null;
    }

    @Test
    void shouldReturnSameLocalDateWhenSourceIsLocalDate() {
        // given
        LocalDate expected = LocalDate.of(2023, Month.APRIL, 15);
        // when
        LocalDate actual = DateConversions.toLocalDate(expected, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void shouldExtractDatePartFromLocalDateTime() {
        // given
        LocalDateTime source = LocalDateTime.of(2022, 12, 31, 23, 59, 59);
        LocalDate expected = source.toLocalDate();
        // when
        LocalDate actual = DateConversions.toLocalDate(source, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void shouldExtractDatePartFromZonedDateTime() {
        // given
        ZonedDateTime source = ZonedDateTime.of(2021, 7, 4, 12, 0, 0, 0, ZoneId.of("America/New_York"));
        LocalDate expected = source.toLocalDate();
        // when
        LocalDate actual = DateConversions.toLocalDate(source, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertUtilDateToLocalDate() {
        // given
        Instant instant = Instant.parse("2020-01-02T10:15:30Z");
        Date source = Date.from(instant);
        LocalDate expected = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        // when
        LocalDate actual = DateConversions.toLocalDate(source, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertCalendarToLocalDate() {
        // given
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2019, Calendar.MARCH, 9, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Instant instant = cal.toInstant();
        LocalDate expected = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        // when
        LocalDate actual = DateConversions.toLocalDate(cal, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertStringToLocalDate() {
        // given – the built‑in converter understands ISO‑8601 dates
        String source = "2018-11-05";
        LocalDate expected = LocalDate.parse(source);
        // when
        LocalDate actual = DateConversions.toLocalDate(source, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertEpochMilliLongToLocalDate() {
        // given
        // 2020‑09‑13T12:26:40Z
        long epochMilli = 1_600_000_000_000L;
        Instant instant = Instant.ofEpochMilli(epochMilli);
        LocalDate expected = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        // when
        LocalDate actual = DateConversions.toLocalDate(epochMilli, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        // given
        Object source = null;
        // when
        LocalDate actual = DateConversions.toLocalDate(source, converter);
        // then
        assertNull(actual);
    }

    @Test
    void shouldReturnSameLocalDateWhenConverterIsNull() {
        // given – a LocalDate source and a null Converter reference
        LocalDate source = LocalDate.of(2022, 12, 31);
        // when
        LocalDate actual = DateConversions.toLocalDate(source, null);
        // then
        assertEquals(source, actual, "When the source is already a LocalDate the converter argument may be null.");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenSourceIsNull() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> DateConversions.toLocalDate(source, converter), "A null source should cause a NullPointerException because toZonedDateTime cannot resolve it.");
    }
}
