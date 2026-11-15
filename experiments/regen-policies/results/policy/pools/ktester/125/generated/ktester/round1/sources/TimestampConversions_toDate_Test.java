package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toDate_Test {

    /**
     * A mock Converter – the implementation of {@code toDate} does not use it,
     *  but the signature requires it.
     */
    @Mock
    private Converter mockConverter;

    /* --------------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final Timestamp timestamp;

        final long expectedEpochMilli;

        final String description;

        ArgumentsHolder(Timestamp timestamp, long expectedEpochMilli, String description) {
            this.timestamp = timestamp;
            this.expectedEpochMilli = expectedEpochMilli;
            this.description = description;
        }

        // JUnit 5 requires a method returning a single argument for @MethodSource
        @Override
        public String toString() {
            return description;
        }
    }

    private static Stream<ArgumentsHolder> edgeCaseProvider() {
        return Stream.of(// negative epoch (pre‑1970)
        new ArgumentsHolder(new Timestamp(-123_456_789L), -123_456_789L, "Negative epoch timestamp should be handled correctly"), // maximum long value
        new ArgumentsHolder(new Timestamp(Long.MAX_VALUE), Long.MAX_VALUE, "Maximum long epoch millis should be converted without overflow"), // subclass that overrides toInstant() to add one hour
        new ArgumentsHolder(new Timestamp(1_622_548_800_123L) {

            @Override
            public Instant toInstant() {
                // original instant + 1 hour
                return Instant.ofEpochMilli(super.getTime()).plusSeconds(3600);
            }
        }, 1_622_548_800_123L + 3_600_000L, "Subclass overriding toInstant() must be respected"), // subclass that throws if converter is used (method must ignore it)
        new ArgumentsHolder(Timestamp.valueOf("2020-01-01 00:00:00.000"), 1_577_836_800_000L, "Method must ignore a converter that would otherwise throw"), // Timestamp created from Instant via modern API
        new ArgumentsHolder(Timestamp.from(Instant.parse("2023-03-15T12:34:56.789Z")), 1_678_889_696_789L, "Timestamp.from(Instant) should be handled correctly"));
    }

    @Test
    void toDate_convertsTimestampToDate_correctly() {
        // given
        // a deterministic instant
        long epochMilli = 1_623_456_789_012L;
        Instant instant = Instant.ofEpochMilli(epochMilli);
        Timestamp timestamp = Timestamp.from(instant);
        // when
        Date result = TimestampConversions.toDate(timestamp, mockConverter);
        // then
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(instant, result.toInstant(), "The instant of the Date must match the original Timestamp");
        assertEquals(timestamp.getTime(), result.getTime(), "Epoch millis must be identical");
    }

    @Test
    void toDate_throwsClassCastException_whenSourceIsNotTimestamp() {
        // given
        Object notATimestamp = new Object();
        // when / then
        assertThrows(ClassCastException.class, () -> TimestampConversions.toDate(notATimestamp, mockConverter), "A non‑Timestamp source should cause a ClassCastException");
    }

    @Test
    void toDate_throwsNullPointerException_whenSourceIsNull() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> TimestampConversions.toDate(nullSource, mockConverter), "Passing null as the source should result in a NullPointerException");
    }

    @Test
    void toDate_usesProvidedConverterButIgnoresIt() {
        // given
        Timestamp ts = new Timestamp(1622548800123L);
        Converter realConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new IllegalStateException("Should not be called");
            }
        };
        // when
        Date result = TimestampConversions.toDate(ts, realConverter);
        // then
        assertEquals(1622548800123L, result.getTime(), "Conversion must ignore the converter and return correct epoch millis");
    }

    @Test
    void toDate_preservesMillisecondPrecisionFromNanosecondTimestamp() {
        // given a Timestamp with nanosecond part (nanoseconds are truncated by Date)
        Timestamp ts = Timestamp.valueOf("2023-03-15 12:34:56.789123456");
        // truncates to milliseconds
        long expectedMillis = ts.getTime();
        // when
        Date result = TimestampConversions.toDate(ts, mockConverter);
        // then
        assertEquals(expectedMillis, result.getTime(), "Date should contain the millisecond part of the original Timestamp");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("edgeCaseProvider")
    void toDate_edgeCases(ArgumentsHolder holder) {
        // given
        Timestamp ts = holder.timestamp;
        Converter converter = holder.description.contains("converter") ? new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new IllegalStateException("Converter should not be invoked");
            }
        } : mockConverter;
        // when
        Date result = TimestampConversions.toDate(ts, converter);
        // then
        assertEquals(holder.expectedEpochMilli, result.getTime(), holder.description);
    }

    @Test
    void toDate_throwsNullPointerException_whenTimestampToInstantReturnsNull() {
        // given a subclass that returns null from toInstant()
        Timestamp ts = new Timestamp(1_622_548_800_123L) {

            @Override
            public Instant toInstant() {
                return null;
            }
        };
        // when / then
        assertThrows(NullPointerException.class, () -> TimestampConversions.toDate(ts, mockConverter), "Date.from(null) should throw NullPointerException");
    }

    @Test
    void toDate_throwsDateTimeException_whenInstantOverflowOccurs() {
        // given a subclass that returns an instant whose epoch‑millis > Long.MAX_VALUE
        Timestamp ts = new Timestamp(0L) {

            @Override
            public Instant toInstant() {
                // epoch seconds close to Long.MAX_VALUE/1000, plus one second to overflow
                long overflowSeconds = Long.MAX_VALUE / 1000 + 1;
                return Instant.ofEpochSecond(overflowSeconds);
            }
        };
        // when / then
        assertThrows(DateTimeException.class, () -> TimestampConversions.toDate(ts, mockConverter), "Date.from(Instant) should throw DateTimeException on overflow");
    }
}
