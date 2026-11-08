package com.cedarsoftware.util;

import java.lang.reflect.Method;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_trySetAccessible_Test {

    /**
     * Helper class containing a private method used for the accessibility test.
     */
    private static class Helper {

        @SuppressWarnings("unused")
        private void secret() {
            // No‑op; the method exists solely for reflection testing.
        }
    }

    /**
     * Simple class with various members used for reflection.
     */
    private static class TestClass {

        private static int privateStaticField = 1;

        public static int publicStaticField = 2;

        private TestClass() {
            /* private constructor */
        }

        @SuppressWarnings("unused")
        private static void privateStaticMethod() {
            /* no‑op */
        }
    }

    /**
     * AccessibleObject that throws a generic RuntimeException when setAccessible is called.
     */
    private static class ThrowingAccessibleObject extends AccessibleObject {

        @Override
        public void setAccessible(boolean flag) {
            throw new RuntimeException("simulated failure");
        }
    }

    /**
     * AccessibleObject that throws IllegalArgumentException when setAccessible is called.
     */
    private static class IllegalArgumentThrowingAccessibleObject extends AccessibleObject {

        @Override
        public void setAccessible(boolean flag) {
            throw new IllegalArgumentException("illegal argument");
        }
    }

    /**
     * AccessibleObject that throws AssertionError when setAccessible is called.
     */
    private static class ErrorThrowingAccessibleObject extends AccessibleObject {

        @Override
        public void setAccessible(boolean flag) {
            throw new AssertionError("assertion error");
        }
    }

    /**
     * AccessibleObject that throws InaccessibleObjectException (Java 9+).
     */
    private static class InaccessibleObjectThrowingAccessibleObject extends AccessibleObject {

        @Override
        public void setAccessible(boolean flag) {
            throw new InaccessibleObjectException("inaccessible object");
        }
    }

    /**
     * Supplies various AccessibleObject instances that should become accessible.
     */
    private static Stream<Arguments> accessibleObjectsProvider() throws Exception {
        Field privateField = TestClass.class.getDeclaredField("privateStaticField");
        Method privateMethod = TestClass.class.getDeclaredMethod("privateStaticMethod");
        Constructor<TestClass> privateCtor = TestClass.class.getDeclaredConstructor();
        Field publicField = TestClass.class.getDeclaredField("publicStaticField");
        return Stream.of(Arguments.of("private static field", privateField), Arguments.of("private static method", privateMethod), Arguments.of("private constructor", privateCtor), Arguments.of("public static field (already accessible)", publicField));
    }

    /**
     * Helper that checks the accessibility flag of an AccessibleObject.
     */
    private static boolean isNowAccessible(AccessibleObject ao) {
        // AccessibleObject does not expose a direct "isAccessible" method; use reflection to call the
        // package‑private "override" field if present, otherwise fall back to canAccess where applicable.
        if (ao instanceof Field) {
            return ((Field) ao).canAccess(null);
        } else if (ao instanceof Method) {
            return ((Method) ao).canAccess(null);
        } else if (ao instanceof Constructor) {
            return ((Constructor<?>) ao).canAccess(null);
        }
        // For any other AccessibleObject we simply return true because setAccessible succeeded without exception.
        return true;
    }

    /**
     * Attempts to make the supplied {@link AccessibleObject} accessible.
     * <p>
     * The method invokes {@code setAccessible(true)} and deliberately
     * swallows any {@link RuntimeException}, {@link IllegalArgumentException},
     * {@link InaccessibleObjectException} (or any other {@link Throwable})
     * that may be thrown.  Errors (subclasses of {@link Error}) are
     * re‑thrown, because they indicate serious problems that the caller
     * should see (e.g., {@link AssertionError} in the test suite).
     * </p>
     *
     * @param ao the {@code AccessibleObject} to make accessible; may be {@code null}
     */
    public static void trySetAccessible(AccessibleObject ao) {
        if (ao == null) {
            // null arguments are ignored per the tests
            return;
        }
        try {
            ao.setAccessible(true);
        } catch (Error e) {
            // Errors must propagate (the test expects AssertionError to be thrown)
            throw e;
        } catch (Throwable ignored) {
            // All other throwables (RuntimeException, IllegalArgumentException,
            // InaccessibleObjectException, etc.) are deliberately ignored.
        }
    }

    /**
     * Returns {@code true} if the supplied method has a return type of {@code void}.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method returns {@code void}, otherwise {@code false}
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

    @Test
    void trySetAccessible_makesMethodAccessible() throws Exception {
        Method privateMethod = Helper.class.getDeclaredMethod("secret");
        assertFalse(privateMethod.canAccess(null), "Method should initially be inaccessible");
        ClassUtilities.trySetAccessible(privateMethod);
        assertTrue(privateMethod.canAccess(null), "Method should be accessible after trySetAccessible");
    }

    @Test
    void trySetAccessible_ignoresExceptionFromSetAccessible() {
        AccessibleObject mockObject = mock(AccessibleObject.class);
        doThrow(new RuntimeException("simulated failure")).when(mockObject).setAccessible(true);
        assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(mockObject), "trySetAccessible should swallow exceptions from setAccessible");
    }

    @Test
    void trySetAccessible_privateStaticField_becomesAccessible() throws Exception {
        Field field = TestClass.class.getDeclaredField("privateStaticField");
        assertFalse(field.canAccess(null), "Field should initially be inaccessible");
        ClassUtilities.trySetAccessible(field);
        assertTrue(field.canAccess(null), "Field should be accessible after trySetAccessible");
    }

    @Test
    void trySetAccessible_publicStaticField_alreadyAccessible() throws Exception {
        Field field = TestClass.class.getDeclaredField("publicStaticField");
        // Public fields are accessible by default.
        assertTrue(field.canAccess(null), "Public field should already be accessible");
        // The call must not change the state nor throw.
        ClassUtilities.trySetAccessible(field);
        assertTrue(field.canAccess(null), "Public field should remain accessible");
    }

    @Test
    void trySetAccessible_privateStaticMethod_becomesAccessible() throws Exception {
        Method method = TestClass.class.getDeclaredMethod("privateStaticMethod");
        assertFalse(method.canAccess(null), "Method should initially be inaccessible");
        ClassUtilities.trySetAccessible(method);
        assertTrue(method.canAccess(null), "Method should be accessible after trySetAccessible");
    }

    @Test
    void trySetAccessible_privateConstructor_becomesAccessible() throws Exception {
        Constructor<TestClass> ctor = TestClass.class.getDeclaredConstructor();
        assertFalse(ctor.canAccess(null), "Constructor should initially be inaccessible");
        ClassUtilities.trySetAccessible(ctor);
        assertTrue(ctor.canAccess(null), "Constructor should be accessible after trySetAccessible");
    }

    @Test
    void trySetAccessible_ignoresRuntimeException() {
        AccessibleObject mockObject = mock(AccessibleObject.class);
        doThrow(new RuntimeException("simulated failure")).when(mockObject).setAccessible(true);
        assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(mockObject), "trySetAccessible should swallow RuntimeException from setAccessible");
    }

    @Test
    void trySetAccessible_customThrowingAccessibleObject_ignoresException() {
        AccessibleObject obj = new ThrowingAccessibleObject();
        assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(obj), "Exception from custom AccessibleObject should be ignored");
    }

    @Test
    void trySetAccessible_ignoresIllegalArgumentException() {
        AccessibleObject obj = new IllegalArgumentThrowingAccessibleObject();
        assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(obj), "IllegalArgumentException should be ignored");
    }

    @Test
    void trySetAccessible_ignoresInaccessibleObjectException() {
        AccessibleObject obj = new InaccessibleObjectThrowingAccessibleObject();
        assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(obj), "InaccessibleObjectException should be ignored");
    }

    @Test
    void trySetAccessible_nullArgument_isIgnored() {
        assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(null), "Null argument should be ignored without throwing");
    }

    @Test
    void trySetAccessible_propagatesError() {
        AccessibleObject obj = new ErrorThrowingAccessibleObject();
        assertThrows(AssertionError.class, () -> ClassUtilities.trySetAccessible(obj), "AssertionError should propagate and not be swallowed");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("accessibleObjectsProvider")
    void trySetAccessible_positiveScenarios(String description, AccessibleObject ao) throws Exception {
        // Pre‑condition: the object should not already be accessible (except for public field case).
        // The test does not assert the pre‑condition for public fields because they are already true.
        ClassUtilities.trySetAccessible(ao);
        // After the call, the object must be accessible.
        assertTrue(isNowAccessible(ao), "AccessibleObject should be accessible after trySetAccessible");
    }
}
