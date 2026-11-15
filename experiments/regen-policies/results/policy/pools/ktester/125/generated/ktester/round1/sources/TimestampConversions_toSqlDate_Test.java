package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import java.util.Date;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toSqlDate_Test {

    /**
     * Shared {@link Converter} instance – the method does not use it, but it must be supplied.
     */
    private static Converter converter;

    /**
     * Timestamp used in the successful conversion test.
     */
    private Timestamp timestamp;

    /**
     * Expected {@link Date} result for {@code timestamp}.
     */
    private Date expectedDate;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the default options.
        converter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Deterministic epoch millisecond value for the basic tests.
        // 2021-06-01T00:00:00Z
        long epochMillis = 1_622_548_800_000L;
        timestamp = new Timestamp(epochMillis);
        expectedDate = new Date(epochMillis);
    }

    /**
     * Provides a stream of valid {@link Timestamp} inputs together with the
     * expected epoch‑millisecond value.  The {@link Converter} argument is
     * varied between {@code null} and a real instance to prove that the
     * method ignores it.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Regular positive timestamp, null converter
        Arguments.of(new Timestamp(1_622_548_800_000L), null, 1_622_548_800_000L), // Zero epoch
        Arguments.of(new Timestamp(0L), null, 0L), // Negative epoch (one day before 1970‑01‑01)
        Arguments.of(new Timestamp(-86_400_000L), null, -86_400_000L), // Extreme value: Long.MAX_VALUE
        Arguments.of(new Timestamp(Long.MAX_VALUE), null, Long.MAX_VALUE), // Timestamp with nanosecond precision – the nanoseconds are truncated by getTime()
        Arguments.of(Timestamp.valueOf("2021-06-01 12:34:56.123456789"), null, 1_622_550_896_123L), // Same as first case but with a non‑null Converter to prove the argument is ignored
        Arguments.of(new Timestamp(1_622_548_800_000L), new Converter(new ConverterOptions()), 1_622_548_800_000L));
    }

    /**
     * Provides inputs that are expected to cause an exception.
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null source – cast triggers NullPointerException
        Arguments.of(null, null, NullPointerException.class), // Wrong type – cast triggers ClassCastException
        Arguments.of("2021-06-01", null, ClassCastException.class));
    }

    @Test
    void toSqlDate_returnsDateWithSameEpochMillis() {
        // Act
        Date result = TimestampConversions.toSqlDate(timestamp, converter);
        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(expectedDate, result, "The Date should have the same epoch milliseconds as the Timestamp");
        assertNotSame(expectedDate, result, "A new Date instance should be created, not the original");
        assertEquals(Date.class, result.getClass(), "Result should be a java.util.Date, not a java.sql.Timestamp");
    }

    @Test
    void toSqlDate_throwsClassCastException_whenSourceIsNotTimestamp() {
        Object notATimestamp = "2021-06-01";
        assertThrows(ClassCastException.class, () -> TimestampConversions.toSqlDate(notATimestamp, converter), "A non‑Timestamp source should cause a ClassCastException");
    }

    @Test
    void toSqlDate_throwsNullPointerException_whenSourceIsNull() {
        assertThrows(NullPointerException.class, () -> TimestampConversions.toSqlDate(null, converter), "A null source should cause a NullPointerException due to the cast");
    }

    @ParameterizedTest(name = "[{index}] from={0}, expectedMillis={2}")
    @MethodSource("validConversionProvider")
    void toSqlDate_convertsVariousTimestamps(Object from, Converter conv, long expectedMillis) {
        // Act
        Date result = TimestampConversions.toSqlDate(from, conv);
        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(expectedMillis, result.getTime(), () -> "Date.getTime() should match the Timestamp's epoch milliseconds");
        assertEquals(Date.class, result.getClass(), "Result must be a java.util.Date (not a java.sql.Timestamp)");
        // Ensure a new instance is created – it must not be the same object as the source
        assertNotSame(from, result, "Result should be a new Date instance, not the original object");
    }

    @ParameterizedTest(name = "[{index}] from={0} => expect {2}")
    @MethodSource("exceptionProvider")
    void toSqlDate_throwsExpectedException(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> TimestampConversions.toSqlDate(from, conv), () -> "Expected " + expectedException.getSimpleName() + " when converting " + from);
    }
}
