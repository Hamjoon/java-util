package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.text.DateFormatSymbols;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

class SafeSimpleDateFormat_setDateFormatSymbols_Test {

    private SafeSimpleDateFormat safeSdf;

    private DateFormatSymbols customSymbols;

    /**
     * Helper holder for a pair of arguments used by the parameterized test.
     */
    private static class ArgumentsProvider {

        final DateFormatSymbols symbols;

        final boolean expectException;

        ArgumentsProvider(DateFormatSymbols symbols, boolean expectException) {
            this.symbols = symbols;
            this.expectException = expectException;
        }
    }

    /**
     * Create a new SafeSimpleDateFormat instance for each test and prepare a
     * DateFormatSymbols object whose short month names are easily recognisable.
     * Using a unique format string ("MMM") ensures that the internal ThreadLocal
     * map creates a fresh SimpleDateFormat for every test execution.
     */
    @BeforeEach
    void setupBeforeEach() {
        safeSdf = new SafeSimpleDateFormat("MMM");
        customSymbols = new DateFormatSymbols();
        String[] shortMonths = new String[12];
        for (int i = 0; i < shortMonths.length; i++) {
            // e.g. M0, M1, … M11
            shortMonths[i] = "M" + i;
        }
        customSymbols.setShortMonths(shortMonths);
    }

    @AfterEach
    void teardownAfterEach() {
        safeSdf = null;
        customSymbols = null;
    }

    /**
     * Supplies normal (non‑null) and null {@link DateFormatSymbols} instances.
     */
    private static Stream<ArgumentsProvider> symbolsProvider() {
        return Stream.of(new ArgumentsProvider(new DateFormatSymbols(Locale.US), false), new ArgumentsProvider(null, true));
    }

    /**
     * Verify that {@code setDateFormatSymbols} forwards the supplied symbols to the
     * underlying SimpleDateFormat instance. After the call, formatting a date
     * that contains a month abbreviation should reflect the custom short month
     * names we supplied.
     */
    @Test
    void setDateFormatSymbols_usesProvidedSymbols() {
        // GIVEN
        safeSdf.setDateFormatSymbols(customSymbols);
        // A date in January (month index 0)
        Date januaryFirst = new GregorianCalendar(2021, Calendar.JANUARY, 1).getTime();
        // WHEN
        String formatted = safeSdf.format(januaryFirst);
        // THEN
        assertEquals("M0", formatted, "The formatted month abbreviation should come from the custom symbols");
    }

    /**
     * Ensure that without calling {@code setDateFormatSymbols} the default
     * locale‑specific month abbreviations are used. This test acts as a sanity
     * check that the custom symbols are indeed overriding the defaults.
     */
    @Test
    void format_withoutCustomSymbols_usesDefaultLocaleSymbols() {
        // GIVEN – a fresh SafeSimpleDateFormat (no custom symbols set)
        // A date in February (month index 1)
        Date februaryFirst = new GregorianCalendar(2021, Calendar.FEBRUARY, 1).getTime();
        // WHEN
        String formatted = safeSdf.format(februaryFirst);
        // THEN – the result must not equal the custom symbol "M1"
        assertNotEquals("M1", formatted, "Without custom symbols the output should be the default locale month abbreviation");
    }

    /**
     * Basic functionality group – verifies that a normal symbols object is accepted
     * and that a {@code null} symbols argument results in a {@link NullPointerException}.
     */
    @ParameterizedTest(name = "[{index}] symbols null? = {0}")
    @MethodSource("symbolsProvider")
    void setDateFormatSymbols_basicFunctionality(ArgumentsProvider provider) {
        if (provider.expectException) {
            // Expect NPE when symbols is null
            assertThrows(NullPointerException.class, () -> safeSdf.setDateFormatSymbols(provider.symbols), "Passing null symbols should propagate NullPointerException");
        } else {
            // Normal path – after setting, the underlying SimpleDateFormat must contain the same symbols
            safeSdf.setDateFormatSymbols(provider.symbols);
            // Retrieve the underlying SimpleDateFormat via reflection (private static method)
            SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat("MMM");
            DateFormatSymbols actual = underlying.getDateFormatSymbols();
            assertEquals(provider.symbols, actual, "The underlying SimpleDateFormat must have the exact symbols that were supplied");
        }
    }

    /**
     * Group: null format string.
     * Creating a {@code SafeSimpleDateFormat} with a {@code null} format should cause
     * {@code getDateFormat(null)} to throw an exception (NPE or IllegalArgumentException).
     * The test verifies that the exception propagates from {@code setDateFormatSymbols}.
     */
    @Test
    void setDateFormatSymbols_nullFormatString_throwsException() {
        // GIVEN a SafeSimpleDateFormat constructed with a null format string
        SafeSimpleDateFormat sdfWithNullFormat = new SafeSimpleDateFormat(null);
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.US);
        // WHEN / THEN – the call must throw either NPE or IllegalArgumentException
        Exception ex = assertThrows(Exception.class, () -> sdfWithNullFormat.setDateFormatSymbols(symbols), "Calling setDateFormatSymbols on a formatter with a null pattern should throw");
        assertTrue(ex instanceof NullPointerException || ex instanceof IllegalArgumentException, "Expected NullPointerException or IllegalArgumentException, but got: " + ex);
    }

    /**
     * Group: invalid format pattern.
     * An illegal pattern should cause {@link SimpleDateFormat} construction to fail,
     * which in turn should be propagated by {@code setDateFormatSymbols}.
     */
    @Test
    void setDateFormatSymbols_invalidPattern_throwsIllegalArgumentException() {
        // GIVEN an illegal pattern (contains unsupported character '?')
        SafeSimpleDateFormat sdfInvalid = new SafeSimpleDateFormat("yyyy-MM-dd-?");
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.US);
        // WHEN / THEN – expect IllegalArgumentException from SimpleDateFormat creation
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> sdfInvalid.setDateFormatSymbols(symbols), "An invalid pattern should cause IllegalArgumentException to be thrown");
        assertNotNull(thrown.getMessage());
    }
}
