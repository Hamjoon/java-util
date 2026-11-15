package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

/**
 * Unit tests for {@link UUIDConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Conversion of a UUID string (with hyphens) to {@link BigInteger} and {@link BigDecimal}.</li>
 *   <li>Edge‑cases such as the all‑zero UUID and a {@code null} source.</li>
 *   <li>Conversion of a {@link UUID} to a {@link Map} that contains the string representation
 *       under the {@link MapConversions#UUID} key.</li>
 *   <li>Behaviour when the source object is not a {@link UUID} but its {@code toString()}
 *       returns a valid hexadecimal literal.</li>
 * </ul>
 *
 * Only the public API shown in the problem statement is used – no additional
 * library classes are instantiated or mocked.
 */
public class UUIDConversionsTest {

    /** A {@link Converter} instance required by the static methods.  The constructor
     *  accepts a {@code ConverterOptions} argument; passing {@code null} is sufficient
     *  for the purposes of these tests because the implementation under test does not
     *  dereference the options. */
    private static final Converter CONVERTER = new Converter(null);

    /* --------------------------------------------------------------------- */
    /*  toBigInteger()                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void toBigInteger_convertsStandardUuidCorrectly() {
        // UUID taken from the RFC example – it contains both digits and letters.
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        // Expected hex string without hyphens.
        String expectedHex = "123e4567e89b12d3a456426655440000";
        BigInteger expected = new BigInteger(expectedHex, 16);

        BigInteger actual = UUIDConversions.toBigInteger(uuid, CONVERTER);
        assertEquals("BigInteger conversion did not match expected value", expected, actual);
    }

    @Test
    public void toBigInteger_allZeroUuidYieldsZero() {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        BigInteger actual = UUIDConversions.toBigInteger(uuid, CONVERTER);
        assertEquals("All‑zero UUID should result in BigInteger zero", BigInteger.ZERO, actual);
    }

    @Test
    public void toBigInteger_acceptsCustomObjectWithHexString() {
        // An object whose toString() returns a hex literal containing hyphens.
        Object custom = new Object() {
            @Override
            public String toString() {
                return "dead-beef-cafe-babe-1234567890ab";
            }
        };
        // Expected hex after hyphen removal.
        String expectedHex = "deadbeefcafebabe1234567890ab";
        BigInteger expected = new BigInteger(expectedHex, 16);

        BigInteger actual = UUIDConversions.toBigInteger(custom, CONVERTER);
        assertEquals("Conversion of custom object failed", expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void toBigInteger_nullSourceThrowsNPE() {
        UUIDConversions.toBigInteger(null, CONVERTER);
    }

    /* --------------------------------------------------------------------- */
    /*  toBigDecimal()                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void toBigDecimal_isBigIntegerWrappedInBigDecimal() {
        UUID uuid = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        BigInteger bi = UUIDConversions.toBigInteger(uuid, CONVERTER);
        BigDecimal expected = new BigDecimal(bi);
        BigDecimal actual = UUIDConversions.toBigDecimal(uuid, CONVERTER);
        assertEquals("BigDecimal should be constructed from the same BigInteger", expected, actual);
    }

    /* --------------------------------------------------------------------- */
    /*  toMap()                                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void toMap_returnsMapWithUuidString() {
        UUID uuid = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
        Map<String, Object> map = UUIDConversions.toMap(uuid, CONVERTER);

        // The library defines the key as a constant – use the same constant for verification.
        String expectedKey = MapConversions.UUID;   // e.g. "uuid"
        assertEquals("Map should contain exactly one entry", 1, map.size());
        assertTrue("Map must contain the expected key", map.containsKey(expectedKey));
        assertEquals("Map value must be the UUID's string representation",
                     uuid.toString(), map.get(expectedKey));
    }

    @Test
    public void toMap_isInsertionOrdered() {
        // The builder used in UUIDConversions specifies insertionOrder().
        // Verify that the iterator order matches insertion order for a single entry.
        UUID uuid = UUID.randomUUID();
        Map<String, Object> map = UUIDConversions.toMap(uuid, CONVERTER);
        String firstKey = map.keySet().iterator().next();
        assertEquals("The sole key must be the UUID constant", MapConversions.UUID, firstKey);
    }

    @Test(expected = ClassCastException.class)
    public void toMap_nonUuidArgumentThrowsClassCastException() {
        // The implementation casts the argument to UUID without checking.
        // Passing an incompatible type must result in a ClassCastException.
        UUIDConversions.toMap("not-a-uuid", CONVERTER);
    }
}
