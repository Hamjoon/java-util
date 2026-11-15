package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UUIDConversions_toBigInteger_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Standard UUID string with hyphens
        Arguments.of("123e4567-e89b-12d3-a456-426655440000", new BigInteger("123e4567e89b12d3a456426655440000", 16), "Convert a standard UUID string containing hyphens to a BigInteger."), // UUID object (its toString() yields the hyphenated form)
        Arguments.of(new UUID(0x123e4567e89b12d3L, 0xa456426655440000L), new BigInteger("123e4567e89b12d3a456426655440000", 16), "Convert a UUID object to a BigInteger; should behave like the string case."), // Plain hexadecimal string without hyphens
        Arguments.of("ABCDEF", new BigInteger("ABCDEF", 16), "Convert a plain hexadecimal string without hyphens to a BigInteger."), // String with only leading zeros (and hyphens) – should become 1
        Arguments.of("0000-0000-0000-0000-000000000001", BigInteger.ONE, "String with only leading zeros (and hyphens) should result in the integer value 1."), // Negative hexadecimal string
        Arguments.of("-1A2B", new BigInteger("-1A2B", 16), "Negative hexadecimal string should be parsed as a negative BigInteger."));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Empty string
        Arguments.of("", NumberFormatException.class, "Empty string should cause NumberFormatException because there is no digit to parse."), // Whitespace only
        Arguments.of("   ", NumberFormatException.class, "String containing only whitespace (after hyphen removal) is not a valid hexadecimal representation."), // Non‑hex characters
        Arguments.of("GHIJKL", NumberFormatException.class, "Non‑hex characters cause NumberFormatException."), // Null from argument
        Arguments.of(null, NullPointerException.class, "A null `from` argument triggers NullPointerException when `toString()` is invoked."));
    }

    @Test
    void toBigInteger_fromUuidObject_returnsCorrectBigInteger() {
        // GIVEN
        UUID uuid = UUID.randomUUID();
        String hexWithoutHyphens = uuid.toString().replace("-", "");
        BigInteger expected = new BigInteger(hexWithoutHyphens, 16);
        // WHEN
        BigInteger actual = UUIDConversions.toBigInteger(uuid, mockConverter);
        // THEN
        assertEquals(expected, actual, "Conversion from UUID object should produce the same BigInteger as parsing its hex representation");
    }

    @Test
    void toBigInteger_fromUuidStringWithHyphens_returnsCorrectBigInteger() {
        // GIVEN
        UUID uuid = UUID.randomUUID();
        // contains hyphens
        String uuidString = uuid.toString();
        String hexWithoutHyphens = uuidString.replace("-", "");
        BigInteger expected = new BigInteger(hexWithoutHyphens, 16);
        // WHEN
        BigInteger actual = UUIDConversions.toBigInteger(uuidString, mockConverter);
        // THEN
        assertEquals(expected, actual, "Conversion from UUID string (with hyphens) should ignore hyphens and produce the correct BigInteger");
    }

    @Test
    void toBigInteger_fromUuidStringWithoutHyphens_returnsCorrectBigInteger() {
        // GIVEN
        UUID uuid = UUID.randomUUID();
        // already hyphen‑free
        String uuidStringNoHyphens = uuid.toString().replace("-", "");
        BigInteger expected = new BigInteger(uuidStringNoHyphens, 16);
        // WHEN
        BigInteger actual = UUIDConversions.toBigInteger(uuidStringNoHyphens, mockConverter);
        // THEN
        assertEquals(expected, actual, "Conversion from a hyphen‑free UUID string should produce the correct BigInteger");
    }

    @Test
    void toBigInteger_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> UUIDConversions.toBigInteger(nullInput, mockConverter), "Passing a null source should result in NullPointerException because the method calls toString()");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void toBigInteger_validInputs(Object from, BigInteger expected, String description) {
        // WHEN
        BigInteger actual = UUIDConversions.toBigInteger(from, mockConverter);
        // THEN
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidInputProvider")
    void toBigInteger_invalidInputs(Object from, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> UUIDConversions.toBigInteger(from, mockConverter), description);
    }

    @Test
    void toBigInteger_toStringReturnsNull_throwsNullPointerException() {
        // GIVEN: an object whose toString() returns null
        Object obj = new Object() {

            @Override
            public String toString() {
                return null;
            }
        };
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> UUIDConversions.toBigInteger(obj, mockConverter), "When toString() returns null, replace() is invoked on a null reference causing NPE.");
    }

    @Test
    void toBigInteger_toStringThrowsException_propagatesException() {
        // GIVEN: an object whose toString() throws an IllegalStateException
        Object obj = new Object() {

            @Override
            public String toString() {
                throw new IllegalStateException("toString failure");
            }
        };
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> UUIDConversions.toBigInteger(obj, mockConverter), "Exceptions thrown by toString() should propagate unchanged.");
        assertEquals("toString failure", ex.getMessage());
    }
}
