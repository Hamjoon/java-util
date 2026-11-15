package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Stream;

class StringConversions_asString_Test {

    class Custom {

        @Override
        public String toString() {
            return "custom-value";
        }
    }

    class CustomObject {

        @Override
        public String toString() {
            return "custom";
        }
    }

    class NullToStringObject {

        @Override
        public String toString() {
            return null;
        }
    }

    class EmptyToStringObject {

        @Override
        public String toString() {
            return "";
        }
    }

    class ThrowingIllegalStateObject {

        @Override
        public String toString() {
            throw new IllegalStateException("boom");
        }
    }

    class NullPointerThrower {

        @Override
        public String toString() {
            throw new NullPointerException("null pointer");
        }
    }

    class RuntimeThrower {

        @Override
        public String toString() {
            throw new RuntimeException("runtime");
        }
    }

    class AssertionErrorThrower {

        @Override
        public String toString() {
            throw new AssertionError("assertion");
        }
    }

    class StackOverflowThrower {

        @Override
        public String toString() {
            throw new StackOverflowError("stack overflow");
        }
    }

    class ThrowingIllegalStateObject {

        @Override
        public String toString() {
            throw new IllegalStateException("boom");
        }
    }

    class CustomObject {

        @Override
        public String toString() {
            return "custom";
        }
    }

    class NullToStringObject {

        @Override
        public String toString() {
            return null;
        }
    }

    class EmptyToStringObject {

        @Override
        public String toString() {
            return "";
        }
    }

    class ThrowingIllegalStateObject {

        @Override
        public String toString() {
            throw new IllegalStateException("boom");
        }
    }

    class NullPointerThrower {

        @Override
        public String toString() {
            throw new NullPointerException("null pointer");
        }
    }

    class RuntimeThrower {

        @Override
        public String toString() {
            throw new RuntimeException("runtime");
        }
    }

    class AssertionErrorThrower {

        @Override
        public String toString() {
            throw new AssertionError("assertion");
        }
    }

    class StackOverflowThrower {

        @Override
        public String toString() {
            throw new StackOverflowError("stack overflow");
        }
    }

    class ThrowingIllegalStateObject {

        @Override
        public String toString() {
            throw new IllegalStateException("boom");
        }
    }

