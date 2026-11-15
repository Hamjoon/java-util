package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Year;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class YearConversions_toFloat_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter needs a ConverterOptions instance – the default options are sufficient.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
        converter = null;
    }

    static Stream<Arguments> nullAndEmptyProvider() {
        return Stream.of(Arguments.of(null, 0f), // empty string should be treated as missing value → 0
        Arguments.of("", 0f));
    }

    static Stream<Arguments> yearInstanceProvider() {
        return Stream.of(Arguments.of(Year.of(1995), 1995f), Arguments.of(Year.of(0), 0f));
    }

    static Stream<Arguments> numericStringProvider() {
        return Stream.of(Arguments.of("2021", 2021f), // whitespace should be trimmed
        Arguments.of("  1987  ", 1987f));
    }

    static Stream<Arguments> numberTypesProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(2005), 2005f), Arguments.of(Long.valueOf(1999L), 1999f), Arguments.of(Short.valueOf((short) 1975), 1975f), Arguments.of(Byte.valueOf((byte) 65), 65f), Arguments.of(BigInteger.valueOf(2022), 2022f), Arguments.of(BigDecimal.valueOf(2018), 2018f), // truncates fraction
        Arguments.of(Double.valueOf(2020.99), 2020f), // truncates fraction
        Arguments.of(Float.valueOf(1994.75f), 1994f));
    }

    static Stream<Arguments> atomicTypesProvider() {
        return Stream.of(Arguments.of(new AtomicInteger(2001), 2001f), Arguments.of(new AtomicLong(1998L), 1998f), Arguments.of(new AtomicBoolean(true), 1f), Arguments.of(new AtomicBoolean(false), 0f));
    }

    static Stream<Arguments> mapWithYearKeyProvider() {
        return Stream.of(Arguments.of(2015, 2015f), Arguments.of("2023", 2023f), Arguments.of(Year.of(1990), 1990f));
    }

    static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("notANumber"), Arguments.of("123ABC"));
    }

    static Stream<Arguments> boundaryIntegerProvider() {
        return Stream.of(Arguments.of(Integer.MAX_VALUE, (float) Integer.MAX_VALUE), Arguments.of(Integer.MIN_VALUE, (float) Integer.MIN_VALUE));
    }

    static Stream<Arguments> floatingPointTruncationProvider() {
        return Stream.of(Arguments.of(Double.valueOf(2022.5), 2022f), Arguments.of(BigDecimal.valueOf(1999.99), 1999f));
    }

    static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE), Arguments.of(new Object()), Arguments.of(Character.valueOf('A')));
    }

    static Stream<Arguments> outOfRangeProvider() {
        return Stream.of(// Long beyond int range – should trigger ArithmeticException
        Arguments.of(Long.valueOf(Integer.MAX_VALUE + 1L), ArithmeticException.class), Arguments.of(Long.valueOf(Integer.MIN_VALUE - 1L), ArithmeticException.class), // BigInteger beyond int range – also ArithmeticException
        Arguments.of(BigInteger.valueOf(Long.MAX_VALUE), ArithmeticException.class), // String representing a number out of int range – NumberFormatException
        Arguments.of("2147483648", NumberFormatException.class), Arguments.of("-2147483649", NumberFormatException.class));
    }

    static Stream<Arguments> mapMissingYearKeyProvider() {
        return Stream.of(Arguments.of(Collections.singletonMap("someOtherKey", 2020)), Arguments.of(Collections.emptyMap()));
    }

    @Test
    void toFloat_fromYear_returnsYearValueAsFloat() {
        // given
        Year year = Year.of(2023);
        float expected = 2023f;
        // when
        float actual = YearConversions.toFloat(year, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void toFloat_fromMapWithYearKey_returnsMappedValueAsFloat() {
        // given
        // MapConversions.YEAR is the key used by the conversion logic; it resolves to "year".
        Map<String, Object> map = Map.of(MapConversions.YEAR, 1999);
        float expected = 1999f;
        // when
        float actual = YearConversions.toFloat(map, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void toFloat_fromStringNumber_returnsParsedNumberAsFloat() {
        // given
        String source = "2025";
        float expected = 2025f;
        // when
        float actual = YearConversions.toFloat(source, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void toFloat_fromInteger_returnsSameValueAsFloat() {
        // given
        Integer source = 1987;
        float expected = 1987f;
        // when
        float actual = YearConversions.toFloat(source, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void toFloat_fromNull_returnsZero() {
        // given
        Object source = null;
        // Converter returns 0 for primitive numeric types.
        float expected = 0f;
        // when
        float actual = YearConversions.toFloat(source, converter);
        // then
        assertEquals(expected, actual);
    }

    // ---------- null / empty handling ----------
    @ParameterizedTest(name = "{index}: from={0}")
    @MethodSource("nullAndEmptyProvider")
    void toFloat_nullAndEmptyHandling(Object from, float expected) {
        assertEquals(expected, YearConversions.toFloat(from, converter));
    }

    // ---------- Year instance handling ----------
    @ParameterizedTest(name = "{index}: Year={0}")
    @MethodSource("yearInstanceProvider")
    void toFloat_yearInstanceHandling(Year year, float expected) {
        assertEquals(expected, YearConversions.toFloat(year, converter));
    }

    // ---------- numeric string handling ----------
    @ParameterizedTest(name = "{index}: string=\"{0}\"")
    @MethodSource("numericStringProvider")
    void toFloat_numericStringHandling(String source, float expected) {
        assertEquals(expected, YearConversions.toFloat(source, converter));
    }

    // ---------- Number types handling ----------
    @ParameterizedTest(name = "{index}: from={0}")
    @MethodSource("numberTypesProvider")
    void toFloat_numberTypesHandling(Object from, float expected) {
        assertEquals(expected, YearConversions.toFloat(from, converter));
    }

    // ---------- Atomic types handling ----------
    @ParameterizedTest(name = "{index}: from={0}")
    @MethodSource("atomicTypesProvider")
    void toFloat_atomicTypesHandling(Object from, float expected) {
        assertEquals(expected, YearConversions.toFloat(from, converter));
    }

    // ---------- map with YEAR key handling ----------
    @ParameterizedTest(name = "{index}: mapValue={0}")
    @MethodSource("mapWithYearKeyProvider")
    void toFloat_mapWithYearKeyHandling(Object mapValue, float expected) {
        Map<?, ?> map = Collections.singletonMap(MapConversions.YEAR, mapValue);
        assertEquals(expected, YearConversions.toFloat(map, converter));
    }

    // ---------- invalid string handling ----------
    @ParameterizedTest(name = "{index}: invalidString=\"{0}\"")
    @MethodSource("invalidStringProvider")
    void toFloat_invalidStringHandling(String source) {
        assertThrows(NumberFormatException.class, () -> YearConversions.toFloat(source, converter));
    }

    // ---------- boundary integer values ----------
    @ParameterizedTest(name = "{index}: int={0}")
    @MethodSource("boundaryIntegerProvider")
    void toFloat_boundaryIntegerValues(int value, float expected) {
        assertEquals(expected, YearConversions.toFloat(value, converter));
    }

    // ---------- floating point truncation ----------
    @ParameterizedTest(name = "{index}: source={0}")
    @MethodSource("floatingPointTruncationProvider")
    void toFloat_floatingPointTruncation(Object source, float expected) {
        assertEquals(expected, YearConversions.toFloat(source, converter));
    }

    // ---------- unsupported type handling ----------
    @ParameterizedTest(name = "{index}: unsupported={0}")
    @MethodSource("unsupportedTypesProvider")
    void toFloat_unsupportedTypeHandling(Object source) {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toFloat(source, converter));
    }

    // ---------- out of int range handling ----------
    @ParameterizedTest(name = "{index}: outOfRange={0}")
    @MethodSource("outOfRangeProvider")
    void toFloat_outOfIntRangeHandling(Object source, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> YearConversions.toFloat(source, converter));
    }

    // ---------- map missing YEAR key handling ----------
    @ParameterizedTest(name = "{index}: map={0}")
    @MethodSource("mapMissingYearKeyProvider")
    void toFloat_mapMissingYearKeyHandling(Map<?, ?> map) {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toFloat(map, converter));
    }

    // ---------- map with null YEAR value handling ----------
    @Test
    void toFloat_mapWithNullYearValueThrowsNPE() {
        Map<?, ?> map = Collections.singletonMap(MapConversions.YEAR, null);
        assertThrows(NullPointerException.class, () -> YearConversions.toFloat(map, converter));
    }

    // ---------- null converter handling ----------
    @Test
    void toFloat_nullConverterThrowsNPE() {
        assertThrows(NullPointerException.class, () -> YearConversions.toFloat(Year.of(2021), null));
    }
}
