package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import java.util.*;
import static org.junit.Assert.*;

public class EnumConversionsTest {

    /** Simple enum used for all tests */
    private enum TestEnum { A, B, C }

    /** -----------------------------------------------------------------
     *  BASIC (happy‑path) tests
     * ----------------------------------------------------------------- */
    @Test
    public void testToMapProducesCorrectEntry() {
        Map<String, Object> result = EnumConversions.toMap(TestEnum.B, null);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("B", result.get("name"));
    }

    @Test
    public void testToEnumSetFromEnumCollection() {
        List<TestEnum> source = Arrays.asList(TestEnum.A, TestEnum.C);
        EnumSet<TestEnum> set = EnumConversions.toEnumSet(source, TestEnum.class);
        assertEquals(EnumSet.of(TestEnum.A, TestEnum.C), set);
    }

    @Test
    public void testToEnumSetFromStringCollection() {
        List<String> source = Arrays.asList("A", "C");
        EnumSet<TestEnum> set = EnumConversions.toEnumSet(source, TestEnum.class);
        assertEquals(EnumSet.of(TestEnum.A, TestEnum.C), set);
    }

    @Test
    public void testToEnumSetFromNumberCollection() {
        List<Integer> source = Arrays.asList(0, 2); // ordinals for A and C
        EnumSet<TestEnum> set = EnumConversions.toEnumSet(source, TestEnum.class);
        assertEquals(EnumSet.of(TestEnum.A, TestEnum.C), set);
    }

    @Test
    public void testToEnumSetFromMixedArray() {
        Object[] source = new Object[] { TestEnum.B, "A", 2 }; // B, A, C
        EnumSet<TestEnum> set = EnumConversions.toEnumSet(source, TestEnum.class);
        assertEquals(EnumSet.of(TestEnum.A, TestEnum.B, TestEnum.C), set);
    }

    @Test
    public void testToEnumSetIgnoresNullElements() {
        List<Object> source = Arrays.asList(TestEnum.A, null, "B", null, 2);
        EnumSet<TestEnum> set = EnumConversions.toEnumSet(source, TestEnum.class);
        assertEquals(EnumSet.of(TestEnum.A, TestEnum.B, TestEnum.C), set);
    }

    @Test
    public void testToEnumSetFromEmptyCollection() {
        List<TestEnum> source = Collections.emptyList();
        EnumSet<TestEnum> set = EnumConversions.toEnumSet(source, TestEnum.class);
        assertTrue(set.isEmpty());
    }

    @Test
    public void testToEnumSetFromEmptyArray() {
        Object[] source = new Object[0];
        EnumSet<TestEnum> set = EnumConversions.toEnumSet(source, TestEnum.class);
        assertTrue(set.isEmpty());
    }

    /** -----------------------------------------------------------------
     *  EDGE‑CASE / EXCEPTION tests
     * ----------------------------------------------------------------- */

    // ---- toMap -------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testToMapWithNullSourceThrowsNPE() {
        EnumConversions.toMap(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWithNonEnumSourceThrowsCCE() {
        EnumConversions.toMap("not-an-enum", null);
    }

    // ---- toEnumSet ---------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testToEnumSetTargetNotEnumThrowsIAE() {
        List<String> source = Arrays.asList("A");
        EnumConversions.toEnumSet(source, String.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToEnumSetSourceNotCollectionOrArrayThrowsIAE() {
        EnumConversions.toEnumSet(new Object(), TestEnum.class);
    }

    @Test
    public void testToEnumSetInvalidStringThrowsIAE() {
        List<String> source = Arrays.asList("INVALID");
        try {
            EnumConversions.toEnumSet(source, TestEnum.class);
            fail("Expected IllegalArgumentException for unknown enum name");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("No enum constant"));
        }
    }

    @Test
    public void testToEnumSetOrdinalOutOfRangeThrowsIAE() {
        List<Integer> source = Arrays.asList(5); // only 0‑2 are valid
        try {
            EnumConversions.toEnumSet(source, TestEnum.class);
            fail("Expected IllegalArgumentException for out‑of‑range ordinal");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid ordinal value"));
        }
    }

    @Test
    public void testToEnumSetUnsupportedElementTypeThrowsIAE() {
        List<Boolean> source = Arrays.asList(Boolean.TRUE);
        try {
            EnumConversions.toEnumSet(source, TestEnum.class);
            fail("Expected IllegalArgumentException for unsupported element type");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("is not convertible"));
        }
    }

    @Test
    public void testToEnumSetFromPrimitiveIntArray() {
        int[] source = new int[] { 0, 1 }; // ordinals for A and B
        EnumSet<TestEnum> set = EnumConversions.toEnumSet(source, TestEnum.class);
        assertEquals(EnumSet.of(TestEnum.A, TestEnum.B), set);
    }
}
