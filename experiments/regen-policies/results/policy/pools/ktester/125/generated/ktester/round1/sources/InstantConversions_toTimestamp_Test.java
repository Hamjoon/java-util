package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DateTimeException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toTimestamp_Test {

    @Mock
    private Converter // not used by the method but required for signature
    mockConverter;

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(Arguments.of("Convert the epoch‑zero Instant to a Timestamp; result should be the Timestamp representing 1970‑01‑01 00:00:00.0 UTC.", Instant.ofEpochMilli(0)), Arguments.of("Convert a non‑trivial Instant (including nanoseconds) to a Timestamp; the returned Timestamp must contain the same epoch seconds and nanosecond fraction as the source Instant.", Instant.parse("2023-01-01T12:34:56.123456789Z")), Arguments.of("Instant at the highest year that java.sql.Timestamp can represent (292278994‑01‑01T00:00:00Z). Conversion should succeed and preserve epoch seconds.", Instant.ofEpochSecond(292278994L, 0)), Arguments.of("Instant with the maximum nanosecond fraction (just under one second). Conversion should succeed and the resulting Timestamp must retain the 999,999,999‑nanosecond component.", Instant.ofEpochSecond(0, 999_999_999L)));
    }

    private static Stream<Arguments> dateTimeExceptionProvider() {
        return Stream.of(Arguments.of("Instant.MAX is outside the range supported by java.sql.Timestamp; the method should propagate the DateTimeException thrown by Timestamp.from.", Instant.MAX), Arguments.of("Instant.MIN is before the epoch and lies outside the range supported by java.sql.Timestamp; Timestamp.from should throw DateTimeException.", Instant.MIN), Arguments.of("An Instant exactly one second before the epoch (1970‑01‑01T00:00:00Z) is not representable by Timestamp; expect DateTimeException.", Instant.ofEpochSecond(-1)), Arguments.of("Instant created from Long.MAX_VALUE milliseconds exceeds Timestamp's range; Timestamp.from must throw DateTimeException.", Instant.ofEpochMilli(Long.MAX_VALUE)));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of("Passing a null source object results in a NullPointerException when Timestamp.from receives a null Instant.", null, NullPointerException.class), Arguments.of("A source object that is not an Instant (e.g., java.util.Date) causes a ClassCastException during the cast to Instant.", new java.util.Date(), ClassCastException.class), Arguments.of("A String representation of an instant cannot be cast to Instant, so a ClassCastException is expected.", "2023-01-01T00:00:00Z", ClassCastException.class), Arguments.of("An Integer cannot be cast to java.time.Instant, so a ClassCastException is expected.", Integer.valueOf(42), ClassCastException.class), Arguments.of("java.sql.Timestamp is not an Instant; casting it to Instant triggers a ClassCastException.", new java.sql.Timestamp(System.currentTimeMillis()), ClassCastException.class), Arguments.of("LocalDateTime cannot be cast to Instant, resulting in a ClassCastException.", java.time.LocalDateTime.now(), ClassCastException.class));
    }

    @Test
    void toTimestamp_WithInstant_ReturnsCorrespondingTimestamp() {
        // given
        Instant now = Instant.now();
        // when
        Timestamp ts = InstantConversions.toTimestamp(now, mockConverter);
        // then
        assertNotNull(ts, "Timestamp should not be null");
        assertEquals(Timestamp.from(now), ts, "Timestamp should represent the same instant");
    }

    @Test
    void toTimestamp_WithNull_ThrowsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> InstantConversions.toTimestamp(nullObj, mockConverter), "Calling toTimestamp with null should throw NPE");
    }

    @Test
    void toTimestamp_WithInvalidType_ThrowsClassCastException() {
        // given
        Object notAnInstant = "2023-01-01T00:00:00Z";
        // when / then
        assertThrows(ClassCastException.class, () -> InstantConversions.toTimestamp(notAnInstant, mockConverter), "Calling toTimestamp with a non‑Instant object should throw CCE");
    }

    /**
     * Valid conversions – the method should return a {@link Timestamp}
     * that is exactly {@link Timestamp#from(Instant)}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toTimestamp_ValidConversions(String description, Instant input) {
        Timestamp result = InstantConversions.toTimestamp(input, mockConverter);
        assertEquals(Timestamp.from(input), result, () -> "Expected Timestamp.from(input) for case: " + description);
    }

    /**
     * Cases that must result in a {@link DateTimeException}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("dateTimeExceptionProvider")
    void toTimestamp_DateTimeExceptionCases(String description, Instant input) {
        assertThrows(DateTimeException.class, () -> InstantConversions.toTimestamp(input, mockConverter), () -> "Expected DateTimeException for case: " + description);
    }

    /**
     * Invalid input handling – null or non‑Instant objects should cause the
     * appropriate unchecked exception.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidInputProvider")
    void toTimestamp_InvalidInputs(String description, Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> InstantConversions.toTimestamp(input, mockConverter), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }

    /**
     * Verify that the {@code Converter} argument is truly ignored.
     * The supplied converter throws if its {@code convert} method is called.
     */
    @Test
    void toTimestamp_ConverterArgumentIgnored() {
        // given: a converter that would explode if invoked
        Converter explodingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Converter should not be used");
            }
        };
        Instant now = Instant.now();
        // when
        Timestamp ts = InstantConversions.toTimestamp(now, explodingConverter);
        // then
        assertEquals(Timestamp.from(now), ts, "Method must ignore the Converter argument and correctly convert the Instant.");
    }
}
