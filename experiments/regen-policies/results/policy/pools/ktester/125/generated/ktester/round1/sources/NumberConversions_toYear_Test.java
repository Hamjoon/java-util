package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.time.Year;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toYear_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    /**
     * Supplies all successful conversion scenarios extracted from the
     * provided JSON test data.
     */
    private static Stream<Arguments> successCases() {
        return Stream.of(// Short value
        Arguments.of(Short.valueOf((short) 2020), Year.of(2020)), // Integer value
        Arguments.of(Integer.valueOf(1999), Year.of(1999)), // Long value
        Arguments.of(Long.valueOf(3000L), Year.of(3000)), // BigDecimal (integral)
        Arguments.of(new BigDecimal("2025"), Year.of(2025)), // Byte value (positive)
        Arguments.of(Byte.valueOf((byte) 85), Year.of(85)), // Negative short
        Arguments.of(Short.valueOf((short) -5), Year.of(-5)), // Integer.MAX_VALUE overflows short -> (short)Integer.MAX_VALUE == -1
        Arguments.of(Integer.valueOf(Integer.MAX_VALUE), Year.of(-1)), // Long.MIN_VALUE low 16 bits are 0 -> (short)Long.MIN_VALUE == 0
        Arguments.of(Long.valueOf(Long.MIN_VALUE), Year.of(0)), // Double.NaN -> shortValue() == 0
        Arguments.of(Double.valueOf(Double.NaN), Year.of(0)), // Double.POSITIVE_INFINITY -> shortValue() == Short.MAX_VALUE (32767)
        Arguments.of(Double.valueOf(Double.POSITIVE_INFINITY), Year.of(32767)), // BigDecimal with fractional part (truncates toward zero)
        Arguments.of(new BigDecimal("2020.99"), Year.of(2020)), // BigInteger larger than short range wraps (65537 -> 1)
        Arguments.of(new BigInteger("65537"), Year.of(1)), // AtomicInteger implements Number
        Arguments.of(new AtomicInteger(2022), Year.of(2022)), // AtomicLong implements Number (negative)
        Arguments.of(new AtomicLong(-2022L), Year.of(-2022)), // Float with fractional part (truncates)
        Arguments.of(Float.valueOf(1999.9f), Year.of(1999)), // Negative byte
        Arguments.of(Byte.valueOf((byte) -100), Year.of(-100)));
    }

    /**
     * Supplies all exception scenarios extracted from the provided JSON.
     */
    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// Null source leads to NullPointerException when casting to Number
        Arguments.of(null, NullPointerException.class), // Non‑Number (String) leads to ClassCastException
        Arguments.of("2022", ClassCastException.class));
    }

    @Test
    void toYear_WithInteger_ReturnsCorrectYear() {
        // given
        Integer source = 2022;
        // when
        Year result = NumberConversions.toYear(source, mockConverter);
        // then
        assertEquals(Year.of(2022), result);
    }

    @Test
    void toYear_WithShort_ReturnsCorrectYear() {
        // given
        Short source = (short) 1999;
        // when
        Year result = NumberConversions.toYear(source, mockConverter);
        // then
        assertEquals(Year.of(1999), result);
    }

    @Test
    void toYear_WithLong_ReturnsCorrectYear() {
        // given
        Long source = 2000L;
        // when
        Year result = NumberConversions.toYear(source, mockConverter);
        // then
        assertEquals(Year.of(2000), result);
    }

    @Test
    void toYear_WithBigDecimal_ReturnsCorrectYear() {
        // given
        BigDecimal source = new BigDecimal(2021);
        // when
        Year result = NumberConversions.toYear(source, mockConverter);
        // then
        assertEquals(Year.of(2021), result);
    }

    @Test
    void toYear_WithOutOfRangeNumber_UsesShortValueTruncation() {
        // given: 70_000 exceeds the range of a signed short (‑32768..32767)
        Integer source = 70_000;
        // shortValue of 70_000 == (short)70_000 == 4464 (because 70_000 - 65536 = 4_464)
        short expectedShort = source.shortValue();
        // when
        Year result = NumberConversions.toYear(source, mockConverter);
        // then
        assertEquals(Year.of(expectedShort), result);
    }

    @Test
    void toYear_WithNonNumber_ThrowsClassCastException() {
        // given
        String source = "2020";
        // when / then
        assertThrows(ClassCastException.class, () -> NumberConversions.toYear(source, mockConverter));
    }

    /**
     * Parameterized test that verifies a correct {@link Year} is produced for
     * a variety of {@link Number} implementations.
     *
     * The expected {@link Year} is calculated using {@code number.shortValue()}
     * which matches the implementation of {@code NumberConversions.toYear}.
     */
    @ParameterizedTest(name = "[{index}] toYear({0}) => {1}")
    @MethodSource("successCases")
    void toYear_ReturnsExpectedYear(Object source, Year expected) {
        // when
        Year result = NumberConversions.toYear(source, mockConverter);
        // then
        assertEquals(expected, result);
    }

    /**
     * Parameterized test that verifies the method throws the expected exception
     * for invalid inputs such as {@code null} or non‑numeric objects.
     */
    @ParameterizedTest(name = "[{index}] toYear({0}) throws {1}")
    @MethodSource("exceptionCases")
    void toYear_ThrowsExpectedException(Object source, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> NumberConversions.toYear(source, mockConverter));
    }
}
