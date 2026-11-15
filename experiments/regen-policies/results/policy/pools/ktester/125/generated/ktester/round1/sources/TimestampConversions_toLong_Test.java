package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toLong_Test {

    /**
     * The mock Converter is not used by the method under test, but it must be supplied.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Resets the mock before each test to guarantee a clean state.
     */
    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension already creates a fresh mock, but we reset it explicitly
        // in case additional stubbing is added later.
        reset(mockConverter);
    }

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(Arguments.of(new Timestamp(0L), 0L), // 2021‑06‑01T12:00:00Z
        Arguments.of(new Timestamp(1_622_548_800_000L), 1_622_548_800_000L), Arguments.of(new Timestamp(-123_456_789L), -123_456_789L), Arguments.of(new Timestamp(Long.MAX_VALUE), Long.MAX_VALUE));
    }

    /**
     * Verify that a valid {@link Timestamp} is correctly converted to its epoch millisecond value.
     */
    @Test
    void toLong_returnsTimestampTime() {
        // given
        // 2021‑11‑20 00:00:00 UTC
        long expectedMillis = 1_638_720_000_000L;
        Timestamp ts = new Timestamp(expectedMillis);
        // when
        long actual = TimestampConversions.toLong(ts, mockConverter);
        // then
        assertEquals(expectedMillis, actual, "The long value returned should match Timestamp#getTime()");
        // The converter is not used; verify no interaction.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Ensure that passing {@code null} results in a {@link NullPointerException}
     * because the method performs an unchecked cast.
     */
    @Test
    void toLong_nullInput_throwsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> TimestampConversions.toLong(nullObj, mockConverter), "Calling toLong with a null source should throw NPE");
    }

    /**
     * Ensure that passing an object that is not a {@link Timestamp} results in a
     * {@link ClassCastException}.
     */
    @Test
    void toLong_invalidType_throwsClassCastException() {
        // given
        Object notATimestamp = "2021-11-20";
        // when / then
        assertThrows(ClassCastException.class, () -> TimestampConversions.toLong(notATimestamp, mockConverter), "Calling toLong with a non‑Timestamp should throw ClassCastException");
    }

    /**
     * Parameterised test covering the normal conversion scenarios defined in the
     * JSON test suite: epoch (0), a typical positive epoch, a negative epoch and
     * {@link Long#MAX_VALUE}.
     */
    @ParameterizedTest(name = "[{index}] Timestamp({0}) → {1}")
    @MethodSource("normalConversionProvider")
    void toLong_normalConversions(Timestamp input, long expected) {
        // when
        long actual = TimestampConversions.toLong(input, mockConverter);
        // then
        assertEquals(expected, actual, () -> "Expected Timestamp#getTime() to return " + expected);
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toLong_timestampWithNanos_ignoresNanos() {
        // given
        Timestamp ts = new Timestamp(123_456_789L);
        // non‑zero nanos, should be ignored by getTime()
        ts.setNanos(987_654_321);
        long expected = 123_456_789L;
        // when
        long actual = TimestampConversions.toLong(ts, mockConverter);
        // then
        assertEquals(expected, actual, "getTime() returns only the millisecond part, nanos must be ignored");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toLong_longMinValue_returnsLongMin() {
        // given
        Timestamp ts = new Timestamp(Long.MIN_VALUE);
        long expected = Long.MIN_VALUE;
        // when
        long actual = TimestampConversions.toLong(ts, mockConverter);
        // then
        assertEquals(expected, actual, "Method must correctly return Long.MIN_VALUE milliseconds");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toLong_sqlStyleString_oneSecondAfterEpoch() {
        // given
        Timestamp ts = Timestamp.valueOf("1970-01-01 00:00:01");
        // one second = 1000 ms
        long expected = 1_000L;
        // when
        long actual = TimestampConversions.toLong(ts, mockConverter);
        // then
        assertEquals(expected, actual, "Timestamp created from SQL‑style string should yield 1000 ms");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toLong_nonNullConverter_isIgnored() {
        // given
        Timestamp ts = new Timestamp(0L);
        // mockConverter is already a non‑null mock instance
        // when
        long actual = TimestampConversions.toLong(ts, mockConverter);
        // then
        assertEquals(0L, actual, "Result must be the timestamp's millisecond value");
        // Ensure the method truly never touches the converter.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toLong_subclassThrowsException_propagatesIt() {
        // given: a Timestamp subclass that deliberately throws from getTime()
        Timestamp badTs = new Timestamp(0L) {

            @Override
            public long getTime() {
                throw new IllegalStateException("forced exception");
            }
        };
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> TimestampConversions.toLong(badTs, mockConverter), "Exception from overridden getTime() must propagate unchanged");
        assertEquals("forced exception", ex.getMessage());
        verifyNoInteractions(mockConverter);
    }
}
