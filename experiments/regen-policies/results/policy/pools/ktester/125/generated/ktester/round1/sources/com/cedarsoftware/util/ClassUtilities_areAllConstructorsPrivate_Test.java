import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_areAllConstructorsPrivate_Test {

    /**
     * No constructors declared – the compiler provides a public no‑arg constructor.
     */
    static class ImplicitPublicConstructor {
    }

    /**
     * Explicit public constructor.
     */
    static class PublicConstructor {

        public PublicConstructor() {
        }
    }

    /**
     * Single private constructor.
     */
    static class SinglePrivateConstructor {

        private SinglePrivateConstructor() {
        }
    }

    /**
     * Multiple private constructors.
     */
    static class MultiplePrivateConstructors {

        private MultiplePrivateConstructors() {
        }

        private MultiplePrivateConstructors(int i) {
        }
    }

    /**
     * Mixed visibility constructors.
     */
    static class MixedConstructors {

        public MixedConstructors() {
        }

        private MixedConstructors(String s) {
        }
    }

    /**
     * All constructors are private (single).
     */
    static class AllPrivate {

        private AllPrivate() {
        }
    }

    /**
     * Multiple private constructors (different signatures).
     */
    static class MultipleAllPrivate {

        private MultipleAllPrivate() {
        }

        private MultipleAllPrivate(String s) {
        }

        private MultipleAllPrivate(int i, double d) {
        }
    }

    /**
     * Protected constructor – not private.
     */
    static class ProtectedConstructor {

        protected ProtectedConstructor() {
        }
    }

    /**
     * Package‑private (default) constructor – not private.
     */
    static class PackagePrivateConstructor {

        PackagePrivateConstructor() {
        }
    }

    /**
     * Interface – no constructors at all.
     */
    interface InterfaceOnly {
    }

    /**
     * Abstract class with a protected constructor.
     */
    static abstract class AbstractWithProtected {

        protected AbstractWithProtected() {
        }
    }

    /**
     * Abstract class with only private constructors.
     */
    static abstract class AbstractAllPrivate {

        private AbstractAllPrivate() {
        }
    }

    /**
     * Class with a private constructor and a static initializer block.
     */
    static class PrivateWithStaticBlock {

        static {
            // static initializer – does not affect constructor visibility
        }

        private PrivateWithStaticBlock() {
        }
    }

    /**
     * Outer class used for inner‑class tests.
     */
    static class Outer {

        /**
         * Private static inner class with a private constructor.
         */
        static class InnerPrivate {

            private InnerPrivate() {
            }
        }

        /**
         * Non‑static inner class – compiler generates a synthetic package‑private constructor.
         */
        class InnerBridge {
        }
    }

    /**
     * Private static inner class with a private constructor.
     */
    static class InnerPrivate {

        private InnerPrivate() {
        }
    }

    /**
     * Non‑static inner class – compiler generates a synthetic package‑private constructor.
     */
    class InnerBridge {
    }

    /**
     * Private static inner class with a private constructor.
     */
//     static class InnerPrivate {

        private InnerPrivate() {
        }
    }

    /**
     * Non‑static inner class – compiler generates a synthetic package‑private constructor.
     */
