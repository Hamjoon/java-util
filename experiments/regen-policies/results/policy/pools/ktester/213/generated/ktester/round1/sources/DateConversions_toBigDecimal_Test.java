package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toBigDecimal_Test {

    /**
     * A sample {@link Date} used in the tests.
     */
    private static Date sampleDate;

    /**
     * Expected {@link BigDecimal} result for {@code sampleDate}.
     */
    private static BigDecimal expectedBigDecimal;

    /**
     * Mocked {@link Converter} – the method under test does not use it, but it must be supplied.
     */
    @Mock
    private Converter mockConverter;

    /* --------------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final Object from;

        final Converter converter;

        final BigDecimal expected;

        ArgumentsHolder(Object from, Converter converter, BigDecimal expected) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return "from=" + from + ", expected=" + expected;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Choose a deterministic instant – e.g., 2023‑03‑15 12:34:56.789 UTC
        // arbitrary non‑zero value
        long epochMillis = 1678884896789L;
        sampleDate = new Date(epochMillis);
        // The method divides the epoch milliseconds by 1 000 (BigDecimalConversions.GRAND)
        // with a scale of 9 and RoundingMode.DOWN.
        expectedBigDecimal = new BigDecimal(epochMillis).divide(new BigDecimal(1000L), 9, RoundingMode.DOWN);
    }

    /**
     * Valid conversion cases – includes zero, positive, negative and large values.
     */
    static Stream<ArgumentsHolder> validConversionProvider() {
        return Stream.of(// epoch start (0 ms)
        new ArgumentsHolder(new Date(0L), null, new BigDecimal("0.000000000")), // positive milliseconds not evenly divisible by 1000
        new ArgumentsHolder(new Date(1234L), null, new BigDecimal("1.234000000")), // negative milliseconds
        new ArgumentsHolder(new Date(-1500L), null, new BigDecimal("-1.500000000")), // very large epoch value (close to Long.MAX_VALUE)
        new ArgumentsHolder(new Date(9_223_372_036_854_775L), null, new BigDecimal("9223372036.854775000")), // subclass: java.sql.Timestamp
        new ArgumentsHolder(new java.sql.Timestamp(5_000L), null, new BigDecimal("5.000000000")), // subclass: java.sql.Date (represents exactly one day)
        new ArgumentsHolder(new java.sql.Date(86_400_000L), null, new BigDecimal("86400.000000000")), // extreme long values
        new ArgumentsHolder(new Date(Long.MAX_VALUE), null, new BigDecimal("9223372036854775.807000000")), new ArgumentsHolder(new Date(Long.MIN_VALUE), null, new BigDecimal("-9223372036854775.808000000")), // non‑null converter should be ignored
        new ArgumentsHolder(new Date(12_345L), new Converter() {
        }, new BigDecimal("12.345000000")));
    }

    /**
     * Null‑input handling – expects NullPointerException.
     */
    static Stream<Object> nullInputProvider() {
        return Stream.of((Object) null);
    }

    /**
     * Invalid type handling – expects ClassCastException.
     */
    static Stream<Object> invalidTypeProvider() {
        return Stream.of("not a date");
    }

    @Test
    void toBigDecimal_WithValidDate_ReturnsExpectedBigDecimal() {
        // GIVEN – sampleDate and a (mocked) Converter are already prepared
        // WHEN
        BigDecimal actual = DateConversions.toBigDecimal(sampleDate, mockConverter);
        // THEN
        assertEquals(expectedBigDecimal, actual, "The BigDecimal conversion of the Date should match the expected value.");
    }

    @Test
    void toBigDecimal_WithNullDate_ThrowsNullPointerException() {
        // GIVEN
        Object nullDate = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toBigDecimal(nullDate, mockConverter), "Converting a null Date should throw NullPointerException.");
    }

    @Test
    void toBigDecimal_WithNegativeEpoch_ReturnsCorrectValue() {
        // GIVEN
        // before 1970
        Date negativeDate = new Date(-123456789L);
        BigDecimal expected = new BigDecimal(negativeDate.getTime()).divide(new BigDecimal(1000L), 9, RoundingMode.DOWN);
        // WHEN
        BigDecimal actual = DateConversions.toBigDecimal(negativeDate, mockConverter);
        // THEN
        assertEquals(expected, actual, "Conversion should correctly handle dates before the Unix epoch.");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("validConversionProvider")
    void toBigDecimal_ValidConversions_ReturnsExpected(ArgumentsHolder holder) {
        // GIVEN
        Object from = holder.from;
        Converter converter = holder.converter != null ? holder.converter : mockConverter;
        BigDecimal expected = holder.expected;
        // WHEN
        BigDecimal actual = DateConversions.toBigDecimal(from, converter);
        // THEN
        assertEquals(0, expected.compareTo(actual), () -> "Expected " + expected + " but got " + actual);
    }

    @Test
    void toBigDecimal_NullDate_ThrowsNullPointerException() {
        // GIVEN
        Object nullDate = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toBigDecimal(nullDate, mockConverter), "Converting a null Date should throw NullPointerException.");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("invalidTypeProvider")
    void toBigDecimal_InvalidType_ThrowsClassCastException(Object invalid) {
        // GIVEN an object that is not a Date
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> DateConversions.toBigDecimal(invalid, mockConverter), "Providing a non‑Date object should result in ClassCastException.");
    }
}
