package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_getNewMap_2_Test {

    /**
     * Simple map that records the capacity passed to its int‑arg constructor.
     */
    private static class MyMap<K, V> extends java.util.HashMap<K, V> {

        final int requestedCapacity;

        MyMap() {
            // default constructor – not used in the test
            this.requestedCapacity = -1;
        }

        MyMap(int capacity) {
            super(capacity);
            this.requestedCapacity = capacity;
        }
    }

    /**
     * CompactMap subclass that supplies {@code MyMap} as the backing map type.
     */
    private static class TestCompactMapWithMyMap<K, V> extends CompactMap<K, V> {

        @Override
        protected Map<K, V> getNewMap() {
            // Return a map instance whose concrete class has an int‑arg constructor.
            return new MyMap<>();
        }

        /**
         * Expose the protected method for testing.
         */
        Map<K, V> callGetNewMap(int size) {
            return super.getNewMap(size);
        }
    }

    /**
     * CompactMap subclass that supplies {@code TreeMap} (no int‑arg constructor).
     */
    private static class TestCompactMapWithTreeMap<K, V> extends CompactMap<K, V> {

        @Override
        protected Map<K, V> getNewMap() {
            // TreeMap does not have a constructor that takes an int.
            return new TreeMap<>();
        }

        /**
         * Expose the protected method for testing.
         */
        Map<K, V> callGetNewMap(int size) {
            return super.getNewMap(size);
        }
    }

    /**
     * Map whose int‑arg constructor deliberately throws for negative sizes.
     */
    private static class ThrowingMap<K, V> extends HashMap<K, V> {

        ThrowingMap() {
            super();
        }

        ThrowingMap(int capacity) {
            // we will throw manually
            super();
            if (capacity < 0) {
                throw new IllegalArgumentException("Negative capacity");
            }
        }
    }

    /**
     * Map with a private int‑arg constructor – not accessible via reflection.
     */
    private static class PrivateCtorMap<K, V> extends HashMap<K, V> {

        PrivateCtorMap() {
            super();
        }

        @SuppressWarnings("unused")
        private PrivateCtorMap(int capacity) {
            super(capacity);
        }
    }

    /**
     * Uses the default backing map (HashMap) – has a public int ctor.
     */
    private static class TestCompactMapDefault<K, V> extends CompactMap<K, V> {

        @Override
        protected Map<K, V> getNewMap() {
            return new HashMap<>();
        }

        Map<K, V> callGetNewMap(int size) {
            return super.getNewMap(size);
        }
    }

    /**
     * Returns a ThrowingMap – constructor throws for negative size.
     */
    private static class TestCompactMapThrowing<K, V> extends CompactMap<K, V> {

        @Override
        protected Map<K, V> getNewMap() {
            return new ThrowingMap<>();
        }

        Map<K, V> callGetNewMap(int size) {
            return super.getNewMap(size);
        }
    }

    /**
     * Returns a PrivateCtorMap – int ctor exists but is private.
     */
    private static class TestCompactMapPrivateCtor<K, V> extends CompactMap<K, V> {

        @Override
        protected Map<K, V> getNewMap() {
            return new PrivateCtorMap<>();
        }

        Map<K, V> callGetNewMap(int size) {
            return super.getNewMap(size);
        }
    }

    /**
     * Returns a Hashtable – has a public int ctor.
     */
    private static class TestCompactMapWithHashtable<K, V> extends CompactMap<K, V> {

        @Override
        protected Map<K, V> getNewMap() {
            return new Hashtable<>();
        }

        Map<K, V> callGetNewMap(int size) {
            return super.getNewMap(size);
        }
    }

    /**
     * Returns null – tests null‑fallback handling.
     */
    private static class TestCompactMapNull<K, V> extends CompactMap<K, V> {

        @Override
        protected Map<K, V> getNewMap() {
            return null;
        }

        Map<K, V> callGetNewMap(int size) {
            return super.getNewMap(size);
        }
    }

    @Test
    void getNewMap_usesIntConstructorWhenAvailable() {
        // GIVEN
        TestCompactMapWithMyMap<String, Integer> compactMap = new TestCompactMapWithMyMap<>();
        int requestedSize = 7;
        // WHEN
        Map<String, Integer> result = compactMap.callGetNewMap(requestedSize);
        // THEN
        assertTrue(result instanceof MyMap, "Result should be an instance of MyMap");
        MyMap<String, Integer> myMap = (MyMap<String, Integer>) result;
        assertEquals(requestedSize, myMap.requestedCapacity, "MyMap should have received the requested capacity via its int‑arg constructor");
        assertEquals(0, myMap.size(), "Newly created map should be empty");
    }

    @Test
    void getNewMap_fallsBackWhenIntConstructorMissing() {
        // GIVEN
        TestCompactMapWithTreeMap<String, Integer> compactMap = new TestCompactMapWithTreeMap<>();
        // arbitrary – TreeMap has no int‑arg ctor
        int requestedSize = 10;
        // WHEN
        Map<String, Integer> result = compactMap.callGetNewMap(requestedSize);
        // THEN
        assertTrue(result instanceof TreeMap, "Result should be the original TreeMap instance (fallback path)");
        assertEquals(0, result.size(), "Fallback map should be empty");
    }

    /* --------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> getOriginalMap(CompactMap<K, V> compactMap) throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("getNewMap");
        m.setAccessible(true);
        return (Map<K, V>) m.invoke(compactMap);
    }

    /**
     * ConstructorFoundAndInstantiated – default HashMap with valid sizes.
     */
    @ParameterizedTest(name = "size={0}")
    @CsvSource({ "10", "0" })
    void getNewMap_defaultHashMap_instantiatesNewMap(int size) throws Exception {
        TestCompactMapDefault<String, Integer> compactMap = new TestCompactMapDefault<>();
        Map<String, Integer> original = getOriginalMap(compactMap);
        Map<String, Integer> result = compactMap.callGetNewMap(size);
        assertTrue(result instanceof HashMap, "Result should be a HashMap");
        assertNotSame(original, result, "Result must be a distinct object from the original map");
        assertEquals(0, result.size(), "Newly created map must be empty");
    }

    /**
     * FallbackWhenNoIntConstructor – TreeMap has no int‑arg constructor.
     */
    @Test
    void getNewMap_treeMap_fallbackToOriginal() throws Exception {
        TestCompactMapWithTreeMap<String, Integer> compactMap = new TestCompactMapWithTreeMap<>();
        Map<String, Integer> original = getOriginalMap(compactMap);
        Map<String, Integer> result = compactMap.callGetNewMap(5);
        assertSame(original, result, "Should fall back to the original TreeMap instance");
        assertTrue(result instanceof TreeMap, "Result type must be TreeMap");
        assertEquals(0, result.size(), "Fallback map should be empty");
    }

    /**
     * FallbackWhenConstructorThrows – ThrowingMap throws for negative size.
     */
    @Test
    void getNewMap_throwingMap_negativeSize_fallbackToOriginal() throws Exception {
        TestCompactMapThrowing<String, Integer> compactMap = new TestCompactMapThrowing<>();
        Map<String, Integer> original = getOriginalMap(compactMap);
        Map<String, Integer> result = compactMap.callGetNewMap(-1);
        assertSame(original, result, "When constructor throws, fallback to original map");
        assertTrue(result instanceof ThrowingMap, "Original map type must be ThrowingMap");
        assertEquals(0, result.size(), "Original map remains empty");
    }

    /**
     * PrivateConstructorFallback – int ctor is private, reflection fails.
     */
    @Test
    void getNewMap_privateCtorMap_fallbackToOriginal() throws Exception {
        TestCompactMapPrivateCtor<String, Integer> compactMap = new TestCompactMapPrivateCtor<>();
        Map<String, Integer> original = getOriginalMap(compactMap);
        Map<String, Integer> result = compactMap.callGetNewMap(3);
        assertSame(original, result, "Private int‑ctor should cause fallback to original map");
        assertTrue(result instanceof PrivateCtorMap, "Original map type must be PrivateCtorMap");
    }

    /**
     * SuccessfulIntConstructorDifferentMapType – Hashtable supports int ctor.
     */
    @ParameterizedTest(name = "size={0}")
    @CsvSource({ "8", "0" })
    void getNewMap_hashtable_instantiatesNewMap(int size) throws Exception {
        TestCompactMapWithHashtable<String, Integer> compactMap = new TestCompactMapWithHashtable<>();
        Map<String, Integer> original = getOriginalMap(compactMap);
        Map<String, Integer> result = compactMap.callGetNewMap(size);
        assertTrue(result instanceof Hashtable, "Result should be a Hashtable");
        assertNotSame(original, result, "Result must be a distinct object from the original Hashtable");
        assertEquals(0, result.size(), "Newly created Hashtable must be empty");
    }

    /**
     * NegativeSizeFallbackForStandardMap – HashMap int ctor throws for negative size.
     */
    @Test
    void getNewMap_hashMap_negativeSize_fallbackToOriginal() throws Exception {
        TestCompactMapDefault<String, Integer> compactMap = new TestCompactMapDefault<>();
        Map<String, Integer> original = getOriginalMap(compactMap);
        Map<String, Integer> result = compactMap.callGetNewMap(-10);
        assertSame(original, result, "Negative size should cause fallback to original HashMap");
        assertTrue(result instanceof HashMap, "Original map type must be HashMap");
    }

    /**
     * NullUnderlyingMapFallback – getNewMap() returns null.
     */
    @Test
    void getNewMap_nullUnderlyingMap_returnsNull() {
        TestCompactMapNull<String, Integer> compactMap = new TestCompactMapNull<>();
        Map<String, Integer> result = compactMap.callGetNewMap(7);
        assertNull(result, "When underlying getNewMap() returns null, the method should return null");
    }
}
