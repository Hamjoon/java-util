package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toString_Test {

    /**
     * Mocked {@link Converter} – should remain unused by the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple POJO with a custom {@code toString()} implementation for testing.
     */
    private static final class Dummy {

        private final String value;

        Dummy(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Dummy[" + value + "]";
        }
    }

    /**
     * Simple POJO with a custom {@code toString()} implementation.
     */
    private static final class CustomObject {

        @Override
        public String toString() {
            return "CustomObject";
        }
    }

    /**
     * Returns {@code null} from {@code toString()}.
     */
    private static final class NullToStringObject {

        @Override
        public String toString() {
            return null;
        }
    }

    /**
     * Throws a {@link RuntimeException} from {@code toString()}.
     */
    private static final class ExceptionToStringObject {

        @Override
        public String toString() {
            throw new RuntimeException("forced exception");
        }
    }

    /**
     * Throws a {@link NullPointerException} from {@code toString()}.
     */
    private static final class NullPointerExceptionObject {

        @Override
        public String toString() {
            throw new NullPointerException("forced NPE");
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} from {@code toString()}.
     */
    private static final class IllegalArgumentExceptionObject {

        @Override
        public String toString() {
            throw new IllegalArgumentException("forced IAE");
        }
    }

    /**
     * Throws an {@link AssertionError} (subclass of {@link Error}) from {@code toString()}.
     */
    private static final class AssertionErrorObject {

        @Override
        public String toString() {
            throw new AssertionError("forced AssertionError");
        }
    }

    /**
     * Returns an empty string from {@code toString()}.
     */
    private static final class EmptyToStringObject {

        @Override
        public String toString() {
            return "";
        }
    }

    /**
     * Dummy {@link Converter} implementation used when a non‑null converter is required.
     */
    private static final class DummyConverter implements Converter {
        // No methods are needed for the test – the interface is empty in this context.
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the mock was never interacted with.
        Mockito.verifyNoInteractions(mockConverter);
    }

    static Stream<Arguments> nullInputProvider() {
        return Stream.of(Arguments.of(null, null));
    }

    static Stream<Arguments> stringInputProvider() {
        return Stream.of(Arguments.of("hello world", null));
    }

    static Stream<Arguments> primitiveWrapperProvider() {
        return Stream.of(Arguments.of(123, "123"), Arguments.of(Boolean.TRUE, "true"), Arguments.of(3.14, "3.14"));
    }

    static Stream<Arguments> otherPrimitiveWrapperProvider() {
        return Stream.of(Arguments.of(1234567890123L, "1234567890123"), Arguments.of(Byte.valueOf((byte) 127), "127"), Arguments.of(Character.valueOf('c'), "c"), Arguments.of(Float.valueOf(2.71f), "2.71"), Arguments.of(new BigInteger("12345678901234567890"), "12345678901234567890"), Arguments.of(new BigDecimal("12345.6789"), "12345.6789"), Arguments.of(new AtomicInteger(5), "5"));
    }

    static Stream<Arguments> nullConverterProvider() {
        return Stream.of(Arguments.of(42, null, "42"), Arguments.of("test", new DummyConverter(), "test"));
    }

    @Test
    void toString_returnsStringRepresentation_ofString() {
        // GIVEN
        String source = "hello world";
        // WHEN
        String result = CharacterConversions.toString(source, mockConverter);
        // THEN
        assertEquals("hello world", result);
    }

    @Test
    void toString_returnsStringRepresentation_ofInteger() {
        // GIVEN
        Integer source = 42;
        // WHEN
        String result = CharacterConversions.toString(source, mockConverter);
        // THEN
        assertEquals("42", result);
    }

    @Test
    void toString_returnsStringRepresentation_ofCustomObject() {
        // GIVEN
        Object source = new Dummy("dummy-value");
        // WHEN
        String result = CharacterConversions.toString(source, mockConverter);
        // THEN
        assertEquals("Dummy[dummy-value]", result);
    }

    @Test
    void toString_returnsStringLiteral_null() {
        // GIVEN
        Object source = null;
        // WHEN
        String result = CharacterConversions.toString(source, mockConverter);
        // THEN
        // Concatenating null with an empty string yields the literal "null"
        assertEquals("null", result);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("nullInputProvider")
    void toString_nullInput(Object from, Converter converter) {
        String result = CharacterConversions.toString(from, converter);
        assertEquals("null", result);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] from=\"{0}\"")
    @MethodSource("stringInputProvider")
    void toString_stringInput(String from, Converter converter) {
        String result = CharacterConversions.toString(from, converter);
        assertEquals(from, result);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] from={0} ({1})")
    @MethodSource("primitiveWrapperProvider")
    void toString_primitiveWrapperInput(Object from, String expected) {
        String result = CharacterConversions.toString(from, mockConverter);
        assertEquals(expected, result);
    }

    // -----------------------------------------------------------------
    @Test
    void toString_customObjectInput() {
        Object from = new CustomObject();
        String result = CharacterConversions.toString(from, mockConverter);
        assertEquals("CustomObject", result);
    }

    // -----------------------------------------------------------------
    @Test
    void toString_defaultObjectInput() {
        Object from = new Object();
        String result = CharacterConversions.toString(from, mockConverter);
        assertTrue(result.startsWith("java.lang.Object@"), "Result should start with class name and '@' – got: " + result);
    }

    // -----------------------------------------------------------------
    @Test
    void toString_arrayInput() {
        int[] from = new int[] { 1, 2, 3 };
        String result = CharacterConversions.toString(from, mockConverter);
        assertTrue(result.startsWith("[I@"), "Result should start with \"[I@\" – got: " + result);
    }

    // -----------------------------------------------------------------
    @Test
    void toString_converterIgnored() {
        String from = "ignored";
        String result = CharacterConversions.toString(from, mockConverter);
        assertEquals("ignored", result);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] from={0} ({1})")
    @MethodSource("otherPrimitiveWrapperProvider")
    void toString_otherPrimitiveWrapperInput(Object from, String expected) {
        String result = CharacterConversions.toString(from, mockConverter);
        assertEquals(expected, result);
    }

    // -----------------------------------------------------------------
    @Test
    void toString_returnsNullLiteralWhenToStringIsNull() {
        Object from = new NullToStringObject();
        String result = CharacterConversions.toString(from, mockConverter);
        assertEquals("null", result);
    }

    // -----------------------------------------------------------------
    @Test
    void toString_propagatesRuntimeException() {
        Object from = new ExceptionToStringObject();
        assertThrows(RuntimeException.class, () -> CharacterConversions.toString(from, mockConverter));
    }

    // -----------------------------------------------------------------
    @Test
    void toString_multiDimensionalArrayInput() {
        int[][] from = new int[][] { { 1, 2 }, { 3, 4 } };
        String result = CharacterConversions.toString(from, mockConverter);
        assertTrue(result.startsWith("[[I@"), "Result should start with \"[[I@\" – got: " + result);
    }

    // -----------------------------------------------------------------
    @Test
    void toString_emptyToStringObject() {
        Object from = new EmptyToStringObject();
        String result = CharacterConversions.toString(from, mockConverter);
        assertEquals("", result);
    }

    // -----------------------------------------------------------------
    @Test
    void toString_stringBuilderInput() {
        StringBuilder from = new StringBuilder("sb content");
        String result = CharacterConversions.toString(from, mockConverter);
        assertEquals("sb content", result);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("nullConverterProvider")
    void toString_nullConverterNonNullFrom(Object from, Converter converter, String expected) {
        String result = CharacterConversions.toString(from, converter);
        assertEquals(expected, result);
    }

    // -----------------------------------------------------------------
    @Test
    void toString_propagatesNullPointerException() {
        Object from = new NullPointerExceptionObject();
        assertThrows(NullPointerException.class, () -> CharacterConversions.toString(from, mockConverter));
    }

    // -----------------------------------------------------------------
    @Test
    void toString_propagatesIllegalArgumentException() {
        Object from = new IllegalArgumentExceptionObject();
        assertThrows(IllegalArgumentException.class, () -> CharacterConversions.toString(from, mockConverter));
    }

    // -----------------------------------------------------------------
    @Test
    void toString_propagatesAssertionError() {
        Object from = new AssertionErrorObject();
        assertThrows(AssertionError.class, () -> CharacterConversions.toString(from, mockConverter));
    }
}
