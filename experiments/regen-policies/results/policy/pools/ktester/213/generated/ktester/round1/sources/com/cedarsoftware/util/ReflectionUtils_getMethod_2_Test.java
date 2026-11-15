package com.cedarsoftware.util;

import java.lang.reflect.Method;

// @ExtendWith(MockitoExtension.class)
class ReflectionUtils_getMethod_2_Test {

    /* -----------------------------------------------------------
     *  Test fixtures
     * ----------------------------------------------------------- */
    static class Sample {

        public void noArgs() {
        }

        public void oneArg(String a) {
        }

        public void twoArgs(String a, Integer b) {
        }

        // overloaded methods – same name, different parameter count
        public void overload() {
        }

        public void overload(String a) {
        }

        public void overload(String a, String b) {
        }
    }

    /**
     * Simple class with a variety of methods used by several test cases.
     */
    static class SampleClass {

        // public no‑arg method
        public void publicNoArg() {
        }

        // overloaded methods
        public void overloaded() {
        }

        public void overloaded(Object a) {
        }

        public void overloaded(Object a, Object b) {
        }

        // method with a single parameter (used for the “argument count zero” negative test)
        public void methodWithParams(String a) {
        }

        // private method – should be made accessible
        @SuppressWarnings("unused")
        private void privateMethod() {
        }
    }

    /**
     * Superclass containing a method that will be inherited by {@link SubClass}.
     */
    static class SuperClass {

        public void inheritedMethod(Object param) {
        }
    }

    /**
     * Subclass that does not override {@code inheritedMethod}.
     */
    static class SubClass extends SuperClass {
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Install a fresh, thread‑safe cache before each test to avoid cross‑test contamination.
//         ReflectionUtils.setMethodCache(new ConcurrentHashMap<>());
    }

//     @AfterEach
    void teardownAfterEach() {
        // Reset the cache after each test as a safety net.
//         ReflectionUtils.setMethodCache(new ConcurrentHashMap<>());
    }

//     static Stream<Arguments> nullAndArgumentValidationProvider() {
//         return Stream.of(Arguments.of("instance is null", null, "anyMethod", 0, "Object instance cannot be null"), Arguments.of("methodName is null", new Object(), null, 0, "Method name cannot be null"), Arguments.of("argCount is negative", new Object(), "toString", -1, "Argument count cannot be negative"));
//     }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method the method to inspect; must not be {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        if (method == null) {
            throw new NullPointerException("method must not be null");
        }
        return method.getReturnType() == void.class;
    }

    /**
     * Finds a method with the given name and parameter types in the supplied
     * class or any of its super‑classes.  If the method cannot be found {@code null}
     * is returned.
     *
     * @param clazz         the class to search; must not be {@code null}
     * @param name          the method name; must not be {@code null}
     * @param parameterTypes the parameter types; may be empty but never {@code null}
     * @return the {@link Method} if found, otherwise {@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        if (clazz == null) {
            throw new NullPointerException("clazz must not be null");
        }
        if (name == null) {
            throw new NullPointerException("name must not be null");
        }
        if (parameterTypes == null) {
            throw new NullPointerException("parameterTypes must not be null");
        }
        // First try the public API (covers inherited public methods)
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            // fall back to a full search of declared methods in the hierarchy
        }
        // Walk the class hierarchy looking at declared methods
        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(name)) {
                    continue;
                }
                if (method.getParameterCount() != parameterTypes.length) {
                    continue;
                }
                Class<?>[] actual = method.getParameterTypes();
                boolean matches = true;
                for (int i = 0; i < actual.length; i++) {
                    if (!actual[i].equals(parameterTypes[i])) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return method;
                }
            }
        }
        return null;
    }

    /**
     * Determines whether a method with the supplied name exists in the given
     * class (or any of its super‑classes), regardless of its parameter list.
     *
     * @param clazz the class to inspect; must not be {@code null}
     * @param name  the method name; must not be {@code null}
     * @return {@code true} if at least one method with the given name is present
     */
    public static boolean isMethodPresent(Class<?> clazz, String name) {
        return findMethod(clazz, name) != null;
    }

//     @Test
//     void getMethod_returnsCorrectMethod_forExactArgumentCount() {
//         Sample instance = new Sample();
//         Method m0 = ReflectionUtils.getMethod(instance, "noArgs", 0);
//         assertNotNull(m0);
//         assertEquals("noArgs", m0.getName());
//         assertEquals(0, m0.getParameterCount());
//         Method m1 = ReflectionUtils.getMethod(instance, "oneArg", 1);
//         assertNotNull(m1);
//         assertEquals("oneArg", m1.getName());
//         assertEquals(1, m1.getParameterCount());
//         Method m2 = ReflectionUtils.getMethod(instance, "twoArgs", 2);
//         assertNotNull(m2);
//         assertEquals("twoArgs", m2.getName());
//         assertEquals(2, m2.getParameterCount());
//     }

//     @Test
//     void getMethod_selectsBestMatch_whenMultipleCandidatesExist() {
//         Sample instance = new Sample();
//         // There are three overloads named "overload". We ask for the version with 2 args.
//         Method m = ReflectionUtils.getMethod(instance, "overload", 2);
//         assertNotNull(m);
//         assertEquals("overload", m.getName());
//         assertEquals(2, m.getParameterCount());
//     }

