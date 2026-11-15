package com.cedarsoftware.util.convert;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.DateTimeException;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toBigInteger_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the default options – they are sufficient for the conversion tests.
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper that builds a {@link Timestamp} from a ISO‑8601 date string and a time‑of‑day.
     */
    private Timestamp createTimestamp(String isoDate, int hour, int minute, int second, int nanoOfSecond) {
        return Timestamp.from(LocalDate.parse(isoDate).atTime(hour, minute, second, nanoOfSecond).atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Computes the expected {@link BigInteger} representation of an {@link Instant}
     * using the same algorithm that {@code InstantConversions.toBigInteger} employs
     * (total nanoseconds since the epoch).
     */
    private static BigInteger expectedBigIntegerFromInstant(Instant instant) {
        // epoch seconds → nanoseconds + the nanosecond adjustment
        return BigInteger.valueOf(instant.getEpochSecond()).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(instant.getNano()));
    }

    @Test
    void testTimestampToBigInteger() {
        // a handful of timestamps with varying dates, times and nanosecond parts
        Timestamp ts1 = createTimestamp("1888-01-02", 12, 30, 45, 123_456_789);
        Timestamp ts2 = createTimestamp("1969-12-31", 23, 59, 59, 999_999_999);
        Timestamp ts3 = createTimestamp("1970-01-01", 0, 0, 1, 1);
        Timestamp ts4 = createTimestamp("2023-06-15", 15, 30, 0, 500_000_000);
        // Expected values calculated directly from the underlying Instant
        BigInteger expected1 = expectedBigIntegerFromInstant(ts1.toInstant());
        BigInteger expected2 = expectedBigIntegerFromInstant(ts2.toInstant());
        BigInteger expected3 = expectedBigIntegerFromInstant(ts3.toInstant());
        BigInteger expected4 = expectedBigIntegerFromInstant(ts4.toInstant());
        // Exercise the method under test
        BigInteger actual1 = TimestampConversions.toBigInteger(ts1, converter);
        BigInteger actual2 = TimestampConversions.toBigInteger(ts2, converter);
        BigInteger actual3 = TimestampConversions.toBigInteger(ts3, converter);
        BigInteger actual4 = TimestampConversions.toBigInteger(ts4, converter);
        // Verify the results
        assertEquals(expected1, actual1, "BigInteger conversion for ts1 is incorrect");
        assertEquals(expected2, actual2, "BigInteger conversion for ts2 is incorrect");
        assertEquals(expected3, actual3, "BigInteger conversion for ts3 is incorrect");
        assertEquals(expected4, actual4, "BigInteger conversion for ts4 is incorrect");
    }

    /**
     * Normal case where a mock {@link Converter} is supplied.
     * The static {@link InstantConversions#toBigInteger} method is mocked to return a
     * pre‑determined {@link BigInteger}. This verifies that {@code TimestampConversions}
     * forwards the {@link Instant} correctly and returns the value produced by the
     * downstream conversion.
     */
    @Test
    void testToBigIntegerNormal() {
        // Arrange – a Timestamp representing 2023‑01‑01T00:00:00Z
        Timestamp ts = Timestamp.from(Instant.parse("2023-01-01T00:00:00Z"));
        // Mock Converter – its behaviour is irrelevant for this test because we
        // mock the static method that receives it.
        Converter mockConverter = Mockito.mock(Converter.class);
        // Expected value that the mocked static method will return
        // epoch seconds for 2023‑01‑01
        BigInteger expected = BigInteger.valueOf(1_672_531_200L);
        // Mock the static InstantConversions.toBigInteger(...) method
        try (MockedStatic<InstantConversions> mocked = Mockito.mockStatic(InstantConversions.class)) {
            mocked.when(() -> InstantConversions.toBigInteger(Mockito.any(Instant.class), Mockito.eq(mockConverter))).thenReturn(expected);
            // Act
            BigInteger actual = TimestampConversions.toBigInteger(ts, mockConverter);
            // Assert
            assertEquals(expected, actual, "The method should return the value supplied by the mocked InstantConversions");
            // Verify that the static method was called exactly once with the expected Instant
            mocked.verify(() -> InstantConversions.toBigInteger(ts.toInstant(), mockConverter));
        }
    }

    /**
     * Passing {@code null} as the {@code from} argument should result in a
     * {@link NullPointerException} because the implementation attempts to cast
     * the argument to {@link Timestamp} and invoke {@code toInstant()} on it.
     */
    @Test
    void testToBigIntegerNullFrom() {
        assertThrows(NullPointerException.class, () -> TimestampConversions.toBigInteger(null, converter), "Null 'from' should cause NullPointerException");
    }

    /**
     * Providing an object that is not a {@link Timestamp} (e.g., a {@link String})
     * must cause a {@link ClassCastException} during the cast in the method.
     */
    @Test
    void testToBigIntegerInvalidType() {
        String notATimestamp = "2023-01-01T00:00:00Z";
        assertThrows(ClassCastException.class, () -> TimestampConversions.toBigInteger(notATimestamp, converter), "Passing a non‑Timestamp should trigger ClassCastException");
    }

    /**
     * A {@code null} {@link Converter} leads to a {@link NullPointerException}
     * when {@link InstantConversions#toBigInteger} tries to use it.
     */
    @Test
    void testToBigIntegerNullConverter() {
        Timestamp ts = Timestamp.from(Instant.parse("2023-01-01T00:00:00Z"));
        assertThrows(NullPointerException.class, () -> TimestampConversions.toBigInteger(ts, null), "Null 'converter' should cause NullPointerException");
    }

    /**
     * Creating a {@link Timestamp} with a millisecond value that exceeds the range
     * supported by {@link Instant} (e.g., {@code Long.MAX_VALUE}) should cause
     * {@link Timestamp#toInstant()} to throw a {@link DateTimeException}.
     */
    @Test
    void testToBigIntegerTimestampOutOfRange() {
        // far beyond Instant's supported range
        Timestamp outOfRange = new Timestamp(Long.MAX_VALUE);
        assertThrows(DateTimeException.class, () -> TimestampConversions.toBigInteger(outOfRange, converter), "Timestamp with out‑of‑range value should cause DateTimeException");
    }
}
