package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

// import org.junit.*;
class ClassUtilities_setUseUnsafe_Test {

    //     @BeforeEach
    void setupBeforeEach() throws Exception {
        ClassUtilities.setUseUnsafe(false);
        setUseUnsafeFlag(false);
        setUnsafeInstance(null);
    }

    //     @AfterEach
    void teardownAfterEach() throws Exception {
        ClassUtilities.setUseUnsafe(false);
        setUseUnsafeFlag(false);
        setUnsafeInstance(null);
    }

    /**
     * Returns the current value of the private static {@code useUnsafe} flag.
     */
    private static boolean getUseUnsafeFlag() throws Exception {
        Field f = ClassUtilities.class.getDeclaredField("useUnsafe");
        f.setAccessible(true);
        return (boolean) f.get(null);
    }

    /**
     * Returns the current value of the private static {@code unsafe} field.
     */
    private static Object getUnsafeInstance() throws Exception {
        Field f = ClassUtilities.class.getDeclaredField("unsafe");
        f.setAccessible(true);
        return f.get(null);
    }

    /**
     * Force‑sets the private static {@code unsafe} field to the supplied value.
     */
    private static void setUnsafeInstance(Object value) throws Exception {
        Field f = ClassUtilities.class.getDeclaredField("unsafe");
        f.setAccessible(true);
        f.set(null, value);
    }

    /**
     * Force‑sets the private static {@code useUnsafe} flag to the supplied value.
     */
    private static void setUseUnsafeFlag(boolean value) throws Exception {
        Field f = ClassUtilities.class.getDeclaredField("useUnsafe");
        f.setAccessible(true);
        f.setBoolean(null, value);
    }

    /**
     * Simulates a failure in the {@code Unsafe} constructor that results in an
     * {@link InvocationTargetException}. This method reproduces the catch‑block logic
     * that {@code ClassUtilities.setUseUnsafe(true)} would execute when such an
     * exception occurs.
     */
    static void simulateInvocationTargetException() throws Exception {
        // Force the flag true and the unsafe field null.
        setUseUnsafeFlag(true);
        setUnsafeInstance(null);
        // Mimic the behaviour inside ClassUtilities.setUseUnsafe when the constructor fails.
        try {
            throw new InvocationTargetException(new Exception("Simulated constructor failure"));
        } catch (InvocationTargetException ite) {
            setUseUnsafeFlag(false);
        }
    }
}
