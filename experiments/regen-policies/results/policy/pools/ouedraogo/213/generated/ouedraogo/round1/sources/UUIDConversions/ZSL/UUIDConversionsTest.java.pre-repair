package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

/**
 * Unit tests for {@link UUIDConversions}.
 *
 * The class under test contains only static package‑private methods, therefore the test class
 * lives in the same package to gain access without using reflection on the methods themselves.
 *
 * The {@code Converter} argument is not used by the implementation, so a {@code null} reference
 * is sufficient for all calls.
 */
public class UUIDConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The implementation does not touch the Converter, so a null reference is safe.
        // Using a real instance would require knowledge of ConverterOptions which is not
        // needed for the current test scenarios.
        dummyConverter = null;
    }

    /**
     * Helper that builds the expected {@link BigInteger} from a UUID string
     * by removing hyphens and interpreting the remaining hex digits.
     */
    private static BigInteger expectedBigIntegerFromUuidString(String uuidString) {
        String hex = uuidString.replace("-", "");
        return new BigInteger(hex, 16);
    }

    @Test
    public void testToBigInteger_WithUuidString() {
        String uuidStr = "123e4567-e89b-12d3-a456-426655440000";
        BigInteger expected = expectedBigIntegerFromUuidString(uuidStr);

        // The method accepts any Object; we pass a plain String.
        BigInteger actual = UUIDConversions.toBigInteger(uuidStr, dummyConverter);

        assertEquals("BigInteger conversion from UUID string failed", expected, actual);
    }

    @Test
    public void testToBigInteger_WithUuidObject() {
        UUID uuid = UUID.fromString("de305d54-75b4-431b-adb2-eb6b9e546014");
        BigInteger expected = expectedBigIntegerFromUuidString(uuid.toString());

        // Pass the UUID object directly.
        BigInteger actual = UUIDConversions.toBigInteger(uuid, dummyConverter);

        assertEquals("BigInteger conversion from UUID object failed", expected, actual);
    }

    @Test(expected = NumberFormatException.class)
    public void testToBigInteger_WithInvalidStringThrowsException() {
        // This string does not represent a hexadecimal number after hyphen removal.
        String bad = "not-a-uuid";
        UUIDConversions.toBigInteger(bad, dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_WithNullThrowsException() {
        UUIDConversions.toBigInteger(null, dummyConverter);
    }

    @Test
    public void testToBigDecimal_DelegatesToBigInteger() {
        UUID uuid = UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6");
        BigInteger expectedBigInt = expectedBigIntegerFromUuidString(uuid.toString());
        BigDecimal expected = new BigDecimal(expectedBigInt);

        BigDecimal actual = UUIDConversions.toBigDecimal(uuid, dummyConverter);

        assertEquals("BigDecimal conversion did not match expected value", expected, actual);
    }

    @Test
    public void testToMap_ReturnsLinkedHashMapWithSingleEntry() {
        UUID uuid = UUID.fromString("6fa459ea-ee8a-3ca4-894e-db77e160355e");
        Map<String, Object> map = UUIDConversions.toMap(uuid, dummyConverter);

        // Verify map characteristics
        assertNotNull("Returned map should not be null", map);
        assertEquals("Map should contain exactly one entry", 1, map.size());

        // Retrieve the single key/value pair
        String key = map.keySet().iterator().next();
        Object value = map.get(key);

        assertEquals("Map value should be the UUID's string representation",
                     uuid.toString(), value);
        // The key is defined in MapConversions.UUID; we cannot reference it directly,
        // but we can assert that it is a non‑empty string.
        assertNotNull("Map key should not be null", key);
        assertFalse("Map key should not be empty", key.isEmpty());
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WithWrongObjectTypeThrowsException() {
        // The method casts the input to UUID without checking; a non‑UUID causes CCE.
        String notUuid = "just-a-string";
        UUIDConversions.toMap(notUuid, dummyConverter);
    }
}
