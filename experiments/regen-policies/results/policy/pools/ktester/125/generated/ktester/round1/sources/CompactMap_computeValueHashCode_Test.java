package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.util.Optional;

// @ExtendWith(MockitoExtension.class)
class CompactMap_computeValueHashCode_Test {

    private TestMap map;

    /**
     * Simple subclass that makes the protected method accessible for testing.
     */
    private static class TestMap extends CompactMap<Object, Object> {

        /* --------------------------------------------------------------------- */
        /* Minimal overrides required for the abstract methods used by the    */
        /* base class.                                                          */
        /* --------------------------------------------------------------------- */
        @Override
        protected int compactSize() {
            return 3;
        }

        @Override
        protected java.util.Map<Object, Object> getNewMap() {
            return new java.util.LinkedHashMap<>();
        }

        /**
         * Public wrapper used by the test cases.
         */
        int computeValueHashCodePublic(Object value) {
            return computeValueHashCode(value);
        }
    }

    /**
     * Object whose {@code hashCode()} is overridden to return a constant.
     */
    private static class CustomHashObject {

        @Override
        public int hashCode() {
            return 42;
        }
    }

    /**
     * Object whose {@code hashCode()} deliberately throws a {@link RuntimeException}.
     */
    private static class ExceptionThrowingObject {

        @Override
        public int hashCode() {
            throw new RuntimeException("forced exception from hashCode");
        }
    }

//     @BeforeEach
    void setupBeforeEach() {
        map = new TestMap();
    }

//     @AfterEach
    void teardownAfterEach() {
        map = null;
    }

    /**
     * Supplies the arguments for {@link #computeValueHashCode_additional(Object, IntSupplier, boolean, String)}.
     */
//     static Stream<Arguments> additionalCases() {
//         // 1. Different CompactMap instance – should behave like any other object.
//         TestMap otherMap = new TestMap();
//         int otherMapHash = otherMap.hashCode();
//         // 2. Integer instance – Integer.hashCode() returns the int value.
//         Integer intObj = Integer.valueOf(123);
//         // 3. String instance.
//         String str = "testString";
//         // 4. Custom object with overridden hashCode().
//         CustomHashObject customObj = new CustomHashObject();
//         // 5. Object whose hashCode() throws RuntimeException.
//         ExceptionThrowingObject throwingObj = new ExceptionThrowingObject();
//         // 6. Primitive int array – uses identity hash code.
//         int[] intArray = new int[] { 1, 2, 3 };
//         return Stream.of(arguments(otherMap, (IntSupplier) () -> otherMapHash, false, "Different CompactMap instance should return its own hashCode, not the constant 17"), arguments(intObj, (IntSupplier) () -> intObj.hashCode(), false, "Integer value should return its int value as hash code"), arguments(str, (IntSupplier) () -> str.hashCode(), false, "String value should return its standard hash code"), arguments(customObj, (IntSupplier) () -> 42, false, "Custom object overrides hashCode() to return constant 42"), // value ignored because we expect an exception
//         arguments(throwingObj, (IntSupplier) () -> 0, true, "Object whose hashCode() throws should propagate RuntimeException"), arguments(intArray, (IntSupplier) () -> System.identityHashCode(intArray), false, "int[] array should return its identity hash code"));
//     }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == Void.TYPE;
    }

    /**
     * Finds a method with the given name and parameter types on the supplied class
     * (including its super‑classes). Returns an {@link Optional} containing the
     * {@link Method} if found, otherwise {@link Optional#empty()}.
     *
     * @param clazz          the class to search; never {@code null}
     * @param methodName     the name of the method; never {@code null}
     * @param parameterTypes the parameter types; may be empty but never {@code null}
     * @return an {@code Optional} with the matching method, or empty if none found
     */
    public static Optional<Method> findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return Optional.of(clazz.getMethod(methodName, parameterTypes));
        } catch (NoSuchMethodException e) {
            // Try declared methods (including private/protected) and walk the hierarchy
            Class<?> current = clazz;
            while (current != null) {
                try {
                    Method m = current.getDeclaredMethod(methodName, parameterTypes);
                    m.setAccessible(true);
                    return Optional.of(m);
                } catch (NoSuchMethodException ignored) {
                    // continue searching super‑class
                }
                current = current.getSuperclass();
            }
            return Optional.empty();
        }
    }

    /**
     * Checks whether a method with the given signature is present on the supplied
     * class (including its super‑classes). This mirrors the behaviour of the
     * original JUnit utility.
     *
     * @param clazz          the class to inspect; never {@code null}
     * @param methodName     the name of the method; never {@code null}
     * @param parameterTypes the parameter types; may be empty but never {@code null}
     * @return {@code true} if such a method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return findMethod(clazz, methodName, parameterTypes).isPresent();
    }

//     @Test
//     void computeValueHashCode_selfReference_returnsFixedValue() {
//         // The map should recognise its own instance and return the constant 17.
//         int result = map.computeValueHashCodePublic(map);
//         assertEquals(17, result, "When the value is the map itself, computeValueHashCode should return 17");
//     }

//     @Test
//     void computeValueHashCode_null_returnsZero() {
//         // Null values are defined to produce a hash code of 0.
//         int result = map.computeValueHashCodePublic(null);
//         assertEquals(0, result, "When the value is null, computeValueHashCode should return 0");
//     }

//     @Test
//     void computeValueHashCode_regularObject_returnsItsHashCode() {
//         // Verify delegation to the object's own hashCode().
//         Object obj = new Object() {
// 
//             @Override
//             public int hashCode() {
//                 return 123_456;
//             }
//         };
//         int result = map.computeValueHashCodePublic(obj);
//         assertEquals(123_456, result, "For a non‑null, non‑self value, computeValueHashCode should return the value's hashCode");
//     }

//     @Test
//     void computeValueHashCode_differentObjects_haveDistinctHashCodes() {
//         // Two distinct objects with different hashCodes should produce different results.
//         Object a = new Object() {
// 
//             @Override
//             public int hashCode() {
//                 return 111;
//             }
//         };
//         Object b = new Object() {
// 
//             @Override
//             public int hashCode() {
//                 return 222;
//             }
//         };
//         int hashA = map.computeValueHashCodePublic(a);
//         int hashB = map.computeValueHashCodePublic(b);
//         assertNotEquals(hashA, hashB, "Distinct objects must yield distinct hash codes from computeValueHashCode");
//     }

    /**
     * Parameterised test covering a broader set of inputs, including:
     * <ul>
     *   <li>Another {@link CompactMap} instance (different reference).</li>
     *   <li>Primitive wrapper {@link Integer}.</li>
     *   <li>{@link String} values.</li>
     *   <li>Custom object with overridden {@code hashCode()}.</li>
     *   <li>Object whose {@code hashCode()} throws.</li>
     *   <li>Primitive array (identity hash code).</li>
     * </ul>
     */
