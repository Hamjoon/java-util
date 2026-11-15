package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToAtomicLong_Test {

    /**
     * Provides a stream of valid conversion arguments.
     *
     * Each {@code Arguments} consists of:
     * <ul>
     *   <li>source object</li>
     *   <li>expected long value inside the resulting {@link AtomicLong}</li>
     *   <li>human‑readable description for the test case</li>
     * </ul>
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// null → null (handled separately below)
        Arguments.of(null, 0L, "null input should return null (handled in separate test)"), // Long → same value
        Arguments.of(123L, 123L, "Long instance should be wrapped with same value"), // Integer → long conversion
        Arguments.of(45, 45L, "Integer instance should be converted to long and wrapped"), // String numeric → parsed long
        Arguments.of("67", 67L, "String representing a number should be parsed and wrapped"), // BigInteger → long conversion
        Arguments.of(new BigInteger("89"), 89L, "BigInteger should be converted to long and wrapped"), // BigDecimal → long conversion (truncates fractional part)
        Arguments.of(new BigDecimal("1011.99"), 1011L, "BigDecimal should be converted to long (truncating) and wrapped"), // AtomicLong → same instance
        Arguments.of(new AtomicLong(2022L), 2022L, "AtomicLong instance should be returned unchanged"));
    }

    /**
     * Provides a stream of invalid conversion arguments.
     *
     * Each {@code Arguments} consists of:
     * <ul>
     *   <li>source object that cannot be converted</li>
     *   <li>the expected exception class</li>
     *   <li>a description for readability</li>
     * </ul>
     */
    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(Arguments.of("notANumber", NumberFormatException.class, "String that cannot be parsed as a number should cause NumberFormatException"), Arguments.of(Boolean.TRUE, IllegalArgumentException.class, "Boolean value is not convertible to AtomicLong, should throw IllegalArgumentException"), Arguments.of('c', IllegalArgumentException.class, "Character value is not convertible to AtomicLong, should throw IllegalArgumentException"), Arguments.of(new Date(), IllegalArgumentException.class, "Date instance is unsupported for AtomicLong conversion, should throw IllegalArgumentException"), Arguments.of(new Object(), IllegalArgumentException.class, "Arbitrary object without conversion support should cause IllegalArgumentException"));
    }

    @Test
    void convertFromLong() {
        // given
        Long source = 123L;
        // when
        AtomicLong result = Converter.convertToAtomicLong(source);
        // then
        assertNotNull(result, "Result should not be null for non‑null input");
        assertEquals(123L, result.get(), "AtomicLong should contain the same long value");
    }

    @Test
    void convertFromInteger() {
        // given
        Integer source = 45;
        // when
        AtomicLong result = Converter.convertToAtomicLong(source);
        // then
        assertNotNull(result);
        assertEquals(45L, result.get());
    }

    @Test
    void convertFromString() {
        // given
        String source = "67";
        // when
        AtomicLong result = Converter.convertToAtomicLong(source);
        // then
        assertNotNull(result);
        assertEquals(67L, result.get());
    }

    @Test
    void convertFromBigInteger() {
        // given
        BigInteger source = new BigInteger("89");
        // when
        AtomicLong result = Converter.convertToAtomicLong(source);
        // then
        assertNotNull(result);
        assertEquals(89L, result.get());
    }

    @Test
    void convertFromNullReturnsNull() {
        // given
        Object source = null;
        // when
        AtomicLong result = Converter.convertToAtomicLong(source);
        // then
        assertNull(result, "Result should be null when the input is null");
    }

    /* --------------------------------------------------------------
     *  Valid conversion scenarios – parameterized test
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: convert ''{0}'' → AtomicLong({1})")
    @MethodSource("validConversionProvider")
    void convertToAtomicLong_ValidConversions(Object source, long expectedValue, String description) {
        // when
        AtomicLong result = Converter.convertToAtomicLong(source);
        // then
        assertNotNull(result, description + " – result should not be null");
        assertEquals(expectedValue, result.get(), description + " – value mismatch");
    }

    @Test
    void convertToAtomicLong_NullInput_ReturnsNull() {
        // given
        Object source = null;
        // when
        AtomicLong result = Converter.convertToAtomicLong(source);
        // then
        assertNull(result, "Result should be null when the input is null");
    }

    @Test
    void convertToAtomicLong_AtomicLongInput_ReturnsSameInstance() {
        // given
        AtomicLong source = new AtomicLong(2022L);
        // when
        AtomicLong result = Converter.convertToAtomicLong(source);
        // then
        assertNotNull(result, "Result should not be null for non‑null AtomicLong input");
        assertSame(source, result, "AtomicLong input should be returned unchanged (same instance)");
    }

    /* --------------------------------------------------------------
     *  Exception scenarios – parameterized test
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: converting ''{0}'' throws {1}")
    @MethodSource("invalidConversionProvider")
    void convertToAtomicLong_InvalidConversions(Object source, Class<? extends Throwable> expectedException, String description) {
        // when / then
        Throwable thrown = assertThrows(expectedException, () -> Converter.convertToAtomicLong(source), description + " – expected exception not thrown");
        // Optional: verify exception message contains something useful
        assertNotNull(thrown.getMessage(), description + " – exception message should not be null");
    }
}
