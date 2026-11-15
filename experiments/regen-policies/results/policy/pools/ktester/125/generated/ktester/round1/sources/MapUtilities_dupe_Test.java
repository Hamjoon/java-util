package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// Mockito not used but kept to match the template
@ExtendWith(MockitoExtension.class)
class MapUtilities_dupe_Test {

    /**
     * Sample data used by several tests
     */
    private Map<Class<?>, Set<String>> sourceMap;

    /**
     * Simple placeholder classes used as map keys
     */
    private static class Foo {
    }

    private static class Bar {
    }

    private static class Baz {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a reproducible source map for each test
        sourceMap = new LinkedHashMap<>();
        Set<String> stringSet = new LinkedHashSet<>(Arrays.asList("alpha", "beta"));
        Set<String> integerSet = new LinkedHashSet<>(Collections.singletonList("42"));
        sourceMap.put(String.class, stringSet);
        sourceMap.put(Integer.class, integerSet);
        sourceMap = new LinkedHashMap<>();
        sourceMap.put(Foo.class, new LinkedHashSet<>(Arrays.asList("foo1", "foo2")));
        sourceMap.put(Bar.class, new LinkedHashSet<>(Collections.singletonList("bar1")));
    }

    @AfterEach
    void teardownAfterEach() {
        // help GC and ensure test isolation
        sourceMap = null;
        sourceMap = null;
    }

    @Test
    void dupeReturnsMutableCopyWhenUnmodifiableIsFalse() {
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(sourceMap, false);
        // The returned map must be a different instance
        assertNotSame(sourceMap, copy);
        // It must contain the same keys
        assertEquals(sourceMap.keySet(), copy.keySet());
        // Each set must also be a new instance but contain the same elements
        for (Class<?> key : sourceMap.keySet()) {
            Set<String> originalSet = sourceMap.get(key);
            Set<String> copiedSet = copy.get(key);
            assertNotSame(originalSet, copiedSet);
            assertEquals(originalSet, copiedSet);
        }
        // Mutating the original map or its sets must NOT affect the copy
        sourceMap.put(Double.class, new LinkedHashSet<>(Collections.singletonList("pi")));
        assertFalse(copy.containsKey(Double.class));
        sourceMap.get(String.class).add("gamma");
        assertFalse(copy.get(String.class).contains("gamma"));
        // Mutating the copy must not affect the original
        copy.get(Integer.class).remove("42");
        assertTrue(sourceMap.get(Integer.class).contains("42"));
    }

    @Test
    void dupeReturnsUnmodifiableCopyWhenUnmodifiableIsTrue() {
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(sourceMap, true);
        // The map itself should be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> copy.put(Long.class, new LinkedHashSet<>()));
        // Each set inside the map should also be unmodifiable
        for (Set<String> set : copy.values()) {
            assertThrows(UnsupportedOperationException.class, () -> set.add("new"));
        }
        // Content must match the source
        assertEquals(sourceMap.keySet(), copy.keySet());
        for (Class<?> key : sourceMap.keySet()) {
            assertEquals(sourceMap.get(key), copy.get(key));
        }
        // Verify that changes to the original after dupe do not affect the unmodifiable copy
        sourceMap.get(String.class).add("delta");
        assertFalse(copy.get(String.class).contains("delta"));
    }

    @Test
    void dupeHandlesEmptyMap() {
        Map<Class<?>, Set<String>> empty = Collections.emptyMap();
        Map<Class<?>, Set<String>> mutableCopy = MapUtilities.dupe(empty, false);
        assertTrue(mutableCopy.isEmpty());
        assertDoesNotThrow(() -> mutableCopy.put(Boolean.class, new LinkedHashSet<>()));
        Map<Class<?>, Set<String>> unmodifiableCopy = MapUtilities.dupe(empty, true);
        assertTrue(unmodifiableCopy.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableCopy.put(Boolean.class, new LinkedHashSet<>()));
    }

    @Test
    void dupeDoesNotAllowNullKeyOrValueInReturnedMapWhenUnmodifiable() {
        // Build a map containing a null key and a null value set
        Map<Class<?>, Set<String>> mapWithNulls = new LinkedHashMap<>();
        mapWithNulls.put(null, null);
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(mapWithNulls, true);
        // The copy should preserve the null entries (the method does not filter them)
        assertTrue(copy.containsKey(null));
        assertNull(copy.get(null));
        // The map itself is unmodifiable, but accessing null entries is allowed
        assertDoesNotThrow(() -> copy.get(null));
    }

    @ParameterizedTest(name = "dupe(null, {0}) throws NPE")
    @CsvSource({ "true", "false" })
    void dupeThrowsNPEWhenOtherIsNull(boolean unmodifiable) {
        assertThrows(NullPointerException.class, () -> MapUtilities.dupe(null, unmodifiable));
    }

