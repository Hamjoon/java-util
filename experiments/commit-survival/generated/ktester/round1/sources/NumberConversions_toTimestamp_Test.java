package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toTimestamp_Test {

    /**
     * The shared Converter instance used for all tests.
     */
    private static Converter converter;

    // 2021‑06‑01T12:00:00Z
    private static final long SAMPLE_MILLIS = 1_622_548_800_000L;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the test tidy.
        converter = null;
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> numericConversionProvider() {
        return Stream.of(// Long value representing 2021‑06‑01T12:00:00.123Z
        org.junit.jupiter.params.provider.Arguments.of(1622548800123L, 1622548800123L), // String containing the same epoch‑millis value
        org.junit.jupiter.params.provider.Arguments.of("1622548800123", 1622548800123L), // Integer interpreted as milliseconds (i.e., 1 622 548 800 ms after epoch)
        org.junit.jupiter.params.provider.Arguments.of(1622548800, 1_622_548_800L));
    }

    @Test
    void toTimestamp_fromLong() {
        Long source = SAMPLE_MILLIS;
        Timestamp ts = NumberConversions.toTimestamp(source, converter);
        assertNotNull(ts, "Timestamp should not be null");
        assertEquals(SAMPLE_MILLIS, ts.getTime(), "Timestamp milliseconds must match the source long value");
    }

    @Test
    void toTimestamp_fromDate() {
        Date source = new Date(SAMPLE_MILLIS);
        Timestamp ts = NumberConversions.toTimestamp(source, converter);
        assertNotNull(ts, "Timestamp should not be null");
        assertEquals(SAMPLE_MILLIS, ts.getTime(), "Timestamp milliseconds must match the source Date value");
    }

    @Test
    void toTimestamp_fromStringNumber() {
        String source = Long.toString(SAMPLE_MILLIS);
        Timestamp ts = NumberConversions.toTimestamp(source, converter);
        assertNotNull(ts, "Timestamp should not be null");
        assertEquals(SAMPLE_MILLIS, ts.getTime(), "Timestamp milliseconds must match the numeric string value");
    }

    @Test
    void toTimestamp_fromInstant() {
        java.time.Instant source = java.time.Instant.ofEpochMilli(SAMPLE_MILLIS);
        Timestamp ts = NumberConversions.toTimestamp(source, converter);
        assertNotNull(ts, "Timestamp should not be null");
        assertEquals(SAMPLE_MILLIS, ts.getTime(), "Timestamp milliseconds must match the source Instant value");
    }

    @Test
    void toTimestamp_fromNull_returnsZeroTimestamp() {
        // According to NumberConversions contract, null converts to zero for primitive targets.
        Timestamp ts = NumberConversions.toTimestamp(null, converter);
        assertNotNull(ts, "Timestamp should not be null even when source is null");
        assertEquals(0L, ts.getTime(), "Timestamp for null input should be epoch (0 ms)");
    }

    /**
     * Parameterized test covering the numeric conversion scenarios:
     * <ul>
     *   <li>Long value (milliseconds)</li>
     *   <li>String containing a numeric long</li>
     *   <li>Integer value interpreted as milliseconds</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index} => from={0}, expected={1}")
    @MethodSource("numericConversionProvider")
    void toTimestamp_numericConversions(Object from, long expectedMillis) {
        Timestamp ts = NumberConversions.toTimestamp(from, converter);
        assertNotNull(ts, "Timestamp should not be null");
        assertEquals(expectedMillis, ts.getTime(), () -> "Timestamp milliseconds must match expected value for input: " + Objects.toString(from));
    }

    /**
     * Verifies that passing a {@code null} {@link Converter} results in a
     * {@link NullPointerException}. The {@code toLong} implementation dereferences
     * the converter, so the exception propagates up to {@code toTimestamp}.
     */
    @Test
    void toTimestamp_nullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toTimestamp(123, null), "Calling toTimestamp with a null Converter should throw NullPointerException");
    }

    /**
     * Ensures that an unsupported source type (plain {@link Object}) triggers
     * an {@link IllegalArgumentException}.
     */
    @Test
    void toTimestamp_unsupportedSource_throwsIAE() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toTimestamp(unsupported, converter), "Unsupported source type should cause IllegalArgumentException");
    }

    /**
     * Checks that a non‑numeric string causes a {@link NumberFormatException}
     * when the internal {@code toLong} attempts to parse it.
     */
    @Test
    void toTimestamp_nonNumericString_throwsNFE() {
        String badString = "not-a-number";
        assertThrows(NumberFormatException.class, () -> NumberConversions.toTimestamp(badString, converter), "Non‑numeric string should cause NumberFormatException");
    }

    /**
     * Validates that a numeric value exceeding {@link Long#MAX_VALUE} results in
     * an {@link ArithmeticException} (or {@link NumberFormatException} depending on
     * the internal implementation). The test accepts either, as both indicate overflow.
     */
    @Test
    void toTimestamp_numericOverflow_throwsException() {
        // Long.MAX_VALUE + 1
        String overflow = "9223372036854775808";
        Exception ex = assertThrows(Exception.class, () -> NumberConversions.toTimestamp(overflow, converter), "Overflowing numeric string should throw an exception");
        // Accept either NumberFormatException or ArithmeticException
        assertTrue(ex instanceof NumberFormatException || ex instanceof ArithmeticException, "Expected NumberFormatException or ArithmeticException, but got: " + ex);
    }
}
