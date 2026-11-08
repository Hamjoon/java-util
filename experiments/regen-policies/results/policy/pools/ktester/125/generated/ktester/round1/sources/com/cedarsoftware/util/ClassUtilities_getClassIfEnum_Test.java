import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_getClassIfEnum_Test {

    class DeepNested {

        static class LevelOne {

            static class LevelTwo {

                class LevelThree {
                }
            }
        }
    }

    static class LevelOne {

        static class LevelTwo {

            class LevelThree {
            }
        }
    }

    static class LevelTwo {

        class LevelThree {
        }
    }

    class LevelThree {
    }

    class LocalClass {
    }

//     class NestedInsideEnum extends OuterEnum.InnerStatic {
    }

    /**
     * Static nested class inside the enum.
     */
    static class InnerStatic {
    }

    /**
     * Non‑static inner class inside the enum.
     */
    class InnerNonStatic {
    }

    /**
     * Static inner class of the enum.
     */
    static class StaticInner {

        /**
         * Non‑static inner class inside the static inner class.
         */
        class NonStaticInner {
        }
    }

    /**
     * Non‑static inner class inside the static inner class.
     */
    class NonStaticInner {
    }

    /**
     * Non‑static inner class inside the static inner class.
     */
//     class NonStaticInner {
    }

    class Local {
    }

    /**
     * Wrapper object for parameterized tests.
     */
    private static final class ArgumentsHolder {

        final Class<?> input;

        final Class<?> expected;

        final String description;

        ArgumentsHolder(Class<?> input, Class<?> expected, String description) {
            this.input = input;
            this.expected = expected;
            this.description = description;
        }
    }

    @SuppressWarnings("unused")
    private static class ClassWithRestrictedReflection {
    }

    @SuppressWarnings("unused")
    private static class ClassWithRestrictedEnclosing {
    }

    static class RegularClass {
    }

    static class OuterClass {

        enum NestedEnum {

            A, B
        }
    }

//     class NestedInsideEnum extends OuterEnum.InnerStatic {
    }

//     private static Stream<ArgumentsHolder> provideEnumScenarios() {
//         return Stream.of(new ArgumentsHolder(SimpleEnum.class, SimpleEnum.class, "Direct enum detection"), new ArgumentsHolder(Enum.class, null, "java.lang.Enum should be ignored"), new ArgumentsHolder(ClassUtilities_getClassIfEnum_Test.RegularClass.class, null, "Regular class with no enum relation"), new ArgumentsHolder(OuterEnum.InnerStatic.class, OuterEnum.class, "Static nested class inside enum"), new ArgumentsHolder(OuterEnum.ONE.new InnerNonStatic().getClass(), OuterEnum.class, "Non‑static inner class inside enum"), new ArgumentsHolder(MultiLevelEnum.StaticInner.NonStaticInner.class, MultiLevelEnum.class, "Deeply nested class inside enum"), new ArgumentsHolder(EnumWithConstantBody.ONE.getClass(), EnumWithConstantBody.class, "Synthetic subclass for enum constant"), new ArgumentsHolder(ClassUtilities_getClassIfEnum_Test.OuterClass.NestedEnum.class, ClassUtilities_getClassIfEnum_Test.OuterClass.NestedEnum.class, "Nested enum inside regular class"), new ArgumentsHolder(EnumWithLocalClass.ONE.defineLocal(), EnumWithLocalClass.class, "Local class inside enum method"));
//     }

//     @Test
//     @DisplayName("null argument returns null")
//     void testNullInput() {
//         Assertions.assertNull(ClassUtilities.getClassIfEnum(null), "Passing null should return null");
//     }

//     @Test
//     @DisplayName("direct enum class is returned")
//     void testDirectEnum() {
//         Class<?> result = ClassUtilities.getClassIfEnum(SimpleEnum.class);
//         Assertions.assertEquals(SimpleEnum.class, result, "The method should return the enum class itself");
//     }

//     @Test
//     @DisplayName("Enum.class itself is ignored")
//     void testEnumBaseClassIgnored() {
//         Assertions.assertNull(ClassUtilities.getClassIfEnum(Enum.class), "Enum.class should be ignored and result in null");
//     }

//     @Test
//     @DisplayName("Non‑enum class returns null")
//     void testNonEnumClass() {
//         Assertions.assertNull(ClassUtilities.getClassIfEnum(ClassUtilities_getClassIfEnum_Test.RegularClass.class), "A regular non‑enum class should yield null");
//     }

