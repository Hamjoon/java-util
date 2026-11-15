package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactLinkedMap_getNewMap_Test {

    private TestableCompactLinkedMap<String, Integer> mapUnderTest;

    /**
     * Concrete subclass that makes {@code getNewMap()} public and allows us to
     * control the value returned by {@code compactSize()}.
     */
    private static class TestableCompactLinkedMap<K, V> extends CompactLinkedMap<K, V> {

        private final int compactSize;

        TestableCompactLinkedMap(int compactSize) {
            this.compactSize = compactSize;
        }

        @Override
        protected int compactSize() {
            return compactSize;
        }

        /**
         * Expose the protected method for testing.
         */
        public Map<K, V> exposeGetNewMap() {
            return super.getNewMap();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        mapUnderTest = new TestableCompactLinkedMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
        mapUnderTest = null;
    }

    /**
     * Provides arguments for {@link #getNewMap_behaviourBasedOnCompactSize(int, int, boolean)}.
     *
     * @return a stream of arguments:
     *         <ul>
     *         <li>compactSize value to be returned by the overridden method</li>
     *         <li>expected initial capacity hint (compactSize + 1)</li>
     *         <li>whether an {@link IllegalArgumentException} is expected</li>
     *         </ul>
     */
    private static Stream<Arguments> compactSizeProvider() {
        return Stream.of(// compactSize = 5  → expected capacity hint = 6, normal operation
        Arguments.of(5, 6, false), // compactSize = 0  → expected capacity hint = 1, normal operation
        Arguments.of(0, 1, false), // compactSize = Integer.MAX_VALUE → overflow → IllegalArgumentException
        Arguments.of(Integer.MAX_VALUE, -1, true));
    }

    @Test
    void getNewMap_returnsLinkedHashMapInstance() {
        Map<String, Integer> newMap = mapUnderTest.exposeGetNewMap();
        // Verify that the returned map is a LinkedHashMap and initially empty.
        assertNotNull(newMap, "The map returned by getNewMap() should not be null");
        assertTrue(newMap instanceof LinkedHashMap, "The map returned by getNewMap() should be an instance of LinkedHashMap");
        assertTrue(newMap.isEmpty(), "The newly created map should be empty");
    }

    @Test
    void getNewMap_preservesInsertionOrder() {
        Map<String, Integer> newMap = mapUnderTest.exposeGetNewMap();
        // Insert entries in a known order.
        newMap.put("first", 1);
        newMap.put("second", 2);
        newMap.put("third", 3);
        // Verify iteration order matches insertion order.
        String[] expectedKeys = { "first", "second", "third" };
        int index = 0;
        for (String key : newMap.keySet()) {
            assertEquals(expectedKeys[index++], key, "LinkedHashMap should preserve insertion order");
        }
    }

    @Test
    void getNewMap_capacityIsAtLeastOne() {
        // The internal capacity of LinkedHashMap is not directly accessible,
        // but we can verify that the map accepts at least one entry without resizing issues.
        Map<String, Integer> newMap = mapUnderTest.exposeGetNewMap();
        // Adding a single element should succeed.
        assertDoesNotThrow(() -> newMap.put("only", 42), "The map should accept at least one entry");
        // Adding many more entries should also succeed, confirming that the map works
        // beyond the initial capacity hint.
        for (int i = 0; i < 100; i++) {
            newMap.put("key" + i, i);
        }
        assertEquals(101, newMap.size(), "Map should contain the expected number of entries after bulk insertion");
    }

    /**
     * Verifies that {@code getNewMap()} creates a {@link LinkedHashMap} whose
     * initial capacity is derived from {@code compactSize() + 1}.  For the overflow
     * case we assert that an {@link IllegalArgumentException} is thrown.
     *
     * @param compactSize          value to be returned by the overridden {@code compactSize()}
     * @param expectedCapacityHint the value {@code compactSize + 1} (used only for documentation)
     * @param expectException      whether the test should expect an {@code IllegalArgumentException}
     */
    @ParameterizedTest(name = "compactSize={0}, expectedHint={1}, expectException={2}")
    @MethodSource("compactSizeProvider")
    void getNewMap_behaviourBasedOnCompactSize(int compactSize, int expectedCapacityHint, boolean expectException) {
        mapUnderTest = new TestableCompactLinkedMap<>(compactSize);
        if (expectException) {
            // The overflow case: constructing the LinkedHashMap should fail.
            assertThrows(IllegalArgumentException.class, () -> mapUnderTest.exposeGetNewMap(), "Expected IllegalArgumentException when compactSize() causes overflow");
            return;
        }
        // Normal case: a LinkedHashMap should be returned.
        Map<String, Integer> newMap = mapUnderTest.exposeGetNewMap();
        assertNotNull(newMap, "getNewMap() must not return null");
        assertTrue(newMap instanceof LinkedHashMap, "The map returned by getNewMap() should be an instance of LinkedHashMap");
        // The map should be initially empty.
        assertTrue(newMap.isEmpty(), "Newly created map must be empty");
        /*
         * The internal capacity of a HashMap (and therefore LinkedHashMap) is not
         * directly exposed.  The table is allocated lazily on the first insertion,
         * so we trigger the allocation by inserting a single entry and then inspect
         * the private 'threshold' field, which reflects the size of the underlying
         * table multiplied by the load factor (default 0.75).  The threshold value
         * therefore gives us insight into the actual table size.
         */
        // trigger internal table allocation
        newMap.put("dummy", 0);
        try {
            // Access the protected 'threshold' field from HashMap via reflection.
            Field thresholdField = HashMap.class.getDeclaredField("threshold");
            thresholdField.setAccessible(true);
            int threshold = (int) thresholdField.get(newMap);
            // Compute the expected table size: the smallest power of two >= (compactSize + 1)
            int expectedTableSize = 1;
            while (expectedTableSize < expectedCapacityHint) {
                expectedTableSize <<= 1;
            }
            // The threshold is tableSize * loadFactor (0.75) rounded down.
            int expectedThreshold = (int) (expectedTableSize * 0.75f);
            assertEquals(expectedThreshold, threshold, String.format("Threshold after first insertion should match expected value for compactSize=%d", compactSize));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Reflection failed while inspecting internal HashMap state: " + e);
        }
        // Additional sanity check: inserting up to (compactSize + 1) entries should succeed.
        // i.e., compactSize + 1
        int entriesToAdd = expectedCapacityHint;
        for (int i = 0; i < entriesToAdd; i++) {
            newMap.put("key" + i, i);
        }
        assertEquals(entriesToAdd + 1, newMap.size(), "Map should contain the expected number of entries after bulk insertion");
    }
}
