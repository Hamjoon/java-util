package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.DateUtilities;
import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toYearMonth_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use a real Converter with default options – the options contain a ZoneId that DateUtilities will use
        ConverterOptions options = new ConverterOptions();
        // Ensure a deterministic ZoneId (UTC) so that parsing results are predictable
        options.setZoneId(ZoneId.of("UTC"));
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test
        converter = null;
    }

    private static Stream<Arguments> successParsingProvider() {
        return Stream.of(// direct YearMonth.parse success
        Arguments.of("2023-07", null, YearMonth.of(2023, 7)), Arguments.of("2023-12", null, YearMonth.of(2023, 12)));
    }

    private static Stream<Arguments> fallbackSuccessProvider() {
        return Stream.of(// full date string – fallback should produce YearMonth 2023‑07
        Arguments.of("2023-07-15", mockConverterWithZone(ZoneId.of("UTC")), YearMonth.of(2023, 7)), // month name with year – fallback should also produce YearMonth 2023‑07
        Arguments.of("July 2023", mockConverterWithZone(ZoneId.of("UTC")), YearMonth.of(2023, 7)));
    }

    private static Stream<Arguments> fallbackNullProvider() {
        return Stream.of(Arguments.of("not-a-date", mockConverterWithZone(ZoneId.of("UTC")), null));
    }

    private static Stream<Arguments> fallbackExceptionProvider() {
        return Stream.of(Arguments.of("2023-13-01", mockConverterWithZone(ZoneId.of("UTC")), IllegalArgumentException.class));
    }

    private static Stream<Arguments> errorCasesProvider() {
        return Stream.of(// null input string – YearMonth.parse throws NPE
        Arguments.of(null, null, NullPointerException.class), // non‑String input – ClassCastException before any parsing
        Arguments.of(12345, null, ClassCastException.class), // fallback with null converter – NPE wrapped in IllegalArgumentException
        Arguments.of("2023-07-15", null, IllegalArgumentException.class), // fallback with converter whose options are null – wrapped IllegalArgumentException
        Arguments.of("2023-07-15", mockConverterWithNullOptions(), IllegalArgumentException.class), // fallback with converter whose zoneId is null – wrapped IllegalArgumentException
        Arguments.of("2023-07-15", mockConverterWithZone(null), IllegalArgumentException.class));
    }

    /**
     * Creates a mock {@link Converter} whose {@link Converter#getOptions()} returns an options
     *  containing the supplied {@link ZoneId}.
     */
    private static Converter mockConverterWithZone(ZoneId zoneId) {
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(zoneId);
        return mockConverter;
    }

    /**
     * Creates a mock {@link Converter} whose {@link Converter#getOptions()} returns {@code null}.
     */
    private static Converter mockConverterWithNullOptions() {
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(null);
        return mockConverter;
    }

    @Test
    void toYearMonth_parsesSimpleYearMonthString() {
        // given
        String input = "2023-07";
        // when
        YearMonth result = StringConversions.toYearMonth(input, converter);
        // then
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(7, result.getMonthValue());
    }

    @Test
    void toYearMonth_parsesFullDateTimeString_usingFallback() {
        String input = "2022-12-31T23:59:59Z";
        try (MockedStatic<DateUtilities> mocked = mockStatic(DateUtilities.class)) {
            ZonedDateTime zdt = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC"));
            mocked.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(zdt);
            YearMonth result = StringConversions.toYearMonth(input, new Converter(new ConverterOptions()));
            assertNotNull(result);
            assertEquals(2022, result.getYear());
            assertEquals(12, result.getMonthValue());
        }
    }

    @Test
    void toYearMonth_returnsNullWhenDateUtilitiesCannotParse() {
        String input = "not-a-date";
        try (MockedStatic<DateUtilities> mocked = mockStatic(DateUtilities.class)) {
            mocked.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(null);
            YearMonth result = StringConversions.toYearMonth(input, new Converter(new ConverterOptions()));
            assertNull(result);
        }
    }

    @Test
    void toYearMonth_throwsIllegalArgumentExceptionForUnparseableString() {
        String input = "2022-13-01";
        try (MockedStatic<DateUtilities> mocked = mockStatic(DateUtilities.class)) {
            mocked.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenThrow(new DateTimeParseException("Bad month", input, 5));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYearMonth(input, new Converter(new ConverterOptions())));
            assertTrue(ex.getMessage().contains("Unable to extract Year-Month from string"));
        }
    }

    @ParameterizedTest(name = "[{index}] direct parse ''{0}'' => {2}")
    @MethodSource("successParsingProvider")
    void toYearMonth_directParseSuccess(String input, Converter converter, YearMonth expected) {
        // When converter is null the method still works because it never uses it for direct parse
        YearMonth result = StringConversions.toYearMonth(input, converter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] fallback parse ''{0}'' => {2}")
    @MethodSource("fallbackSuccessProvider")
    void toYearMonth_fallbackParseSuccess(String input, Converter converter, YearMonth expected) {
        try (MockedStatic<DateUtilities> mockedDateUtil = mockStatic(DateUtilities.class)) {
            // Simulate DateUtilities.parseDate returning a ZonedDateTime matching the expected YearMonth
            ZonedDateTime zdt = ZonedDateTime.of(expected.getYear(), expected.getMonthValue(), 1, 0, 0, 0, 0, converter.getOptions().getZoneId() != null ? converter.getOptions().getZoneId() : ZoneId.of("UTC"));
            mockedDateUtil.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(zdt);
            YearMonth result = StringConversions.toYearMonth(input, converter);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] fallback returns null for ''{0}''")
    @MethodSource("fallbackNullProvider")
    void toYearMonth_fallbackReturnsNull(String input, Converter converter, YearMonth expected) {
        try (MockedStatic<DateUtilities> mockedDateUtil = mockStatic(DateUtilities.class)) {
            // DateUtilities returns null indicating it could not parse the string
            mockedDateUtil.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenReturn(null);
            YearMonth result = StringConversions.toYearMonth(input, converter);
            assertNull(result);
        }
    }

    @ParameterizedTest(name = "[{index}] fallback throws exception for ''{0}''")
    @MethodSource("fallbackExceptionProvider")
    void toYearMonth_fallbackThrowsException(String input, Converter converter, Class<? extends Throwable> expectedException) {
        try (MockedStatic<DateUtilities> mockedDateUtil = mockStatic(DateUtilities.class)) {
            // Simulate DateUtilities throwing an exception (e.g., DateTimeParseException)
            mockedDateUtil.when(() -> DateUtilities.parseDate(eq(input), any(ZoneId.class), eq(true))).thenThrow(new DateTimeParseException("Invalid date", input, 0));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toYearMonth(input, converter));
            assertTrue(ex.getMessage().contains("Unable to extract Year-Month from string"));
            assertNotNull(ex.getCause());
        }
    }

    @ParameterizedTest(name = "[{index}] error case ''{0}'' expects {2}")
    @MethodSource("errorCasesProvider")
    void toYearMonth_errorScenarios(Object input, Converter converter, Class<? extends Throwable> expectedException) {
        // For cases where DateUtilities would be invoked we need to mock it to avoid NPEs
        try (MockedStatic<DateUtilities> mockedDateUtil = mockStatic(DateUtilities.class)) {
            // Default behaviour – if called, return null (won't matter for these error paths)
            mockedDateUtil.when(() -> DateUtilities.parseDate(anyString(), any(ZoneId.class), anyBoolean())).thenReturn(null);
            assertThrows(expectedException, () -> StringConversions.toYearMonth(input, converter));
        }
    }
}
