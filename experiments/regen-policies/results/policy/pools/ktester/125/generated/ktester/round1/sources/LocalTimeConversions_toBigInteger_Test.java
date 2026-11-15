package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.time.LocalTime;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toBigInteger_Test {

    /**
     * A real {@link Converter} instance – the conversion logic inside {@code toBigInteger}
     *  does not actually use the {@code Converter}, but we provide a valid instance to
     *  satisfy the method signature.
     */
    private Converter converter;

    /**
     * Simple stub that extends {@link Converter}. The concrete behaviour of Converter
     * is irrelevant for {@code LocalTimeConversions.toBigInteger}, but the test data
     * includes a non‑null instance to ensure the method truly ignores it.
     */
    private static class MockConverter extends Converter {

        MockConverter() {
            super(new ConverterOptions());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – they are sufficient for the test.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC; not strictly required.
        converter = null;
    }

    private static Stream<Arguments> toBigIntegerConversionTests() {
        return Stream.of(// Midnight, converter = null
        Arguments.of("Convert midnight (00:00:00.000000000) to BigInteger; nanoseconds of day should be 0.", LocalTime.of(0, 0, 0, 0), null, BigInteger.valueOf(0L)), // Typical time with nanosecond part
        Arguments.of("Convert a typical time with nanosecond part; verify correct nanoseconds calculation.", LocalTime.of(12, 34, 56, 789_000_000), null, // 12:34:56.789 = 45_296_789_000_000 ns
        BigInteger.valueOf(45_296_789_000_000L)), // Last nanosecond of the day
        Arguments.of("Convert the last nanosecond of the day; ensure maximum value is returned.", LocalTime.of(23, 59, 59, 999_999_999), null, BigInteger.valueOf(86_399_999_999_999L)), // Null source
        Arguments.of("Passing null as the source object should cause a NullPointerException when casting to LocalTime.", null, null, NullPointerException.class), // Incompatible type
        Arguments.of("Providing an object of an incompatible type should result in a ClassCastException.", "not a LocalTime", null, ClassCastException.class));
    }

    private static Stream<Arguments> additionalToBigIntegerConversionTests() {
        return Stream.of(// Smallest non‑zero nanosecond
        Arguments.of("Convert the smallest non‑zero nanosecond of the day; result should be 1.", LocalTime.of(0, 0, 0, 1), null, BigInteger.valueOf(1L)), // Early‑morning fractional nanosecond part
        Arguments.of("Early‑morning time with a fractional nanosecond part; verify exact nanosecond count.", LocalTime.of(0, 0, 0, 123_456_789), null, BigInteger.valueOf(123_456_789L)), // Exactly noon
        Arguments.of("Exactly noon (12:00:00.000000000); nanoseconds should equal 12 hours in nanoseconds.", LocalTime.of(12, 0, 0, 0), null, BigInteger.valueOf(43_200_000_000_000L)), // Second‑to‑last nanosecond of the day
        Arguments.of("Second‑to‑last nanosecond of the day; ensures boundary handling just before the maximum.", LocalTime.of(23, 59, 59, 999_999_998), null, BigInteger.valueOf(86_399_999_999_998L)), // Parsed time with millisecond precision
        Arguments.of("Parse a time string with millisecond precision; verify conversion of hour, minute, second and sub‑second parts.", LocalTime.parse("15:30:45.250"), null, BigInteger.valueOf(55_845_250_000_000L)), // Conversion with a non‑null Converter argument (ignored)
        Arguments.of("Conversion with a non‑null Converter argument (which is ignored); checks that the method does not depend on the converter.", LocalTime.of(5, 0, 0, 0), new MockConverter(), BigInteger.valueOf(18_000_000_000_000L)), // Midnight with a non‑null Converter
        Arguments.of("Midnight conversion when a Converter instance is supplied; confirms identical result to the null‑converter case.", LocalTime.of(0, 0, 0, 0), new MockConverter(), BigInteger.valueOf(0L)));
    }

    @Test
    void toBigInteger_convertsMidnightCorrectly() {
        // given
        // 00:00:00.000000000
        LocalTime lt = LocalTime.MIDNIGHT;
        // when
        BigInteger result = LocalTimeConversions.toBigInteger(lt, converter);
        // then
        assertEquals(BigInteger.valueOf(0L), result, "Midnight should convert to a BigInteger value of 0 nanoseconds");
    }

    @Test
    void toBigInteger_convertsArbitraryTimeCorrectly() {
        // given
        // 12:34:56.789123456 -> calculate expected nanos manually via API
        LocalTime lt = LocalTime.of(12, 34, 56, 789_123_456);
        long expectedNanos = lt.toNanoOfDay();
        // when
        BigInteger result = LocalTimeConversions.toBigInteger(lt, converter);
        // then
        assertEquals(BigInteger.valueOf(expectedNanos), result, "The conversion should return the nanoseconds‑of‑day as a BigInteger");
    }

    @Test
    void toBigInteger_throwsWhenSourceIsNotLocalTime() {
        // given
        Object notALocalTime = "13:00";
        // when / then
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toBigInteger(notALocalTime, converter), "A non‑LocalTime source should result in a ClassCastException");
    }

    @Test
    void toBigInteger_throwsWhenSourceIsNull() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toBigInteger(nullSource, converter), "Passing null as the source should throw NullPointerException");
    }

    /**
     * -----------------------------------------------------------------------
     *  First group – “toBigInteger conversion tests”
     *  -----------------------------------------------------------------------
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("toBigIntegerConversionTests")
    void toBigInteger_conversionTests(String description, Object from, Converter conv, Object expected) {
        // The method under test never uses the Converter, but we still pass the supplied value.
        if (expected instanceof Class) {
            // Expected an exception.
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> LocalTimeConversions.toBigInteger(from, conv), description);
        } else {
            // Expected a concrete BigInteger result.
            BigInteger result = LocalTimeConversions.toBigInteger(from, conv);
            assertEquals(expected, result, description);
        }
    }

    /**
     * -----------------------------------------------------------------------
     *  Second group – “additional toBigInteger conversion tests”
     *  -----------------------------------------------------------------------
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("additionalToBigIntegerConversionTests")
    void additional_toBigInteger_conversionTests(String description, Object from, Converter conv, Object expected) {
        if (expected instanceof Class) {
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> LocalTimeConversions.toBigInteger(from, conv), description);
        } else {
            BigInteger result = LocalTimeConversions.toBigInteger(from, conv);
            assertEquals(expected, result, description);
        }
    }
}
