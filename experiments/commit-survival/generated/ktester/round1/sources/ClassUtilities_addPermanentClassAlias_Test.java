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
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_addPermanentClassAlias_Test {

    /**
     * Holds the alias that was added in the current test so it can be cleaned up afterwards.
     */
    private String currentAlias;

    @BeforeEach
    void setupBeforeEach() {
        // Ensure we start each test with a clean state.
        currentAlias = null;
    }

    @AfterEach
    void teardownAfterEach() {
        // Remove the alias that might have been added during the test to avoid cross‑test contamination.
        if (currentAlias != null) {
            ClassUtilities.removePermanentClassAlias(currentAlias);
            currentAlias = null;
        }
    }

    @Test
    void addPermanentClassAlias_ShouldBeRetrievableViaForName() {
        // GIVEN
        String alias = "stringAlias";
        Class<?> target = String.class;
        // remember for cleanup
        currentAlias = alias;
        // WHEN
        ClassUtilities.addPermanentClassAlias(target, alias);
        Class<?> result = ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader());
        // THEN
        assertNotNull(result, "Class retrieved by alias should not be null");
        assertEquals(target, result, "Alias should resolve to the class that was added");
    }

    @Test
    void addPermanentClassAlias_OverwriteExistingAlias_ShouldReturnLatestClass() {
        // GIVEN
        String alias = "numericAlias";
        Class<?> first = Integer.class;
        Class<?> second = Long.class;
        // remember for cleanup
        currentAlias = alias;
        // WHEN – first registration
        ClassUtilities.addPermanentClassAlias(first, alias);
        Class<?> firstResult = ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader());
        // WHEN – second registration (same alias, different class)
        ClassUtilities.addPermanentClassAlias(second, alias);
        Class<?> secondResult = ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader());
        // THEN
        assertEquals(first, firstResult, "First registration should resolve to the first class");
        assertEquals(second, secondResult, "Second registration should overwrite and resolve to the second class");
    }

    @Test
    void removePermanentClassAlias_ShouldNoLongerResolveAlias() {
        // GIVEN
        String alias = "mapAlias";
        Class<?> target = java.util.HashMap.class;
        // remember for cleanup (in case removal fails)
        currentAlias = alias;
        // WHEN – add alias
        ClassUtilities.addPermanentClassAlias(target, alias);
        Class<?> beforeRemoval = ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader());
        assertEquals(target, beforeRemoval, "Alias must resolve before removal");
        // AND – remove alias
        ClassUtilities.removePermanentClassAlias(alias);
        // clear the field because we have already removed it
        currentAlias = null;
        // THEN – alias should no longer be found
        Class<?> afterRemoval = ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader());
        assertNull(afterRemoval, "Alias should not resolve after it has been removed");
    }

    @Test
    void addPermanentClassAlias_NullAlias_ShouldBeHandledByForName() {
        // GIVEN
        String alias = null;
        Class<?> target = Boolean.class;
        // No need to store alias for cleanup because null cannot be removed.
        // WHEN
        ClassUtilities.addPermanentClassAlias(target, alias);
        Class<?> result = ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader());
        // THEN
        // HashMap permits a null key, so forName should return the class associated with null.
        assertEquals(target, result, "Null alias should resolve to the class that was added");
        // Cleanup: remove the null alias to keep map clean for other tests.
        ClassUtilities.removePermanentClassAlias(null);
    }

    @Test
    void addPermanentClassAlias_NullClass_ShouldStoreNullValue() {
        // GIVEN
        String alias = "nullClassAlias";
        // remember for cleanup
        currentAlias = alias;
        Class<?> target = null;
        // WHEN
        ClassUtilities.addPermanentClassAlias(target, alias);
        Class<?> result = ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader());
        // THEN
        assertNull(result, "Alias associated with a null class should resolve to null");
    }

    /* --------------------------------------------------------------------- */
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

    @Test
    void addPermanentClassAlias_BasicFunctionality_MultipleDistinctEntries() {
        // GIVEN
        String alias1 = "MyStringAlias";
        Class<?> class1 = String.class;
        String alias2 = "DateAlias";
        Class<?> class2 = java.util.Date.class;
        // Register both aliases
        ClassUtilities.addPermanentClassAlias(class1, alias1);
        ClassUtilities.addPermanentClassAlias(class2, alias2);
        // Remember the last alias for the generic @AfterEach cleanup
        currentAlias = alias2;
        // WHEN – retrieve the internal map
        Map<String, Class<?>> map = getNameToClassMap();
        // THEN – both entries must be present
        assertEquals(class1, map.get(alias1), "Map should contain first alias->class mapping");
        assertEquals(class2, map.get(alias2), "Map should contain second alias->class mapping");
    }

    @Test
    void addPermanentClassAlias_NullHandling_NullClassWithNonNullAlias() {
        // GIVEN
        String alias = "NullClassAlias";
        currentAlias = alias;
        Class<?> clazz = null;
        // WHEN
        ClassUtilities.addPermanentClassAlias(clazz, alias);
        Map<String, Class<?>> map = getNameToClassMap();
        // THEN
        assertTrue(map.containsKey(alias), "Map must contain the alias key");
        assertNull(map.get(alias), "Map value for the alias should be null");
    }

    @Test
    void addPermanentClassAlias_NullHandling_NullAliasWithNonNullClass() {
        // GIVEN
        String alias = null;
        Class<?> clazz = String.class;
        // WHEN
        ClassUtilities.addPermanentClassAlias(clazz, alias);
        Map<String, Class<?>> map = getNameToClassMap();
        // THEN
        assertTrue(map.containsKey(null), "Map must contain a null key");
        assertEquals(clazz, map.get(null), "Null key should map to the provided class");
        // Cleanup
        ClassUtilities.removePermanentClassAlias(null);
    }

    @Test
    void addPermanentClassAlias_NullHandling_BothNull() {
        // GIVEN
        String alias = null;
        Class<?> clazz = null;
        // WHEN
        ClassUtilities.addPermanentClassAlias(clazz, alias);
        Map<String, Class<?>> map = getNameToClassMap();
        // THEN
        assertTrue(map.containsKey(null), "Map must contain a null key");
        assertNull(map.get(null), "Null key should map to null value");
        // Cleanup
        ClassUtilities.removePermanentClassAlias(null);
    }

    @Test
    void addPermanentClassAlias_DuplicateAlias_Overwrite() {
        // GIVEN
        String alias = "DupAlias";
        Class<?> first = String.class;
        Class<?> second = Integer.class;
        currentAlias = alias;
        // WHEN – first insertion
        ClassUtilities.addPermanentClassAlias(first, alias);
        Class<?> firstResult = ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader());
        // WHEN – second insertion (same alias, different class)
        ClassUtilities.addPermanentClassAlias(second, alias);
        Class<?> secondResult = ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader());
        // THEN
        assertEquals(first, firstResult, "First registration should resolve to the first class");
        assertEquals(second, secondResult, "Second registration should overwrite the previous mapping");
    }

    @Test
    void addPermanentClassAlias_IdempotentInsertion_NoChangeOnDuplicate() {
        // GIVEN
        String alias = "IdemAlias";
        Class<?> clazz = java.util.Date.class;
        currentAlias = alias;
        // WHEN – first insertion
        ClassUtilities.addPermanentClassAlias(clazz, alias);
        Map<String, Class<?>> mapAfterFirst = getNameToClassMap();
        int sizeAfterFirst = mapAfterFirst.size();
        // WHEN – duplicate insertion of same pair
        ClassUtilities.addPermanentClassAlias(clazz, alias);
        Map<String, Class<?>> mapAfterSecond = getNameToClassMap();
        int sizeAfterSecond = mapAfterSecond.size();
        // THEN – map size unchanged and entry identical
        assertEquals(sizeAfterFirst, sizeAfterSecond, "Map size should not increase on idempotent insertion");
        assertEquals(clazz, mapAfterSecond.get(alias), "Alias should still map to the original class");
    }

    @Test
    void addPermanentClassAlias_PrimitiveClassAlias() {
        // GIVEN
        String alias = "intAlias";
        Class<?> primitive = int.class;
        currentAlias = alias;
        // WHEN
        ClassUtilities.addPermanentClassAlias(primitive, alias);
        Class<?> result = ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader());
        // THEN
        assertEquals(primitive, result, "Alias should resolve to the primitive class");
        Map<String, Class<?>> map = getNameToClassMap();
        assertEquals(primitive, map.get(alias), "Internal map must store the primitive class as value");
    }

    @Test
    void addPermanentClassAlias_ArrayClassAlias() {
        // GIVEN
        String alias = "StringArrayAlias";
        Class<?> arrayClazz = String[].class;
        currentAlias = alias;
        // WHEN
        ClassUtilities.addPermanentClassAlias(arrayClazz, alias);
        Class<?> result = ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader());
        // THEN
        assertEquals(arrayClazz, result, "Alias should resolve to the array class");
        Map<String, Class<?>> map = getNameToClassMap();
        assertEquals(arrayClazz, map.get(alias), "Internal map must store the array class as value");
    }

    @Test
    void addPermanentClassAlias_SpecialCharactersInAlias() {
        // GIVEN
        String alias = "spécial-αβγ";
        Class<?> clazz = java.util.Date.class;
        currentAlias = alias;
        // WHEN
        ClassUtilities.addPermanentClassAlias(clazz, alias);
        Class<?> result = ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader());
        // THEN
        assertEquals(clazz, result, "Alias with Unicode characters should resolve correctly");
        Map<String, Class<?>> map = getNameToClassMap();
        assertEquals(clazz, map.get(alias), "Internal map must store the Unicode alias unchanged");
    }

    @Test
    void addPermanentClassAlias_AliasIdenticalToFQCN() {
        // GIVEN
        // Alias string equals a fully qualified class name of another class.
        // same as String's FQCN
        String alias = "java.lang.String";
        // but we map it to Integer
        Class<?> clazz = Integer.class;
        currentAlias = alias;
        // WHEN
        ClassUtilities.addPermanentClassAlias(clazz, alias);
        Class<?> result = ClassUtilities.forName(alias, ClassUtilities.class.getClassLoader());
        // THEN
        assertEquals(clazz, result, "Alias that matches another class name should resolve to the mapped class");
        Map<String, Class<?>> map = getNameToClassMap();
        assertEquals(clazz, map.get(alias), "Map must contain the explicit alias mapping, not the original class");
    }
}
