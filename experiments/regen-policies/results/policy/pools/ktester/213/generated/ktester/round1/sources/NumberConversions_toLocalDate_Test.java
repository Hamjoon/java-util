package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toLocalDate_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – no special configuration required for the conversions we test.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Supplies the arguments for {@link #testToLocalDateExceptions(Object, Converter, Class, String)}.
     */
    private static Stream<Arguments> exceptionCases() {
        // A valid converter instance for the cases that need it.
        Converter validConverter = converter;
        return Stream.of(// 1. Null source object – toZonedDateTime returns null → NPE when calling toLocalDate()
        Arguments.of(null, validConverter, NullPointerException.class, "Null source object should cause NullPointerException"), // 2. Unsupported plain Object – conversion cannot map to ZonedDateTime
        Arguments.of(new Object(), validConverter, IllegalArgumentException.class, "Unsupported type (Object) should cause IllegalArgumentException"), // 3. Invalid date string (February 30) – parsing fails
        Arguments.of("2023-02-30T10:15:30", validConverter, DateTimeParseException.class, "Invalid date string (Feb 30) should cause DateTimeParseException"), // 4. Completely unparsable string
        Arguments.of("not-a-date", validConverter, DateTimeParseException.class, "Non‑date string should cause DateTimeParseException"), // 5. LocalDate without zone information – toZonedDateTime cannot create a ZonedDateTime
        Arguments.of(LocalDate.now(), validConverter, DateTimeException.class, "LocalDate lacks time‑zone; should cause DateTimeException"), // 6. Instant out of ZonedDateTime range – overflow leads to DateTimeException
        Arguments.of(Instant.ofEpochSecond(Long.MAX_VALUE), validConverter, DateTimeException.class, "Out‑of‑range Instant should cause DateTimeException"), // 7. Null converter – internal logic dereferences it, causing NPE
        Arguments.of(ZonedDateTime.now(), null, NullPointerException.class, "Null converter should cause NullPointerException"), // 8. AtomicInteger – not a supported temporal type
        Arguments.of(new AtomicInteger(5), validConverter, IllegalArgumentException.class, "AtomicInteger should cause IllegalArgumentException"));
    }

    @Test
    void testFromLocalDate() {
        // given
        LocalDate source = LocalDate.of(2022, Month.MARCH, 15);
        // when
        LocalDate result = NumberConversions.toLocalDate(source, converter);
        // then
        assertEquals(source, result);
    }

    @Test
    void testFromLocalDateTime() {
        // given
        LocalDateTime source = LocalDateTime.of(2022, Month.MARCH, 15, 10, 30, 45);
        LocalDate expected = source.toLocalDate();
        // when
        LocalDate result = NumberConversions.toLocalDate(source, converter);
        // then
        assertEquals(expected, result);
    }

    @Test
    void testFromZonedDateTime() {
        // given
        ZonedDateTime source = ZonedDateTime.of(2022, 3, 15, 10, 30, 45, 0, ZoneId.of("UTC"));
        LocalDate expected = source.toLocalDate();
        // when
        LocalDate result = NumberConversions.toLocalDate(source, converter);
        // then
        assertEquals(expected, result);
    }

    @Test
    void testFromInstant() {
        // given
        Instant instant = Instant.parse("2022-03-15T10:30:45Z");
        LocalDate expected = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        // when
        LocalDate result = NumberConversions.toLocalDate(instant, converter);
        // then
        assertEquals(expected, result);
    }

    @Test
    void testFromDate() {
        // given
        Instant instant = Instant.parse("2022-03-15T10:30:45Z");
        Date source = Date.from(instant);
        LocalDate expected = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        // when
        LocalDate result = NumberConversions.toLocalDate(source, converter);
        // then
        assertEquals(expected, result);
    }

    @Test
    void testFromCalendar() {
        // given
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2022, Calendar.MARCH, 15, 10, 30, 45);
        Date source = cal.getTime();
        LocalDate expected = source.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        // when
        LocalDate result = NumberConversions.toLocalDate(cal, converter);
        // then
        assertEquals(expected, result);
    }

    /**
     * Parameterized test that verifies the various exception scenarios for
     * {@link NumberConversions#toLocalDate(Object, Converter)}.
     *
     * Each {@link Arguments} entry contains:
     * <ul>
     *   <li>the source object (or {@code null})</li>
     *   <li>the {@link Converter} instance (or {@code null})</li>
     *   <li>the expected exception class</li>
     *   <li>a description (used only for readability in the test source)</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("exceptionCases")
    void testToLocalDateExceptions(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        // The test simply asserts that the expected exception is thrown.
        assertThrows(expectedException, () -> NumberConversions.toLocalDate(from, conv));
    }
}
