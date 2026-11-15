package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class DateConversions_toBigInteger_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter has a public constructor that requires a ConverterOptions instance.
        // Using the default options is sufficient for the built‑in conversions we need.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Helper that mirrors the logic used by {@code InstantConversions.toBigInteger}
     * (epoch‑seconds * 1_000_000_000 + nanos).
     */
    private static BigInteger instantToBigInteger(Instant instant) {
        if (instant == null) {
            return null;
        }
        return BigInteger.valueOf(instant.getEpochSecond()).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(instant.getNano()));
    }

    private static Stream<Arguments> provideInstantsForBigInteger() {
        return Stream.of(arguments("Instant representing the epoch (0 seconds) should be converted to a BigInteger value of 0", Instant.ofEpochSecond(0, 0), instantToBigInteger(Instant.ofEpochSecond(0, 0))), arguments("Instant with 1 second and 500 ms nanoseconds should be converted to 1_500_000_000 nanoseconds", Instant.ofEpochSecond(1, 500_000_000), instantToBigInteger(Instant.ofEpochSecond(1, 500_000_000))));
    }

    private static Stream<Arguments> provideUnsupportedTypes() {
        return Stream.of(arguments("Providing an empty List should result in IllegalArgumentException", new ArrayList<>()), arguments("Providing a Boolean value should result in IllegalArgumentException", Boolean.TRUE));
    }

    private static Stream<Arguments> provideMalformedStrings() {
        return Stream.of(arguments("A string with an invalid ISO‑8601 instant should cause DateTimeParseException", "2023-13-40T25:61:61Z", DateTimeParseException.class), arguments("A non‑parseable string should result in IllegalArgumentException", "not-a-date", IllegalArgumentException.class));
    }

    @Test
    void toBigInteger_fromInstant_returnsCorrectBigInteger() {
        // given
        Instant now = Instant.now();
        // when
        BigInteger result = DateConversions.toBigInteger(now, converter);
        // then
        assertEquals(instantToBigInteger(now), result, "Conversion from Instant should match the Instant → BigInteger algorithm");
    }

    @Test
    void toBigInteger_fromDate_returnsCorrectBigInteger() {
        // given
        // current time in milliseconds
        Date date = new Date();
        Instant expectedInstant = date.toInstant();
        // when
        BigInteger result = DateConversions.toBigInteger(date, converter);
        // then
        assertEquals(instantToBigInteger(expectedInstant), result, "Conversion from java.util.Date should use the underlying Instant representation");
    }

    @Test
    void toBigInteger_fromStringRepresentingMillis_returnsCorrectBigInteger() {
        // given
        long millis = System.currentTimeMillis();
        String source = Long.toString(millis);
        Instant expectedInstant = Instant.ofEpochMilli(millis);
        // when
        BigInteger result = DateConversions.toBigInteger(source, converter);
        // then
        assertEquals(instantToBigInteger(expectedInstant), result, "String containing epoch milliseconds should be converted to the same BigInteger as a Date");
    }

    @Test
    void toBigInteger_fromNull_returnsNull() {
        // given
        Object source = null;
        // when
        BigInteger result = DateConversions.toBigInteger(source, converter);
        // then
        assertNull(result, "Null source should result in a null BigInteger");
    }

    /**
     * Group: toBigInteger_fromInstant **
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideInstantsForBigInteger")
    void toBigInteger_fromInstant_parameterised(String description, Instant instant, BigInteger expected) {
        // when
        BigInteger actual = DateConversions.toBigInteger(instant, converter);
        // then
        assertEquals(expected, actual, description);
    }

    /**
     * Group: toBigInteger_nullFrom **
     */
    @Test
    void toBigInteger_nullFrom_throwsNullPointerException() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> DateConversions.toBigInteger(from, converter), "Passing a null 'from' argument should cause a NullPointerException");
    }

    /**
     * Group: toBigInteger_nullConverter **
     */
    @Test
    void toBigInteger_nullConverter_throwsNullPointerException() {
        // given
        Instant instant = Instant.ofEpochSecond(0, 0);
        Converter nullConverter = null;
        // when / then
        assertThrows(NullPointerException.class, () -> DateConversions.toBigInteger(instant, nullConverter), "Passing a null Converter should cause a NullPointerException");
    }

    /**
     * Group: toBigInteger_unsupportedType **
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideUnsupportedTypes")
    void toBigInteger_unsupportedType_throwsIllegalArgumentException(String description, Object unsupported) {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toBigInteger(unsupported, converter), description);
    }

    /**
     * Group: toBigInteger_malformedString **
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideMalformedStrings")
    void toBigInteger_malformedString_throwsException(String description, String malformed, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> DateConversions.toBigInteger(malformed, converter), description);
    }
}
