package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

/**
 * JUnit‑4 test suite for {@code UUIDConversions}.
 *
 * The suite is organised as follows:
 *
 * <ul>
 *   <li>Listing of the static methods that are visible to the test class (the class is in the same package).</li>
 *   <li>Basic functional tests that verify the normal behaviour of each method.</li>
 *   <li>Edge‑case / exception tests that cover null arguments, wrong types and malformed data.</li>
 * </ul>
 *
 * All tests use only the public API that is shown in the problem statement.
 */
public class UUIDConversionsTest {

    /* ---------------------------------------------------------------------- */
    /* 1.  Public (package‑visible) static methods of UUIDConversions          */
    /* ---------------------------------------------------------------------- */
    // static BigDecimal toBigDecimal(Object from, Converter converter)
    // static BigInteger toBigInteger(Object from, Converter converter)
    // static Map<String,Object> toMap(Object from, Converter converter)

    /* ---------------------------------------------------------------------- */
    /* Helper – a minimal Converter instance (the methods under test never use
       the converter, they only need a non‑null reference).                     */
    /* ---------------------------------------------------------------------- */
    private static final Converter DUMMY_CONVERTER = new Converter(null);

    /* ---------------------------------------------------------------------- */
    /* 2.  Basic functional tests                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBigInteger_WithUuidObject() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        // Expected value: the UUID interpreted as a 128‑bit hex number
        String hex = uuid.toString().replace("-", "");
        BigInteger expected = new BigInteger(hex, 16);

        BigInteger actual = UUIDConversions.toBigInteger(uuid, DUMMY_CONVERTER);
        Assert.assertEquals("BigInteger conversion from UUID failed", expected, actual);
    }

    @Test
    public void testToBigInteger_WithUuidString() {
        // Pass a String that looks like a UUID – the method only cares about the string value
        String uuidString = "123e4567-e89b-12d3-a456-426655440000";
        String hex = uuidString.replace("-", "");
        BigInteger expected = new BigInteger(hex, 16);

        BigInteger actual = UUIDConversions.toBigInteger(uuidString, DUMMY_CONVERTER);
        Assert.assertEquals("BigInteger conversion from UUID string failed", expected, actual);
    }

    @Test
    public void testToBigDecimal_WithUuidObject() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        String hex = uuid.toString().replace("-", "");
        BigInteger bi = new BigInteger(hex, 16);
        BigDecimal expected = new BigDecimal(bi);

        BigDecimal actual = UUIDConversions.toBigDecimal(uuid, DUMMY_CONVERTER);
        Assert.assertEquals("BigDecimal conversion from UUID failed", expected, actual);
    }

    @Test
    public void testToMap_WithUuidObject() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");

        Map<String, Object> map = UUIDConversions.toMap(uuid, DUMMY_CONVERTER);
        Assert.assertNotNull("Returned map must not be null", map);
        Assert.assertEquals("Map should contain exactly one entry", 1, map.size());

        // The key used by the library is defined in MapConversions.UUID.
        // Because we cannot import that constant here, we just verify that the stored value
        // equals uuid.toString() and that the map contains a single entry.
        Object value = map.values().iterator().next();
        Assert.assertEquals("Map value must be the UUID string", uuid.toString(), value);
    }

    /* ---------------------------------------------------------------------- */
    /* 3.  Edge‑case / exception tests                                       */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput() {
        UUIDConversions.toBigInteger(null, DUMMY_CONVERTER);
    }

    @Test(expected = NumberFormatException.class)
    public void testToBigInteger_MalformedHex() {
        // The string contains a non‑hex character ('g')
        UUIDConversions.toBigInteger("g12345", DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput() {
        UUIDConversions.toBigDecimal(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        UUIDConversions.toMap(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongObjectType() {
        // Pass a plain String – the method casts the argument to UUID and must fail
        UUIDConversions.toMap("not-a-uuid", DUMMY_CONVERTER);
    }

    @Test
    public void testToMap_ReturnedMapIsInsertionOrdered() {
        // The builder is instructed to use insertion order; we verify that the map
        // preserves that order by inserting a second entry after creation.
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        Map<String, Object> map = UUIDConversions.toMap(uuid, DUMMY_CONVERTER);

        // The map implementation is CompactMap; its entrySet iterator order should be insertion order.
        // Adding a second entry and checking order ensures the map respects insertion ordering.
        map.put("extraKey", "extraValue");
        Assert.assertEquals("First entry should be the UUID entry", uuid.toString(),
                map.entrySet().iterator().next().getValue());
    }
}
