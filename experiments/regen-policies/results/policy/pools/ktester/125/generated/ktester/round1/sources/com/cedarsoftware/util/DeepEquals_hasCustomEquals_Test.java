import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeepEquals_hasCustomEquals_Test {

    class LocalDummyClass {
    }

//     class LocalDummyClass {
    }

    class DummyClass {
    }

    /**
     * No custom {@code equals} implementation – inherits from {@link Object}.
     */
    static class NoCustomEquals {

        int id;

        NoCustomEquals(int id) {
            this.id = id;
        }
    }

    /**
     * Directly overrides {@code equals}.
     */
    static class DirectCustomEquals {

        int id;

        DirectCustomEquals(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof DirectCustomEquals))
                return false;
            DirectCustomEquals that = (DirectCustomEquals) o;
            return this.id == that.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    /**
     * Superclass overrides {@code equals}; subclass does not.
     */
    static class SuperWithEquals {

        int id;

        SuperWithEquals(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof SuperWithEquals))
                return false;
            SuperWithEquals that = (SuperWithEquals) o;
            return this.id == that.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    static class SubWithoutEquals extends SuperWithEquals {

        SubWithoutEquals(int id) {
            super(id);
        }
    }

    /**
     * Simple POJO without custom {@code equals}.
     */
    static class PlainClass {

        int id;

        PlainClass(int id) {
            this.id = id;
        }
    }

    /**
     * Directly overrides {@code equals(Object)}.
     */
    static class DirectOverride {

        int id;

        DirectOverride(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof DirectOverride))
                return false;
            DirectOverride that = (DirectOverride) o;
            return this.id == that.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    /**
     * Subclass that inherits {@code equals} from its superclass.
     */
    static class SubClassOfOverride extends DirectOverride {

        SubClassOfOverride(int id) {
            super(id);
        }
    }

    /**
     * Interface – does not declare its own {@code equals}.
     */
    interface SampleInterface {
    }

    /**
     * Abstract class providing its own public {@code equals(Object)}.
     */
    static abstract class AbstractOverride {

        int id;

        AbstractOverride(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof AbstractOverride))
                return false;
            AbstractOverride that = (AbstractOverride) o;
            return this.id == that.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    /**
     * Class with a *private* equals‑like method – not an override.
     */
    static class PrivateEquals {

        private boolean equalsPrivate(Object o) {
            return false;
        }
    }

    /**
     * Class that defines a public method with a different signature (equals(String)).
     */
    static class DifferentSignatureEquals {

        public boolean equals(String s) {
            return false;
        }
    }

    /**
     * Empty class with package‑private visibility (used for module‑access test).
     */
    class ModulePrivateClass {
    }

    /* -------------------------------------------------------------
       Arguments holder for parameterised tests
       ------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final Class<?> clazz;

        final boolean expected;

        final String displayName;

        ArgumentsHolder(Class<?> clazz, boolean expected, String displayName) {
            this.clazz = clazz;
            this.expected = expected;
            this.displayName = displayName;
        }
    }

//     class LocalDummyClass {
    }

    /**
     * Supplies the basic detection scenarios (class + expected result).
     * The description field from the JSON is used as the test name via {@code @DisplayName}.
     */
//     private static Stream<ArgumentsHolder> detectionProvider() {
//         return Stream.of(Arguments.of(new ArgumentsHolder(PlainClass.class, false, "Plain class without custom equals")), Arguments.of(new ArgumentsHolder(DirectOverride.class, true, "Directly overrides equals")), Arguments.of(new ArgumentsHolder(SubClassOfOverride.class, true, "Subclass inherits custom equals")), Arguments.of(new ArgumentsHolder(SampleInterface.class, false, "Interface does not declare equals")), Arguments.of(new ArgumentsHolder(int.class, false, "Primitive type class")), Arguments.of(new ArgumentsHolder(Integer.class, true, "Wrapper class Integer overrides equals")), Arguments.of(new ArgumentsHolder(String.class, true, "Final class java.lang.String overrides equals")), Arguments.of(new ArgumentsHolder(AbstractOverride.class, true, "Abstract class with public equals")), Arguments.of(new ArgumentsHolder(CustomEnum.class, true, "Enum that overrides equals")), Arguments.of(new ArgumentsHolder(PrivateEquals.class, false, "Class with private equals (not visible)")), Arguments.of(new ArgumentsHolder(DifferentSignatureEquals.class, false, "Class with equals(String) only")), Arguments.of(new ArgumentsHolder(int[].class, false, "Array class does not declare equals")));
//     }