//     class InnerBridge {
    }

    /**
     * Class with only the compiler‑generated package‑private no‑arg constructor.
     */
    static class PackagePrivateNoArg {
    }

    /**
     * Class with a private constructor and a protected constructor.
     */
    static class MixedPrivateProtected {

        private MixedPrivateProtected() {
        }

        protected MixedPrivateProtected(int i) {
        }
    }

    /**
     * Private varargs constructor.
     */
    static class PrivateVarargs {

        private PrivateVarargs(String... values) {
        }
    }

    /**
     * Generic class with a private constructor.
     */
    static class GenericPrivate<T> {

        private GenericPrivate() {
        }
    }

    /**
     * Private constructor that declares a checked exception.
     */
    static class PrivateThrowsException {

        private PrivateThrowsException() throws Exception {
        }
    }

    /**
     * Helper interface used in the original test suite.
     */
    interface TestInterface {
    }

    static Stream<Arguments> allPrivateProvider() {
        return Stream.of(Arguments.of("All constructors private – single private ctor", ClassUtilities_areAllConstructorsPrivate_Test.AllPrivate.class, true), Arguments.of("All constructors private – multiple private ctors", ClassUtilities_areAllConstructorsPrivate_Test.MultipleAllPrivate.class, true));
    }

    static Stream<Arguments> nonPrivateProvider() {
        return Stream.of(Arguments.of("Explicit public constructor", ClassUtilities_areAllConstructorsPrivate_Test.PublicConstructor.class, false), Arguments.of("Protected constructor (non‑private)", ClassUtilities_areAllConstructorsPrivate_Test.ProtectedConstructor.class, false), Arguments.of("Package‑private constructor (default)", ClassUtilities_areAllConstructorsPrivate_Test.PackagePrivateConstructor.class, false), Arguments.of("Mixed constructors (public + private)", ClassUtilities_areAllConstructorsPrivate_Test.MixedConstructors.class, false), Arguments.of("Standard library class java.lang.String", String.class, false));
    }

    static Stream<Arguments> noConstructorProvider() {
        return Stream.of(Arguments.of("Interface – no constructors", ClassUtilities_areAllConstructorsPrivate_Test.InterfaceOnly.class, true), Arguments.of("Primitive type int.class", int.class, true), Arguments.of("Void class – no constructors", Void.class, true), Arguments.of("Array class String[] – no constructors", String[].class, true));
    }

//     @Test
//     @DisplayName("Class with only implicit public constructor → false")
//     void testImplicitPublicConstructor() {
//         assertFalse(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.ImplicitPublicConstructor.class), "Implicit public constructor should cause method to return false");
//     }

//     @Test
//     @DisplayName("Class with explicit public constructor → false")
//     void testExplicitPublicConstructor() {
//         assertFalse(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.PublicConstructor.class), "Explicit public constructor should cause method to return false");
//     }

//     @Test
//     @DisplayName("Class with a single private constructor → true")
//     void testSinglePrivateConstructor() {
//         assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.SinglePrivateConstructor.class), "All constructors are private, should return true");
//     }

//     @Test
//     @DisplayName("Class with multiple private constructors → true")
//     void testMultiplePrivateConstructors() {
//         assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.MultiplePrivateConstructors.class), "All constructors are private, should return true");
//     }

//     @Test
//     @DisplayName("Class with mixed visibility constructors → false")
//     void testMixedConstructors() {
//         assertFalse(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.MixedConstructors.class), "Presence of a public constructor should cause method to return false");
//     }

//     @Test
//     @DisplayName("Class with no declared constructors (interface) → true")
//     void testInterface() {
//         assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.TestInterface.class), "Interfaces have no constructors, the loop is empty → true");
//     }

//     @Test
//     @DisplayName("Null argument should throw NullPointerException")
//     void testNullArgument() {
//         assertThrows(NullPointerException.class, () -> ClassUtilities.areAllConstructorsPrivate(null), "Passing null should result in NPE from Class#getDeclaredConstructors");
//     }

    /**
     * Group: All constructors are private
     */
//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("allPrivateProvider")
//     void testAllConstructorsPrivate(String description, Class<?> clazz, boolean expected) {
//         assertEquals(expected, ClassUtilities.areAllConstructorsPrivate(clazz), description);
//     }

    /**
     * Group: At least one non‑private constructor
     */
//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("nonPrivateProvider")
//     void testAtLeastOneNonPrivate(String description, Class<?> clazz, boolean expected) {
//         assertEquals(expected, ClassUtilities.areAllConstructorsPrivate(clazz), description);
//     }

    /**
     * Group: Classes with no declared constructors
     */
//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("noConstructorProvider")
//     void testNoDeclaredConstructors(String description, Class<?> clazz, boolean expected) {
//         assertEquals(expected, ClassUtilities.areAllConstructorsPrivate(clazz), description);
//     }

    /**
     * Group: Abstract class with non‑private constructor
     */
//     @Test
//     @DisplayName("Abstract class with protected constructor → false")
//     void testAbstractWithProtected() {
//         assertFalse(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.AbstractWithProtected.class), "Abstract class with protected ctor should return false");
//     }

    /**
     * Group: Enum (synthetic private constructor)
     */
