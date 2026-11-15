package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_forName_Test {

    /**
     * Alias used in the alias‑related tests.
     */
    private static final String ALIAS = "mapski";

    /**
     * Fully‑qualified ClassUtilities_forName_Testof a known class used in the positive test.
     */
    private static final String VALID_CLASS_NAME = "java.util.Date";

    /**
     * Name that does not correspond to any loadable class.
     */
    private static final String UNKNOWN_CLASS_NAME = "com.this.does.not.Exist";

    /**
     * Custom class‑loader that pretends to know a single alternate name.
     */
    private static class AlternateNameClassLoader extends ClassLoader {

        private final String alternateName;

        private final Class<?> targetClass;

        AlternateNameClassLoader(String alternateName, Class<?> targetClass) {
            super(AlternateNameClassLoader.class.getClassLoader());
            this.alternateName = alternateName;
            this.targetClass = targetClass;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            // First delegate to the parent – this handles system classes.
            try {
                return super.loadClass(name);
            } catch (ClassNotFoundException ignored) {
                // fall through to our custom handling
            }
            if (alternateName.equals(name)) {
                return targetClass;
            }
            throw new ClassNotFoundException(name);
        }
    }

    /**
     * ClassLoader that throws a {@link SecurityException} for a specific class name.
     */
    private static class RestrictedClassLoader extends ClassLoader {

        private final String prohibitedName;

        RestrictedClassLoader(String prohibitedName) {
            super(RestrictedClassLoader.class.getClassLoader());
            this.prohibitedName = prohibitedName;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (prohibitedName.equals(name)) {
                throw new SecurityException("Access to " + name + " is restricted");
            }
            return super.loadClass(name);
        }
    }

    /**
     * ClassLoader that throws a generic unchecked exception for any load attempt.
     */
    private static class FaultyClassLoader extends ClassLoader {

        FaultyClassLoader() {
            super(FaultyClassLoader.class.getClassLoader());
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new RuntimeException("Unexpected failure while loading " + name);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the alias map is empty before each test.
        ClassUtilities.removePermanentClassAlias(ALIAS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up any alias that might have been added during a test.
        ClassUtilities.removePermanentClassAlias(ALIAS);
    }

    @Test
    void testForName_NullOrEmptyName_ReturnsNull() {
        assertNull(ClassUtilities.forName(null, ClassUtilities.getClassLoader()), "null ClassUtilities_forName_Testshould return null");
        assertNull(ClassUtilities.forName("", ClassUtilities.getClassLoader()), "empty ClassUtilities_forName_Testshould return null");
        assertNull(ClassUtilities.forName("   ", ClassUtilities.getClassLoader()), "blank ClassUtilities_forName_Testshould return null");
    }

    @Test
    void testForName_ValidFullyQualifiedName_ReturnsClass() {
        Class<?> result = ClassUtilities.forName(VALID_CLASS_NAME, ClassUtilities.getClassLoader());
        assertNotNull(result, "Result should not be null for a valid class name");
        assertEquals(java.util.Date.class, result, "Returned class should match the requested type");
    }

    @Test
    void testForName_UnknownName_ReturnsNull() {
        Class<?> result = ClassUtilities.forName(UNKNOWN_CLASS_NAME, ClassUtilities.getClassLoader());
        assertNull(result, "Unknown class ClassUtilities_forName_Testshould result in null");
    }

    @Test
    void testForName_WithPermanentAlias() {
        // Register an alias that points to HashMap.class
        ClassUtilities.addPermanentClassAlias(java.util.HashMap.class, ALIAS);
        // Resolve via alias – should succeed
        Class<?> viaAlias = ClassUtilities.forName(ALIAS, ClassUtilities.getClassLoader());
        assertNotNull(viaAlias, "Alias should resolve to a class");
        assertEquals(java.util.HashMap.class, viaAlias, "Alias should resolve to the class it was bound to");
        // Remove the alias and verify that resolution fails
        ClassUtilities.removePermanentClassAlias(ALIAS);
        Class<?> afterRemoval = ClassUtilities.forName(ALIAS, ClassUtilities.getClassLoader());
        assertNull(afterRemoval, "Removed alias should no longer resolve");
    }

    @Test
    void testForName_WithCustomClassLoader_ResolvesAlternateName() {
        // The custom loader will resolve the ClassUtilities_forName_Test"my.long" to java.lang.Long.class
        String alternateName = "my.long";
        AlternateNameClassLoader loader = new AlternateNameClassLoader(alternateName, Long.class);
        Class<?> result = ClassUtilities.forName(alternateName, loader);
        assertNotNull(result, "Custom class loader should resolve the alternate name");
        assertEquals(Long.class, result, "The class returned should be the one supplied by the loader");
    }

    @Test
    void testForName_WithCustomClassLoader_UnknownNameReturnsNull() {
        AlternateNameClassLoader loader = new AlternateNameClassLoader("some.name", Long.class);
        Class<?> result = ClassUtilities.forName("com.unloadable.Class", loader);
        assertNull(result, "When the custom loader cannot find the class, forName should return null");
    }

    @Test
    void testForName_ValidClassNameWithCustomLoader_ReturnsClass() {
        // Mock a ClassLoader that knows how to load java.util.ArrayList
        ClassLoader mockLoader = new ClassLoader() {

            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if ("java.util.ArrayList".equals(name)) {
                    return java.util.ArrayList.class;
                }
                return super.loadClass(name);
            }
        };
        Class<?> result = ClassUtilities.forName("java.util.ArrayList", mockLoader);
        assertNotNull(result, "Custom loader should resolve the class");
        assertEquals(java.util.ArrayList.class, result, "Returned class should be the one supplied by the custom loader");
    }

    @Test
    void testForName_SecurityExceptionWrappedInIllegalArgumentException() {
        // The RestrictedClassLoader throws SecurityException for the given name.
        RestrictedClassLoader restrictedLoader = new RestrictedClassLoader("java.lang.System");
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.forName("java.lang.System", restrictedLoader), "SecurityException should be wrapped in IllegalArgumentException");
        assertTrue(thrown.getMessage().contains("Security exception"), "Exception message should explain the security problem");
        assertTrue(thrown.getCause() instanceof SecurityException, "Cause of the IllegalArgumentException should be SecurityException");
    }

    @Test
    void testForName_OtherExceptionReturnsNull() {
        // FaultyClassLoader throws a RuntimeException (any other checked/unchecked exception).
        FaultyClassLoader faultyLoader = new FaultyClassLoader();
        // The method is expected to catch the generic Exception and return null.
        Class<?> result = ClassUtilities.forName("java.lang.Thread", faultyLoader);
        assertNull(result, "When an unexpected exception occurs, forName should return null");
    }
}
