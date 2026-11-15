package com.cedarsoftware.util;

import org.mockito.Mock;
import static org.mockito.Mockito.*;
import java.lang.reflect.*;
// added import
import java.lang.reflect.InaccessibleObjectException;
// import org.junit.*;
import java.util.ArrayList;
import java.util.List;
// import static org.junit.Assert.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.provider.MethodSource;
// corrected with semicolon
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_trySetAccessible_Test {

    /* --------------------------------------------------------------------- */
    private static class Fixture {

        // a private static method – not accessible by default
        @SuppressWarnings("unused")
        private static void hidden() {
            /* no‑op */
        }
    }

    /* --------------------------------------------------------------------- */
    private static class SampleClass {

        private SampleClass() {
            /* private ctor */
        }

        private static void privateStaticMethod() {
            /* no‑op */
        }

        private void privateInstanceMethod() {
            /* no‑op */
        }

        public void publicMethod() {
            /* no‑op */
        }

        private static String privateStaticField = "secret";

        private String privateInstanceField = "secret";

        public String alreadyAccessibleField = "visible";
    }

    /**
     * A tiny {@link AccessibleObject} subclass that throws a custom
     * {@link RuntimeException} when {@code setAccessible(true)} is invoked.
     */
    private static class ThrowingAccessibleObject extends AccessibleObject {

        private final RuntimeException toThrow;

        ThrowingAccessibleObject(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public void setAccessible(boolean flag) {
            if (flag) {
                throw toThrow;
            }
        }
    }

    /**
     * Helper that works across JDK versions (pre‑Java 9 {@code canAccess} may not exist).
     */
    private static boolean isAccessible(AccessibleObject ao) {
        try {
            if (ao instanceof Method) {
                return ((Method) ao).canAccess(null);
            } else if (ao instanceof Constructor) {
                return ((Constructor<?>) ao).canAccess(null);
            } else if (ao instanceof Field) {
                return ((Field) ao).canAccess(null);
            } else {
                // Fallback – AccessibleObject has a protected isAccessible()
                Method m = AccessibleObject.class.getDeclaredMethod("isAccessible");
                m.setAccessible(true);
                return (boolean) m.invoke(ao);
            }
        } catch (Throwable t) {
            // If anything goes wrong we treat the object as not accessible.
            return false;
        }
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> normalOperationProvider() throws Exception {
        // private static method
        Method privateMethod = SampleClass.class.getDeclaredMethod("privateStaticMethod");
        // private field (static)
        Field privateField = SampleClass.class.getDeclaredField("privateStaticField");
        // private constructor
        Constructor<SampleClass> privateCtor = SampleClass.class.getDeclaredConstructor();
        // public method (already accessible)
        Method publicMethod = SampleClass.class.getMethod("publicMethod");
        // already accessible field (we set it true beforehand)
        Field alreadyAccessible = SampleClass.class.getField("alreadyAccessibleField");
        alreadyAccessible.setAccessible(true);
        return Stream.of(Arguments.of("Normal operation with Method", privateMethod), Arguments.of("Normal operation with Field", privateField), Arguments.of("Normal operation with Constructor", privateCtor), Arguments.of("Normal operation with public Method", publicMethod), Arguments.of("Normal operation with already accessible Field", alreadyAccessible));
    }

    private static Stream<Arguments> exceptionSwallowingProvider() throws Exception {
        // SecurityException
        AccessibleObject secMock = mock(AccessibleObject.class);
        doThrow(new SecurityException("simulated")).when(secMock).setAccessible(true);
        // IllegalArgumentException
        AccessibleObject iaeMock = mock(AccessibleObject.class);
        doThrow(new IllegalArgumentException("bad arg")).when(iaeMock).setAccessible(true);
        // InaccessibleObjectException (Java 9+)
        AccessibleObject ioeMock = mock(AccessibleObject.class);
        doThrow(new InaccessibleObjectException("module barrier")).when(ioeMock).setAccessible(true);
        // Custom RuntimeException via concrete subclass
        AccessibleObject customRuntime = new ThrowingAccessibleObject(new RuntimeException("custom runtime"));
        return Stream.of(Arguments.of("SecurityException", secMock), Arguments.of("IllegalArgumentException", iaeMock), Arguments.of("InaccessibleObjectException", ioeMock), Arguments.of("custom RuntimeException", customRuntime));
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("trySetAccessible makes a private method accessible")
    void testTrySetAccessible_makesMethodAccessible() throws Exception {
        Method hiddenMethod = Fixture.class.getDeclaredMethod("hidden");
        assertFalse(hiddenMethod.canAccess(null), "Method should not be accessible before trySetAccessible");
        ClassUtilities.trySetAccessible(hiddenMethod);
        assertTrue(hiddenMethod.canAccess(null), "Method should be accessible after trySetAccessible");
    }

    @Test
    @DisplayName("trySetAccessible silently ignores exceptions from setAccessible")
    void testTrySetAccessible_ignoresException(@Mock AccessibleObject mockObject) throws Exception {
        doThrow(new RuntimeException("simulated security exception")).when(mockObject).setAccessible(true);
        assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(mockObject), "trySetAccessible should swallow any exception from setAccessible");
        verify(mockObject, times(1)).setAccessible(true);
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("normalOperationProvider")
    @DisplayName("trySetAccessible makes various members accessible without throwing")
    void testTrySetAccessible_normalOperation(String description, AccessibleObject ao) throws Exception {
        if (!description.contains("already accessible")) {
            assertFalse(isAccessible(ao), "Pre‑condition: member should not be accessible");
        }
        assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(ao), "trySetAccessible should not throw for normal operation");
        assertTrue(isAccessible(ao), "Member should be accessible after trySetAccessible");
    }

    @Test
    @DisplayName("trySetAccessible handles null argument without throwing")
    void testTrySetAccessible_nullArgument() {
        assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(null), "trySetAccessible must ignore NullPointerException from a null argument");
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("exceptionSwallowingProvider")
    @DisplayName("trySetAccessible swallows various Exceptions thrown by setAccessible")
    void testTrySetAccessible_swallowsExceptions(String description, AccessibleObject mockObj) throws Exception {
        assertDoesNotThrow(() -> ClassUtilities.trySetAccessible(mockObj), "trySetAccessible should swallow " + description);
        verify(mockObj, times(1)).setAccessible(true);
    }

    @Test
    @DisplayName("trySetAccessible propagates Errors (e.g., AssertionError) instead of swallowing")
    void testTrySetAccessible_propagatesError(@Mock AccessibleObject errorMock) throws Exception {
        doThrow(new AssertionError("assertion failure")).when(errorMock).setAccessible(true);
        AssertionError thrown = assertThrows(AssertionError.class, () -> ClassUtilities.trySetAccessible(errorMock), "Errors should NOT be swallowed by trySetAccessible");
        assertEquals("assertion failure", thrown.getMessage(), "Propagation should preserve the original error message");
    }
}
