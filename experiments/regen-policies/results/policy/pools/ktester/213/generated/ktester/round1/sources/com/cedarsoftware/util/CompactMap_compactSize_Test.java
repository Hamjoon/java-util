package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
// import org.junit.Test;
// import static org.junit.Assert.*;

class CompactMap_compactSize_Test {

    /**
     * Simple subclass that only exposes the protected {@code compactSize()}
     * method.
     */
    private static class ExposedMap<K, V> extends CompactMap<K, V> {

        @Override
        public int size() {
            // delegate – not used in these tests
            return super.size();
        }

        /**
         * Makes the protected method visible to the test.
         */
        public int getCompactSize() {
            return compactSize();
        }

        /**
         * Default map implementation – not relevant for the compact‑size tests.
         */
        @Override
        protected Map<K, V> getNewMap() {
            return new HashMap<>();
        }
    }

    /**
     * Subclass that overrides {@code compactSize()} to a custom value.
     */
    private static class CustomSizeMap<K, V> extends CompactMap<K, V> {

        private final int customSize;

        CustomSizeMap(int customSize) {
            this.customSize = customSize;
        }

        @Override
        protected int compactSize() {
            return customSize;
        }

        @Override
        protected Map<K, V> getNewMap() {
            return new HashMap<>();
        }
    }

//     @Test
//     void defaultCompactSize_isDefinedConstant() {
//         ExposedMap<String, Integer> map = new ExposedMap<>();
//         // The default implementation returns the static constant DEFAULT_COMPACT_SIZE (50).
//         assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, map.getCompactSize(), "compactSize() should return the default constant when not overridden");
//     }

//     @Test
//     void overriddenCompactSize_returnsCustomValue() {
//         int custom = 7;
//         CustomSizeMap<String, Integer> map = new CustomSizeMap<>(custom);
//         assertEquals(custom, map.compactSize(), "compactSize() should return the overridden custom value");
//     }

//     @Test
//     void constructorRejectsCompactSizeLessThanTwo() {
//         // A subclass that reports a compact size of 1 should cause the constructor to throw.
//         try {
//             new CustomSizeMap<String, Integer>(1);
//             fail("CompactMap constructor must reject compactSize() < 2");
//         } catch (IllegalArgumentException ignored) {
//             // expected
//         }
//     }

//     @Test
//     void constructorAcceptsCompactSizeEqualOrGreaterThanTwo() {
//         // Size = 2 is the minimum allowed.
//         try {
//             new CustomSizeMap<String, Integer>(2);
//         } catch (Exception e) {
//             fail("CompactMap constructor should accept compactSize() == 2, but threw: " + e);
//         }
//     }

    /**
     * Verify that the protected {@code compactSize()} method can be invoked via reflection
     * on a plain {@link CompactMap} instance and still returns the default constant (50).
     */
//     @Test
//     void compactSize_reflectionReturnsDefault() throws Exception {
//         // Instantiate a concrete subclass (ExposedMap) – we only need a concrete instance.
//         CompactMap<String, Integer> map = new ExposedMap<>();
//         // Obtain the protected method via reflection.
//         Method method = CompactMap.class.getDeclaredMethod("compactSize");
//         // bypass Java access checks
//         method.setAccessible(true);
//         // Invoke the method on the map instance.
//         Object result = method.invoke(map);
//         assertTrue(result instanceof Integer);
//         assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, result, "Reflection invocation of compactSize() must return the default constant");
//     }

    /**
     * Verify that a {@link java.lang.invoke.MethodHandle} can be used to call the protected
     * {@code compactSize()} method and obtain the expected default value (50).
     */
//     @Test
//     void compactSize_methodHandleReturnsDefault() throws Throwable {
//         CompactMap<String, Integer> map = new ExposedMap<>();
//         // Build a MethodHandle for the virtual (instance) method "compactSize".
//         MethodHandles.Lookup lookup = MethodHandles.lookup();
//         MethodType methodType = MethodType.methodType(int.class);
//         MethodHandle mh = lookup.findVirtual(CompactMap.class, "compactSize", methodType);
//         // Invoke the handle; it returns an int primitive.
//         int compactSize = (int) mh.invoke(map);
//         assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, compactSize, "MethodHandle invocation of compactSize() must return the default constant");
//     }
}