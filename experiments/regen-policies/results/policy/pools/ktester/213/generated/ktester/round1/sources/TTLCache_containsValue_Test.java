package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

class TTLCache_containsValue_Test {

    private TTLCache<Integer, String> ttlCache;

    static final class Person {

        final int id;

        final String name;

        Person(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Person) && ((Person) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    static final class TestValueEqualsException extends RuntimeException {

        TestValueEqualsException(String msg) {
            super(msg);
        }
    }

    static final class ExplodingValue {

        @Override
        public boolean equals(Object obj) {
            throw new TestValueEqualsException("Exploding equals invoked");
        }

        @Override
        public int hashCode() {
            return 42;
        }
    }

    /**
     * Creates a fresh cache before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Use a very large TTL so entries never expire during the test execution.
        ttlCache = new TTLCache<>(60_000L, -1);
    }

    /**
     * Clears the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    /**
     * Shuts down the shared purge thread once all tests have finished.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    // ---------- positive cases ----------
    static Stream<Object> positiveValues() {
        return Stream.of("apple", // null value case
        null);
    }

    // ---------- negative cases ----------
    static Stream<Object[]> negativeValues() {
        return Stream.of(// value absent, cache non‑empty
        new Object[] { "banana", false, false }, // null absent, cache non‑empty
        new Object[] { null, false, false }, // any value, cache empty
        new Object[] { "any", false, true });
    }

    /**
     * Sets a private field on the given target object.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = findField(target.getClass(), fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Retrieves a private field value from the given target object.
     */
    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field f = findField(target.getClass(), fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Walks the class hierarchy to find a declared field.
     */
    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> cur = clazz;
        while (cur != null) {
            try {
                return cur.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                cur = cur.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field '" + name + "' not found in class hierarchy of " + clazz);
    }

    @Test
    void containsValue_returnsTrueWhenValueIsPresent() {
        ttlCache.put(1, "Alpha");
        ttlCache.put(2, "Beta");
        ttlCache.put(3, "Gamma");
        assertTrue(ttlCache.containsValue("Alpha"), "Cache should contain value \"Alpha\"");
        assertTrue(ttlCache.containsValue("Beta"), "Cache should contain value \"Beta\"");
        assertTrue(ttlCache.containsValue("Gamma"), "Cache should contain value \"Gamma\"");
    }

    @Test
    void containsValue_returnsFalseWhenValueIsAbsent() {
        ttlCache.put(1, "Alpha");
        ttlCache.put(2, "Beta");
        assertFalse(ttlCache.containsValue("Gamma"), "Cache should not contain value \"Gamma\"");
        assertFalse(ttlCache.containsValue(null), "Cache should not contain null when no entry stores null");
    }

    @Test
    void containsValue_handlesNullValuesCorrectly() {
        ttlCache.put(1, null);
        ttlCache.put(2, "Beta");
        assertTrue(ttlCache.containsValue(null), "Cache should report presence of a null value");
        assertTrue(ttlCache.containsValue("Beta"), "Cache should still report presence of a non‑null value");
        assertFalse(ttlCache.containsValue("Alpha"), "Cache should not report a value that was never added");
    }

    @ParameterizedTest(name = "containsValue positive – value: {0}")
    @MethodSource("positiveValues")
    void containsValue_positiveCases(Object search) {
        // Prepare cache according to description
        if (search == null) {
            ttlCache.put(1, null);
        } else {
            ttlCache.put(1, search);
        }
        assertTrue(ttlCache.containsValue(search));
    }

    @ParameterizedTest(name = "containsValue negative – value: {0}")
    @MethodSource("negativeValues")
    void containsValue_negativeCases(Object search, boolean expected, boolean emptyCache) {
        if (!emptyCache) {
            ttlCache.put(1, "something");
        }
        assertEquals(expected, ttlCache.containsValue(search));
    }

    @Test
    void containsValue_typeSafetyEdgeCase() {
        // String value
        ttlCache.put(1, "123");
        // Integer search
        assertFalse(ttlCache.containsValue(123));
    }

    @Test
    void containsValue_duplicateValues() {
        ttlCache.put(1, "orange");
        ttlCache.put(2, "orange");
        assertTrue(ttlCache.containsValue("orange"));
    }

    @Test
    void containsValue_equalButDifferentInstance() {
        // literal
        ttlCache.put(1, "pear");
        assertTrue(ttlCache.containsValue(new String("pear")));
    }

    @Test
    void containsValue_customEqualsObject() {
        Person stored = new Person(1, "Alice");
        // same id, different name
        Person search = new Person(1, "Bob");
        ttlCache.put(1, stored);
        assertTrue(ttlCache.containsValue(search));
    }

    @Test
    void containsValue_nanHandling() {
        ttlCache.put(1, Double.NaN);
        assertTrue(ttlCache.containsValue(Double.NaN));
    }

    @Test
    void containsValue_zeroSignDistinction() {
        ttlCache.put(1, 0.0d);
        assertFalse(ttlCache.containsValue(-0.0d));
    }

    @Test
    void containsValue_booleanTrue() {
        ttlCache.put(1, Boolean.TRUE);
        assertTrue(ttlCache.containsValue(Boolean.TRUE));
    }

    @Test
    void containsValue_afterRemoval() {
        ttlCache.put(1, "toBeRemoved");
        ttlCache.remove(1);
        assertFalse(ttlCache.containsValue("toBeRemoved"));
    }

    @Test
    void containsValue_collectionEquality() {
        List<Integer> stored = Arrays.asList(1, 2, 3);
        ttlCache.put(1, stored);
        List<Integer> search = new ArrayList<>(Arrays.asList(1, 2, 3));
        assertTrue(ttlCache.containsValue(search));
    }

    @Test
    void containsValue_nullInternalMap_throwsNPE() throws Exception {
        // Create a fresh instance then replace its cacheMap with null via reflection
        TTLCache<Integer, Object> cache = new TTLCache<>(60_000L, -1);
        setPrivateField(cache, "cacheMap", null);
        assertThrows(NullPointerException.class, () -> cache.containsValue(null));
    }

    @Test
    void containsValue_entryWithNullNode_throwsNPE() throws Exception {
        ttlCache.put(1, "any");
        // Obtain the internal CacheEntry and nullify its node field
        ConcurrentMap<Integer, ?> map = (ConcurrentMap<Integer, ?>) getPrivateField(ttlCache, "cacheMap");
        // CacheEntry instance
        Object entry = map.get(1);
        setPrivateField(entry, "node", null);
        assertThrows(NullPointerException.class, () -> ttlCache.containsValue("any"));
    }

    @Test
    void containsValue_entryValueEqualsThrowsException_propagates() {
        ExplodingValue evil = new ExplodingValue();
        ttlCache.put(1, evil);
        TestValueEqualsException ex = assertThrows(TestValueEqualsException.class, () -> ttlCache.containsValue("trigger"));
        assertEquals("Exploding equals invoked", ex.getMessage());
    }

    @Test
    void containsValue_concurrentMapIterationThrowsRuntimeException() throws Exception {
        // Mock a ConcurrentMap whose values().iterator() throws
        @SuppressWarnings("unchecked")
        ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, Object>> badMap = (ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, Object>>) mock(ConcurrentMap.class);
        // values() returns a collection whose iterator() throws
        Collection<TTLCache.CacheEntry<Integer, Object>> badValues = mock(Collection.class);
        when(badMap.values()).thenReturn(badValues);
        when(badValues.iterator()).thenThrow(new RuntimeException("Iteration failure"));
        // Inject the mock map via reflection
        setPrivateField(ttlCache, "cacheMap", badMap);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ttlCache.containsValue("unused"));
        assertEquals("Iteration failure", ex.getMessage());
    }
}
