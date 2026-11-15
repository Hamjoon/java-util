import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import java.time.DayOfWeek;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_isClassFinal_Test {

    interface MyInterface {
    }

    final class MyFinalClass {
    }

    class MyNonFinalClass {
    }

    abstract class MyAbstractClass {
    }

//     interface MyInterface {
    }

//     interface MyInterface {
    }

//     final class MyFinalClass {
    }

//     class MyNonFinalClass {
    }

//     abstract class MyAbstractClass {
    }

    /**
     * Simple user‑defined final class used by the parameterized test.
     */
    static final class FinalTestClass {
    }

    /**
     * Abstract class used by the parameterized test.
     */
    static abstract class AbstractList {
    }

//     final class MyFinalClass {
    }

//     class MyNonFinalClass {
    }

//     abstract class MyAbstractClass {
    }

//     interface MyInterface {
    }

    /**
     * Provides a stream of arguments: the class to test, the expected boolean result,
     * and a short description used for the assertion message.
     */
    private static Stream<Arguments> isClassFinalProvider() {
        return Stream.of(Arguments.of(String.class, true, "String is declared as public final class String"), Arguments.of(ArrayList.class, false, "ArrayList is a regular, non‑final class"), Arguments.of(List.class, false, "Interface List is not final"), Arguments.of(int.class, false, "Primitive type int has no modifiers, should be reported as not final"), Arguments.of(ClassUtilities_isClassFinal_Test.FinalTestClass.class, true, "User‑defined final class should be recognised as final"), Arguments.of(DayOfWeek.class, true, "Enum types are implicitly final"), Arguments.of(Override.class, false, "Annotation types are interfaces and not final"), Arguments.of(Collections.class, true, "java.util.Collections is a public final utility class"), Arguments.of(Integer.class, true, "Wrapper class Integer is declared final"), Arguments.of(String[].class, false, "Array classes have no modifiers"), Arguments.of(ClassUtilities_isClassFinal_Test.AbstractList.class, false, "Abstract classes are not final"));
    }

    /**
     * A user‑defined final class – the method should return {@code true}.
     */
//     @Test
//     void shouldReturnTrueForUserDefinedFinalClass() {
//         final class MyFinalClass {
//         }
//         assertTrue(ClassUtilities.isClassFinal(MyFinalClass.class), "User‑defined final class must be reported as final");
//     }

    /**
     * A regular (non‑final) class – the method should return {@code false}.
     */
//     @Test
//     void shouldReturnFalseForNonFinalClass() {
//         class MyNonFinalClass {
//         }
//         assertFalse(ClassUtilities.isClassFinal(MyNonFinalClass.class), "Non‑final class must be reported as not final");
//     }

    /**
     * An abstract class – abstract classes are not final, so the method must return {@code false}.
     */
//     @Test
//     void shouldReturnFalseForAbstractClass() {
//         abstract class MyAbstractClass {
//         }
//         assertFalse(ClassUtilities.isClassFinal(MyAbstractClass.class), "Abstract class must be reported as not final");
//     }

    /**
     * An interface – interfaces cannot be final, the method must return {@code false}.
     */
//     @Test
//     void shouldReturnFalseForInterface() {
//         interface MyInterface {
//         }
//         assertFalse(ClassUtilities.isClassFinal(MyInterface.class), "Interface must be reported as not final");
//     }

    /**
     * The JDK {@link java.lang.String} class is declared {@code final}.
     */
//     @Test
//     void shouldReturnTrueForJdkFinalClass() {
//         assertTrue(ClassUtilities.isClassFinal(String.class), "java.lang.String is final and should be reported as such");
//     }

    /**
     * The JDK {@link java.util.ArrayList} class is not final.
     */
//     @Test
//     void shouldReturnFalseForJdkNonFinalClass() {
//         assertFalse(ClassUtilities.isClassFinal(ArrayList.class), "java.util.ArrayList is not final and should be reported as such");
//     }

    /**
     * Passing {@code null} should result in a {@link NullPointerException}.
     */
//     @Test
//     void shouldThrowNPEWhenClassIsNull() {
//         assertThrows(NullPointerException.class, () -> ClassUtilities.isClassFinal(null), "Calling isClassFinal(null) must throw NullPointerException");
//     }

    /**
     * Parameterized test that verifies {@link ClassUtilities#isClassFinal(Class)} returns the
     * expected boolean for a variety of class kinds.
     *
     * @param clazz
     * 		the class to evaluate
     * @param expected
     * 		the expected result (true if the class is final)
     * @param description
     * 		a short description used in the assertion message
     */
//     @ParameterizedTest(name = "[{index}] {2}")
//     @MethodSource("isClassFinalProvider")
//     void shouldDetectFinalModifierCorrectly(Class<?> clazz, boolean expected, String description) {
//         assertEquals(expected, ClassUtilities.isClassFinal(clazz), () -> "Failed for: " + description);
//     }

    /**
     * The JDK {@link java.util.Collections} class is declared {@code final}.
     */
//     @Test
//     void shouldReturnTrueForJdkFinalUtilityClass() {
//         assertTrue(ClassUtilities.isClassFinal(Collections.class), "java.util.Collections is final and should be reported as such");
//     }
}