package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_isEmpty_Test {

    /**
     * Map instance used by the tests.
     */
    private CompactMap<String, Object> map;

    /**
     * Create a fresh map before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        map = new CompactMap<>() {

            // Use a small compact size so we can trigger the different internal states.
            @Override
            protected int compactSize() {
                return 3;
            }

            // The key that enables the single‑value optimisation.
            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            // Backing map when the compact size is exceeded.
            @Override
            protected Map<String, Object> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
    }

    /**
     * No special teardown required.
     */
    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the map to help the GC in long test runs.
        if (map != null) {
            map.clear();
        }
    }

//     static Stream<Arguments> basicBehaviorProvider() {
//         return Stream.of(Arguments.of("newlyCreated", (Runnable) () -> {
//             CompactMap<String, Object> m = new CompactMap<>();
//             assertTrue(m.isEmpty());
//             assertEquals(0, m.size());
//         }), Arguments.of("afterPut", (Runnable) () -> {
//             CompactMap<String, Object> m = new CompactMap<>();
//             assertNull(m.put("k", "v"));
//             assertFalse(m.isEmpty());
//             assertEquals(1, m.size());
//         }), Arguments.of("afterClear", (Runnable) () -> {
//             CompactMap<String, Object> m = new CompactMap<>();
//             m.put("k", "v");
//             m.clear();
//             assertTrue(m.isEmpty());
//             assertEquals(0, m.size());
//         }));
//     }

