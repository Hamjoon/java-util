package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.stream.Stream;
import java.util.LinkedHashMap;
import java.util.Map;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.junit.runners.Parameterized;
// import org.junit.runners.Parameterized.Parameters;
import java.util.Collection;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class CompactMap_getNewMap_Test {

    /**
     * Simple subclass that makes {@code getNewMap()} publicly accessible.
     */
    private static class ExposedMap<K, V> extends CompactMap<K, V> {

        /**
         * Calls the protected {@code getNewMap()} from the superclass.
         */
        public Map<K, V> invokeGetNewMap() {
            return super.getNewMap();
        }
    }

    /**
     * Subclass that overrides {@code getNewMap()} to return a {@link LinkedHashMap}.
     */
    private static class OverriddenMap<K, V> extends CompactMap<K, V> {

        @Override
        protected Map<K, V> getNewMap() {
            return new LinkedHashMap<>();
        }

        public Map<K, V> invokeGetNewMap() {
            return super.getNewMap();
        }
    }

    /* -------------------------------------------------------------
     *  Parameterised tests – default HashMap expectations
     * ------------------------------------------------------------- */
//     @RunWith(Parameterized.class)
    public static class DefaultHashMapExpectationsTest {

        private final String description;

        private final Runnable assertion;

        public DefaultHashMapExpectationsTest(String description, Runnable assertion) {
            this.description = description;
            this.assertion = assertion;
        }

//         @Parameters(name = "{index}: {0}")
//         public static Collection<Object[]> data() {
//             return Stream.of(new Object[] { "Returns a non‑null empty java.util.HashMap instance", (Runnable) () -> {
//                 ExposedMap<String, Integer> map = new ExposedMap<>();
//                 Map<String, Integer> m = map.invokeGetNewMap();
//                 assertNotNull(m);
//                 assertTrue(m.isEmpty());
//             } }, new Object[] { "Instance of java.util.HashMap (class name contains \"HashMap\")", (Runnable) () -> {
//                 ExposedMap<String, Integer> map = new ExposedMap<>();
//                 Map<String, Integer> m = map.invokeGetNewMap();
//                 assertTrue(m.getClass().getSimpleName().contains("HashMap"), "Class name should contain 'HashMap'");
//             } }).toList();
//         }

//         @Test
//         public void runAssertion() {
//             assertion.run();
//         }
    }

    /* -------------------------------------------------------------
     *  Parameterised tests – mutability & isolation expectations
     * ------------------------------------------------------------- */
//     @RunWith(Parameterized.class)
    public static class MutabilityAndIsolationExpectationsTest {

        private final String description;

        private final Runnable assertion;

        public MutabilityAndIsolationExpectationsTest(String description, Runnable assertion) {
            this.description = description;
            this.assertion = assertion;
        }

//         @Parameters(name = "{index}: {0}")
//         public static Collection<Object[]> data() {
//             return Stream.of(new Object[] { "The returned map is mutable – put(key, value) succeeds and size becomes 1", (Runnable) () -> {
//                 ExposedMap<String, String> map = new ExposedMap<>();
//                 Map<String, String> m = map.invokeGetNewMap();
//                 assertEquals(0, m.size());
//                 m.put("k", "v");
//                 assertEquals(1, m.size());
//                 assertEquals("v", m.get("k"));
//             } }, new Object[] { "Modifying the map returned by one call does not affect the map returned by another call", (Runnable) () -> {
//                 ExposedMap<String, Integer> map = new ExposedMap<>();
//                 Map<String, Integer> first = map.invokeGetNewMap();
//                 Map<String, Integer> second = map.invokeGetNewMap();
//                 first.put("a", 10);
//                 assertTrue(first.containsKey("a"));
//                 assertFalse(second.containsKey("a"));
//                 assertEquals(1, first.size());
//                 assertEquals(0, second.size());
//             } }).toList();
//         }

//         @Test
//         public void runAssertion() {
//             assertion.run();
//         }
    }

    /**
     * Supplies a stream of expectations that all revolve around the default
     * {@code getNewMap()} implementation returning a {@link HashMap}.
     */
//     private static Stream<Arguments> defaultHashMapExpectations() {
//         return Stream.of(Arguments.of("Returns a non‑null empty java.util.HashMap instance", (Runnable) () -> {
//             ExposedMap<String, Integer> map = new ExposedMap<>();
//             Map<String, Integer> m = map.invokeGetNewMap();
//             assertNotNull(m);
//             assertTrue(m.isEmpty());
//         }), Arguments.of("Instance of java.util.HashMap (class name contains \"HashMap\")", (Runnable) () -> {
//             ExposedMap<String, Integer> map = new ExposedMap<>();
//             Map<String, Integer> m = map.invokeGetNewMap();
//             assertTrue(m.getClass().getSimpleName().contains("HashMap"), "Class name should contain 'HashMap'");
//         }));
//     }

    /**
     * Supplies a stream of expectations that verify mutability and isolation of maps
     * created by successive {@code getNewMap()} calls.
     */
//     private static Stream<Arguments> mutabilityAndIsolationExpectations() {
//         return Stream.of(Arguments.of("The returned map is mutable – put(key, value) succeeds and size becomes 1", (Runnable) () -> {
//             ExposedMap<String, String> map = new ExposedMap<>();
//             Map<String, String> m = map.invokeGetNewMap();
//             assertEquals(0, m.size());
//             m.put("k", "v");
//             assertEquals(1, m.size());
//             assertEquals("v", m.get("k"));
//         }), Arguments.of("Modifying the map returned by one call does not affect the map returned by another call", (Runnable) () -> {
//             ExposedMap<String, Integer> map = new ExposedMap<>();
//             Map<String, Integer> first = map.invokeGetNewMap();
//             Map<String, Integer> second = map.invokeGetNewMap();
//             first.put("a", 10);
//             assertTrue(first.containsKey("a"));
//             assertFalse(second.containsKey("a"));
//             assertEquals(1, first.size());
//             assertEquals(0, second.size());
//         }));
//     }

//     @Test
//     void defaultGetNewMapReturnsHashMap() {
//         ExposedMap<String, Integer> map = new ExposedMap<>();
//         Map<String, Integer> newMap = map.invokeGetNewMap();
//         assertNotNull("getNewMap() should never return null", newMap);
//         assertTrue("Default implementation should return a HashMap", newMap instanceof HashMap);
//         assertTrue("Newly created map should be empty", newMap.isEmpty());
//         // mutability check
//         newMap.put("one", 1);
//         assertEquals("Map should contain the inserted entry", 1, (int) newMap.get("one"));
//         assertEquals("Size should be 1 after insertion", 1, newMap.size());
//     }

//     @Test
//     void overriddenGetNewMapReturnsLinkedHashMap() {
//         OverriddenMap<String, Integer> map = new OverriddenMap<>();
//         Map<String, Integer> newMap = map.invokeGetNewMap();
//         assertNotNull("Overridden getNewMap() should not return null", newMap);
//         assertTrue("Overridden implementation should return a LinkedHashMap", newMap instanceof LinkedHashMap);
//         assertTrue("Overridden map instance should be empty", newMap.isEmpty());
//         // mutability check
//         newMap.put("two", 2);
//         assertEquals("Map should contain the inserted entry", 2, (int) newMap.get("two"));
//         assertEquals("Size should be 1 after insertion", 1, newMap.size());
//     }

//     @Test
//     void getNewMapReturnsDistinctInstancesOnEachCall() {
//         ExposedMap<String, Integer> map = new ExposedMap<>();
//         Map<String, Integer> first = map.invokeGetNewMap();
//         Map<String, Integer> second = map.invokeGetNewMap();
//         assertNotSame("Each call must return a new map instance", first, second);
//         // mutate one map and verify the other is unaffected.
//         first.put("key", 1);
//         assertTrue("First map should contain the inserted key", first.containsKey("key"));
//         assertFalse("Second map must remain untouched", second.containsKey("key"));
//     }

//     @ParameterizedTest(name = "{index} ⇒ {0}")
//     @MethodSource("defaultHashMapExpectations")
//     void defaultImplementationAssertions(String description, Runnable assertion) {
//         // The description is only used for the test name; the actual assertion is executed here.
//         assertion.run();
//     }

//     @ParameterizedTest(name = "{index} ⇒ {0}")
//     @MethodSource("mutabilityAndIsolationExpectations")
//     void mutabilityAndIsolationAssertions(String description, Runnable assertion) {
//         assertion.run();
//     }
}