//     @Test
//     void getMethod_throwsWhenMethodNotFound() {
//         Sample instance = new Sample();
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(instance, "nonExistent", 0));
//         assertTrue(ex.getMessage().contains("Method 'nonExistent'"));
//     }

//     @Test
//     void getMethod_throwsWhenArgumentCountIsNegative() {
//         Sample instance = new Sample();
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(instance, "noArgs", -1));
//         assertEquals("Argument count cannot be negative", ex.getMessage());
//     }

//     @Test
//     void getMethod_cachesResult_onSubsequentCalls() {
//         Sample instance = new Sample();
//         // First call – method is looked up and cached
//         Method first = ReflectionUtils.getMethod(instance, "noArgs", 0);
//         assertNotNull(first);
//         // Second call – should retrieve the same Method instance from the cache
//         Method second = ReflectionUtils.getMethod(instance, "noArgs", 0);
//         assertNotNull(second);
//         // The two Method objects must be the same (cached) instance
//         assertSame(first, second);
//     }

//     @Test
//     void getMethod_validatesNullParameters() {
//         Sample instance = new Sample();
//         assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethod(null, "noArgs", 0));
//         assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethod(instance, null, 0));
//     }

    /**
     * Verifies that {@code getMethod} validates its arguments and throws an
     * {@link IllegalArgumentException} with the expected message fragment.
     */
//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("nullAndArgumentValidationProvider")
//     void getMethod_validatesArguments(String description, Object instance, String methodName, int argCount, String expectedMessageFragment) {
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(instance, methodName, argCount));
//         assertTrue(ex.getMessage().contains(expectedMessageFragment), () -> "Expected message to contain [" + expectedMessageFragment + "] but was [" + ex.getMessage() + "]");
//     }

//     @Test
//     void getMethod_returnsPublicNoArgMethod() {
//         SampleClass instance = new SampleClass();
//         Method m = ReflectionUtils.getMethod(instance, "publicNoArg", 0);
//         assertNotNull(m, "Method should not be null");
//         assertEquals("publicNoArg", m.getName());
//         assertEquals(0, m.getParameterCount());
//         // The method should be accessible (public methods are already accessible)
//         assertTrue(m.canAccess(instance), "Public method should be accessible");
//     }

//     @Test
//     void getMethod_selectsOverloadedMethodWithTwoArgs() {
//         SampleClass instance = new SampleClass();
//         Method m = ReflectionUtils.getMethod(instance, "overloaded", 2);
//         assertNotNull(m);
//         assertEquals("overloaded", m.getName());
//         assertEquals(2, m.getParameterCount());
//         // Verify that the selected method's parameter types are both Object.class
//         Class<?>[] paramTypes = m.getParameterTypes();
//         assertEquals(Object.class, paramTypes[0]);
//         assertEquals(Object.class, paramTypes[1]);
//     }

//     @Test
//     void getMethod_findsMethodDeclaredInSuperclass() {
//         SubClass instance = new SubClass();
//         Method m = ReflectionUtils.getMethod(instance, "inheritedMethod", 1);
//         assertNotNull(m);
//         assertEquals("inheritedMethod", m.getName());
//         assertEquals(1, m.getParameterCount());
//         // Declaring class should be SuperClass
//         assertEquals(SuperClass.class, m.getDeclaringClass());
//     }

//     @Test
//     void getMethod_makesPrivateMethodAccessible() {
//         SampleClass instance = new SampleClass();
//         Method m = ReflectionUtils.getMethod(instance, "privateMethod", 0);
//         assertNotNull(m);
//         assertEquals("privateMethod", m.getName());
//         assertEquals(0, m.getParameterCount());
//         // The method must be accessible after the utility call
//         assertTrue(m.canAccess(instance), "Private method should have been made accessible by ReflectionUtils");
//     }

//     @Test
//     void getMethod_throwsWhenMethodDoesNotExist() {
//         SampleClass instance = new SampleClass();
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(instance, "nonExistent", 3));
//         assertTrue(ex.getMessage().contains("Method 'nonExistent' with 3 parameters not found"), () -> "Unexpected exception message: " + ex.getMessage());
//     }

//     @Test
//     void getMethod_returnsSameMethodInstanceOnSubsequentCalls() {
//         SampleClass instance = new SampleClass();
//         Method first = ReflectionUtils.getMethod(instance, "publicNoArg", 0);
//         assertNotNull(first);
//         Method second = ReflectionUtils.getMethod(instance, "publicNoArg", 0);
//         assertNotNull(second);
//         // The cache should cause the exact same Method object to be returned.
//         assertSame(first, second, "Expected cached Method instance to be returned on second call");
//     }

//     @Test
//     void getMethod_throwsWhenArgCountZeroButMethodHasParameters() {
//         SampleClass instance = new SampleClass();
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(instance, "methodWithParams", 0));
//         assertTrue(ex.getMessage().contains("Method 'methodWithParams' with 0 parameters not found"), () -> "Unexpected exception message: " + ex.getMessage());
//     }
}