package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;

// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;
// import static org.junit.Assert.*;
class CompactMap_containsKey_Test {

    /**
     * The map instance used for most tests – a compact size of 3 keeps the array state
     *  reachable without triggering the backing‑map transition.
     */
    private CompactMap<String, Integer> map;

    /**
     * A map with a very small {@code compactSize} to force a transition to a backing
     *  {@link java.util.HashMap} after a few puts.
     */
    private CompactMap<String, Integer> largeMap;

    //     @BeforeEach
    void setupBeforeEach() {
        // Map that stays in the compact‑array state for up to 3 entries.
        map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                // small enough to stay in array for 2 entries
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }
        };
        // Map that will switch to a backing map after 2 entries.
        largeMap = new CompactMap<>() {

            @Override
            protected int compactSize() {
                // trigger backing map on the 3rd entry
                return 2;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }
        };
    }

    //     @AfterEach
    void teardownAfterEach() {
        map.clear();
        largeMap.clear();
    }

    /**
     * Helper that forces the private {@code val} field to a given value.
     */
    private void setInternalVal(CompactMap<?, ?> target, Object newVal) throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        f.set(target, newVal);
    }

    /**
     * Initialise the two maps before each demo run.
     */
    private void setup() {
        map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                // small enough to stay in array for 2 entries
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }
        };
        largeMap = new CompactMap<>() {

            @Override
            protected int compactSize() {
                // trigger backing map on the 3rd entry
                return 2;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }
        };
    }

    /**
     * Run a handful of checks that mirror the original unit tests.
     */
    private void runChecks() throws Exception {
        // ---- empty map -------------------------------------------------
        assert !map.containsKey("anything");
        assert !map.containsKey(null);
        // ---- single entry ---------------------------------------------
        map.put("only", 42);
        assert map.containsKey("only");
        assert !map.containsKey("missing");
        map.put(null, 99);
        assert map.containsKey(null);
        assert !map.containsKey("another");
        // ---- compact‑array state ---------------------------------------
        map.clear();
        map.put("first", 1);
        map.put("second", 2);
        assert map.containsKey("first");
        assert map.containsKey("second");
        assert !map.containsKey("third");
        // ---- backing‑map state -----------------------------------------
        largeMap.clear();
        largeMap.put("a", 10);
        largeMap.put("b", 20);
        // forces transition to backing map
        largeMap.put("c", 30);
        assert largeMap.containsKey("a");
        assert largeMap.containsKey("b");
        assert largeMap.containsKey("c");
        assert !largeMap.containsKey("d");
        // ---- after removal ---------------------------------------------
        map.clear();
        map.put("x", 1);
        map.put("y", 2);
        assert map.containsKey("x");
        assert map.containsKey("y");
        map.remove("x");
        assert !map.containsKey("x");
        assert map.containsKey("y");
        // ---- direct manipulation of the internal Object[] -------------
        map.clear();
        Object[] entries = new Object[] { "a", 10, "missing", 20, null, 30, "valueOnlyKey", "keyOnly" };
        setInternalVal(map, entries);
        assert map.containsKey("a");
        assert !map.containsKey("unknown");
        assert map.containsKey(null);
        // stored as a value only
        assert !map.containsKey("keyOnly");
    }

    /**
     * Entry point for the demo.
     */
//     public static void main(String[] args) throws Exception {
//         CompactMapContainsKeyDemo demo = new CompactMapContainsKeyDemo();
//         demo.setup();
//         demo.runChecks();
//         System.out.println("All CompactMap.containsKey checks passed.");
//     }
}