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

public class UUIDConversionsTest {

    // A dummy Converter instance – the UUIDConversions methods do not use it,
    // but the signature requires it.
    private Converter dummyConverter;

    @Before
    public void setUp() {
        // Converter requires a ConverterOptions instance; we can pass a minimal one.
        // Since the class is not used inside UUIDConversions, we can safely pass null
        // or a default‑constructed options object if the constructor permits it.
        dummyConverter = new Converter(null);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* ----------------------------------------------------------------------
     * Tests for toBigInteger(Object, Converter)
     * ---------------------------------------------------------------------- */

    @Test
    public void testToBigInteger_WithValidUuidString() {
        UUID uuid = UUID.randomUUID();
        // UUID string contains hyphens; the method removes them and treats the result as hex.
        BigInteger result = UUIDConversions.toBigInteger(uuid, dummyConverter);
        // Verify that the result equals the numeric value of the UUID.
        BigInteger expected = new BigInteger(uuid.toString().replace("-", ""), 16);
        assertEquals(expected, result);
    }

    @Test
    public void testToBigInteger_WithValidHexString() {
        // Any object whose toString() returns a hex string should be accepted.
        Object hexObj = new Object() {
            @Override public String toString() { return "deadbeefcafebabe"; }
        };
        BigInteger result = UUIDConversions.toBigInteger(hexObj, dummyConverter);
        assertEquals(new BigInteger("deadbeefcafebabe", 16), result);
    }

    @Test
    public void testToBigInteger_NullInput_ShouldThrowNPE() {
        thrown.expect(NullPointerException.class);
        UUIDConversions.toBigInteger(null, dummyConverter);
    }

    @Test
    public void testToBigInteger_InvalidHex_ShouldThrowNumberFormatException() {
        Object badHex = new Object() {
            @Override public String toString() { return "GHIJKL"; } // not valid hex
        };
        thrown.expect(NumberFormatException.class);
        UUIDConversions.toBigInteger(badHex, dummyConverter);
    }

    /* ----------------------------------------------------------------------
     * Tests for toBigDecimal(Object, Converter)
     * ---------------------------------------------------------------------- */

    @Test
    public void testToBigDecimal_WithValidUuid() {
        UUID uuid = UUID.randomUUID();
        BigDecimal result = UUIDConversions.toBigDecimal(uuid, dummyConverter);
        BigDecimal expected = new BigDecimal(UUIDConversions.toBigInteger(uuid, dummyConverter));
        assertEquals(expected, result);
    }

    @Test
    public void testToBigDecimal_NullInput_ShouldThrowNPE() {
        thrown.expect(NullPointerException.class);
        UUIDConversions.toBigDecimal(null, dummyConverter);
    }

    /* ----------------------------------------------------------------------
     * Tests for toMap(Object, Converter)
     * ---------------------------------------------------------------------- */

    @Test
    public void testToMap_WithValidUuid() {
        UUID uuid = UUID.randomUUID();
        Map<String, Object> result = UUIDConversions.toMap(uuid, dummyConverter);

        // The map should have exactly one entry whose value is the UUID string.
        assertEquals(1, result.size());
        // The key is defined in MapConversions.UUID – we verify via the constant if available.
        // If the constant is not accessible, we fall back to checking the only entry.
        String key = result.keySet().iterator().next();
        assertEquals(uuid.toString(), result.get(key));
    }

    @Test
    public void testToMap_NullInput_ShouldThrowNPE() {
        thrown.expect(NullPointerException.class);
        UUIDConversions.toMap(null, dummyConverter);
    }

    @Test
    public void testToMap_WithWrongType_ShouldThrowClassCastException() {
        thrown.expect(ClassCastException.class);
        // Pass an object that is not a UUID; the method casts it directly.
        UUIDConversions.toMap("not-a-uuid", dummyConverter);
    }

    /* ----------------------------------------------------------------------
     * Additional sanity checks – ensure that the three conversion methods are
     * consistent with each other.
     * ---------------------------------------------------------------------- */

    @Test
    public void testConversionsConsistency() {
        UUID uuid = UUID.randomUUID();

        BigInteger bi = UUIDConversions.toBigInteger(uuid, dummyConverter);
        BigDecimal bd = UUIDConversions.toBigDecimal(uuid, dummyConverter);
        Map<String, Object> map = UUIDConversions.toMap(uuid, dummyConverter);

        assertEquals(new BigDecimal(bi), bd);
        assertEquals(uuid.toString(), map.values().iterator().next());
    }
}
