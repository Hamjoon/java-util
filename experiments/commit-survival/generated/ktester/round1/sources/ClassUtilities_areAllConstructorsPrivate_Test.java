package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.ReflectPermission;
import java.security.Permission;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_areAllConstructorsPrivate_Test {

    /**
     * A class whose only constructors are private.
     */
    static class AllPrivateConstructors {

        private AllPrivateConstructors() {
        }

        private AllPrivateConstructors(int x) {
        }
    }

    /**
     * A class with a public no‑arg constructor.
     */
    static class PublicConstructor {

        public PublicConstructor() {
        }
    }

    /**
     * A class that mixes private and public constructors.
     */
    static class MixedConstructors {

        private MixedConstructors() {
        }

        public MixedConstructors(String s) {
        }
    }

    /**
     * Class with an implicit public no‑arg constructor.
     */
    static class PublicDefaultConstructor {
    }

    /**
     * Class that defines a single private no‑arg constructor.
     */
    static class SinglePrivateConstructor {

        private SinglePrivateConstructor() {
        }
    }

    /**
     * Abstract class with a protected constructor (not private).
     */
    abstract static class AbstractProtectedConstructor {

        protected AbstractProtectedConstructor() {
        }
    }

    /**
     * Class with a package‑private (default) constructor.
     */
    static class PackagePrivateConstructor {

        // no modifier → package‑private
        PackagePrivateConstructor() {
        }
    }

    /**
     * Class with a single protected constructor.
     */
    static class ProtectedConstructor {

        protected ProtectedConstructor() {
        }
    }

    /**
     * Abstract class with a private constructor only.
     */
    abstract static class AbstractPrivateConstructor {

        private AbstractPrivateConstructor() {
        }
    }

    /**
     * Abstract class with no declared constructors → compiler supplies public default.
     */
    abstract static class AbstractNoConstructor {
    }

    /**
     * A {@link SecurityManager} that denies reflective access by throwing a
     * {@link SecurityException} when a {@link ReflectPermission} with the name
     * {@code "suppressAccessChecks"} is checked.
     */
    private static class DenyingSecurityManager extends SecurityManager {

        @Override
        public void checkPermission(Permission perm) {
            if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                throw new SecurityException("Reflective access denied by test SecurityManager");
            }
            // Allow all other permissions
        }
    }

    /**
     * Normal (non‑exception) test cases – expected boolean result.
     */
    private static Stream<Arguments> normalCases() {
        return Stream.of(// group: basic behavior
        Arguments.of(PublicDefaultConstructor.class, false, "Class has an implicit public no‑arg constructor; should return false."), Arguments.of(SinglePrivateConstructor.class, true, "Class defines exactly one private constructor; should return true."), Arguments.of(MixedConstructors.class, false, "Class has mixed visibility constructors; should return false."), Arguments.of(AllPrivateConstructors.class, true, "Class declares multiple private constructors; should return true."), Arguments.of(Runnable.class, true, "Interface has no constructors; should return true (vacuously)."), Arguments.of(AbstractProtectedConstructor.class, false, "Abstract class with a protected constructor; not private → false."), Arguments.of(int.class, true, "Primitive type has no constructors; should return true."), // group: special types and modifiers
        Arguments.of(PackagePrivateConstructor.class, false, "Package‑private constructor is not private; should return false."), Arguments.of(ProtectedConstructor.class, false, "Protected constructor is not private; should return false."), Arguments.of(EnumWithPrivateConstructor.class, true, "Enum constructors are implicitly private; should return true."), Arguments.of(AnnotationType.class, true, "Annotation types have no constructors; should return true."), Arguments.of(String[].class, true, "Array class has no constructors; should return true."), Arguments.of(AbstractPrivateConstructor.class, true, "Abstract class with only a private constructor; should return true."), Arguments.of(AbstractNoConstructor.class, false, "Abstract class without explicit constructors gets public default; should return false."));
    }

    /**
     * Exception‑expecting test cases – expected exception class.
     */
    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// null argument
        Arguments.of(null, NullPointerException.class, "Passing null should propagate NullPointerException."), // SecurityManager denial
        Arguments.of(PublicDefaultConstructor.class, SecurityException.class, "SecurityManager that denies reflective access should cause SecurityException."), // InaccessibleObjectException – may not be reproducible in all environments
        Arguments.of(PackagePrivateConstructor.class, InaccessibleObjectException.class, "Non‑open module access may throw InaccessibleObjectException."));
    }

    @Test
    @DisplayName("All declared constructors are private → true")
    void testAllConstructorsPrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(AllPrivateConstructors.class), "Expected true because every declared constructor is private");
    }

    @Test
    @DisplayName("At least one public constructor → false")
    void testPublicConstructor() {
        assertFalse(ClassUtilities.areAllConstructorsPrivate(PublicConstructor.class), "Expected false because there is a public constructor");
    }

    @Test
    @DisplayName("Mixed visibility constructors → false")
    void testMixedConstructors() {
        assertFalse(ClassUtilities.areAllConstructorsPrivate(MixedConstructors.class), "Expected false because not all constructors are private");
    }

    @Test
    @DisplayName("Interface (no constructors) → true")
    void testInterfaceHasNoConstructors() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(Runnable.class), "Interfaces have no declared constructors, method should return true");
    }

    @Test
    @DisplayName("Enum (compiler‑generated private constructors) → true")
    void testEnumConstructorsArePrivate() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(ExampleEnum.class), "Enum constructors are implicitly private, should return true");
    }

    @Test
    @DisplayName("Array class (no constructors) → true")
    void testArrayClassHasNoConstructors() {
        assertTrue(ClassUtilities.areAllConstructorsPrivate(int[].class), "Array classes have no declared constructors, should return true");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("normalCases")
    @DisplayName("areAllConstructorsPrivate – normal scenarios")
    void testAreAllConstructorsPrivateNormal(Class<?> clazz, boolean expected, String description) {
        assertEquals(expected, ClassUtilities.areAllConstructorsPrivate(clazz), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionCases")
    @DisplayName("areAllConstructorsPrivate – exception scenarios")
    void testAreAllConstructorsPrivateExceptions(Class<?> clazz, Class<? extends Throwable> expectedException, String description) {
        // Special handling for the SecurityException case
        if (expectedException == SecurityException.class) {
            SecurityManager original = System.getSecurityManager();
            System.setSecurityManager(new DenyingSecurityManager());
            try {
                assertThrows(expectedException, () -> ClassUtilities.areAllConstructorsPrivate(clazz), description);
            } finally {
                // restore original manager
                System.setSecurityManager(original);
            }
            return;
        }
        // InaccessibleObjectException is difficult to trigger reliably in an unnamed module.
        // The test is kept for completeness but disabled to avoid false failures.
        if (expectedException == InaccessibleObjectException.class) {
            Assumptions.assumeTrue(false, "InaccessibleObjectException cannot be reliably produced in the current test environment.");
        }
        // General case (e.g., NullPointerException)
        assertThrows(expectedException, () -> ClassUtilities.areAllConstructorsPrivate(clazz), description);
    }
}
