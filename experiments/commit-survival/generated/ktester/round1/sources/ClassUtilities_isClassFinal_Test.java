package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_isClassFinal_Test {

    /**
     * A simple final class used for the positive test.
     */
    private static final class FinalTestClass {
    }

    /**
     * A simple non‑final class used for the negative test.
     */
    private static class NonFinalTestClass {
    }

    /**
     * Simple interface for testing that interfaces are not final.
     */
    private interface TestInterface {
    }

    /**
     * Simple abstract class for testing that abstract classes are not final.
     */
    private abstract static class AbstractTestClass {
    }

    /**
     * User‑defined final class for the custom‑final test case.
     */
    private static final class CustomFinalClass {
    }

    private static Stream<Arguments> finalClassProvider() {
        return Stream.of(Arguments.of("String is a final class", String.class, true), Arguments.of("Collections is declared final", java.util.Collections.class, true), Arguments.of("Enum types are implicitly final", TestEnum.class, true));
    }

    private static Stream<Arguments> nonFinalClassProvider() {
        return Stream.of(Arguments.of("ArrayList is a regular (non‑final) class", java.util.ArrayList.class, false), Arguments.of("Object is not declared final", Object.class, false));
    }

    private static Stream<Arguments> interfaceProvider() {
        return Stream.of(Arguments.of("java.util.List is an interface", java.util.List.class, false));
    }

    private static Stream<Arguments> abstractClassProvider() {
        return Stream.of(Arguments.of("java.util.AbstractList is abstract", java.util.AbstractList.class, false));
    }

    private static Stream<Arguments> primitiveProvider() {
        return Stream.of(Arguments.of("int primitive type", int.class, false), Arguments.of("void primitive type", void.class, false));
    }

    private static Stream<Arguments> arrayProvider() {
        return Stream.of(Arguments.of("int[] array class", int[].class, false), Arguments.of("String[] array class", String[].class, false));
    }

    private static Stream<Arguments> customFinalProvider() {
        return Stream.of(Arguments.of("User‑defined final class", CustomFinalClass.class, true));
    }

    @Test
    @DisplayName("Final class should be reported as final")
    void testFinalClassIsDetected() {
        assertTrue(ClassUtilities.isClassFinal(FinalTestClass.class), "FinalTestClass is declared final and should be reported as such.");
    }

    @Test
    @DisplayName("Non‑final class should not be reported as final")
    void testNonFinalClassIsDetected() {
        assertFalse(ClassUtilities.isClassFinal(NonFinalTestClass.class), "NonFinalTestClass is not declared final and should be reported as non‑final.");
    }

    @Test
    @DisplayName("Interface should not be reported as final")
    void testInterfaceIsNotFinal() {
        assertFalse(ClassUtilities.isClassFinal(TestInterface.class), "Interfaces are implicitly abstract, not final.");
    }

    @Test
    @DisplayName("Enum type should be reported as final")
    void testEnumIsFinal() {
        assertTrue(ClassUtilities.isClassFinal(TestEnum.class), "Enum types are implicitly final.");
    }

    @Test
    @DisplayName("Array class should not be reported as final")
    void testArrayClassIsNotFinal() {
        Class<?> arrayClass = int[].class;
        assertFalse(ClassUtilities.isClassFinal(arrayClass), "Array classes have no FINAL modifier.");
    }

    @Test
    @DisplayName("Primitive type class should not be reported as final")
    void testPrimitiveClassIsNotFinal() {
        assertFalse(ClassUtilities.isClassFinal(int.class), "Primitive type classes have no FINAL modifier.");
    }

    @Test
    @DisplayName("Abstract class should not be reported as final")
    void testAbstractClassIsNotFinal() {
        assertFalse(ClassUtilities.isClassFinal(AbstractTestClass.class), "Abstract classes are not final.");
    }

    // ----- Final class detection -------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("finalClassProvider")
    @DisplayName("Final class detection")
    void testFinalClassDetection(String description, Class<?> clazz, boolean expected) {
        assertEquals(expected, ClassUtilities.isClassFinal(clazz), description);
    }

    // ----- Non‑final class detection ---------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nonFinalClassProvider")
    @DisplayName("Non‑final class detection")
    void testNonFinalClassDetection(String description, Class<?> clazz, boolean expected) {
        assertEquals(expected, ClassUtilities.isClassFinal(clazz), description);
    }

    // ----- Interface detection ----------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("interfaceProvider")
    @DisplayName("Interface detection")
    void testInterfaceDetection(String description, Class<?> clazz, boolean expected) {
        assertEquals(expected, ClassUtilities.isClassFinal(clazz), description);
    }

    // ----- Abstract class detection -----------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("abstractClassProvider")
    @DisplayName("Abstract class detection")
    void testAbstractClassDetection(String description, Class<?> clazz, boolean expected) {
        assertEquals(expected, ClassUtilities.isClassFinal(clazz), description);
    }

    // ----- Primitive type detection ------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("primitiveProvider")
    @DisplayName("Primitive type detection")
    void testPrimitiveDetection(String description, Class<?> clazz, boolean expected) {
        assertEquals(expected, ClassUtilities.isClassFinal(clazz), description);
    }

    // ----- Array class detection --------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("arrayProvider")
    @DisplayName("Array class detection")
    void testArrayDetection(String description, Class<?> clazz, boolean expected) {
        assertEquals(expected, ClassUtilities.isClassFinal(clazz), description);
    }

    // ----- Null input handling ----------------------------------------------------
    @Test
    @DisplayName("Null input should throw NullPointerException")
    void testNullInputHandling() {
        assertThrows(NullPointerException.class, () -> ClassUtilities.isClassFinal(null), "Passing null should result in a NullPointerException.");
    }

    // ----- Custom final class detection -------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("customFinalProvider")
    @DisplayName("Custom final class detection")
    void testCustomFinalClassDetection(String description, Class<?> clazz, boolean expected) {
        assertEquals(expected, ClassUtilities.isClassFinal(clazz), description);
    }

    // ----- Security exception handling --------------------------------------------
    @Test
    @DisplayName("SecurityException from Class#getModifiers should propagate")
    void testSecurityExceptionPropagation() {
        // Mockito can mock final classes when the inline mock maker is on the classpath.
        @SuppressWarnings("unchecked")
        Class<?> restrictedMock = Mockito.mock(Class.class);
        Mockito.when(restrictedMock.getModifiers()).thenThrow(new SecurityException("Access denied by SecurityManager"));
        assertThrows(SecurityException.class, () -> ClassUtilities.isClassFinal(restrictedMock), "SecurityException should propagate from isClassFinal.");
    }
}
