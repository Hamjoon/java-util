package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactLinkedMap_useCopyIterator_Test {

    private TestableCompactLinkedMap<String, Integer> mapUnderTest;

    // for the SecurityException test
    private SecurityManager originalSecurityManager;

    /**
     * Simple concrete subclass that exposes the protected method for testing.
     */
    private static class TestableCompactLinkedMap<K, V> extends CompactLinkedMap<K, V> {

        /**
         * Exposes the protected {@code useCopyIterator()} as a public method.
         */
        public boolean useCopyIteratorPublic() {
            return super.useCopyIterator();
        }
    }

    /**
     * Subclass that overrides the method to return {@code true}.
     */
    private static class OverriddenTrueMap<K, V> extends CompactLinkedMap<K, V> {

        @Override
        protected boolean useCopyIterator() {
            return true;
        }
    }

    /**
     * Subclass that overrides the method to throw a {@link RuntimeException}.
     */
    private static class OverriddenExceptionMap<K, V> extends CompactLinkedMap<K, V> {

        @Override
        protected boolean useCopyIterator() {
            throw new RuntimeException("forced exception");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        mapUnderTest = new TestableCompactLinkedMap<>();
        originalSecurityManager = System.getSecurityManager();
    }

    @AfterEach
    void teardownAfterEach() {
        mapUnderTest = null;
        // Restore any SecurityManager that might have been installed.
        System.setSecurityManager(originalSecurityManager);
    }

    private static Stream<Class<?>> genericMapProvider() {
        return Stream.of(// raw type works for any generics; we just need the class object
        CompactLinkedMap.class);
    }

    @Test
    void useCopyIterator_returnsFalse() {
        // GIVEN a newly created CompactLinkedMap (via the testable subclass)
        // WHEN the protected method is invoked through the public wrapper
        boolean result = mapUnderTest.useCopyIteratorPublic();
        // THEN it should return false as per the default implementation
        assertFalse(result, "useCopyIterator() should return false by default");
    }

    @Test
    void useCopyIterator_reflectionAccess_returnsFalse() throws Exception {
        Method m = CompactLinkedMap.class.getDeclaredMethod("useCopyIterator");
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(mapUnderTest);
        assertFalse(result, "Reflection call should return false");
    }

    @ParameterizedTest(name = "[{index}] Generic map {0}")
    @MethodSource("genericMapProvider")
    void useCopyIterator_reflectionWithDifferentGenerics_returnsFalse(Class<?> mapClass) throws Exception {
        // instantiate via reflection to avoid raw type warnings
        @SuppressWarnings("unchecked")
        CompactLinkedMap<?, ?> genericMap = (CompactLinkedMap<?, ?>) mapClass.getDeclaredConstructor().newInstance();
        Method m = CompactLinkedMap.class.getDeclaredMethod("useCopyIterator");
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(genericMap);
        assertFalse(result, "useCopyIterator() must stay false regardless of generic parameters");
    }

    @Test
    void useCopyIterator_subclassOverride_returnsTrue() {
        OverriddenTrueMap<Integer, String> overridden = new OverriddenTrueMap<>();
        // Use reflection to call the protected method (or simply call via subclass if accessible)
        Method m;
        try {
            m = CompactLinkedMap.class.getDeclaredMethod("useCopyIterator");
            m.setAccessible(true);
            boolean result = (boolean) m.invoke(overridden);
            assertTrue(result, "Overridden method should return true");
        } catch (Exception e) {
            fail("Unexpected exception while testing overridden method: " + e);
        }
    }

    @Test
    void useCopyIterator_subclassWithoutOverride_inheritsFalse() {
        // Use the same TestableCompactLinkedMap which does NOT override the method
        TestableCompactLinkedMap<String, String> subclass = new TestableCompactLinkedMap<>();
        assertFalse(subclass.useCopyIteratorPublic(), "Subclass without override should inherit false behavior");
    }

    @Test
    void useCopyIterator_reflection_nullInstance_throwsNullPointerException() throws Exception {
        Method m = CompactLinkedMap.class.getDeclaredMethod("useCopyIterator");
        m.setAccessible(true);
        assertThrows(NullPointerException.class, () -> m.invoke(null), "Invoking on a null reference must throw NullPointerException");
    }

    @Test
    void useCopyIterator_reflection_getMethod_throwsNoSuchMethodException() {
        assertThrows(NoSuchMethodException.class, () -> CompactLinkedMap.class.getMethod("useCopyIterator"), "Class.getMethod should not find a protected method and throw NoSuchMethodException");
    }

    @Test
    void useCopyIterator_reflection_withoutSetAccessible_throwsIllegalAccessException() throws Exception {
        Method m = CompactLinkedMap.class.getDeclaredMethod("useCopyIterator");
        // deliberately NOT calling setAccessible(true)
        assertThrows(IllegalAccessException.class, () -> m.invoke(mapUnderTest), "Invoking without setAccessible should throw IllegalAccessException");
    }

    @Test
    void useCopyIterator_reflection_extraArgument_throwsIllegalArgumentException() throws Exception {
        Method m = CompactLinkedMap.class.getDeclaredMethod("useCopyIterator");
        m.setAccessible(true);
        assertThrows(IllegalArgumentException.class, // method takes no args
        () -> m.invoke(mapUnderTest, true), "Providing extra arguments should cause IllegalArgumentException");
    }

    @Test
    void useCopyIterator_reflection_overriddenThrowsRuntimeException_wrappedInInvocationTargetException() throws Exception {
        OverriddenExceptionMap<String, String> overridden = new OverriddenExceptionMap<>();
        Method m = CompactLinkedMap.class.getDeclaredMethod("useCopyIterator");
        m.setAccessible(true);
        InvocationTargetException ite = assertThrows(InvocationTargetException.class, () -> m.invoke(overridden), "RuntimeException from overridden method must be wrapped in InvocationTargetException");
        assertTrue(ite.getCause() instanceof RuntimeException, "Cause of InvocationTargetException should be the RuntimeException thrown by the override");
        assertEquals("forced exception", ite.getCause().getMessage(), "Exception message should match the one thrown by the overridden method");
    }

    @Test
    void useCopyIterator_reflection_setAccessible_throwsSecurityException() throws Exception {
        // Install a SecurityManager that denies reflective access
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(java.security.Permission perm) {
                // Allow everything except suppressAccessChecks
                if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                    throw new SecurityException("Reflective access denied by test SecurityManager");
                }
            }
        });
        Method m = CompactLinkedMap.class.getDeclaredMethod("useCopyIterator");
        // The following call should now throw SecurityException
        assertThrows(SecurityException.class, m::setAccessible, "setAccessible should throw SecurityException when a SecurityManager denies it");
    }
}
