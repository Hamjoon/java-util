package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Permission;
import java.util.stream.Stream;

class CompactMap_isCaseInsensitive_Test {

    /* -------------------------------------------------------------
     * 4. Mutable field based override (false → true)
     * ------------------------------------------------------------- */
    static class MutableCaseInsensitiveMap extends CompactMap<String, String> {

        private boolean caseInsensitive = false;

        void setCaseInsensitive(boolean v) {
            this.caseInsensitive = v;
        }

        @Override
        protected boolean isCaseInsensitive() {
            return caseInsensitive;
        }
    }

    /* -------------------------------------------------------------
     * Helper that obtains the Method object for the protected method.
     * ------------------------------------------------------------- */
    private static Method getIsCaseInsensitiveMethod() throws NoSuchMethodException {
        Method m = CompactMap.class.getDeclaredMethod("isCaseInsensitive");
        // callers decide whether to call setAccessible(true)
        return m;
    }

    static Stream<MutableCaseInsensitiveMap> mutableMapProvider() {
        return Stream.of(new MutableCaseInsensitiveMap());
    }

    @Test
    void defaultImplementationReturnsFalse() {
        // Given: a plain CompactMap instance
        CompactMap<String, String> map = new CompactMap<>();
        // When: we query its case‑insensitivity flag
        boolean result = map.isCaseInsensitive();
        // Then: the default implementation must return false
        assertFalse(result, "CompactMap.isCaseInsensitive() should return false by default");
    }

    @Test
    void overriddenImplementationReturnsTrue() {
        // Given: a subclass that overrides isCaseInsensitive() to return true
        CompactMap<String, String> caseInsensitiveMap = new CompactMap<String, String>() {

            @Override
            protected boolean isCaseInsensitive() {
                return true;
            }
        };
        // When: we query the overridden flag
        boolean result = caseInsensitiveMap.isCaseInsensitive();
        // Then: the overridden method should return true
        assertTrue(result, "Overridden CompactMap.isCaseInsensitive() should return true");
    }

    @Test
    void overriddenImplementationCanReturnFalseExplicitly() {
        // Given: a subclass that explicitly returns false (even though this matches the default)
        CompactMap<String, String> explicitFalseMap = new CompactMap<String, String>() {

            @Override
            protected boolean isCaseInsensitive() {
                return false;
            }
        };
        // When: we query the flag
        boolean result = explicitFalseMap.isCaseInsensitive();
        // Then: the method must return false
        assertFalse(result, "Overridden CompactMap.isCaseInsensitive() returning false should behave like the default");
    }

    /* -------------------------------------------------------------
     * 1b. Default implementation – reflective call
     * ------------------------------------------------------------- */
    @Test
    void defaultImplementationReturnsFalseViaReflection() throws Exception {
        CompactMap<String, String> map = new CompactMap<>();
        Method m = getIsCaseInsensitiveMethod();
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(map);
        assertFalse(result, "Reflective call should also return false for the default implementation");
    }

    @Test
    void overriddenImplementationReturnsTrueViaReflection() throws Exception {
        CompactMap<String, String> map = new CompactMap<String, String>() {

            @Override
            protected boolean isCaseInsensitive() {
                return true;
            }
        };
        Method m = getIsCaseInsensitiveMethod();
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(map);
        assertTrue(result, "Reflective call should return true for overridden implementation");
    }

    @Test
    void overriddenImplementationCanReturnFalseExplicitlyViaReflection() throws Exception {
        CompactMap<String, String> map = new CompactMap<String, String>() {

            @Override
            protected boolean isCaseInsensitive() {
                return false;
            }
        };
        Method m = getIsCaseInsensitiveMethod();
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(map);
        assertFalse(result, "Reflective call should return false for explicit false override");
    }

    @ParameterizedTest
    @MethodSource("mutableMapProvider")
    void mutableFieldInitiallyFalse(MutableCaseInsensitiveMap map) throws Exception {
        Method m = getIsCaseInsensitiveMethod();
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(map);
        assertFalse(result, "Initially the mutable field is false");
    }

    @ParameterizedTest
    @MethodSource("mutableMapProvider")
    void mutableFieldSetToTrue(MutableCaseInsensitiveMap map) throws Exception {
        map.setCaseInsensitive(true);
        Method m = getIsCaseInsensitiveMethod();
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(map);
        assertTrue(result, "After setting the field to true, the method should reflect the change");
    }

