package com.cedarsoftware.util;

import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_getClassIfEnum_Test {

    /**
     * A regular (non‑enum) class used for the tests.
     */
    private static class RegularClass {
    }

    /**
     * Outer class containing a static inner enum.
     */
    private static class OuterClass {

        enum InnerEnum {

            ONE, TWO
        }
    }

//     @Test
//     @DisplayName("getClassIfEnum returns null when the argument is null")
//     void testNullArgument() {
//         assertNull(ClassUtilities.getClassIfEnum(null), "When the supplied Class is null the method should return null.");
//     }

//     @Test
//     @DisplayName("getClassIfEnum returns null for a non‑enum class")
//     void testNonEnumReturnsNull() {
//         Class<?> result = ClassUtilities.getClassIfEnum(RegularClass.class);
//         assertNull(result, "For a class that is not an enum the method should return null.");
//     }

//     @ParameterizedTest(name = "[{index}] {2}")
//     @MethodSource("enumClassProvider")
//     @DisplayName("Parameterized tests for getClassIfEnum")
//     void testGetClassIfEnumParameterized(Class<?> inputClass, Class<?> expectedEnumClass, String description) {
//         // First call
//         Class<?> firstResult = ClassUtilities.getClassIfEnum(inputClass);
//         assertEquals(expectedEnumClass, firstResult, "First invocation: " + description);
//         // Second call to verify caching (when expected is non‑null)
//         if (expectedEnumClass != null) {
//             Class<?> secondResult = ClassUtilities.getClassIfEnum(inputClass);
//             // The cached value should be the exact same Class object (reference equality)
//             assertSame(firstResult, secondResult, "Second invocation should return the cached Class instance.");
//         }
//     }

//     @Test
//     @DisplayName("getClassIfEnum returns null for the Enum base class")
//     void testBaseEnumClass() {
//         assertNull(ClassUtilities.getClassIfEnum(Enum.class), "The abstract java.lang.Enum class should not be considered an enum type.");
//     }

//     @Test
//     @DisplayName("getClassIfEnum returns the enum class for a static inner enum")
//     void testStaticInnerEnum() {
//         Class<?> result = ClassUtilities.getClassIfEnum(OuterClass.InnerEnum.class);
//         assertEquals(OuterClass.InnerEnum.class, result, "Nested (static inner) enum should be correctly identified and returned.");
//     }

//     @Test
//     @DisplayName("getClassIfEnum resolves class of an enum constant")
//     void testEnumConstantClass() {
//         // DayOfWeek is a JDK enum; the constant's class is a synthetic subclass.
//         Class<?> result = ClassUtilities.getClassIfEnum(DayOfWeek.MONDAY.getClass());
//         assertEquals(DayOfWeek.class, result, "Enum constant with a synthetic subclass should resolve to the original enum type.");
//     }

//     @Test
//     @DisplayName("getClassIfEnum returns the enum class when an enum type is supplied")
//     void testEnumClassReturned() {
//         Class<?> result = ClassUtilities.getClassIfEnum(DayOfWeek.class);
//         assertNotNull(result, "Result should not be null for an enum type.");
//         assertEquals(DayOfWeek.class, result, "The method should return the exact enum class that was supplied.");
//     }

//     @Test
//     @DisplayName("getClassIfEnum returns null for a primitive type")
//     void testPrimitiveType() {
//         assertNull(ClassUtilities.getClassIfEnum(int.class), "Primitive type classes are not enums; method should return null.");
//     }

//     @Test
//     @DisplayName("getClassIfEnum returns null for an enum array type")
//     void testEnumArrayClass() {
//         assertNull(ClassUtilities.getClassIfEnum(DayOfWeek[].class), "Array type of an enum is not an enum; method should return null.");
//     }

//     @Test
//     @DisplayName("getClassIfEnum returns null for a regular final class")
//     void testFinalRegularClass() {
//         assertNull(ClassUtilities.getClassIfEnum(AtomicInteger.class), "A regular non‑enum final class must yield null.");
//     }
}