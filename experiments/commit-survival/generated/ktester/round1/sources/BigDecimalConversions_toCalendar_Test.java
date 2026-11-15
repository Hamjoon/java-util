package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toCalendar_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock chain: Converter -> ConverterOptions -> TimeZone
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that no unexpected interactions happened in each test
        verifyNoMoreInteractions(mockConverter, mockOptions);
    }

    // ---------- Positive seconds (including large values) ----------
    private static Stream<Arguments> positiveSecondsProvider() {
        return Stream.of(// standard positive value
        Arguments.of(new BigDecimal("12345.678"), TimeZone.getTimeZone("UTC"), 12_345_678L), // large value close to Long.MAX_VALUE/1000
        Arguments.of(new BigDecimal("9223372036.854775807"), TimeZone.getTimeZone("UTC"), Long.MAX_VALUE), // boundary just above 1 ms
        Arguments.of(new BigDecimal("0.001001"), TimeZone.getTimeZone("UTC"), 1L), // value less than one millisecond (truncates to 0)
        Arguments.of(new BigDecimal("0.000999"), TimeZone.getTimeZone("UTC"), 0L));
    }

    // ---------- Negative seconds (including large negative) ----------
    private static Stream<Arguments> negativeSecondsProvider() {
        return Stream.of(// simple negative value
        Arguments.of(new BigDecimal("-42"), TimeZone.getTimeZone("UTC"), -42_000L), // large negative close to Long.MIN_VALUE/1000
        Arguments.of(new BigDecimal("-9223372036.854775808"), TimeZone.getTimeZone("UTC"), Long.MIN_VALUE), // negative value just beyond -1 ms
        Arguments.of(new BigDecimal("-0.001001"), TimeZone.getTimeZone("UTC"), -1L), // negative sub‑millisecond (truncates to 0)
        Arguments.of(new BigDecimal("-0.000999"), TimeZone.getTimeZone("UTC"), 0L));
    }

    // ---------- Time‑zone handling ----------
    private static Stream<Arguments> timeZoneProvider() {
        return Stream.of(Arguments.of(new BigDecimal("1"), TimeZone.getTimeZone("America/New_York"), 1_000L), Arguments.of(new BigDecimal("1"), TimeZone.getTimeZone("Asia/Tokyo"), 1_000L));
    }

    @Test
    void toCalendar_WithWholeSeconds_ReturnsCorrectCalendar() {
        // given
        // 10 seconds
        BigDecimal seconds = BigDecimal.valueOf(10);
        // when
        Calendar calendar = BigDecimalConversions.toCalendar(seconds, mockConverter);
        // then
        assertNotNull(calendar, "Calendar should not be null");
        assertEquals(10_000L, calendar.getTimeInMillis(), "Milliseconds should be seconds * 1_000");
        assertEquals(TimeZone.getTimeZone("UTC"), calendar.getTimeZone(), "Calendar should use the TimeZone supplied by ConverterOptions");
        // verify interactions
        verify(mockConverter).getOptions();
        verify(mockOptions).getTimeZone();
    }

    @Test
    void toCalendar_WithFractionalSeconds_ReturnsCorrectCalendar() {
        // given
        // 1.234 seconds
        BigDecimal seconds = new BigDecimal("1.234");
        // when
        Calendar calendar = BigDecimalConversions.toCalendar(seconds, mockConverter);
        // then
        assertNotNull(calendar);
        // 1.234 * 1_000 = 1234.0 → longValue() = 1234
        assertEquals(1_234L, calendar.getTimeInMillis(), "Milliseconds should reflect seconds multiplied by 1_000");
        assertEquals(TimeZone.getTimeZone("UTC"), calendar.getTimeZone());
        verify(mockConverter).getOptions();
        verify(mockOptions).getTimeZone();
    }

    @Test
    void toCalendar_WithZeroSeconds_ReturnsEpochCalendar() {
        // given
        BigDecimal seconds = BigDecimal.ZERO;
        // when
        Calendar calendar = BigDecimalConversions.toCalendar(seconds, mockConverter);
        // then
        assertNotNull(calendar);
        assertEquals(0L, calendar.getTimeInMillis(), "Zero seconds should result in epoch (0 ms) calendar");
        assertEquals(TimeZone.getTimeZone("UTC"), calendar.getTimeZone());
        verify(mockConverter).getOptions();
        verify(mockOptions).getTimeZone();
    }

    @Test
    void toCalendar_WithNullFrom_ThrowsClassCastException() {
        // given
        Object nullObject = null;
        // when / then
        assertThrows(ClassCastException.class, () -> BigDecimalConversions.toCalendar(nullObject, mockConverter), "Passing a null (or non‑BigDecimal) should result in ClassCastException");
        // No interaction with options is required because the method fails before they are used
        verify(mockConverter, never()).getOptions();
        verify(mockOptions, never()).getTimeZone();
    }

    @ParameterizedTest(name = "[Positive] seconds={0}, tz={1}")
    @MethodSource("positiveSecondsProvider")
    void toCalendar_PositiveScenarios(BigDecimal seconds, TimeZone tz, long expectedMillis) {
        // adjust the mock to return the requested time‑zone
        when(mockOptions.getTimeZone()).thenReturn(tz);
        Calendar cal = BigDecimalConversions.toCalendar(seconds, mockConverter);
        assertNotNull(cal);
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Milliseconds should be seconds * 1_000 truncated to long");
        assertEquals(tz, cal.getTimeZone(), "Calendar must be created with the supplied TimeZone");
        verify(mockConverter).getOptions();
        verify(mockOptions).getTimeZone();
    }

    @ParameterizedTest(name = "[Negative] seconds={0}, tz={1}")
    @MethodSource("negativeSecondsProvider")
    void toCalendar_NegativeScenarios(BigDecimal seconds, TimeZone tz, long expectedMillis) {
        when(mockOptions.getTimeZone()).thenReturn(tz);
        Calendar cal = BigDecimalConversions.toCalendar(seconds, mockConverter);
        assertNotNull(cal);
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Milliseconds should be seconds * 1_000 truncated to long");
        assertEquals(tz, cal.getTimeZone());
        verify(mockConverter).getOptions();
        verify(mockOptions).getTimeZone();
    }

    @ParameterizedTest(name = "[TZ] seconds={0}, tz={1}")
    @MethodSource("timeZoneProvider")
    void toCalendar_TimeZoneScenarios(BigDecimal seconds, TimeZone tz, long expectedMillis) {
        when(mockOptions.getTimeZone()).thenReturn(tz);
        Calendar cal = BigDecimalConversions.toCalendar(seconds, mockConverter);
        assertNotNull(cal);
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Epoch milliseconds must not be affected by the time‑zone");
        assertEquals(tz, cal.getTimeZone(), "Calendar must be created with the supplied TimeZone");
        verify(mockConverter).getOptions();
        verify(mockOptions).getTimeZone();
    }

    // ---------- Invalid input type ----------
    @Test
    void toCalendar_NonBigDecimalInput_ThrowsClassCastException() {
        Object notABigDecimal = "not a BigDecimal";
        assertThrows(ClassCastException.class, () -> BigDecimalConversions.toCalendar(notABigDecimal, mockConverter), "Passing a non‑BigDecimal should cause a ClassCastException");
        // No interaction with the converter chain because the cast fails first
        verify(mockConverter, never()).getOptions();
        verify(mockOptions, never()).getTimeZone();
    }

    @Test
    void toCalendar_NullFrom_ThrowsNullPointerException() {
        // The method casts null to BigDecimal, then attempts to call multiply → NPE
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toCalendar(null, mockConverter), "Passing null should result in NullPointerException");
        // No interaction with the converter chain because the failure occurs before any call
        verify(mockConverter, never()).getOptions();
        verify(mockOptions, never()).getTimeZone();
    }

    // ---------- Null converter handling ----------
    @Test
    void toCalendar_NullConverter_ThrowsNullPointerException() {
        BigDecimal seconds = BigDecimal.valueOf(10);
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toCalendar(seconds, null), "A null Converter should cause NullPointerException when getOptions() is invoked");
        // No mock verification needed – we passed a literal null
    }
}
