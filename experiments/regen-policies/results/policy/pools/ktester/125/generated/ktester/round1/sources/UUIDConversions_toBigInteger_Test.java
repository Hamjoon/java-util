package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UUIDConversions_toBigInteger_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    /**
     * Simple wrapper whose {@code toString()} returns the supplied value.
     */
    private static final class CustomObject {

        private final String value;

        CustomObject(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * toString() returns {@code null}.
     */
    private static final class NullToStringObject {

        @Override
        public String toString() {
            return null;
        }
    }

    /**
     * toString() throws a {@code RuntimeException}.
     */
    private static final class ExceptionToStringObject {

        @Override
        public String toString() {
            throw new RuntimeException("toString failure");
        }
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// String with hyphens
        Arguments.of("dead-beef", new BigInteger("3735928559")), // String without hyphens
        Arguments.of("deadbeef", new BigInteger("3735928559")), // Integer (its toString() is "255")
        Arguments.of(255, new BigInteger("597")), // Custom object whose toString() returns "c0ffee"
        Arguments.of(new CustomObject("c0ffee"), new BigInteger("12648430")), // Full UUID string with hyphens
        Arguments.of("123e4567-e89b-12d3-a456-426655440000", new BigInteger("24197857161011715162171839636988778104")), // Same UUID without hyphens
        Arguments.of("123e4567e89b12d3a456426655440000", new BigInteger("24197857161011715162171839636988778104")), // Leading hyphen is stripped like any other hyphen
        Arguments.of("-deadbeef", new BigInteger("3735928559")), // Hyphen followed by a digit (sign is ignored)
        Arguments.of("-1", new BigInteger("1")), // Leading zeros are ignored by BigInteger
        Arguments.of("0000dead-beef", new BigInteger("3735928559")), // Upper‑case hex digits
        Arguments.of("DEAD-BEEF", new BigInteger("3735928559")), // StringBuilder (its toString() yields the same string)
        Arguments.of(new StringBuilder("dead-beef"), new BigInteger("3735928559")), // Non‑null Converter argument (ignored)
        // converter will be null in the test call
        Arguments.of(255, new BigInteger("597")));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null source
        Arguments.of(null, NullPointerException.class), // Empty string
        Arguments.of("", NumberFormatException.class), // Single hyphen becomes empty after replace
        Arguments.of("-", NumberFormatException.class), // Non‑hex character 'g'
        Arguments.of("g123", NumberFormatException.class), // Trailing newline (not a hyphen)
        Arguments.of("dead-beef\n", NumberFormatException.class), // Leading/trailing spaces (not stripped)
        Arguments.of("  deadbeef  ", NumberFormatException.class), // Plus sign is not a hyphen and is invalid for base‑16
        Arguments.of("+deadbeef", NumberFormatException.class), // "0x" prefix remains after hyphen removal
        Arguments.of("0xdeadbeef", NumberFormatException.class), // Boolean true -> "true"
        Arguments.of(Boolean.TRUE, NumberFormatException.class), // int[] array default toString() like "[I@..."
        Arguments.of(new int[] { 1, 2, 3 }, NumberFormatException.class), // toString() returns null
        Arguments.of(new NullToStringObject(), NullPointerException.class), // toString() throws RuntimeException
        Arguments.of(new ExceptionToStringObject(), RuntimeException.class));
    }

    @Test
    @DisplayName("toBigInteger converts a UUID string (with hyphens) to the correct BigInteger")
    void testToBigIntegerFromUuidString() {
        // GIVEN
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        // contains hyphens
        String uuidString = uuid.toString();
        // WHEN
        BigInteger result = UUIDConversions.toBigInteger(uuidString, mockConverter);
        // THEN
        String expectedHex = uuidString.replace("-", "");
        BigInteger expected = new BigInteger(expectedHex, 16);
        assertEquals(expected, result, "BigInteger conversion from UUID string is incorrect");
    }

    @Test
    @DisplayName("toBigInteger converts a UUID object to the correct BigInteger")
    void testToBigIntegerFromUuidObject() {
        // GIVEN
        // random UUID
        UUID uuid = UUID.randomUUID();
        // WHEN
        BigInteger result = UUIDConversions.toBigInteger(uuid, mockConverter);
        // THEN
        String expectedHex = uuid.toString().replace("-", "");
        BigInteger expected = new BigInteger(expectedHex, 16);
        assertEquals(expected, result, "BigInteger conversion from UUID object is incorrect");
    }

    @Test
    @DisplayName("toBigInteger throws NullPointerException when source object is null")
    void testToBigIntegerNullSource() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> UUIDConversions.toBigInteger(nullSource, mockConverter), "Expected NullPointerException when source is null");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("validConversionProvider")
    @DisplayName("Valid conversions to BigInteger")
    void testValidConversions(Object from, BigInteger expected) {
        // The method does not use the Converter, so we can safely pass null.
        BigInteger result = UUIDConversions.toBigInteger(from, null);
        assertEquals(expected, result, () -> "Conversion of [" + from + "] did not produce the expected BigInteger");
    }

    @ParameterizedTest(name = "[{index}] from={0} ⇒ {1}")
    @MethodSource("exceptionProvider")
    @DisplayName("Invalid inputs that should throw exceptions")
    void testExceptionScenarios(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> UUIDConversions.toBigInteger(from, null), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }

    @Test
    @DisplayName("Conversion of a real UUID object")
    void testUuidObjectConversion() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        BigInteger result = UUIDConversions.toBigInteger(uuid, mockConverter);
        BigInteger expected = new BigInteger("24197857161011715162171839636988778104");
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Method tolerates a non‑null Converter argument")
    void testWithNonNullConverter() {
        // The mock is not used internally, but we verify the call does not fail.
        BigInteger result = UUIDConversions.toBigInteger("dead-beef", mockConverter);
        assertEquals(new BigInteger("3735928559"), result);
    }
}
