import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassName_Test {

    class Base {
    }

    class Derived extends Base {
    }

    /**
     * Simple user‑defined class for testing custom types.
     */
    static class MyClass {
    }

    /**
     * Container for a static inner class.
     */
    static class Outer {

        static class Inner {
        }
    }

    static class Inner {
    }

//     static class Inner {
    }

//     class Base {
    }

//     class Derived extends Base {
    }

    /**
     * Supplies the arguments for {@link #getClassName_parameterised(Object, String)}.
     * Each {@link Arguments} entry contains:
     * <ul>
     *   <li>the object to pass to {@code getClassName}</li>
     *   <li>the expected fully‑qualified class name (or the literal "null")</li>
     * </ul>
     */
//     static Stream<Arguments> provideObjectsForGetClassName() {
//         // Simple objects
//         Integer intObj = Integer.valueOf(42);
//         String strObj = "Hello, World!";
//         Double doubleObj = Double.valueOf(3.14);
//         // Arrays
//         int[] intArray = new int[] { 1, 2, 3 };
//         String[] strArray = new String[] { "a", "b", "c" };
//         int[][] int2dArray = new int[2][3];
//         // Collections / generic types
//         ArrayList<Object> arrayList = new ArrayList<>();
//         AtomicReference<String> atomicRef = new AtomicReference<>("test");
//         // Class object itself
//         Class<?> classObj = ArrayList.class;
//         // Custom user‑defined types
//         MyClass myClassInstance = new MyClass();
//         MyEnum myEnumInstance = MyEnum.VALUE;
//         // Static inner class
//         Outer.Inner innerInstance = new Outer.Inner();
//         // Anonymous subclass
//         Object anonymousInstance = new Object() {
//         };
//         return Stream.of(Arguments.of(null, "null"), Arguments.of(intObj, Integer.class.getName()), Arguments.of(strObj, String.class.getName()), Arguments.of(doubleObj, Double.class.getName()), Arguments.of(intArray, intArray.getClass().getName()), Arguments.of(strArray, strArray.getClass().getName()), Arguments.of(int2dArray, int2dArray.getClass().getName()), Arguments.of(arrayList, arrayList.getClass().getName()), Arguments.of(atomicRef, atomicRef.getClass().getName()), Arguments.of(classObj, Class.class.getName()), Arguments.of(myClassInstance, ReflectionUtils_getClassName_Test.MyClass.class.getName()), Arguments.of(myEnumInstance, MyEnum.class.getName()), Arguments.of(innerInstance, ReflectionUtils_getClassName_Test.Outer.Inner.class.getName()), Arguments.of(anonymousInstance, anonymousInstance.getClass().getName()));
//     }

//     @Test
//     void getClassName_returnsStringNull_whenObjectIsNull() {
//         // GIVEN
//         Object input = null;
//         // WHEN
//         String result = ReflectionUtils.getClassName(input);
//         // THEN
//         assertEquals("null", result, "Expected the literal string \"null\" for a null argument");
//     }

//     @Test
//     void getClassName_returnsClassName_forRegularObject() {
//         // GIVEN
//         String input = "hello";
//         // WHEN
//         String result = ReflectionUtils.getClassName(input);
//         // THEN
//         assertEquals(String.class.getName(), result, "The class name of a String instance should be java.lang.String");
//     }

//     @Test
//     void getClassName_returnsClassName_forArrayObject() {
//         // GIVEN
//         int[] input = new int[] { 1, 2, 3 };
//         // WHEN
//         String result = ReflectionUtils.getClassName(input);
//         // THEN
//         assertEquals(int[].class.getName(), result, "Array class names include the leading '[' character");
//     }

//     @Test
//     void getClassName_returnsClassName_forCustomClassHierarchy() {
//         // GIVEN
//         class Base {
//         }
//         class Derived extends Base {
//         }
//         Derived instance = new Derived();
//         // WHEN
//         String result = ReflectionUtils.getClassName(instance);
//         // THEN
//         assertEquals(Derived.class.getName(), result, "Should return the concrete runtime class name, not the superclass name");
//     }

    /**
     * Parameterised test that iterates over a wide range of possible
     * argument types (null, primitives wrappers, arrays, enums, inner
     * classes, anonymous classes, generic types, etc.).
     */
//     @ParameterizedTest(name = "{index}: getClassName({0}) => \"{1}\"")
//     @MethodSource("provideObjectsForGetClassName")
//     void getClassName_parameterised(Object input, String expectedClassName) {
//         // WHEN
//         String actual = ReflectionUtils.getClassName(input);
//         // THEN
//         assertEquals(expectedClassName, actual, () -> "Unexpected class name for input: " + (input == null ? "null" : input.getClass()));
//     }
}