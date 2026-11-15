package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UUIDConversions_toBigInteger_Test {

    @Mock
    private Converter // Converter is not used by the method but must be supplied
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock for us via the @Mock annotation
        // Ensure the mock is clean before each test
        reset(mockConverter);
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Standard UUID string with hyphens
        Arguments.of("123e4567-e89b-12d3-a456-426655440000", "24197857161011715162171839636988778112"), // Same UUID without hyphens
        Arguments.of("123e4567e89b12d3a456426655440000", "24197857161011715162171839636988778112"), // Upper‑case UUID with hyphens
        Arguments.of("ABCDEF12-3456-7890-ABCD-EF1234567890", "226854911280625539646413048519949160720"), // Simple two‑digit hex
        Arguments.of("ff", "255"), // Zero value
        Arguments.of("0", "0"), // Negative sign handling – hyphen is stripped
        Arguments.of("-ff", "255"), // Non‑string object: a UUID instance
        Arguments.of(UUID.fromString("123e4567-e89b-12d3-a456-426655440000"), "24197857161011715162171839636988778112"), // Large 64‑digit hex (2^256‑1)
        Arguments.of("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", "115792089237316195423570985008687907853269984665640564039457584007913129639935"), // Odd length hex string
        Arguments.of("abc", "2748"));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null input
        Arguments.of(null, NullPointerException.class), // Empty string
        Arguments.of("", NumberFormatException.class), // Invalid characters
        Arguments.of("zzzz", NumberFormatException.class), // Whitespace handling – leading space
        Arguments.of(" ff ", NumberFormatException.class), // Object whose toString() returns null
        Arguments.of(new Object() {

            @Override
            public String toString() {
                return null;
            }
        }, NullPointerException.class), // Plus sign handling – not accepted by BigInteger(String, radix)
        Arguments.of("+ff", NumberFormatException.class), // Only hyphens string – becomes empty after replace
        Arguments.of("---", NumberFormatException.class), // Newline whitespace handling
        Arguments.of("\nff", NumberFormatException.class), // Non‑hex numeric object (Integer)
        Arguments.of(12345, NumberFormatException.class), // Array object toString (default Java array representation)
        Arguments.of(new int[] { 1, 2, 3 }, NumberFormatException.class));
    }

    @Test
    void toBigInteger_fromUuidString_withHyphens() {
        // GIVEN
        UUID uuid = UUID.randomUUID();
        // contains hyphens
        String uuidString = uuid.toString();
        String hexWithoutHyphens = uuidString.replace("-", "");
        BigInteger expected = new BigInteger(hexWithoutHyphens, 16);
        // WHEN
        BigInteger actual = UUIDConversions.toBigInteger(uuidString, mockConverter);
        // THEN
        assertEquals(expected, actual, "BigInteger conversion from UUID string with hyphens failed");
    }

    @Test
    void toBigInteger_fromUuidObject() {
        // GIVEN
        // UUID's toString() yields the same format
        UUID uuid = UUID.randomUUID();
        String hexWithoutHyphens = uuid.toString().replace("-", "");
        BigInteger expected = new BigInteger(hexWithoutHyphens, 16);
        // WHEN
        BigInteger actual = UUIDConversions.toBigInteger(uuid, mockConverter);
        // THEN
        assertEquals(expected, actual, "BigInteger conversion from UUID object failed");
    }

    @Test
    void toBigInteger_fromHexString_withoutHyphens() {
        // GIVEN
        // 32‑hex‑digit string, no hyphens
        String hex = "0123456789abcdef0123456789abcdef";
        BigInteger expected = new BigInteger(hex, 16);
        // WHEN
        BigInteger actual = UUIDConversions.toBigInteger(hex, mockConverter);
        // THEN
        assertEquals(expected, actual, "BigInteger conversion from plain hex string failed");
    }

    @Test
    void toBigInteger_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> UUIDConversions.toBigInteger(nullObj, mockConverter), "Expected NPE when source object is null");
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for the "Valid conversions" group
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from=''{0}'' → expected={1}")
    @MethodSource("validConversionProvider")
    void toBigInteger_validConversions(Object from, String expectedDecimal) {
        // GIVEN
        BigInteger expected = new BigInteger(expectedDecimal);
        // WHEN
        BigInteger actual = UUIDConversions.toBigInteger(from, mockConverter);
        // THEN
        assertEquals(expected, actual, () -> "Conversion of " + from + " did not produce expected BigInteger");
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for exception‑producing cases
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from=''{0}'' expects {1}")
    @MethodSource("exceptionProvider")
    void toBigInteger_exceptions(Object from, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> UUIDConversions.toBigInteger(from, mockConverter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }

    /* -------------------------------------------------------------
     *  Additional explicit tests for clarity (optional)
     * ------------------------------------------------------------- */
    @Test
    void toBigInteger_fromUuidObjectExplicit() {
        // GIVEN
        UUID uuid = UUID.randomUUID();
        String hex = uuid.toString().replace("-", "");
        BigInteger expected = new BigInteger(hex, 16);
        // WHEN
        BigInteger actual = UUIDConversions.toBigInteger(uuid, mockConverter);
        // THEN
        assertEquals(expected, actual, "Conversion of UUID object failed");
    }
}
