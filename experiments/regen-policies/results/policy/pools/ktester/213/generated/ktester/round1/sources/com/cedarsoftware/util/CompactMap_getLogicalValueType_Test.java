package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
// import org.junit.*;
// import org.junit.runner.RunWith;
// import static org.junit.Assert.*;
import org.junit.jupiter.api.DisplayName;
// @ExtendWith(MockitoExtension.class)
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompactMap_getLogicalValueType_Test {

    /* --------------------------------------------------------------
     *  Reflection based scenarios
     * -------------------------------------------------------------- */
    @Nested
    @DisplayName("Reflection based scenarios")
    class ReflectionScenarios {

        /**
         * Parameterized test that injects a value directly into the private {@code val}
         * field and checks that {@link CompactMap#getLogicalValueType()} reports the
         * expected logical type.
         */
        @ParameterizedTest(name = "{index} => set val to {2} → expect {1}")
        @MethodSource("com.cedarsoftware.util.CompactMap_getLogicalValueType_Test#reflectionCases")
        void testReflectionBasedLogicalValueType(Object internalValue, CompactMap.LogicalValueType expected, String description) {
            // any subclass works – we just need a concrete instance
            CompactMap<String, Integer> map = new EmptyMap();
            setInternalVal(map, internalValue);
            assertEquals(expected, map.getLogicalValueType(), "Reflection test failed for: " + description);
        }

        /**
         * Provides a {@link Stream} of arguments for {@link #testReflectionBasedLogicalValueType}.
         *
         * The arguments consist of:
         * <ul>
         *   <li>the object to assign to the internal {@code val} field</li>
         *   <li>the expected {@link CompactMap.LogicalValueType}</li>
         *   <li>a short description for the test name</li>
         * </ul>
         */
//         static Stream<Arguments> reflectionCases() {
//             // Obtain the private EMPTY_MAP sentinel via reflection
//             Object emptySentinel;
//             try {
//                 Field f = CompactMap.class.getDeclaredField("EMPTY_MAP");
//                 f.setAccessible(true);
//                 emptySentinel = f.get(null);
//             } catch (ReflectiveOperationException e) {
//                 throw new RuntimeException(e);
//             }
//             return Stream.of(// ARRAY – an Object[] (note: not a primitive array)
//             Arguments.of(new String[] { "a", "b" }, CompactMap.LogicalValueType.ARRAY, "Object[] array"), // MAP – a real Map instance
//             Arguments.of(new HashMap<>(), CompactMap.LogicalValueType.MAP, "HashMap instance"), // MAP – a Mockito mock of Map (verifies Mockito usage)
//             Arguments.of(mock(Map.class), CompactMap.LogicalValueType.MAP, "Mockito mock Map"), // EMPTY – the sentinel constant (already the default, but explicit)
//             Arguments.of(emptySentinel, CompactMap.LogicalValueType.EMPTY, "EMPTY_MAP sentinel"), // OBJECT – a plain Integer object
//             Arguments.of(Integer.valueOf(42), CompactMap.LogicalValueType.OBJECT, "Plain Integer object"), // OBJECT – null value (falls through to OBJECT)
//             Arguments.of(null, CompactMap.LogicalValueType.OBJECT, "null value"), // OBJECT – primitive int array (not an Object[])
//             Arguments.of(new int[] { 1, 2, 3 }, CompactMap.LogicalValueType.OBJECT, "primitive int[] array"));
//         }

        /**
         * Test that a {@link CompactMap.CompactMapEntry} stored directly in {@code val}
         *  yields the {@code ENTRY} logical type.
         */
        @Test
        @DisplayName("ENTRY – set internal val to a CompactMapEntry instance")
        void testEntryViaReflection() {
            // Create a map instance so we can instantiate the inner class
            CompactMap<String, Integer> map = new EntryMap();
            // Directly create a CompactMapEntry using the outer instance
            CompactMap<String, Integer>.CompactMapEntry entry = map.new CompactMapEntry("key", 77);
            // Inject the entry into the internal state
            setInternalVal(map, entry);
            assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType(), "When 'val' holds a CompactMapEntry the type must be ENTRY");
        }
    }

    /**
     * CompactMap that uses the default EMPTY sentinel.
     */
    private static class EmptyMap extends CompactMap<String, Integer> {
        // No overrides needed – defaults are sufficient.
    }

    /**
     * CompactMap that stores a single value directly (OBJECT) when the key
     *  matches the configured single‑value key.
     */
    private static class ObjectMap extends CompactMap<String, Integer> {

        @Override
        protected String getSingleValueKey() {
            return "id";
        }

        @Override
        protected int compactSize() {
            // any value ≥ 2 works
            return 5;
        }

//         @Override
//         protected Map<String, Integer> getNewMap() {
//             return new HashMap<>();
//         }
    }

    /**
     * CompactMap that stores a single entry via {@link CompactMap.CompactMapEntry}
     *  (ENTRY) when the key does **not** match the single‑value key.
     */
    private static class EntryMap extends CompactMap<String, Integer> {

        @Override
        protected String getSingleValueKey() {
            return "id";
        }

        @Override
        protected int compactSize() {
            return 5;
        }

//         @Override
//         protected Map<String, Integer> getNewMap() {
//             return new HashMap<>();
//         }
    }

    /**
     * CompactMap that stays in the compact array representation (ARRAY) for a
     *  size between 2 and {@code compactSize()}.
     */
    private static class ArrayMap extends CompactMap<String, Integer> {

        @Override
        protected int compactSize() {
            // small enough to stay in array state
            return 5;
        }

//         @Override
//         protected Map<String, Integer> getNewMap() {
//             return new HashMap<>();
//         }
    }

    /**
     * CompactMap that exceeds {@code compactSize()} and therefore switches to a
     *  backing {@link Map} (MAP).
     */
    private static class BackingMap extends CompactMap<String, Integer> {

        @Override
        protected int compactSize() {
            // force map transition after 2 entries
            return 2;
        }

//         @Override
//         protected Map<String, Integer> getNewMap() {
//             return new LinkedHashMap<>();
//         }
    }

    /**
     * Utility to set the protected {@code val} field via reflection.
     */
    private static void setInternalVal(CompactMap<?, ?> map, Object newVal) {
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(map, newVal);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("EMPTY – freshly instantiated map")
    void testEmptyLogicalValueType() {
        CompactMap<String, Integer> map = new EmptyMap();
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType(), "A freshly instantiated CompactMap should report EMPTY");
    }

    @Test
    @DisplayName("OBJECT – single entry with the configured single‑value key")
    void testObjectLogicalValueType() {
        CompactMap<String, Integer> map = new ObjectMap();
        map.put("id", 123);
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType(), "Single entry with the configured single‑value key should be OBJECT");
    }

    @Test
    @DisplayName("ENTRY – single entry with a non‑single‑value key")
    void testEntryLogicalValueType() {
        CompactMap<String, Integer> map = new EntryMap();
        map.put("otherKey", 456);
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType(), "Single entry with a non‑single‑value key should be ENTRY");
    }

    @Test
    @DisplayName("ARRAY – map with 2‑to‑compactSize entries")
    void testArrayLogicalValueType() {
        CompactMap<String, Integer> map = new ArrayMap();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType(), "Map with 2‑to‑compactSize entries should be ARRAY");
    }

    @Test
    @DisplayName("MAP – map larger than compactSize")
    void testMapLogicalValueType() {
        CompactMap<String, Integer> map = new BackingMap();
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType(), "Map larger than compactSize should be MAP");
    }

    @Test
    @DisplayName("EMPTY after removing the sole entry")
    void testEmptyAfterRemove() {
        CompactMap<String, Integer> map = new ObjectMap();
        map.put("id", 999);
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType(), "Pre‑condition: map should be OBJECT before removal");
        map.remove("id");
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType(), "Map becomes EMPTY after the only entry is removed");
    }

    @Test
    @DisplayName("EMPTY after clear()")
    void testEmptyAfterClear() {
        CompactMap<String, Integer> map = new BackingMap();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType(), "Pre‑condition: map should be MAP before clear");
        map.clear();
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType(), "clear() must reset the internal state to EMPTY");
    }
}