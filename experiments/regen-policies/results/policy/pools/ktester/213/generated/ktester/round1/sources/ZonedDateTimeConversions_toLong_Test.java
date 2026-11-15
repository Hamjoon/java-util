package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toLong_Test {

    /**
     * A real Converter instance – the method under test does not use it, but it must be non‑null.
     */
    private static Converter realConverter;

    /**
     * A mock Converter used to verify that the method does not interact with the Converter.
     */
    @Mock
    private Converter mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; using the default constructor.
        realConverter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> provideValidConversions() {
        return Stream.of(// Normal conversion – positive epoch
        Arguments.of("Standard UTC ZonedDateTime (2023‑05‑10T12:30:45Z)", ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 0, ZoneId.of("UTC")), realConverter, 1_683_714_645_000L), Arguments.of("Epoch start (1970‑01‑01T00:00:00Z)", ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), realConverter, 0L), Arguments.of("One second before epoch (1969‑12‑31T23:59:59Z)", ZonedDateTime.of(1969, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC")), realConverter, -1_000L), // Non‑UTC zone conversion
        Arguments.of("America/New_York zone (2023‑05‑10T12:30:45‑04:00)", ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 0, ZoneId.of("America/New_York")), null, 1_683_729_045_000L), // Daylight‑Saving‑Time spring‑forward transition – before jump
        Arguments.of("DST pre‑jump (2023‑03‑12T01:30:00‑05:00)", ZonedDateTime.of(2023, 3, 12, 1, 30, 0, 0, ZoneId.of("America/New_York")), null, 1_678_602_600_000L), // DST after jump
        Arguments.of("DST post‑jump (2023‑03‑12T03:30:00‑04:00)", ZonedDateTime.of(2023, 3, 12, 3, 30, 0, 0, ZoneId.of("America/New_York")), null, 1_678_606_200_000L), // Extreme future epoch (Long.MAX_VALUE)
        Arguments.of("Maximum long epoch millis", ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.MAX_VALUE), ZoneOffset.UTC), null, Long.MAX_VALUE), // Extreme past epoch (Long.MIN_VALUE)
        Arguments.of("Minimum long epoch millis", ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.MIN_VALUE), ZoneOffset.UTC), null, Long.MIN_VALUE), // Non‑null Converter argument (ignored)
        Arguments.of("Non‑null custom Converter (ignored)", ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 0, ZoneId.of("UTC")), new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                // dummy implementation
                return src;
            }
        }, 1_683_714_645_000L));
    }

    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(// Null source object
        Arguments.of("Null source argument should throw NullPointerException", null, null, NullPointerException.class), // Invalid source type (String)
        Arguments.of("String source should throw ClassCastException", "2023-05-10T12:30:45Z", null, ClassCastException.class), // Instant overflow beyond Long.MAX_VALUE
        Arguments.of("Instant > Long.MAX_VALUE milliseconds should throw DateTimeException", ZonedDateTime.ofInstant(Instant.ofEpochSecond(Long.MAX_VALUE / 1000 + 1), ZoneOffset.UTC), null, DateTimeException.class), // Instant underflow beyond Long.MIN_VALUE
        Arguments.of("Instant < Long.MIN_VALUE milliseconds should throw DateTimeException", ZonedDateTime.ofInstant(Instant.ofEpochSecond(Long.MIN_VALUE / 1000 - 1), ZoneOffset.UTC), null, DateTimeException.class));
    }

    @Test
    void toLong_convertsZonedDateTimeToEpochMilli() {
        // given
        ZonedDateTime zdt = ZonedDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        15, // hour, minute, second, nano
        12, // hour, minute, second, nano
        30, // hour, minute, second, nano
        45, // hour, minute, second, nano
        123_000_000, // zone
        ZoneId.of("UTC"));
        long expected = zdt.toInstant().toEpochMilli();
        // when
        long actual = ZonedDateTimeConversions.toLong(zdt, realConverter);
        // then
        assertEquals(expected, actual, "ZonedDateTimeConversions.toLong should return the epoch‑millis of the supplied ZonedDateTime");
    }

    @Test
    void toLong_usesNoConverterInteraction() {
        // given
        ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of("UTC"));
        // when
        long result = ZonedDateTimeConversions.toLong(zdt, mockConverter);
        // then
        assertEquals(zdt.toInstant().toEpochMilli(), result);
        // Verify that the method never calls any method on the Converter instance.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toLong_nullInput_throwsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toLong(nullObj, realConverter), "Passing null as the source should result in a NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideValidConversions")
    void toLong_returnsExpectedEpochMilli(String description, ZonedDateTime from, Converter converter, long expected) {
        long actual = ZonedDateTimeConversions.toLong(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideExceptionCases")
    void toLong_throwsExpectedException(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toLong(from, converter), description);
    }
}
