package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class StringConversions_toYear_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * A normal, functional converter – used for the majority of test cases.
     */
    private static final Converter DUMMY_CONVERTER = new Converter(new ConverterOptions());

    /**
     * A converter that throws a {@link RuntimeException} for any conversion request.
     */
    private static final Converter THROWING_CONVERTER = Mockito.mock(Converter.class, invocation -> {
        throw new RuntimeException("Converter failure");
    });

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Converter with default options – sufficient for the built‑in
        // String → ZonedDateTime conversion that the method under test may invoke.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    /**
     * Valid 4‑digit year strings that should be parsed directly.
     */
    private static Stream<Arguments> validYearStrings() {
        return Stream.of(Arguments.of("2023", Year.of(2023), "plain 4‑digit string"), Arguments.of("  1999  ", Year.of(1999), "leading/trailing whitespace"));
    }

    /**
     * Null or empty/whitespace‑only inputs – expected result is {@code null}.
     */
    private static Stream<Arguments> nullOrEmptyInputs() {
        return Stream.of(Arguments.of(null, null, "null source"), Arguments.of("", null, "empty string"), Arguments.of("   \t\n  ", null, "whitespace‑only string"));
    }

    /**
     * Strings that cannot be parsed as a year nor as a date – should raise
     *  {@link IllegalArgumentException}.
     */
    private static Stream<Arguments> illegalStringInputs() {
        return Stream.of(Arguments.of("abc", IllegalArgumentException.class, "non‑numeric"), Arguments.of("12a3", IllegalArgumentException.class, "mixed alphanumeric"));
    }

    /**
     * Numeric strings with sign or leading zeros – still parsed to a {@link Year}.
     */
    private static Stream<Arguments> signedOrZeroPadded() {
        return Stream.of(Arguments.of("+2020", Year.of(2020), "leading plus sign"), Arguments.of("0005", Year.of(5), "leading zeros"));
    }

    /**
     * Out‑of‑range year values – {@link Year#of(int)} throws {@link DateTimeException},
     *  which is wrapped as {@link IllegalArgumentException}.
     */
    private static Stream<Arguments> outOfRangeYears() {
        return Stream.of(Arguments.of("1000000000", IllegalArgumentException.class, "far beyond Year.MAX_VALUE"));
    }

    /**
     * Objects that can be converted to {@link ZonedDateTime} – the year is extracted.
     */
    private static Stream<Arguments> zonedDateTimeFallback() {
        // java.util.Date representing 2022‑03‑15 00:00:00 UTC
        Calendar cal = GregorianCalendar.from(ZonedDateTime.of(LocalDateTime.of(2022, Month.MARCH, 15, 0, 0, 0), ZoneOffset.UTC));
        Date date = cal.getTime();
        return Stream.of(Arguments.of(date, Year.of(2022), "java.util.Date"), Arguments.of(LocalDate.of(1995, 5, 23), Year.of(1995), "LocalDate"), Arguments.of(Instant.parse("2008-11-01T12:34:56Z"), Year.of(2008), "Instant"));
    }

    /**
     * Types that {@link StringConversions#toZonedDateTime(Object, Converter)} cannot handle –
     *  the method returns {@code null}.
     */
    private static Stream<Arguments> unsupportedObjectReturnsNull() {
        return Stream.of(Arguments.of(new Object(), null, "plain Object"));
    }

    /**
     * Non‑String inputs cause the initial cast to fail, resulting in {@link ClassCastException}.
     */
    private static Stream<Arguments> nonStringInputs() {
        return Stream.of(Arguments.of(123, ClassCastException.class, "Integer source"), Arguments.of(new Date(), ClassCastException.class, "Date source (non‑String)"));
    }

    /**
     * Integer overflow string – parsing fails and fallback also fails, leading to
     *  {@link IllegalArgumentException}.
     */
    private static Stream<Arguments> integerOverflow() {
        return Stream.of(Arguments.of("2147483648", IllegalArgumentException.class, "value > Integer.MAX_VALUE"));
    }

    /**
     * Whitespace string with a {@code null} converter – the fallback call throws
     *  {@link NullPointerException} which is wrapped as {@link IllegalArgumentException}.
     */
    private static Stream<Arguments> nullConverter() {
        return Stream.of(Arguments.of("   ", IllegalArgumentException.class, "null converter"));
    }

    /**
     * Converter that throws an exception during the fallback conversion – should be
     *  wrapped as {@link IllegalArgumentException}.
     */
    private static Stream<Arguments> throwingConverter() {
        return Stream.of(Arguments.of("not-a-year", IllegalArgumentException.class, "converter throws"));
    }

    /**
     * Verify that a plain numeric string is parsed directly to a {@link Year}.
     */
    @Test
    void testDirectYearParsing() {
        Year result = StringConversions.toYear(" 2023 ", DUMMY_CONVERTER);
        assertNotNull(result);
        assertEquals(2023, result.getValue());
    }

    /**
     * When the string cannot be parsed as an integer, the method should fall back
     * to converting the string to a {@link ZonedDateTime} and then extract the year.
     */
    @Test
    void testFallbackToZonedDateTime() {
        // ISO‑8601 format is understood by the default Converter.
        String iso = "2021-07-15T13:45:30Z";
        Year result = StringConversions.toYear(iso, converter);
        assertNotNull(result);
        assertEquals(2021, result.getValue());
    }

    /**
     * An empty (or whitespace‑only) string is trimmed to {@code null}. Parsing fails,
     * the fallback conversion returns {@code null}, and the method should therefore
     * return {@code null}.
     */
    @Test
    void testReturnsNullForEmptyString() {
        Year result = StringConversions.toYear("   ", converter);
        assertNull(result);
    }

    /**
     * If both the integer parsing and the fallback {@link ZonedDateTime} conversion
     * fail, the method must throw an {@link IllegalArgumentException} with a
     * meaningful message.
     */
    @Test
    void testIllegalArgumentExceptionOnUnparseableString() {
        String bad = "not-a-year-or-date";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYear(bad, converter));
        // The message should contain the original (trimmed) string.
        assertTrue(ex.getMessage().contains("Unable to parse 4-digit year from 'not-a-year-or-date'"));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validYearStrings")
    void testValidYearStringsParsedDirectly(String input, Year expected, String description) {
        Year result = StringConversions.toYear(input, DUMMY_CONVERTER);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nullOrEmptyInputs")
    void testNullOrEmptyInputsReturnNull(Object input, Year expected, String description) {
        Year result = StringConversions.toYear(input, DUMMY_CONVERTER);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("illegalStringInputs")
    void testIllegalArgumentExceptionForUnparseableStrings(String input, Class<? extends Exception> expectedException, String description) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYear(input, DUMMY_CONVERTER));
        assertTrue(ex.getMessage().contains("Unable to parse 4-digit year from"));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("signedOrZeroPadded")
    void testSignedOrZeroPaddedStrings(String input, Year expected, String description) {
        Year result = StringConversions.toYear(input, DUMMY_CONVERTER);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("outOfRangeYears")
    void testOutOfRangeYearValues(String input, Class<? extends Exception> expectedException, String description) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYear(input, DUMMY_CONVERTER));
        assertTrue(ex.getMessage().contains("Unable to parse 4-digit year from"));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("unsupportedObjectReturnsNull")
    void testUnsupportedObjectReturnsNull(Object input, Year expected, String description) {
        Year result = StringConversions.toYear(input, DUMMY_CONVERTER);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nonStringInputs")
    void testNonStringInputCausesClassCastException(Object input, Class<? extends Exception> expectedException, String description) {
        assertThrows(ClassCastException.class, () -> StringConversions.toYear(input, DUMMY_CONVERTER));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("integerOverflow")
    void testIntegerOverflowString(String input, Class<? extends Exception> expectedException, String description) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYear(input, DUMMY_CONVERTER));
        assertTrue(ex.getMessage().contains("Unable to parse 4-digit year from"));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nullConverter")
    void testNullConverterLeadsToIllegalArgumentException(String input, Class<? extends Exception> expectedException, String description) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYear(input, null));
        assertTrue(ex.getMessage().contains("Unable to parse 4-digit year from"));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("throwingConverter")
    void testThrowingConverterWrappedAsIllegalArgumentException(String input, Class<? extends Exception> expectedException, String description) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYear(input, THROWING_CONVERTER));
        assertTrue(ex.getMessage().contains("Unable to parse 4-digit year from"));
    }

    @Test
    void testFallbackToZonedDateTimeExplicit() {
        String iso = "2021-07-15T13:45:30Z";
        Year result = StringConversions.toYear(iso, DUMMY_CONVERTER);
        assertNotNull(result);
        assertEquals(2021, result.getValue());
    }

    @Test
    void testReturnsNullForEmptyStringExplicit() {
        Year result = StringConversions.toYear("   ", DUMMY_CONVERTER);
        assertNull(result);
    }

    @Test
    void testIllegalArgumentExceptionOnUnparseableStringExplicit() {
        String bad = "not-a-year-or-date";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYear(bad, DUMMY_CONVERTER));
        assertTrue(ex.getMessage().contains("Unable to parse 4-digit year from 'not-a-year-or-date'"));
    }
}
