package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toLong_Test {

    /**
     * A mock {@link Converter} – the method under test does not use it, but it is required by the signature.
     */
    @Mock
    private Converter converter;

    /**
     * A mock {@link Converter} – the method under test does not use it, but it is required by the signature.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Helper that creates a {@link Timestamp} from a date string (ISO‑8601) at midnight UTC.
     */
    private static Timestamp createTimestamp(String isoDate) {
        return Timestamp.from(java.time.LocalDate.parse(isoDate).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
    }

    private static Stream<Arguments> validTimestampProvider() {
        // Standard case
        // 2021‑06‑01T12:00:00Z
        Timestamp tsStandard = new Timestamp(1622548800000L);
        // Before epoch (negative)
        // one day before 1970‑01‑01
        Timestamp tsBeforeEpoch = new Timestamp(-86400000L);
        // Long.MAX_VALUE
        Timestamp tsMax = new Timestamp(Long.MAX_VALUE);
        // Long.MIN_VALUE
        Timestamp tsMin = new Timestamp(Long.MIN_VALUE);
        // Zero epoch
        Timestamp tsZero = new Timestamp(0L);
        // Timestamp with explicit nanoseconds (should be ignored)
        Timestamp tsNanos = new Timestamp(1622548800123L);
        tsNanos.setNanos(123456789);
        // Non‑null Converter argument (method ignores it)
        Converter dummyConverter = new Converter() {
        };
        Timestamp tsWithConverter = new Timestamp(123456789L);
        return Stream.of(Arguments.of(tsStandard, null, 1622548800000L, "Standard case: a Timestamp representing 2021‑06‑01T12:00:00Z."), Arguments.of(tsBeforeEpoch, null, -86400000L, "Timestamp before the Unix epoch (one day earlier)."), Arguments.of(tsMax, null, Long.MAX_VALUE, "Timestamp created with Long.MAX_VALUE milliseconds."), Arguments.of(tsMin, null, Long.MIN_VALUE, "Timestamp constructed with Long.MIN_VALUE should return that value."), Arguments.of(tsZero, null, 0L, "Conversion of the epoch start (0 ms) should return 0."), Arguments.of(tsNanos, null, 1622548800123L, "Timestamp stores nanoseconds separately; getTime() returns only the millisecond part."), Arguments.of(tsWithConverter, dummyConverter, 123456789L, "Non‑null Converter argument is ignored; method returns timestamp's millisecond value."));
    }

    private static Stream<Arguments> exceptionProvider() {
        // Null source
        Object nullSource = null;
        // Non‑Timestamp objects
        Date javaUtilDate = new Date(0L);
        String stringSource = "2021-01-01";
        return Stream.of(Arguments.of(nullSource, null, NullPointerException.class, "Passing a null reference for the 'from' argument should cause a NullPointerException."), Arguments.of(javaUtilDate, null, ClassCastException.class, "Providing a java.util.Date (not java.sql.Timestamp) should result in ClassCastException."), Arguments.of(stringSource, null, ClassCastException.class, "Providing a String instead of a Timestamp should also trigger a ClassCastException."));
    }

    @Test
    void toLongReturnsExactEpochMillis() {
        Timestamp ts = createTimestamp("2023-06-15");
        long expected = ts.getTime();
        long actual = TimestampConversions.toLong(ts, mockConverter);
        assertEquals(expected, actual, "toLong should return the exact epoch millisecond value of the Timestamp");
    }

    @Test
    void toLongWithNullSourceThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> TimestampConversions.toLong(null, mockConverter), "Casting a null source to Timestamp should result in NPE when getTime() is invoked");
    }

    @Test
    void toLongWithInvalidSourceThrowsClassCastException() {
        Object notATimestamp = "2023-06-15";
        assertThrows(ClassCastException.class, () -> TimestampConversions.toLong(notATimestamp, mockConverter), "Providing a non‑Timestamp source must result in a ClassCastException");
    }

    /**
     * Valid conversion scenarios – the method should return the exact
     * millisecond value stored in the {@link Timestamp}, ignoring any
     * nanosecond component and regardless of the supplied {@link Converter}.
     */
    @ParameterizedTest(name = "{3}")
    @MethodSource("validTimestampProvider")
    void toLongValidConversions(Timestamp from, Converter converter, long expected, String description) {
        long actual = TimestampConversions.toLong(from, converter);
        assertEquals(expected, actual, description);
    }

    /**
     * Scenarios that must result in an exception (null source or wrong type).
     */
    @ParameterizedTest(name = "{3}")
    @MethodSource("exceptionProvider")
    void toLongExceptionScenarios(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> TimestampConversions.toLong(from, converter), description);
    }
}
