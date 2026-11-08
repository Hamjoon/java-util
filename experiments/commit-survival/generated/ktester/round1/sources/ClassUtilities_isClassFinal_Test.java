package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_isClassFinal_Test {

    /**
     * A regular (non‑final) class used for the negative test case.
     */
    private static class NonFinalTestClass {
    }

    /**
     * An interface – interfaces are never final.
     */
    private interface TestInterface {
    }

    /**
     * A simple final class used for the positive test case.
     */
    private static final class FinalTestClass {
    }

    // -----------------------------------------------------------------
    static class ClassUtilitiesTest {

        /**
         * Final inner class – should be reported as final.
         */
        static final class FinalInnerClass {
        }

        /**
         * Non‑final inner class – should be reported as non‑final.
         */
        static class NonFinalInnerClass {
        }

        /**
         * Record – records are implicitly final (Java 16+).
         */
        record RecordClass() {
        }
    }

    /**
     * Final inner class – should be reported as final.
     */
    static final class FinalInnerClass {
    }

    /**
     * Non‑final inner class – should be reported as non‑final.
     */
    static class NonFinalInnerClass {
    }

    /**
     * Final inner class – should be reported as final.
     */
    static final class FinalInnerClass {
    }

    /**
     * Non‑final inner class – should be reported as non‑final.
     */
    static class NonFinalInnerClass {
    }

    /**
     * Supplies the arguments for {@link #isClassFinal_variousInputs}.
     *
     * <p>Each {@code Arguments} instance contains:
     * <ol>
     *   <li>The {@link Class} to test.</li>
     *   <li>The expected boolean result (null if an exception is expected).</li>
     *   <li>The expected exception class (null when no exception is expected).</li>
     *   <li>A short description taken from the original JSON.</li>
     * </ol>
     */
    private static Stream<Arguments> provideIsClassFinalCases() {
        return Stream.of(// String – declared final
        Arguments.of(String.class, true, null, "String is declared as `public final class String`"), // ArrayList – regular, non‑final
        Arguments.of(java.util.ArrayList.class, false, null, "ArrayList is a regular, non‑final class"), // AbstractList – abstract but not final
        Arguments.of(java.util.AbstractList.class, false, null, "AbstractList is abstract, not final"), // List interface – never final
        Arguments.of(java.util.List.class, false, null, "Interface List is not final"), // Primitive int – no modifiers
        Arguments.of(int.class, false, null, "Primitive type int is not final"), // null – should throw NPE
        Arguments.of(null, null, NullPointerException.class, "Passing null should cause NullPointerException"), // Math – declared final
        Arguments.of(Math.class, true, null, "Math is declared as `public final class Math`"), // Integer wrapper – declared final
        Arguments.of(Integer.class, true, null, "Wrapper class Integer is final"), // Collections – utility class declared final
        Arguments.of(java.util.Collections.class, true, null, "Collections is declared as `public final class Collections`"), // DayOfWeek enum – implicitly final
        Arguments.of(java.time.DayOfWeek.class, true, null, "Enum DayOfWeek is implicitly final"), // int[] – array class, not final
        Arguments.of(int[].class, false, null, "Array class int[] is not final"), // Annotation type – interfaces, never final
        Arguments.of(java.lang.annotation.Retention.class, false, null, "Annotation types are interfaces, never final"), // User‑defined final inner class
        Arguments.of(ClassUtilitiesTest.FinalInnerClass.class, true, null, "User‑defined final inner class"), // User‑defined non‑final inner class
        Arguments.of(ClassUtilitiesTest.NonFinalInnerClass.class, false, null, "User‑defined non‑final inner class"), // Record – implicitly final (Java 16+)
        Arguments.of(ClassUtilitiesTest.RecordClass.class, true, null, "Record class is implicitly final"));
    }

    // -----------------------------------------------------------------
    @Test
    void finalClassShouldBeDetected() {
        assertTrue(ClassUtilities.isClassFinal(FinalTestClass.class), "FinalTestClass should be reported as final");
    }

    @Test
    void nonFinalClassShouldNotBeDetected() {
        assertFalse(ClassUtilities.isClassFinal(NonFinalTestClass.class), "NonFinalTestClass should not be reported as final");
    }

    @Test
    void interfaceShouldNeverBeReportedAsFinal() {
        assertFalse(ClassUtilities.isClassFinal(TestInterface.class), "Interfaces are never final");
    }

    @Test
    void enumShouldBeReportedAsFinal() {
        assertTrue(ClassUtilities.isClassFinal(TestEnum.class), "Enum types are implicitly final");
    }

    @Test
    void arrayClassIsNotFinal() {
        assertFalse(ClassUtilities.isClassFinal(int[].class), "Array classes are not final");
    }

    @Test
    void primitiveClassIsNotFinal() {
        // Primitive types have the modifier 0, therefore not final
        assertFalse(ClassUtilities.isClassFinal(int.class), "Primitive types are not final");
    }

    @Test
    void nullArgumentThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ClassUtilities.isClassFinal(null), "Passing null should result in NPE");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideIsClassFinalCases")
    void isClassFinal_variousInputs(Class<?> clazz, Boolean expectedResult, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            // Expecting an exception (e.g., NPE for null argument)
            assertThrows(expectedException, () -> ClassUtilities.isClassFinal(clazz), description);
        } else {
            // Normal boolean result expected
            boolean actual = ClassUtilities.isClassFinal(clazz);
            assertEquals(expectedResult.booleanValue(), actual, () -> description + " – expected: " + expectedResult);
        }
    }
}
