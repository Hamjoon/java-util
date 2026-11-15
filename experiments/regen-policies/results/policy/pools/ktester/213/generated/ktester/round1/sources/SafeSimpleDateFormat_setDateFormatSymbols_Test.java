package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Locale;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_setDateFormatSymbols_Test {

    private SafeSimpleDateFormat safeSdf;

    private DateFormatSymbols customSymbols;

    private Date marchDate;

    @BeforeEach
    void setupBeforeEach() {
        // Create a SafeSimpleDateFormat that formats month abbreviations.
        safeSdf = new SafeSimpleDateFormat("MMM");
        // Build custom month symbols where March is represented by "Foo".
        String[] shortMonths = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        // index 2 = March
        shortMonths[Calendar.MARCH] = "Foo";
        customSymbols = new DateFormatSymbols();
        customSymbols.setShortMonths(shortMonths);
        // also set the long names for completeness
        customSymbols.setMonths(shortMonths);
        // A concrete Date that falls in March.
        Calendar cal = new GregorianCalendar(2020, Calendar.MARCH, 15);
        marchDate = cal.getTime();
    }

    /**
     * Provides arguments for {@link #setDateFormatSymbols_ValidSymbols}.
     *
     * @return a stream of arguments: symbols, pattern, date, expected formatted string
     */
    private static Stream<Arguments> validSymbolsProvider() {
        // ---- custom symbols where March = "Foo" ----
        DateFormatSymbols custom = new DateFormatSymbols();
        String[] months = { "Jan", "Feb", "Foo", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        custom.setMonths(months);
        custom.setShortMonths(months);
        Calendar calMar = new GregorianCalendar(2020, Calendar.MARCH, 15);
        Date march = calMar.getTime();
        // ---- default US symbols (no modification) ----
        DateFormatSymbols usDefault = DateFormatSymbols.getInstance(Locale.US);
        Calendar calJul = new GregorianCalendar(2020, Calendar.JULY, 4);
        Date july = calJul.getTime();
        return Stream.of(// pattern "MMM" uses short month names; expect "Foo" for March
        Arguments.of(custom, "MMM", march, "Foo"), // pattern "MMMM" uses full month names; expect "July" for July
        Arguments.of(usDefault, "MMMM", july, "July"));
    }

    @Test
    void setDateFormatSymbols_ShouldApplyCustomSymbols() {
        // Act: inject the custom symbols into the SafeSimpleDateFormat.
        safeSdf.setDateFormatSymbols(customSymbols);
        // Format the March date; the result should use the custom short month name.
        String formatted = safeSdf.format(marchDate);
        // Assert: the formatted month abbreviation matches the custom symbol.
        assertEquals("Foo", formatted);
    }

    @ParameterizedTest(name = "Pattern ''{1}'' with supplied symbols should format to ''{3}''")
    @MethodSource("validSymbolsProvider")
    void setDateFormatSymbols_ValidSymbols(DateFormatSymbols symbols, String pattern, Date date, String expected) {
        // Arrange: create a fresh instance with the required pattern
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);
        // Act: inject the symbols and format the date
        sdf.setDateFormatSymbols(symbols);
        String actual = sdf.format(date);
        // Assert: the formatted output matches the expectation
        assertEquals(expected, actual);
    }

    @Test
    void setDateFormatSymbols_NullSymbols_ThrowsNullPointerException() {
        // Arrange: a SafeSimpleDateFormat instance (pattern irrelevant for this test)
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("MMM");
        // Act & Assert: passing null must result in NPE (delegated to SimpleDateFormat)
        assertThrows(NullPointerException.class, () -> sdf.setDateFormatSymbols(null), "Expected NullPointerException when symbols argument is null");
    }

    @Test
    void setDateFormatSymbols_InvalidPattern_ThrowsIllegalArgumentException() {
        // Arrange: an illegal pattern that SimpleDateFormat cannot parse
        String illegalPattern = "yyyy-MM-dd-@@";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(illegalPattern);
        DateFormatSymbols symbols = DateFormatSymbols.getInstance(Locale.US);
        // Act & Assert: the call to setDateFormatSymbols triggers the lazy creation
        // of the underlying SimpleDateFormat which will throw IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> sdf.setDateFormatSymbols(symbols), "Expected IllegalArgumentException for an invalid format pattern");
    }

    @Test
    void setDateFormatSymbols_SymbolsWithNullMonths_ThrowsNullPointerException() {
        // Arrange: corrupt a DateFormatSymbols instance by nulling its month array
        DateFormatSymbols corrupted = new DateFormatSymbols();
        // intentionally break internal state
        corrupted.setMonths(null);
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("MMM");
        // Act & Assert: SimpleDateFormat#setDateFormatSymbols will NPE when copying null arrays
        assertThrows(NullPointerException.class, () -> sdf.setDateFormatSymbols(corrupted), "Expected NullPointerException when month arrays inside symbols are null");
    }
}
