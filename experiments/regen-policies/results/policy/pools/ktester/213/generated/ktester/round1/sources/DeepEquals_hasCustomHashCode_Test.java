package com.cedarsoftware.util;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.util.Collections;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Assertions;

@ExtendWith(MockitoExtension.class)
// @RunWith(JUnit4.class)
class DeepEquals_hasCustomHashCode_Test {

    /**
     * Simple class that relies on {@code Object}'s default {@code hashCode}
     * implementation.
     */
    private static class NoCustomHash {
        // No hashCode() override
    }

    /**
     * Simple class that provides its own {@code hashCode} implementation.
     */
    private static class CustomHash {

        @Override
        public int hashCode() {
            return 42;
        }
    }

    private static Stream<Arguments> customHashCodeClasses() {
        // Standard JDK classes known to override hashCode()
        return Stream.of(Arguments.of("java.lang.String", String.class), Arguments.of("com.cedarsoftware.util.DeepEquals (itself)", DeepEquals.class), Arguments.of("java.util.ArrayList", java.util.ArrayList.class), Arguments.of("java.lang.Boolean", Boolean.class), Arguments.of("java.util.HashMap", java.util.HashMap.class), Arguments.of("java.util.Collections$EmptyList", Collections.emptyList().getClass()));
    }

    private static Stream<Arguments> noCustomHashCodeClasses() {
        return Stream.of(Arguments.of("java.lang.Object", Object.class), Arguments.of("java.util.concurrent.atomic.AtomicInteger", java.util.concurrent.atomic.AtomicInteger.class), Arguments.of("java.lang.Runnable (interface)", Runnable.class));
    }

    /* -----------------------------------------------------------------
     * Helper – a tiny JUnit‑4 version of assertThrows.
     * ----------------------------------------------------------------- */
    private static <T extends Throwable> void assertThrows(Class<T> expectedType, Runnable executable, String message) {
        try {
            executable.run();
            Assertions.fail(message + " – expected exception: " + expectedType.getName());
        } catch (Throwable actual) {
            if (!expectedType.isInstance(actual)) {
                Assertions.fail(message + " – unexpected exception type: " + actual.getClass().getName());
            }
            // expected exception – test passes
        }
    }

    @Test
    void hasCustomHashCode_whenClassOverridesHashCode_returnsTrue() {
        Assertions.assertTrue(DeepEquals.hasCustomHashCode(CustomHash.class), "Class that overrides hashCode() should be reported as having a custom hashCode");
    }

    @Test
    void hasCustomHashCode_whenClassDoesNotOverrideHashCode_returnsFalse() {
        Assertions.assertFalse(DeepEquals.hasCustomHashCode(NoCustomHash.class), "Class that does not override hashCode() should be reported as not having a custom hashCode");
    }

    @Test
    void hasCustomHashCode_whenObjectClass_returnsFalse() {
        Assertions.assertFalse(DeepEquals.hasCustomHashCode(Object.class), "java.lang.Object should not be reported as having a custom hashCode");
    }

    @Test
    void hasCustomHashCode_whenNullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DeepEquals.hasCustomHashCode(null), "Passing null should result in NullPointerException");
    }

    @Test
    void hasCustomHashCode_whenPrimitiveClass_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> DeepEquals.hasCustomHashCode(int.class), "Primitive type classes should cause IllegalArgumentException");
    }

    @Test
    void hasCustomHashCode_whenReflectionSecurityException_propagatesException() {
        // Mock the static call to ReflectionUtils.getMethod to throw SecurityException
        try (MockedStatic<ReflectionUtils> mocked = Mockito.mockStatic(ReflectionUtils.class)) {
            mocked.when(() -> ReflectionUtils.getMethod(DeepEquals.class, "hashCode")).thenThrow(new SecurityException("Access denied"));
            SecurityException thrown = Assertions.assertThrows(SecurityException.class, () -> DeepEquals.hasCustomHashCode(DeepEquals.class));
            Assertions.assertEquals("Access denied", thrown.getMessage());
        }
    }
}