package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.util.Objects;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toDate_Test {

    /**
     * Mocked {@link Converter} – not used for the numeric inputs exercised here.
     */
    @Mock
    private Converter mockConverter;

    static Stream<Object> unsupportedSourceProvider() {
        return Stream.of(// non‑numeric string
        "some string", // arbitrary object
        new Object());
    }

    @Test
    void toDate_givenLong_returnsDateWithSameEpochMillis() {
        // GIVEN
        // 2021‑06‑01T00:00:00Z
        long epochMillis = 1_622_548_800_000L;
        Long source = epochMillis;
        // WHEN
        Date result = NumberConversions.toDate(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(epochMillis, result.getTime(), "Date.getTime() must match the original long value");
    }

    @Test
    void toDate_givenInteger_returnsDateWithSameEpochMillis() {
        // GIVEN
        // small positive number
        int epochMillis = 123_456_789;
        Integer source = epochMillis;
        // WHEN
        Date result = NumberConversions.toDate(source, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals((long) epochMillis, result.getTime(), "Date should represent the integer value as milliseconds since epoch");
    }

    @Test
    void toDate_givenStringNumeric_returnsDateWithParsedMillis() {
        // GIVEN
        // epoch start
        String source = "0";
        // WHEN
        Date result = NumberConversions.toDate(source, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(0L, result.getTime(), "String \"0\" should be parsed to epoch start (0 ms)");
    }

    @Test
    void toDate_returnsNewDateInstanceEachCall() {
        // GIVEN
        Long source = 1_000L;
        // WHEN
        Date first = NumberConversions.toDate(source, mockConverter);
        Date second = NumberConversions.toDate(source, mockConverter);
        // THEN
        assertNotSame(first, second, "Each call must produce a distinct Date instance");
        assertEquals(first.getTime(), second.getTime(), "Both Dates should represent the same instant");
    }

    @Test
    void toDate_givenJavaUtilDate_returnsCopyWithSameEpochMillis() {
        // GIVEN a java.util.Date instance
        Date original = new Date(1_622_548_800_000L);
        // WHEN converting it
        Date result = NumberConversions.toDate(original, mockConverter);
        // THEN a new Date with the same epoch millis is produced
        assertNotNull(result);
        assertNotSame(original, result, "Result must be a new Date instance");
        assertEquals(original.getTime(), result.getTime(), "Converted Date must have the same epoch milliseconds as the source");
    }

    @Test
    void toDate_givenNullFrom_throwsNullPointerException() {
        // The contract of NumberConversions.toLong (and thus toDate) is to throw NPE
        // when the source object is null.
        assertThrows(NullPointerException.class, () -> NumberConversions.toDate(null, null), "Passing null as the source should result in NPE");
    }

    @ParameterizedTest
    @MethodSource("unsupportedSourceProvider")
    void toDate_givenUnsupportedSource_throwsIllegalArgumentException(Object unsupported) {
        // GIVEN an unsupported source (non‑numeric, non‑Date)
        // WHEN converting it
        // THEN an IllegalArgumentException is expected
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDate(unsupported, mockConverter), () -> "Source of type " + (unsupported == null ? "null" : unsupported.getClass()) + " should trigger IllegalArgumentException");
    }

    @Test
    void toDate_givenBigIntegerOverflow_throwsArithmeticException() {
        // Create a BigInteger that is one larger than Long.MAX_VALUE
        BigInteger overflow = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        assertThrows(ArithmeticException.class, () -> NumberConversions.toDate(overflow, mockConverter), "Overflowing numeric value should cause ArithmeticException");
    }

    @Test
    void toDate_converterThrowsRuntimeException_isPropagated() {
        // Arrange a mock Converter that throws RuntimeException for any conversion request.
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("forced failure");
        });
        // Use a source that forces the conversion path to go through the Converter.
        // A plain Long would bypass the Converter, so we use a custom object.
        Object customSource = new Object();
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> NumberConversions.toDate(customSource, throwingConverter), "RuntimeException from Converter must be propagated");
        assertEquals("forced failure", ex.getMessage());
    }
}
