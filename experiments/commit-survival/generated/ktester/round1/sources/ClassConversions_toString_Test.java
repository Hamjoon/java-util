package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassConversions_toString_Test {

    /**
     * Mocked Converter – the method under test does not use it, but it is required by the signature.
     */
    @Mock
    private Converter converter;

    /**
     * Mocked Converter – the method under test does not use it, but it is required by the signature.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Supplies all successful test cases (i.e. those that should return a
     * non‑null string).  The {@code Converter} argument is either {@code null}
     * or a Mockito mock (created here to avoid dependence on the real
     * implementation).
     */
    static Stream<Arguments> provideSuccessfulToStringCases() {
        // A mock Converter that will be used for the cases where a non‑null
        // converter is required.  The method under test never touches it.
        Converter dummyConverter = Mockito.mock(Converter.class);
        return Stream.of(// --- basic cases -------------------------------------------------
        Arguments.of(String.class, null, "java.lang.String"), Arguments.of(int[].class, null, "[I"), Arguments.of(java.util.List.class, dummyConverter, "java.util.List"), // --- primitives --------------------------------------------------
        Arguments.of(int.class, null, "int"), Arguments.of(void.class, null, "void"), // --- annotation ---------------------------------------------------
        Arguments.of(java.lang.annotation.Retention.class, null, "java.lang.annotation.Retention"), // --- enum --------------------------------------------------------
        Arguments.of(java.time.DayOfWeek.class, null, "java.time.DayOfWeek"), // --- inner interface (uses '$' in the name) --------------------
        Arguments.of(java.util.Map.Entry.class, null, "java.util.Map$Entry"), // --- multi‑dimensional object array ------------------------------
        Arguments.of(String[][].class, null, "[[Ljava.lang.String;"), // --- primitive array ------------------------------------------------
        Arguments.of(double[].class, null, "[D"), Arguments.of(int[][].class, null, "[[I"), // --- same class with a non‑null converter -----------------------
        Arguments.of(String.class, dummyConverter, "java.lang.String"));
    }

    /**
     * Supplies all cases that are expected to throw an exception.
     */
    static Stream<Arguments> provideExceptionToStringCases() {
        Converter dummyConverter = Mockito.mock(Converter.class);
        return Stream.of(// Null 'from' argument – NPE when getName() is invoked on null.
        Arguments.of(null, null, NullPointerException.class), // Invalid type – ClassCastException on the cast to (Class<?>).
        Arguments.of(new Object(), null, ClassCastException.class), // Null 'from' with a non‑null converter – still NPE.
        Arguments.of(null, dummyConverter, NullPointerException.class), // Invalid type with a non‑null converter – still ClassCastException.
        Arguments.of(new Object(), dummyConverter, ClassCastException.class));
    }

    @Test
    void toStringReturnsFullyQualifiedClassName() {
        // given
        Class<?> clazz = java.util.ArrayList.class;
        // when
        String result = ClassConversions.toString(clazz, converter);
        // then
        assertEquals("java.util.ArrayList", result);
    }

    @Test
    void toStringReturnsPrimitiveClassName() {
        // given
        Class<?> primitiveClazz = int.class;
        // when
        String result = ClassConversions.toString(primitiveClazz, converter);
        // then
        assertEquals("int", result);
    }

    @Test
    void toStringThrowsClassCastExceptionWhenArgumentIsNotAClass() {
        // given
        Object notAClass = "I am not a Class object";
        // when / then
        assertThrows(ClassCastException.class, () -> ClassConversions.toString(notAClass, converter));
    }

    /**
     * Parameterized test that verifies the correct string representation is returned
     * for a wide range of {@code Class} objects.  The {@code converter} argument is
     * exercised with both {@code null} and a mock instance to prove it is ignored.
     */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("provideSuccessfulToStringCases")
    void toStringReturnsExpectedName(Object from, Converter converter, String expected) {
        // when
        String result = ClassConversions.toString(from, converter);
        // then
        assertEquals(expected, result);
    }

    /**
     * Parameterized test that verifies the method throws the expected exception
     * when the {@code from} argument cannot be cast to {@code Class<?>}.
     */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("provideExceptionToStringCases")
    void toStringThrowsExpectedException(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> ClassConversions.toString(from, converter));
    }
}
