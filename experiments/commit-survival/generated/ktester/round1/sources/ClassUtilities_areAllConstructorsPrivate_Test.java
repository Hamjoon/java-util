package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_areAllConstructorsPrivate_Test {

    /**
     * Class that defines only private constructors.
     */
    static class PrivateCtorClass {

        private PrivateCtorClass() {
        }

        private PrivateCtorClass(int x) {
        }
    }

    /**
     * Class that defines a public no‑arg constructor.
     */
    static class PublicCtorClass {

        public PublicCtorClass() {
        }
    }

    /**
     * Class that defines both private and public constructors.
     */
    static class MixedCtorClass {

        private MixedCtorClass() {
        }

        public MixedCtorClass(String s) {
        }
    }

    /**
     * Interface – has no constructors at all.
     */
    interface TestInterface {
    }

    abstract class AbstractProtectedCtor {

        protected AbstractProtectedCtor() {
        }
    }

    class PackagePrivateCtor {

        PackagePrivateCtor() {
        }
    }

    /**
     * Container for all helper types referenced from the JSON test cases.
     * The nested classes are deliberately simple – they only exist to provide
     * the required constructor visibility for the tests.
     */
    public static class ClassUtilitiesTestHelper {

        /**
         * All constructors are private.
         */
        public static class AllPrivate {

            private AllPrivate() {
            }

            private AllPrivate(int i) {
            }
        }

        /**
         * Mixed visibility: one private, one public.
         */
        public static class MixedVisibility {

            private MixedVisibility() {
            }

            public MixedVisibility(String s) {
            }
        }

        /**
         * No explicit constructors – the compiler supplies a public no‑arg ctor.
         */
        public static class NoConstructor {
        }

        /**
         * Sample enum – compiler generates private constructors.
         */
        public enum SampleEnum {

            ONE, TWO
        }

        /**
         * Package‑private (default) visibility constructor.
         */
        public static class PackagePrivateCtor {

            PackagePrivateCtor() {
            }
        }

        /**
         * Protected constructor.
         */
        public static class ProtectedCtor {

            protected ProtectedCtor() {
            }
        }

        /**
         * Abstract class with only private constructors.
         */
        public abstract static class AbstractAllPrivate {

            private AbstractAllPrivate() {
            }
        }

        /**
         * Multiple private constructors (overloaded).
         */
        public static class MultiPrivateConstructors {

            private MultiPrivateConstructors() {
            }

            private MultiPrivateConstructors(int i) {
            }

            private MultiPrivateConstructors(String s, double d) {
            }
        }

        /**
         * All constructors are public.
         */
        public static class AllPublic {

            public AllPublic() {
            }

            public AllPublic(int i) {
            }
        }

        /**
         * Private and protected constructors.
         */
        public static class PrivateAndProtected {

            private PrivateAndProtected() {
            }

            protected PrivateAndProtected(int i) {
            }
        }

        /**
         * Simple interface – has no constructors.
         */
        public interface SampleInterface {
        }

        /**
         * Annotation type – also has no constructors.
         */
        @Retention(RetentionPolicy.RUNTIME)
        public @interface SampleAnnotation {
        }
    }

    /**
     * All constructors are private.
     */
    public static class AllPrivate {

        private AllPrivate() {
        }

        private AllPrivate(int i) {
        }
    }

    /**
     * Mixed visibility: one private, one public.
     */
    public static class MixedVisibility {

        private MixedVisibility() {
        }

        public MixedVisibility(String s) {
        }
    }

    /**
     * No explicit constructors – the compiler supplies a public no‑arg ctor.
     */
    public static class NoConstructor {
    }

    /**
     * Package‑private (default) visibility constructor.
     */
    public static class PackagePrivateCtor {

        PackagePrivateCtor() {
        }
    }

    /**
     * Protected constructor.
     */
    public static class ProtectedCtor {

        protected ProtectedCtor() {
        }
    }

    /**
     * Abstract class with only private constructors.
     */
    public abstract static class AbstractAllPrivate {

        private AbstractAllPrivate() {
        }
    }

    /**
     * Multiple private constructors (overloaded).
     */
    public static class MultiPrivateConstructors {

        private MultiPrivateConstructors() {
        }

        private MultiPrivateConstructors(int i) {
        }

        private MultiPrivateConstructors(String s, double d) {
        }
    }

    /**
     * All constructors are public.
     */
    public static class AllPublic {

        public AllPublic() {
        }

        public AllPublic(int i) {
        }
    }

    /**
     * Private and protected constructors.
     */
    public static class PrivateAndProtected {

        private PrivateAndProtected() {
        }

        protected PrivateAndProtected(int i) {
        }
    }

    /**
     * Simple interface – has no constructors.
     */
    public interface SampleInterface {
    }

    /**
     * All constructors are private.
     */
    public static class AllPrivate {

        private AllPrivate() {
        }

        private AllPrivate(int i) {
        }
    }

    /**
     * Mixed visibility: one private, one public.
     */
    public static class MixedVisibility {

        private MixedVisibility() {
        }

        public MixedVisibility(String s) {
        }
    }

    /**
     * No explicit constructors – the compiler supplies a public no‑arg ctor.
     */
    public static class NoConstructor {
    }

    /**
     * Package‑private (default) visibility constructor.
     */
    public static class PackagePrivateCtor {

        PackagePrivateCtor() {
        }
    }

    /**
     * Protected constructor.
     */
    public static class ProtectedCtor {

        protected ProtectedCtor() {
        }
    }

    /**
     * Abstract class with only private constructors.
     */
    public abstract static class AbstractAllPrivate {

        private AbstractAllPrivate() {
        }
    }

    /**
     * Multiple private constructors (overloaded).
     */
    public static class MultiPrivateConstructors {

        private MultiPrivateConstructors() {
        }

        private MultiPrivateConstructors(int i) {
        }

        private MultiPrivateConstructors(String s, double d) {
        }
    }

    /**
     * All constructors are public.
     */
    public static class AllPublic {

        public AllPublic() {
        }

        public AllPublic(int i) {
        }
    }

    /**
     * Private and protected constructors.
     */
    public static class PrivateAndProtected {

        private PrivateAndProtected() {
        }

        protected PrivateAndProtected(int i) {
        }
    }

    /**
     * Simple interface – has no constructors.
     */
    public interface SampleInterface {
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset any custom SecurityManager that a test may have installed
        if (System.getSecurityManager() != null) {
            System.setSecurityManager(null);
        }
    }

    /**
     * Supplies the (Class, expectedResult) pairs for the majority of test cases.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> booleanCases() {
        return Stream.of(// All constructors private → true
        org.junit.jupiter.params.provider.Arguments.of(ClassUtilitiesTestHelper.AllPrivate.class, true, "All constructors are private"), // Mixed visibility → false
        org.junit.jupiter.params.provider.Arguments.of(ClassUtilitiesTestHelper.MixedVisibility.class, false, "Mixed private/public constructors"), // Implicit public default constructor → false
        org.junit.jupiter.params.provider.Arguments.of(ClassUtilitiesTestHelper.NoConstructor.class, false, "No explicit constructors (public default)"), // Enum (private synthetic constructors) → true
        org.junit.jupiter.params.provider.Arguments.of(ClassUtilitiesTestHelper.SampleEnum.class, true, "Enum constructors are implicitly private"), // Package‑private constructor → false
        org.junit.jupiter.params.provider.Arguments.of(ClassUtilitiesTestHelper.PackagePrivateCtor.class, false, "Package‑private constructor is not private"), // Protected constructor → false
        org.junit.jupiter.params.provider.Arguments.of(ClassUtilitiesTestHelper.ProtectedCtor.class, false, "Protected constructor is not private"), // Abstract class with only private constructors → true
        org.junit.jupiter.params.provider.Arguments.of(ClassUtilitiesTestHelper.AbstractAllPrivate.class, true, "Abstract class with private constructors"), // Multiple private constructors → true
        org.junit.jupiter.params.provider.Arguments.of(ClassUtilitiesTestHelper.MultiPrivateConstructors.class, true, "All overloaded constructors are private"), // All public constructors → false
        org.junit.jupiter.params.provider.Arguments.of(ClassUtilitiesTestHelper.AllPublic.class, false, "All constructors are public"), // Private + protected → false
        org.junit.jupiter.params.provider.Arguments.of(ClassUtilitiesTestHelper.PrivateAndProtected.class, false, "Private and protected constructors mixed"), // Interface (no constructors) → true
        org.junit.jupiter.params.provider.Arguments.of(ClassUtilitiesTestHelper.SampleInterface.class, true, "Interface has no constructors"), // Annotation (no constructors) → true
        org.junit.jupiter.params.provider.Arguments.of(ClassUtilitiesTestHelper.SampleAnnotation.class, true, "Annotation type has no constructors"), // Primitive type → true (empty constructor array)
        org.junit.jupiter.params.provider.Arguments.of(int.class, true, "Primitive type class"), // Array type → true (empty constructor array)
        org.junit.jupiter.params.provider.Arguments.of(String[].class, true, "Array class"));
    }

    @Test
    void allConstructorsPrivate_ShouldReturnTrue() {
        // GIVEN a class whose every declared constructor is private
        // WHEN checking the constructors
        boolean result = ClassUtilities.areAllConstructorsPrivate(PrivateCtorClass.class);
        // THEN the method reports true
        assertTrue(result, "All constructors are private, expected true");
    }

    @Test
    void publicConstructor_ShouldReturnFalse() {
        // GIVEN a class with a public constructor
        boolean result = ClassUtilities.areAllConstructorsPrivate(ClassUtilitiesTestHelper.AllPublic.class);
        // THEN the method reports false
        assertFalse(result, "Public constructor present, expected false");
    }

    @Test
    void mixedConstructors_ShouldReturnFalse() {
        // GIVEN a class that has both private and public constructors
        boolean result = ClassUtilities.areAllConstructorsPrivate(ClassUtilitiesTestHelper.MixedVisibility.class);
        // THEN the method reports false because not all are private
        assertFalse(result, "Mixed visibility constructors, expected false");
    }

    @Test
    void interfaceHasNoConstructors_ShouldReturnTrue() {
        // GIVEN an interface (no declared constructors)
        boolean result = ClassUtilities.areAllConstructorsPrivate(ClassUtilitiesTestHelper.SampleInterface.class);
        // THEN the method returns true (empty set satisfies the condition)
        assertTrue(result, "Interface has no constructors, should be considered true");
    }

    @Test
    void enumConstructorsArePrivate_ShouldReturnTrue() {
        // GIVEN an enum type (compiler‑generated private constructors)
        boolean result = ClassUtilities.areAllConstructorsPrivate(ClassUtilitiesTestHelper.SampleEnum.class);
        // THEN the method returns true
        assertTrue(result, "Enum constructors are implicitly private, expected true");
    }

    @Test
    void abstractClassWithProtectedConstructor_ShouldReturnFalse() {
        // GIVEN an abstract class with a protected constructor
        abstract class AbstractProtectedCtor {

            protected AbstractProtectedCtor() {
            }
        }
        boolean result = ClassUtilities.areAllConstructorsPrivate(AbstractProtectedCtor.class);
        assertFalse(result, "Protected constructor is not private, expected false");
    }

    @Test
    void classWithPackagePrivateConstructor_ShouldReturnFalse() {
        // GIVEN a class with package‑private (default) visibility constructor
        class PackagePrivateCtor {

            PackagePrivateCtor() {
            }
        }
        boolean result = ClassUtilities.areAllConstructorsPrivate(PackagePrivateCtor.class);
        assertFalse(result, "Package‑private constructor is not private, expected false");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("booleanCases")
    void areAllConstructorsPrivate_VariousScenarios_ReturnsExpected(Class<?> clazz, boolean expected, String description) {
        // WHEN
        boolean result = ClassUtilities.areAllConstructorsPrivate(clazz);
        // THEN
        assertEquals(expected, result, () -> "Expected " + expected + " for class " + clazz.getName() + " (" + description + ")");
    }

    @Test
    void nullArgument_ShouldThrowNullPointerException() {
        // GIVEN a null reference
        // WHEN/THEN the method must throw NPE
        assertThrows(NullPointerException.class, () -> ClassUtilities.areAllConstructorsPrivate(null), "Passing null should result in NullPointerException");
    }

    @Test
    void securityManagerDeniesReflectiveAccess_ShouldThrowSecurityException() {
        // Install a SecurityManager that blocks reflective access
        SecurityManager denyingManager = new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Disallow any ReflectPermission which is required for getDeclaredConstructors()
                if (perm instanceof java.lang.reflect.ReflectPermission) {
                    throw new SecurityException("Reflective access denied by test SecurityManager");
                }
                // Allow everything else
            }
        };
        System.setSecurityManager(denyingManager);
        try {
            // The call should now propagate a SecurityException
            assertThrows(SecurityException.class, () -> ClassUtilities.areAllConstructorsPrivate(ClassUtilitiesTestHelper.AllPrivate.class), "Expected SecurityException when reflective access is denied");
        } finally {
            // Ensure the custom SecurityManager does not affect other tests
            System.setSecurityManager(null);
        }
    }
}
