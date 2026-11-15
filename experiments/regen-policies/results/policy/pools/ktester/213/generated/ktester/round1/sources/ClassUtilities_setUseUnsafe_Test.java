package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

// import org.junit.*;
// import static org.junit.Assert.*;
// @ExtendWith(MockitoExtension.class)
class ClassUtilities_setUseUnsafe_Test {

    /**
     * Reflection handles for the private static fields in {@link ClassUtilities}.
     */
    private static Field useUnsafeField;

    private static Field unsafeField;

    /**
     * A test‑only version of {@code com.cedarsoftware.util.Unsafe} that allows us to
     * control its constructor behaviour. The production class is shadowed by this
     * class because test classes appear earlier on the classpath.
     *
     * <p>The static {@code mode} determines how the constructor behaves:
     * <ul>
     *   <li>{@link Mode#NORMAL} – construction succeeds.</li>
     *   <li>{@link Mode#INVOCATION_TARGET_EXCEPTION} – throws {@link InvocationTargetException}.</li>
     *   <li>{@link Mode#RUNTIME_EXCEPTION} – throws a generic {@link RuntimeException}.</li>
     * </ul>
     */
    class TestUnsafe {

        /**
         * Behaviour modes for the constructor.
         */
        enum Mode {

            NORMAL, INVOCATION_TARGET_EXCEPTION, RUNTIME_EXCEPTION
        }

        private static volatile Mode mode = Mode.NORMAL;

        /**
         * Allows tests to set the desired behaviour.
         */
        static void setMode(Mode m) {
            mode = m;
        }

        /**
         * Constructor used by {@link ClassUtilities#setUseUnsafe(boolean)}.
         */
        public TestUnsafe() throws InvocationTargetException {
            switch(mode) {
                case NORMAL:
                    // successful construction – nothing to do.
                    break;
                case INVOCATION_TARGET_EXCEPTION:
                    // Simulate the failure that the production code expects.
                    throw new InvocationTargetException(new Exception("simulated cause"));
                case RUNTIME_EXCEPTION:
                    // Simulate an unexpected runtime failure.
                    throw new RuntimeException("simulated runtime exception from Unsafe constructor");
                default:
                    throw new IllegalStateException("Unsupported mode: " + mode);
            }
        }
    }

    /**
     * Test‑only version of {@code com.cedarsoftware.util.Unsafe} that allows us to
     * control its constructor behaviour. The production class is shadowed by this
     * class because test classes appear earlier on the classpath.
     *
     * <p>The static {@code mode} determines how the constructor behaves:
     * <ul>
     *   <li>{@link Mode#NORMAL} – construction succeeds.</li>
     *   <li>{@link Mode#INVOCATION_TARGET_EXCEPTION} – throws {@link InvocationTargetException}.</li>
     *   <li>{@link Mode#RUNTIME_EXCEPTION} – throws a generic {@link RuntimeException}.</li>
     * </ul>
     * </p>
     */
    public static class Unsafe {

        /**
         * Behaviour modes for the constructor.
         */
        public enum Mode {

            NORMAL, INVOCATION_TARGET_EXCEPTION, RUNTIME_EXCEPTION
        }

        private static volatile Mode mode = Mode.NORMAL;

        /**
         * Allows tests to set the desired behaviour.
         */
        public static void setMode(Mode m) {
            mode = m;
        }

        /**
         * Constructor used by {@link ClassUtilities#setUseUnsafe(boolean)}.
         */
        public Unsafe() throws InvocationTargetException {
            switch(mode) {
                case NORMAL:
                    // successful construction – nothing to do.
                    break;
                case INVOCATION_TARGET_EXCEPTION:
                    throw new InvocationTargetException(new Exception("simulated cause"));
                case RUNTIME_EXCEPTION:
                    throw new RuntimeException("simulated runtime exception from Unsafe constructor");
                default:
                    throw new IllegalStateException("Unsupported mode: " + mode);
            }
        }
    }

    //     @BeforeAll
    static void setupBeforeAll() throws NoSuchFieldException {
        // Obtain the private static fields once for the whole test suite.
        useUnsafeField = ClassUtilities.class.getDeclaredField("useUnsafe");
        useUnsafeField.setAccessible(true);
        unsafeField = ClassUtilities.class.getDeclaredField("unsafe");
        unsafeField.setAccessible(true);
    }

    //     @BeforeEach
    void setupBeforeEach() throws IllegalAccessException {
        // Ensure a clean state before each test.
        useUnsafeField.setBoolean(null, false);
        unsafeField.set(null, null);
        // Default behaviour of the test Unsafe is to succeed.
        Unsafe.setMode(Unsafe.Mode.NORMAL);
    }

    //     @AfterEach
    void teardownAfterEach() throws IllegalAccessException {
        // Reset the static state after each test to avoid cross‑test contamination.
        useUnsafeField.setBoolean(null, false);
        unsafeField.set(null, null);
        Unsafe.setMode(Unsafe.Mode.NORMAL);
    }

    /**
     * Helper to read the current value of the private static {@code useUnsafe} flag.
     */
    private static boolean getUseUnsafeFlag() throws IllegalAccessException {
        return useUnsafeField.getBoolean(null);
    }

    /**
     * Helper to read the current value of the private static {@code unsafe} reference.
     */
    private static Object getUnsafeInstance() throws IllegalAccessException {
        return unsafeField.get(null);
    }

    /**
     * Initialise the reflective handles – should be called once before any use.
     */
    static void initReflectionHandles() throws NoSuchFieldException {
        useUnsafeField = ClassUtilities.class.getDeclaredField("useUnsafe");
        useUnsafeField.setAccessible(true);
        unsafeField = ClassUtilities.class.getDeclaredField("unsafe");
        unsafeField.setAccessible(true);
    }

    /**
     * Reset the static state to a clean baseline.
     */
    static void resetState() throws IllegalAccessException {
        useUnsafeField.setBoolean(null, false);
        unsafeField.set(null, null);
        Unsafe.setMode(Unsafe.Mode.NORMAL);
    }
}
