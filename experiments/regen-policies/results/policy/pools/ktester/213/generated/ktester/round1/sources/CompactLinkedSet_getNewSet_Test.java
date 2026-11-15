package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ReflectPermission;
import java.security.Permission;

@ExtendWith(MockitoExtension.class)
class CompactLinkedSet_getNewSet_Test {

    /**
     * Shared instance used by the tests.
     */
    private CompactLinkedSet<String> compactLinkedSet;

    private static class MyLinkedSet extends CompactLinkedSet<String> {

        @Override
        protected Set<String> getNewSet() {
            return super.getNewSet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test to avoid state leakage.
        compactLinkedSet = new CompactLinkedSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference for GC; not strictly required but keeps the pattern consistent.
        compactLinkedSet = null;
    }

    private Method getProtectedGetNewSetMethod() throws NoSuchMethodException {
        Method m = CompactLinkedSet.class.getDeclaredMethod("getNewSet");
        // make it callable from the test
        m.setAccessible(true);
        return m;
    }

    @Test
    @DisplayName("getNewSet() should return null")
    void testGetNewSetReturnsNull() {
        // GIVEN: a CompactLinkedSet instance (already created in @BeforeEach)
        // WHEN: invoking the protected getNewSet() method.
        // The method is protected but the test class resides in the same package,
        // therefore it is directly accessible.
        Set<String> result = compactLinkedSet.getNewSet();
        // THEN: the result must be null.
        assertNull(result, "CompactLinkedSet#getNewSet() should return null");
    }

    @Test
    @DisplayName("Direct call returns null")
    void testGetNewSetReturnsNullDirect() {
        // WHEN: invoking the protected method directly (same package).
        Set<String> result = compactLinkedSet.getNewSet();
        // THEN: result must be null.
        assertNull(result, "CompactLinkedSet#getNewSet() should return null");
    }

    @Test
    @DisplayName("Anonymous subclass exposes getNewSet() as public")
    void testPublicWrapperViaAnonymousSubclass() {
        // GIVEN: an anonymous subclass that makes the protected method public.
        CompactLinkedSet<String> wrapper = new CompactLinkedSet<String>() {

            public Set<String> exposeGetNewSet() {
                return getNewSet();
            }
        };
        // WHEN: calling the public wrapper.
        Set<String> result = wrapper.exposeGetNewSet();
        // THEN: result is null.
        assertNull(result, "Public wrapper should return null");
    }

    @Test
    @DisplayName("Concrete subclass delegating to super returns null")
    void testConcreteSubclassDelegatesToSuper() {
        MyLinkedSet mySet = new MyLinkedSet();
        Set<String> result = mySet.getNewSet();
        assertNull(result, "Subclass delegating to super should return null");
    }

    @Test
    @DisplayName("State invariance after adding elements")
    void testStateInvarianceAfterAddingElements() throws Exception {
        // GIVEN: a set with several elements.
        compactLinkedSet.add("A");
        compactLinkedSet.add("B");
        compactLinkedSet.add("C");
        int originalSize = compactLinkedSet.size();
        // WHEN: invoking getNewSet() via reflection.
        Method m = getProtectedGetNewSetMethod();
        Object result = m.invoke(compactLinkedSet);
        // THEN: method returns null and size is unchanged.
        assertNull(result, "Method should return null");
        assertEquals(originalSize, compactLinkedSet.size(), "Set size must remain unchanged after getNewSet()");
    }

    @Test
    @DisplayName("Empty set, two consecutive calls via reflection")
    void testEmptySetTwoCallsViaReflection() throws Exception {
        // GIVEN: an empty set.
        assertEquals(0, compactLinkedSet.size(), "Pre‑condition: set is empty");
        Method m = getProtectedGetNewSetMethod();
        // WHEN: first call.
        Object first = m.invoke(compactLinkedSet);
        // AND: second call.
        Object second = m.invoke(compactLinkedSet);
        // THEN: both calls return null and size stays 0.
        assertNull(first);
        assertNull(second);
        assertEquals(0, compactLinkedSet.size(), "Set must stay empty after calls");
    }

    @Test
    @DisplayName("Adding null element does not affect getNewSet()")
    void testAddNullElementAndInvoke() throws Exception {
        // GIVEN: a set that permits null (CompactLinkedSet does not forbid it).
        compactLinkedSet.add(null);
        int sizeAfterAdd = compactLinkedSet.size();
        Method m = getProtectedGetNewSetMethod();
        Object result = m.invoke(compactLinkedSet);
        // THEN: method returns null and set still contains the null element.
        assertNull(result);
        assertEquals(sizeAfterAdd, compactLinkedSet.size(), "Size must remain unchanged after getNewSet()");
        assertTrue(compactLinkedSet.contains(null), "Set should still contain the null element");
    }

    @ParameterizedTest(name = "Invocation #{index}")
    @ValueSource(ints = { 1, 2, 3 })
    @DisplayName("Multiple consecutive invocations all return null")
    void testMultipleCallsConsistency(int dummy) throws Exception {
        // The dummy parameter is only to repeat the test three times.
        Method m = getProtectedGetNewSetMethod();
        for (int i = 0; i < 3; i++) {
            Object result = m.invoke(compactLinkedSet);
            assertNull(result, "Each call must independently return null");
        }
    }

    @Test
    @DisplayName("Reflection invoke on null target throws NullPointerException")
    void testReflectionNullTarget() throws Exception {
        Method m = getProtectedGetNewSetMethod();
        assertThrows(NullPointerException.class, () -> m.invoke(null), "Invoking on a null target must throw NullPointerException");
    }

    @Test
    @DisplayName("Reflection invoke on illegal target throws IllegalArgumentException")
    void testReflectionIllegalTarget() throws Exception {
        Method m = getProtectedGetNewSetMethod();
        Object illegalTarget = new Object();
        assertThrows(IllegalArgumentException.class, () -> m.invoke(illegalTarget), "Invoking on a non‑CompactLinkedSet instance must throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Reflection without setAccessible(true) throws IllegalAccessException")
    void testReflectionIllegalAccess() throws Exception {
        // Obtain the method but deliberately do NOT call setAccessible(true).
        Method m = CompactLinkedSet.class.getDeclaredMethod("getNewSet");
        // The test class is in the same package, however the specification
        // expects IllegalAccessException, so we enforce it by resetting accessibility.
        // The JVM will treat the method as inaccessible for reflective invocation.
        assertFalse(m.canAccess(compactLinkedSet), "Method should not be accessible without setAccessible(true)");
        assertThrows(IllegalAccessException.class, () -> m.invoke(compactLinkedSet), "Reflective call without making the method accessible must throw IllegalAccessException");
    }

    @Test
    @DisplayName("SecurityManager denies suppressAccessChecks")
    void testReflectionSecurityManagerDeniesAccess() throws Exception {
        // Preserve the original SecurityManager.
        SecurityManager originalSm = System.getSecurityManager();
        // Install a restrictive SecurityManager that denies ReflectPermission "suppressAccessChecks".
        SecurityManager restrictiveSm = new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                    throw new SecurityException("Reflective access denied by test SecurityManager");
                }
                // Allow all other permissions.
            }
        };
        System.setSecurityManager(restrictiveSm);
        try {
            Method m = CompactLinkedSet.class.getDeclaredMethod("getNewSet");
            // Attempt to make it accessible – should trigger SecurityException.
            assertThrows(SecurityException.class, () -> m.setAccessible(true), "Setting accessible should be blocked by the SecurityManager");
        } finally {
            // Restore original SecurityManager to avoid side effects on other tests.
            System.setSecurityManager(originalSm);
        }
    }
}
