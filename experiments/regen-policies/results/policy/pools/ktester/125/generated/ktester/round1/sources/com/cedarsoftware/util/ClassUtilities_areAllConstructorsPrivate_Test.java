import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_areAllConstructorsPrivate_Test {

    /**
     * Interface – has no constructors at all.
     */
    interface InterfaceOnly {
    }

    /**
     * A class whose only constructor is private.
     */
    private static final class AllPrivate {

        private AllPrivate() {
        }
    }

    /**
     * A class with a mixture of private and public constructors.
     */
    private static final class Mixed {

        private Mixed() {
        }

        public Mixed(String s) {
        }
    }

    // intentionally empty – compiler will generate a public default constructor
    private static final class NoConstructor {
    }

    /**
     * A class with a single private no‑arg constructor.
     */
    private static final class PrivateNoArg {

        private PrivateNoArg() {
        }
    }

    // no explicit constructor – compiler generates a protected one
    private abstract static class AbstractWithDefaultCtor {
    }

    /**
     * Container for all test‑specific classes.
     */
    static class ClassUtilitiesTestClasses {

        /**
         * All declared constructors are private.
         */
        static final class AllPrivateConstructors {

            private AllPrivateConstructors() {
            }

            private AllPrivateConstructors(int i) {
            }
        }

        /**
         * Mixed private and public constructors.
         */
        static final class MixedConstructors {

            private MixedConstructors() {
            }

            public MixedConstructors(String s) {
            }
        }

        /**
         * No explicit constructors – compiler supplies a public no‑arg ctor.
         */
        static final class NoDeclaredConstructors {
        }

        /**
         * Package‑private (default) visibility constructor.
         */
        static final class PackagePrivateConstructor {

            PackagePrivateConstructor() {
            }
        }

        /**
         * Interface – has no constructors at all.
         */
        interface InterfaceOnly {
        }

        /**
         * Abstract class with a single private constructor.
         */
        abstract static class AbstractAllPrivate {

            private AbstractAllPrivate() {
            }
        }

        /**
         * Enum – constructors are implicitly private.
         */
        enum EnumWithImplicitPrivate {

            ONE, TWO
        }

        /**
         * Class with a protected constructor.
         */
        static final class ProtectedConstructor {

            protected ProtectedConstructor() {
            }
        }

        /**
         * Abstract class with a protected constructor.
         */
        abstract static class AbstractProtectedConstructor {

            protected AbstractProtectedConstructor() {
            }
        }

        /**
         * Abstract class with no declared constructors – gets a public default ctor.
         */
        abstract static class AbstractNoDeclaredConstructors {
        }

        /**
         * Simple Java record – has an implicit public canonical constructor.
         */
        record SimpleRecord(String value) {
        }
    }

    /**
     * All declared constructors are private.
     */
    static final class AllPrivateConstructors {

        private AllPrivateConstructors() {
        }

        private AllPrivateConstructors(int i) {
        }
    }

    /**
     * Mixed private and public constructors.
     */
    static final class MixedConstructors {

        private MixedConstructors() {
        }

        public MixedConstructors(String s) {
        }
    }

    /**
     * No explicit constructors – compiler supplies a public no‑arg ctor.
     */
    static final class NoDeclaredConstructors {
    }

    /**
     * Package‑private (default) visibility constructor.
     */
    static final class PackagePrivateConstructor {

        PackagePrivateConstructor() {
        }
    }

    /**
     * Interface – has no constructors at all.
     */
//     interface InterfaceOnly {
    }

    /**
     * Abstract class with a single private constructor.
     */
    abstract static class AbstractAllPrivate {

        private AbstractAllPrivate() {
        }
    }

    /**
     * Class with a protected constructor.
     */
    static final class ProtectedConstructor {

        protected ProtectedConstructor() {
        }
    }

    /**
     * Abstract class with a protected constructor.
     */
    abstract static class AbstractProtectedConstructor {

        protected AbstractProtectedConstructor() {
        }
    }

    /**
     * Abstract class with no declared constructors – gets a public default ctor.
     */
    abstract static class AbstractNoDeclaredConstructors {
    }

    /**
     * All declared constructors are private.
     */
//     static final class AllPrivateConstructors {

        private AllPrivateConstructors() {
        }

        private AllPrivateConstructors(int i) {
        }
    }

    /**
     * Mixed private and public constructors.
     */
//     static final class MixedConstructors {

        private MixedConstructors() {
        }

        public MixedConstructors(String s) {
        }
    }

    /**
     * No explicit constructors – compiler supplies a public no‑arg ctor.
     */
//     static final class NoDeclaredConstructors {
    }

    /**
     * Package‑private (default) visibility constructor.
     */
//     static final class PackagePrivateConstructor {

        PackagePrivateConstructor() {
        }
    }

    /**
     * Interface – has no constructors at all.
     */
//     interface InterfaceOnly {
    }

    /**
     * Abstract class with a single private constructor.
     */
//     abstract static class AbstractAllPrivate {

        private AbstractAllPrivate() {
        }
    }

    /**
     * Class with a protected constructor.
     */
