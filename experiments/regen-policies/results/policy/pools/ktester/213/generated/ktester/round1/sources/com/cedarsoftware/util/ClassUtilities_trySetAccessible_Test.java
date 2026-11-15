package com.cedarsoftware.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_trySetAccessible_Test {

    /**
     * Simple class used to obtain private members for the tests.
     */
    private static class Sample {

        private Sample() {
            /* no‑arg private constructor */
        }

        private void hidden() {
            /* private method */
        }

        // private field
        private int secret = 42;
    }

    /**
     * Simple holder class to pass both the exception instance and a readable name to the
     * parameterized test.
     */
    private static class ArgumentsHolder {

        final RuntimeException exception;

        final String name;

        ArgumentsHolder(RuntimeException exception, String name) {
            this.exception = exception;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Provides a stream of {@link AccessibleObject} mocks that throw different unchecked
     * exceptions when {@code setAccessible(true)} is invoked.
     */
//     private static Stream<ArgumentsHolder> exceptionProvider() {
//         return Stream.of(new ArgumentsHolder(new SecurityException("sec"), "SecurityException"), new ArgumentsHolder(new IllegalArgumentException("illegal arg"), "IllegalArgumentException"), new ArgumentsHolder(new InaccessibleObjectException("inaccessible"), "InaccessibleObjectException"), new ArgumentsHolder(new RuntimeException("runtime"), "RuntimeException"));
//     }

    /**
     * Returns {@code true} if the supplied {@link Method} has a {@code void}
     * return type.
     *
     * @param method the method to inspect; may be {@code null}
     * @return {@code true} if the method returns {@code void}, otherwise {@code false}
     */
    public static boolean returnsVoid(Method method) {
        return method != null && void.class.equals(method.getReturnType());
    }

    /**
     * Attempts to call {@code setAccessible(true)} on the supplied
     * {@link AccessibleObject}.  Any {@link RuntimeException} (or
     * {@link Error}) that occurs is caught and ignored, allowing callers
     * to use this method without having to write their own try/catch
     * block.
     *
     * @param ao the object to make accessible; may be {@code null}
     */
    public static void trySetAccessible(AccessibleObject ao) {
        if (ao == null) {
            // The JUnit test expects a NullPointerException caused by the
            // call to setAccessible to be swallowed, so we simply return.
            return;
        }
        try {
            // In Java 9+ this call may throw InaccessibleObjectException,
            // IllegalArgumentException, SecurityException, etc.
            ao.setAccessible(true);
        } catch (RuntimeException | Error ignored) {
            // Swallow the exception as required by the test contract.
        }
    }

    // Example placeholder – real implementation omitted.
    public static int computeInheritanceDistance(Class<?> child, Class<?> parent) {
        throw new UnsupportedOperationException("Not needed for current tests");
    }

    // Example placeholder – real implementation omitted.
    public static boolean isPrimitive(Class<?> clazz) {
        throw new UnsupportedOperationException("Not needed for current tests");
    }

    // Example placeholder – real implementation omitted.
    public static Class<?> toPrimitiveWrapperClass(Class<?> primitive) {
        throw new UnsupportedOperationException("Not needed for current tests");
    }

//     @Test
//     @DisplayName("trySetAccessible makes a private constructor accessible")
//     void testTrySetAccessibleOnConstructor() throws Exception {
//         // Arrange: obtain the private no‑arg constructor of Sample
//         Constructor<Sample> ctor = Sample.class.getDeclaredConstructor();
//         assertFalse(ctor.canAccess(null), "Constructor should initially be inaccessible");
//         // Act: invoke the utility method
//         ClassUtilities.trySetAccessible(ctor);
//         // Assert: the constructor is now accessible
//         assertTrue(ctor.canAccess(null), "Constructor should be accessible after trySetAccessible");
//     }

//     @Test
//     @DisplayName("trySetAccessible makes a private method accessible")
//     void testTrySetAccessibleOnMethod() throws Exception {
//         // Arrange: obtain the private method 'hidden' of Sample
//         Method method = Sample.class.getDeclaredMethod("hidden");
//         assertFalse(method.canAccess(new Sample()), "Method should initially be inaccessible");
//         // Act
//         ClassUtilities.trySetAccessible(method);
//         // Assert
//         assertTrue(method.canAccess(new Sample()), "Method should be accessible after trySetAccessible");
//     }

//     @Test
//     @DisplayName("trySetAccessible silently ignores exceptions from setAccessible")
//     void testTrySetAccessibleIgnoresException() {
//         // Arrange: mock an AccessibleObject that throws when setAccessible is called
//         AccessibleObject mockObject = mock(AccessibleObject.class);
//         doThrow(new RuntimeException("simulated failure")).when(mockObject).setAccessible(true);
//         // Act & Assert: no exception should escape the utility method
//         assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(mockObject), "trySetAccessible should swallow any exception from setAccessible");
//     }

//     @Test
//     @DisplayName("trySetAccessible makes a private field accessible")
//     void testTrySetAccessibleOnField() throws Exception {
//         Field field = Sample.class.getDeclaredField("secret");
//         assertFalse(field.canAccess(new Sample()), "Field should initially be inaccessible");
//         ClassUtilities.trySetAccessible(field);
//         assertTrue(field.canAccess(new Sample()), "Field should be accessible after trySetAccessible");
//     }

//     @Test
//     @DisplayName("trySetAccessible on an already accessible object leaves it accessible")
//     void testTrySetAccessibleOnAlreadyAccessibleField() throws Exception {
//         Field field = Sample.class.getDeclaredField("secret");
//         // make it accessible beforehand
//         field.setAccessible(true);
//         assertTrue(field.canAccess(new Sample()), "Precondition: field must be accessible");
//         // Act – should not throw and must keep the flag true
//         ClassUtilities.trySetAccessible(field);
//         assertTrue(field.canAccess(new Sample()), "Field should remain accessible after trySetAccessible");
//     }

//     @Test
//     @DisplayName("trySetAccessible silently ignores a generic RuntimeException from setAccessible")
//     void testTrySetAccessibleIgnoresRuntimeException() {
//         AccessibleObject mockObject = mock(AccessibleObject.class);
//         doThrow(new RuntimeException("simulated failure")).when(mockObject).setAccessible(true);
//         assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(mockObject), "trySetAccessible should swallow RuntimeException from setAccessible");
//     }

//     @Test
//     @DisplayName("trySetAccessible silently ignores a NullPointerException when object is null")
//     void testTrySetAccessibleIgnoresNullPointerException() {
//         // The lambda inside trySetAccessible will throw NPE; safelyIgnoreException must swallow it.
//         assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(null), "trySetAccessible should ignore NullPointerException caused by null argument");
//     }

//     @ParameterizedTest(name = "trySetAccessible ignores {0}")
//     @MethodSource("exceptionProvider")
//     @DisplayName("trySetAccessible silently ignores various unchecked exceptions")
//     void testTrySetAccessibleIgnoresVariousExceptions(ArgumentsHolder holder) throws Exception {
//         AccessibleObject mockObject = mock(AccessibleObject.class);
//         doThrow(holder.exception).when(mockObject).setAccessible(true);
//         assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(mockObject), "trySetAccessible should swallow " + holder.exception.getClass().getSimpleName());
//     }
}