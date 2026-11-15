package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toBigInteger_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // a real Converter instance – its behaviour is not exercised because we mock the static
        // conversion methods that would use it.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validConversionProvider() {
        // OffsetDateTime
        OffsetDateTime odt = OffsetDateTime.parse("2023-05-15T10:15:30+02:00");
        Instant odtInstant = odt.toInstant();
        // we will compute manually below
        BigInteger odtExpected = InstantConversions.toBigInteger(odtInstant, null);
        // java.util.Date (epoch millis)
        Date date = new Date(1_680_000_000_000L);
        Instant dateInstant = date.toInstant();
        // Long (epoch millis)
        Long longValue = 1_680_000_123_456L;
        Instant longInstant = Instant.ofEpochMilli(longValue);
        // String ISO‑8601 with offset
        String isoString = "2023-05-15T10:15:30+02:00";
        Instant stringInstant = OffsetDateTime.parse(isoString).toInstant();
        // Instant (already an instant)
        Instant instant = Instant.parse("2023-01-01T00:00:00Z");
        // java.sql.Timestamp
        Timestamp ts = new Timestamp(1_680_000_123_456L);
        Instant tsInstant = ts.toInstant();
        // Helper to compute the expected BigInteger (nanoseconds since epoch)
        java.util.function.Function<Instant, BigInteger> toBigInt = i -> BigInteger.valueOf(i.getEpochSecond()).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(i.getNano()));
        return Stream.of(Arguments.of(odt, toBigInt.apply(odtInstant), "OffsetDateTime with +02:00 offset"), Arguments.of(date, toBigInt.apply(dateInstant), "java.util.Date (epoch‑millis)"), Arguments.of(longValue, toBigInt.apply(longInstant), "Long representing epoch‑millis"), Arguments.of(isoString, toBigInt.apply(stringInstant), "ISO‑8601 string with offset"), Arguments.of(instant, toBigInt.apply(instant), "Instant instance"), Arguments.of(ts, toBigInt.apply(tsInstant), "java.sql.Timestamp (epoch‑millis)"));
    }

    /**
     * Helper that uses reflection to call the package‑private {@code toInstant}
     * method of {@link OffsetDateTimeConversions}.  This is only used to obtain
     * the instant that the real implementation would produce, without re‑implementing
     * the conversion logic.
     */
    private static Instant invokeToInstant(Object from, Converter converter) {
        try {
            var method = OffsetDateTimeConversions.class.getDeclaredMethod("toInstant", Object.class, Converter.class);
            method.setAccessible(true);
            return (Instant) method.invoke(null, from, converter);
        } catch (Exception e) {
            // In the context of these tests the reflective call should never fail.
            throw new RuntimeException(e);
        }
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null source object
        Arguments.of(null, NullPointerException.class, "null source object"), // Null converter
        Arguments.of(Instant.parse("2023-01-01T00:00:00Z"), NullPointerException.class, "null converter"), // Unsupported types
        Arguments.of(Boolean.TRUE, IllegalArgumentException.class, "unsupported Boolean"), Arguments.of(new Object(), IllegalArgumentException.class, "unsupported Object"), Arguments.of(Double.valueOf(12345.67), IllegalArgumentException.class, "unsupported Double"), // Malformed strings
        Arguments.of("not-a-date", DateTimeParseException.class, "non‑parseable string"), Arguments.of("2023-05-15T10:15:30", DateTimeParseException.class, "string without offset"), Arguments.of("2023-13-01T00:00:00+00:00", DateTimeParseException.class, "string with invalid month"));
    }

    @Test
    void testToBigInteger_WithOffsetDateTime_ReturnsExpectedValue() {
        // GIVEN
        OffsetDateTime offsetDateTime = OffsetDateTime.parse("2023-08-15T12:34:56+02:00");
        Instant expectedInstant = offsetDateTime.toInstant();
        BigInteger expectedBigInteger = new BigInteger("12345678901234567890");
        try (MockedStatic<OffsetDateTimeConversions> mockedOffset = mockStatic(OffsetDateTimeConversions.class, CALLS_REAL_METHODS);
            MockedStatic<InstantConversions> mockedInstant = mockStatic(InstantConversions.class)) {
            // stub the internal static calls
            mockedOffset.when(() -> OffsetDateTimeConversions.toInstant(offsetDateTime, converter)).thenReturn(expectedInstant);
            mockedInstant.when(() -> InstantConversions.toBigInteger(expectedInstant, converter)).thenReturn(expectedBigInteger);
            // WHEN
            BigInteger result = OffsetDateTimeConversions.toBigInteger(offsetDateTime, converter);
            // THEN
            assertEquals(expectedBigInteger, result);
            mockedOffset.verify(() -> OffsetDateTimeConversions.toInstant(offsetDateTime, converter));
            mockedInstant.verify(() -> InstantConversions.toBigInteger(expectedInstant, converter));
        }
    }

    @Test
    void testToBigInteger_NullInput_ReturnsNull() {
        // GIVEN
        try (MockedStatic<OffsetDateTimeConversions> mockedOffset = mockStatic(OffsetDateTimeConversions.class, CALLS_REAL_METHODS);
            MockedStatic<InstantConversions> mockedInstant = mockStatic(InstantConversions.class)) {
            mockedOffset.when(() -> OffsetDateTimeConversions.toInstant(null, converter)).thenReturn(null);
            mockedInstant.when(() -> InstantConversions.toBigInteger(null, converter)).thenReturn(null);
            // WHEN
            BigInteger result = OffsetDateTimeConversions.toBigInteger(null, converter);
            // THEN
            assertNull(result);
            mockedOffset.verify(() -> OffsetDateTimeConversions.toInstant(null, converter));
            mockedInstant.verify(() -> InstantConversions.toBigInteger(null, converter));
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void testToBigInteger_ValidConversions(Object from, BigInteger expected, String description) {
        // GIVEN – compute the instant that the real implementation would obtain
        Instant expectedInstant;
        try (MockedStatic<OffsetDateTimeConversions> mockedOffset = mockStatic(OffsetDateTimeConversions.class, CALLS_REAL_METHODS);
            MockedStatic<InstantConversions> mockedInstant = mockStatic(InstantConversions.class)) {
            // Resolve the instant that toInstant would return for the supplied input.
            // We invoke the real private static method via reflection to keep the test
            // independent from the actual conversion logic.
            expectedInstant = invokeToInstant(from, converter);
            // Stub the static calls
            mockedOffset.when(() -> OffsetDateTimeConversions.toInstant(from, converter)).thenReturn(expectedInstant);
            mockedInstant.when(() -> InstantConversions.toBigInteger(expectedInstant, converter)).thenReturn(expected);
            // WHEN
            BigInteger result = OffsetDateTimeConversions.toBigInteger(from, converter);
            // THEN
            assertEquals(expected, result, "Unexpected BigInteger result for: " + description);
            mockedOffset.verify(() -> OffsetDateTimeConversions.toInstant(from, converter));
            mockedInstant.verify(() -> InstantConversions.toBigInteger(expectedInstant, converter));
        }
    }

    @ParameterizedTest(name = "[{index}] Expect {1} for {2}")
    @MethodSource("exceptionProvider")
    void testToBigInteger_ExceptionScenarios(Object from, Class<? extends Throwable> expectedException, String description) {
        // GIVEN – mock toInstant to throw the expected exception
        try (MockedStatic<OffsetDateTimeConversions> mockedOffset = mockStatic(OffsetDateTimeConversions.class, CALLS_REAL_METHODS)) {
            if (expectedException == NullPointerException.class && from == null) {
                // Null source -> toInstant should throw NPE
                mockedOffset.when(() -> OffsetDateTimeConversions.toInstant(null, converter)).thenThrow(new NullPointerException("source is null"));
            } else if (expectedException == NullPointerException.class && converter == null) {
                // Null converter -> we simulate the NPE inside toInstant
                mockedOffset.when(() -> OffsetDateTimeConversions.toInstant(from, null)).thenThrow(new NullPointerException("converter is null"));
            } else if (expectedException == IllegalArgumentException.class) {
                mockedOffset.when(() -> OffsetDateTimeConversions.toInstant(from, converter)).thenThrow(new IllegalArgumentException("unsupported type"));
            } else if (expectedException == DateTimeParseException.class) {
                mockedOffset.when(() -> OffsetDateTimeConversions.toInstant(from, converter)).thenThrow(DateTimeParseException.class.cast(new DateTimeParseException("cannot parse", from.toString(), 0)));
            } else {
                fail("Test case not configured for exception type: " + expectedException);
            }
            // WHEN / THEN
            assertThrows(expectedException, () -> OffsetDateTimeConversions.toBigInteger(from, converter), "Expected exception for case: " + description);
        }
    }

    @Test
    void testToBigInteger_NullInput_ReturnsNullWhenToInstantReturnsNull() {
        // GIVEN – toInstant returns null (instead of throwing)
        try (MockedStatic<OffsetDateTimeConversions> mockedOffset = mockStatic(OffsetDateTimeConversions.class, CALLS_REAL_METHODS);
            MockedStatic<InstantConversions> mockedInstant = mockStatic(InstantConversions.class)) {
            mockedOffset.when(() -> OffsetDateTimeConversions.toInstant(null, converter)).thenReturn(null);
            mockedInstant.when(() -> InstantConversions.toBigInteger(null, converter)).thenReturn(null);
            // WHEN
            BigInteger result = OffsetDateTimeConversions.toBigInteger(null, converter);
            // THEN
            assertNull(result, "Result should be null when underlying Instant is null");
            mockedOffset.verify(() -> OffsetDateTimeConversions.toInstant(null, converter));
            mockedInstant.verify(() -> InstantConversions.toBigInteger(null, converter));
        }
    }
}