//     static final class ProtectedConstructor {

        protected ProtectedConstructor() {
        }
    }

    /**
     * Abstract class with a protected constructor.
     */
//     abstract static class AbstractProtectedConstructor {

        protected AbstractProtectedConstructor() {
        }
    }

    /**
     * Abstract class with no declared constructors – gets a public default ctor.
     */
//     abstract static class AbstractNoDeclaredConstructors {
    }

    /**
     * Supplies the arguments for {@link #testAreAllConstructorsPrivate}.
     */
    private static Stream<Arguments> provideClassesForAllConstructorsPrivate() {
        return Stream.of(Arguments.of(ClassUtilitiesTestClasses.AllPrivateConstructors.class, true, "All constructors are private"), Arguments.of(ClassUtilitiesTestClasses.MixedConstructors.class, false, "Mixed private/public constructors"), Arguments.of(ClassUtilitiesTestClasses.NoDeclaredConstructors.class, false, "No explicit constructors, compiler adds public default"), Arguments.of(ClassUtilitiesTestClasses.PackagePrivateConstructor.class, false, "Package‑private (default) constructor is not private"), Arguments.of(ClassUtilitiesTestClasses.InterfaceOnly.class, true, "Interface has no constructors"), Arguments.of(ClassUtilitiesTestClasses.AbstractAllPrivate.class, true, "Abstract class with only a private constructor"), Arguments.of(ClassUtilitiesTestClasses.EnumWithImplicitPrivate.class, true, "Enum constructors are implicitly private"), Arguments.of(int.class, true, "Primitive type has no declared constructors"), Arguments.of(String[].class, true, "Array class has no declared constructors"), Arguments.of(ClassUtilitiesTestClasses.ProtectedConstructor.class, false, "Class with only a protected constructor"), Arguments.of(ClassUtilitiesTestClasses.AbstractProtectedConstructor.class, false, "Abstract class with protected constructor"), Arguments.of(ClassUtilitiesTestClasses.AbstractNoDeclaredConstructors.class, false, "Abstract class gets public default constructor"), Arguments.of(ClassUtilitiesTestClasses.SimpleRecord.class, false, "Record has an implicit public canonical constructor"));
    }

//     @Test
//     @DisplayName("All constructors are private → true")
//     void testAllConstructorsPrivate() {
//         Assertions.assertTrue(ClassUtilities.areAllConstructorsPrivate(AllPrivate.class), "AllPrivate should have only private constructors");
//         Assertions.assertTrue(ClassUtilities.areAllConstructorsPrivate(PrivateNoArg.class), "PrivateNoArg should have only a private constructor");
//         Assertions.assertTrue(ClassUtilities.areAllConstructorsPrivate(TestEnum.class), "Enum constructors are implicitly private");
//     }

//     @Test
//     @DisplayName("Mixed visibility constructors → false")
//     void testMixedConstructors() {
//         Assertions.assertFalse(ClassUtilities.areAllConstructorsPrivate(Mixed.class), "Mixed contains a public constructor, so result must be false");
//     }

//     @Test
//     @DisplayName("Class with no explicit constructor → false")
//     void testNoExplicitConstructor() {
//         Assertions.assertFalse(ClassUtilities.areAllConstructorsPrivate(NoConstructor.class), "NoConstructor gets a public default constructor, so result must be false");
//     }

//     @Test
//     @DisplayName("Abstract class with default (protected) constructor → false")
//     void testAbstractClassDefaultConstructor() {
//         Assertions.assertFalse(ClassUtilities.areAllConstructorsPrivate(AbstractWithDefaultCtor.class), "Abstract class default constructor is protected, not private");
//     }

//     @Test
//     @DisplayName("Null argument throws NullPointerException")
//     void testNullArgument() {
//         Assertions.assertThrows(NullPointerException.class, () -> ClassUtilities.areAllConstructorsPrivate(null), "Passing null should result in NullPointerException");
//     }

    /* --------------------------------------------------------------------- */
//     @ParameterizedTest(name = "{index}: {2}")
//     @MethodSource("provideClassesForAllConstructorsPrivate")
//     void testAreAllConstructorsPrivate(Class<?> clazz, Boolean expectedResult, String description) {
//         Assertions.assertEquals(expectedResult, ClassUtilities.areAllConstructorsPrivate(clazz), description);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     @DisplayName("java.lang.String may trigger InaccessibleObjectException under JPMS")
//     void testJdkClassPotentialInaccessibleObjectException() {
//         try {
//             // In a fully opened module this will simply return false (public constructor)
//             boolean result = ClassUtilities.areAllConstructorsPrivate(String.class);
//             // The method should return false because String has a public constructor.
//             Assertions.assertFalse(result, "String has a public constructor, expected false");
//         } catch (InaccessibleObjectException e) {
//             // If the runtime environment disallows reflective access, the exception is
//             // propagated as documented in the original test case.
//             Assertions.assertTrue(e instanceof InaccessibleObjectException, "Expected InaccessibleObjectException when accessing java.lang.String");
//         }
//     }
}