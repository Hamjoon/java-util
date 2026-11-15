package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_addPermanentClassAlias_Test {

    /**
     * Alias used for all tests in this class.
     */
    private static final String TEST_ALIAS = "unitTestAlias";

    /**
     * Remove the test alias before each test so that each test starts with a clean state.
     */
    @BeforeEach
    void setupBeforeEach() {
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
    }

    /**
     * After each test, clean up any alias that might have been added.
     */
    @AfterEach
    void teardownAfterEach() {
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
    }

    /**
     * Clears a specific alias (including {@code null}) from the internal map.
     */
    private static void clearAlias(Object alias) {
        // removePermanentClassAlias accepts a String, but passing null is safe – it delegates to the map.
        ClassUtilities.removePermanentClassAlias((String) alias);
    }

    /**
     * -------------------------------------------------------------
     *  Parameterized test data for normal and edge‑case alias additions.
     *  -------------------------------------------------------------
     */
    private static Stream<Arguments> aliasAdditionProvider() {
        return Stream.of(// Normal alias + class
        Arguments.of(String.class, "StringAlias", String.class, "Add a normal class with a normal alias"), // Distinct alias + class
        Arguments.of(java.util.Date.class, "DateAlias", java.util.Date.class, "Add a second distinct alias for a different class"), // Null class, non‑null alias
        Arguments.of(null, "NullClassAlias", null, "Null class with a regular alias"), // Null alias, non‑null class
        Arguments.of(Boolean.class, null, Boolean.class, "Null alias with a regular class"), // Both null
        Arguments.of(null, null, null, "Both class and alias are null"), // Empty string alias
        Arguments.of(Object.class, "", Object.class, "Empty string as alias"), // Whitespace alias
        Arguments.of(Object.class, "   ", Object.class, "Whitespace‑only alias"));
    }

    @Test
    void addPermanentClassAlias_ShouldMakeAliasResolvable() {
        // given
        Class<?> expected = java.util.HashMap.class;
        // when
        ClassUtilities.addPermanentClassAlias(expected, TEST_ALIAS);
        Class<?> actual = ClassUtilities.forName(TEST_ALIAS, ClassUtilities.getClassLoader());
        // then
        assertSame(expected, actual, "Class retrieved via forName should be the same class that was aliased.");
    }

    @Test
    void addPermanentClassAlias_OverwriteExistingAlias_ShouldReturnNewClass() {
        // given
        Class<?> firstClass = java.util.ArrayList.class;
        Class<?> secondClass = java.util.LinkedList.class;
        // when – first alias
        ClassUtilities.addPermanentClassAlias(firstClass, TEST_ALIAS);
        Class<?> firstResult = ClassUtilities.forName(TEST_ALIAS, ClassUtilities.getClassLoader());
        // when – overwrite with second alias
        ClassUtilities.addPermanentClassAlias(secondClass, TEST_ALIAS);
        Class<?> secondResult = ClassUtilities.forName(TEST_ALIAS, ClassUtilities.getClassLoader());
        // then
        assertSame(firstClass, firstResult, "Initial alias should resolve to the first class.");
        assertSame(secondClass, secondResult, "After overwriting, alias should resolve to the second class.");
    }

    @Test
    void removePermanentClassAlias_ShouldMakeAliasUnresolvable() {
        // given
        ClassUtilities.addPermanentClassAlias(String.class, TEST_ALIAS);
        assertNotNull(ClassUtilities.forName(TEST_ALIAS, ClassUtilities.getClassLoader()), "Alias must be resolvable before removal.");
        // when
        ClassUtilities.removePermanentClassAlias(TEST_ALIAS);
        Class<?> result = ClassUtilities.forName(TEST_ALIAS, ClassUtilities.getClassLoader());
        // then
        assertNull(result, "Alias should no longer be resolvable after removal.");
    }

    @Test
    void addPermanentClassAlias_NullClass_ShouldStoreNullWithoutException() {
        // given
        Class<?> nullClass = null;
        // when
        ClassUtilities.addPermanentClassAlias(nullClass, TEST_ALIAS);
        Class<?> result = ClassUtilities.forName(TEST_ALIAS, ClassUtilities.getClassLoader());
        // then
        assertNull(result, "Storing a null class should not throw and forName should return null.");
    }

    /**
     * Helper to obtain the private static {@code nameToClass} map via reflection.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Class<?>> getNameToClassMap() {
        try {
            Field f = ClassUtilities.class.getDeclaredField("nameToClass");
            f.setAccessible(true);
            return (Map<String, Class<?>>) f.get(null);
        } catch (Exception e) {
            throw new AssertionError("Unable to access ClassUtilities.nameToClass via reflection", e);
        }
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("aliasAdditionProvider")
    void addPermanentClassAlias_StoresMappingCorrectly(Class<?> clazz, String alias, Class<?> expectedValue, String description) {
        // Ensure a clean state for the specific alias (including null)
        clearAlias(alias);
        try {
            // when
            ClassUtilities.addPermanentClassAlias(clazz, alias);
            // then – inspect the internal map directly
            Map<String, Class<?>> map = getNameToClassMap();
            assertTrue(map.containsKey(alias), () -> "Map should contain the alias key: " + alias);
            assertEquals(expectedValue, map.get(alias), () -> "Map entry for alias '" + alias + "' should map to the expected class.");
        } finally {
            // Clean up after the test regardless of outcome
            clearAlias(alias);
        }
    }

    /**
     * -------------------------------------------------------------
     *  Overwrite existing alias with a different class.
     *  -------------------------------------------------------------
     */
    @Test
    void addPermanentClassAlias_OverwriteExistingAlias_UpdatesMapping() {
        final String alias = "StringAlias";
        clearAlias(alias);
        try {
            // first mapping
            ClassUtilities.addPermanentClassAlias(String.class, alias);
            Map<String, Class<?>> map = getNameToClassMap();
            assertEquals(String.class, map.get(alias), "Initial mapping should be String.class");
            // overwrite with Integer.class
            ClassUtilities.addPermanentClassAlias(Integer.class, alias);
            assertEquals(Integer.class, map.get(alias), "After overwrite the mapping should be Integer.class");
        } finally {
            clearAlias(alias);
        }
    }

    /**
     * -------------------------------------------------------------
     *  Idempotent addition – same class and alias again should not change state.
     *  -------------------------------------------------------------
     */
    @Test
    void addPermanentClassAlias_IdempotentSameEntry_NoChange() {
        final String alias = "IdempotentAlias";
        clearAlias(alias);
        try {
            ClassUtilities.addPermanentClassAlias(Object.class, alias);
            Map<String, Class<?>> map = getNameToClassMap();
            Class<?> before = map.get(alias);
            // Re‑add the same mapping
            ClassUtilities.addPermanentClassAlias(Object.class, alias);
            Class<?> after = map.get(alias);
            assertSame(before, after, "Re‑adding the same alias/class should leave the map unchanged");
        } finally {
            clearAlias(alias);
        }
    }

    /**
     * -------------------------------------------------------------
     *  Verify that removePermanentClassAlias truly removes the entry,
     *  even when the key is {@code null}.
     *  -------------------------------------------------------------
     */
    @Test
    void removePermanentClassAlias_RemovesNullKeyEntry() {
        // Add a null‑key entry
        ClassUtilities.addPermanentClassAlias(Boolean.class, null);
        Map<String, Class<?>> map = getNameToClassMap();
        assertTrue(map.containsKey(null), "Map should contain a null key after addition");
        // Remove it
        ClassUtilities.removePermanentClassAlias(null);
        assertFalse(map.containsKey(null), "Map should no longer contain the null key after removal");
    }

    /**
     * -------------------------------------------------------------
     *  Ensure that calling addPermanentClassAlias with a {@code null}
     *  alias does not throw an exception (HashMap permits null keys).
     *  -------------------------------------------------------------
     */
    @Test
    void addPermanentClassAlias_NullAlias_DoesNotThrow() {
        assertDoesNotThrow(() -> ClassUtilities.addPermanentClassAlias(String.class, null), "Adding a null alias should not throw an exception");
        // Verify the entry exists
        Map<String, Class<?>> map = getNameToClassMap();
        assertTrue(map.containsKey(null), "Map should contain a null key after adding null alias");
        assertEquals(String.class, map.get(null), "Null key should map to the provided class");
        // Clean up
        clearAlias(null);
    }

    /**
     * -------------------------------------------------------------
     *  Ensure that adding a {@code null} class with a non‑null alias stores a null value.
     *  -------------------------------------------------------------
     */
    @Test
    void addPermanentClassAlias_NullClass_StoresNullValue() {
        final String alias = "NullClassAlias";
        clearAlias(alias);
        try {
            ClassUtilities.addPermanentClassAlias(null, alias);
            Map<String, Class<?>> map = getNameToClassMap();
            assertTrue(map.containsKey(alias), "Map should contain the alias key even when class is null");
            assertNull(map.get(alias), "The value for the alias should be null");
        } finally {
            clearAlias(alias);
        }
    }
}
