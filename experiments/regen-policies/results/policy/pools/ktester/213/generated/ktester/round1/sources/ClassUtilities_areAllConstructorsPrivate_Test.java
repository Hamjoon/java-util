package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_areAllConstructorsPrivate_Test {

    /**
     * All constructors are private – should return true.
     */
    private static final class AllPrivateConstructors {

        private AllPrivateConstructors() {
        }

        private AllPrivateConstructors(String s) {
        }
    }

    /**
     * At least one public constructor – should return false.
     */
    private static final class MixedConstructors {

        private MixedConstructors() {
        }

        public MixedConstructors(int i) {
        }
    }

    /**
     * No explicit constructor – compiler supplies a public default one.
     */
    private static final class ImplicitPublicConstructor {
        // empty
    }

    /**
     * Package‑private constructor – not private.
     */
    static final class PackagePrivateConstructor {

        PackagePrivateConstructor() {
        }
    }

    /**
     * Abstract class with a protected constructor – not private.
     */
    private abstract static class AbstractWithProtectedCtor {

        protected AbstractWithProtectedCtor() {
        }
    }

    /**
     * Primitive type – has no constructors, the loop is never entered,
     *        so the method returns true.
     */
    private static final class PrimitiveWrapper {
    }

    /**
     * Class with only private constructors (including a private no‑arg ctor).
     */
    private static final class PrivateOnlyClass {

        private PrivateOnlyClass() {
        }

        private PrivateOnlyClass(String s) {
        }
    }

    /**
     * Class with a public no‑arg constructor.
     */
    private static final class PublicConstructorClass {

        public PublicConstructorClass() {
        }
    }

    /**
     * Class with a protected constructor.
     */
    private static final class ProtectedConstructorClass {

        protected ProtectedConstructorClass() {
        }
    }

    /**
     * Class with a package‑private (default) constructor.
     */
    static final class PackagePrivateConstructorClass {

        PackagePrivateConstructorClass() {
        }
    }

    /**
     * Class with several constructors, one private and at least one public.
     */
    private static final class MixedConstructorsClass {

        private MixedConstructorsClass() {
        }

        public MixedConstructorsClass(int i) {
        }
    }

    /**
     * Abstract class whose only constructor is private.
     */
    private abstract static class AbstractPrivateConstructorClass {

        private AbstractPrivateConstructorClass() {
        }
    }

    /**
     * Empty interface – interfaces declare no constructors.
     */
    private interface EmptyInterface {
    }

    /**
     * Abstract class whose only constructor is protected.
     */
    private abstract static class AbstractProtectedConstructorClass {

        protected AbstractProtectedConstructorClass() {
        }
    }

    /**
     * Final class with a single private constructor.
     */
    private static final class FinalPrivateConstructorClass {

        private FinalPrivateConstructorClass() {
        }
    }

    /**
     * Class with multiple overloaded private constructors.
     */
    private static final class MultiplePrivateConstructorsClass {

        private MultiplePrivateConstructorsClass() {
        }

        private MultiplePrivateConstructorsClass(int i) {
        }

        private MultiplePrivateConstructorsClass(String s) {
        }
    }

    /**
     * Outer class containing a non‑static inner class (synthetic package‑private ctor).
     */
    private static class OuterClass {

        class InnerClass {
            /* implicit package‑private synthetic ctor */
        }
    }

    class InnerClass {
        /* implicit package‑private synthetic ctor */
    }

    class InnerClass {
        /* implicit package‑private synthetic ctor */
    }

    /* -----------------------------------------------------------------
       Parameter provider – maps each test case from the JSON description
       to a JUnit {@code Arguments} instance.
       ----------------------------------------------------------------- */
    private static Stream<Arguments> provideClasses() {
        return Stream.of(// Class defines only private constructors → true
        Arguments.of(PrivateOnlyClass.class, true, null), // Class has a public no‑arg constructor → false
        Arguments.of(PublicConstructorClass.class, false, null), // Class defines a protected constructor → false
        Arguments.of(ProtectedConstructorClass.class, false, null), // Class defines a package‑private constructor → false
        Arguments.of(PackagePrivateConstructorClass.class, false, null), // Mixed visibility constructors → false
        Arguments.of(MixedConstructorsClass.class, false, null), // Abstract class with only private constructor → true
        Arguments.of(AbstractPrivateConstructorClass.class, true, null), // Interface has no declared constructors → true
        Arguments.of(EmptyInterface.class, true, null), // Enum types have implicitly private constructors → true
        Arguments.of(SampleEnum.class, true, null), // java.lang.Object – no declared constructors (JVM supplies default) → true
        Arguments.of(Object.class, true, null), // Null argument → NullPointerException
        Arguments.of(null, null, NullPointerException.class), // Multiple private constructors → true
        Arguments.of(MultiplePrivateConstructorsClass.class, true, null), // Abstract class with protected constructor → false
        Arguments.of(AbstractProtectedConstructorClass.class, false, null), // Final class with private constructor → true
        Arguments.of(FinalPrivateConstructorClass.class, true, null), // Record type – implicit public canonical constructor → false
        Arguments.of(SampleRecord.class, false, null), // Primitive type int → true
        Arguments.of(int.class, true, null), // Array type String[] → true
        Arguments.of(String[].class, true, null), // Annotation type → true
        Arguments.of(SampleAnnotation.class, true, null), // Non‑static inner class – synthetic package‑private ctor → false
        Arguments.of(OuterClass.InnerClass.class, false, null));
    }

    @Test
    @DisplayName("All constructors are private → true")
    void testAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(AllPrivateConstructors.class), "All constructors are private, expected true");
    }

    @Test
    @DisplayName("Mixed visibility constructors → false")
    void testMixedConstructors() {
        assertFalse(ClassUtilities.areAllConstructorsPrivate(MixedConstructors.class), "Presence of a public constructor should make the result false");
    }

    @Test
    @DisplayName("Implicit public no‑arg constructor → false")
    void testImplicitPublicConstructor() {
        assertFalse(ClassUtilities.areAllConstructorsPrivate(ImplicitPublicConstructor.class), "Compiler‑generated public constructor should cause false");
    }

    @Test
    @DisplayName("Package‑private constructor → false")
    void testPackagePrivateConstructor() {
        assertFalse(ClassUtilities.areAllConstructorsPrivate(PackagePrivateConstructor.class), "Package‑private constructor is not private, expected false");
    }

    @Test
    @DisplayName("Abstract class with protected constructor → false")
    void testAbstractWithProtectedCtor() {
        assertFalse(ClassUtilities.areAllConstructorsPrivate(AbstractWithProtectedCtor.class), "Protected constructor is not private, expected false");
    }

    @Test
    @DisplayName("Primitive type (int) → true")
    void testPrimitiveType() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(int.class), "Primitive types have no constructors, method should return true");
    }

    @Test
    @DisplayName("Array type (String[]) → true")
    void testArrayType() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(String[].class), "Array classes have no constructors, method should return true");
    }

    /* -----------------------------------------------------------------
       Parameterized test that consumes the data from {@code provideClasses}.
       ----------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0} → expected: {1}")
    @MethodSource("provideClasses")
    void testAreAllConstructorsPrivate(Class<?> clazz, Boolean expectedResult, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            // Verify that the expected exception is thrown for null input.
            assertThrows(expectedException, () -> ClassUtilities.areAllConstructorsPrivate(clazz), "Expected exception " + expectedException.getSimpleName() + " for input: null");
        } else {
            boolean actual = ClassUtilities.areAllConstructorsPrivate(clazz);
            assertEquals(expectedResult, actual, () -> "Unexpected result for class " + (clazz != null ? clazz.getName() : "null"));
        }
    }
}
