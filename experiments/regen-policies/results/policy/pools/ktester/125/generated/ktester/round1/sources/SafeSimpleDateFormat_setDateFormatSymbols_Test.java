package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.text.*;
import java.time.ZoneId;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_setDateFormatSymbols_Test {

    /**
     * Mocked static context for {@link SafeSimpleDateFormat#getDateFormat(String)}
     */
    private MockedStatic<SafeSimpleDateFormat> safeStaticMock;

    /**
     * Mock SimpleDateFormat that will be returned by the static method
     */
    private SimpleDateFormat mockSimpleDateFormat;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the static mock and the SimpleDateFormat mock before each test
        safeStaticMock = Mockito.mockStatic(SafeSimpleDateFormat.class);
        mockSimpleDateFormat = mock(SimpleDateFormat.class);
        // By default, any call to getDateFormat(String) returns the same mock instance.
        safeStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenReturn(mockSimpleDateFormat);
    }

    @AfterEach
    void teardownAfterEach() {
        // Close the static mock to avoid leaking it into other tests
        safeStaticMock.close();
    }

    @Test
    void setDateFormatSymbols_DelegatesToUnderlyingSimpleDateFormat() {
        // ----- GIVEN ---------------------------------------------------------
        String format = "yyyy-MM-dd";
        DateFormatSymbols symbols = new DateFormatSymbols();
        // Ensure the static method returns the mock for the specific format used
        safeStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(format)).thenReturn(mockSimpleDateFormat);
        // The constructor of SafeSimpleDateFormat also calls getDateFormat, which is
        // already stubbed to return the mockSimpleDateFormat.
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(format);
        // ----- WHEN ----------------------------------------------------------
        safe.setDateFormatSymbols(symbols);
        // ----- THEN ----------------------------------------------------------
        // Verify that the underlying SimpleDateFormat received the exact symbols instance.
        verify(mockSimpleDateFormat, times(1)).setDateFormatSymbols(symbols);
    }

    @Test
    void setDateFormatSymbols_WithNullSymbols_DelegatesCorrectly() {
        // ----- GIVEN ---------------------------------------------------------
        String format = "MM/dd/yyyy";
        safeStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(format)).thenReturn(mockSimpleDateFormat);
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(format);
        // ----- WHEN ----------------------------------------------------------
        safe.setDateFormatSymbols(null);
        // ----- THEN ----------------------------------------------------------
        verify(mockSimpleDateFormat, times(1)).setDateFormatSymbols(null);
    }

    @Test
    @DisplayName("setDateFormatSymbols - normal behavior with custom month names")
    void setDateFormatSymbols_NormalBehavior_CustomMonths() throws Exception {
        // Use the real static method – close the mock created in @BeforeEach
        safeStaticMock.close();
        String pattern = "MMM dd, yyyy";
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(pattern);
        // custom month names: {"Foo","Bar","Baz","Qux","Quux","Corge","Grault","Garply","Waldo","Fred","Plugh","Xyzzy"}
        String[] customMonths = { "Foo", "Bar", "Baz", "Qux", "Quux", "Corge", "Grault", "Garply", "Waldo", "Fred", "Plugh", "Xyzzy" };
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.US);
        symbols.setMonths(customMonths);
        safe.setDateFormatSymbols(symbols);
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.set(2023, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();
        String formatted = safe.format(date);
        assertTrue(formatted.startsWith("Foo"), "Month name should be replaced by custom name \"Foo\"");
    }

    @Test
    @DisplayName("setDateFormatSymbols - null argument should throw NPE")
    void setDateFormatSymbols_NullArgument_ThrowsNPE() {
        // The mock SimpleDateFormat will throw NPE when null is passed – we let it happen.
        String format = "yyyy-MM-dd";
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(format);
        assertThrows(NullPointerException.class, () -> safe.setDateFormatSymbols(null), "Passing null DateFormatSymbols should result in NullPointerException");
    }

    @Test
    @DisplayName("setDateFormatSymbols - different format strings are independent")
    void setDateFormatSymbols_DifferentFormats_Independent() throws Exception {
        // use real caching
        safeStaticMock.close();
        // Instance A uses pattern with month name
        SafeSimpleDateFormat a = new SafeSimpleDateFormat("MMM dd, yyyy");
        // Instance B uses a numeric pattern (no month name)
        SafeSimpleDateFormat b = new SafeSimpleDateFormat("yyyy-MM-dd");
        // Custom symbols for instance A
        String[] monthsA = { "Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa", "Lambda", "Mu" };
        DateFormatSymbols symbolsA = new DateFormatSymbols(Locale.US);
        symbolsA.setMonths(monthsA);
        a.setDateFormatSymbols(symbolsA);
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.set(2023, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();
        // Instance A should use custom month name
        String formattedA = a.format(date);
        assertTrue(formattedA.startsWith("Alpha"), "Instance A should use custom month name \"Alpha\"");
        // Instance B should remain unaffected (default month name)
        String formattedB = b.format(date);
        // default month name for January in US locale is "Jan"
        assertTrue(formattedB.contains("Jan") || formattedB.contains("01"), "Instance B should not be affected by symbols set on instance A");
    }

    @Test
    @DisplayName("setDateFormatSymbols - same format string shared cache")
    void setDateFormatSymbols_SameFormat_SharedCache() throws Exception {
        // real caching
        safeStaticMock.close();
        // Both instances share the same pattern
        SafeSimpleDateFormat first = new SafeSimpleDateFormat("MMM dd, yyyy");
        SafeSimpleDateFormat second = new SafeSimpleDateFormat("MMM dd, yyyy");
        // Apply custom symbols on the first instance
        String[] months = { "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve" };
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.US);
        symbols.setMonths(months);
        first.setDateFormatSymbols(symbols);
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.set(2023, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();
        // Both instances should now use the custom month name
        String formattedFirst = first.format(date);
        String formattedSecond = second.format(date);
        assertTrue(formattedFirst.startsWith("One"), "First instance should use custom month name \"One\"");
        assertTrue(formattedSecond.startsWith("One"), "Second instance should reflect the same custom month name due to shared cache");
    }

    @Test
    @DisplayName("setDateFormatSymbols - overriding previous symbols")
    void setDateFormatSymbols_OverridePreviousSymbols() throws Exception {
        // real SimpleDateFormat
        safeStaticMock.close();
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("MMM dd, yyyy");
        // First set of symbols
        String[] monthsFirst = { "JanU", "FebU", "MarU", "AprU", "MayU", "JunU", "JulU", "AugU", "SepU", "OctU", "NovU", "DecU" };
        DateFormatSymbols firstSymbols = new DateFormatSymbols(Locale.US);
        firstSymbols.setMonths(monthsFirst);
        safe.setDateFormatSymbols(firstSymbols);
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.set(2023, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();
        String formattedFirst = safe.format(date);
        assertTrue(formattedFirst.startsWith("JanU"), "First formatting should use \"JanU\"");
        // Second set of symbols
        String[] monthsSecond = { "J1", "F1", "M1", "A1", "M2", "J2", "J3", "A2", "S1", "O1", "N1", "D1" };
        DateFormatSymbols secondSymbols = new DateFormatSymbols(Locale.US);
        secondSymbols.setMonths(monthsSecond);
        safe.setDateFormatSymbols(secondSymbols);
        String formattedSecond = safe.format(date);
        assertTrue(formattedSecond.startsWith("J1"), "Second formatting should reflect overridden month name \"J1\"");
    }

    @Test
    @DisplayName("setDateFormatSymbols - parsing with custom month names")
    void setDateFormatSymbols_ParsingWithCustomMonths() throws Exception {
        // real SimpleDateFormat
        safeStaticMock.close();
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("MMM dd, yyyy");
        String[] months = { "M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", "M10", "M11", "M12" };
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.US);
        symbols.setMonths(months);
        safe.setDateFormatSymbols(symbols);
        String toParse = "M3 15, 2022";
        Date parsed = safe.parse(toParse);
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.setTime(parsed);
        assertEquals(2022, cal.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    @DisplayName("setDateFormatSymbols - does not affect timezone")
    void setDateFormatSymbols_DoesNotAffectTimezone() throws Exception {
        // real SimpleDateFormat
        safeStaticMock.close();
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TimeZone utc = TimeZone.getTimeZone("UTC");
        // set explicit timezone
        safe.setTimeZone(utc);
        // Verify timezone before changing symbols
        assertEquals(utc, safe.getTimeZone(), "Timezone should be UTC before symbol change");
        // Apply custom symbols (month names irrelevant for this pattern)
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.US);
        safe.setDateFormatSymbols(symbols);
        // Verify timezone remains UTC
        assertEquals(utc, safe.getTimeZone(), "Timezone must remain unchanged after setDateFormatSymbols");
        // Formatting a known instant should reflect UTC
        Calendar cal = Calendar.getInstance(utc);
        cal.set(2023, Calendar.JANUARY, 1, 12, 34, 56);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();
        String formatted = safe.format(date);
        assertTrue(formatted.startsWith("2023-01-01 12:34:56"), "Formatted string should reflect UTC time and not be altered by symbol change");
    }

    @Test
    @DisplayName("setDateFormatSymbols - null format string")
    void setDateFormatSymbols_NullFormatString_ThrowsNPE() {
        // Construct with null format – this will hit getDateFormat(null) inside the method.
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(null);
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.US);
        assertThrows(NullPointerException.class, () -> safe.setDateFormatSymbols(symbols), "Calling setDateFormatSymbols on an instance built with null format should throw NPE");
    }

    @Test
    @DisplayName("setDateFormatSymbols - invalid format pattern propagates IAE")
    void setDateFormatSymbols_InvalidPattern_ThrowsIllegalArgumentException() {
        // The pattern "@@@invalid@@@" cannot be parsed by SimpleDateFormat.
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("@@@invalid@@@");
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.US);
        assertThrows(IllegalArgumentException.class, () -> safe.setDateFormatSymbols(symbols), "Invalid pattern should cause IllegalArgumentException from SimpleDateFormat");
    }

    @Test
    @DisplayName("setDateFormatSymbols - symbols with null month names cause formatting NPE")
    void setDateFormatSymbols_NullMonthArray_FormattingThrowsNPE() throws Exception {
        // real SimpleDateFormat
        safeStaticMock.close();
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("MMM dd, yyyy");
        // Create a DateFormatSymbols instance and force its internal month array to null via reflection
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.US);
        Field monthsField = DateFormatSymbols.class.getDeclaredField("months");
        monthsField.setAccessible(true);
        // corrupt the symbols
        monthsField.set(symbols, null);
        safe.setDateFormatSymbols(symbols);
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.set(2023, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();
        assertThrows(NullPointerException.class, () -> safe.format(date), "Formatting after setting malformed symbols (null month array) should throw NPE");
    }
}
