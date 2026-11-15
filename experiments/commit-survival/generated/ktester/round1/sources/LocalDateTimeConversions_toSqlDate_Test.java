package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toSqlDate_Test {

    /**
     * Converter instance used for all tests.
     */
    private static Converter converter;

    /**
     * Preserve the original default time‑zone so we can restore it after the suite.
     */
    private static TimeZone originalDefaultZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the default constructor – it creates an empty options set.
        converter = new Converter(new ConverterOptions());
        // Force the JVM default zone to UTC for deterministic temporal tests.
        originalDefaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone.
        TimeZone.setDefault(originalDefaultZone);
        converter = null;
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> validNumericProvider() {
        return Stream.of(Arguments.of(1622548800000L, new Date(1622548800000L)), Arguments.of(12345, new Date(12345L)));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> stringNumericProvider() {
        return Stream.of(// valid numeric string
        Arguments.of("1622548800000", new Date(1622548800000L)), // non‑numeric string – expect exception
        Arguments.of("not-a-number", ConversionException.class));
    }

    @Test
    void toSqlDate_fromLong_returnsDateWithSameEpochMillis() {
        // GIVEN
        // 2021‑07‑11T00:00:00Z
        long epochMillis = 1_625_097_600_000L;
        // WHEN
        Date actual = LocalDateTimeConversions.toSqlDate(epochMillis, converter);
        // THEN
        assertEquals(new Date(epochMillis), actual);
    }

    @Test
    void toSqlDate_fromIntegerNumber_isConvertedToDate() {
        // GIVEN
        // interpreted as milliseconds since epoch
        int number = 12_345;
        // WHEN
        Date actual = LocalDateTimeConversions.toSqlDate(number, converter);
        // THEN
        assertEquals(new Date((long) number), actual);
    }

    @Test
    void toSqlDate_fromStringNumber_isConvertedToDate() {
        // GIVEN
        String numberAsString = "9876543210";
        // WHEN
        Date actual = LocalDateTimeConversions.toSqlDate(numberAsString, converter);
        // THEN
        assertEquals(new Date(Long.parseLong(numberAsString)), actual);
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("validNumericProvider")
    void toSqlDate_validNumericConversions(Object from, Date expected) {
        Date actual = LocalDateTimeConversions.toSqlDate(from, converter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\"")
    @MethodSource("stringNumericProvider")
    void toSqlDate_stringNumericConversion(Object from, Object expected) {
        if (expected instanceof Date) {
            Date actual = LocalDateTimeConversions.toSqlDate(from, converter);
            assertEquals(expected, actual);
        } else {
            assertThrows((Class<?>) expected, () -> LocalDateTimeConversions.toSqlDate(from, converter));
        }
    }

    // -----------------------------------------------------------------
    @Test
    void toSqlDate_fromUtilDate_returnsSameInstant() {
        Date source = new Date(Instant.parse("2022-01-15T08:30:00Z").toEpochMilli());
        Date actual = LocalDateTimeConversions.toSqlDate(source, converter);
        assertEquals(source, actual);
    }

    @Test
    void toSqlDate_fromLocalDateTime_convertsUsingDefaultZone() {
        // 2022‑01‑15T08:30:00 (no zone) -> interpreted as UTC because we forced the default zone to UTC
        LocalDateTime ldt = LocalDateTime.of(2022, 1, 15, 8, 30, 0);
        long expectedMillis = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Date actual = LocalDateTimeConversions.toSqlDate(ldt, converter);
        assertEquals(new Date(expectedMillis), actual);
    }

    @Test
    void toSqlDate_fromInstant_convertsDirectly() {
        Instant instant = Instant.parse("2022-03-10T14:45:30Z");
        Date actual = LocalDateTimeConversions.toSqlDate(instant, converter);
        assertEquals(Date.from(instant), actual);
    }

    // -----------------------------------------------------------------
    @Test
    void toSqlDate_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toSqlDate(null, converter));
    }

    @Test
    void toSqlDate_nullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toSqlDate(1622548800000L, null));
    }

    @Test
    void toSqlDate_booleanSource_throwsConversionException() {
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toSqlDate(Boolean.TRUE, converter));
    }

    @Test
    void toSqlDate_localTimeSource_throwsConversionException() {
        LocalTime lt = LocalTime.of(12, 30, 45);
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toSqlDate(lt, converter));
    }

    @Test
    void toSqlDate_arraySource_throwsConversionException() {
        int[] arr = { 1, 2, 3 };
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toSqlDate(arr, converter));
    }

    @Test
    void toSqlDate_floatingPointSource_throwsConversionException() {
        double d = 12345.67;
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toSqlDate(d, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void toSqlDate_stringOverflow_throwsConversionException() {
        // Long.MAX_VALUE + 1
        String overflow = "9223372036854775808";
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toSqlDate(overflow, converter));
    }

    @Test
    void toSqlDate_bigDecimalOverflow_throwsConversionException() {
        BigDecimal big = new BigDecimal("9223372036854775808");
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toSqlDate(big, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void toSqlDate_converterThrowsException_propagatesConversionException() {
        // Create a mock Converter that throws a RuntimeException when any conversion is attempted.
        Converter throwingConverter = Mockito.mock(Converter.class);
        Mockito.when(throwingConverter.convert(Mockito.any(), Mockito.any())).thenThrow(new RuntimeException("forced failure"));
        // The wrapper in LocalDateTimeConversions should translate this to a ConversionException.
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toSqlDate(1622548800000L, throwingConverter));
    }
}