    /* -------------------------------------------------------------
     * 5. Overridden true regardless of map contents
     * ------------------------------------------------------------- */
    @Test
    void trueOverrideIgnoresMapContents() throws Exception {
        CompactMap<String, String> map = new CompactMap<String, String>() {

            @Override
            protected boolean isCaseInsensitive() {
                return true;
            }
        };
        // Populate with several entries
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");
        Method m = getIsCaseInsensitiveMethod();
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(map);
        assertTrue(result, "Method should still return true regardless of map entries");
    }

    /* -------------------------------------------------------------
     * 6. Anonymous subclass true override (already covered by #2)
     * ------------------------------------------------------------- */
    @Test
    void anonymousSubclassTrueOverrideViaReflection() throws Exception {
        CompactMap<String, String> map = new CompactMap<String, String>() {

            @Override
            protected boolean isCaseInsensitive() {
                return true;
            }
        };
        Method m = getIsCaseInsensitiveMethod();
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(map);
        assertTrue(result, "Anonymous subclass overriding to true must return true via reflection");
    }

    /* -------------------------------------------------------------
     * 7. Subclass throws RuntimeException
     * ------------------------------------------------------------- */
    @Test
    void subclassThrowsRuntimeExceptionViaReflection() throws Exception {
        CompactMap<String, String> map = new CompactMap<String, String>() {

            @Override
            protected boolean isCaseInsensitive() {
                throw new RuntimeException("intentional");
            }
        };
        Method m = getIsCaseInsensitiveMethod();
        m.setAccessible(true);
        InvocationTargetException ite = assertThrows(InvocationTargetException.class, () -> m.invoke(map), "Invocation should wrap the RuntimeException in InvocationTargetException");
        assertTrue(ite.getCause() instanceof RuntimeException);
        assertEquals("intentional", ite.getCause().getMessage());
    }

    /* -------------------------------------------------------------
     * 8. Reflection without setAccessible – expect IllegalAccessException
     * ------------------------------------------------------------- */
    @Test
    void reflectionWithoutSetAccessibleThrowsIllegalAccessException() throws Exception {
        CompactMap<String, String> map = new CompactMap<>();
        Method m = getIsCaseInsensitiveMethod();
        // Do NOT call setAccessible(true)
        assertThrows(IllegalAccessException.class, () -> m.invoke(map), "Calling a protected method via reflection without setAccessible should fail");
    }

    /* -------------------------------------------------------------
     * 9. Null instance invocation – expect NullPointerException
     * ------------------------------------------------------------- */
    @Test
    void nullInstanceInvocationThrowsNullPointerException() throws Exception {
        Method m = getIsCaseInsensitiveMethod();
        m.setAccessible(true);
        assertThrows(NullPointerException.class, () -> m.invoke(null), "Invoking a non‑static method with a null target must throw NullPointerException");
    }

    /* -------------------------------------------------------------
     * 10. Subclass throws IllegalStateException (unchecked)
     * ------------------------------------------------------------- */
    @Test
    void subclassThrowsIllegalStateExceptionViaReflection() throws Exception {
        CompactMap<String, String> map = new CompactMap<String, String>() {

            @Override
            protected boolean isCaseInsensitive() {
                throw new IllegalStateException("bad state");
            }
        };
        Method m = getIsCaseInsensitiveMethod();
        m.setAccessible(true);
        InvocationTargetException ite = assertThrows(InvocationTargetException.class, () -> m.invoke(map), "Invocation should wrap IllegalStateException");
        assertTrue(ite.getCause() instanceof IllegalStateException);
        assertEquals("bad state", ite.getCause().getMessage());
    }

    /* -------------------------------------------------------------
     * 11. SecurityManager blocks reflective access
     * ------------------------------------------------------------- */
    @Test
    @Disabled("SecurityManager is deprecated and may be disabled in the test runtime environment")
    void securityManagerBlocksReflectiveAccess() throws Exception {
        // Preserve the original security manager
        SecurityManager original = System.getSecurityManager();
        // Install a restrictive security manager
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything except suppressAccessChecks
                if ("suppressAccessChecks".equals(perm.getName())) {
                    throw new SecurityException("Reflective access suppressed by test SecurityManager");
                }
            }
        });
        try {
            CompactMap<String, String> map = new CompactMap<>();
            Method m = getIsCaseInsensitiveMethod();
            // The following line should trigger a SecurityException when setAccessible is called
            assertThrows(SecurityException.class, () -> m.setAccessible(true), "SecurityManager should prevent suppressAccessChecks");
        } finally {
            // Restore original security manager to avoid side effects
            System.setSecurityManager(original);
        }
    }
}
