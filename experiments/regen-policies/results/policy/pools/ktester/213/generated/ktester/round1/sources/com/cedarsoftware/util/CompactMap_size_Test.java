package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
// import org.junit.*;
// import org.junit.runner.RunWith;
import org.junit.jupiter.api.*;
// import org.junit.runners.Parameterized;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class CompactMap_size_Test {

    //     @RunWith(Parameterized.class)
    public static class DirectValManipulationTest {

        private final Object valToInject;

        private final int expectedSize;

        private final String description;

        public DirectValManipulationTest(Object valToInject, int expectedSize, String description) {
            this.valToInject = valToInject;
            this.expectedSize = expectedSize;
            this.description = description;
        }
        //         @Test
        //         public void sizeWithDirectValManipulation() throws Exception {
        //             CompactMap<String, Object> map = new CompactMap<>() {
        //
        //                 @Override
        //                 protected String getSingleValueKey() {
        //                     return "id";
        //                 }
        //
        //                 @Override
        //                 protected Map<String, Object> getNewMap() {
        //                     return new LinkedHashMap<>();
        //                 }
        //             };
        //             Field valField = CompactMap.class.getDeclaredField("val");
        //             valField.setAccessible(true);
        //             valField.set(map, valToInject);
        //             Assert.assertEquals(description, expectedSize, map.size());
        //         }
    }

    /**
     * Supplies the arguments for {@link #sizeWithDirectValManipulation(Object, int, String)}.
     */
    private static Stream<Arguments> sizeProvider() {
        // 1. Object[] length 4 → 4>>1 = 2
        Object[] arrayLen4 = new Object[] { "k0", "v0", "k1", "v1" };
        // 2. Object[] length 3 (odd) → 3>>1 = 1
        Object[] arrayLen3 = new Object[] { "k0", "v0", "k1" };
        // 3. Map with three entries
        Map<String, Integer> threeEntryMap = new LinkedHashMap<>();
        threeEntryMap.put("a", 1);
        threeEntryMap.put("b", 2);
        threeEntryMap.put("c", 3);
        // 4. EMPTY_MAP sentinel (private – obtain via reflection)
        String emptySentinel;
        try {
            Field f = CompactMap.class.getDeclaredField("EMPTY_MAP");
            f.setAccessible(true);
            emptySentinel = (String) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 5. Non‑array, non‑Map object (e.g., a plain String)
        String plainString = "just a string";
        // 6. Empty Object[] → length 0 >> 1 = 0
        Object[] emptyArray = new Object[] {};
        // 7. Object[] length 1 → 1>>1 = 0
        Object[] arrayLen1 = new Object[] { "onlyKey" };
        // 8. Empty Map instance
        Map<String, String> emptyMap = new HashMap<>();
        // 9. null value
        Object nullVal = null;
        return Stream.of(Arguments.of(arrayLen4, 2, "Object[] length 4 (even) → size 2"), Arguments.of(arrayLen3, 1, "Object[] length 3 (odd) → size 1"), Arguments.of(threeEntryMap, 3, "Map with three entries → size 3"), Arguments.of(emptySentinel, 0, "EMPTY_MAP sentinel → size 0"), Arguments.of(plainString, 1, "Non‑array, non‑Map object → size 1"), Arguments.of(emptyArray, 0, "Empty Object[] → size 0"), Arguments.of(arrayLen1, 0, "Object[] length 1 → size 0"), Arguments.of(emptyMap, 0, "Empty Map instance → size 0"), Arguments.of(nullVal, 1, "null val → size 1"));
    }

    /**
     * Verify that a newly‑created CompactMap reports size 0 and is empty.
     */
    @Test
    void sizeWhenEmpty() {
        CompactMap<String, Object> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected Map<String, Object> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    /**
     * Verify that a map containing exactly one entry reports size 1.
     */
    @Test
    void sizeWhenSingleEntry() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "single";
            }

            @Override
            protected int compactSize() {
                // any value >= 2 works
                return 4;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertEquals(0, map.size());
        map.put("single", 42);
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
    }

    /**
     * Verify that when the number of entries is between 2 and {@code compactSize}
     * the map stores data in the compact array representation and size() returns the
     * correct number of key‑value pairs.
     */
    @Test
    void sizeWhenCompactArray() {
        final int compactSize = 5;
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return compactSize;
            }

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(3, map.size());
        assertFalse(map.isEmpty());
    }

    /**
     * Verify that when more than {@code compactSize} entries are added the map
     * switches to a backing {@link java.util.Map} implementation and size()
     * reflects the number of entries stored in that map.
     */
    @Test
    void sizeWhenBackingMap() {
        final int compactSize = 4;
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return compactSize;
            }

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        for (int i = 0; i < compactSize + 2; i++) {
            map.put("k" + i, i);
        }
        int expectedSize = compactSize + 2;
        assertEquals(expectedSize, map.size());
        assertFalse(map.isEmpty());
        map.remove("k0");
        map.remove("k1");
        assertEquals(expectedSize - 2, map.size());
    }

    /**
     * Parameterized test that sets {@code CompactMap.val} to various objects
     * and verifies that {@link CompactMap#size()} returns the expected value.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("sizeProvider")
    void sizeWithDirectValManipulation(Object valToInject, int expectedSize, String description) throws Exception {
        CompactMap<String, Object> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected Map<String, Object> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        Field valField = CompactMap.class.getDeclaredField("val");
        valField.setAccessible(true);
        valField.set(map, valToInject);
        assertEquals(expectedSize, map.size(), description);
    }
}
