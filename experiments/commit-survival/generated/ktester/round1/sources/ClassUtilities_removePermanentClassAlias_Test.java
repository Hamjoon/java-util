package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_removePermanentClassAlias_Test {

    private static final String ALIAS_1 = "testAlias1";

    private static final String ALIAS_2 = "testAlias2";

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test. If an alias was left over from a previous
        // test, remove it so that the static map does not retain stale entries.
        ClassUtilities.removePermanentClassAlias(ALIAS_1);
        ClassUtilities.removePermanentClassAlias(ALIAS_2);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up any aliases that might have been added during a test.
        ClassUtilities.removePermanentClassAlias(ALIAS_1);
        ClassUtilities.removePermanentClassAlias(ALIAS_2);
        getNameToClassMap().clear();
    }

    @Test
    void removeExistingAliasShouldMakeAliasUnresolvable() {
        // given: an alias that maps to a known class
        ClassUtilities.addPermanentClassAlias(String.class, ALIAS_1);
        Class<?> beforeRemoval = ClassUtilities.forName(ALIAS_1, ClassUtilities.class.getClassLoader());
        assertNotNull(beforeRemoval, "Alias should resolve before removal");
        assertEquals(String.class, beforeRemoval, "Alias should resolve to the correct class");
        // when: the alias is removed
        ClassUtilities.removePermanentClassAlias(ALIAS_1);
        // then: the alias can no longer be resolved
        Class<?> afterRemoval = ClassUtilities.forName(ALIAS_1, ClassUtilities.class.getClassLoader());
        assertNull(afterRemoval, "Alias should be null after removal");
    }

    @Test
    void removingNonExistingAliasShouldNotThrowAndLeaveMapUntouched() {
        // given: an alias that has never been added
        Class<?> before = ClassUtilities.forName(ALIAS_2, ClassUtilities.class.getClassLoader());
        assertNull(before, "Alias should not exist before removal");
        // when: we attempt to remove it (should be a no‑op, no exception)
        assertDoesNotThrow(() -> ClassUtilities.removePermanentClassAlias(ALIAS_2));
        // then: the alias is still not resolvable
        Class<?> after = ClassUtilities.forName(ALIAS_2, ClassUtilities.class.getClassLoader());
        assertNull(after, "Alias should still be null after attempting removal of a non‑existent entry");
    }

    @Test
    void removingOneAliasShouldNotAffectOtherAliases() {
        // given: two distinct aliases
        ClassUtilities.addPermanentClassAlias(Integer.class, ALIAS_1);
        ClassUtilities.addPermanentClassAlias(Long.class, ALIAS_2);
        // sanity check both resolve correctly
        assertEquals(Integer.class, ClassUtilities.forName(ALIAS_1, ClassUtilities.class.getClassLoader()), "First alias should resolve to Integer");
        assertEquals(Long.class, ClassUtilities.forName(ALIAS_2, ClassUtilities.class.getClassLoader()), "Second alias should resolve to Long");
        // when: we remove only the first alias
        ClassUtilities.removePermanentClassAlias(ALIAS_1);
        // then: first alias is gone, second alias still works
        assertNull(ClassUtilities.forName(ALIAS_1, ClassUtilities.class.getClassLoader()), "First alias should be null after removal");
        assertEquals(Long.class, ClassUtilities.forName(ALIAS_2, ClassUtilities.class.getClassLoader()), "Second alias should remain resolvable");
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
            throw new RuntimeException(e);
        }
    }

    @Test
    void removeExistingAliasLeavesOtherEntriesUntouched() {
        // Add a known alias and a second, unrelated alias.
        ClassUtilities.addPermanentClassAlias(Double.class, "MyAlias");
        ClassUtilities.addPermanentClassAlias(Float.class, "OtherAlias");
        // Verify both are present.
        assertEquals(Double.class, ClassUtilities.forName("MyAlias", ClassUtilities.class.getClassLoader()));
        assertEquals(Float.class, ClassUtilities.forName("OtherAlias", ClassUtilities.class.getClassLoader()));
        // Remove the target alias.
        ClassUtilities.removePermanentClassAlias("MyAlias");
        // Verify removal and that the other entry is still resolvable.
        assertNull(ClassUtilities.forName("MyAlias", ClassUtilities.class.getClassLoader()));
        assertEquals(Float.class, ClassUtilities.forName("OtherAlias", ClassUtilities.class.getClassLoader()));
    }

    @Test
    void removeNonExistingAliasDoesNotAlterMap() {
        Map<String, Class<?>> map = getNameToClassMap();
        // set up a known entry
        map.put("Existing", String.class);
        int sizeBefore = map.size();
        ClassUtilities.removePermanentClassAlias("UnknownAlias");
        int sizeAfter = map.size();
        assertEquals(sizeBefore, sizeAfter, "Map size should stay unchanged");
        assertTrue(map.containsKey("Existing"));
    }

    @Test
    void removeNullAliasHandlesGracefully() {
        Map<String, Class<?>> map = getNameToClassMap();
        // Optionally put a null key.
        map.put(null, Integer.class);
        assertTrue(map.containsKey(null), "Map should contain a null key before removal");
        // The method should not throw.
        assertDoesNotThrow(() -> ClassUtilities.removePermanentClassAlias(null));
        // After removal the null key must be gone.
        assertFalse(map.containsKey(null), "Null key should be removed");
    }

    @Test
    void removeOneAliasAmongMultipleLeavesOthersIntact() {
        ClassUtilities.addPermanentClassAlias(Boolean.class, "AliasA");
        ClassUtilities.addPermanentClassAlias(Byte.class, "AliasB");
        ClassUtilities.addPermanentClassAlias(Character.class, "AliasC");
        // Remove middle alias.
        ClassUtilities.removePermanentClassAlias("AliasB");
        assertEquals(Boolean.class, ClassUtilities.forName("AliasA", ClassUtilities.class.getClassLoader()));
        assertNull(ClassUtilities.forName("AliasB", ClassUtilities.class.getClassLoader()));
        assertEquals(Character.class, ClassUtilities.forName("AliasC", ClassUtilities.class.getClassLoader()));
    }

    @Test
    void removeFromEmptyMapDoesNotThrowAndLeavesMapEmpty() {
        Map<String, Class<?>> map = getNameToClassMap();
        assertTrue(map.isEmpty(), "Map should start empty");
        assertDoesNotThrow(() -> ClassUtilities.removePermanentClassAlias("AnyAlias"));
        assertTrue(map.isEmpty(), "Map should remain empty after removal attempt");
    }

    @ParameterizedTest(name = "removeEmptyStringAlias '' (case {index})")
    @ValueSource(strings = { "" })
    void removeEmptyStringAlias(String alias) {
        ClassUtilities.addPermanentClassAlias(Long.class, alias);
        assertEquals(Long.class, ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));
        ClassUtilities.removePermanentClassAlias(alias);
        assertNull(ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));
    }

    @ParameterizedTest(name = "removeWhitespaceAlias \"{0}\"")
    @ValueSource(strings = { "   ", "\t", "\n" })
    void removeWhitespaceAlias(String alias) {
        ClassUtilities.addPermanentClassAlias(Integer.class, alias);
        assertEquals(Integer.class, ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));
        ClassUtilities.removePermanentClassAlias(alias);
        assertNull(ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));
    }

    @ParameterizedTest(name = "removeSpecialCharAlias \"{0}\"")
    @ValueSource(strings = { "@#%$!", "~`|\\", "©®™" })
    void removeSpecialCharacterAlias(String alias) {
        ClassUtilities.addPermanentClassAlias(Float.class, alias);
        assertEquals(Float.class, ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));
        ClassUtilities.removePermanentClassAlias(alias);
        assertNull(ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader()));
    }

    @Test
    void removeVeryLongAlias() {
        // 1 KiB string
        String longAlias = "a".repeat(1024);
        ClassUtilities.addPermanentClassAlias(Double.class, longAlias);
        assertEquals(Double.class, ClassUtilities.forName(longAlias, ClassUtilities.class.getClassLoader()));
        ClassUtilities.removePermanentClassAlias(longAlias);
        assertNull(ClassUtilities.forName(longAlias, ClassUtilities.class.getClassLoader()));
    }

    @Test
    void removeAliasMappedToNullValue() throws Exception {
        Map<String, Class<?>> map = getNameToClassMap();
        // Directly insert a null value (no public API for this).
        map.put("NullValueAlias", null);
        assertTrue(map.containsKey("NullValueAlias"));
        assertNull(map.get("NullValueAlias"));
        // Removal should succeed and the key should disappear.
        ClassUtilities.removePermanentClassAlias("NullValueAlias");
        assertFalse(map.containsKey("NullValueAlias"));
    }

    @Test
    void removeAliasIsCaseSensitive() {
        // Two keys differing only by case.
        ClassUtilities.addPermanentClassAlias(String.class, "MyAlias");
        ClassUtilities.addPermanentClassAlias(Integer.class, "myalias");
        // Remove the lower‑case version.
        ClassUtilities.removePermanentClassAlias("myalias");
        // Verify only the lower‑case key is gone.
        assertNull(ClassUtilities.forName("myalias", ClassUtilities.class.getClassLoader()));
        assertEquals(String.class, ClassUtilities.forName("MyAlias", ClassUtilities.class.getClassLoader()));
    }

    @Test
    void reAddAliasAfterRemovalWorks() {
        // Initial addition.
        ClassUtilities.addPermanentClassAlias(Long.class, "ReusableAlias");
        assertEquals(Long.class, ClassUtilities.forName("ReusableAlias", ClassUtilities.class.getClassLoader()));
        // Remove it.
        ClassUtilities.removePermanentClassAlias("ReusableAlias");
        assertNull(ClassUtilities.forName("ReusableAlias", ClassUtilities.class.getClassLoader()));
        // Re‑add with a different class to ensure the map accepts the key again.
        ClassUtilities.addPermanentClassAlias(Integer.class, "ReusableAlias");
        assertEquals(Integer.class, ClassUtilities.forName("ReusableAlias", ClassUtilities.class.getClassLoader()));
    }
}
