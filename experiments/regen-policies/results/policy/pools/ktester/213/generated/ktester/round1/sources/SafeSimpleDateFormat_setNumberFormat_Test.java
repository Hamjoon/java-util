package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.text.*;
import java.util.Date;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_setNumberFormat_Test {

    /**
     * Holds the mock for the static method {@code SafeSimpleDateFormat.getDateFormat(String)}.
     */
    private MockedStatic<SafeSimpleDateFormat> staticMock;

    /**
     * A mock {@link SimpleDateFormat} that will be returned from the static stub.
     */
    private SimpleDateFormat mockSdf;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock SimpleDateFormat for each test.
        mockSdf = mock(SimpleDateFormat.class);
        // Mock the static method getDateFormat(String) to return the mock SimpleDateFormat.
        staticMock = Mockito.mockStatic(SafeSimpleDateFormat.class);
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenReturn(mockSdf);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock so that other tests are not affected.
        if (staticMock != null) {
            staticMock.close();
        }
    }

    private static Stream<Arguments> validNumberFormats() {
        return Stream.of(// DecimalFormat with a custom pattern
        Arguments.of(new DecimalFormat("00.00")), // Default NumberFormat instance
        Arguments.of(NumberFormat.getInstance()));
    }

    @Test
    void setNumberFormat_delegatesToUnderlyingSimpleDateFormat() {
        // GIVEN
        String pattern = "yyyy/MM/dd";
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(pattern);
        NumberFormat mockNumberFormat = mock(NumberFormat.class);
        // WHEN
        safeSdf.setNumberFormat(mockNumberFormat);
        // THEN
        // Verify that the underlying SimpleDateFormat received the same NumberFormat instance.
        verify(mockSdf, times(1)).setNumberFormat(mockNumberFormat);
    }

    @ParameterizedTest(name = "[{index}] format = {0}")
    @MethodSource("validNumberFormats")
    @DisplayName("setNumberFormat - valid NumberFormat")
    void setNumberFormat_validNumberFormat(NumberFormat format) {
        // GIVEN
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(pattern);
        // WHEN
        safeSdf.setNumberFormat(format);
        // THEN
        verify(mockSdf, times(1)).setNumberFormat(format);
    }

    @Test
    @DisplayName("setNumberFormat - null NumberFormat")
    void setNumberFormat_nullNumberFormat() {
        // GIVEN
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(pattern);
        // Configure the mock to throw NPE when null is passed.
        doThrow(new NullPointerException()).when(mockSdf).setNumberFormat(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> safeSdf.setNumberFormat(null), "Expected NullPointerException when passing null NumberFormat");
    }

    @Test
    @DisplayName("setNumberFormat - invalid pattern")
    void setNumberFormat_invalidPattern() {
        // GIVEN
        String invalidPattern = "@@@";
        // Make the static factory throw IllegalArgumentException for the bad pattern.
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat(invalidPattern)).thenThrow(new IllegalArgumentException("Invalid pattern"));
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(invalidPattern);
        NumberFormat nf = new DecimalFormat("00.00");
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> safeSdf.setNumberFormat(nf), "Expected IllegalArgumentException propagated from getDateFormat");
    }

    @Test
    @DisplayName("setNumberFormat - overrides previous format")
    void setNumberFormat_overridesPrevious() {
        // GIVEN
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(pattern);
        NumberFormat first = new DecimalFormat("00.00");
        NumberFormat second = new DecimalFormat("##0.##");
        // WHEN – first call
        safeSdf.setNumberFormat(first);
        // WHEN – second call
        safeSdf.setNumberFormat(second);
        // THEN – verify both delegations occurred in order.
        InOrder inOrder = inOrder(mockSdf);
        inOrder.verify(mockSdf).setNumberFormat(first);
        inOrder.verify(mockSdf).setNumberFormat(second);
    }

    @Test
    @DisplayName("setNumberFormat - effect on formatting")
    void setNumberFormat_effectOnFormatting() {
        // Close the static mock so that the real getDateFormat method is used.
        staticMock.close();
        // Pattern that formats only milliseconds (numeric field).
        String pattern = "SSS";
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(pattern);
        // Use a DecimalFormat that forces three digits with leading zeros.
        NumberFormat nf = new DecimalFormat("000");
        safeSdf.setNumberFormat(nf);
        // Date with 7 milliseconds after epoch.
        Date date = new Date(7L);
        String result = safeSdf.format(date);
        // Expect "007" because the DecimalFormat pads to three digits.
        assertEquals("007", result, "Formatted output should reflect the custom NumberFormat applied to the numeric field");
    }

    @Test
    @DisplayName("setNumberFormat - SafeSimpleDateFormat constructed with null pattern")
    void setNumberFormat_nullPatternThrowsNPE() {
        // GIVEN
        staticMock.when(() -> SafeSimpleDateFormat.getDateFormat((String) isNull())).thenThrow(new NullPointerException("Pattern is null"));
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(null);
        NumberFormat nf = new DecimalFormat("00.00");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> safeSdf.setNumberFormat(nf), "Expected NullPointerException because the SafeSimpleDateFormat pattern is null");
    }

    @Test
    @DisplayName("setNumberFormat - SafeSimpleDateFormat constructed with empty pattern")
    void setNumberFormat_emptyPatternNoException() {
        // GIVEN
        String emptyPattern = "";
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(emptyPattern);
        NumberFormat nf = NumberFormat.getInstance();
        // WHEN
        safeSdf.setNumberFormat(nf);
        // THEN
        verify(mockSdf, times(1)).setNumberFormat(nf);
    }
}
