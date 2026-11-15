package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toDate_Test {

    /**
     * Converter used for all tests – created once for the whole test suite.
     */
    private static Converter converter;

    /**
     * Converter used for the simple “no‑converter” tests – created once for the whole suite.
     */
    private static Converter defaultConverter;

    /**
     * Mocked converter that can be programmed per‑test.
     */
    @Mock
    private Converter mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter is only needed for the tests that deliberately pass a non‑null instance.
        defaultConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the pattern tidy.
        converter = null;
    }

    /**
     * Creates an {@link Instant} from epoch milliseconds safely handling overflow.
     */
    private static Instant safeInstantFromMillis(long epochMilli) {
        return Instant.ofEpochMilli(epochMilli);
    }

    /**
     * Provides arguments for the “Valid conversions” parameterized test.
     */
    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// java.util.Date – the method should simply return the same instant.
        Arguments.of(new Date(1622548800000L), null, new Date(1622548800000L), "Pass a java.util.Date instance; toDate should return an equal Date."), // java.time.Instant – direct mapping.
        Arguments.of(Instant.parse("2022-01-15T08:30:00Z"), null, Date.from(Instant.parse("2022-01-15T08:30:00Z")), "Pass an Instant; toDate should produce the corresponding Date."), // LocalDateTime with a mock converter that resolves it using the system default zone.
        Arguments.of(LocalDateTime.of(2023, 3, 10, 14, 45, 30), "mock", Date.from(LocalDateTime.of(2023, 3, 10, 14, 45, 30).atZone(ZoneId.systemDefault()).toInstant()), "Pass a LocalDateTime together with a Converter that knows how to turn it into an Instant."), // java.sql.Timestamp – should map to the same epoch millis.
        Arguments.of(Timestamp.valueOf("2024-07-20 09:15:00"), null, Date.from(Instant.parse("2024-07-20T09:15:00Z")), "Pass a java.sql.Timestamp; conversion should reach Instant and finally a Date."));
    }

    /**
     * Provides arguments for the “Additional valid conversions” parameterized test.
     */
    private static Stream<Arguments> additionalValidConversionsProvider() {
        // Helper calendar in UTC set to 2022‑05‑01 00:00:00Z
        Calendar utcCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(1651363200000L);
        utcCal.setLenient(false);
        return Stream.of(// Calendar (UTC)
        Arguments.of(utcCal, null, Date.from(Instant.ofEpochMilli(1651363200000L)), "Convert a java.util.Calendar (UTC) set to 2022‑05‑01 00:00:00."), // LocalDate – start of day in system default zone.
        Arguments.of(LocalDate.of(2022, 12, 31), null, Date.from(LocalDate.of(2022, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()), "Convert a LocalDate; treated as start of day in the JVM default zone."), // LocalTime – combined with today’s date in system default zone.
        Arguments.of(LocalTime.of(13, 45, 20), null, Date.from(LocalDate.now().atTime(LocalTime.of(13, 45, 20)).atZone(ZoneId.systemDefault()).toInstant()), "Convert a LocalTime; combined with current date in system default zone."), // OffsetDateTime – offset applied.
        Arguments.of(OffsetDateTime.parse("2021-05-20T10:15:30+02:00"), null, Date.from(OffsetDateTime.parse("2021-05-20T10:15:30+02:00").toInstant()), "Convert an OffsetDateTime; offset should be applied."), // ZonedDateTime – zone offset accounted for.
        Arguments.of(ZonedDateTime.of(2020, 2, 29, 23, 59, 59, 0, ZoneId.of("America/New_York")), null, Date.from(ZonedDateTime.of(2020, 2, 29, 23, 59, 59, 0, ZoneId.of("America/New_York")).toInstant()), "Convert a ZonedDateTime in America/New_York."), // BigInteger zero.
        Arguments.of(BigInteger.valueOf(0L), null, Date.from(Instant.ofEpochMilli(0L)), "BigInteger value 0 interpreted as epoch milliseconds."), // BigInteger Long.MAX_VALUE.
        Arguments.of(BigInteger.valueOf(Long.MAX_VALUE), null, Date.from(Instant.ofEpochMilli(Long.MAX_VALUE)), "Upper bound of epoch milliseconds that java.util.Date can hold."), // BigDecimal with fractional seconds.
        Arguments.of(// seconds
        BigDecimal.valueOf(1622548800.5), null, Date.from(Instant.ofEpochMilli(1622548800L * 1000 + 500)), "BigDecimal representing seconds with a fractional part."), // String parsed by a mock converter.
        Arguments.of("2023-01-01T00:00:00Z", "mock", Date.from(Instant.parse("2023-01-01T00:00:00Z")), "A String that the supplied Converter can parse into an Instant."));
    }

    /**
     * Verify that a {@link BigInteger} representing an epoch‑millisecond value is correctly
     * converted to a {@link Date} via {@code BigIntegerConversions.toDate}.
     */
    @Test
    void toDate_convertsBigIntegerEpochMillis_toDate() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        long epochMillis = 1_640_995_200_000L;
        BigInteger bigInt = BigInteger.valueOf(epochMillis);
        Instant expectedInstant = Instant.ofEpochMilli(epochMillis);
        Date expectedDate = Date.from(expectedInstant);
        // WHEN
        Date actual = BigIntegerConversions.toDate(bigInt, converter);
        // THEN
        assertNotNull(actual, "Resulting Date must not be null");
        assertEquals(expectedDate.getTime(), actual.getTime(), "Date produced from BigInteger should match the expected epoch milliseconds");
    }

    /**
     * Verify that a {@link BigInteger} representing an epoch‑second value (with nanos = 0)
     * is also handled correctly.  The conversion logic inside {@code toInstant} treats a
     * {@code BigInteger} as milliseconds, but many implementations also accept seconds.
     * This test confirms the behaviour for the common milliseconds case and guards against
     * accidental changes.
     */
    @Test
    void toDate_convertsBigIntegerEpochSeconds_toDate() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z in seconds
        long epochSeconds = 1_640_995_200L;
        // Convert seconds to milliseconds for the expected Date.
        long epochMillis = epochSeconds * 1_000L;
        BigInteger bigInt = BigInteger.valueOf(epochSeconds);
        Date expected = Date.from(Instant.ofEpochMilli(epochMillis));
        // WHEN
        Date actual = BigIntegerConversions.toDate(bigInt, converter);
        // THEN
        assertNotNull(actual);
        assertEquals(expected.getTime(), actual.getTime(), "Date should represent the same instant when a BigInteger of epoch seconds is supplied");
    }

    /**
     * Ensure that passing {@code null} as the source value results in {@code null}
     * (the Converter returns {@code null} for object types).  The method under test
     * delegates to {@code toInstant}, which follows the same contract.
     */
    @Test
    void toDate_nullInput_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Date result = BigIntegerConversions.toDate(source, converter);
        // THEN
        assertNull(result, "Converting a null source should yield a null Date");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("validConversionsProvider")
    void toDate_validConversions(Object from, String converterKind, Date expected, String description) {
        Converter used = ("mock".equals(converterKind)) ? mockConverter : defaultConverter;
        // If a mock is required, program it to return the expected instant.
        if ("mock".equals(converterKind)) {
            Instant instant = expected.toInstant();
            when(mockConverter.convert(eq(from), eq(Instant.class))).thenReturn(instant);
        }
        Date actual = BigIntegerConversions.toDate(from, used);
        assertNotNull(actual, "Resulting Date must not be null");
        assertEquals(expected.getTime(), actual.getTime(), "Converted Date does not match expected instant");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("additionalValidConversionsProvider")
    void toDate_additionalValidConversions(Object from, String converterKind, Date expected, String description) {
        Converter used = ("mock".equals(converterKind)) ? mockConverter : defaultConverter;
        if ("mock".equals(converterKind)) {
            // The mock should return the instant that corresponds to the expected Date.
            when(mockConverter.convert(eq(from), eq(Instant.class))).thenReturn(expected.toInstant());
        }
        Date actual = BigIntegerConversions.toDate(from, used);
        assertNotNull(actual, "Resulting Date must not be null");
        assertEquals(expected.getTime(), actual.getTime(), "Converted Date does not match expected instant");
    }

    @Test
    void toDate_nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toDate(null, defaultConverter), "When the source object is null, a NullPointerException should be thrown");
    }

    @Test
    void toDate_unsupportedInteger_throwsIllegalArgumentException() {
        Integer unsupported = Integer.valueOf(12345);
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toDate(unsupported, mockConverter), "Unsupported type Integer should cause IllegalArgumentException");
    }

    @Test
    void toDate_unparseableString_throwsDateTimeParseException() {
        String badString = "not-a-timestamp";
        when(mockConverter.convert(eq(badString), eq(Instant.class))).thenThrow(DateTimeParseException.class);
        assertThrows(DateTimeParseException.class, () -> BigIntegerConversions.toDate(badString, mockConverter), "Unparseable string should propagate DateTimeParseException");
    }

    @Test
    void toDate_localDateTimeWithoutConverter_throwsIllegalArgumentException() {
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 10, 14, 45, 30);
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toDate(ldt, null), "LocalDateTime without a Converter must throw IllegalArgumentException");
    }

    @Test
    void toDate_stringWithoutConverter_throwsIllegalArgumentException() {
        String iso = "2023-08-15T12:34:56Z";
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toDate(iso, null), "String ISO‑8601 instant without a Converter must throw IllegalArgumentException");
    }

    @Test
    void toDate_localDateWithoutConverter_throwsIllegalArgumentException() {
        LocalDate ld = LocalDate.of(2024, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toDate(ld, null), "LocalDate without a Converter must throw IllegalArgumentException");
    }

    @Test
    void toDate_offsetTimeWithoutConverter_throwsIllegalArgumentException() {
        OffsetTime ot = OffsetTime.parse("10:15:30+02:00");
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toDate(ot, null), "OffsetTime without a Converter must throw IllegalArgumentException");
    }

    @Test
    void toDate_unsupportedDouble_throwsIllegalArgumentException() {
        Double d = Double.valueOf(3.14159);
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toDate(d, mockConverter), "Unsupported Double type should cause IllegalArgumentException");
    }

    @Test
    void toDate_unsupportedCollection_throwsIllegalArgumentException() {
        List<String> list = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toDate(list, mockConverter), "Unsupported collection type should cause IllegalArgumentException");
    }

    @Test
    void toDate_converterThrowsRuntimeException_propagates() {
        LocalDateTime ldt = LocalDateTime.of(2025, 5, 20, 14, 0, 0);
        when(mockConverter.convert(eq(ldt), eq(Instant.class))).thenThrow(new RuntimeException("Conversion failed"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> BigIntegerConversions.toDate(ldt, mockConverter), "RuntimeException from Converter must propagate unchanged");
        assertEquals("Conversion failed", ex.getMessage());
    }

    @Test
    void toDate_converterThrowsDateTimeParseException_propagates() {
        String bad = "invalid‑format";
        when(mockConverter.convert(eq(bad), eq(Instant.class))).thenThrow(DateTimeParseException.class);
        assertThrows(DateTimeParseException.class, () -> BigIntegerConversions.toDate(bad, mockConverter), "DateTimeParseException from Converter must propagate");
    }

    @Test
    void toDate_instantBeyondDateRange_throwsDateTimeException() {
        // Instant far in the future (beyond Long.MAX_VALUE milliseconds)
        Instant tooFar = Instant.ofEpochSecond(Long.MAX_VALUE / 1000 + 1, 0);
        assertThrows(DateTimeException.class, () -> BigIntegerConversions.toDate(tooFar, defaultConverter), "Instant beyond Date range should cause DateTimeException");
    }

    @Test
    void toDate_bigIntegerBeyondLongMax_throwsDateTimeException() {
        BigInteger tooBig = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        assertThrows(DateTimeException.class, () -> BigIntegerConversions.toDate(tooBig, defaultConverter), "BigInteger > Long.MAX_VALUE should cause DateTimeException");
    }

    @Test
    void toDate_bigIntegerBelowLongMin_throwsDateTimeException() {
        BigInteger tooSmall = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE);
        assertThrows(DateTimeException.class, () -> BigIntegerConversions.toDate(tooSmall, defaultConverter), "BigInteger < Long.MIN_VALUE should cause DateTimeException");
    }

    @Test
    void toDate_boundaryInstantLongMin_works() {
        Instant min = Instant.ofEpochMilli(Long.MIN_VALUE);
        Date expected = Date.from(min);
        Date actual = BigIntegerConversions.toDate(min, defaultConverter);
        assertEquals(expected.getTime(), actual.getTime(), "Instant at Long.MIN_VALUE should be convertible to Date");
    }

    @Test
    void toDate_boundaryInstantLongMax_works() {
        Instant max = Instant.ofEpochMilli(Long.MAX_VALUE);
        Date expected = Date.from(max);
        Date actual = BigIntegerConversions.toDate(max, defaultConverter);
        assertEquals(expected.getTime(), actual.getTime(), "Instant at Long.MAX_VALUE should be convertible to Date");
    }
}
