package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link EnumConversions}.
 *
 * The tests cover:
 *   • Normal operation with a valid enum value
 *   • Null source object (should throw NPE)
 *   • Source object that is not an enum (should throw ClassCastException)
 *   • Passing a null {@code Converter} (the method does not use the converter)
 *   • Independence of the returned map instance
 */
public class EnumConversionsTest {

    /** Simple enum used only for testing. */
    private enum TestEnum {
        A, B, C
    }

    /** Helper to create a dummy {@link Converter}. The constructor accepts {@code null}
     *  because the converter is not used inside {@code EnumConversions.toMap}.
     */
    private static Converter dummyConverter() {
        return new Converter(null);
    }

    @Test
    public void testToMapBasic() {
        TestEnum value = TestEnum.A;
        Map result = EnumConversions.toMap(value, dummyConverter());

        // The map must contain exactly one entry with key "name"
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map should contain key 'name'", result.containsKey("name"));
        assertEquals("Enum name should be stored under key 'name'",
                     value.name(),
                     result.get("name"));
    }

    @Test
    public void testToMapNullConverter() {
        TestEnum value = TestEnum.B;
        Map result = EnumConversions.toMap(value, null);

        assertEquals("B", result.get("name"));
    }

    @Test(expected = NullPointerException.class)
    public void testToMapNullFrom() {
        EnumConversions.toMap(null, dummyConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToMapNonEnumFrom() {
        EnumConversions.toMap("I am not an enum", dummyConverter());
    }

    @Test
    public void testReturnedMapIsIndependent() {
        TestEnum first = TestEnum.C;
        Map firstMap = EnumConversions.toMap(first, dummyConverter());

        // Modify the first map
        firstMap.put("extra", 123);

        // Obtain a second map and ensure it does NOT contain the extra entry
        Map secondMap = EnumConversions.toMap(TestEnum.A, dummyConverter());
        assertFalse("Second map should not contain entry added to first map", secondMap.containsKey("extra"));
        assertEquals("Second map should still contain correct enum name",
                     TestEnum.A.name(),
                     secondMap.get("name"));
    }
}
