package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

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
 *   <li>Typical conversion of a {@link UUID} to {@link BigInteger} and {@link BigDecimal}</li>
 *   <li>Conversion when the source object is a custom {@code toString()} provider</li>
 *   <li>Behaviour when {@code null} is supplied (expected NPE)</li>
 *   <li>Conversion of a {@link UUID} to a {@link Map} and verification of the expected key/value</li>
 *   <li>Class‑cast failure when the source object is not a {@link UUID}</li>
 * </ul>
 *
 * The {@link Converter} argument is required by the API but is not used by the
 * implementation; a minimal instance with default options is sufficient.
 */
public class UUIDConversionsTest {

    private Converter dummyConverter;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // The Converter constructor requires a ConverterOptions instance.
        // Using the default constructor is sufficient for these tests.
        dummyConverter = new Converter(ConverterOptions.DEFAULT);
    }

    /**
     * Helper that builds the expected {@link BigInteger} from a UUID string
     * by stripping hyphens and parsing the remaining hex digits.
     */
    private static BigInteger expectedBigIntegerFromUUID(UUID uuid) {
        String hex = uuid.toString().replace("-", "");
        return new BigInteger(hex, 16);
    }

    @Test
    public void testToBigInteger_FromUUID_Typical() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        BigInteger expected = expectedBigIntegerFromUUID(uuid);

        BigInteger actual = UUIDConversions.toBigInteger(uuid, dummyConverter);

        assertEquals("BigInteger conversion from UUID should match expected value", expected, actual);
    }

    @Test
    public void testToBigDecimal_FromUUID_Typical() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        BigInteger bi = expectedBigIntegerFromUUID(uuid);
        BigDecimal expected = new BigDecimal(bi);

        BigDecimal actual = UUIDConversions.toBigDecimal(uuid, dummyConverter);

        assertEquals("BigDecimal conversion from UUID should match expected value", expected, actual);
    }

    @Test
    public void testToBigInteger_FromCustomObject_WithHexString() {
        // Custom object whose toString() returns a hex representation (no hyphens)
        Object hexProvider = new Object() {
            @Override
            public String toString() {
                return "deadbeefcafebabe";
            }
        };

        BigInteger expected = new BigInteger("deadbeefcafebabe", 16);
        BigInteger actual = UUIDConversions.toBigInteger(hexProvider, dummyConverter);

        assertEquals("BigInteger conversion should use the object's toString() value as hex", expected, actual);
    }

    @Test
    public void testToBigInteger_NullInput_ThrowsNPE() {
        thrown.expect(NullPointerException.class);
        UUIDConversions.toBigInteger(null, dummyConverter);
    }

    @Test
    public void testToBigDecimal_NullInput_ThrowsNPE() {
        thrown.expect(NullPointerException.class);
        UUIDConversions.toBigDecimal(null, dummyConverter);
    }

    @Test
    public void testToMap_FromUUID_ContainsExpectedEntry() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        Map<String, Object> result = UUIDConversions.toMap(uuid, dummyConverter);

        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());

        // The constant key used inside UUIDConversions is MapConversions.UUID.
        // Its value is defined as the literal string "UUID" in the library,
        // so we assert against that literal.
        assertTrue("Map must contain the key \"UUID\"", result.containsKey("UUID"));
        assertEquals("Map value must be the UUID's string representation",
                uuid.toString(),
                result.get("UUID"));
    }

    @Test
    public void testToMap_NonUUIDInput_ThrowsClassCastException() {
        thrown.expect(ClassCastException.class);
        // Passing a plain String will cause the cast (UUID) from inside toMap to fail.
        UUIDConversions.toMap("not-a-uuid", dummyConverter);
    }
}