    private static Stream<Arguments> normalCasesProvider() {
        // Helper objects used in several test cases
        class CustomObject {

            @Override
            public String toString() {
                return "custom";
            }
        }
        class NullToStringObject {

            @Override
            public String toString() {
                return null;
            }
        }
        class EmptyToStringObject {

            @Override
            public String toString() {
                return "";
            }
        }
        class ThrowingIllegalStateObject {

            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        }
        // Proxy that returns a custom string from toString()
        InvocationHandler handler = (proxy, method, args) -> {
            if ("toString".equals(method.getName())) {
                return "proxyString";
            }
            return null;
        };
        Object proxy = Proxy.newProxyInstance(StringConversions.class.getClassLoader(), new Class<?>[] { java.io.Serializable.class }, handler);
        // Build the argument stream
        return Stream.of(// null input
        Arguments.of("When the input is null the method should return null (null‑branch).", null, null), // Integer
        Arguments.of("A non‑null Integer should be converted by delegating to Integer.toString().", 123, "123"), // Custom object with overridden toString()
        Arguments.of("Object with an overridden toString() returning \"custom\" – verifies that the method uses the object's own toString implementation.", new CustomObject(), "custom"), // int array – default Object.toString()
        Arguments.of("An array does not have a friendly toString; the method should return the default representation (class name + '@' + hash).", new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }.toString()), // Plain String
        Arguments.of("A String object's toString() returns itself; the method should return the original string.", "test", "test"), // Boolean.TRUE
        Arguments.of("A Boolean true should be converted using Boolean.toString(), yielding \"true\".", Boolean.TRUE, "true"), // Double
        Arguments.of("A Double instance should be converted via Double.toString(), preserving the decimal representation.", Double.valueOf(3.14), "3.14"), // Enum constant
        Arguments.of("An enum constant should be converted using Enum.toString(), which returns the constant name.", DayOfWeek.MONDAY, "MONDAY"), // Object whose toString() returns null
        Arguments.of("When an object's toString() returns null, the method should propagate that null result.", new NullToStringObject(), null), // Object whose toString() returns empty string
        Arguments.of("An object whose toString() returns an empty string should cause asString() to return the empty string.", new EmptyToStringObject(), ""), // StringBuilder
        Arguments.of("StringBuilder implements CharSequence and its toString() returns the built string.", new StringBuilder("builder"), "builder"), // java.util.Date
        Arguments.of("A java.util.Date instance should be converted using Date.toString(); the exact format depends on the JVM locale, so the expected value is obtained by calling toString() at runtime.", new Date(0L), new Date(0L).toString()), // LocalDateTime
        Arguments.of("A LocalDateTime's toString() returns the ISO‑8601 representation.", LocalDateTime.of(2023, 12, 31, 23, 59, 59), "2023-12-31T23:59:59"), // Dynamic proxy
        Arguments.of("A dynamic proxy that overrides toString() should return the proxy's custom string.", proxy, "proxyString"), // Object whose toString() throws IllegalStateException – expect propagation
        Arguments.of("If toString() throws an exception, asString() should let the exception propagate unchanged.", new ThrowingIllegalStateObject(), IllegalStateException.class));
    }

    private static Stream<Arguments> exceptionCasesProvider() {
        // Helper objects that throw different throwables from toString()
        class NullPointerThrower {

            @Override
            public String toString() {
                throw new NullPointerException("null pointer");
            }
        }
        class RuntimeThrower {

            @Override
            public String toString() {
                throw new RuntimeException("runtime");
            }
        }
        class AssertionErrorThrower {

            @Override
            public String toString() {
                throw new AssertionError("assertion");
            }
        }
        class StackOverflowThrower {

            @Override
            public String toString() {
                throw new StackOverflowError("stack overflow");
            }
        }
        return Stream.of(Arguments.of("When the object's toString() throws a NullPointerException, asString() should let the exception propagate unchanged.", new NullPointerThrower(), NullPointerException.class), Arguments.of("When the object's toString() throws a generic RuntimeException, asString() must propagate it without alteration.", new RuntimeThrower(), RuntimeException.class), Arguments.of("If toString() throws an AssertionError (an Error type), asString() should not catch it and the error should bubble up.", new AssertionErrorThrower(), AssertionError.class), Arguments.of("When toString() throws a StackOverflowError, asString() must allow the error to propagate.", new StackOverflowThrower(), StackOverflowError.class));
    }

    @Test
    @DisplayName("asString(null) should return null")
    void testAsStringWithNull() {
        // Given
        Object input = null;
        // When
        String result = StringConversions.asString(input);
        // Then
        assertNull(result, "Result should be null when input is null");
    }

    @Test
    @DisplayName("asString(non‑null) should return the object's toString() value")
    void testAsStringWithNonNull() {
        // Given
        Integer intObj = 42;
        String expectedInt = intObj.toString();
        // When
        String intResult = StringConversions.asString(intObj);
        // Then
        assertEquals(expectedInt, intResult, "Result should match Integer.toString()");
        // Additional verification with a custom object
        class Custom {

            @Override
            public String toString() {
                return "custom-value";
            }
        }
        Custom customObj = new Custom();
        // When
        String customResult = StringConversions.asString(customObj);
        // Then
        assertEquals("custom-value", customResult, "Result should match the overridden toString() of the custom object");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalCasesProvider")
    @DisplayName("asString – normal behaviour")
    void testAsStringNormal(String description, Object input, Object expected) {
        // When
        String result = StringConversions.asString(input);
        // Then
        if (expected == null) {
            assertNull(result, description);
        } else {
            assertEquals(expected, result, description);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionCasesProvider")
    @DisplayName("asString – exception propagation")
    void testAsStringExceptions(String description, Object input, Class<? extends Throwable> expectedException) {
        // When / Then
        Throwable thrown = assertThrows(expectedException, () -> StringConversions.asString(input), description);
        // Verify that the thrown exception is exactly the expected type (sub‑classing is allowed)
        assertTrue(expectedException.isInstance(thrown), description);
    }

    /* -------------------------------------------------------------
     *  Additional test to verify that a thrown IllegalStateException
     *  from toString() is propagated (covered in normalCasesProvider
     *  as a special case where the expected value is an exception class).
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("asString – IllegalStateException propagation")
    void testAsStringIllegalStateExceptionPropagation() {
        class ThrowingIllegalStateObject {

            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        }
        ThrowingIllegalStateObject obj = new ThrowingIllegalStateObject();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> StringConversions.asString(obj), "IllegalStateException should propagate unchanged");
        assertEquals("boom", ex.getMessage());
    }
}
