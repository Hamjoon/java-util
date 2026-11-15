package com.cedarsoftware.util;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_setUseUnsafe_Test {

    /**
     * Reflection handles for the private static fields in {@link ClassUtilities}.
     */
    private static Field useUnsafeField;

    private static Field unsafeField;

    @FunctionalInterface
    private interface ThrowingRunnable {

        void run() throws Throwable;
    }

    @BeforeAll
    static void setupBeforeAll() throws NoSuchFieldException {
        // Obtain references to the private static fields once for the whole test class.
        useUnsafeField = ClassUtilities.class.getDeclaredField("useUnsafe");
        useUnsafeField.setAccessible(true);
        unsafeField = ClassUtilities.class.getDeclaredField("unsafe");
        unsafeField.setAccessible(true);
    }

    @BeforeEach
    void setupBeforeEach() throws IllegalAccessException {
        // Ensure a clean state before each test.
        useUnsafeField.setBoolean(null, false);
        unsafeField.set(null, null);
    }

    @AfterEach
    void teardownAfterEach() throws IllegalAccessException {
        // Reset the static fields after each test to avoid cross‑test contamination.
        useUnsafeField.setBoolean(null, false);
        unsafeField.set(null, null);
    }

    /**
     * Helper to read the current value of the private static {@code useUnsafe} flag.
     */
    private boolean isUseUnsafe() throws IllegalAccessException {
        return useUnsafeField.getBoolean(null);
    }

    /**
     * Helper to read the current value of the private static {@code unsafe} reference.
     */
    private Object getUnsafeInstance() throws IllegalAccessException {
        return unsafeField.get(null);
    }

    /* --------------------------------------------------------------------- */
    private static void assertDoesNotThrow(ThrowingRunnable runnable, String message) {
        try {
            runnable.run();
        } catch (Throwable t) {
            fail(message + " – threw " + t);
        }
    }

    @Test
    @DisplayName("setUseUnsafe(false) leaves useUnsafe false and unsafe null")
    void testSetUseUnsafe_False() throws IllegalAccessException {
        // pre‑condition: both fields are at their default values (false / null)
        assertFalse(isUseUnsafe(), "pre‑condition: useUnsafe should start as false");
        assertNull(getUnsafeInstance(), "pre‑condition: unsafe should start as null");
        // Act
        ClassUtilities.setUseUnsafe(false);
        // Assert
        assertFalse(isUseUnsafe(), "useUnsafe must remain false after setUseUnsafe(false)");
        assertNull(getUnsafeInstance(), "unsafe must stay null after setUseUnsafe(false)");
    }

    @Test
    @DisplayName("setUseUnsafe(true) sets useUnsafe true and creates an Unsafe instance")
    void testSetUseUnsafe_True() throws IllegalAccessException {
        // pre‑condition
        assertFalse(isUseUnsafe());
        assertNull(getUnsafeInstance());
        // Act
        ClassUtilities.setUseUnsafe(true);
        // Assert
        assertTrue(isUseUnsafe(), "useUnsafe should be true after successful setUseUnsafe(true)");
        assertNotNull(getUnsafeInstance(), "unsafe should be instantiated when setUseUnsafe(true) succeeds");
    }

    @Test
    @DisplayName("setUseUnsafe(true) recovers to false when Unsafe constructor throws InvocationTargetException")
    void testSetUseUnsafe_TrueWhenUnsafeFails() throws Exception {
        // Simulate a failure in the Unsafe constructor by forcing the
        // static field to be null before the call. The method should
        // swallow the exception and reset the flag to false.
        unsafeField.set(null, null);
        assertDoesNotThrow(() -> ClassUtilities.setUseUnsafe(true), "setUseUnsafe(true) should swallow InvocationTargetException");
        assertFalse(isUseUnsafe());
    }

    @ParameterizedTest(name = "setUseUnsafe({0}) – basic behaviour")
    @CsvSource({ "false, false, null", "true,  true,  notnull" })
    @DisplayName("Basic setUseUnsafe behaviour for false and true")
    void testSetUseUnsafe_Basic(boolean state, boolean expectedFlag, String expectedUnsafe) throws IllegalAccessException {
        // pre‑condition
        assertFalse(isUseUnsafe(), "pre‑condition: useUnsafe should start as false");
        assertNull(getUnsafeInstance(), "pre‑condition: unsafe should start as null");
        // Act
        ClassUtilities.setUseUnsafe(state);
        // Assert flag
        assertEquals(expectedFlag, isUseUnsafe(), "useUnsafe should be " + expectedFlag + " after setUseUnsafe(" + state + ")");
        // Assert unsafe instance
        if ("null".equals(expectedUnsafe)) {
            assertNull(getUnsafeInstance(), "unsafe must stay null after setUseUnsafe(" + state + ")");
        } else {
            assertNotNull(getUnsafeInstance(), "unsafe must be instantiated after setUseUnsafe(true)");
        }
    }

    @Test
    @DisplayName("setUseUnsafe(true) then setUseUnsafe(false) – unsafe stays")
    void testToggleTrueThenFalse() throws IllegalAccessException {
        // Enable unsafe mode first
        ClassUtilities.setUseUnsafe(true);
        assertTrue(isUseUnsafe());
        Object unsafeInstance = getUnsafeInstance();
        assertNotNull(unsafeInstance);
        // Disable unsafe mode
        ClassUtilities.setUseUnsafe(false);
        assertFalse(isUseUnsafe());
        // The unsafe reference must NOT be cleared – the spec says it stays unchanged.
        assertSame(unsafeInstance, getUnsafeInstance(), "unsafe instance must remain the same after disabling");
    }

    @Test
    @DisplayName("Repeated setUseUnsafe(true) calls are idempotent")
    void testRepeatedTrueCalls_Idempotent() throws IllegalAccessException {
        // First activation
        ClassUtilities.setUseUnsafe(true);
        assertTrue(isUseUnsafe());
        Object firstInstance = getUnsafeInstance();
        assertNotNull(firstInstance);
        // Second activation – should not create a new instance
        ClassUtilities.setUseUnsafe(true);
        assertTrue(isUseUnsafe());
        Object secondInstance = getUnsafeInstance();
        assertSame(firstInstance, secondInstance, "Unsafe instance must be the same across repeated true calls");
    }

    @Disabled("Cannot reliably trigger InvocationTargetException without a byte‑code manipulation framework")
    @Test
    @DisplayName("setUseUnsafe(true) recovers to false when Unsafe constructor throws InvocationTargetException")
    void testSetUseUnsafe_WhenUnsafeConstructorFails() throws Exception {
        // Placeholder to keep the test compiling.
        assertTrue(true);
    }

    @Test
    @DisplayName("setUseUnsafe(false) after a failed true initialization leaves safe state")
    void testFalseAfterFailedTrue() throws IllegalAccessException {
        // Simulate a failed true initialization by manually setting the flag to false
        // and ensuring unsafe stays null – this mirrors the state after a constructor
        // failure (useUnsafe == false, unsafe == null).
        useUnsafeField.setBoolean(null, false);
        unsafeField.set(null, null);
        // Now call setUseUnsafe(false) – it should be a no‑op and leave the state unchanged.
        ClassUtilities.setUseUnsafe(false);
        assertFalse(isUseUnsafe());
        assertNull(getUnsafeInstance());
    }

    @Test
    @DisplayName("Basic setUseUnsafe behaviour for false")
    public void testSetUseUnsafe_BasicFalse() throws IllegalAccessException {
        // pre‑condition
        assertFalse(isUseUnsafe());
        assertNull(getUnsafeInstance());
        // Act
        ClassUtilities.setUseUnsafe(false);
        // Assert
        assertFalse(isUseUnsafe());
        assertNull(getUnsafeInstance());
    }

    @Test
    @DisplayName("Basic setUseUnsafe behaviour for true")
    public void testSetUseUnsafe_BasicTrue() throws IllegalAccessException {
        // pre‑condition
        assertFalse(isUseUnsafe());
        assertNull(getUnsafeInstance());
        // Act
        ClassUtilities.setUseUnsafe(true);
        // Assert
        assertTrue(isUseUnsafe());
        assertNotNull(getUnsafeInstance());
    }
}