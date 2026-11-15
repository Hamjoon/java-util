package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class LRUCache_containsValue_Test {

    private LRUCache<Integer, String> cache;

    static class CustomKey {

        final int id;

        CustomKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CustomKey && ((CustomKey) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    /* -------------------------------------------------------------
     *  Custom equals implementation (MagicString).
     * ------------------------------------------------------------- */
    static class MagicString {

        private final String magic = "magic";

        @Override
        public boolean equals(Object o) {
            return (o instanceof String) && magic.equals(o);
        }

        @Override
        public int hashCode() {
            return magic.hashCode();
        }
    }

    static class BadEquals {

        @Override
        public boolean equals(Object o) {
            // Force a ClassCastException when compared with anything other than Integer
            // will throw ClassCastException for non‑Integer
            Integer i = (Integer) o;
            return i == 42;
        }
    }

    @AfterEach
    void teardownAfterEach() {
        if (cache != null) {
            cache.clear();
        }
    }

    /* -------------------------------------------------------------
     *  Helper to create a cache with the given strategy type.
     * ------------------------------------------------------------- */
    private void setUp(LRUCache.StrategyType strategy) {
        // capacity 3 is enough for the tests; the strategy determines the internal implementation.
        cache = new LRUCache<>(3, strategy);
    }

    /* -------------------------------------------------------------
     *  Parameter source – both supported strategies.
     * ------------------------------------------------------------- */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_returnsTrueWhenValueExists(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "alpha");
        cache.put(2, "beta");
        cache.put(3, "gamma");
        assertTrue(cache.containsValue("alpha"), "Cache should contain value \"alpha\"");
        assertTrue(cache.containsValue("beta"), "Cache should contain value \"beta\"");
        assertTrue(cache.containsValue("gamma"), "Cache should contain value \"gamma\"");
        assertFalse(cache.containsValue("delta"), "Cache should not contain value \"delta\"");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_handlesNullValue(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, null);
        cache.put(2, "notNull");
        assertTrue(cache.containsValue(null), "Cache should report containing a null value");
        assertTrue(cache.containsValue("notNull"), "Cache should report containing \"notNull\"");
        assertFalse(cache.containsValue("missing"), "Cache should not report a missing value");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_reflectsEviction(LRUCache.StrategyType strategy) {
        setUp(strategy);
        // Fill the cache to capacity.
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        // Access key 1 to make it most‑recently used.
        cache.get(1);
        // Adding a fourth entry forces eviction of the least‑recently used (key 2).
        cache.put(4, "four");
        // The eviction may be asynchronous for the threaded strategy, so we wait briefly.
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline && cache.containsKey(2)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }
        assertFalse(cache.containsKey(2), "Key 2 should have been evicted");
        assertFalse(cache.containsValue("two"), "Value \"two\" should no longer be present after eviction");
        // Values that remain should still be reported.
        assertTrue(cache.containsValue("one"));
        assertTrue(cache.containsValue("three"));
        assertTrue(cache.containsValue("four"));
    }

    /* -------------------------------------------------------------
     *  Helper to replace the private {@code strategy} field with a
     *  custom (mock) map. Used for exception‑propagation tests.
     * ------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private void injectStrategyMap(Map<Integer, Object> customMap) throws Exception {
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, customMap);
    }

    /* -------------------------------------------------------------
     *  Basic presence / absence tests.
     * ------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_basicPresence(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "apple");
        cache.put(2, "orange");
        assertTrue(cache.containsValue("apple"), "Value \"apple\" should be reported as present");
        assertFalse(cache.containsValue("banana"), "Value \"banana\" should be reported as absent");
    }

    /* -------------------------------------------------------------
     *  Null handling – present and absent.
     * ------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_nullValuePresent(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, null);
        cache.put(2, "notNull");
        assertTrue(cache.containsValue(null), "Cache should report containing a null value");
        assertTrue(cache.containsValue("notNull"));
        assertFalse(cache.containsValue("missing"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_nullValueAbsent(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "value1");
        cache.put(2, "value2");
        assertFalse(cache.containsValue(null), "Cache should report not containing null when none stored");
    }

    /* -------------------------------------------------------------
     *  Empty cache – always false.
     * ------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_emptyCache(LRUCache.StrategyType strategy) {
        setUp(strategy);
        assertFalse(cache.containsValue("any"), "Empty cache must return false for any value");
    }

    /* -------------------------------------------------------------
     *  Duplicate values and equality semantics.
     * ------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_duplicateValues(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "shared");
        // duplicate string instance (or equal)
        cache.put(2, "shared");
        cache.put(3, Integer.valueOf(5));
        cache.put(4, Integer.valueOf(5));
        assertTrue(cache.containsValue("shared"), "Duplicate string values should be detected");
        assertTrue(cache.containsValue(Integer.valueOf(5)), "Equal Integer objects should be detected");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_customKeyEquality(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, new CustomKey(1));
        // two distinct instances, same id
        cache.put(2, new CustomKey(1));
        assertTrue(cache.containsValue(new CustomKey(1)), "Equal CustomKey should be found");
        assertFalse(cache.containsValue(new CustomKey(2)), "Non‑present CustomKey should not be found");
    }

    /* -------------------------------------------------------------
     *  Type‑mismatch scenarios.
     * ------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_typeMismatch_IntegerVsString(LRUCache.StrategyType strategy) {
        setUp(strategy);
        // Integer value
        cache.put(1, 123);
        assertFalse(cache.containsValue("123"), "String \"123\" should not match Integer 123");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_typeMismatch_StringVsBoolean(LRUCache.StrategyType strategy) {
        setUp(strategy);
        // String value
        cache.put(1, "true");
        assertFalse(cache.containsValue(Boolean.TRUE), "Boolean true should not match String \"true\"");
    }

    /* -------------------------------------------------------------
     *  Mutable objects (List) – reference vs. structural equality.
     * ------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_mutableObject_referenceUnchanged(LRUCache.StrategyType strategy) {
        setUp(strategy);
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        cache.put(1, list);
        // mutate the list after insertion
        list.add("c");
        assertTrue(cache.containsValue(list), "Same list instance should still be reported even after mutation");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_mutableObject_structuralEquality(LRUCache.StrategyType strategy) {
        setUp(strategy);
        List<String> stored = Arrays.asList("a", "b");
        cache.put(1, stored);
        List<String> equalList = Arrays.asList("a", "b");
        List<String> differentOrder = Arrays.asList("b", "a");
        assertTrue(cache.containsValue(equalList), "List with same elements and order should be equal");
        assertFalse(cache.containsValue(differentOrder), "List with same elements but different order should not be equal");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_customEquals_magicString(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, new MagicString());
        assertTrue(cache.containsValue("magic"), "MagicString should match literal \"magic\"");
        assertFalse(cache.containsValue("not-magic"), "Non‑matching literal should not be reported");
    }

    /* -------------------------------------------------------------
     *  After removal / after clear.
     * ------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_afterRemoval(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "temp");
        cache.remove(1);
        assertFalse(cache.containsValue("temp"), "Removed value must no longer be reported");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_afterClear(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "a");
        cache.put(2, "b");
        cache.clear();
        assertFalse(cache.containsValue("a"));
        assertFalse(cache.containsValue("b"));
        assertFalse(cache.containsValue("any"));
    }

    /* -------------------------------------------------------------
     *  Threaded strategy specific checks.
     * ------------------------------------------------------------- */
    @Test
    void containsValue_threadedStrategy_containsAndMissing() {
        setUp(LRUCache.StrategyType.THREADED);
        cache.put(1, "concurrent");
        cache.put(2, "other");
        assertTrue(cache.containsValue("concurrent"));
        assertFalse(cache.containsValue("missing"));
    }

    /* -------------------------------------------------------------
     *  Exception propagation from the underlying strategy map.
     * ------------------------------------------------------------- */
    @Test
    void containsValue_unsupportedOperationException_propagates() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, Object> mockMap = Mockito.mock(Map.class);
        Mockito.when(mockMap.containsValue(any())).thenThrow(new UnsupportedOperationException("Not supported"));
        injectStrategyMap(mockMap);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> cache.containsValue("trigger"));
        assertEquals("Not supported", ex.getMessage());
    }

    @Test
    void containsValue_nullNotAllowed_propagates() throws Exception {
        setUp(LRUCache.StrategyType.THREADED);
        // Threaded strategy uses ConcurrentHashMapNullSafe which throws NPE for null values.
        // We simply call containsValue(null) and expect NPE.
        assertThrows(NullPointerException.class, () -> cache.containsValue(null));
    }

    @Test
    void containsValue_classCastException_propagates() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, Object> mockMap = Mockito.mock(Map.class);
        Mockito.when(mockMap.containsValue(any())).thenThrow(new ClassCastException("bad cast"));
        injectStrategyMap(mockMap);
        ClassCastException ex = assertThrows(ClassCastException.class, () -> cache.containsValue(new BadEquals()));
        assertEquals("bad cast", ex.getMessage());
    }

    @Test
    void containsValue_concurrentModificationException_propagates() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, Object> mockMap = Mockito.mock(Map.class);
        Mockito.when(mockMap.containsValue(any())).thenThrow(new ConcurrentModificationException("CM"));
        injectStrategyMap(mockMap);
        ConcurrentModificationException ex = assertThrows(ConcurrentModificationException.class, () -> cache.containsValue("any"));
        assertEquals("CM", ex.getMessage());
    }
}
