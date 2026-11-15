package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_setNumberFormat_Test {

    /**
     * Mocked static context for {@link SafeSimpleDateFormat#getDateFormat(String)}
     */
    private MockedStatic<SafeSimpleDateFormat> safeSdfStaticMock;

    /**
     * Mock SimpleDateFormat that will be returned by the static method
     */
    private SimpleDateFormat mockSimpleDateFormat;

    /**
     * Mock NumberFormat supplied to the method under test
     */
    private NumberFormat mockNumberFormat;

    /**
     * A {@link NumberFormat} that throws a {@link RuntimeException}
     * when {@link #setMaximumFractionDigits(int)} is called.
     * The {@code setNumberFormat} method of {@link SimpleDateFormat}
     * stores the reference without invoking any method, therefore
     * the exception will only surface if the format attempts to use
     * the NumberFormat later.  This test verifies that the exception
     * propagates when we manually invoke a method on the stored
     * NumberFormat after delegation.
     */
    static class ExplodingNumberFormat extends NumberFormat {

        @Override
        public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
            return null;
        }

        @Override
        public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
            return null;
        }

        @Override
        public Number parse(String source, ParsePosition pos) {
            return null;
        }

        @Override
        public void setMaximumFractionDigits(int newValue) {
            throw new RuntimeException("boom");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create mocks
        mockSimpleDateFormat = mock(SimpleDateFormat.class);
        mockNumberFormat = mock(NumberFormat.class);
        // Mock the static method getDateFormat(String) to always return our mock SimpleDateFormat
        safeSdfStaticMock = Mockito.mockStatic(SafeSimpleDateFormat.class);
        safeSdfStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenReturn(mockSimpleDateFormat);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock so it does not affect other tests
        safeSdfStaticMock.close();
    }

    @Test
    void setNumberFormat_delegatesToUnderlyingSimpleDateFormat() {
        // GIVEN – a SafeSimpleDateFormat instance (constructor also uses the static mock)
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat("yyyy/MM/dd");
        // WHEN – we set a custom NumberFormat
        safeSdf.setNumberFormat(mockNumberFormat);
        // THEN – the underlying SimpleDateFormat should receive the same NumberFormat instance
        verify(mockSimpleDateFormat, times(1)).setNumberFormat(mockNumberFormat);
    }

    @Test
    @DisplayName("Cache‑miss: custom DecimalFormat with zero‑padding is applied")
    void setNumberFormat_cacheMiss_zeroPadding() {
        // Use the real static method – we want an actual SimpleDateFormat instance.
        safeSdfStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        String pattern = "yyyy-MM-dd HH:mm:ss";
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(pattern);
        // DecimalFormat that forces two‑digit zero padding
        DecimalFormat df = new DecimalFormat("00");
        safeSdf.setNumberFormat(df);
        // Verify that the underlying SimpleDateFormat now uses our DecimalFormat
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        assertSame(df, underlying.getNumberFormat(), "Underlying SimpleDateFormat should hold the supplied DecimalFormat");
        // Format a known date and check the result
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2023, Calendar.JANUARY, 2, 3, 4, 5);
        cal.set(Calendar.MILLISECOND, 0);
        String formatted = safeSdf.format(cal.getTime());
        assertEquals("2023-01-02 03:04:05", formatted, "Formatted output must reflect zero‑padded numeric fields");
    }

    @Test
    @DisplayName("Cache‑hit: changing NumberFormat on the same pattern updates the delegate")
    void setNumberFormat_cacheHit_changeNumberFormat() {
        safeSdfStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        String pattern = "yyyy-MM-dd HH:mm:ss";
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(pattern);
        // First call – zero padding
        DecimalFormat df1 = new DecimalFormat("00");
        safeSdf.setNumberFormat(df1);
        SimpleDateFormat sdf1 = SafeSimpleDateFormat.getDateFormat(pattern);
        // Second call – grouping format (should not affect date fields)
        DecimalFormat df2 = new DecimalFormat("#,#");
        safeSdf.setNumberFormat(df2);
        SimpleDateFormat sdf2 = SafeSimpleDateFormat.getDateFormat(pattern);
        // The cached SimpleDateFormat instance must be the same object
        assertSame(sdf1, sdf2, "Cache must return the same SimpleDateFormat instance");
        // Its NumberFormat reference must now be df2
        assertSame(df2, sdf2.getNumberFormat(), "NumberFormat should be replaced by the second call");
        // Formatting a date should still produce the same textual representation
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2023, Calendar.DECEMBER, 31, 23, 59, 59);
        cal.set(Calendar.MILLISECOND, 0);
        String formatted = safeSdf.format(cal.getTime());
        assertEquals("2023-12-31 23:59:59", formatted, "Formatting must remain correct after NumberFormat change");
    }

    @Test
    @DisplayName("setNumberFormat throws NPE when null NumberFormat is supplied")
    void setNumberFormat_nullNumberFormat_throwsNPE() {
        safeSdfStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        assertThrows(NullPointerException.class, () -> safeSdf.setNumberFormat(null), "Passing null should propagate NullPointerException");
    }

    @Test
    @DisplayName("setNumberFormat on instance created with null pattern throws IAE")
    void setNumberFormat_nullPattern_throwsIllegalArgumentException() {
        safeSdfStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(null);
        DecimalFormat df = new DecimalFormat("00");
        assertThrows(IllegalArgumentException.class, () -> safeSdf.setNumberFormat(df), "Null pattern must cause IllegalArgumentException during getDateFormat");
    }

    @Test
    @DisplayName("setNumberFormat on empty pattern throws IllegalArgumentException")
    void setNumberFormat_emptyPattern_throwsIllegalArgumentException() {
        safeSdfStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat("");
        DecimalFormat df = new DecimalFormat("00");
        assertThrows(IllegalArgumentException.class, () -> safeSdf.setNumberFormat(df), "Empty pattern must be rejected");
    }

    @Test
    @DisplayName("Locale‑specific DecimalFormat is accepted and does not affect date fields")
    void setNumberFormat_localeSpecificDecimalFormat() {
        safeSdfStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        String pattern = "dd.MM.yyyy HH:mm:ss";
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(pattern);
        DecimalFormatSymbols germanSymbols = new DecimalFormatSymbols(Locale.GERMANY);
        DecimalFormat germanDf = new DecimalFormat("#,##0.00", germanSymbols);
        safeSdf.setNumberFormat(germanDf);
        // Verify the NumberFormat reference
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        assertSame(germanDf, underlying.getNumberFormat(), "German DecimalFormat must be attached");
        // Format a known date
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2023, Calendar.APRIL, 5, 6, 7, 8);
        cal.set(Calendar.MILLISECOND, 0);
        String formatted = safeSdf.format(cal.getTime());
        assertEquals("05.04.2023 06:07:08", formatted, "Formatting must respect the pattern; NumberFormat does not alter date fields");
    }

    @Test
    @DisplayName("Patterns containing quoted literals work with custom NumberFormat")
    void setNumberFormat_patternWithLiterals() {
        safeSdfStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        String pattern = "'Year:' yyyy 'Month:' MM";
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(pattern);
        DecimalFormat df = new DecimalFormat("00");
        safeSdf.setNumberFormat(df);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2021, Calendar.DECEMBER, 31, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        String formatted = safeSdf.format(cal.getTime());
        assertEquals("Year: 2021 Month: 12", formatted, "Literal text must be preserved while numeric fields are formatted");
    }

    @Test
    @DisplayName("Concurrent setNumberFormat calls do not cause race conditions")
    void setNumberFormat_concurrentUpdates() throws Exception {
        safeSdfStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        String pattern = "yyyy-MM-dd HH:mm:ss";
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(pattern);
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        // Prepare distinct DecimalFormats for each thread
        DecimalFormat df1 = new DecimalFormat("00");
        DecimalFormat df2 = new DecimalFormat("#,#");
        DecimalFormat df3 = new DecimalFormat("0.##");
        List<DecimalFormat> formats = Arrays.asList(df1, df2, df3);
        ExecutorService exec = Executors.newFixedThreadPool(formats.size());
        // Each thread sets a different NumberFormat
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < formats.size(); i++) {
            final DecimalFormat fmt = formats.get(i);
            futures.add(exec.submit(() -> safeSdf.setNumberFormat(fmt)));
        }
        for (Future<?> f : futures) {
            // rethrow any exception
            f.get();
        }
        exec.shutdownNow();
        // The final NumberFormat should be the one set by the last thread (df3)
        assertSame(df3, underlying.getNumberFormat(), "After concurrent updates the NumberFormat must be the last one applied");
    }

    @Test
    @DisplayName("Timezone offset pattern retains correct offset after setting NumberFormat")
    void setNumberFormat_patternWithTimezoneOffset() {
        safeSdfStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        String pattern = "yyyy-MM-dd Z";
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(pattern);
        DecimalFormat df = new DecimalFormat("00");
        safeSdf.setNumberFormat(df);
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.set(2022, Calendar.JANUARY, 1, 0, 0, 0);
        utc.set(Calendar.MILLISECOND, 0);
        String formatted = safeSdf.format(utc.getTime());
        // Expected offset for UTC is +0000
        assertEquals("2022-01-01 +0000", formatted, "Timezone offset must be correctly rendered regardless of NumberFormat");
    }

    @Test
    @DisplayName("Setting the same NumberFormat instance twice does not change reference")
    void setNumberFormat_sameInstanceTwice() {
        safeSdfStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(pattern);
        DecimalFormat df = new DecimalFormat("00");
        safeSdf.setNumberFormat(df);
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        NumberFormat firstRef = underlying.getNumberFormat();
        safeSdf.setNumberFormat(df);
        NumberFormat secondRef = underlying.getNumberFormat();
        assertSame(firstRef, secondRef, "Re‑setting with the identical instance must keep the same reference");
        // Formatting should be deterministic
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2021, Calendar.JUNE, 15, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        String formatted = safeSdf.format(cal.getTime());
        assertEquals("2021-06-15", formatted);
    }

    @Test
    @DisplayName("Unclosed quote in pattern results in IllegalArgumentException")
    void setNumberFormat_malformedPattern_unclosedQuote() {
        safeSdfStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        String malformed = "yyyy-MM-dd 'Year:";
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(malformed);
        DecimalFormat df = new DecimalFormat("00");
        assertThrows(IllegalArgumentException.class, () -> safeSdf.setNumberFormat(df), "Malformed pattern should cause IllegalArgumentException");
    }

    @Test
    @DisplayName("Pattern containing illegal characters throws IllegalArgumentException")
    void setNumberFormat_illegalCharactersInPattern() {
        safeSdfStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        String illegal = "yyyy-@@-dd";
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(illegal);
        DecimalFormat df = new DecimalFormat("00");
        assertThrows(IllegalArgumentException.class, () -> safeSdf.setNumberFormat(df), "Illegal characters in pattern must be rejected");
    }

    @Test
    @DisplayName("RuntimeException thrown by NumberFormat propagates when used")
    void setNumberFormat_numberFormatThrowsRuntimeExceptionOnUse() {
        safeSdfStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenCallRealMethod();
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        ExplodingNumberFormat exploding = new ExplodingNumberFormat();
        // setNumberFormat itself should not throw (it only stores the reference)
        safeSdf.setNumberFormat(exploding);
        // Retrieve the underlying SimpleDateFormat and trigger a method that uses the NumberFormat
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        assertSame(exploding, underlying.getNumberFormat(), "Underlying SimpleDateFormat must hold the exploding NumberFormat");
        // The following call forces SimpleDateFormat to use the NumberFormat,
        // which will cause the RuntimeException.
        assertThrows(RuntimeException.class, () -> underlying.setMaximumFractionDigits(2), "RuntimeException from NumberFormat must propagate");
    }
}
