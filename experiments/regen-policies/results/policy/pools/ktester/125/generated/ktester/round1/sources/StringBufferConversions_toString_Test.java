package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringBufferConversions_toString_Test {

    /**
     * A mock {@link Converter} – it is not used by the method but satisfies the signature.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple holder for the regular test parameters.
     */
    private static class ArgumentsHolder {

        final Object from;

        // may be null – test will replace with mock if needed
        final Converter converter;

        final String expected;

        final String description;

        ArgumentsHolder(Object from, Converter converter, String expected, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.description = description;
        }

        // JUnit 5 requires a static method returning a Stream of arguments.
        // We use a custom method source that unwraps the holder.
        static Stream<org.junit.jupiter.params.provider.Arguments> toArguments(Stream<ArgumentsHolder> stream) {
            return stream.map(h -> org.junit.jupiter.params.provider.Arguments.of(h.from, h.converter, h.expected, h.description));
        }
    }

    /**
     * Holder for exception‑related test data.
     */
    private static class ExceptionHolder {

        final Object from;

        final Class<? extends Throwable> exceptionClass;

        final String message;

        final String description;

        ExceptionHolder(Object from, Class<? extends Throwable> exceptionClass, String message, String description) {
            this.from = from;
            this.exceptionClass = exceptionClass;
            this.message = message;
            this.description = description;
        }

        static Stream<org.junit.jupiter.params.provider.Arguments> toArguments(Stream<ExceptionHolder> stream) {
            return stream.map(e -> org.junit.jupiter.params.provider.Arguments.of(e.from, e.exceptionClass, e.message, e.description));
        }
    }

    /**
     * Minimal mock implementation of Converter – only needed to satisfy the signature.
     */
    private static class MockConverter implements Converter {
        // No methods are required for the current tests; this class exists solely
        // to allow non‑null Converter instances in test data.
    }

    /**
     * Data for the “Normal behavior” group.
     */
    private static Stream<ArgumentsHolder> normalBehaviorProvider() {
        return Stream.of(new ArgumentsHolder("simple string", null, "simple string", "When `from` is a regular String, the method should return the same string."), new ArgumentsHolder(new StringBuilder("builder"), new MockConverter(), "builder", "StringBuilder should be converted via its toString()."), new ArgumentsHolder(Integer.valueOf(42), null, "42", "Integer wrapper should return its toString representation."), new ArgumentsHolder(new Object() {

            @Override
            public String toString() {
                return "custom";
            }
        }, null, "custom", "Custom object with overridden toString."));
    }

    /**
     * Data for the “Converter ignored for other CharSequence types” group.
     */
    private static Stream<ArgumentsHolder> charSequenceProvider() {
        return Stream.of(new ArgumentsHolder(new StringBuffer("buffer"), new MockConverter(), "buffer", "StringBuffer should be converted correctly."), new ArgumentsHolder(java.nio.CharBuffer.wrap("charbuf"), new MockConverter(), "charbuf", "CharBuffer should be converted correctly."));
    }

    /**
     * Data for the “Various primitive wrapper and enum types” group.
     */
    private static Stream<ArgumentsHolder> primitiveWrapperProvider() {
        return Stream.of(new ArgumentsHolder(Boolean.TRUE, null, "true", "Boolean true."), new ArgumentsHolder(Double.valueOf(3.14), null, "3.14", "Double representation."), new ArgumentsHolder(SampleEnum.ONE, null, "ONE", "Enum constant name."));
    }

    /**
     * Data for the “Custom toString returning null” group.
     */
    private static Stream<ArgumentsHolder> nullResultProvider() {
        return Stream.of(new ArgumentsHolder(new Object() {

            @Override
            public String toString() {
                return null;
            }
        }, null, null, "toString() explicitly returns null."));
    }

    /**
     * Data for the “toString throws runtime exception” group.
     */
    private static Stream<ExceptionHolder> exceptionProvider() {
        return Stream.of(new ExceptionHolder(new Object() {

            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        }, IllegalStateException.class, "boom", "toString() throws IllegalStateException."), new ExceptionHolder(new Object() {

            @Override
            public String toString() {
                throw new NullPointerException("np");
            }
        }, NullPointerException.class, "np", "toString() throws NullPointerException."), new ExceptionHolder(new Object() {

            @Override
            public String toString() {
                throw new AssertionError("assert");
            }
        }, AssertionError.class, "assert", "toString() throws AssertionError."));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> normalBehaviorProvider() {
        return ArgumentsHolder.toArguments(normalBehaviorProviderRaw());
    }

    private static Stream<ArgumentsHolder> normalBehaviorProviderRaw() {
        return Stream.of(new ArgumentsHolder("simple string", null, "simple string", "When `from` is a regular String, the method should return the same string."), new ArgumentsHolder(new StringBuilder("builder"), new MockConverter(), "builder", "StringBuilder toString() result."), new ArgumentsHolder(Integer.valueOf(42), null, "42", "Integer wrapper toString() result."), new ArgumentsHolder(new Object() {

            @Override
            public String toString() {
                return "custom";
            }
        }, null, "custom", "Custom object's toString()."));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> charSequenceProvider() {
        return ArgumentsHolder.toArguments(charSequenceProviderRaw());
    }

    private static Stream<ArgumentsHolder> charSequenceProviderRaw() {
        return Stream.of(new ArgumentsHolder(new StringBuffer("buffer"), new MockConverter(), "buffer", "StringBuffer toString() result."), new ArgumentsHolder(java.nio.CharBuffer.wrap("charbuf"), new MockConverter(), "charbuf", "CharBuffer toString() result."));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> primitiveWrapperProvider() {
        return ArgumentsHolder.toArguments(primitiveWrapperProviderRaw());
    }

    private static Stream<ArgumentsHolder> primitiveWrapperProviderRaw() {
        return Stream.of(new ArgumentsHolder(Boolean.TRUE, null, "true", "Boolean true."), new ArgumentsHolder(Double.valueOf(3.14), null, "3.14", "Double value."), new ArgumentsHolder(SampleEnum.ONE, null, "ONE", "Enum constant."));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> nullResultProvider() {
        return ArgumentsHolder.toArguments(nullResultProviderRaw());
    }

    private static Stream<ArgumentsHolder> nullResultProviderRaw() {
        return Stream.of(new ArgumentsHolder(new Object() {

            @Override
            public String toString() {
                return null;
            }
        }, null, null, "toString() returns null."));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> exceptionProvider() {
        return ExceptionHolder.toArguments(exceptionProviderRaw());
    }

    private static Stream<ExceptionHolder> exceptionProviderRaw() {
        return Stream.of(new ExceptionHolder(new Object() {

            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        }, IllegalStateException.class, "boom", "IllegalStateException from toString()."), new ExceptionHolder(new Object() {

            @Override
            public String toString() {
                throw new NullPointerException("np");
            }
        }, NullPointerException.class, "np", "NullPointerException from toString()."), new ExceptionHolder(new Object() {

            @Override
            public String toString() {
                throw new AssertionError("assert");
            }
        }, AssertionError.class, "assert", "AssertionError from toString()."));
    }

    @Test
    void toString_WithString_ReturnsSameString() {
        // GIVEN
        String source = "Hello, World!";
        // WHEN
        String result = StringBufferConversions.toString(source, mockConverter);
        // THEN
        assertEquals(source, result, "The returned string should be identical to the source string.");
    }

    @Test
    void toString_WithInteger_ReturnsIntegerToString() {
        // GIVEN
        Integer source = 42;
        // WHEN
        String result = StringBufferConversions.toString(source, mockConverter);
        // THEN
        assertEquals("42", result, "The method should return the integer's toString representation.");
    }

    @Test
    void toString_WithCustomObject_UsesItsToString() {
        // GIVEN
        Object source = new Object() {

            @Override
            public String toString() {
                return "CustomToString";
            }
        };
        // WHEN
        String result = StringBufferConversions.toString(source, mockConverter);
        // THEN
        assertEquals("CustomToString", result, "The method must delegate to the object's overridden toString implementation.");
    }

    @Test
    void toString_WithNull_ThrowsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringBufferConversions.toString(source, mockConverter), "Calling toString with a null source should throw NullPointerException.");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("normalBehaviorProvider")
    void toString_NormalBehavior(Object from, Converter converter, String expected, String _description) {
        // WHEN
        String result = StringBufferConversions.toString(from, converter != null ? converter : mockConverter);
        // THEN
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("charSequenceProvider")
    void toString_CharSequenceTypes(Object from, Converter converter, String expected, String _description) {
        String result = StringBufferConversions.toString(from, converter != null ? converter : mockConverter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("primitiveWrapperProvider")
    void toString_PrimitiveWrappersAndEnum(Object from, Converter converter, String expected, String _description) {
        String result = StringBufferConversions.toString(from, converter != null ? converter : mockConverter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullResultProvider")
    void toString_CustomToStringReturnsNull(Object from, Converter converter, String expected, String _description) {
        String result = StringBufferConversions.toString(from, converter != null ? converter : mockConverter);
        assertNull(result, "Method should propagate a null result from the object's toString().");
    }

    @Test
    void toString_NullSource_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringBufferConversions.toString(null, mockConverter), "Calling toString with a null source must throw NullPointerException.");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionProvider")
    void toString_PropagatesExceptions(Object from, Class<? extends Throwable> exceptionClass, String message, String _description) {
        Throwable thrown = assertThrows(exceptionClass, () -> StringBufferConversions.toString(from, mockConverter));
        assertEquals(message, thrown.getMessage(), "Exception message should be preserved.");
    }
}
