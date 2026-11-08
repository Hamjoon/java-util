package com.cedarsoftware.util;

import java.lang.reflect.Field;

// import org.junit.*;
// import static org.junit.Assert.*;
// @ExtendWith(MockitoExtension.class)
class CompactMap_remove_Test {

    /**
     * Map instance used by the tests – created fresh for each test method.
     */
    private CompactMap<String, Object> map;

    //     @BeforeEach
    void setupBeforeEach() {
        // Default compact size gives us room to test both compact‑array and backing‑map states.
        map = createMap(4);
    }

    //     @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental cross‑test contamination.
        map.clear();
        map = null;
    }

    /**
     * Helper that creates a {@link CompactMap} with a configurable {@code compactSize}.
     * The map uses {@link LinkedHashMap} as the backing map and defines a custom
     * {@code singleValueKey} (required for the single‑entry optimisation).
     *
     * @param compactSize the size at which the map switches from a compact array to a backing map
     * @return a new {@link CompactMap} instance ready for use
     */
//     private CompactMap<String, Object> createMap(final int compactSize) {
//         return new CompactMap<String, Object>() {
// 
//             @Override
//             protected int compactSize() {
//                 return compactSize;
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 // LinkedHashMap gives deterministic iteration order
//                 return new LinkedHashMap<>();
//             }
//         };
//     }

    /**
     * Sets the private {@code val} field of a {@link CompactMap} via reflection.
     */
    private static void setInternalVal(CompactMap<?, ?> map, Object newVal) {
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(map, newVal);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs a handful of removal scenarios and prints the results.
     */
//     public static void main(String[] args) {
//         CompactMapRemoveDemo demo = new CompactMapRemoveDemo();
//         // -----------------------------------------------------------------
//         // 1. Remove from an empty map
//         demo.map = demo.createMap(4);
//         System.out.println("Empty map size before: " + demo.map.size());
//         System.out.println("remove(null) => " + demo.map.remove("nonexistent"));
//         System.out.println("Empty map size after: " + demo.map.size());
//         System.out.println();
//         // -----------------------------------------------------------------
//         // 2. Single‑entry optimisation removal
//         demo.map = demo.createMap(4);
//         demo.map.put("value", 42);
//         System.out.println("Single entry size before: " + demo.map.size());
//         System.out.println("remove('value') => " + demo.map.remove("value"));
//         System.out.println("Map empty after removal? " + demo.map.isEmpty());
//         System.out.println();
//         // -----------------------------------------------------------------
//         // 3. Removal from compact array (size between 2 and compactSize)
//         demo.map = demo.createMap(4);
//         demo.map.put("a", 1);
//         demo.map.put("b", 2);
//         demo.map.put("c", 3);
//         System.out.println("Compact array size before: " + demo.map.size());
//         System.out.println("remove('b') => " + demo.map.remove("b"));
//         System.out.println("Size after removal: " + demo.map.size());
//         System.out.println("get('a') = " + demo.map.get("a"));
//         System.out.println("get('c') = " + demo.map.get("c"));
//         System.out.println();
//         // -----------------------------------------------------------------
//         // 4. Removal when map has transitioned to a backing map
//         // force early transition
//         demo.map = demo.createMap(2);
//         demo.map.put("k1", "v1");
//         demo.map.put("k2", "v2");
//         demo.map.put("k3", "v3");
//         demo.map.put("k4", "v4");
//         demo.map.put("k5", "v5");
//         System.out.println("Backing map size before: " + demo.map.size());
//         System.out.println("remove('k3') => " + demo.map.remove("k3"));
//         System.out.println("Size after removal: " + demo.map.size());
//         System.out.println("Remaining keys: " + demo.map.keySet());
//         System.out.println();
//         // -----------------------------------------------------------------
//         // 5. Removal of a null key (behaves like any absent key)
//         demo.map = demo.createMap(4);
//         demo.map.put("x", 100);
//         System.out.println("remove(null) on map with entries => " + demo.map.remove(null));
//         System.out.println("Size unchanged? " + demo.map.size());
//         System.out.println();
//         // -----------------------------------------------------------------
//         // 6. Direct manipulation of internal representation (compact array)
//         demo.map = demo.createMap(4);
//         Object[] internalArray = new Object[] { "alpha", "value1", "beta", "value2" };
//         setInternalVal(demo.map, internalArray);
//         System.out.println("Manually set compact array, size: " + demo.map.size());
//         System.out.println("remove('beta') => " + demo.map.remove("beta"));
//         System.out.println("Size after removal: " + demo.map.size());
//         System.out.println("Remaining entry: " + demo.map.get("alpha"));
//         System.out.println();
//         // -----------------------------------------------------------------
//         // 7. Direct manipulation of internal representation (backing map)
//         demo.map = demo.createMap(4);
//         Map<String, String> backing = new HashMap<>();
//         backing.put("onlyKey", "onlyValue");
//         setInternalVal(demo.map, backing);
//         System.out.println("Manually set backing map, size: " + demo.map.size());
//         System.out.println("remove('onlyKey') => " + demo.map.remove("onlyKey"));
//         System.out.println("Map empty? " + demo.map.isEmpty());
//         System.out.println();
//         // -----------------------------------------------------------------
//         // 8. Removal from a TreeMap backing (sorted order)
//         demo.map = demo.createMap(4);
//         TreeMap<String, String> tree = new TreeMap<>();
//         tree.put("a", "A");
//         tree.put("b", "B");
//         setInternalVal(demo.map, tree);
//         System.out.println("TreeMap backing size: " + demo.map.size());
//         System.out.println("remove('b') => " + demo.map.remove("b"));
//         System.out.println("Size after removal: " + demo.map.size());
//         Iterator<String> it = demo.map.keySet().iterator();
//         System.out.println("Remaining key: " + (it.hasNext() ? it.next() : "none"));
//         System.out.println();
//         // -----------------------------------------------------------------
//         // 9. Removal from a LinkedHashMap backing (preserves insertion order)
//         demo.map = demo.createMap(4);
//         LinkedHashMap<String, String> linked = new LinkedHashMap<>();
//         linked.put("first", "valueFirst");
//         linked.put("second", "valueSecond");
//         setInternalVal(demo.map, linked);
//         System.out.println("LinkedHashMap backing size: " + demo.map.size());
//         System.out.println("remove('first') => " + demo.map.remove("first"));
//         System.out.println("Size after removal: " + demo.map.size());
//         it = demo.map.keySet().iterator();
//         System.out.println("Remaining key (insertion order): " + (it.hasNext() ? it.next() : "none"));
//     }
}