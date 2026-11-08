package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_useCopyIterator_Test {

    /**
     * Minimal stub for the library's {@code CaseInsensitiveMap}.
     * It extends {@link HashMap} so that it behaves like a regular map
     * but also provides {@code getWrappedMap()}.
     */
    private interface CaseInsensitiveMap<K, V> extends Map<K, V> {

        /**
         * Returns the underlying map that the case‑insensitive wrapper delegates to.
         */
        Map<K, V> getWrappedMap();
    }

    /**
     * Simple implementation of {@link CaseInsensitiveMap} that delegates
     * all {@link Map} operations to an internal {@code wrapped} map.
     */
    private static class SimpleCaseInsensitiveMap<K, V> extends HashMap<K, V> implements CaseInsensitiveMap<K, V> {

        private final Map<K, V> wrapped;

        SimpleCaseInsensitiveMap(Map<K, V> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public Map<K, V> getWrappedMap() {
            return wrapped;
        }

        // Delegate all Map methods to the wrapped map for realism.
        @Override
        public int size() {
            return wrapped.size();
        }

        @Override
        public boolean isEmpty() {
            return wrapped.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return wrapped.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return wrapped.containsValue(value);
        }

        @Override
        public V get(Object key) {
            return wrapped.get(key);
        }

        @Override
        public V put(K key, V value) {
            return wrapped.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return wrapped.remove(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            wrapped.putAll(m);
        }

        @Override
        public void clear() {
            wrapped.clear();
        }

        @Override
        public Set<K> keySet() {
            return wrapped.keySet();
        }

        @Override
        public Collection<V> values() {
            return wrapped.values();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return wrapped.entrySet();
        }
    }

    /**
     * A {@link TreeMap} that also implements {@link CaseInsensitiveMap}.
     * Used to test a map that is both {@link SortedMap} and {@link CaseInsensitiveMap}.
     */
    private static class BothMap<K, V> extends TreeMap<K, V> implements CaseInsensitiveMap<K, V> {

        private final Map<K, V> wrapped;

        BothMap(Map<K, V> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public Map<K, V> getWrappedMap() {
            return wrapped;
        }
    }

    /**
     * Concrete subclass of {@link CompactMap} that lets us control the
     * result of {@code getNewMap()} for each test case.
     */
    private static class TestCompactMap<K, V> extends CompactMap<K, V> {

        private final Supplier<Map<K, V>> mapSupplier;

        TestCompactMap(Supplier<Map<K, V>> mapSupplier) {
            this.mapSupplier = mapSupplier;
        }

        @Override
        protected Map<K, V> getNewMap() {
            return mapSupplier.get();
        }

        /**
         * Exposes the protected {@code useCopyIterator()} for testing.
         */
        boolean callUseCopyIterator() {
            return useCopyIterator();
        }
    }

    private static Stream<Arguments> branchCoverageProvider() {
        return Stream.of(// SortedMap directly (TreeMap)
        Arguments.of("SortedMap (TreeMap) → true", (Supplier<Map<String, String>>) TreeMap::new, true), // Non‑sorted map (HashMap)
        Arguments.of("HashMap → false", (Supplier<Map<String, String>>) HashMap::new, false), // CaseInsensitiveMap wrapping a SortedMap
        Arguments.of("CaseInsensitiveMap wrapping TreeMap → true", (Supplier<Map<String, String>>) () -> new SimpleCaseInsensitiveMap<>(new TreeMap<>()), true), // CaseInsensitiveMap wrapping a non‑sorted map
        Arguments.of("CaseInsensitiveMap wrapping HashMap → false", (Supplier<Map<String, String>>) () -> new SimpleCaseInsensitiveMap<>(new HashMap<>()), false));
    }

    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(// getNewMap() returns null
        Arguments.of("null map → false", (Supplier<Map<String, String>>) () -> null, false), // CaseInsensitiveMap whose getWrappedMap() returns null
        Arguments.of("CaseInsensitiveMap wrapping null → false", (Supplier<Map<String, String>>) () -> new SimpleCaseInsensitiveMap<>(null), false), // Map implements both interfaces; unwrap yields non‑sorted HashMap
        Arguments.of("BothMap unwrapping to HashMap → false", (Supplier<Map<String, String>>) () -> new BothMap<>(new HashMap<>()), false), // Map implements both interfaces; unwrap yields SortedMap (TreeMap)
        Arguments.of("BothMap unwrapping to TreeMap → true", (Supplier<Map<String, String>>) () -> new BothMap<>(new TreeMap<>()), true));
    }

    @Test
    void useCopyIteratorReturnsTrueWhenUnderlyingMapIsSortedMap() {
        // Given: a CompactMap whose getNewMap() returns a TreeMap (which implements SortedMap)
        TestCompactMap<String, String> map = new TestCompactMap<>(new TreeMap<>());
        // When: we invoke the protected method via the public wrapper
        boolean result = map.callUseCopyIterator();
        // Then: the result should be true because the underlying map is a SortedMap
        assertTrue(result, "useCopyIterator() should return true when the underlying map is a SortedMap");
    }

    @Test
    void useCopyIteratorReturnsFalseWhenUnderlyingMapIsNotSortedMap() {
        // Given: a CompactMap whose getNewMap() returns a regular HashMap (not a SortedMap)
        TestCompactMap<String, String> map = new TestCompactMap<>(new HashMap<>());
        // When: we invoke the protected method via the public wrapper
        boolean result = map.callUseCopyIterator();
        // Then: the result should be false because the underlying map is not a SortedMap
        assertFalse(result, "useCopyIterator() should return false when the underlying map is not a SortedMap");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("branchCoverageProvider")
    void useCopyIteratorBranchCoverage(String description, Supplier<Map<String, String>> mapSupplier, boolean expected) {
        TestCompactMap<String, String> map = new TestCompactMap<>(mapSupplier);
        boolean result = map.callUseCopyIterator();
        assertEquals(expected, result, () -> "Unexpected result for case: " + description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("edgeCaseProvider")
    void useCopyIteratorEdgeCases(String description, Supplier<Map<String, String>> mapSupplier, boolean expected) {
        TestCompactMap<String, String> map = new TestCompactMap<>(mapSupplier);
        boolean result = map.callUseCopyIterator();
        assertEquals(expected, result, () -> "Edge case failed: " + description);
    }

    @Test
    void useCopyIteratorPropagatesExceptionFromGetNewMap() {
        TestCompactMap<String, String> map = new TestCompactMap<>(() -> {
            throw new IllegalStateException("boom");
        });
        IllegalStateException ex = assertThrows(IllegalStateException.class, map::callUseCopyIterator, "Expected IllegalStateException to be propagated");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void useCopyIteratorPropagatesExceptionFromWrappedMap() {
        // CaseInsensitiveMap whose getWrappedMap() throws NPE
        CaseInsensitiveMap<String, String> badCIM = new SimpleCaseInsensitiveMap<>(new HashMap<>()) {

            @Override
            public Map<String, String> getWrappedMap() {
                throw new NullPointerException("wrapped NPE");
            }
        };
        TestCompactMap<String, String> map = new TestCompactMap<>(() -> badCIM);
        NullPointerException ex = assertThrows(NullPointerException.class, map::callUseCopyIterator, "Expected NullPointerException from getWrappedMap() to be propagated");
        assertEquals("wrapped NPE", ex.getMessage());
    }
}
