package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;

// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;
// import static org.junit.Assert.*;
class CompactMap_containsValue_Test {

    /**
     * Map under test – a CompactMap with a very small compactSize so we can
     *  trigger the different internal storage states (array → backing map).
     */
    private CompactMap<String, Integer> map;

    //     @BeforeEach
    void setupBeforeEach() {
        // Create an anonymous subclass that forces a compactSize of 2 and uses a plain HashMap as the backing map.
        map = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                // switch to a backing map after 2 entries
                return 2;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                // The key used for the single‑value optimisation – not important for these tests.
                return "singleKey";
            }
        };
    }

    //     @AfterEach
    void teardownAfterEach() throws Exception {
        // Ensure the map is cleared for the next test.
        // Reset the internal 'val' field to the EMPTY_MAP sentinel via reflection.
        Field emptyMapField = CompactMap.class.getDeclaredField("EMPTY_MAP");
        emptyMapField.setAccessible(true);
        Object emptyMap = emptyMapField.get(null);
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        f.set(map, emptyMap);
        map.clear();
        map = null;
    }

    /**
     * Creates a new {@code CompactMap} instance that forces a {@code compactSize}
     *  of {@code 2} and uses a plain {@link HashMap} as the backing map.
     */
    private void setUp() {
        map = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                // switch to a backing map after 2 entries
                return 2;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                // The key used for the single‑value optimisation – not important for these tests.
                return "singleKey";
            }
        };
    }

    /**
     * Restores the internal state of the map so that subsequent runs start from a clean slate.
     */
    private void tearDown() throws Exception {
        // Reset the internal 'val' field to the EMPTY_MAP sentinel via reflection.
        Field emptyMapField = CompactMap.class.getDeclaredField("EMPTY_MAP");
        emptyMapField.setAccessible(true);
        Object emptyMap = emptyMapField.get(null);
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        f.set(map, emptyMap);
        map.clear();
        map = null;
    }

    private void testContainsValueEmptyMap() {
        if (!map.isEmpty())
            throw new AssertionError("Map should be empty");
        if (map.containsValue("any"))
            throw new AssertionError("Empty map must not contain any value");
        if (map.containsValue(null))
            throw new AssertionError("Empty map must not contain null");
    }

    private void testContainsValueSingleEntry() {
        if (map.put("one", 42) != null)
            throw new AssertionError("First put should return null");
        if (!map.containsValue(42))
            throw new AssertionError("Map should contain value 42");
        if (map.containsValue(99))
            throw new AssertionError("Map must not contain value 99");
        if (map.containsValue(null))
            throw new AssertionError("Map must not contain null yet");
        // Replace the value with null and verify.
        map.put("one", null);
        if (!map.containsValue(null))
            throw new AssertionError("Map should now contain null");
    }

    private void testContainsValueCompactArrayState() {
        // With compactSize == 2, two entries keep the internal storage as an Object[] (compact array).
        if (map.put("a", 1) != null)
            throw new AssertionError("First put should return null");
        if (map.put("b", 2) != null)
            throw new AssertionError("Second put should return null");
        if (!map.containsValue(1))
            throw new AssertionError("Map should contain value 1");
        if (!map.containsValue(2))
            throw new AssertionError("Map should contain value 2");
        if (map.containsValue(3))
            throw new AssertionError("Map must not contain value 3");
    }

    private void testContainsValueBackingMapState() {
        // Adding a third entry forces the internal representation to become a Map.
        if (map.put("x", 10) != null)
            throw new AssertionError("First put should return null");
        if (map.put("y", 20) != null)
            throw new AssertionError("Second put should return null");
        if (map.put("z", 30) != null)
            throw new AssertionError("Third put should return null (triggers map switch)");
        if (!map.containsValue(10))
            throw new AssertionError("Map should contain 10");
        if (!map.containsValue(20))
            throw new AssertionError("Map should contain 20");
        if (!map.containsValue(30))
            throw new AssertionError("Map should contain 30");
        if (map.containsValue(40))
            throw new AssertionError("Map must not contain 40");
    }

    private void testContainsValueWithNullInBackingMap() {
        // Force backing‑map state first.
        map.put("k1", 1);
        map.put("k2", 2);
        // > compactSize → backing map
        map.put("k3", 3);
        // Insert a null value.
        map.put("nullKey", null);
        if (!map.containsValue(null))
            throw new AssertionError("Map should contain null");
    }

    private void testContainsValueArrayRepresentation() {
        // Build the array: [k1, "apple", k2, "banana", k3, null]
        Object[] entries = new Object[] { "k1", "apple", "k2", "banana", "k3", null };
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(map, entries);
        } catch (Exception e) {
            throw new AssertionError("Failed to inject array representation", e);
        }
        if (!map.containsValue("apple"))
            throw new AssertionError("Array should contain \"apple\"");
        if (map.containsValue("orange"))
            throw new AssertionError("Array must not contain \"orange\"");
        if (map.containsValue("null"))
            throw new AssertionError("String \"null\" must not be treated as null");
        if (!map.containsValue(null))
            throw new AssertionError("Array should contain a real null value");
    }

    private void testContainsValueMapRepresentation() {
        Map<String, Object> backing = new HashMap<>();
        backing.put("a", "apple");
        backing.put("b", "pear");
        backing.put("c", "cherry");
        // Add a null entry to satisfy the null‑value case.
        backing.put("d", null);
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(map, backing);
        } catch (Exception e) {
            throw new AssertionError("Failed to inject map representation", e);
        }
        if (!map.containsValue("pear"))
            throw new AssertionError("Map should contain \"pear\"");
        if (map.containsValue("banana"))
            throw new AssertionError("Map must not contain \"banana\"");
        if (!map.containsValue(null))
            throw new AssertionError("Map should contain null");
    }

    private void testContainsValueArrayOddLengthThrows() {
        Object[] oddArray = new Object[] { "k1", "v1", "k2" };
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(map, oddArray);
        } catch (Exception e) {
            throw new AssertionError("Failed to inject odd‑length array", e);
        }
        try {
            map.containsValue("any");
            throw new AssertionError("Expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException expected) {
            // expected – test passes
        }
    }

    private void testContainsValueWhenValIsNullThrows() {
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(map, null);
        } catch (Exception e) {
            throw new AssertionError("Failed to set val to null", e);
        }
        try {
            map.containsValue("any");
            throw new AssertionError("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected – test passes
        }
    }

    private void testContainsValueBackingMapThrowsRuntimeException() {
        Map<String, Integer> explodingMap = new HashMap<String, Integer>() {

            @Override
            public boolean containsValue(Object value) {
                throw new RuntimeException("boom");
            }
        };
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(map, explodingMap);
        } catch (Exception e) {
            throw new AssertionError("Failed to inject exploding map", e);
        }
        try {
            map.containsValue("any");
            throw new AssertionError("Expected RuntimeException");
        } catch (RuntimeException ex) {
            if (!"boom".equals(ex.getMessage()))
                throw new AssertionError("Unexpected RuntimeException message: " + ex.getMessage());
            // expected – test passes
        }
    }

    /**
     * Executes all demo tests.
     */
    private void runAllTests() throws Exception {
        setUp();
        testContainsValueEmptyMap();
        tearDown();
        setUp();
        testContainsValueSingleEntry();
        tearDown();
        setUp();
        testContainsValueCompactArrayState();
        tearDown();
        setUp();
        testContainsValueBackingMapState();
        tearDown();
        setUp();
        testContainsValueWithNullInBackingMap();
        tearDown();
        setUp();
        testContainsValueArrayRepresentation();
        tearDown();
        setUp();
        testContainsValueMapRepresentation();
        tearDown();
        setUp();
        testContainsValueArrayOddLengthThrows();
        tearDown();
        setUp();
        testContainsValueWhenValIsNullThrows();
        tearDown();
        setUp();
        testContainsValueBackingMapThrowsRuntimeException();
        tearDown();
    }

    /**
     * Main entry point – runs the demo.
     */
//     public static void main(String[] args) throws Exception {
//         new CompactMapContainsValueDemo().runAllTests();
//         System.out.println("All CompactMap.containsValue() demo checks passed.");
//     }
}