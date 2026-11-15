package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toSqlDate_Test {

    /**
     * The Converter instance used for all tests.
     */
    private static Converter converter;

    /**
     * Opens Mockito annotations (not strictly required here but kept for consistency).
     */
    private AutoCloseable mocks;

    @BeforeAll
    static void setupBeforeAll() {
        // Mock a ConverterOptions that provides no custom overrides.
        ConverterOptions options = mock(ConverterOptions.class);
        when(options.getConverterOverrides()).thenReturn(Collections.emptyMap());
        // Create the real Converter with the mocked options.
        converter = new Converter(options);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for each test (helps if we add more @Mock fields later).
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the Mockito session to avoid memory‑leaks.
        if (mocks != null) {
            mocks.close();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Explicitly release the static Converter reference.
        converter = null;
    }

    private static Stream<Arguments> directNumericProvider() {
        return Stream.of(// Long literal
        Arguments.of(1622548800000L, 1622548800000L), // Integer literal (auto‑boxed)
        Arguments.of(12345, 12345L), // Short literal (auto‑boxed to Short, then promoted)
        Arguments.of((short) 12345, 12345L));
    }

    private static Stream<Arguments> unsupportedSourceProvider() {
        return Stream.of(// Boolean
        Arguments.of(true), // Arbitrary Object
        Arguments.of(new Object()));
    }

    private static Stream<Arguments> malformedStringProvider() {
        return Stream.of(Arguments.of("123abc"), Arguments.of(""));
    }

    private static Stream<Arguments> customTypeWithoutConverterProvider() {
        return Stream.of(// BigInteger that exceeds long range
        Arguments.of(new BigInteger("12345678901234567890")), // LocalDateTime – no default conversion to epoch millis
        Arguments.of(LocalDateTime.of(2023, 1, 1, 0, 0)));
    }

    @Test
    void toSqlDate_fromLong_returnsDateWithSameMillis() {
        // given
        // 2022‑01‑01 00:00:00 UTC
        Long epochMillis = 1_640_995_200_000L;
        Date expected = new Date(epochMillis);
        // when
        Date actual = NumberConversions.toSqlDate(epochMillis, converter);
        // then
        assertNotNull(actual);
        assertEquals(expected.getTime(), actual.getTime(), "Date produced from a Long should have the same epoch milliseconds");
    }

    @Test
    void toSqlDate_fromStringNumber_returnsDateWithParsedMillis() {
        // given
        // same moment as above, expressed as a String
        String epochString = "1640995200000";
        Date expected = new Date(Long.parseLong(epochString));
        // when
        Date actual = NumberConversions.toSqlDate(epochString, converter);
        // then
        assertNotNull(actual);
        assertEquals(expected.getTime(), actual.getTime(), "String containing a numeric epoch should be parsed to the correct Date");
    }

    @Test
    void toSqlDate_fromDate_returnsDateWithSameInstant() {
        // given
        // arbitrary timestamp
        java.util.Date source = new java.util.Date(1_600_000_000_000L);
        Date expected = new Date(source.getTime());
        // when
        Date actual = NumberConversions.toSqlDate(source, converter);
        // then
        assertNotNull(actual);
        assertEquals(expected.getTime(), actual.getTime(), "Passing a Date should yield a Date with the same epoch milliseconds");
    }

    @Test
    void toSqlDate_fromNull_returnsEpochZeroDate() {
        // given
        Object source = null;
        // Converter returns 0 for null when converting to a primitive long
        Date expected = new Date(0L);
        // when
        Date actual = NumberConversions.toSqlDate(source, converter);
        // then
        assertNotNull(actual);
        assertEquals(expected.getTime(), actual.getTime(), "Null source should be converted to epoch 0 (1970‑01‑01T00:00:00Z)");
    }

    /**
     * Parameterised test for the "Direct numeric types" group.
     *
     * <p>Each argument is a pair of (input object, expected epoch millis).
     */
    @ParameterizedTest(name = "{index} => from={0}, expectedMillis={1}")
    @MethodSource("directNumericProvider")
    void toSqlDate_directNumericTypes(Object from, long expectedMillis) {
        Date actual = NumberConversions.toSqlDate(from, converter);
        assertNotNull(actual);
        assertEquals(expectedMillis, actual.getTime(), () -> "Numeric input [" + from + "] should be converted to Date with millis " + expectedMillis);
    }

    @Test
    void toSqlDate_fromNull_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> NumberConversions.toSqlDate(source, converter), "Null source should cause NullPointerException because toLong cannot derive a value");
    }

    @ParameterizedTest(name = "{index} => unsupported type {0}")
    @MethodSource("unsupportedSourceProvider")
    void toSqlDate_unsupportedSourceTypes(Object from) {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toSqlDate(from, converter), "Unsupported source type should trigger IllegalArgumentException");
    }

    @ParameterizedTest(name = "{index} => malformed numeric string \"{0}\"")
    @MethodSource("malformedStringProvider")
    void toSqlDate_malformedNumericString(String malformed) {
        assertThrows(NumberFormatException.class, () -> NumberConversions.toSqlDate(malformed, converter), "Malformed numeric string should cause NumberFormatException");
    }

    @ParameterizedTest(name = "{index} => custom type without converter {0}")
    @MethodSource("customTypeWithoutConverterProvider")
    void toSqlDate_customTypesWithoutConverter(Object from) {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toSqlDate(from, converter), "Custom type without a registered Converter should trigger IllegalArgumentException");
    }
}
