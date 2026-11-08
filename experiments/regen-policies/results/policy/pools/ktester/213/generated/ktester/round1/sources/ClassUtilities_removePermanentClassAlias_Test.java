package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_removePermanentClassAlias_Test {

    /**
     * Simple class used only for aliasing in the tests.
     */
    private static class Dummy {
    }

    /**
     * Additional dummy classes for multi‑entry scenarios.
     */
    private static class A {
    }

    private static class B {
    }

    private static class C {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the alias is not present before each test.
        // removePermanentClassAlias is safe to call even if the alias does not exist.
        ClassUtilities.removePermanentClassAlias("DummyAlias");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up – make sure the alias is removed so that tests stay independent.
        ClassUtilities.removePermanentClassAlias("DummyAlias");
    }

    /**
     * Clears the internal alias map. Used to guarantee a clean state for tests that need it.
     */
    private static void clearAliasMap() {
        getAliasMap().clear();
    }

    @Test
    @DisplayName("add alias, verify it can be resolved, then remove and verify it is no longer resolvable")
    void testAddThenRemoveAlias() throws Exception {
        // Arrange – add a permanent alias for Dummy
        ClassUtilities.addPermanentClassAlias(Dummy.class, "DummyAlias");
        // Act – resolve the alias using forName (should succeed)
        Class<?> resolvedBeforeRemoval = ClassUtilities.forName("DummyAlias", ClassUtilities.getClassLoader());
        // Assert – the resolved class must be Dummy
        assertNotNull(resolvedBeforeRemoval, "Alias should resolve to a class");
        assertEquals(Dummy.class, resolvedBeforeRemoval, "Alias should resolve to Dummy class");
        // Now remove the alias
        ClassUtilities.removePermanentClassAlias("DummyAlias");
        // Act & Assert – trying to resolve the same alias should now fail with ClassNotFoundException
        assertThrows(ClassNotFoundException.class, () -> {
            ClassUtilities.forName("DummyAlias", ClassUtilities.getClassLoader());
        }, "After removal, the alias should no longer be resolvable");
    }

    @Test
    @DisplayName("removing a non‑existent alias does not affect existing mappings")
    void testRemoveNonExistentAliasDoesNotThrow() throws Exception {
        // Arrange – add a known alias
        ClassUtilities.addPermanentClassAlias(Dummy.class, "DummyAlias");
        // Act – attempt to remove an alias that was never added
        assertDoesNotThrow(() -> ClassUtilities.removePermanentClassAlias("NonExistentAlias"), "Removing a non‑existent alias should not throw an exception");
        // Verify the original alias is still resolvable
        Class<?> stillResolved = ClassUtilities.forName("DummyAlias", ClassUtilities.getClassLoader());
        assertEquals(Dummy.class, stillResolved, "Existing alias must remain after removing a different alias");
        // Clean up
        ClassUtilities.removePermanentClassAlias("DummyAlias");
    }

    /**
     * Retrieves the private static {@code nameToClass} map via reflection.
     *
     * @return the internal alias map
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Class<?>> getAliasMap() {
        try {
            Field f = ClassUtilities.class.getDeclaredField("nameToClass");
            f.setAccessible(true);
            return (Map<String, Class<?>>) f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to access nameToClass map", e);
        }
    }

    @Test
    @DisplayName("remove an existing alias and ensure other entries stay intact")
    void testRemoveExistingAlias() {
        // Arrange – add two distinct aliases
        ClassUtilities.addPermanentClassAlias(Dummy.class, "myAlias");
        ClassUtilities.addPermanentClassAlias(A.class, "otherAlias");
        Map<String, Class<?>> map = getAliasMap();
        assertTrue(map.containsKey("myAlias"));
        assertTrue(map.containsKey("otherAlias"));
        // Act – remove the target alias
        ClassUtilities.removePermanentClassAlias("myAlias");
        // Assert – target alias gone, other alias still present
        assertFalse(map.containsKey("myAlias"), "Alias \"myAlias\" should be removed");
        assertTrue(map.containsKey("otherAlias"), "Other alias must remain untouched");
    }

    @Test
    @DisplayName("removing a non‑present alias leaves the map unchanged")
    void testRemoveNonPresentAliasNoChange() {
        // Arrange – map contains a single entry
        ClassUtilities.addPermanentClassAlias(Dummy.class, "existingAlias");
        Map<String, Class<?>> map = getAliasMap();
        int originalSize = map.size();
        // Act – attempt to remove a missing key
        ClassUtilities.removePermanentClassAlias("missingAlias");
        // Assert – size unchanged and original entry still there
        assertEquals(originalSize, map.size(), "Map size should stay the same");
        assertTrue(map.containsKey("existingAlias"), "Existing entry must stay");
    }

    @Test
    @DisplayName("null alias argument propagates NullPointerException")
    void testRemoveNullAliasThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ClassUtilities.removePermanentClassAlias(null), "Passing null should result in NullPointerException");
    }

    @Test
    @DisplayName("removing from an empty alias map does nothing and throws no exception")
    void testRemoveFromEmptyMap() {
        // Ensure map is empty
        clearAliasMap();
        assertTrue(getAliasMap().isEmpty(), "Pre‑condition: map must be empty");
        // Act & Assert – no exception and map stays empty
        assertDoesNotThrow(() -> ClassUtilities.removePermanentClassAlias("anyAlias"));
        assertTrue(getAliasMap().isEmpty(), "Map should remain empty after removal attempt");
    }

    @Test
    @DisplayName("alias removal is case‑sensitive")
    void testCaseSensitivity() {
        // Arrange – add alias with specific case
        ClassUtilities.addPermanentClassAlias(Dummy.class, "TestAlias");
        Map<String, Class<?>> map = getAliasMap();
        assertTrue(map.containsKey("TestAlias"));
        // Act – try to remove using different case
        ClassUtilities.removePermanentClassAlias("testalias");
        // Assert – original key still present
        assertTrue(map.containsKey("TestAlias"), "Case‑sensitive map should not remove \"TestAlias\" when \"testalias\" is supplied");
        assertEquals(1, map.size(), "Map size must be unchanged");
    }

    @Test
    @DisplayName("removing one entry from multiple entries leaves others untouched")
    void testMultipleEntriesRemoval() {
        // Arrange – add three aliases
        ClassUtilities.addPermanentClassAlias(A.class, "A");
        ClassUtilities.addPermanentClassAlias(B.class, "B");
        ClassUtilities.addPermanentClassAlias(C.class, "C");
        Map<String, Class<?>> map = getAliasMap();
        assertEquals(3, map.size());
        // Act – remove entry "B"
        ClassUtilities.removePermanentClassAlias("B");
        // Assert – map now contains only "A" and "C"
        assertFalse(map.containsKey("B"), "Alias \"B\" should be removed");
        assertTrue(map.containsKey("A"));
        assertTrue(map.containsKey("C"));
        assertEquals(2, map.size(), "Map size should be reduced by one");
    }

    @Test
    @DisplayName("aliases with special characters are removed correctly")
    void testSpecialCharactersAliasRemoval() {
        String special = "#$%&*";
        ClassUtilities.addPermanentClassAlias(Dummy.class, special);
        Map<String, Class<?>> map = getAliasMap();
        assertTrue(map.containsKey(special));
        // Act
        ClassUtilities.removePermanentClassAlias(special);
        // Assert
        assertFalse(map.containsKey(special), "Special‑character alias should be removed");
    }

    @Test
    @DisplayName("whitespace‑only alias strings are handled like any other key")
    void testWhitespaceAliasRemoval() {
        String whitespace = "   ";
        ClassUtilities.addPermanentClassAlias(Dummy.class, whitespace);
        Map<String, Class<?>> map = getAliasMap();
        assertTrue(map.containsKey(whitespace));
        // Act
        ClassUtilities.removePermanentClassAlias(whitespace);
        // Assert
        assertFalse(map.containsKey(whitespace), "Whitespace‑only alias should be removed");
    }
}
