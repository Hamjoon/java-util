package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Type;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringBufferConversions_toString_Test {

    /**
     * A mock {@link Converter} – the implementation is irrelevant for the method under test.
     */
    @Mock
    private Converter mockConverter;

    /* -------------------------------------------------------------
     *  Helper – a Converter implementation that throws on any call.
     * ------------------------------------------------------------- */
    private static class ThrowingConverter implements Converter {

        @Override
        public Object convert(Object src, Class<?> target) {
            throw new IllegalStateException("Should not be called");
        }

        @Override
        public <T> T convert(Object src, Type target) {
            throw new IllegalStateException("Should not be called");
        }
    }

    private static Stream<Arguments> providePrimaryCases() {
        return Stream.of(// 1. Regular String, converter = null
        arguments("Verify that toString returns the result of Object.toString() for a regular String instance, even when the Converter argument is null.", "simple string", null, "simple string"), // 2. StringBuilder, converter = null
        arguments("Confirm that toString delegates to the object's toString method for a StringBuilder instance.", new StringBuilder("builder"), null, "builder"), // 3. Plain Object – default Object.toString()
        arguments("Ensure that toString returns the default Object.toString() representation (class name + hash code) for a plain Object.", new Object(), null, Pattern.compile("java\\.lang\\.Object@.*")), // 4. Null source – should throw NPE
        arguments("Verify that passing a null 'from' argument throws NullPointerException because the method invokes from.toString().", null, null, NullPointerException.class), // 5. Converter supplied (throws if used) – should be ignored
        arguments("Check that the Converter argument is ignored; the method still returns from.toString().", "custom", new ThrowingConverter(), "custom"));
    }

    private static Stream<Arguments> provideAdditionalCases() {
        return Stream.of(// 1. StringBuffer, converter = null
        arguments("Verify that a StringBuffer instance is correctly converted by delegating to its own toString() implementation.", new StringBuffer("buffer"), null, "buffer"), // 2. Integer wrapper, converter = null
        arguments("Check that a wrapper type (Integer) is handled by returning the result of Integer.toString().", Integer.valueOf(42), null, "42"), // 3. Primitive array, converter = null – default Object.toString()
        arguments("Ensure that an array (which inherits Object.toString()) yields the default class‑name@hash representation.", new int[] { 1, 2, 3 }, null, Pattern.compile("\\[I@.*")), // 4. List, converter = null
        arguments("Validate that a collection’s toString() result (list format) is returned unchanged.", List.of("a", "b"), null, "[a, b]"), // 5. Anonymous class overriding toString()
        arguments("Confirm that a custom anonymous class overriding toString() is respected.", new Object() {

            @Override
            public String toString() {
                return "overridden";
            }
        }, null, "overridden"), // 6. toString() throws RuntimeException – should propagate
        arguments("Test that an exception thrown from the object's toString() propagates out of the method.", new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("toString failure");
            }
        }, null, RuntimeException.class), // 7. Converter that throws if any method is called – should be ignored
        arguments("Provide a Converter implementation that throws if any of its methods are invoked; the method should still return the object's toString() result, proving the converter argument is ignored.", "ignoreConverter", new ThrowingConverter(), "ignoreConverter"));
    }

    @Test
    @DisplayName("toString returns the object's toString() result")
    void toString_returnsObjectToString() {
        // GIVEN
        StringBuilder sb = new StringBuilder("hello");
        // WHEN
        String result = StringBufferConversions.toString(sb, mockConverter);
        // THEN
        assertEquals("hello", result);
    }

    @Test
    @DisplayName("toString works with a custom object overriding toString()")
    void toString_customObject() {
        // GIVEN
        Object custom = new Object() {

            @Override
            public String toString() {
                return "customString";
            }
        };
        // WHEN
        String result = StringBufferConversions.toString(custom, mockConverter);
        // THEN
        assertEquals("customString", result);
    }

    @Test
    @DisplayName("toString throws NullPointerException when source is null")
    void toString_nullSource_throwsNPE() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringBufferConversions.toString(nullObj, mockConverter));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("providePrimaryCases")
    void toString_primaryGroup(String description, Object from, Converter converter, Object expected) {
        if (expected instanceof Class) {
            // Expected an exception.
            assertThrows((Class<? extends Throwable>) expected, () -> StringBufferConversions.toString(from, converter), description);
        } else if (expected instanceof Pattern) {
            // Expected a regex match.
            String result = StringBufferConversions.toString(from, converter);
            assertTrue(((Pattern) expected).matcher(result).matches(), description + " – result: " + result);
        } else {
            // Expected a concrete string result.
            String result = StringBufferConversions.toString(from, converter);
            assertEquals(expected, result, description);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideAdditionalCases")
    void toString_additionalGroup(String description, Object from, Converter converter, Object expected) {
        if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> StringBufferConversions.toString(from, converter), description);
        } else if (expected instanceof Pattern) {
            String result = StringBufferConversions.toString(from, converter);
            assertTrue(((Pattern) expected).matcher(result).matches(), description + " – result: " + result);
        } else {
            String result = StringBufferConversions.toString(from, converter);
            assertEquals(expected, result, description);
        }
    }
}
