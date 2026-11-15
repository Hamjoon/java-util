package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
// import org.junit.*;
// import org.junit.runner.RunWith;
// import org.junit.runners.JUnit4;

// @ExtendWith(MockitoExtension.class)
// @RunWith(JUnit4.class)
class CompactMap_size_Test {

    /* ---------------------------------------------------------------------- */
    private static final class Arguments extends ArrayList<Object> {

        private Arguments(Object... values) {
            super(Arrays.asList(values));
        }

        static Arguments of(Object... values) {
            return new Arguments(values);
        }
    }

    /**
     * Sets the protected {@code val} field of a {@link CompactMap} instance.
     *
     * @param map   the map whose internal state should be changed
     * @param value the value to assign to {@code val}
     */
    private static void setInternalVal(CompactMap<?, ?> map, Object value) {
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(map, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Provides various {@code Object[]} configurations and the expected size
     * according to the implementation (length >> 1).
     */
    private static Stream<Arguments> arrayProvider() {
        return Stream.of(Arguments.of(new Object[] { "k1", "v1", "k2", "v2" }, 2, "Object[] length 4 (two key/value pairs)"), Arguments.of(new Object[] { "lonely" }, 0, "Object[] length 1 (odd length, treated as 0 pairs)"), Arguments.of(new Object[] {}, 0, "Empty Object[] (length 0)"), Arguments.of(new Object[] { "onlyKey", "onlyValue" }, 1, "Object[] length 2 (one key/value pair)"), Arguments.of(new Object[] { "k1", "v1", "extra" }, 1, "Object[] length 3 (odd length >1)"));
    }

    private static Stream<Arguments> mapProvider() {
        return Stream.of(Arguments.of(Collections.emptyMap(), 0, "empty Map"), Arguments.of(Collections.singletonMap("k", "v"), 1, "Map with a single entry"), Arguments.of(Map.of("a", 1, "b", 2, "c", 3), 3, "Map with three entries"), Arguments.of(createMapWithSize(10), 10, "Map with ten entries"));
    }

    private static Map<Integer, Integer> createMapWithSize(int size) {
        Map<Integer, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            m.put(i, i);
        }
        return m;
    }

    private static Stream<Arguments> exceptionMapProvider() {
        return Stream.of(Arguments.of(UnsupportedOperationException.class, "Map.size() throws UnsupportedOperationException"), Arguments.of(IllegalStateException.class, "Map.size() throws IllegalStateException"), Arguments.of(RuntimeException.class, "Map.size() throws generic RuntimeException"));
    }

    /**
     * Verify size transitions from empty → single → compact array → backing map.
     */
//     @Test
//     void testSizeTransitionsThroughAllStates() {
//         CompactMap<String, Object> map = new CompactMap<>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
// 
//             @Override
//             protected int compactSize() {
//                 // transition after the 3rd entry
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         // empty
//         Assert.assertEquals(0, map.size());
//         Assert.assertTrue(map.isEmpty());
//         // first entry (single‑value state)
//         Assert.assertNull(map.put("value", 10.0d));
//         Assert.assertEquals(1, map.size());
//         Assert.assertFalse(map.isEmpty());
//         // second entry (compact array)
//         Assert.assertNull(map.put("alpha", "beta"));
//         Assert.assertEquals(2, map.size());
//         // third entry (still compact array – size == compactSize)
//         Assert.assertNull(map.put("gamma", 3));
//         Assert.assertEquals(3, map.size());
//         // fourth entry – forces transition to a backing map
//         Assert.assertNull(map.put("delta", true));
//         Assert.assertEquals(4, map.size());
//     }

    /**
     * Verify that a map which immediately uses a backing {@link Map}
     * (because {@code compactSize()} is set to 2) reports the correct size.
     */
//     @Test
//     void testSizeWhenBackingMapIsUsedFromTheStart() {
//         CompactMap<String, Integer> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 // any size <=2 forces immediate backing‑map usage
//                 return 2;
//             }
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         map.put("one", 1);
//         map.put("two", 2);
//         map.put("three", 3);
//         Assert.assertEquals(3, map.size());
//         Assert.assertFalse(map.isEmpty());
//     }

    /**
     * Verify the default constructor (builder‑less) works and reports size correctly.
     */
//     @Test
//     void testSizeWithDefaultConstructor() {
//         CompactMap<String, String> map = new CompactMap<>();
//         Assert.assertEquals(0, map.size());
//         Assert.assertTrue(map.isEmpty());
//         map.put("a", "A");
//         map.put("b", "B");
//         map.put("c", "C");
//         Assert.assertEquals(3, map.size());
//         Assert.assertFalse(map.isEmpty());
//     }

    /**
     * Verify that the builder creates a map whose {@code size()} behaves like a normal map.
     */
//     @Test
//     void testSizeUsingBuilder() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().compactSize(// small compact size to stay in array storage initially
//         5).insertionOrder().build();
//         // initially empty
//         Assert.assertEquals(0, map.size());
//         // add a few entries (still within compactSize)
//         map.put("x", 1);
//         map.put("y", 2);
//         Assert.assertEquals(2, map.size());
//         // exceed compactSize to trigger backing map transition
//         map.put("z", 3);
//         map.put("w", 4);
//         map.put("v", 5);
//         Assert.assertEquals(5, map.size());
//     }

//     @ParameterizedTest(name = "[{index}] {2} ⇒ expected size {1}")
//     @MethodSource("arrayProvider")
//     void testSizeWhenValIsObjectArray(Object[] array, int expectedSize, String description) {
//         arrayProvider().forEach(arg -> {
//             Object[] array = (Object[]) arg.get(0);
//             int expected = (int) arg.get(1);
//             String description = (String) arg.get(2);
//             CompactMap<String, Object> map = new CompactMap<>();
//             setInternalVal(map, array);
//             Assert.assertEquals("size() should return length>>1 for " + description, expected, map.size());
//         });
//     }

//     @ParameterizedTest(name = "[{index}] {2} ⇒ expected size {1}")
//     @MethodSource("mapProvider")
//     void testSizeWhenValIsMap(Map<?, ?> backingMap, int expectedSize, String description) {
//         mapProvider().forEach(arg -> {
//             Map<?, ?> backing = (Map<?, ?>) arg.get(0);
//             int expected = (int) arg.get(1);
//             String description = (String) arg.get(2);
//             CompactMap<String, Object> map = new CompactMap<>();
//             setInternalVal(map, backing);
//             Assert.assertEquals("size() should delegate to Map.size() for " + description, expected, map.size());
//         });
//     }

//     @Test
//     void testSizeWhenValIsEmptyMapConstant() {
//         CompactMap<String, Object> map = new CompactMap<>();
//         Field emptyMapField = CompactMap.class.getDeclaredField("EMPTY_MAP");
//         emptyMapField.setAccessible(true);
//         Object emptyMapConstant = emptyMapField.get(null);
//         setInternalVal(map, emptyMapConstant);
//         Assert.assertEquals(0, map.size());
//     }

//     @Test
//     void testSizeWhenValIsNonArrayNonMapObject() {
//         CompactMap<String, Object> map = new CompactMap<>();
//         setInternalVal(map, "just a plain String");
//         Assert.assertEquals(1, map.size());
//     }

//     @Test
//     void testSizeWhenValIsNull() {
//         CompactMap<String, Object> map = new CompactMap<>();
//         setInternalVal(map, null);
//         Assert.assertEquals(1, map.size());
//     }

//     @ParameterizedTest(name = "[{index}] {1}")
//     @MethodSource("exceptionMapProvider")
//     void testSizePropagatesExceptionFromBackingMap(Class<? extends RuntimeException> exClass, String description) {
//         exceptionMapProvider().forEach(arg -> {
//             @SuppressWarnings("unchecked")
//             Class<? extends RuntimeException> exClass = (Class<? extends RuntimeException>) arg.get(0);
//             String description = (String) arg.get(1);
//             // Simple Map implementation that throws the desired exception from size()
//             Map<Object, Object> throwingMap = new HashMap<Object, Object>() {
// 
//                 @Override
//                 public int size() {
//                     try {
//                         throw exClass.getDeclaredConstructor().newInstance();
//                     } catch (ReflectiveOperationException e) {
//                         throw new RuntimeException(e);
//                     }
//                 }
//             };
//             CompactMap<String, Object> map = new CompactMap<>();
//             setInternalVal(map, throwingMap);
//             try {
//                 map.size();
//                 Assert.fail("Expected exception of type " + exClass.getSimpleName() + " for: " + description);
//             } catch (RuntimeException e) {
//                 Assert.assertTrue(exClass.isInstance(e));
//             }
//         });
//     }

//     @Test
//     void sanityCheckDefaultBehavior() {
//         CompactMap<String, Integer> map = new CompactMap<>();
//         Assert.assertEquals(0, map.size());
//         map.put("one", 1);
//         Assert.assertEquals(1, map.size());
//     }
}