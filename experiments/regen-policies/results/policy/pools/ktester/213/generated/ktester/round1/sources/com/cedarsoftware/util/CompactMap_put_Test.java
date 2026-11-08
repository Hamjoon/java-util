package com.cedarsoftware.util;

import java.lang.reflect.Field;

// import org.junit.*;
// import static org.junit.Assert.*;
class CompactMap_put_Test {

    /**
     * A map with a very small {@code compactSize} so state transitions happen quickly.
     */
    private CompactMap<String, Object> map;

    //     @BeforeEach
//     void setupBeforeEach() {
//         map = new CompactMap<String, Object>() {
// 
//             // use a tiny compact size to force a transition to a backing map after a few puts
//             @Override
//             protected int compactSize() {
//                 // 0‑2 entries → compact array, >2 → backing map
//                 return 2;
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 // the special single‑value key
//                 return "id";
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 // use a LinkedHashMap so ordering is predictable in assertions
//                 return new LinkedHashMap<>();
//             }
//         };
//     }

    //     @AfterEach
    void teardownAfterEach() {
        map = null;
    }

    /* --------------------------------------------------------------
     *  Helper methods for reflection access to the private `val` field.
     * -------------------------------------------------------------- */
    private static void setPrivateVal(CompactMap<?, ?> map, Object value) throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        f.set(map, value);
    }

    private static Object getPrivateVal(CompactMap<?, ?> map) throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        return f.get(map);
    }

    /**
     * Simple manual demonstration that the map works as expected.
     * This can be executed without any testing framework.
     */
//     public static void main(String[] args) throws Exception {
//         CompactMapPutDemo demo = new CompactMapPutDemo();
//         demo.setupBeforeEach();
//         // Direct single‑value storage using the special key
//         demo.map.put("id", "single");
//         System.out.println("After putting ('id','single'): " + demo.map);
//         // Adding a second entry forces a compact‑array transition
//         demo.map.put("other", "value");
//         System.out.println("After putting ('other','value'): " + demo.map);
//         // Adding a third entry exceeds compactSize and switches to a backing map
//         demo.map.put("third", "v3");
//         System.out.println("After putting ('third','v3'): " + demo.map);
//         // Demonstrate reflection helpers
//         Object internal = getPrivateVal(demo.map);
//         System.out.println("Internal representation class: " + internal.getClass().getName());
//         demo.teardownAfterEach();
//     }
}