//     @ParameterizedTest(name = "{index} => scenario={0}")
//     @MethodSource("basicBehaviorProvider")
//     void testBasicBehaviorParameterized(String scenario, Runnable testBody) {
//         // Each scenario runs its own isolated code block.
//         testBody.run();
//     }

    @Test
    void testIsEmptyWhenCreated() {
        assertTrue(map.isEmpty(), "A newly created CompactMap should be empty");
        assertEquals(0, map.size(), "Size of a newly created CompactMap should be 0");
    }

    @Test
    void testIsEmptyAfterSinglePut() {
        assertNull(map.put("value", 10.0d), "First put should return null");
        assertFalse(map.isEmpty(), "Map should not be empty after one entry is added");
        assertEquals(1, map.size(), "Size should be 1 after one entry is added");
    }

    @Test
    void testIsEmptyAfterRemoveAll() {
        map.put("value", 10.0d);
        map.put("alpha", "beta");
        assertEquals(2, map.size(), "Map should contain two entries before removal");
        // Remove both entries
        map.remove("alpha");
        map.remove("value");
        assertTrue(map.isEmpty(), "Map should be empty after all entries are removed");
        assertEquals(0, map.size(), "Size should be 0 after all entries are removed");
    }

    @Test
    void testIsEmptyAfterClear() {
        map.put("value", 1);
        map.put("other", 2);
        assertFalse(map.isEmpty(), "Map should not be empty before clear");
        map.clear();
        assertTrue(map.isEmpty(), "Map should be empty after clear");
        assertEquals(0, map.size(), "Size should be 0 after clear");
    }

    @Test
    void testIsEmptyWithBuilder() {
        CompactMap<String, Object> built = CompactMap.<String, Object>builder().compactSize(3).build();
        assertTrue(built.isEmpty(), "Map built via builder should be empty initially");
        assertEquals(0, built.size(), "Builder‑created map should have size 0");
    }

    @Test
    void testIsEmptyWhenCopyingEmptyMap() {
        Map<String, Object> emptySource = new HashMap<>();
        CompactMap<String, Object> copy = new CompactMap<>(emptySource);
        assertTrue(copy.isEmpty(), "Copy of an empty map should be empty");
        assertEquals(0, copy.size(), "Copy of an empty map should have size 0");
    }

    @Test
    void testIsEmptyWhenCopyingNonEmptyMap() {
        Map<String, Object> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        CompactMap<String, Object> copy = new CompactMap<>(source);
        assertFalse(copy.isEmpty(), "Copy of a non‑empty map should not be empty");
        assertEquals(2, copy.size(), "Size should match the source map size");
    }

    @Test
    void testIsEmptyWhenValReplacedViaReflection() throws Exception {
        // Obtain the protected field 'val' from CompactMap
        Field valField = CompactMap.class.getDeclaredField("val");
        valField.setAccessible(true);
        // Replace it with an arbitrary non‑sentinel object
        Object dummy = new Object();
        valField.set(map, dummy);
        // The map should now report non‑empty because reference equality fails
        assertFalse(map.isEmpty(), "isEmpty() must return false when val is not the sentinel");
    }

    @Test
    void testIsEmptyAfterRemoveSingleEntry() {
        map.put("value", 42);
        assertEquals(1, map.size(), "Pre‑condition: map should contain one entry");
        map.remove("value");
        assertTrue(map.isEmpty(), "Map should be empty after removing its only entry");
        assertEquals(0, map.size(), "Size should be 0 after removal");
    }

    @Test
    void testIsEmptyAfterPartialRemove() {
        map.put("value", 1);
        map.put("alpha", "beta");
        assertEquals(2, map.size(), "Pre‑condition: map should contain two entries");
        map.remove("alpha");
        assertFalse(map.isEmpty(), "Map should not be empty after removing only one entry");
        assertEquals(1, map.size(), "Size should be 1 after partial removal");
    }

    @Test
    void testIsEmptyPutAllWithEmptySource() {
        Map<String, Object> empty = Collections.emptyMap();
        map.putAll(empty);
        assertTrue(map.isEmpty(), "putAll(empty) must leave the map empty");
        assertEquals(0, map.size(), "Size must remain 0 after putAll(empty)");
    }

    @Test
    void testIsEmptyPutAllWithNonEmptySource() {
        Map<String, Object> src = new HashMap<>();
        src.put("k1", "v1");
        src.put("k2", "v2");
        map.putAll(src);
        assertFalse(map.isEmpty(), "putAll(non‑empty) must make the map non‑empty");
        assertEquals(2, map.size(), "Size must reflect the source map size");
    }

    @Test
    void testIsEmptyWithNullKey() {
        map.put(null, "notNull");
        assertFalse(map.isEmpty(), "Map with null key should not be empty");
        assertEquals(1, map.size());
    }

    @Test
    void testIsEmptyWithNullValue() {
        map.put("notNullKey", null);
        assertFalse(map.isEmpty(), "Map with null value should not be empty");
        assertEquals(1, map.size());
    }

    @Test
    void testIsEmptyWithNullKeyAndValue() {
        map.put(null, null);
        assertFalse(map.isEmpty(), "Map with null key and null value should not be empty");
        assertEquals(1, map.size());
    }

    @Test
    void testIsEmptyIdempotentClearOnFreshMap() {
        // map is already empty
        map.clear();
        assertTrue(map.isEmpty(), "Calling clear() on an already empty map must keep it empty");
        assertEquals(0, map.size());
    }

    @Test
    void testIsEmptyIdempotentClearAfterNonEmpty() {
        map.put("a", 1);
        map.put("b", 2);
        // first clear – should reset to empty
        map.clear();
        assertTrue(map.isEmpty(), "Map should be empty after first clear");
        // second clear – no side‑effects expected
        map.clear();
        assertTrue(map.isEmpty(), "Map should remain empty after second clear");
        assertEquals(0, map.size());
    }

    @Test
    void testIsEmptyBuilderCreatesEmptyMap() {
        CompactMap<String, Object> built = CompactMap.<String, Object>builder().compactSize(3).build();
        assertTrue(built.isEmpty(), "Builder‑created map should be empty initially");
        assertEquals(0, built.size());
    }

    @Test
    void testIsEmptyBuilderWithPrepopulatedSource() {
        Map<String, Object> src = Collections.singletonMap("k", "v");
        CompactMap<String, Object> built = CompactMap.<String, Object>builder().compactSize(3).sourceMap(src).build();
        assertFalse(built.isEmpty(), "Builder‑created map with source entries must not be empty");
        assertEquals(1, built.size());
    }
}