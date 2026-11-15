package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toBigDecimal_Test {

    /**
     * A mocked {@link Converter}. The method under test does not use it, but the signature requires it.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> provideNormalCases() {
        return Stream.of(// Epoch start – should produce 0.000000000
        Arguments.of(new Date(0L), "0.000000000", "Epoch start (1970‑01‑01T00:00:00Z)"), // Positive timestamp – 1 234 567 890 ms => 1 234 567.890000000 seconds
        Arguments.of(new Date(1_234_567_890L), "1234567.890000000", "Positive timestamp"), // java.sql.Timestamp is a subclass of java.util.Date
        Arguments.of(new Timestamp(9_876_543_210L), "9876543.210000000", "Timestamp subclass handling"));
    }

    /**
     * Verifies that a normal {@link Date} is converted to a {@link BigDecimal}
     * using the same algorithm as the implementation.
     */
    @Test
    void toBigDecimal_convertsDateCorrectly() {
        // GIVEN
        // current time in milliseconds
        Date now = new Date();
        long epochMillis = now.getTime();
        // Expected value calculated with the same formula the production code uses.
        BigDecimal expected = new BigDecimal(epochMillis).divide(BigDecimalConversions.GRAND, 9, RoundingMode.DOWN);
        // WHEN
        BigDecimal actual = DateConversions.toBigDecimal(now, mockConverter);
        // THEN
        assertEquals(expected, actual, "The BigDecimal conversion of a Date should match the expected scaled value.");
    }

    /**
     * Verifies that passing {@code null} as the source object results in a {@link NullPointerException},
     * because the implementation casts the argument to {@link Date} without a null‑check.
     */
    @Test
    void toBigDecimal_nullSource_throwsNPE() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toBigDecimal(nullSource, mockConverter), "Calling toBigDecimal with a null source should throw NullPointerException.");
    }

    /**
     * Parameterized test covering typical {@link Date} (and {@link Timestamp}) inputs.
     *
     * <p>Each argument supplies:
     * <ul>
     *   <li>a {@code Date} (or subclass) instance</li>
     *   <li>the expected {@link BigDecimal} string representation (scale 9)</li>
     *   <li>a short description for readability</li>
     * </ul>
     * The expected value is calculated using the same formula as the production code
     * to avoid duplication of the conversion logic.</p>
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideNormalCases")
    void toBigDecimal_normalCases(Date from, String expectedString, String description) {
        // GIVEN
        long epochMillis = Objects.requireNonNull(from).getTime();
        BigDecimal expected = new BigDecimal(epochMillis).divide(BigDecimalConversions.GRAND, 9, RoundingMode.DOWN);
        // WHEN
        BigDecimal actual = DateConversions.toBigDecimal(from, mockConverter);
        // THEN
        assertEquals(expected, actual, () -> "Conversion failed for case: " + description);
        // also verify string representation matches the supplied expected string
        assertEquals(expectedString, actual.toPlainString(), () -> "String representation mismatch for case: " + description);
    }

    /**
     * Verifies that providing an object that is not a {@link Date} (or subclass)
     * causes a {@link ClassCastException} at the cast operation.
     */
    @Test
    void toBigDecimal_invalidType_throwsClassCastException() {
        // GIVEN
        Object notADate = new Object();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> DateConversions.toBigDecimal(notADate, mockConverter), "Providing a non‑Date object should result in ClassCastException.");
    }
}
