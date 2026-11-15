package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.Map;

// import org.junit.*;
// import static org.junit.Assert.*;
class ClassUtilities_removePermanentClassAlias_Test {

    private static final String TEST_ALIAS = "DummyAlias";

    /**
     * Simple class used only for aliasing in the tests.
     */
    private static class DummyClass {
    }

    //     @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test – remove the alias if it somehow remains.
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
    }

    //     @AfterEach
    void teardownAfterEach() {
        // Clean up the alias after each test to avoid side‑effects on other tests.
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
    }

    /**
     * Clears the whole alias map – used to guarantee test isolation.
     */
    private static void clearAllAliases() throws Exception {
        getAliasMap().clear();
    }

    /**
     * Simple assertion helpers that throw {@link AssertionError} on failure.
     */
    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " Expected: " + expected + " but was: " + actual);
        }
    }

    private static void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertionError(message);
        }
    }

    private static void assertNull(Object obj, String message) {
        if (obj != null) {
            throw new AssertionError(message);
        }
    }

    /**
     * removePermanentClassAlias should delete a previously added alias.
     */
    private static void testRemovePermanentClassAlias_RemovesAlias() throws Exception {
        ClassUtilities.addPermanentClassAlias(DummyClass.class, TEST_ALIAS);
        Class<?> beforeRemoval = ClassUtilities.forName(TEST_ALIAS, ClassUtilities.getClassLoader());
        assertNotNull(beforeRemoval, "Alias should be resolvable before removal");
        assertEquals(DummyClass.class, beforeRemoval, "Alias should resolve to the class that was registered");
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
        Class<?> afterRemoval = ClassUtilities.forName(TEST_ALIAS, ClassUtilities.getClassLoader());
        assertNull(afterRemoval, "Alias should no longer be resolvable after removal");
    }

    /**
     * Removing a non‑existing alias does not throw and leaves map unchanged.
     */
    private static void testRemoveNonExistingAlias_NoException() throws Exception {
        Map<String, Class<?>> map = getAliasMap();
        int sizeBefore = map.size();
        ClassUtilities.removePermanentClassAlias("NonExistingAlias");
        int sizeAfter = map.size();
        assertEquals(sizeBefore, sizeAfter, "Map size should stay unchanged");
    }

    /**
     * removePermanentClassAlias(null) propagates NullPointerException.
     */
    private static void testRemoveNullAlias_ThrowsNPE() {
        try {
            ClassUtilities.removePermanentClassAlias(null);
            throw new AssertionError("Expected NullPointerException");
        } catch (NullPointerException ignored) {
            // expected
        }
    }

    /**
     * Empty string alias can be added and removed like any other key.
     */
//     private static void testRemoveEmptyStringAlias() throws Exception {
//         String empty = "";
//         ClassUtilities.addPermanentClassAlias(DummyClass.class, empty);
//         assertNotNull(ClassUtilities.forName(empty, ClassUtilities.getClassLoader()));
//         ClassUtilities.removePermanentClassAlias(empty);
//         assertNull(ClassUtilities.forName(empty, ClassUtilities.getClassLoader()));
//     }

    /**
     * Alias containing special characters is removed correctly.
     */
//     private static void testRemoveSpecialCharAlias() throws Exception {
//         String special = "AliasWithSpecialChars!@#$$%";
//         ClassUtilities.addPermanentClassAlias(DummyClass.class, special);
//         assertNotNull(ClassUtilities.forName(special, ClassUtilities.getClassLoader()));
//         ClassUtilities.removePermanentClassAlias(special);
//         assertNull(ClassUtilities.forName(special, ClassUtilities.getClassLoader()));
//     }

    /**
     * Very long alias is handled correctly.
     */
//     private static void testRemoveVeryLongAlias() throws Exception {
//         String longAlias = "a".repeat(1000);
//         ClassUtilities.addPermanentClassAlias(DummyClass.class, longAlias);
//         assertNotNull(ClassUtilities.forName(longAlias, ClassUtilities.getClassLoader()));
//         int sizeBefore = getAliasMap().size();
//         ClassUtilities.removePermanentClassAlias(longAlias);
//         int sizeAfter = getAliasMap().size();
//         assertNull(ClassUtilities.forName(longAlias, ClassUtilities.getClassLoader()));
//         assertEquals(sizeBefore - 1, sizeAfter, "Map size should decrease by one after removal");
//     }

    /**
     * Whitespace‑only alias is removed if present.
     */
