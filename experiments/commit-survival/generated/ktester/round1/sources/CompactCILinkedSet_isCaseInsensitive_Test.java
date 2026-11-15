package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Permission;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(MockitoExtension.class)
class CompactCILinkedSet_isCaseInsensitive_Test {

    /**
     * Instance used in the tests.
     */
    private CompactCILinkedSet<String> set;

    /**
     * Simple public subclass that forwards the protected method.
     */
    static class PublicCompactCILinkedSet<E> extends CompactCILinkedSet<E> {

        public boolean isCaseInsensitivePublic() {
            return super.isCaseInsensitive();
        }
    }

    /**
     * Subclass that overrides the method to return false, but still offers a super‑call.
     */
    static class OverridingCompactCILinkedSet<E> extends CompactCILinkedSet<E> {

        @Override
        protected boolean isCaseInsensitive() {
            // deliberately break the contract
            return false;
        }

        public boolean callSuperIsCaseInsensitive() {
            return CompactCILinkedSet.super.isCaseInsensitive();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor for a fresh instance before each test.
        set = new CompactCILinkedSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly necessary but keeps symmetry.
        set = null;
    }

    @Test
    void isCaseInsensitive_returnsTrue_forDefaultInstance() {
        // GIVEN – a freshly constructed CompactCILinkedSet (handled in @BeforeEach)
        // WHEN
        boolean result = set.isCaseInsensitive();
        // THEN
        assertTrue(result, "CompactCILinkedSet.isCaseInsensitive() should always return true");
    }

    @Test
    void isCaseInsensitive_returnsTrue_whenConstructedFromCollection() {
        // GIVEN – a set created from an existing collection
        CompactCILinkedSet<String> collectionBasedSet = new CompactCILinkedSet<>(Collections.singletonList("example"));
        // WHEN
        boolean result = collectionBasedSet.isCaseInsensitive();
        // THEN
        assertTrue(result, "CompactCILinkedSet.isCaseInsensitive() should return true even when constructed from a collection");
    }

    @Test
    void isCaseInsensitive_returnsTrue_viaPublicSubclass() {
        // GIVEN
        PublicCompactCILinkedSet<String> publicSet = new PublicCompactCILinkedSet<>();
        // WHEN
        boolean result = publicSet.isCaseInsensitivePublic();
        // THEN
        assertTrue(result, "Public subclass should expose true from isCaseInsensitive()");
    }

    @ParameterizedTest(name = "Generic type {0} should still return true")
    @ValueSource(strings = { "String", "Integer" })
    void isCaseInsensitive_returnsTrue_forDifferentGenerics(String genericType) throws Exception {
        // The test does not need to actually use the generic type at runtime;
        // we just instantiate the public subclass with the appropriate type.
        if ("String".equals(genericType)) {
            PublicCompactCILinkedSet<String> set = new PublicCompactCILinkedSet<>();
            assertTrue(set.isCaseInsensitivePublic());
        } else {
            PublicCompactCILinkedSet<Integer> set = new PublicCompactCILinkedSet<>();
            assertTrue(set.isCaseInsensitivePublic());
        }
    }

    @Test
    void reflection_withSetAccessible_true_returnsTrue() throws Exception {
        Method m = CompactCILinkedSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(set);
        assertTrue(result, "Reflectively invoked method (accessible) should return true");
    }

    @Test
    void reflection_withoutSetAccessible_throwsIllegalAccessException() throws Exception {
        Method m = CompactCILinkedSet.class.getDeclaredMethod("isCaseInsensitive");
        // Do NOT call setAccessible(true)
        assertThrows(IllegalAccessException.class, () -> m.invoke(set));
    }

    @Test
    void reflection_invokeWithUnexpectedArgument_throwsIllegalArgumentException() throws Exception {
        Method m = CompactCILinkedSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        // The method expects no arguments; passing one should cause IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> m.invoke(set, 42));
    }

    @Test
    void reflection_invokeOnNullInstance_throwsNullPointerException() throws Exception {
        Method m = CompactCILinkedSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        assertThrows(NullPointerException.class, () -> m.invoke(null));
    }

    @Test
    void superCallFromOverridingSubclass_returnsTrue() {
        OverridingCompactCILinkedSet<String> sub = new OverridingCompactCILinkedSet<>();
        // The overridden method returns false, but the helper calls super.
        assertTrue(sub.callSuperIsCaseInsensitive(), "Super call should return true even if subclass overrides to false");
    }

    @Test
    void reflection_overrideThrowsRuntimeException_wrappedInInvocationTargetException() throws Exception {
        // Create an anonymous subclass that overrides the protected method to throw.
        CompactCILinkedSet<String> throwingInstance = new CompactCILinkedSet<String>() {

            @Override
            protected boolean isCaseInsensitive() {
                throw new RuntimeException("forced");
            }
        };
        Method m = CompactCILinkedSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        InvocationTargetException ite = assertThrows(InvocationTargetException.class, () -> m.invoke(throwingInstance));
        assertTrue(ite.getCause() instanceof RuntimeException);
        assertEquals("forced", ite.getCause().getMessage());
    }

    @Test
    void concurrentInvocations_allReturnTrue() throws Exception {
        final int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicBoolean anyFailure = new AtomicBoolean(false);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // ensure all threads start together
                    startLatch.await();
                    if (!set.isCaseInsensitive()) {
                        anyFailure.set(true);
                    }
                } catch (Exception e) {
                    anyFailure.set(true);
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // Release all threads
        startLatch.countDown();
        // Wait for completion
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Threads did not finish in time");
        executor.shutdownNow();
        assertFalse(anyFailure.get(), "All concurrent invocations should return true");
    }

    @Test
    void reflection_setAccessible_deniedBySecurityManager_throwsSecurityException() throws Exception {
        // Install a restrictive SecurityManager for the duration of this test.
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything except suppressAccessChecks (used by setAccessible)
                if ("suppressAccessChecks".equals(perm.getName())) {
                    throw new SecurityException("Reflective access denied by test SecurityManager");
                }
            }
        });
        try {
            Method m = CompactCILinkedSet.class.getDeclaredMethod("isCaseInsensitive");
            // This should trigger the SecurityException defined above.
            assertThrows(SecurityException.class, m::setAccessible);
        } finally {
            // Restore original security manager to avoid side effects.
            System.setSecurityManager(original);
        }
    }

    // IllegalAccessException in the JSON description.
    @Test
    @Disabled("Demonstrates that direct compile‑time access is illegal; no runtime test needed.")
    void compileTimeIllegalAccess_isCaseInsensitive_notAllowed() {
        // The following line would not compile:
        // boolean illegal = set.isCaseInsensitive(); // illegal: protected access
    }
}
