package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toSqlDate_Test {

    /**
     * A mock Converter is sufficient because {@code DoubleConversions.toSqlDate} does not
     *  use the Converter argument.
     */
    @Mock
    Converter mockConverter;

    private static Stream<Arguments> provideTypicalValues() {
        return Stream.of(// Zero seconds → epoch date
        Arguments.of(0.0, "Zero seconds should map to the epoch date.", 0L), // One day (86400 seconds) → next calendar day
        Arguments.of(86_400.0, "One day (86400 seconds) should produce the next calendar day.", 86_400_000L), // Negative one day → previous calendar day
        Arguments.of(-86_400.0, "Negative one day should produce the previous calendar day.", -86_400_000L), // Fractional seconds (1.5) – the time part is truncated in java.sql.Date
        Arguments.of(1.5, "Fractional seconds (1.5) still fall on the epoch date because the time part is truncated.", 1_500L));
    }

    private static Stream<Arguments> provideSpecialDoubleValues() {
        return Stream.of(// Double.NaN → (long)NaN == 0
        Arguments.of(Double.NaN, "Double.NaN casts to 0 when converted to long, resulting in the epoch date.", 0L), // Double.POSITIVE_INFINITY → (long)Infinity == Long.MAX_VALUE
        Arguments.of(Double.POSITIVE_INFINITY, "Double.POSITIVE_INFINITY casts to Long.MAX_VALUE; java.sql.Date(Long.MAX_VALUE) yields the maximum representable date.", Long.MAX_VALUE), // Double.NEGATIVE_INFINITY → (long)-Infinity == Long.MIN_VALUE
        Arguments.of(Double.NEGATIVE_INFINITY, "Double.NEGATIVE_INFINITY casts to Long.MIN_VALUE; java.sql.Date(Long.MIN_VALUE) yields the minimum representable date.", Long.MIN_VALUE), // Very large number of seconds (1 × 10¹²) → fits in a long
        Arguments.of(1_000_000_000_000.0, "Very large number of seconds (1×10¹²) still fits in a long; the resulting date is far in the future.", 1_000_000_000_000_000L));
    }

    @Test
    void convertsPositiveDoubleToSqlDate() {
        // seconds since epoch (fractional part ignored after *1000)
        Double source = 1_234_567.89;
        Date result = DoubleConversions.toSqlDate(source, mockConverter);
        long expectedMillis = (long) (source * 1000L);
        assertEquals(expectedMillis, result.getTime(), "The milliseconds value of the produced java.sql.Date is incorrect.");
    }

    @Test
    void convertsZeroDoubleToSqlDate() {
        Double source = 0.0;
        Date result = DoubleConversions.toSqlDate(source, mockConverter);
        assertEquals(0L, result.getTime(), "Zero seconds should produce a java.sql.Date with epoch time 0.");
    }

    @Test
    void convertsNegativeDoubleToSqlDate() {
        Double source = -12345.678;
        Date result = DoubleConversions.toSqlDate(source, mockConverter);
        long expectedMillis = (long) (source * 1000L);
        assertEquals(expectedMillis, result.getTime(), "Negative seconds should be correctly converted to a java.sql.Date.");
    }

    @Test
    void convertsFractionalSecondsTruncatesProperly() {
        // 1.999 seconds → 1999 ms
        Double source = 1.999;
        Date result = DoubleConversions.toSqlDate(source, mockConverter);
        long expectedMillis = (long) (source * 1000L);
        assertEquals(expectedMillis, result.getTime(), "Fractional seconds must be multiplied by 1000 and truncated to long.");
    }

    @Test
    void throwsClassCastExceptionWhenSourceIsNotDouble() {
        // Arrange
        Object notADouble = "not a double";
        // Act & Assert
        assertThrows(ClassCastException.class, () -> DoubleConversions.toSqlDate(notADouble, mockConverter), "Providing an object that is not a Double should cause a ClassCastException.");
    }

    /**
     * Parameterised test covering the “Valid conversions – typical values”
     * group from the JSON specification.
     *
     * <p>For each input double we compute the expected milliseconds
     * {@code (long)(value * 1000)} and assert that the produced
     * {@link java.sql.Date} contains exactly that epoch value.</p>
     */
    @ParameterizedTest(name = "[{index}] from={0} → expected epoch ms={2}")
    @MethodSource("provideTypicalValues")
    void convertsTypicalValues(Object from, String description, long expectedMillis) {
        // Act
        Date result = DoubleConversions.toSqlDate(from, mockConverter);
        // Assert
        assertEquals(expectedMillis, result.getTime(), () -> "Failed for case: " + description);
    }

    /**
     * Parameterised test covering the “Edge cases – special double values”
     * group.  The expected epoch milliseconds are derived from the same
     * casting logic used inside {@code DoubleConversions.toSqlDate}.
     */
    @ParameterizedTest(name = "[{index}] from={0} ({1}) → expected epoch ms={2}")
    @MethodSource("provideSpecialDoubleValues")
    void convertsSpecialDoubleValues(Object from, String description, long expectedMillis) {
        // Act
        Date result = DoubleConversions.toSqlDate(from, mockConverter);
        // Assert
        assertEquals(expectedMillis, result.getTime(), () -> "Failed for case: " + description);
    }

    @Test
    void throwsNullPointerExceptionWhenSourceIsNull() {
        // Arrange
        Object nullSource = null;
        // Act & Assert
        assertThrows(NullPointerException.class, () -> DoubleConversions.toSqlDate(nullSource, mockConverter), "Passing null as the source object should trigger a NullPointerException.");
    }
}