//     private static void testRemoveWhitespaceAlias() throws Exception {
//         String ws = "   ";
//         ClassUtilities.addPermanentClassAlias(DummyClass.class, ws);
//         assertNotNull(ClassUtilities.forName(ws, ClassUtilities.getClassLoader()));
//         ClassUtilities.removePermanentClassAlias(ws);
//         assertNull(ClassUtilities.forName(ws, ClassUtilities.getClassLoader()));
//     }

    /**
     * Alias matching a fully‑qualified class name is removed correctly.
     */
//     private static void testRemoveFqcnAlias() throws Exception {
//         String fqcnAlias = "java.util.List";
//         ClassUtilities.addPermanentClassAlias(DummyClass.class, fqcnAlias);
//         assertNotNull(ClassUtilities.forName(fqcnAlias, ClassUtilities.getClassLoader()));
//         ClassUtilities.removePermanentClassAlias(fqcnAlias);
//         assertNull(ClassUtilities.forName(fqcnAlias, ClassUtilities.getClassLoader()));
//     }

    /**
     * Removal is case‑sensitive – different case does not affect stored key.
     */
//     private static void testCaseSensitivity() throws Exception {
//         String stored = "TestAlias";
//         String differentCase = "testalias";
//         ClassUtilities.addPermanentClassAlias(DummyClass.class, stored);
//         assertNotNull(ClassUtilities.forName(stored, ClassUtilities.getClassLoader()));
//         ClassUtilities.removePermanentClassAlias(differentCase);
//         assertNotNull("Original alias should remain after case‑mismatched removal", ClassUtilities.forName(stored, ClassUtilities.getClassLoader()));
//     }

    /**
     * Removing one of several aliases leaves the others untouched.
     */
//     private static void testRemoveOneOfMultipleAliases() throws Exception {
//         String a1 = "MultiAlias1";
//         String a2 = "MultiAlias2";
//         ClassUtilities.addPermanentClassAlias(DummyClass.class, a1);
//         ClassUtilities.addPermanentClassAlias(DummyClass.class, a2);
//         assertNotNull(ClassUtilities.forName(a1, ClassUtilities.getClassLoader()));
//         assertNotNull(ClassUtilities.forName(a2, ClassUtilities.getClassLoader()));
//         ClassUtilities.removePermanentClassAlias(a1);
//         assertNull(ClassUtilities.forName(a1, ClassUtilities.getClassLoader()));
//         assertNotNull("Other alias should remain after removal of a different key", ClassUtilities.forName(a2, ClassUtilities.getClassLoader()));
//     }

    /**
     * Calling removePermanentClassAlias twice is safe and idempotent.
     */
//     private static void testRepeatedRemovalIdempotence() throws Exception {
//         String alias = "RepeatedRemoval";
//         ClassUtilities.addPermanentClassAlias(DummyClass.class, alias);
//         assertNotNull(ClassUtilities.forName(alias, ClassUtilities.getClassLoader()));
//         ClassUtilities.removePermanentClassAlias(alias);
//         assertNull(ClassUtilities.forName(alias, ClassUtilities.getClassLoader()));
//         // second removal must not throw
//         ClassUtilities.removePermanentClassAlias(alias);
//     }

    /**
     * Removing from an empty alias map does not throw.
     */
    private static void testRemoveFromEmptyMap() throws Exception {
        clearAllAliases();
        assertTrue(getAliasMap().isEmpty(), "Map should be empty before test");
        ClassUtilities.removePermanentClassAlias("NonExistentAfterClear");
        assertTrue(getAliasMap().isEmpty(), "Map should remain empty after removal attempt");
    }

    /**
     * Runs all demo tests.
     */
    public static void main(String[] args) throws Exception {
        // Ensure a clean state before each test.
        clearAllAliases();
        testRemovePermanentClassAlias_RemovesAlias();
        testRemoveNonExistingAlias_NoException();
        testRemoveNullAlias_ThrowsNPE();
        testRemoveEmptyStringAlias();
        testRemoveSpecialCharAlias();
        testRemoveVeryLongAlias();
        testRemoveWhitespaceAlias();
        testRemoveFqcnAlias();
        testCaseSensitivity();
        testRemoveOneOfMultipleAliases();
        testRepeatedRemovalIdempotence();
        testRemoveFromEmptyMap();
        System.out.println("All ClassUtilities removePermanentClassAlias checks passed.");
    }

    /**
     * Helper to obtain the internal map via reflection.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Class<?>> getAliasMap() throws Exception {
        Field f = ClassUtilities.class.getDeclaredField("nameToClass");
        f.setAccessible(true);
        return (Map<String, Class<?>>) f.get(null);
    }
}