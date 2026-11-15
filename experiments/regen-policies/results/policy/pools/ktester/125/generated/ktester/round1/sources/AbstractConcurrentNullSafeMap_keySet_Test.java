package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_keySet_Test {

    private TestConcurrentNullSafeMap<String, String> map;

    /**
     * Simple concrete implementation used only for testing.
     */
    private static class TestConcurrentNullSafeMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestConcurrentNullSafeMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    /**
     * Concrete map with <String,Integer> used for type‑safety edge cases.
     */
    private static class StringIntegerMap extends AbstractConcurrentNullSafeMap<String, Integer> {

        StringIntegerMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new TestConcurrentNullSafeMap<>();
        // Populate with a mixture of normal and null keys.
        map.put("alpha", "A");
        map.put("beta", "B");
        map.put(null, "NULL_KEY");
        // null value – should not affect keySet behaviour
        map.put("gamma", null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure map is cleared after each test to avoid cross‑test contamination.
        map.clear();
    }

    /**
     * Helper to create a fresh map with two entries for size‑related tests.
     */
    private TestConcurrentNullSafeMap<String, Integer> freshTwoEntryMap() {
        TestConcurrentNullSafeMap<String, Integer> m = new TestConcurrentNullSafeMap<>();
        m.put("a", 1);
        m.put("b", 2);
        return m;
    }

    private static Stream<Arguments> sizeTestProvider() {
        TestConcurrentNullSafeMap<String, Integer> empty = new TestConcurrentNullSafeMap<>();
        TestConcurrentNullSafeMap<String, Integer> two = new AbstractConcurrentNullSafeMap_keySet_Test().freshTwoEntryMap();
        return Stream.of(Arguments.of("After putting two entries (\"a\"->1, \"b\"->2)", two, 2), Arguments.of("When the map is empty", empty, 0));
    }

    private static Stream<Arguments> containsTestProvider() {
        return Stream.of(Arguments.of("Map contains \"alpha\" key", "alpha", true), Arguments.of("Map contains null key", null, true), Arguments.of("Map does not contain \"missing\" key", "missing", false));
    }

    private static Stream<Arguments> removeTestProvider() {
        return Stream.of(Arguments.of("Remove existing key \"toRemove\"", "toRemove", true), Arguments.of("Remove null key", null, true), Arguments.of("Remove absent key \"absent\"", "absent", false));
    }

    @Test
    void keySetSizeReflectsMapSize() {
        Set<String> ks = map.keySet();
        // Map contains 4 entries (including a null key)
        assertEquals(4, ks.size(), "keySet size should equal map size");
    }

    @Test
    void iteratorReturnsAllKeysIncludingNull() {
        Set<String> ks = map.keySet();
        Set<String> iterated = new HashSet<>();
        for (String k : ks) {
            iterated.add(k);
        }
        // Expected keys: "alpha", "beta", "gamma", null
        Set<String> expected = new HashSet<>(Arrays.asList("alpha", "beta", "gamma", null));
        assertEquals(expected, iterated, "Iterator should return all keys, including null");
    }

    @Test
    void containsWorksForNullAndNonNullKeys() {
        Set<String> ks = map.keySet();
        assertTrue(ks.contains(null), "keySet should contain null key");
        assertTrue(ks.contains("alpha"), "keySet should contain existing key");
        assertFalse(ks.contains("delta"), "keySet should not contain absent key");
    }

    @Test
    void removeViaKeySetUpdatesUnderlyingMap() {
        Set<String> ks = map.keySet();
        // Remove a normal key
        assertTrue(ks.remove("beta"), "remove should return true for existing key");
        assertFalse(map.containsKey("beta"), "Underlying map should no longer contain removed key");
        assertEquals(3, ks.size(), "Size should shrink after removal");
        // Remove the null key
        assertTrue(ks.remove(null), "remove should handle null key");
        assertFalse(map.containsKey(null), "Underlying map should no longer contain null key");
        assertEquals(2, ks.size(), "Size should reflect second removal");
    }

    @Test
    void clearViaKeySetEmptiesMap() {
        Set<String> ks = map.keySet();
        ks.clear();
        assertTrue(map.isEmpty(), "Map should be empty after keySet.clear()");
        assertEquals(0, ks.size(), "keySet size should be zero after clear");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("sizeTestProvider")
    void keySetSizeScenarios(String description, TestConcurrentNullSafeMap<String, Integer> mapUnderTest, int expectedSize) {
        assertEquals(expectedSize, mapUnderTest.keySet().size(), "keySet size should match expected for scenario: " + description);
    }

    @Test
    void iteratorReturnsUnmaskedKeysInOrder() {
        TestConcurrentNullSafeMap<String, Integer> m = freshTwoEntryMap();
        m.put(null, 3);
        List<String> expected = new ArrayList<>(Arrays.asList("a", "b", null));
        List<String> actual = new ArrayList<>();
        for (String k : m.keySet()) {
            actual.add(k);
        }
        assertEquals(expected, actual, "Iterator should yield keys in map's iteration order with null unmasked");
    }

    @Test
    void iteratorNextThrowsNoSuchElementExceptionWhenExhausted() {
        TestConcurrentNullSafeMap<String, Integer> m = freshTwoEntryMap();
        Iterator<String> it = m.keySet().iterator();
        // consume all elements
        while (it.hasNext()) {
            it.next();
        }
        assertFalse(it.hasNext(), "hasNext should be false after full consumption");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() after iterator is exhausted must throw NoSuchElementException");
    }

    @ParameterizedTest(name = "{index}: contains({1}) => {2}")
    @MethodSource("containsTestProvider")
    void keySetContainsScenarios(String description, Object key, boolean expected) {
        TestConcurrentNullSafeMap<String, Integer> m = freshTwoEntryMap();
        m.put(null, 99);
        assertEquals(expected, m.keySet().contains(key), description);
    }

    @ParameterizedTest(name = "{index}: remove({1}) => {2}")
    @MethodSource("removeTestProvider")
    void keySetRemoveScenarios(String description, Object key, boolean expected) {
        TestConcurrentNullSafeMap<String, Integer> m = freshTwoEntryMap();
        m.put(null, 99);
        boolean result = m.keySet().remove(key);
        assertEquals(expected, result, description);
        // verify map state matches expectation
        boolean stillPresent = m.containsKey(key);
        assertEquals(!expected, stillPresent, "After removal, presence of key should be opposite of removal result");
    }

    @Test
    void clearEmptiesUnderlyingMap() {
        TestConcurrentNullSafeMap<String, Integer> m = freshTwoEntryMap();
        m.keySet().clear();
        assertTrue(m.isEmpty(), "Map should be empty after clear via keySet");
    }

    @Test
    void iteratorRemoveForwardsToUnderlyingMap() {
        TestConcurrentNullSafeMap<String, Integer> m = freshTwoEntryMap();
        Iterator<String> it = m.keySet().iterator();
        assertTrue(it.hasNext(), "Iterator should have at least one element");
        String first = it.next();
        // remove the element we just visited
        it.remove();
        assertFalse(m.containsKey(first), "Underlying map must not contain the removed key");
        assertEquals(m.size(), m.keySet().size(), "keySet size must stay in sync after iterator.remove()");
    }

    @Test
    void iteratorRemoveIllegalStateBeforeNext() {
        TestConcurrentNullSafeMap<String, Integer> m = freshTwoEntryMap();
        Iterator<String> it = m.keySet().iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    @Test
    void iteratorRemoveIllegalStateAfterRemove() {
        TestConcurrentNullSafeMap<String, Integer> m = freshTwoEntryMap();
        Iterator<String> it = m.keySet().iterator();
        it.next();
        // first successful remove
        it.remove();
        assertThrows(IllegalStateException.class, it::remove, "Second remove() without intervening next() must throw IllegalStateException");
    }

    @Test
    void iteratorHasNextAfterFullConsumption() {
        TestConcurrentNullSafeMap<String, Integer> m = freshTwoEntryMap();
        Iterator<String> it = m.keySet().iterator();
        while (it.hasNext()) {
            it.next();
        }
        assertFalse(it.hasNext(), "hasNext must return false after all elements have been consumed");
    }

    @Test
    void sizeAfterIteratorRemove() {
        TestConcurrentNullSafeMap<String, Integer> m = freshTwoEntryMap();
        m.put("c", 3);
        Iterator<String> it = m.keySet().iterator();
        // advance to first element
        it.next();
        // remove it
        it.remove();
        assertEquals(2, m.keySet().size(), "Size should reflect removal performed via iterator");
    }

    @Test
    void iteratorRemoveUnsupportedOperationPropagation() {
        // ConcurrentHashMap's iterator does not support remove – it throws UOE.
        TestConcurrentNullSafeMap<String, Integer> m = freshTwoEntryMap();
        Iterator<String> it = m.keySet().iterator();
        // advance
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove, "Underlying iterator's remove() should propagate UnsupportedOperationException");
    }

    @Test
    void containsWithIncompatibleTypeThrowsClassCastException() {
        TestConcurrentNullSafeMap<String, Integer> m = freshTwoEntryMap();
        Set<String> ks = m.keySet();
        assertAll(() -> assertThrows(ClassCastException.class, () -> ks.contains(12345), "Integer argument should cause ClassCastException"), () -> assertThrows(ClassCastException.class, () -> ks.contains(new Object()), "Arbitrary Object argument should cause ClassCastException"));
    }

    @Test
    void removeWithIncompatibleTypeThrowsClassCastException() {
        TestConcurrentNullSafeMap<String, Integer> m = freshTwoEntryMap();
        Set<String> ks = m.keySet();
        assertThrows(ClassCastException.class, () -> ks.remove(true), "Boolean argument should cause ClassCastException");
    }

    @Test
    void containsNullWhenAbsentReturnsFalse() {
        // no null key
        TestConcurrentNullSafeMap<String, Integer> m = freshTwoEntryMap();
        assertFalse(m.keySet().contains(null), "contains(null) must return false when null key is absent");
    }

    @Test
    void removeNullWhenAbsentReturnsFalse() {
        // no null key
        TestConcurrentNullSafeMap<String, Integer> m = freshTwoEntryMap();
        assertFalse(m.keySet().remove(null), "remove(null) must return false when null key is absent");
        assertEquals(2, m.size(), "Map size must stay unchanged after failed null removal");
    }

    @Test
    void iteratorNextClassCastExceptionForIncompatibleInternalKey() throws Exception {
        // Map is declared as <String,Integer> but we inject a raw Integer key.
        StringIntegerMap m = new StringIntegerMap();
        // Use reflection to reach the protected internalMap field.
        Field internalField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> internal = (ConcurrentMap<Object, Object>) internalField.get(m);
        // Insert an incompatible key directly.
        // 42 is an Integer, not a String
        internal.put(42, 100);
        // The iterator should attempt to cast this key to String and fail.
        Iterator<String> it = m.keySet().iterator();
        assertThrows(ClassCastException.class, it::next, "Iterating over a keySet with an incompatible internal key must throw ClassCastException");
    }

    @Test
    void containsWithIncompatibleTypeOnStringIntegerMapThrowsClassCastException() {
        StringIntegerMap m = new StringIntegerMap();
        m.put("one", 1);
        Set<String> ks = m.keySet();
        // Passing an Integer should trigger the cast inside contains()
        assertThrows(ClassCastException.class, () -> ks.contains(123), "ClassCastException expected when contains() receives an incompatible type");
    }

    @Test
    void removeWithIncompatibleTypeOnStringIntegerMapThrowsClassCastException() {
        StringIntegerMap m = new StringIntegerMap();
        m.put("one", 1);
        Set<String> ks = m.keySet();
        assertThrows(ClassCastException.class, () -> ks.remove(123), "ClassCastException expected when remove() receives an incompatible type");
    }
}