//     @Test
//     @DisplayName("Enum synthetic private constructor → true")
//     void testSyntheticEnum() {
//         assertTrue(ClassUtilities.areAllConstructorsPrivate(SyntheticEnum.class), "Enum constructors are private synthetic → true");
//     }

    /**
     * Group: Private constructor + static initializer
     */
//     @Test
//     @DisplayName("Class with private ctor and static block → true")
//     void testPrivateWithStaticBlock() {
//         assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.PrivateWithStaticBlock.class), "Static initializer does not affect constructor visibility");
//     }

    /**
     * Group: Inner class with private constructor
     */
//     @Test
//     @DisplayName("Static inner class with private ctor → true")
//     void testInnerPrivate() {
//         assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.Outer.InnerPrivate.class), "Static inner class with private ctor should return true");
//     }

    /**
     * Group: Compiler‑generated package‑private default ctor
     */
//     @Test
//     @DisplayName("Class with default package‑private ctor → false")
//     void testPackagePrivateNoArg() {
//         assertFalse(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.PackagePrivateNoArg.class), "Default package‑private ctor should cause false");
//     }

    /**
     * Group: Abstract class with only private constructors
     */
//     @Test
//     @DisplayName("Abstract class with only private ctors → true")
//     void testAbstractAllPrivate() {
//         assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.AbstractAllPrivate.class), "Abstract class with only private ctors should return true");
//     }

    /**
     * Group: Mixed private and protected constructors
     */
//     @Test
//     @DisplayName("Class with private and protected constructors → false")
//     void testMixedPrivateProtected() {
//         assertFalse(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.MixedPrivateProtected.class), "Presence of protected ctor forces false");
//     }

    /**
     * Group: Private varargs constructor
     */
//     @Test
//     @DisplayName("Class with private varargs constructor → true")
//     void testPrivateVarargs() {
//         assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.PrivateVarargs.class), "Private varargs ctor should be considered private");
//     }

    /**
     * Group: Generic class with private constructor
     */
//     @Test
//     @DisplayName("Generic class with private ctor → true")
//     void testGenericPrivate() {
//         assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.GenericPrivate.class), "Generic class private ctor should return true");
//     }

    /**
     * Group: Private constructor that throws a checked exception
     */
//     @Test
//     @DisplayName("Private ctor throwing checked exception → true")
//     void testPrivateThrowsException() {
//         assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.PrivateThrowsException.class), "Exception declaration does not affect visibility");
//     }

    /**
     * Group: Synthetic bridge constructor for non‑static inner class
     */
//     @Test
//     @DisplayName("Non‑static inner class synthetic bridge ctor → false")
//     void testInnerBridge() {
//         // The inner class is non‑static; the compiler creates a synthetic
//         // package‑private constructor that takes the outer instance.
//         assertFalse(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.Outer.InnerBridge.class), "Synthetic bridge ctor is package‑private → false");
//     }

    /**
     * Group: Security manager denies reflective access – simulated via Mockito
     */
//     @Test
//     @DisplayName("SecurityException propagates when reflective access is denied")
//     void testSecurityExceptionPropagation(@Mock Class<?> mockedClass) {
//         // Simulate SecurityException from getDeclaredConstructors()
//         when(mockedClass.getDeclaredConstructors()).thenThrow(new SecurityException("Access denied"));
//         SecurityException se = assertThrows(SecurityException.class, () -> ClassUtilities.areAllConstructorsPrivate(mockedClass), "Method should propagate SecurityException from reflection");
//         assertEquals("Access denied", se.getMessage());
//         // Verify that the mock was indeed called
//         verify(mockedClass).getDeclaredConstructors();
//     }

//     @Test
//     @DisplayName("Class with mixed visibility constructors → false")
//     void testMixedConstructorsOriginal() {
//         assertFalse(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.MixedConstructors.class), "Presence of a public constructor should cause method to return false");
//     }

//     @Test
//     @DisplayName("Class with no declared constructors (interface) → true")
//     void testInterfaceOriginal() {
//         assertTrue(ClassUtilities.areAllConstructorsPrivate(ClassUtilities_areAllConstructorsPrivate_Test.TestInterface.class), "Interfaces have no constructors, the loop is empty → true");
//     }
}