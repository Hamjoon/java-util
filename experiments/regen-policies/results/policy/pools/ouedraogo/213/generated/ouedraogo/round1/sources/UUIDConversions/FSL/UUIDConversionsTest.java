package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

/**
 * Unit tests for {@link UUIDConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Conversion of a {@link UUID} (and a {@link String} that looks like a UUID) to {@link BigInteger}.</li>
 *   <li>Conversion of the same value to {@link BigDecimal} (should be equivalent to {@code new BigDecimal(BigInteger)}).</li>
 *   <li>Behaviour when {@code null} is supplied as the source object.</li>
 *   <li>Conversion of a {@link UUID} to a {@link Map} using the constant key defined in {@code MapConversions}.</li>
 *   <li>Ensuring a {@code ClassCastException} is thrown when a non‑UUID object is supplied to {@code toMap}.</li>
 * </ul>
 *
 * No real {@link Converter} instance is required because the implementation does not use it;
 * a {@code null} reference is passed deliberately.
 */
public class UUIDConversionsTest {

    /** A deterministic UUID used by several tests. */
    private static final UUID FIXED_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");

    /** Helper that returns the constant key defined in {@code MapConversions}. */
    private static String uuidMapKey() throws Exception {
        // MapConversions is part of the same library; we retrieve the public static field "UUID".
        Class<?> mapConvClass = Class.forName("com.cedarsoftware.util.convert.MapConversions");
        return (String) mapConvClass.getField("UUID").get(null);
    }

    @Test
    public void testToBigIntegerFromUUID() {
        // Expected value: the UUID string without hyphens interpreted as a hex number.
        String hex = FIXED_UUID.toString().replace("-", "");
        BigInteger expected = new BigInteger(hex, 16);

        BigInteger actual = UUIDConversions.toBigInteger(FIXED_UUID, null);
        assertEquals("BigInteger conversion from UUID failed", expected, actual);
    }

    @Test
    public void testToBigIntegerFromStringRepresentingUUID() {
        String uuidString = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
        String hex = uuidString.replace("-", "");
        BigInteger expected = new BigInteger(hex, 16);

        BigInteger actual = UUIDConversions.toBigInteger(uuidString, null);
        assertEquals("BigInteger conversion from String failed", expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigIntegerNullInput() {
        // The implementation calls from.toString(), so a null argument must raise NPE.
        UUIDConversions.toBigInteger(null, null);
    }

    @Test
    public void testToBigDecimalMatchesBigInteger() {
        BigInteger bi = UUIDConversions.toBigInteger(FIXED_UUID, null);
        BigDecimal expected = new BigDecimal(bi);

        BigDecimal actual = UUIDConversions.toBigDecimal(FIXED_UUID, null);
        assertEquals("BigDecimal should be constructed from the same BigInteger", expected, actual);
    }

    @Test
    public void testToMapWithValidUUID() throws Exception {
        Map<String, Object> map = UUIDConversions.toMap(FIXED_UUID, null);

        assertNotNull("Resulting map must not be null", map);
        assertEquals("Map should contain exactly one entry", 1, map.size());

        String key = uuidMapKey(); // retrieve the constant defined in MapConversions
        assertTrue("Map must contain the UUID key from MapConversions", map.containsKey(key));
        assertEquals("Map value must be the UUID's string representation", FIXED_UUID.toString(), map.get(key));
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWithInvalidTypeThrowsException() {
        // Passing a plain String should cause a ClassCastException because the method casts to UUID.
        UUIDConversions.toMap("not-a-uuid", null);
    }
}