//     @Test
//     @DisplayName("Static nested class inside an enum returns the enclosing enum")
//     void testStaticNestedInsideEnum() {
//         Class<?> result = ClassUtilities.getClassIfEnum(OuterEnum.InnerStatic.class);
//         Assertions.assertEquals(OuterEnum.class, result, "Static nested class inside an enum should resolve to the enum");
//     }

//     @Test
//     @DisplayName("Non‑static inner class inside an enum returns the enclosing enum")
//     void testNonStaticInnerInsideEnum() {
//         OuterEnum outer = OuterEnum.ONE;
//         Class<?> innerClass = outer.new InnerNonStatic().getClass();
//         Class<?> result = ClassUtilities.getClassIfEnum(innerClass);
//         Assertions.assertEquals(OuterEnum.class, result, "Non‑static inner class inside an enum should resolve to the enum");
//     }

//     @Test
//     @DisplayName("Enclosing class hierarchy traversal finds enum")
//     void testDeeplyNestedInsideEnum() {
//         // A class that extends a static inner class of the enum – its enclosing class is the enum.
//         class NestedInsideEnum extends OuterEnum.InnerStatic {
//         }
//         Class<?> result = ClassUtilities.getClassIfEnum(NestedInsideEnum.class);
//         Assertions.assertEquals(OuterEnum.class, result, "A class whose enclosing class is an enum should resolve to that enum");
//     }

//     @Test
//     @DisplayName("Synthetic enum‑constant subclass resolves to its enum")
//     void testEnumConstantSpecificSubclass() {
//         Class<?> constantClass = EnumWithConstantBody.ONE.getClass();
//         Class<?> result = ClassUtilities.getClassIfEnum(constantClass);
//         Assertions.assertEquals(EnumWithConstantBody.class, result, "Synthetic subclass for an enum constant should resolve to the enum");
//     }

//     @Test
//     @DisplayName("Nested enum inside a regular class is detected")
//     void testNestedEnumInsideRegularClass() {
//         Class<?> result = ClassUtilities.getClassIfEnum(ClassUtilities_getClassIfEnum_Test.OuterClass.NestedEnum.class);
//         Assertions.assertEquals(ClassUtilities_getClassIfEnum_Test.OuterClass.NestedEnum.class, result, "A top‑level enum declared inside another class should be recognised");
//     }

//     @Test
//     @DisplayName("Local class defined inside an enum method resolves to the enum")
//     void testLocalClassInsideEnumMethod() {
//         Class<?> localClass = EnumWithLocalClass.ONE.defineLocal();
//         Class<?> result = ClassUtilities.getClassIfEnum(localClass);
//         Assertions.assertEquals(EnumWithLocalClass.class, result, "Local class inside an enum method should resolve to the enclosing enum");
//     }

//     @Test
//     @DisplayName("Non‑static inner class nested inside a static inner class of an enum")
//     void testMixedStaticNonStaticNesting() {
//         Class<?> inner = MultiLevelEnum.StaticInner.NonStaticInner.class;
//         Class<?> result = ClassUtilities.getClassIfEnum(inner);
//         Assertions.assertEquals(MultiLevelEnum.class, result, "A non‑static inner class inside a static inner class of an enum should resolve to the enum");
//     }

//     @ParameterizedTest(name = "[{index}] {2}")
//     @MethodSource("provideEnumScenarios")
//     @DisplayName("Parameterized enum detection scenarios")
//     void testParameterizedEnumScenarios(ArgumentsHolder holder) {
//         Class<?> result = ClassUtilities.getClassIfEnum(holder.input);
//         Assertions.assertEquals(holder.expected, result, "Unexpected result for scenario: " + holder.description);
//     }

//     @Disabled("Requires a custom SecurityManager that throws on reflective access; not reliable in all CI environments")
//     @Test
//     @DisplayName("SecurityException during hierarchy traversal")
//     void testSecurityExceptionDuringHierarchy() {
//         Assertions.assertThrows(SecurityException.class, () -> {
//             Class<?> problematic = ClassWithRestrictedReflection.class;
//             ClassUtilities.getClassIfEnum(problematic);
//         });
//     }

//     @Disabled("Requires a custom SecurityManager that throws on getEnclosingClass(); see above")
//     @Test
//     @DisplayName("SecurityException during enclosing class traversal")
//     void testSecurityExceptionDuringEnclosing() {
//         Assertions.assertThrows(SecurityException.class, () -> {
//             Class<?> problematic = ClassWithRestrictedEnclosing.class;
//             ClassUtilities.getClassIfEnum(problematic);
//         });
//     }
}