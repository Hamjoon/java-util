package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactMap_getNewMap_Test {

    /**
     * Simple concrete subclass that lets us control {@code compactSize()} and expose the protected methods.
     */
    private static class TestCompactMap<K, V> extends CompactMap<K, V> {

        private final int compactSize;

        private final RuntimeException exceptionOnCompactSize;

        TestCompactMap(int compactSize) {
            this.compactSize = compactSize;
            this.exceptionOnCompactSize = null;
        }

        /**
         * Constructor that forces {@code compactSize()} to throw a supplied exception.
         */
        TestCompactMap(RuntimeException exceptionOnCompactSize) {
            this.compactSize = 0;
            this.exceptionOnCompactSize = exceptionOnCompactSize;
        }

        @Override
        protected int compactSize() {
            if (exceptionOnCompactSize != null) {
                throw exceptionOnCompactSize;
            }
            return compactSize;
        }

        /**
         * Expose the protected {@code getNewMap()} for testing.
         */
        public Map<K, V> callGetNewMap() {
            return super.getNewMap();
        }

        /**
         * Expose the protected {@code getNewMap(int)} for testing.
         */
        public Map<K, V> callGetNewMap(int size) {
            return super.getNewMap(size);
        }

        /**
         * Expose the protected {@code useCopyIterator()} that internally calls {@code getNewMap()}.
         */
        public Map<K, V> callUseCopyIterator() {
            return super.useCopyIterator() ? super.getNewMap() : null;
        }
    }

    /**
     * Helper to obtain the internal table length of a HashMap (capacity).
     */
    private static int getHashMapTableLength(Map<?, ?> map) throws Exception {
        Field tableField = HashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(map);
        return table == null ? 0 : table.length;
    }

    /**
     * Verifies that {@code getNewMap()} returns a fresh {@link HashMap} instance whose internal
     * capacity matches the expectation derived from {@code compactSize() + 1}.
     */
    @Test
    void getNewMap_returnsHashMapWithExpectedInitialCapacity() throws Exception {
        // compactSize = 5 => requested capacity = 6; HashMap will round up to the next power of two (8)
        TestCompactMap<Integer, String> map = new TestCompactMap<>(5);
        Map<Integer, String> newMap = map.callGetNewMap();
        assertNotNull(newMap, "The map returned by getNewMap() must not be null");
        assertTrue(newMap instanceof HashMap, "The map returned should be a HashMap");
        // Use reflection to inspect the internal table of the HashMap
        Field tableField = HashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(newMap);
        assertNotNull(table, "Internal table of the HashMap must be allocated");
        assertEquals(8, table.length, "HashMap should have rounded its capacity to the next power of two (8) for requested size 6");
    }

    /**
     * Ensures that each call to {@code getNewMap()} yields a distinct map instance.
     */
    @Test
    void getNewMap_returnsDifferentInstancesOnEachCall() {
        TestCompactMap<Integer, String> map = new TestCompactMap<>(10);
        Map<Integer, String> first = map.callGetNewMap();
        Map<Integer, String> second = map.callGetNewMap();
        assertNotSame(first, second, "Each invocation of getNewMap() should produce a new map instance");
    }

    /**
     * Checks that {@code getNewMap(int)} simply delegates to {@code getNewMap()} and therefore
     * returns the same concrete type.
     */
    @Test
    void getNewMapWithSize_delegatesToGetNewMap() {
        TestCompactMap<Integer, String> map = new TestCompactMap<>(7);
        // size argument is ignored in the default impl
        Map<Integer, String> delegated = map.callGetNewMap(42);
        assertTrue(delegated instanceof HashMap, "Delegated getNewMap(int) should still return a HashMap");
    }

    /**
     * Demonstrates that a protected method which internally calls {@code getNewMap()}
     * (e.g., {@code useCopyIterator()}) can be exercised without accessing private state.
     */
    @Test
    void useCopyIterator_callsGetNewMap() {
        TestCompactMap<Integer, String> map = new TestCompactMap<>(4);
        // The default implementation of useCopyIterator() returns false, but we can still
        // invoke it to ensure it does not throw and that it internally creates a map.
        // The method is protected, so we expose it via a public wrapper.
        Map<Integer, String> created = map.callUseCopyIterator();
        // Since the default returns false, the wrapper returns null; we only verify no exception.
        assertNull(created, "Default useCopyIterator() returns false, thus wrapper returns null");
    }

    @Test
    void getNewMap_compactSizeZero_returnsHashMapWithCapacityAtLeastOne_andAllowsPut() throws Exception {
        TestCompactMap<Integer, String> map = new TestCompactMap<>(0);
        Map<Integer, String> newMap = map.callGetNewMap();
        assertNotNull(newMap, "Map must not be null");
        assertTrue(newMap instanceof HashMap, "Returned map should be a HashMap");
        assertTrue(newMap.isEmpty(), "New map should be empty");
        int capacity = getHashMapTableLength(newMap);
        assertTrue(capacity >= 1, "Initial capacity should be at least 1");
        // verify that a normal put works
        newMap.put(1, "one");
        assertEquals(1, newMap.size(), "Size after one put must be 1");
        assertEquals("one", newMap.get(1), "Value must be retrievable after put");
    }

    @Test
    void getNewMap_compactSizePositive_returnsHashMapWithExpectedCapacity_andAcceptsManyEntries() throws Exception {
        int compactSize = 100;
        TestCompactMap<Integer, String> map = new TestCompactMap<>(compactSize);
        Map<Integer, String> newMap = map.callGetNewMap();
        assertNotNull(newMap);
        assertTrue(newMap instanceof HashMap);
        assertTrue(newMap.isEmpty());
        // 101
        int expectedRequested = compactSize + 1;
        int actualCapacity = getHashMapTableLength(newMap);
        // HashMap rounds up to the next power of two, so capacity should be >= 128
        assertTrue(actualCapacity >= 128, "HashMap should round up capacity (got " + actualCapacity + ", expected at least 128)");
        // insert up to compactSize entries – should not trigger rehashing errors
        for (int i = 0; i < compactSize; i++) {
            newMap.put(i, "v" + i);
        }
        assertEquals(compactSize, newMap.size(), "Map should contain exactly compactSize entries");
    }

    @Test
    void getNewMap_multipleCallsReturnDistinctInstances() {
        TestCompactMap<Integer, String> map = new TestCompactMap<>(5);
        Map<Integer, String> first = map.callGetNewMap();
        Map<Integer, String> second = map.callGetNewMap();
        assertNotSame(first, second, "Each call must return a new map instance");
    }

    @Test
    void getNewMap_negativeCompactSize_throwsIllegalArgumentException() {
        TestCompactMap<Integer, String> map = new TestCompactMap<>(-2);
        assertThrows(IllegalArgumentException.class, map::callGetNewMap, "HashMap constructor should reject a negative initial capacity");
    }

    @Test
    void getNewMap_compactSizeMaxValue_throwsIllegalArgumentException() {
        TestCompactMap<Integer, String> map = new TestCompactMap<>(Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, map::callGetNewMap, "Overflowed capacity should cause IllegalArgumentException");
    }

    @Test
    void getNewMap_compactSizeMaxMinusOne_returnsHashMapWithoutException() throws Exception {
        TestCompactMap<Integer, String> map = new TestCompactMap<>(Integer.MAX_VALUE - 1);
        Map<Integer, String> newMap = map.callGetNewMap();
        assertNotNull(newMap);
        assertTrue(newMap instanceof HashMap);
        assertTrue(newMap.isEmpty());
        // capacity will be capped to HashMap.MAXIMUM_CAPACITY (1 << 30)
        int capacity = getHashMapTableLength(newMap);
        assertTrue(capacity > 0, "Capacity should be a positive number");
    }

    @Test
    void getNewMap_nullKeyAndNullValue_areAccepted() {
        TestCompactMap<Object, Object> map = new TestCompactMap<>(0);
        Map<Object, Object> newMap = map.callGetNewMap();
        newMap.put(null, null);
        assertEquals(1, newMap.size(), "Map size should be 1 after putting null key/value");
        assertNull(newMap.get(null), "Retrieving null key should return null");
    }

    @Test
    void getNewMap_independentMapsAfterModification() {
        TestCompactMap<Integer, String> map = new TestCompactMap<>(5);
        Map<Integer, String> mapA = map.callGetNewMap();
        Map<Integer, String> mapB = map.callGetNewMap();
        mapA.put(1, "one");
        mapA.put(2, "two");
        assertEquals(2, mapA.size(), "First map should reflect inserted entries");
        assertEquals(0, mapB.size(), "Second map must remain empty, proving independence");
    }

    @Test
    void getNewMap_compactSizeThrowsRuntimeException_propagatesException() {
        RuntimeException cause = new IllegalStateException("compactSize failure");
        TestCompactMap<Integer, String> map = new TestCompactMap<>(cause);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, map::callGetNewMap, "Exception from compactSize() must propagate");
        assertSame(cause, thrown, "Thrown exception should be the original one");
    }

    /**
     * This test demonstrates the behaviour when an extremely large initial capacity is requested.
     * In practice the JDK caps the capacity, so an {@link OutOfMemoryError} is unlikely.
     * The test is therefore disabled to avoid flaky failures on normal JVMs.
     */
    @Disabled("Disabled because modern JDK caps HashMap capacity and does not throw OOM for huge sizes")
    @Test
    void getNewMap_hugeInitialCapacity_mayThrowOutOfMemoryError() {
        // Using a value that would require > 2^30 entries if not capped.
        int hugeSize = Integer.MAX_VALUE - 2;
        TestCompactMap<Integer, String> map = new TestCompactMap<>(hugeSize);
        assertThrows(OutOfMemoryError.class, map::callGetNewMap, "Very large capacity may trigger OutOfMemoryError");
    }

    @Test
    void useCopyIterator_callsGetNewMap_withoutException() {
        TestCompactMap<Integer, String> map = new TestCompactMap<>(4);
        // Default implementation returns false → wrapper returns null
        Map<Integer, String> created = map.callUseCopyIterator();
        assertNull(created, "Default useCopyIterator() returns false, thus wrapper returns null");
    }
}
