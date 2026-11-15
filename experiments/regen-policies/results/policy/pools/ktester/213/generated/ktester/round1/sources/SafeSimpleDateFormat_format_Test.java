package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.text.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_format_Test {

    private static final String PATTERN = "yyyy-MM-dd";

    private SafeSimpleDateFormat safeSdf;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test – the constructor performs its own
        // internal initialisation, which we want to keep untouched.
        safeSdf = new SafeSimpleDateFormat(PATTERN);
    }

    private static Stream<Arguments> normalFormattingProvider() {
        return Stream.of(// epoch start, empty buffer, YEAR field
        Arguments.of(new Date(0L), new StringBuffer(), new FieldPosition(DateFormat.YEAR_FIELD), "1970-01-01"), // specific date, pre‑populated buffer, MONTH field
        Arguments.of(new Date(1622505600000L), new StringBuffer("Prefix-"), new FieldPosition(DateFormat.MONTH_FIELD), "Prefix-2021-06-01"), // epoch start, buffer with leading space, YEAR field (used for index offset test)
        Arguments.of(new Date(0L), new StringBuffer(" "), new FieldPosition(DateFormat.YEAR_FIELD), " 1970-01-01"));
    }

    /**
     * Verifies that {@code SafeSimpleDateFormat.format(Date, StringBuffer, FieldPosition)}
     * delegates to the {@link SimpleDateFormat} returned by {@code getDateFormat(String)}.
     */
    @Test
    void format_delegatesToUnderlyingSimpleDateFormat() {
        // given
        // epoch start
        Date date = new Date(0L);
        StringBuffer toAppendTo = new StringBuffer();
        FieldPosition fieldPos = new FieldPosition(0);
        SimpleDateFormat mockDelegate = mock(SimpleDateFormat.class);
        StringBuffer expectedResult = new StringBuffer("1970-01-01");
        when(mockDelegate.format(date, toAppendTo, fieldPos)).thenReturn(expectedResult);
        // Mock the static factory method getDateFormat(String)
        try (MockedStatic<SafeSimpleDateFormat> mockedStatic = mockStatic(SafeSimpleDateFormat.class)) {
            mockedStatic.when(() -> SafeSimpleDateFormat.getDateFormat(PATTERN)).thenReturn(mockDelegate);
            // when
            StringBuffer actualResult = safeSdf.format(date, toAppendTo, fieldPos);
            // then
            assertSame(expectedResult, actualResult, "The format method should return the StringBuffer from the delegate.");
            verify(mockDelegate, times(1)).format(date, toAppendTo, fieldPos);
        }
    }

    /**
     * Ensures that a {@code NullPointerException} is propagated when the underlying
     * {@link SimpleDateFormat} receives a {@code null} date argument.
     */
    @Test
    void format_nullDate_throwsNullPointerException() {
        // given
        StringBuffer sb = new StringBuffer();
        FieldPosition fp = new FieldPosition(0);
        SimpleDateFormat mockDelegate = mock(SimpleDateFormat.class);
        when(mockDelegate.format(null, sb, fp)).thenThrow(new NullPointerException());
        try (MockedStatic<SafeSimpleDateFormat> mockedStatic = mockStatic(SafeSimpleDateFormat.class)) {
            mockedStatic.when(() -> SafeSimpleDateFormat.getDateFormat(PATTERN)).thenReturn(mockDelegate);
            // when / then
            assertThrows(NullPointerException.class, () -> safeSdf.format(null, sb, fp), "Formatting a null Date should result in a NullPointerException.");
            verify(mockDelegate, times(1)).format(null, sb, fp);
        }
    }

    @ParameterizedTest
    @MethodSource("normalFormattingProvider")
    void format_variousInputs_delegatesAndAppendsCorrectly(Date date, StringBuffer toAppendTo, FieldPosition fieldPos, String expectedString) {
        // Use a real SimpleDateFormat as the delegate so that the formatting logic is exercised.
        SimpleDateFormat realDelegate = new SimpleDateFormat(PATTERN);
        try (MockedStatic<SafeSimpleDateFormat> mockedStatic = mockStatic(SafeSimpleDateFormat.class)) {
            mockedStatic.when(() -> SafeSimpleDateFormat.getDateFormat(PATTERN)).thenReturn(realDelegate);
            // when
            StringBuffer result = safeSdf.format(date, toAppendTo, fieldPos);
            // then
            assertSame(toAppendTo, result, "The same StringBuffer instance should be returned.");
            assertEquals(expectedString, result.toString(), "The formatted string should be correctly appended to the buffer.");
        }
    }

    @Test
    void format_nullStringBuffer_throwsNullPointerException() {
        Date date = new Date();
        FieldPosition fp = new FieldPosition(DateFormat.HOUR_OF_DAY0_FIELD);
        SimpleDateFormat realDelegate = new SimpleDateFormat(PATTERN);
        try (MockedStatic<SafeSimpleDateFormat> mockedStatic = mockStatic(SafeSimpleDateFormat.class)) {
            mockedStatic.when(() -> SafeSimpleDateFormat.getDateFormat(PATTERN)).thenReturn(realDelegate);
            assertThrows(NullPointerException.class, () -> safeSdf.format(date, null, fp), "A null StringBuffer should cause a NullPointerException.");
        }
    }

    @Test
    void format_nullFieldPosition_formatsWithoutException() {
        Date date = new Date(0L);
        StringBuffer sb = new StringBuffer();
        SimpleDateFormat realDelegate = new SimpleDateFormat(PATTERN);
        try (MockedStatic<SafeSimpleDateFormat> mockedStatic = mockStatic(SafeSimpleDateFormat.class)) {
            mockedStatic.when(() -> SafeSimpleDateFormat.getDateFormat(PATTERN)).thenReturn(realDelegate);
            // when – fieldPosition is null
            StringBuffer result = safeSdf.format(date, sb, null);
            // then
            assertEquals("1970-01-01", result.toString(), "Formatting should succeed even when FieldPosition is null.");
        }
    }

    @Test
    void format_invalidFieldPositionIndex_doesNotThrow() {
        Date date = new Date(0L);
        StringBuffer sb = new StringBuffer();
        // out‑of‑range index
        FieldPosition fp = new FieldPosition(Integer.MAX_VALUE);
        SimpleDateFormat realDelegate = new SimpleDateFormat(PATTERN);
        try (MockedStatic<SafeSimpleDateFormat> mockedStatic = mockStatic(SafeSimpleDateFormat.class)) {
            mockedStatic.when(() -> SafeSimpleDateFormat.getDateFormat(PATTERN)).thenReturn(realDelegate);
            // when – should not throw any exception
            StringBuffer result = safeSdf.format(date, sb, fp);
            // then
            assertEquals("1970-01-01", result.toString(), "Formatting should succeed even with an out‑of‑range FieldPosition index.");
            // FieldPosition values are undefined for an invalid field; we only assert no exception.
        }
    }

    @Test
    void format_populatesFieldPositionWithYearIndexes() {
        // 1970‑01‑01
        Date date = new Date(0L);
        StringBuffer sb = new StringBuffer();
        FieldPosition fp = new FieldPosition(DateFormat.YEAR_FIELD);
        SimpleDateFormat realDelegate = new SimpleDateFormat(PATTERN);
        try (MockedStatic<SafeSimpleDateFormat> mockedStatic = mockStatic(SafeSimpleDateFormat.class)) {
            mockedStatic.when(() -> SafeSimpleDateFormat.getDateFormat(PATTERN)).thenReturn(realDelegate);
            safeSdf.format(date, sb, fp);
            // The year "1970" starts at index 0 and ends at index 4 (exclusive)
            assertEquals(0, fp.getBeginIndex(), "Begin index of YEAR field should be 0 for the pattern yyyy‑MM‑dd.");
            assertEquals(4, fp.getEndIndex(), "End index of YEAR field should be 4 for the pattern yyyy‑MM‑dd.");
        }
    }

    @Test
    void format_fieldPositionIndexesRespectExistingBufferContent() {
        // 1970‑01‑01
        Date date = new Date(0L);
        // leading space
        StringBuffer sb = new StringBuffer(" ");
        FieldPosition fp = new FieldPosition(DateFormat.YEAR_FIELD);
        SimpleDateFormat realDelegate = new SimpleDateFormat(PATTERN);
        try (MockedStatic<SafeSimpleDateFormat> mockedStatic = mockStatic(SafeSimpleDateFormat.class)) {
            mockedStatic.when(() -> SafeSimpleDateFormat.getDateFormat(PATTERN)).thenReturn(realDelegate);
            safeSdf.format(date, sb, fp);
            // After formatting, buffer should be " 1970-01-01"
            assertEquals(" 1970-01-01", sb.toString(), "The formatted date should be appended after the leading space.");
            // Year starts after the leading space, i.e. at index 1
            assertEquals(1, fp.getBeginIndex(), "Begin index should account for pre‑existing characters.");
            assertEquals(5, fp.getEndIndex(), "End index should account for pre‑existing characters.");
        }
    }

    @Test
    void format_whenSafeSimpleDateFormatCreatedWithNullPattern_throwsNullPointerException() {
        SafeSimpleDateFormat sdfWithNullPattern = new SafeSimpleDateFormat(null);
        Date date = new Date();
        StringBuffer sb = new StringBuffer();
        FieldPosition fp = new FieldPosition(DateFormat.YEAR_FIELD);
        // No need to mock getDateFormat – the real static method will be invoked and
        // will attempt to create a SimpleDateFormat with a null pattern, which throws NPE.
        assertThrows(NullPointerException.class, () -> sdfWithNullPattern.format(date, sb, fp), "Creating SimpleDateFormat with a null pattern should result in NPE.");
    }

    @Test
    void format_whenSafeSimpleDateFormatCreatedWithInvalidPattern_throwsIllegalArgumentException() {
        SafeSimpleDateFormat sdfInvalid = new SafeSimpleDateFormat("yyyy-MM-dd-@@");
        Date date = new Date();
        StringBuffer sb = new StringBuffer();
        FieldPosition fp = new FieldPosition(DateFormat.YEAR_FIELD);
        // The static factory will try to instantiate SimpleDateFormat with the illegal pattern,
        // which throws IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> sdfInvalid.format(date, sb, fp), "An illegal pattern should cause SimpleDateFormat to throw IllegalArgumentException.");
    }
}
