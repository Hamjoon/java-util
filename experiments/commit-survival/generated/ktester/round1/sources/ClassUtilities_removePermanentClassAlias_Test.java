package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_removePermanentClassAlias_Test {

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test by removing any aliases that may have been left behind
        // from a previous test run. Removing a non‑existent alias is a no‑op.
        ClassUtilities.removePermanentClassAlias("aliasA");
        ClassUtilities.removePermanentClassAlias("aliasB");
        ClassUtilities.removePermanentClassAlias("aliasC");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up any aliases that a test may have added.
        ClassUtilities.removePermanentClassAlias("aliasA");
        ClassUtilities.removePermanentClassAlias("aliasB");
        ClassUtilities.removePermanentClassAlias("aliasC");
        ClassUtilities.removePermanentClassAlias("myAlias");
        ClassUtilities.removePermanentClassAlias("nonExisting");
        ClassUtilities.removePermanentClassAlias("");
        ClassUtilities.removePermanentClassAlias("aliasOne");
        ClassUtilities.removePermanentClassAlias("aliasTwo");
        ClassUtilities.removePermanentClassAlias("aliasD");
    }

    private static Stream<Arguments> singleAliasRemovalProvider() {
        return Stream.of(// alias exists in the map
        Arguments.of("myAlias", true, "Alias exists in the map: after adding a permanent class alias, calling removePermanentClassAlias should delete that entry."), // alias does not exist
        Arguments.of("nonExisting", false, "Alias does not exist: removal of a non‑present key should be a no‑op."), // null alias
        Arguments.of(null, false, "Null alias: verify that passing null does not cause a NullPointerException and leaves the map untouched."), // empty string alias (test both presence and absence in separate runs)
        Arguments.of("", false, "Empty string alias not present: removal should be a no‑op."), Arguments.of("", true, "Empty string alias present: removal should delete the entry."));
    }

    @Test
    void removeExistingAliasShouldMakeForNameReturnNull() {
        // given
        ClassUtilities.addPermanentClassAlias(String.class, "aliasA");
        Class<?> beforeRemoval = ClassUtilities.forName("aliasA", ClassUtilities.class.getClassLoader());
        assertNotNull(beforeRemoval, "Alias should resolve before removal");
        assertEquals(String.class, beforeRemoval, "Resolved class should be String");
        // when
        ClassUtilities.removePermanentClassAlias("aliasA");
        // then
        Class<?> afterRemoval = ClassUtilities.forName("aliasA", ClassUtilities.class.getClassLoader());
        assertNull(afterRemoval, "Alias should no longer resolve after removal");
    }

    @Test
    void removingNonExistingAliasShouldNotThrowAndLeaveMapUntouched() {
        // given
        ClassUtilities.addPermanentClassAlias(Integer.class, "aliasB");
        Class<?> before = ClassUtilities.forName("aliasB", ClassUtilities.class.getClassLoader());
        assertEquals(Integer.class, before, "AliasB should resolve to Integer");
        // when / then
        assertDoesNotThrow(() -> ClassUtilities.removePermanentClassAlias("nonExistingAlias"), "Removing a non‑existent alias should not throw an exception");
        // then – existing alias still works
        Class<?> stillThere = ClassUtilities.forName("aliasB", ClassUtilities.class.getClassLoader());
        assertEquals(Integer.class, stillThere, "Existing alias should still resolve after attempting to remove a different alias");
    }

    @Test
    void removingOneAliasShouldNotAffectOtherAliases() {
        // given
        ClassUtilities.addPermanentClassAlias(Double.class, "aliasC");
        ClassUtilities.addPermanentClassAlias(Float.class, "aliasD");
        Class<?> doubleBefore = ClassUtilities.forName("aliasC", ClassUtilities.class.getClassLoader());
        Class<?> floatBefore = ClassUtilities.forName("aliasD", ClassUtilities.class.getClassLoader());
        assertEquals(Double.class, doubleBefore, "aliasC should resolve to Double");
        assertEquals(Float.class, floatBefore, "aliasD should resolve to Float");
        // when
        ClassUtilities.removePermanentClassAlias("aliasC");
        // then
        Class<?> doubleAfter = ClassUtilities.forName("aliasC", ClassUtilities.class.getClassLoader());
        Class<?> floatAfter = ClassUtilities.forName("aliasD", ClassUtilities.class.getClassLoader());
        assertNull(doubleAfter, "aliasC should be removed");
        assertEquals(Float.class, floatAfter, "aliasD should remain unaffected");
    }

    /* --------------------------------------------------------------
     *  Helper to obtain the private static map via reflection.
     * -------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static Map<String, Class<?>> getNameToClassMap() throws Exception {
        Field field = ClassUtilities.class.getDeclaredField("nameToClass");
        field.setAccessible(true);
        return (Map<String, Class<?>>) field.get(null);
    }

    /**
     * Test removal of a single alias under various conditions:
     * - existing alias ("myAlias")
     * - non‑existing alias ("nonExisting")
     * - null alias
     * - empty‑string alias
     */
    @ParameterizedTest(name = "{index}: remove alias = {0}")
    @MethodSource("singleAliasRemovalProvider")
    void removePermanentClassAlias_singleAliasScenarios(String alias, boolean aliasWasAdded, String description) throws Exception {
        // Arrange ----------------------------------------------------
        if (aliasWasAdded) {
            // add an alias only when the test case expects it to be present
            ClassUtilities.addPermanentClassAlias(Boolean.class, alias);
        }
        Map<String, Class<?>> mapBefore = getNameToClassMap();
        int sizeBefore = mapBefore.size();
        boolean containsBefore = mapBefore.containsKey(alias);
        // Act --------------------------------------------------------
        // The method under test must not throw for any input, including null.
        assertDoesNotThrow(() -> ClassUtilities.removePermanentClassAlias(alias), "removePermanentClassAlias should never throw, even for null or missing keys");
        // Assert -----------------------------------------------------
        Map<String, Class<?>> mapAfter = getNameToClassMap();
        int sizeAfter = mapAfter.size();
        boolean containsAfter = mapAfter.containsKey(alias);
        if (alias == null) {
            // HashMap.remove(null) returns null and does not modify the map.
            assertEquals(sizeBefore, sizeAfter, "Map size must stay unchanged when null is passed");
            assertEquals(containsBefore, containsAfter, "Presence of null key must stay unchanged");
        } else if (alias.isEmpty()) {
            // Empty string may or may not have been present.
            if (containsBefore) {
                assertFalse(containsAfter, "Empty string alias should be removed if it existed");
                assertEquals(sizeBefore - 1, sizeAfter, "Map size should decrease by one when empty string key existed");
            } else {
                assertFalse(containsAfter, "Empty string alias was not present, still not present after removal");
                assertEquals(sizeBefore, sizeAfter, "Map size unchanged when empty string key did not exist");
            }
        } else {
            // Normal alias handling
            if (aliasWasAdded) {
                assertFalse(containsAfter, "Alias should be removed from the map");
                assertEquals(sizeBefore - 1, sizeAfter, "Map size should decrease by one after successful removal");
            } else {
                assertEquals(containsBefore, containsAfter, "Map content must stay unchanged for non‑existing alias");
                assertEquals(sizeBefore, sizeAfter, "Map size must stay unchanged for non‑existing alias");
            }
        }
    }

    /**
     * Test that removing one of two distinct aliases only affects the targeted entry.
     */
    @Test
    void removePermanentClassAlias_multipleEntries_removalOfFirstAlias() throws Exception {
        // Arrange – add two distinct aliases
        ClassUtilities.addPermanentClassAlias(Long.class, "aliasOne");
        ClassUtilities.addPermanentClassAlias(Short.class, "aliasTwo");
        Map<String, Class<?>> map = getNameToClassMap();
        assertEquals(2, map.size(), "Setup sanity check – map should contain two entries");
        assertTrue(map.containsKey("aliasOne"));
        assertTrue(map.containsKey("aliasTwo"));
        // Act – remove the first alias
        ClassUtilities.removePermanentClassAlias("aliasOne");
        // Assert – only the targeted entry is gone
        Map<String, Class<?>> after = getNameToClassMap();
        assertEquals(1, after.size(), "Map size should decrease by one after removal");
        assertFalse(after.containsKey("aliasOne"), "aliasOne must be removed");
        assertTrue(after.containsKey("aliasTwo"), "aliasTwo must remain");
        assertEquals(Short.class, after.get("aliasTwo"));
    }

    /**
     * Test that removing the second alias works symmetrically.
     */
    @Test
    void removePermanentClassAlias_multipleEntries_removalOfSecondAlias() throws Exception {
        // Arrange – add two distinct aliases (order reversed compared to previous test)
        ClassUtilities.addPermanentClassAlias(Byte.class, "aliasTwo");
        ClassUtilities.addPermanentClassAlias(Character.class, "aliasOne");
        Map<String, Class<?>> map = getNameToClassMap();
        assertEquals(2, map.size(), "Setup sanity check – map should contain two entries");
        assertTrue(map.containsKey("aliasOne"));
        assertTrue(map.containsKey("aliasTwo"));
        // Act – remove the second alias
        ClassUtilities.removePermanentClassAlias("aliasTwo");
        // Assert – only the targeted entry is gone
        Map<String, Class<?>> after = getNameToClassMap();
        assertEquals(1, after.size(), "Map size should decrease by one after removal");
        assertFalse(after.containsKey("aliasTwo"), "aliasTwo must be removed");
        assertTrue(after.containsKey("aliasOne"), "aliasOne must remain");
        assertEquals(Character.class, after.get("aliasOne"));
    }
}
