package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toDate_Test {

    /**
     * Converter is required by the signature of {@code TimestampConversions.toDate}.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter is cheap to construct; we use the default options.
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper that creates a {@link Timestamp} from a ISO‑date string and time components.
     */
    private static Timestamp createTimestamp(String isoDate, int hour, int minute, int second, int nanos) {
        return Timestamp.from(java.time.LocalDate.parse(isoDate).atTime(hour, minute, second, nanos).atZone(java.time.ZoneId.systemDefault()).toInstant());
    }

    /**
     * Parameter source for the “TimestampConversions.toDate tests” group.
     */
    private static Stream<Arguments> basicConversionCases() {
        return Stream.of(// epoch (0 L)
        Arguments.of(new Timestamp(0L), null, Date.from(Instant.ofEpochMilli(0L)), null), // positive epoch 1_600_000_000_000 L → 2020‑09‑13T12:26:40Z
        Arguments.of(new Timestamp(1_600_000_000_000L), null, Date.from(Instant.ofEpochMilli(1_600_000_000_000L)), null), // negative epoch –1_000 L → 1969‑12‑31T23:59:59.999Z
        Arguments.of(new Timestamp(-1_000L), null, Date.from(Instant.ofEpochMilli(-1_000L)), null), // null source → NPE
        Arguments.of(null, null, null, NullPointerException.class), // wrong type (java.util.Date) → ClassCastException
        Arguments.of((Timestamp) (Object) new java.util.Date(), null, null, ClassCastException.class));
    }

    /**
     * Parameter source for the “TimestampConversions.toDate edge cases” group.
     */
    private static Stream<Arguments> edgeConversionCases() {
        // 1. Timestamp with explicit nanoseconds (sub‑millisecond part must be dropped)
        Timestamp tsWithNanos = new Timestamp(1_600_000_000_123L);
        // 0.123456789 s, but Date only keeps ms
        tsWithNanos.setNanos(123_456_789);
        Date expectedNanos = Date.from(Instant.ofEpochMilli(1_600_000_000_123L));
        // 2. Non‑null Converter that must not be called
        Converter spyingConverter = mock(Converter.class);
        // No stubbing – if the method tries to call it, the mock will record an invocation.
        // 3. Long.MAX_VALUE milliseconds – should overflow Instant and cause DateTimeException
        Timestamp tsMax = new Timestamp(Long.MAX_VALUE);
        // 4. Long.MIN_VALUE milliseconds – same reasoning
        Timestamp tsMin = new Timestamp(Long.MIN_VALUE);
        // 5. Epoch with half‑second nanoseconds – should truncate to 0 ms
        Timestamp tsHalfSecond = new Timestamp(0L);
        tsHalfSecond.setNanos(500_000_000);
        Date expectedHalfSecond = Date.from(Instant.ofEpochMilli(0L));
        return Stream.of(// sub‑millisecond nanoseconds
        Arguments.of(tsWithNanos, null, expectedNanos, null), // converter not used
        Arguments.of(new Timestamp(0L), spyingConverter, Date.from(Instant.ofEpochMilli(0L)), null), // overflow – Long.MAX_VALUE
        Arguments.of(tsMax, null, null, DateTimeException.class), // underflow – Long.MIN_VALUE
        Arguments.of(tsMin, null, null, DateTimeException.class), // epoch with 0.5 s nanos
        Arguments.of(tsHalfSecond, null, expectedHalfSecond, null));
    }

    @Test
    void testToDateReturnsCorrectDate() {
        Timestamp ts = createTimestamp("2023-07-15", 13, 45, 30, 123_456_789);
        Date result = TimestampConversions.toDate(ts, converter);
        // The expected value is simply the Date representation of the same instant.
        Date expected = Date.from(ts.toInstant());
        assertEquals(expected, result, "TimestampConversions.toDate should return a Date equal to the instant of the source Timestamp");
    }

    @Test
    void testToDateWithNullSourceThrowsNPE() {
        // The method casts the argument to Timestamp, so a null argument results in NPE.
        assertThrows(NullPointerException.class, () -> TimestampConversions.toDate(null, converter), "Calling toDate with a null source should throw NullPointerException");
    }

    @Test
    void testToDateWithInvalidSourceThrowsClassCastException() {
        // Passing an object that is not a Timestamp must cause a ClassCastException.
        Object notATimestamp = "2023-01-01";
        assertThrows(ClassCastException.class, () -> TimestampConversions.toDate(notATimestamp, converter), "Calling toDate with a non‑Timestamp source should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("basicConversionCases")
    void testBasicConversions(Timestamp from, Converter conv, Date expected, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> TimestampConversions.toDate(from, conv), "Expected exception for input: " + from);
        } else {
            // The method does not use the Converter, so we can safely pass null.
            Date result = TimestampConversions.toDate(from, conv);
            assertEquals(expected, result, "Conversion result does not match expected Date for input: " + from);
        }
    }

    @ParameterizedTest(name = "[{index}] edge case – {3}")
    @MethodSource("edgeConversionCases")
    void testEdgeCases(Timestamp from, Converter conv, Date expected, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> TimestampConversions.toDate(from, conv), "Expected exception for edge case input: " + from);
        } else {
            Date result = TimestampConversions.toDate(from, conv);
            assertEquals(expected, result, "Edge‑case conversion result does not match expected Date for input: " + from);
            // Verify that the supplied converter (if any) was never invoked.
            if (conv != null) {
                verifyNoInteractions(conv);
            }
        }
    }
}
