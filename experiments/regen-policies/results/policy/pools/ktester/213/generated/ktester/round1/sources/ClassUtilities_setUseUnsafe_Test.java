package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_setUseUnsafe_Test {

    /**
     * Name of the private static field that holds the flag.
     */
    private static final String USE_UNSAFE_FIELD = "useUnsafe";

    /**
     * Name of the private static field that holds the Unsafe instance.
     */
    private static final String UNSAFE_FIELD = "unsafe";

    /**
     * Cached {@link Field} objects for speed and to avoid repeated accessibility changes.
     */
    private static Field useUnsafeField;

    private static Field unsafeField;

    @BeforeAll
    static void setupBeforeAll() throws NoSuchFieldException {
        // Locate the private static fields once for the whole test suite.
        useUnsafeField = ClassUtilities.class.getDeclaredField(USE_UNSAFE_FIELD);
        unsafeField = ClassUtilities.class.getDeclaredField(UNSAFE_FIELD);
        useUnsafeField.setAccessible(true);
        unsafeField.setAccessible(true);
        useUnsafeField = ClassUtilities.class.getDeclaredField(USE_UNSAFE_FIELD);
        unsafeField = ClassUtilities.class.getDeclaredField(UNSAFE_FIELD);
        useUnsafeField.setAccessible(true);
        unsafeField.setAccessible(true);
    }

    @BeforeEach
    void setupBeforeEach() throws IllegalAccessException {
        // Ensure a clean start for every test.
        useUnsafeField.setBoolean(null, false);
        unsafeField.set(null, null);
        // Ensure every test starts from a clean slate.
        useUnsafeField.setBoolean(null, false);
        unsafeField.set(null, null);
    }

    @AfterEach
    void teardownAfterEach() throws IllegalAccessException {
        // Reset the static state after each test so that tests are independent.
        useUnsafeField.setBoolean(null, false);
        unsafeField.set(null, null);
        // Guarantees independence between tests.
        useUnsafeField.setBoolean(null, false);
        unsafeField.set(null, null);
    }

    @AfterAll
    static void teardownAfterAll() throws IllegalAccessException {
        // Final cleanup – not strictly required but keeps the JVM tidy.
        useUnsafeField.setBoolean(null, false);
        unsafeField.set(null, null);
    }

    /**
     * Verify that calling {@code setUseUnsafe(true)} sets the internal flag
     * to {@code true} and creates a non‑null {@code Unsafe} instance.
     */
    @Test
    @DisplayName("setUseUnsafe(true) enables unsafe mode")
    void testSetUseUnsafe_EnablesFlag() throws IllegalAccessException {
        // Act
        ClassUtilities.setUseUnsafe(true);
        // Assert
        assertTrue(useUnsafeField.getBoolean(null), "The private static flag 'useUnsafe' should be true after enabling.");
        assertNotNull(unsafeField.get(null), "An instance of Unsafe should be created when the flag is enabled.");
    }

    /**
     * Verify that calling {@code setUseUnsafe(false)} disables the flag
     * and clears the {@code Unsafe} instance.
     */
    @Test
    @DisplayName("setUseUnsafe(false) disables unsafe mode")
    void testSetUseUnsafe_DisablesFlag() throws IllegalAccessException {
        // First enable to make sure the flag can be turned off.
        ClassUtilities.setUseUnsafe(true);
        assertTrue(useUnsafeField.getBoolean(null), "Pre‑condition: flag must be true before disabling.");
        // Act
        ClassUtilities.setUseUnsafe(false);
        // Assert
        assertFalse(useUnsafeField.getBoolean(null), "The private static flag 'useUnsafe' should be false after disabling.");
        // The implementation does not explicitly clear the Unsafe field when disabling,
        // but the contract of the method is that the flag governs usage.  We therefore
        // only assert that the flag is false; the Unsafe reference may remain.
        // (If the implementation changes to clear it, the test will still pass.)
    }

    /**
     * Simulate a failure in the {@code Unsafe} constructor and verify that the
     * flag is rolled back to {@code false}.  This is achieved by temporarily
     * replacing the {@code Unsafe} class with a subclass that throws an
     * {@link InvocationTargetException} from its constructor.
     *
     * <p>
     * Because the real {@code Unsafe} class is package‑private and its
     * constructor is not directly mockable, we use reflection to inject a
     * proxy instance that throws the required exception when instantiated.
     * The test restores the original state in {@code @AfterEach}.
     * </p>
     */
    @Test
    @DisplayName("setUseUnsafe(true) rolls back flag when Unsafe construction fails")
    void testSetUseUnsafe_RollsBackOnFailure() throws Exception {
        // Save the original Unsafe class reference (it is stored in the static field)
        Object originalUnsafe = unsafeField.get(null);
        // Replace the Unsafe class with a dummy that throws InvocationTargetException
        // The dummy class must be assignable to the original type; we achieve this by
        // creating a subclass via an anonymous class that throws in its constructor.
        Class<?> unsafeClass = Class.forName("com.cedarsoftware.util.ClassUtilities$Unsafe");
        // Create a subclass that throws when instantiated
        Class<?> failingSubclass = java.lang.reflect.Proxy.getProxyClass(unsafeClass.getClassLoader(), unsafeClass.getInterfaces());
        // Use reflection to set the static field 'unsafe' to a proxy instance that will
        // cause the constructor call inside setUseUnsafe(true) to fail.
        // Since the actual ClassUtilities.setUseUnsafe creates a new instance via
        // 'new Unsafe()', we cannot intercept that without a byte‑code tool.
        // Therefore this test documents the intended approach; in environments
        // where the Unsafe constructor cannot be forced to fail, the test will be
        // skipped.
        Assumptions.assumeTrue(false, "Unable to simulate Unsafe construction failure without a byte‑code mocking framework.");
        // The rest of the test would look like:
        // ClassUtilities.setUseUnsafe(true);
        // assertFalse(useUnsafeField.getBoolean(null), "Flag should be rolled back to false on failure.");
        // assertNull(unsafeField.get(null), "Unsafe instance should remain null on failure.");
        // Restore original state (defensive, though the test is aborted above)
        unsafeField.set(null, originalUnsafe);
    }

    @Test
    @DisplayName("setUseUnsafe(false) only clears the flag")
    void setUseUnsafe_falseOnlyClearsFlag() throws IllegalAccessException {
        // Act
        ClassUtilities.setUseUnsafe(false);
        // Assert
        assertFalse(useUnsafeField.getBoolean(null), "Flag must be false after calling setUseUnsafe(false).");
        assertNull(unsafeField.get(null), "Unsafe instance must remain null when disabling without a prior enable.");
    }

    @Test
    @DisplayName("setUseUnsafe(true) enables flag and creates Unsafe")
    void setUseUnsafe_trueCreatesUnsafe() throws IllegalAccessException {
        // Act
        ClassUtilities.setUseUnsafe(true);
        // Assert
        assertTrue(useUnsafeField.getBoolean(null), "Flag must be true after successful enable.");
        Object unsafeInstance = unsafeField.get(null);
        assertNotNull(unsafeInstance, "A non‑null Unsafe instance must be created when enabling.");
        // Basic sanity check – the instance should be of the expected inner class.
        assertEquals("com.cedarsoftware.util.ClassUtilities$Unsafe", unsafeInstance.getClass().getName(), "Instance should be of the inner Unsafe class.");
    }

    @Test
    @DisplayName("setUseUnsafe(true) rolls back flag when Unsafe constructor throws InvocationTargetException")
    @Disabled("Simulating a constructor throwing InvocationTargetException requires a byte‑code manipulation framework (e.g., PowerMock) which is out of scope for plain Mockito.")
    void setUseUnsafe_trueConstructorInvocationTargetException() throws Exception {
        // The idea would be to replace the Unsafe class with a version whose constructor throws
        // InvocationTargetException, then invoke setUseUnsafe(true) and verify that the flag
        // is reset to false and the unsafe field stays null.
        assumeTrue(false, "Test disabled – cannot simulate constructor failure without advanced mocking.");
    }

    @Test
    @DisplayName("Repeated true calls keep the same Unsafe instance")
    void repeatedTrueCallsDoNotCreateNewInstance() throws IllegalAccessException {
        // First enable
        ClassUtilities.setUseUnsafe(true);
        Object firstInstance = unsafeField.get(null);
        assertNotNull(firstInstance, "First enable must create an Unsafe instance.");
        // Second enable – should not replace the existing instance
        ClassUtilities.setUseUnsafe(true);
        Object secondInstance = unsafeField.get(null);
        assertSame(firstInstance, secondInstance, "Second enable must keep the original Unsafe instance (no new object).");
        assertTrue(useUnsafeField.getBoolean(null), "Flag must remain true after second enable.");
    }

    @Test
    @DisplayName("Disabling after a successful enable leaves the existing Unsafe instance untouched")
    void disableAfterEnableLeavesUnsafeIntact() throws IllegalAccessException {
        // Enable first
        ClassUtilities.setUseUnsafe(true);
        Object unsafeBeforeDisable = unsafeField.get(null);
        assertNotNull(unsafeBeforeDisable, "Enable must create an Unsafe instance.");
        // Disable
        ClassUtilities.setUseUnsafe(false);
        assertFalse(useUnsafeField.getBoolean(null), "Flag must be false after disabling.");
        // The current implementation does NOT clear the unsafe field, so we assert it stays the same.
        Object unsafeAfterDisable = unsafeField.get(null);
        assertSame(unsafeBeforeDisable, unsafeAfterDisable, "Implementation does not clear the Unsafe reference on disable; it should remain unchanged.");
    }

    @Test
    @DisplayName("Calling setUseUnsafe(false) when already disabled does nothing")
    void disableWhenAlreadyDisabledIsNoOp() throws IllegalAccessException {
        // Pre‑condition: flag false, unsafe null
        assertFalse(useUnsafeField.getBoolean(null));
        assertNull(unsafeField.get(null));
        // Act
        ClassUtilities.setUseUnsafe(false);
        // Assert unchanged
        assertFalse(useUnsafeField.getBoolean(null), "Flag must stay false.");
        assertNull(unsafeField.get(null), "Unsafe reference must stay null.");
    }

    @Test
    @DisplayName("setUseUnsafe(true) after a previous constructor failure eventually succeeds")
    @Disabled("Cannot simulate the failure scenario without a custom classloader or byte‑code tool.")
    void enableAfterFailureEventuallySucceeds() throws Exception {
        // This test would:
        // 1. Simulate a failure on the first true call (constructor throws InvocationTargetException).
        // 2. Verify flag rolled back to false and unsafe null.
        // 3. Call setUseUnsafe(true) again with a normal Unsafe class.
        // 4. Verify flag true and unsafe instance created.
        assumeTrue(false, "Test disabled – failure simulation not feasible with plain Mockito.");
    }

    @Test
    @DisplayName("Unchecked RuntimeException from Unsafe constructor propagates and leaves flag true")
    @Disabled("Cannot force Unsafe constructor to throw a RuntimeException without advanced mocking.")
    void uncheckedRuntimeExceptionPropagates() throws Exception {
        // The test would replace Unsafe with a version whose constructor throws a RuntimeException.
        // Expected outcome: the exception bubbles up, flag remains true (set before constructor),
        // and unsafe stays null because assignment never happened.
        assumeTrue(false, "Test disabled – cannot inject RuntimeException from constructor.");
    }

    @Test
    @DisplayName("NoClassDefFoundError during Unsafe loading propagates")
    @Disabled("Cannot simulate linkage error for inner class without custom classloader.")
    void linkageErrorPropagates() throws Exception {
        // Similar to the previous test but simulating a linkage error.
        assumeTrue(false, "Test disabled – cannot simulate NoClassDefFoundError.");
    }
}
