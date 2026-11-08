package com.cedarsoftware.util;

import java.lang.reflect.AccessibleObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_trySetAccessible_Test {

    @Mock
    private AccessibleObject mockAccessibleObject;

    class Sample {

        private String hidden = "secret";
    }

    @Nested
    @DisplayName("Method accessibility tests")
    class MethodAccessibility {

        @Test
        @DisplayName("trySetAccessible makes a private Method accessible without throwing")
        void testMethodBecomesAccessible() throws Exception {
            // Arrange – obtain a private method from ClassUtilities
            Method method = ClassUtilities.class.getDeclaredMethod("addPermanentClassAlias", Class.class, String.class);
            // Ensure it is not accessible initially (Java 9+ deprecates isAccessible, but it still works)
            assertFalse(method.canAccess(null), "Method should not be accessible before the call");
            // Act
            ClassUtilities.trySetAccessible(method);
            // Assert – method should now be accessible and no exception should be thrown
            assertTrue(method.canAccess(null), "Method should be accessible after trySetAccessible");
        }
    }

    @Nested
    @DisplayName("Field accessibility edge cases")
    class FieldAccessibility {

        @Test
        @DisplayName("trySetAccessible on an already public field leaves it accessible")
        void testAlreadyPublicField() throws NoSuchFieldException {
            // Arrange – public field
            class DummyClass {

                public int alreadyPublic = 42;
            }
            Field field = DummyClass.class.getField("alreadyPublic");
            // Public fields are accessible by default
            assertTrue(field.canAccess(new DummyClass()));
            // Act – should be a no‑op but must not fail
            ClassUtilities.trySetAccessible(field);
            // Assert – still accessible
            assertTrue(field.canAccess(new DummyClass()));
        }
    }

    class DummyClass {

        public int alreadyPublic = 42;
    }

    class DummyClass {

        public int alreadyPublic = 42;
    }

    @Nested
    @DisplayName("Exception handling tests")
    class ExceptionHandling {

        /**
         * Helper {@link AccessibleObject} that throws a supplied unchecked exception
         * when {@code setAccessible(true)} is invoked.
         */
        static class ThrowingAccessibleObject extends AccessibleObject {

            private final RuntimeException toThrow;

            ThrowingAccessibleObject(RuntimeException toThrow) {
                this.toThrow = toThrow;
            }

            @Override
            public void setAccessible(boolean flag) {
                throw toThrow;
            }

            @Override
            public boolean isAccessible() {
                // Not used in the tests; returning false is fine.
                return false;
            }
        }

        private static Stream<Arguments> exceptionProvider() {
            return Stream.of(Arguments.of(new SecurityException("security")), Arguments.of(new InaccessibleObjectException("module")), Arguments.of(new IllegalArgumentException("illegal arg")), Arguments.of(new RuntimeException("generic")));
        }

        @ParameterizedTest(name = "trySetAccessible should swallow {0}")
        @MethodSource("exceptionProvider")
        @DisplayName("trySetAccessible swallows various unchecked exceptions")
        void testSwallowUncheckedExceptions(RuntimeException exceptionToThrow) {
            // Arrange
            AccessibleObject throwingObj = new ThrowingAccessibleObject(exceptionToThrow);
            // Act & Assert – no exception should escape
            assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(throwingObj));
        }

        @Test
        @DisplayName("trySetAccessible silently ignores a null argument")
        void testNullArgumentIsIgnored() {
            // Act & Assert – passing null must not throw
            assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(null));
        }

        @Test
        @DisplayName("trySetAccessible ignores SecurityException from custom AccessibleObject")
        void testSecurityExceptionIsIgnored() {
            // Arrange – custom object that throws SecurityException
            AccessibleObject securityObj = new ThrowingAccessibleObject(new SecurityException("simulated security"));
            // Act & Assert
            assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(securityObj));
        }
    }

    /**
     * Helper {@link AccessibleObject} that throws a supplied unchecked exception
     * when {@code setAccessible(true)} is invoked.
     */
    static class ThrowingAccessibleObject extends AccessibleObject {

        private final RuntimeException toThrow;

        ThrowingAccessibleObject(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public void setAccessible(boolean flag) {
            throw toThrow;
        }

        @Override
        public boolean isAccessible() {
            // Not used in the tests; returning false is fine.
            return false;
        }
    }

    /**
     * Helper {@link AccessibleObject} that throws a supplied unchecked exception
     * when {@code setAccessible(true)} is invoked.
     */
    static class ThrowingAccessibleObject extends AccessibleObject {

        private final RuntimeException toThrow;

        ThrowingAccessibleObject(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public void setAccessible(boolean flag) {
            throw toThrow;
        }

        @Override
        public boolean isAccessible() {
            // Not used in the tests; returning false is fine.
            return false;
        }
    }

    @Test
    @DisplayName("trySetAccessible should invoke setAccessible(true) on the provided AccessibleObject")
    void testTrySetAccessibleInvokesSetAccessible() {
        // Arrange – the mock is already created by MockitoExtension
        // (no additional stubbing needed because the default behavior is doNothing)
        // Act
        ClassUtilities.trySetAccessible(mockAccessibleObject);
        // Assert
        verify(mockAccessibleObject, times(1)).setAccessible(true);
    }

    @Test
    @DisplayName("trySetAccessible should suppress any exception thrown by setAccessible")
    void testTrySetAccessibleSuppressesException() {
        // Arrange – make setAccessible throw a RuntimeException
        doThrow(new RuntimeException("simulated failure")).when(mockAccessibleObject).setAccessible(true);
        // Act & Assert – the method must not propagate the exception
        assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(mockAccessibleObject));
        // Verify that setAccessible was indeed called
        verify(mockAccessibleObject, times(1)).setAccessible(true);
    }

    @Test
    @DisplayName("trySetAccessible works with a real private field")
    void testTrySetAccessibleWithRealField() throws NoSuchFieldException {
        // Arrange – obtain a private field from a sample class
        class Sample {

            private String hidden = "secret";
        }
        Field field = Sample.class.getDeclaredField("hidden");
        assertFalse(field.canAccess(new Sample()), "Field should not be accessible before the call");
        // Act
        ClassUtilities.trySetAccessible(field);
        // Assert – after the call the field should be accessible
        assertTrue(field.canAccess(new Sample()), "Field should be accessible after trySetAccessible");
    }
}