//     @Test
//     void hasCustomEquals_returnsFalseWhenClassDoesNotOverrideEquals() {
//         Class<?> clazz = NoCustomEquals.class;
//         boolean result = DeepEquals.hasCustomEquals(clazz);
//         Assertions.assertFalse(result, "Expected hasCustomEquals to be false for a class that does not override equals()");
//     }

//     @Test
//     void hasCustomEquals_returnsTrueWhenClassOverridesEqualsDirectly() {
//         Class<?> clazz = DirectCustomEquals.class;
//         boolean result = DeepEquals.hasCustomEquals(clazz);
//         Assertions.assertTrue(result, "Expected hasCustomEquals to be true for a class that directly overrides equals()");
//     }

//     @Test
//     void hasCustomEquals_returnsTrueWhenSuperclassOverridesEquals() {
//         Class<?> clazz = SubWithoutEquals.class;
//         boolean result = DeepEquals.hasCustomEquals(clazz);
//         Assertions.assertTrue(result, "Expected hasCustomEquals to be true when a superclass provides a custom equals()");
//     }

//     @Test
//     void hasCustomEquals_isConsistentAcrossMultipleInvocations() {
//         Class<?> clazz = DirectCustomEquals.class;
//         for (int i = 0; i < 5; i++) {
//             Assertions.assertTrue(DeepEquals.hasCustomEquals(clazz), "hasCustomEquals should consistently return true on repeated calls");
//         }
//     }

//     @Test
//     void hasCustomEquals_throwsNullPointerExceptionWhenClassIsNull() {
//         Class<?> clazz = null;
//         Assertions.assertThrows(NullPointerException.class, () -> DeepEquals.hasCustomEquals(clazz), "Calling hasCustomEquals with a null argument should throw NPE");
//     }

//     @ParameterizedTest(name = "{2}")
//     @MethodSource("detectionProvider")
//     void hasCustomEquals_detectionScenarios(Class<?> clazz, boolean expected, String _displayName) {
//         boolean result = DeepEquals.hasCustomEquals(holder.clazz);
//         Assertions.assertEquals(holder.expected, result, () -> String.format("hasCustomEquals(%s) expected %s but got %s", holder.clazz.getName(), holder.expected, result));
//     }

//     @Test
//     void hasCustomEquals_propagatesSecurityExceptionFromReflectionUtils() {
//         try (MockedStatic<ReflectionUtils> mocked = mockStatic(ReflectionUtils.class)) {
//             mocked.when(() -> ReflectionUtils.getMethod(String.class, "equals", Object.class)).thenThrow(new SecurityException("Access denied"));
//             SecurityException ex = Assertions.assertThrows(SecurityException.class, () -> DeepEquals.hasCustomEquals(String.class), "Expected SecurityException to propagate from ReflectionUtils");
//             Assertions.assertEquals("Access denied", ex.getMessage());
//         }
//     }

//     @Test
//     void hasCustomEquals_propagatesIllegalAccessExceptionFromReflectionUtils() {
//         class LocalDummyClass {
//         }
//         try (MockedStatic<ReflectionUtils> mocked = mockStatic(ReflectionUtils.class)) {
//             mocked.when(() -> ReflectionUtils.getMethod(LocalDummyClass.class, "equals", Object.class)).thenThrow(new IllegalAccessException("Module not opened"));
//             IllegalAccessException ex = Assertions.assertThrows(IllegalAccessException.class, () -> DeepEquals.hasCustomEquals(LocalDummyClass.class), "Expected IllegalAccessException to propagate from ReflectionUtils");
//             Assertions.assertEquals("Module not opened", ex.getMessage());
//         }
//     }

//     @Test
//     void hasCustomEquals_detectsAnonymousClassWithCustomEquals() {
//         Object anon = new Object() {
// 
//             @Override
//             public boolean equals(Object o) {
//                 return this == o;
//             }
//         };
//         Class<?> clazz = anon.getClass();
//         boolean result = DeepEquals.hasCustomEquals(clazz);
//         Assertions.assertTrue(result, "Anonymous subclass that overrides equals should be detected as having a custom equals");
//     }
}