//     @ParameterizedTest(name = "{index}: {3}")
//     @MethodSource("additionalCases")
//     void computeValueHashCode_additional(Object value, IntSupplier expectedSupplier, boolean expectException, String description) {
//         if (expectException) {
//             // The method should propagate the RuntimeException from value.hashCode()
//             assertThrows(RuntimeException.class, () -> map.computeValueHashCodePublic(value), description);
//         } else {
//             int expected = expectedSupplier.getAsInt();
//             int actual = map.computeValueHashCodePublic(value);
//             assertEquals(expected, actual, description);
//         }
//     }

//     @Test
//     void computeValueHashCode_otherCompactMapInstance() {
//         TestMap otherMap = new TestMap();
//         // normal hashCode, not the special 17
//         int expected = otherMap.hashCode();
//         int actual = map.computeValueHashCodePublic(otherMap);
//         assertEquals(expected, actual, "Different CompactMap instance should return its own hashCode");
//     }

//     @Test
//     void computeValueHashCode_integerValue() {
//         Integer intObj = Integer.valueOf(123);
//         // returns the int value
//         int expected = intObj.hashCode();
//         int actual = map.computeValueHashCodePublic(intObj);
//         assertEquals(expected, actual, "Integer value should return its int value as hash code");
//     }

//     @Test
//     void computeValueHashCode_stringValue() {
//         String str = "testString";
//         int expected = str.hashCode();
//         int actual = map.computeValueHashCodePublic(str);
//         assertEquals(expected, actual, "String value should return its standard hash code");
//     }

//     @Test
//     void computeValueHashCode_customObject() {
//         CustomHashObject customObj = new CustomHashObject();
//         int expected = 42;
//         int actual = map.computeValueHashCodePublic(customObj);
//         assertEquals(expected, actual, "Custom object overrides hashCode() to return constant 42");
//     }

//     @Test
//     void computeValueHashCode_exceptionThrowingObject() {
//         ExceptionThrowingObject throwingObj = new ExceptionThrowingObject();
//         RuntimeException ex = assertThrows(RuntimeException.class, () -> map.computeValueHashCodePublic(throwingObj));
//         assertEquals("forced exception from hashCode", ex.getMessage());
//     }

//     @Test
//     void computeValueHashCode_intArrayIdentityHash() {
//         int[] intArray = new int[] { 1, 2, 3 };
//         int expected = System.identityHashCode(intArray);
//         int actual = map.computeValueHashCodePublic(intArray);
//         assertEquals(expected, actual, "int[] array should return its identity hash code");
//     }
}