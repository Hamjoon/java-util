import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_getClassIfEnum_Test {

    static class Inner {
    }

    class Local {
    }

    class LocalClass {
    }

    /**
     * Static inner class inside the enum.
     */
    static class InnerClass {

        /**
         * Further nested static class.
         */
        static class NestedInner {
        }
    }

    /**
     * Further nested static class.
     */
    static class NestedInner {
    }

    /**
     * Further nested static class.
     */
//     static class NestedInner {
    }

    /**
     * Regular (non‑enum) class used to verify that unrelated classes return {@code null}.
     */
    private static class PlainClass {
    }

    /**
     * Regular outer class that contains a nested enum.
     */
    private static class OuterClass {

        enum InnerEnum {

            X, Y
        }
    }

    /**
     * Static inner class with its own nested class – used in some tests.
     */
//     static class InnerClass {

        /**
         * Further nested static class.
         */
        static class NestedInner {
        }
    }

    /**
     * Further nested static class.
     */
//     static class NestedInner {
    }

    /**
     * Further nested static class.
     */
//     static class NestedInner {
    }

    /**
     * Nested static class inside {@link InnerClass}.
     */
//     static Stream<Class<?>> nullReturningClasses() {
//         // regular class, primitive, array of enum, Object, another non‑enum class
//         return Stream.of(PlainClass.class, int.class, SimpleEnum[].class, Object.class, String.class);
//     }

//     @Test
//     @DisplayName("null argument returns null")
//     void testNullInput() {
//         assertNull(ClassUtilities.getClassIfEnum(null), "When the input class is null the method should return null");
//     }

//     @Test
//     @DisplayName("Enum class returns itself")
//     void testEnumClassItself() {
//         assertSame(TestEnum.class, ClassUtilities.getClassIfEnum(TestEnum.class), "The enum class itself should be returned");
//     }

//     @Test
//     @DisplayName("Enum.class (java.lang.Enum) is ignored")
//     void testJavaEnumClassIgnored() {
//         assertNull(ClassUtilities.getClassIfEnum(Enum.class), "The base java.lang.Enum class must be ignored and result in null");
//     }

//     @Test
//     @DisplayName("Non‑enum class returns null")
//     void testNonEnumClass() {
//         assertNull(ClassUtilities.getClassIfEnum(PlainClass.class), "A regular non‑enum class should yield null");
//     }

//     @Test
//     @DisplayName("Enclosing enum is found for a nested class")
//     void testEnclosingEnumFound() {
//         Class<?> result = ClassUtilities.getClassIfEnum(EnumWithInner.InnerClass.class);
//         assertSame(EnumWithInner.class, result, "The enclosing enum should be returned for a nested class");
//     }

//     @Test
//     @DisplayName("Superclass chain containing an enum returns that enum")
//     void testSuperclassContainsEnum() {
//         // Obtain the synthetic subclass generated for the enum constant
//         TestEnum enumConstant = TestEnum.A;
//         Class<?> anonSubclass = enumConstant.getClass();
//         assertSame(TestEnum.class, ClassUtilities.getClassIfEnum(anonSubclass), "Superclass chain that includes an enum should resolve to that enum");
//     }

//     @Test
//     @DisplayName("Enum class returns itself")
//     void testDirectEnum() {
//         assertSame(SimpleEnum.class, ClassUtilities.getClassIfEnum(SimpleEnum.class), "A class that is itself an enum should be returned unchanged.");
//     }

//     @Test
//     @DisplayName("Nested inner class enclosed by enum resolves to the enum")
//     void testNestedInnerClassEnclosedByEnum() {
//         Class<?> result = ClassUtilities.getClassIfEnum(InnerClass.NestedInner.class);
//         assertSame(EnumWithInner.class, result, "A deeper nested inner class whose outermost enclosing class is an enum should still be resolved to that enum");
//     }

//     @Test
//     @DisplayName("Primitive type input returns null")
//     void testPrimitiveInput() {
//         assertNull(ClassUtilities.getClassIfEnum(int.class), "A primitive class (int) is not an enum and has no enclosing class; the method should return null.");
//     }

//     @Test
//     @DisplayName("Enum array input returns null")
//     void testEnumArrayInput() {
//         assertNull(ClassUtilities.getClassIfEnum(SimpleEnum[].class), "Array types are not enums; even if the component type is an enum, the method should return null.");
//     }

//     @Test
//     @DisplayName("Object class input returns null")
//     void testObjectClassInput() {
//         assertNull(ClassUtilities.getClassIfEnum(Object.class), "The root Object class is not an enum and has no enclosing class; the method should return null.");
//     }

//     @Test
//     @DisplayName("Local class defined inside enum method resolves to the enum")
//     void testLocalClassInsideEnumMethod() {
//         Class<?> localClass = SimpleEnum.getLocalClass();
//         assertSame(SimpleEnum.class, ClassUtilities.getClassIfEnum(localClass), "A local (non‑static) class created inside a method of an enum has the enum as its enclosing class; the method should resolve to that enum.");
//     }

//     @Test
//     @DisplayName("Lambda expression inside enum method resolves to the enum")
//     void testLambdaInsideEnumMethod() {
//         Class<?> lambdaClass = SimpleEnum.getLambdaClass();
//         assertSame(SimpleEnum.class, ClassUtilities.getClassIfEnum(lambdaClass), "A lambda expression defined inside an enum method is compiled to a synthetic class whose enclosing class is the enum; the method should return the enum.");
//     }

//     @Test
//     @DisplayName("Nested enum inside regular class returns itself")
//     void testNestedEnumInsideRegularClass() {
//         assertSame(OuterClass.InnerEnum.class, ClassUtilities.getClassIfEnum(OuterClass.InnerEnum.class), "An enum declared inside a non‑enum outer class should be returned unchanged when its Class object is supplied.");
//     }

//     @Test
//     @DisplayName("Enum constant with overridden method (anonymous subclass) resolves to the enum")
//     void testEnumConstantAnonymousSubclass() {
//         Class<?> anonSubclass = EnumWithBody.CONST.getClass();
//         assertSame(EnumWithBody.class, ClassUtilities.getClassIfEnum(anonSubclass), "Enum constants that provide a class body are compiled as anonymous subclasses of the enum; the method must climb the superclass chain to return the actual enum type.");
//     }

//     @ParameterizedTest(name = "{index} => class={0}")
//     @MethodSource("nullReturningClasses")
//     @DisplayName("Various non‑enum inputs should return null")
//     void testVariousNonEnumInputs(Class<?> input) {
//         assertNull(ClassUtilities.getClassIfEnum(input), () -> "Class " + input + " should yield null because it is not related to an enum.");
//     }
}