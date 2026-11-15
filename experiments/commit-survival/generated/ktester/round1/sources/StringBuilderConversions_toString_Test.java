package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.time.DayOfWeek;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringBuilderConversions_toString_Test {

    /**
     * Mocked Converter – the method under test does not use it, but it must be supplied.
     */
    @Mock
    private Converter converter;

    /**
     * An example object whose {@code toString()} implementation will be exercised.
     */
    private StringBuilder sampleObject;

    /**
     * Simple class that overrides {@code toString()}
     */
    static final class CustomToString {

        private final String value;

        CustomToString(String v) {
            this.value = v;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * {@code toString()} throws a generic {@link RuntimeException}
     */
    static final class BadToString {

        @Override
        public String toString() {
            throw new RuntimeException("Bad toString");
        }
    }

    /**
     * {@code toString()} returns {@code null}
     */
    static final class NullString {

        @Override
        public String toString() {
            return null;
        }
    }

    /**
     * {@code toString()} returns an empty string
     */
    static final class EmptyStringToString {

        @Override
        public String toString() {
            return "";
        }
    }

    /**
     * {@code toString()} throws a {@link NullPointerException}
     */
    static final class NPEInToString {

        @Override
        public String toString() {
            throw new NullPointerException("NPE in toString");
        }
    }

    /**
     * {@code toString()} throws an {@link AssertionError} (subclass of {@link Error})
     */
    static final class ErrorInToString {

        @Override
        public String toString() {
            throw new AssertionError("Error in toString");
        }
    }

    /**
     * A dummy {@link Converter} implementation – does nothing.
     */
    static final class DummyConverter implements Converter {
        // No methods are required for the current tests; the interface is empty in the production code.
    }

    /**
     * A {@link Converter} that explodes if any method is invoked.
     */
    static final class ExplodingConverter implements Converter {
        // All methods (if any) would throw – we rely on Mockito to create such a mock.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh sample object before each test.
        sampleObject = new StringBuilder("sample text");
    }

    static Stream<Arguments> standardObjectsProvider() {
        return Stream.of(arguments("Hello", "Hello", null), arguments(Integer.valueOf(123), "123", null), arguments(new CustomToString("Custom"), "Custom", null));
    }

    static Stream<Arguments> primitiveWrappersProvider() {
        return Stream.of(arguments(Double.valueOf(3.14), "3.14"), arguments(Boolean.TRUE, "true"), arguments(Character.valueOf('A'), "A"));
    }

    @Test
    void toStringReturnsObjectToString() {
        // GIVEN – a non‑null object and a (mocked) Converter
        // WHEN
        String result = StringBuilderConversions.toString(sampleObject, converter);
        // THEN
        assertEquals(sampleObject.toString(), result, "The method should return the result of the object's toString() method.");
    }

    @Test
    void toStringThrowsNullPointerExceptionWhenObjectIsNull() {
        // GIVEN – a null source object
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringBuilderConversions.toString(null, converter), "Calling toString with a null source should throw NullPointerException.");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("standardObjectsProvider")
    void standardObjects_toString(Object from, String expected, Converter conv) {
        // The converter argument is ignored – we pass the supplied one (may be null).
        String result = StringBuilderConversions.toString(from, conv);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] wrapper={0}")
    @MethodSource("primitiveWrappersProvider")
    void primitiveWrappers_toString(Object from, String expected) {
        String result = StringBuilderConversions.toString(from, converter);
        assertEquals(expected, result);
    }

    @Test
    void array_intArray_toString() {
        int[] arr = new int[] { 1, 2, 3 };
        String result = StringBuilderConversions.toString(arr, converter);
        assertTrue(result.startsWith("[I@"), "int[] default toString() should start with \"[I@\" – got: " + result);
    }

    @Test
    void array_stringArray_toString() {
        String[] arr = new String[] { "a", "b" };
        String result = StringBuilderConversions.toString(arr, converter);
        assertTrue(result.startsWith("[Ljava.lang.String;@"), "String[] default toString() should start with \"[Ljava.lang.String;@\" – got: " + result);
    }

    @Test
    void enum_toString() {
        DayOfWeek day = DayOfWeek.MONDAY;
        String result = StringBuilderConversions.toString(day, converter);
        assertEquals("MONDAY", result);
    }

    @Test
    void converterArgumentIgnored_nonNullConverter() {
        String from = "Ignored";
        // Use a real (non‑mock) dummy converter to prove it is not consulted.
        DummyConverter dummy = new DummyConverter();
        String result = StringBuilderConversions.toString(from, dummy);
        assertEquals("Ignored", result);
    }

    @Test
    void converterArgumentIgnored_explodingConverter() {
        String from = "No conversion needed";
        // Create a mock that throws on any interaction – the method under test must never call it.
        Converter exploding = Mockito.mock(Converter.class, invocation -> {
            throw new IllegalStateException("Converter should not be invoked");
        });
        String result = StringBuilderConversions.toString(from, exploding);
        assertEquals("No conversion needed", result);
    }

    @Test
    void toStringReturnsNull_whenObjectToStringReturnsNull() {
        NullString obj = new NullString();
        String result = StringBuilderConversions.toString(obj, converter);
        assertNull(result, "Method should return null when object's toString() returns null");
    }

    @Test
    void toStringReturnsEmptyString_whenObjectToStringReturnsEmpty() {
        EmptyStringToString obj = new EmptyStringToString();
        String result = StringBuilderConversions.toString(obj, converter);
        assertEquals("", result, "Method should return the empty string unchanged");
    }

    @Test
    void toStringPropagatesRuntimeException_fromBadToString() {
        BadToString obj = new BadToString();
        assertThrows(RuntimeException.class, () -> StringBuilderConversions.toString(obj, converter), "RuntimeException from object's toString() must propagate");
    }

    @Test
    void toStringPropagatesNullPointerException_fromNPEInToString() {
        NPEInToString obj = new NPEInToString();
        assertThrows(NullPointerException.class, () -> StringBuilderConversions.toString(obj, converter), "NullPointerException from object's toString() must propagate");
    }

    @Test
    void toStringPropagatesError_fromErrorInToString() {
        ErrorInToString obj = new ErrorInToString();
        assertThrows(AssertionError.class, () -> StringBuilderConversions.toString(obj, converter), "Error from object's toString() must propagate unchanged");
    }

    @Test
    void stringBuilder_toString() {
        StringBuilder sb = new StringBuilder("builder");
        String result = StringBuilderConversions.toString(sb, converter);
        assertEquals("builder", result);
    }

    @Test
    void stringBuffer_toString() {
        StringBuffer sbuf = new StringBuffer("buffer");
        String result = StringBuilderConversions.toString(sbuf, converter);
        assertEquals("buffer", result);
    }
}
