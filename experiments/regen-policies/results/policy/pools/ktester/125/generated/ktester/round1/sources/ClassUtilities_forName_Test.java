package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
// import org.junit.Test;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_forName_Test {

    /* -------------------------------------------------------------
     *  Helper classes and holder for test scenarios
     * ------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final String name;

        final ClassLoader loader;

        final Class<?> expectedClass;

        ArgumentsHolder(String name, ClassLoader loader, Class<?> expectedClass) {
            this.name = name;
            this.loader = loader;
            this.expectedClass = expectedClass;
        }

        @Override
        public String toString() {
            return "name='" + name + "', loader=" + (loader == null ? "null" : loader.getClass().getSimpleName()) + ", expected=" + (expectedClass == null ? "null" : expectedClass.getName());
        }
    }

    /**
     * Delegates to the parent loader (i.e., normal class‑loading behaviour).
     */
    private static class CustomDelegatingClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return super.loadClass(name);
        }
    }

    /**
     * Throws a generic {@link RuntimeException} when loadClass is invoked.
     */
    private static class RuntimeThrowingClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) {
            throw new RuntimeException("Runtime failure for " + name);
        }
    }

    /**
     * Throws a {@link ClassNotFoundException} for any requested class.
     */
    private static class ClassNotFoundThrowingClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException("Class not found: " + name);
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} from loadClass.
     */
    private static class IllegalArgumentThrowingClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) {
            throw new IllegalArgumentException("Bad argument for " + name);
        }
    }

    /**
     * Throws a {@link LinkageError} (e.g., NoClassDefFoundError).
     */
    private static class LinkageErrorThrowingClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) {
            throw new NoClassDefFoundError("Linkage error for " + name);
        }
    }

    /**
     * Throws a {@link NullPointerException} from loadClass.
     */
    private static class NullPointerThrowingClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) {
            throw new NullPointerException("NPE for " + name);
        }
    }

    /* -------------------------------------------------------------
     *  6. Additional scenarios – parameterised
     * ------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> additionalScenarioProvider() {
        List<ArgumentsHolder> list = new ArrayList<>();
        // Primitive name – ClassNotFoundException -> null
        list.add(new ArgumentsHolder("int", null, null));
        // Delegating loader – should succeed
        list.add(new ArgumentsHolder("java.lang.String", new CustomDelegatingClassLoader(), String.class));
        // RuntimeException from loader – generic catch returns null
        list.add(new ArgumentsHolder("java.lang.String", new RuntimeThrowingClassLoader(), null));
        // ClassNotFoundException from loader – generic catch returns null
        list.add(new ArgumentsHolder("com.example.MissingClass", new ClassNotFoundThrowingClassLoader(), null));
        // IllegalArgumentException from loader – generic catch returns null
        list.add(new ArgumentsHolder("java.lang.String", new IllegalArgumentThrowingClassLoader(), null));
        // NullPointerException from loader – generic catch returns null
        list.add(new ArgumentsHolder("java.lang.String", new NullPointerThrowingClassLoader(), null));
        return list.stream();
    }

    /* -------------------------------------------------------------
     *  7. Error propagation – LinkageError should NOT be caught
     * ------------------------------------------------------------- */
//     @Test
//     @DisplayName("forName propagates LinkageError thrown by the ClassLoader")
//     void testForName_LinkageError_Propagates() {
//         ClassLoader linkageErrorLoader = new LinkageErrorThrowingClassLoader();
//         // The method must propagate the LinkageError; the test passes if it is thrown.
//         ClassUtilities.forName("java.lang.String", linkageErrorLoader);
//     }
}