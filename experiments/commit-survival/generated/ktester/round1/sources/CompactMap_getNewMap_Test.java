package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactMap_getNewMap_Test {

    /**
     * Simple subclass that makes {@code getNewMap()} publicly accessible.
     */
    private static class ExposedCompactMap<K, V> extends CompactMap<K, V> {

        ExposedCompactMap() {
            super();
        }

        /**
         * Allows test code to call the protected method.
         */
        public Map<K, V> getNewMapPublic() {
            return super.getNewMap();
        }
    }

    /**
     * Subclass that overrides {@code compactSize()} to a custom value.
     */
    private static class CustomSizeCompactMap<K, V> extends CompactMap<K, V> {

        private final int customSize;

        CustomSizeCompactMap(int customSize) {
            this.customSize = customSize;
        }

        @Override
        protected int compactSize() {
            return customSize;
        }

        public Map<K, V> getNewMapPublic() {
            return super.getNewMap();
        }
    }

    /**
     * Subclass that changes its compactSize() value at runtime.
     */
    private static class DynamicCompactMap<K, V> extends CompactMap<K, V> {

        private int sizeValue;

        DynamicCompactMap(int initial) {
            this.sizeValue = initial;
        }

        void setCompactSize(int newSize) {
            this.sizeValue = newSize;
        }

        @Override
        protected int compactSize() {
            return sizeValue;
        }

        public Map<K, V> getNewMapPublic() {
            return super.getNewMap();
        }
    }

    /**
     * Subclass whose compactSize() deliberately throws an unchecked exception.
     */
    private static class ThrowingCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            throw new IllegalStateException("compactSize failure");
        }

        public Map<K, V> getNewMapPublic() {
            return super.getNewMap();
        }
    }

    private static Stream<Integer> negativeCompactSizeProvider() {
        return Stream.of(-2, -3, -4, Integer.MIN_VALUE);
    }

    /**
     * Verify that the default implementation returns a new, empty {@link HashMap}.
     */
    @Test
    void testGetNewMapReturnsEmptyHashMap() {
        ExposedCompactMap<Integer, String> map = new ExposedCompactMap<>();
        Map<Integer, String> newMap = map.getNewMapPublic();
        assertNotNull(newMap, "getNewMap() must not return null");
        assertTrue(newMap instanceof HashMap, "Default implementation should return a HashMap");
        assertTrue(newMap.isEmpty(), "Returned map should be empty");
    }

    /**
     * Verify that each call returns a distinct map instance.
     */
    @Test
    void testGetNewMapReturnsDifferentInstances() {
        ExposedCompactMap<Integer, String> map = new ExposedCompactMap<>();
        Map<Integer, String> first = map.getNewMapPublic();
        Map<Integer, String> second = map.getNewMapPublic();
        assertNotSame(first, second, "Each invocation should produce a new map instance");
    }

    /**
     * Verify that the map created respects the overridden {@code compactSize()} value.
     * The initial capacity of a {@link HashMap} is the next power‑of‑two greater than or equal
     * to {@code compactSize() + 1}.  We inspect the internal {@code threshold} field (capacity *
     * load factor) via reflection to confirm the expected capacity.
     */
    @Test
    void testGetNewMapRespectsCustomCompactSize() throws Exception {
        // arbitrary small size for test
        final int customCompactSize = 5;
        CustomSizeCompactMap<Integer, String> map = new CustomSizeCompactMap<>(customCompactSize);
        Map<Integer, String> newMap = map.getNewMapPublic();
        assertTrue(newMap instanceof HashMap, "Custom size map should still be a HashMap");
        // Use reflection to read the private 'threshold' field of HashMap.
        // The threshold = capacity * loadFactor (default loadFactor = 0.75).
        // For an initial capacity of (customCompactSize + 1) = 6,
        // the HashMap rounds up to the next power of two => 8.
        // Expected threshold = (int)(8 * 0.75) = 6.
        Field thresholdField = HashMap.class.getDeclaredField("threshold");
        thresholdField.setAccessible(true);
        int threshold = (int) thresholdField.get(newMap);
        // next power of two >= 6
        int expectedCapacity = 8;
        int expectedThreshold = (int) (expectedCapacity * 0.75f);
        assertEquals(expectedThreshold, threshold, "HashMap threshold should reflect the capacity derived from custom compactSize");
    }

    /**
     * Positive compactSize (5) – map should be empty and fully mutable.
     */
    @Test
    void testPositiveCompactSizeFive() {
        CustomSizeCompactMap<Integer, String> map = new CustomSizeCompactMap<>(5);
        Map<Integer, String> newMap = map.getNewMapPublic();
        assertTrue(newMap.isEmpty(), "Map must start empty");
        assertDoesNotThrow(() -> newMap.put(1, "one"));
        assertEquals("one", newMap.get(1));
        assertDoesNotThrow(() -> newMap.remove(1));
    }

    /**
     * compactSize() returns 0 – HashMap should be created with capacity 1.
     */
    @Test
    void testCompactSizeZero() {
        CustomSizeCompactMap<Integer, String> map = new CustomSizeCompactMap<>(0);
        Map<Integer, String> newMap = map.getNewMapPublic();
        assertTrue(newMap.isEmpty());
        // Verify mutability
        newMap.put(10, "ten");
        assertEquals("ten", newMap.get(10));
        newMap.clear();
        assertTrue(newMap.isEmpty());
    }

    /**
     * compactSize() returns -1 – HashMap treats capacity 0 as default, no exception.
     */
    @Test
    void testCompactSizeMinusOne() {
        CustomSizeCompactMap<Integer, String> map = new CustomSizeCompactMap<>(-1);
        Map<Integer, String> newMap = map.getNewMapPublic();
        assertTrue(newMap instanceof HashMap);
        assertTrue(newMap.isEmpty());
        // Mutability checks
        newMap.put(2, "two");
        assertEquals("two", newMap.get(2));
        newMap.remove(2);
        assertTrue(newMap.isEmpty());
    }

    /**
     * compactSize() returns -2 – should cause IllegalArgumentException from HashMap.
     */
    @Test
    void testCompactSizeMinusTwoThrows() {
        CustomSizeCompactMap<Integer, String> map = new CustomSizeCompactMap<>(-2);
        assertThrows(IllegalArgumentException.class, map::getNewMapPublic, "HashMap constructor must reject negative initial capacity (-1)");
    }

    /**
     * compactSize() returns -3 – also IllegalArgumentException.
     */
    @Test
    void testCompactSizeMinusThreeThrows() {
        CustomSizeCompactMap<Integer, String> map = new CustomSizeCompactMap<>(-3);
        assertThrows(IllegalArgumentException.class, map::getNewMapPublic, "HashMap constructor must reject negative initial capacity (-2)");
    }

    /**
     * compactSize() returns Integer.MIN_VALUE – results in negative capacity, expect exception.
     */
    @Test
    void testCompactSizeIntegerMinValueThrows() {
        CustomSizeCompactMap<Integer, String> map = new CustomSizeCompactMap<>(Integer.MIN_VALUE);
        assertThrows(IllegalArgumentException.class, map::getNewMapPublic, "Negative capacity caused by Integer.MIN_VALUE should throw IllegalArgumentException");
    }

    /**
     * compactSize() returns Integer.MAX_VALUE – overflow to negative, expect exception.
     */
    @Test
    void testCompactSizeIntegerMaxValueThrows() {
        CustomSizeCompactMap<Integer, String> map = new CustomSizeCompactMap<>(Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, map::getNewMapPublic, "Overflowed capacity (Integer.MIN_VALUE) must cause IllegalArgumentException");
    }

    /**
     * compactSize() returns Integer.MAX_VALUE - 1 – should succeed, capacity capped at HashMap.MAXIMUM_CAPACITY.
     */
    @Test
    void testCompactSizeMaxMinusOneSucceeds() {
        CustomSizeCompactMap<Integer, String> map = new CustomSizeCompactMap<>(Integer.MAX_VALUE - 1);
        Map<Integer, String> newMap = map.getNewMapPublic();
        assertTrue(newMap instanceof HashMap);
        assertTrue(newMap.isEmpty());
        // Simple put/get to prove mutability
        newMap.put(123, "value");
        assertEquals("value", newMap.get(123));
    }

    /**
     * Verify that map type is HashMap and that mutable operations succeed (compactSize = 2).
     */
    @Test
    void testMapTypeAndMutability() {
        CustomSizeCompactMap<Integer, String> map = new CustomSizeCompactMap<>(2);
        Map<Integer, String> newMap = map.getNewMapPublic();
        assertTrue(newMap.getClass().getName().contains("HashMap"));
        // Mutability
        newMap.put(1, "a");
        newMap.put(2, "b");
        assertEquals(2, newMap.size());
        newMap.remove(1);
        assertFalse(newMap.containsKey(1));
        newMap.clear();
        assertTrue(newMap.isEmpty());
    }

    /**
     * Ensure successive calls produce distinct map instances (compactSize = 2).
     */
    @Test
    void testDistinctInstancesOnSuccessiveCalls() {
        CustomSizeCompactMap<Integer, String> map = new CustomSizeCompactMap<>(2);
        Map<Integer, String> first = map.getNewMapPublic();
        Map<Integer, String> second = map.getNewMapPublic();
        assertNotSame(first, second, "Two calls must return different map objects");
    }

    /**
     * Dynamic compactSize – each call should reflect the current value.
     */
    @Test
    void testDynamicCompactSizePerInvocation() throws Exception {
        DynamicCompactMap<Integer, String> map = new DynamicCompactMap<>(1);
        Map<Integer, String> first = map.getNewMapPublic();
        // Capture threshold after first creation (capacity based on compactSize = 1 -> initCap = 2 -> power‑of‑2 = 2)
        Field thresholdField = HashMap.class.getDeclaredField("threshold");
        thresholdField.setAccessible(true);
        int firstThreshold = (int) thresholdField.get(first);
        // Change compactSize and create a second map
        map.setCompactSize(4);
        Map<Integer, String> second = map.getNewMapPublic();
        int secondThreshold = (int) thresholdField.get(second);
        // Expected capacities: (1+1)=2 => power‑of‑2 2, threshold = 2*0.75 = 1
        // (4+1)=5 => power‑of‑2 8, threshold = 8*0.75 = 6
        assertEquals(1, firstThreshold, "First map threshold should correspond to compactSize 1");
        assertEquals(6, secondThreshold, "Second map threshold should correspond to compactSize 4");
    }

    /**
     * Calling getNewMap() must not alter the CompactMap's own observable state.
     */
    @Test
    void testNoSideEffectsOnCompactMapState() {
        ExposedCompactMap<Integer, String> map = new ExposedCompactMap<>();
        // should be 0 for a fresh CompactMap
        int sizeBefore = map.size();
        map.getNewMapPublic();
        int sizeAfter = map.size();
        assertEquals(sizeBefore, sizeAfter, "CompactMap size must remain unchanged after getNewMap()");
    }

    /**
     * compactSize() throws unchecked exception – it must propagate unchanged.
     */
    @Test
    void testCompactSizeThrowsUncheckedException() {
        ThrowingCompactMap<Integer, String> map = new ThrowingCompactMap<>();
        IllegalStateException ex = assertThrows(IllegalStateException.class, map::getNewMapPublic, "Exception from compactSize() should propagate");
        assertEquals("compactSize failure", ex.getMessage());
    }

    /**
     * Parameterized test for compactSize values that should cause IllegalArgumentException.
     */
    @ParameterizedTest(name = "compactSize = {0} should throw IllegalArgumentException")
    @MethodSource("negativeCompactSizeProvider")
    void testNegativeCompactSizeThrows(int badSize) {
        CustomSizeCompactMap<Integer, String> map = new CustomSizeCompactMap<>(badSize);
        assertThrows(IllegalArgumentException.class, map::getNewMapPublic);
    }
}
