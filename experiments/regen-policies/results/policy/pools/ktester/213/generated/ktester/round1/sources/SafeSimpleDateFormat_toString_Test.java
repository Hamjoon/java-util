package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_toString_Test {

    private SafeSimpleDateFormat safeSimpleDateFormat;

    private String defaultFormat;

    @BeforeEach
    void setupBeforeEach() {
        defaultFormat = "yyyy/MM/dd";
        safeSimpleDateFormat = new SafeSimpleDateFormat(defaultFormat);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        safeSimpleDateFormat = null;
        defaultFormat = null;
    }

    /**
     * Supplies a wide range of format strings (including null) together
     * with the expected result of {@code toString()}.
     */
    private static Stream<Arguments> formatProvider() {
        return Stream.of(// typical pattern
        Arguments.of("yyyy-MM-dd", "yyyy-MM-dd"), // empty string
        Arguments.of("", ""), // null format – expected to return null
        Arguments.of(null, null), // whitespace‑only pattern
        Arguments.of("   ", "   "), // literal text enclosed in single quotes
        Arguments.of("'Year:' yyyy", "'Year:' yyyy"), // very long pattern (over 30 chars)
        Arguments.of("yyyyMMddHHmmssyyyyMMddHHmmssyyyyMMddHHmmss", "yyyyMMddHHmmssyyyyMMddHHmmssyyyyMMddHHmmss"), // Unicode literal character Ω
        Arguments.of("yyyy-MM-dd'Ω'", "yyyy-MM-dd'Ω'"), // Escaped single quote (two consecutive quotes)
        Arguments.of("yyyy-MM-dd''", "yyyy-MM-dd''"), // Literal 'T' and timezone offset
        Arguments.of("yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ssZ"), // Non‑date symbols – should be returned unchanged
        Arguments.of("abcXYZ", "abcXYZ"), // Newline character inside the pattern
        Arguments.of("\nyyyy", "\nyyyy"), // Unicode copyright symbol encoded as an escape sequence
        Arguments.of("yyyy-\u00A9-MM", "yyyy-\u00A9-MM"));
    }

    @Test
    void toStringReturnsProvidedFormat() {
        // given – safeSimpleDateFormat is created in @BeforeEach with defaultFormat
        // when
        String result = safeSimpleDateFormat.toString();
        // then
        assertEquals(defaultFormat, result, "toString() should return the format string supplied to the constructor");
    }

    @Test
    void toStringHandlesEmptyFormatString() {
        // given
        String emptyFormat = "";
        SafeSimpleDateFormat emptySdf = new SafeSimpleDateFormat(emptyFormat);
        // when
        String result = emptySdf.toString();
        // then
        assertEquals(emptyFormat, result, "toString() should correctly handle an empty format string");
    }

    @Test
    void toStringHandlesComplexPattern() {
        // given
        String complexFormat = "EEE, dd MMM yyyy HH:mm:ss Z";
        SafeSimpleDateFormat complexSdf = new SafeSimpleDateFormat(complexFormat);
        // when
        String result = complexSdf.toString();
        // then
        assertEquals(complexFormat, result, "toString() should return the exact complex pattern supplied");
    }

    @Test
    void toStringIsConsistentAcrossMultipleInstances() {
        // given
        String formatA = "yyyy-MM-dd";
        String formatB = "dd/MM/yyyy HH:mm";
        SafeSimpleDateFormat sdfA = new SafeSimpleDateFormat(formatA);
        SafeSimpleDateFormat sdfB = new SafeSimpleDateFormat(formatB);
        // when / then
        assertAll(() -> assertEquals(formatA, sdfA.toString(), "First instance should return its own format"), () -> assertEquals(formatB, sdfB.toString(), "Second instance should return its own format"));
    }

    @ParameterizedTest(name = "[{index}] format=''{0}'' → expected=''{1}''")
    @MethodSource("formatProvider")
    void toStringReturnsExactProvidedPattern(String format, String expected) {
        // given – construct a new instance for each parameter set
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(format);
        // when
        String actual = sdf.toString();
        // then
        assertEquals(expected, actual, () -> "toString() should return the exact format string supplied to the constructor");
    }

    @Test
    void toStringRemainsUnchangedAfterMutatingDateFormatState() {
        // given
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);
        // mutate various DateFormat properties
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        sdf.setLenient(false);
        Calendar cal = new GregorianCalendar(2020, Calendar.JANUARY, 1);
        sdf.setCalendar(cal);
        NumberFormat nf = NumberFormat.getInstance();
        sdf.setNumberFormat(nf);
        DateFormatSymbols dfs = new DateFormatSymbols();
        dfs.setMonths(new String[] { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" });
        sdf.setDateFormatSymbols(dfs);
        // when
        String result = sdf.toString();
        // then – the stored format string must be exactly the original pattern
        assertEquals(pattern, result, "toString() must remain unchanged after calling mutating DateFormat methods");
    }
}
