package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// import org.junit.*;
// import static org.junit.Assert.*;
class CompactMap_getLogicalValueType_Test {

    private TestCompactMap<String, Object> map;

    /**
     * Simple concrete subclass used for testing.
     */
    private static class TestCompactMap<K, V> extends CompactMap<K, V> {

        /**
         * Expose the protected method for the test.
         */
        public LogicalValueType exposeLogicalValueType() {
            return getLogicalValueType();
        }

        /**
         * Allow the test to set the internal storage directly.
         */
        public void setVal(Object v) {
            this.val = v;
        }

        /* -----------------------------------------------------------------
         * The following configuration methods are overridden to provide a
         * deterministic environment for the tests. The concrete values are
         * not important as long as they are consistent and satisfy the
         * CompactMap constructor's requirements.
         * ----------------------------------------------------------------- */
        @Override
        protected int compactSize() {
            // any value >= 2 is acceptable
            return 3;
        }

        @Override
        protected Map<K, V> getNewMap() {
            // simple backing map
            return new LinkedHashMap<>();
        }

        @SuppressWarnings("unchecked")
        @Override
        protected K getSingleValueKey() {
            // The test does not rely on the actual key value; we simply
            // return a constant that can be cast to K.
            return (K) "singleKey";
        }
    }

    //     @BeforeEach
    void setupBeforeEach() {
        map = new TestCompactMap<>();
    }

    //     @AfterEach
    void teardownAfterEach() {
        map = null;
    }

    // -----------------------------------------------------------------
    void setup() {
        map = new TestCompactMap<>();
    }

    void teardown() {
        map = null;
    }

//     LogicalValueType emptyLogicalValueType() {
//         // By default the map is empty; no manipulation required.
//         return map.exposeLogicalValueType();
//     }

//     LogicalValueType objectLogicalValueType() {
//         map.setVal("just a value");
//         return map.exposeLogicalValueType();
//     }

//     LogicalValueType entryLogicalValueType() {
//         // CompactMapEntry is a protected inner class; we can instantiate it
//         // via the subclass instance.
//         CompactMap<String, Object>.CompactMapEntry entry = map.new CompactMapEntry("key", 123);
//         map.setVal(entry);
//         return map.exposeLogicalValueType();
//     }

//     LogicalValueType arrayLogicalValueType() {
//         Object[] array = new Object[] { "k1", "v1", "k2", "v2" };
//         map.setVal(array);
//         return map.exposeLogicalValueType();
//     }

//     LogicalValueType mapLogicalValueType() {
//         Map<String, Object> backing = new HashMap<>();
//         backing.put("a", 1);
//         backing.put("b", 2);
//         map.setVal(backing);
//         return map.exposeLogicalValueType();
//     }

    // -----------------------------------------------------------------
//     public static void main(String[] args) {
//         CompactMapLogicalValueTypeDemo demo = new CompactMapLogicalValueTypeDemo();
//         demo.setup();
//         System.out.println("EMPTY  : " + demo.emptyLogicalValueType());
//         System.out.println("OBJECT : " + demo.objectLogicalValueType());
//         // reset
//         demo.setup();
//         System.out.println("ENTRY  : " + demo.entryLogicalValueType());
//         // reset
//         demo.setup();
//         System.out.println("ARRAY  : " + demo.arrayLogicalValueType());
//         // reset
//         demo.setup();
//         System.out.println("MAP    : " + demo.mapLogicalValueType());
//         demo.teardown();
//     }
}