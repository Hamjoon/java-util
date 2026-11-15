package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class CompactMap_getLogicalValueType_Test {

    /**
     * Minimal concrete subclass of {@link CompactMap} that supplies the
     * abstract configuration methods required for the tests.
     */
    private static class TestCompactMap<K, V> extends CompactMap<K, V> {

        TestCompactMap() {
            // invoke the protected constructor
            super();
        }

        /**
         * Expose the protected {@code getLogicalValueType()} for testing.
         */
        CompactMap.LogicalValueType exposeLogicalValueType() {
            return getLogicalValueType();
        }

        @Override
        protected int compactSize() {
            // any value >= 2 is fine for the tests
            return 3;
        }

        @Override
        protected Map<K, V> getNewMap() {
            return new HashMap<>();
        }

        /**
         * The base class expects the single‑value key to be of type {@code K}.
         * For the purpose of these tests the actual value is irrelevant, so we
         * simply return {@code null}.
         */
        @Override
        protected K getSingleValueKey() {
            return null;
        }
    }

    /**
     * Simple {@link InvocationHandler} that forwards every method call to a
     * backing {@link Map} instance. Used to create a dynamic proxy that
     * pretends to be a {@code Map}.
     */
    private static class ForwardingHandler implements InvocationHandler {

        private final Map<?, ?> target;

        ForwardingHandler(Map<?, ?> target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(target, args);
        }
    }

    /**
     * Simple holder for a single parameterised test case.
     */
    private static final class TestCase {

        final String description;

        final Object internalVal;

        final CompactMap.LogicalValueType expected;

        TestCase(String description, Object internalVal, CompactMap.LogicalValueType expected) {
            this.description = description;
            this.internalVal = internalVal;
            this.expected = expected;
        }
    }

    /**
     * Supplies the test data for {@link #testLogicalValueTypeParameterized}.
     *
     * <p>The data covers:
     * <ul>
     *   <li>Object[] (simple array)</li>
     *   <li>Multi‑dimensional Object[] (still an Object[])</li>
     *   <li>Map implementation (HashMap)</li>
     *   <li>Dynamic proxy that implements Map</li>
     *   <li>Plain object (String)</li>
     *   <li>Null value (falls through to OBJECT)</li>
     *   <li>Primitive int[] (does NOT match Object[])</li>
     * </ul>
     * </p>
     */
//     private static Stream<Arguments> provideLogicalValueTypeCases() {
//         // 1. Simple Object[] → ARRAY
//         Object[] simpleArray = new Object[] { "a", 1, "b", 2 };
//         // 2. Multi‑dimensional Object[] → ARRAY
//         String[][] multiArray = new String[][] { { "x", "y" }, { "z" } };
//         // 3. Regular Map → MAP
//         Map<String, Integer> hashMap = new HashMap<>();
//         hashMap.put("one", 1);
//         hashMap.put("two", 2);
//         // 4. Dynamic proxy implementing Map → MAP
//         Map<String, Integer> realMap = new HashMap<>();
//         realMap.put("proxyKey", 99);
//         @SuppressWarnings("unchecked")
//         Map<String, Integer> proxyMap = (Map<String, Integer>) Proxy.newProxyInstance(CompactMap_getLogicalValueType_Test.class.getClassLoader(), new Class[] { Map.class }, new ForwardingHandler(realMap));
//         // 5. Plain object (String) → OBJECT
//         String plainObject = "just a plain string";
//         // 6. Null value → OBJECT (fallback case)
//         Object nullObject = null;
//         // 7. Primitive int[] → OBJECT (does NOT match Object[])
//         int[] primitiveArray = new int[] { 1, 2, 3 };
//         return Stream.of(new TestCase("val is a simple Object[]", simpleArray, CompactMap.LogicalValueType.ARRAY), new TestCase("val is a multi‑dimensional Object[]", multiArray, CompactMap.LogicalValueType.ARRAY), new TestCase("val is a regular Map implementation", hashMap, CompactMap.LogicalValueType.MAP), new TestCase("val is a dynamic proxy implementing Map", proxyMap, CompactMap.LogicalValueType.MAP), new TestCase("val is a plain non‑array, non‑map object", plainObject, CompactMap.LogicalValueType.OBJECT), new TestCase("val is null (no matching condition)", nullObject, CompactMap.LogicalValueType.OBJECT), new TestCase("val is a primitive int[] (not Object[])", primitiveArray, CompactMap.LogicalValueType.OBJECT));
//     }

    /**
     * Entry point that runs all tests.  Use {@code java -ea} to enable the
     * assertions.
     */
//     public static void main(String[] args) {
//         testLogicalValueTypeEmpty();
//         testLogicalValueTypeObject();
//         testLogicalValueTypeEntry();
//         testLogicalValueTypeArray();
//         testLogicalValueTypeMap();
//         testLogicalValueTypeParameterized();
//         System.out.println("All CompactMap logical‑value‑type tests passed.");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testLogicalValueTypeEmpty() {
//         TestCompactMap<String, Integer> map = new TestCompactMap<>();
//         // By default the protected field 'val' is EMPTY_MAP, no manipulation needed
//         assert map.exposeLogicalValueType() == CompactMap.LogicalValueType.EMPTY : "LogicalValueType should be EMPTY when map is newly created";
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testLogicalValueTypeObject() {
//         TestCompactMap<String, String> map = new TestCompactMap<>();
//         map.val = "just a plain object";
//         assert map.exposeLogicalValueType() == CompactMap.LogicalValueType.OBJECT : "LogicalValueType should be OBJECT when val holds a plain object";
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testLogicalValueTypeEntry() {
//         TestCompactMap<String, Integer> map = new TestCompactMap<>();
//         CompactMap<String, Integer>.CompactMapEntry entry = map.new CompactMapEntry("key", 42);
//         map.val = entry;
//         assert map.exposeLogicalValueType() == CompactMap.LogicalValueType.ENTRY : "LogicalValueType should be ENTRY when val is a CompactMapEntry";
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testLogicalValueTypeArray() {
//         TestCompactMap<String, Integer> map = new TestCompactMap<>();
//         map.val = new Object[] { "a", 1, "b", 2 };
//         assert map.exposeLogicalValueType() == CompactMap.LogicalValueType.ARRAY : "LogicalValueType should be ARRAY when val is an Object[]";
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void testLogicalValueTypeMap() {
//         TestCompactMap<String, Integer> map = new TestCompactMap<>();
//         Map<String, Integer> backing = new HashMap<>();
//         backing.put("x", 99);
//         backing.put("y", 100);
//         map.val = backing;
//         assert map.exposeLogicalValueType() == CompactMap.LogicalValueType.MAP : "LogicalValueType should be MAP when val is a Map";
//     }

    /* --------------------------------------------------------------------- */
//     @ParameterizedTest(name = "{index}: {0}")
//     @MethodSource("provideLogicalValueTypeCases")
//     void testLogicalValueTypeParameterized(String description, Object internalVal, CompactMap.LogicalValueType expected) {
//         provideLogicalValueTypeCases().forEach(tc -> {
//             TestCompactMap<String, Object> map = new TestCompactMap<>();
//             map.val = tc.internalVal;
//             assert map.exposeLogicalValueType() == tc.expected : "LogicalValueType should be " + tc.expected + " when " + tc.description;
//         });
//     }
}