    @ParameterizedTest(name = "dupe(mapWithNullValue, {0}) throws NPE")
    @CsvSource({ "true", "false" })
    void dupeThrowsNPEWhenValueSetIsNull(boolean unmodifiable) {
        Map<Class<?>, Set<String>> mapWithNullValue = new LinkedHashMap<>();
        // value set is null
        mapWithNullValue.put(Foo.class, null);
        assertThrows(NullPointerException.class, () -> MapUtilities.dupe(mapWithNullValue, unmodifiable));
    }

    @Test
    void dupeCopiesNullKeyWhenMutable() {
        Map<Class<?>, Set<String>> mapWithNullKey = new LinkedHashMap<>();
        mapWithNullKey.put(null, new LinkedHashSet<>(Collections.singleton("value1")));
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(mapWithNullKey, false);
        // null key must be present
        assertTrue(copy.containsKey(null));
        // the associated set must be mutable and contain the original element
        Set<String> set = copy.get(null);
        assertNotNull(set);
        assertTrue(set.contains("value1"));
        assertDoesNotThrow(() -> set.add("value2"));
        assertTrue(set.contains("value2"));
    }

    @Test
    void dupeCopiesNullKeyWhenUnmodifiable() {
        Map<Class<?>, Set<String>> mapWithNullKey = new LinkedHashMap<>();
        mapWithNullKey.put(null, new LinkedHashSet<>(Collections.singleton("value1")));
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(mapWithNullKey, true);
        // null key must be present
        assertTrue(copy.containsKey(null));
        // the map itself must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> copy.put(Foo.class, new LinkedHashSet<>()));
        // the set for the null key must be unmodifiable
        Set<String> set = copy.get(null);
        assertNotNull(set);
        assertThrows(UnsupportedOperationException.class, () -> set.add("value2"));
        // original value still present
        assertTrue(set.contains("value1"));
    }

    @Test
    void dupePreservesInsertionOrder() {
        Map<Class<?>, Set<String>> ordered = new LinkedHashMap<>();
        ordered.put(Foo.class, new LinkedHashSet<>(Arrays.asList("a", "b")));
        ordered.put(Bar.class, new LinkedHashSet<>(Collections.singleton("c")));
        ordered.put(Baz.class, new LinkedHashSet<>(Arrays.asList("x", "y", "z")));
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(ordered, false);
        // LinkedHashMap preserves iteration order – compare the key lists directly
        List<Class<?>> expectedOrder = new ArrayList<>(ordered.keySet());
        List<Class<?>> actualOrder = new ArrayList<>(copy.keySet());
        assertEquals(expectedOrder, actualOrder, "Key order should be preserved");
        // Also verify that each set preserves its insertion order
        Iterator<String> expectedIter = ordered.get(Foo.class).iterator();
        Iterator<String> actualIter = copy.get(Foo.class).iterator();
        while (expectedIter.hasNext()) {
            assertEquals(expectedIter.next(), actualIter.next());
        }
    }

    @Test
    void dupeMutableCopyIsDeepAndIndependent() {
        Map<Class<?>, Set<String>> original = new LinkedHashMap<>();
        original.put(Foo.class, new LinkedHashSet<>(Collections.singleton("orig")));
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, false);
        // Mutate original map and its set
        original.put(Bar.class, new LinkedHashSet<>(Collections.singleton("newBar")));
        original.get(Foo.class).add("addedToOriginal");
        // Copy must stay unchanged
        assertFalse(copy.containsKey(Bar.class));
        assertEquals(Collections.singleton("orig"), copy.get(Foo.class));
        assertFalse(copy.get(Foo.class).contains("addedToOriginal"));
    }

    @Test
    void dupeUnmodifiableCopyIsSnapshotAndIndependent() {
        Map<Class<?>, Set<String>> original = new LinkedHashMap<>();
        original.put(Foo.class, new LinkedHashSet<>(Collections.singleton("orig")));
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, true);
        // Mutate original after dupe
        original.get(Foo.class).add("addedLater");
        // Snapshot in copy must not see the later addition
        Set<String> copiedSet = copy.get(Foo.class);
        assertEquals(Collections.singleton("orig"), copiedSet);
        assertThrows(UnsupportedOperationException.class, () -> copiedSet.add("shouldFail"));
    }

    @Test
    void dupeEmptyMapMutable() {
        Map<Class<?>, Set<String>> empty = Collections.emptyMap();
        Map<Class<?>, Set<String>> result = MapUtilities.dupe(empty, false);
        assertTrue(result.isEmpty());
        assertDoesNotThrow(() -> result.put(Foo.class, new LinkedHashSet<>()));
    }

    @Test
    void dupeEmptyMapUnmodifiable() {
        Map<Class<?>, Set<String>> empty = Collections.emptyMap();
        Map<Class<?>, Set<String>> result = MapUtilities.dupe(empty, true);
        assertTrue(result.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.put(Foo.class, new LinkedHashSet<>()));
    }
}
