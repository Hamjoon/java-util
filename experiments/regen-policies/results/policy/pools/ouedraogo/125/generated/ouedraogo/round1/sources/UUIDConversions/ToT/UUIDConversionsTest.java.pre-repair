package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link UUIDConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal conversion of a UUID string to {@code BigInteger} and {@code BigDecimal}</li>
 *   <li>Case‑insensitivity and hyphen handling</li>
 *   <li>Failure when the source string contains non‑hex characters</li>
 *   <li>Conversion of a {@code UUID} instance to a {@code Map}</li>
 *   <li>Null handling for the {@code toMap} method (expected NPE)</li>
 * </ul>
 *
 * The {@code Converter} argument required by the static methods is not used by the
 * implementation, therefore a minimal instance created with a {@code null}
 * {@code ConverterOptions} is sufficient.
 */
public class UUIDConversionsTest {

    /** A dummy {@code Converter} instance – the implementation under test never calls any method on it. */
    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter constructor accepts a ConverterOptions instance; passing null is allowed for the tests.
        dummyConverter = new Converter(null);
    }

    /**
     * Verify that a well‑formed UUID string is correctly converted to a {@code BigInteger}.
     * The expected value is computed by removing the hyphens and parsing the resulting
     * hexadecimal string.
     */
    @Test
    public void testToBigInteger_ValidUuid() {
        String uuidString = "123e4567-e89b-12d3-a456-426655440000";
        UUID uuid = UUID.fromString(uuidString);

        // Expected: hex representation without hyphens
        String expectedHex = uuidString.replace("-", "");
        BigInteger expected = new BigInteger(expectedHex, 16);

        BigInteger actual = UUIDConversions.toBigInteger(uuid, dummyConverter);
        assertEquals("BigInteger conversion did not match expected value", expected, actual);
    }

    /**
     * The conversion must be case‑insensitive.  UUID strings may contain upper‑case
     * hexadecimal digits; the implementation should treat them the same as lower‑case.
     */
    @Test
    public void testToBigInteger_CaseInsensitive() {
        String uuidString = "ABCDEFAB-1234-5678-9ABC-DEF012345678";
        UUID uuid = UUID.fromString(uuidString.toLowerCase()); // Java accepts lower‑case only
        String expectedHex = uuidString.replace("-", "").toLowerCase(); // hex parser is case‑insensitive

        BigInteger expected = new BigInteger(expectedHex, 16);
        BigInteger actual = UUIDConversions.toBigInteger(uuid, dummyConverter);
        assertEquals("Conversion should ignore case of hex digits", expected, actual);
    }

    /**
     * When the source object's {@code toString()} contains characters that are not
     * valid hexadecimal digits, {@code BigInteger(String, int)} throws
     * {@link NumberFormatException}.  This behaviour is exercised here.
     */
    @Test(expected = NumberFormatException.class)
    public void testToBigInteger_InvalidHexCharacters() {
        // An object whose toString() yields a non‑hex string.
        Object badSource = new Object() {
            @Override
            public String toString() {
                return "GGGG-XXXX-YYYY-ZZZZ";
            }
        };
        // The method should propagate the NumberFormatException.
        UUIDConversions.toBigInteger(badSource, dummyConverter);
    }

    /**
     * {@code toBigDecimal} simply wraps the {@code BigInteger} result in a {@code BigDecimal}.
     * The test confirms that the numeric value is preserved.
     */
    @Test
    public void testToBigDecimal_ValidUuid() {
        String uuidString = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6";
        UUID uuid = UUID.fromString(uuidString);

        String hex = uuidString.replace("-", "");
        BigDecimal expected = new BigDecimal(new BigInteger(hex, 16));

        BigDecimal actual = UUIDConversions.toBigDecimal(uuid, dummyConverter);
        assertEquals("BigDecimal conversion should match the wrapped BigInteger", expected, actual);
    }

    /**
     * Verify that a {@code UUID} instance is transformed into a map containing a single
     * entry whose value is the canonical UUID string.
     * The exact key name is defined in {@code MapConversions.UUID}; the test does not
     * rely on its literal value – it only checks that the map has one entry and that
     * the stored value equals {@code uuid.toString()}.
     */
    @Test
    public void testToMap_ValidUuid() {
        UUID uuid = UUID.randomUUID();

        Map<String, Object> result = UUIDConversions.toMap(uuid, dummyConverter);

        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map must contain the UUID string as a value", result.containsValue(uuid.toString()));
    }

    /**
     * Passing {@code null} to {@code toMap} causes a {@link NullPointerException}
     * because the method casts the argument to {@code UUID} and invokes a method on it.
     */
    @Test(expected = NullPointerException.class)
    public void testToMap_NullArgument() {
        UUIDConversions.toMap(null, dummyConverter);
    }
}
