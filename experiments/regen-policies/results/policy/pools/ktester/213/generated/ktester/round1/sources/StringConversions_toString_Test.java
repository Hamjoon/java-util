package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.DayOfWeek;
import java.util.UUID;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toString_Test {

    @Mock
    private Converter // injected by MockitoExtension
    mockConverter;

    class Custom {

        @Override
        public String toString() {
            return "custom-value";
        }
    }

    /**
     * Custom object with a specific toString() result.
     */
    static class CustomToString {

        @Override
        public String toString() {
            return "custom-result";
        }
    }

    /**
     * toString() that throws IllegalStateException.
     */
    static class ToStringThrows {

        @Override
        public String toString() {
            throw new IllegalStateException("toString failure");
        }
    }

    /**
     * toString() that returns an empty string.
     */
    static class EmptyStringToString {

        @Override
        public String toString() {
            return "";
        }
    }

    /**
     * toString() that returns null.
     */
    static class NullToString {

        @Override
        public String toString() {
            return null;
        }
    }

    /**
     * toString() that throws RuntimeException.
     */
    static class ToStringRuntimeException {

        @Override
        public String toString() {
            throw new RuntimeException("runtime failure");
        }
    }

    /**
     * toString() that throws NullPointerException.
     */
    static class NPEToString {

        @Override
        public String toString() {
            throw new NullPointerException("explicit NPE from toString");
        }
    }

    /**
     * toString() that throws IllegalArgumentException.
     */
    static class IllegalArgumentToString {

        @Override
        public String toString() {
            throw new IllegalArgumentException("illegal argument in toString");
        }
    }

    /**
     * toString() that throws AssertionError.
     */
    static class AssertionErrorToString {

        @Override
        public String toString() {
            throw new AssertionError("assertion failed in toString");
        }
    }

    /**
     * Custom unchecked exception used in tests.
     */
    static class MyRuntimeException extends RuntimeException {

        MyRuntimeException(String message) {
            super(message);
        }
    }

    /**
     * toString() that throws the custom unchecked exception.
     */
    static class CustomRuntimeExceptionToString {

        @Override
        public String toString() {
            throw new MyRuntimeException("custom runtime failure");
        }
    }

    /**
     * toString() that throws StackOverflowError.
     */
    static class StackOverflowToString {

        @Override
        public String toString() {
            throw new StackOverflowError("simulated stack overflow");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates and injects the mockConverter automatically.
        // Ensure a clean state before each test.
        reset(mockConverter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the converter was not used in any test case.
        verifyNoInteractions(mockConverter);
    }

    private static Stream<Arguments> normalReturnCases() {
        return Stream.of(// null input – already covered by a dedicated test, but kept for completeness
        Arguments.of(null, null, "null input should return null"), // primitive wrappers and simple objects
        Arguments.of(123, "123", "Integer wrapper"), Arguments.of("Hello, World!", "Hello, World!", "String instance"), Arguments.of(45.67, "45.67", "Double instance"), Arguments.of(true, "true", "Boolean true"), // custom object with overridden toString()
        Arguments.of(new CustomToString(), "custom-result", "Custom object with overridden toString()"), // object whose toString() returns empty string
        Arguments.of(new EmptyStringToString(), "", "Object returning empty string"), // object whose toString() returns null
        Arguments.of(new NullToString(), null, "Object returning null from toString()"), // UUID instance
        Arguments.of(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), "123e4567-e89b-12d3-a456-426614174000", "UUID toString()"), // Enum constant
        Arguments.of(DayOfWeek.MONDAY, "MONDAY", "Enum constant DayOfWeek.MONDAY"), // StringBuilder
        Arguments.of(new StringBuilder("builder content"), "builder content", "StringBuilder toString()"));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(Arguments.of(new ToStringThrows(), IllegalStateException.class, "toString failure", "Object whose toString() throws IllegalStateException"), Arguments.of(new ToStringRuntimeException(), RuntimeException.class, "runtime failure", "Object whose toString() throws RuntimeException"), Arguments.of(new NPEToString(), NullPointerException.class, "explicit NPE from toString", "Object whose toString() throws NullPointerException"), Arguments.of(new IllegalArgumentToString(), IllegalArgumentException.class, "illegal argument in toString", "Object whose toString() throws IllegalArgumentException"), Arguments.of(new AssertionErrorToString(), AssertionError.class, "assertion failed in toString", "Object whose toString() throws AssertionError"), Arguments.of(new CustomRuntimeExceptionToString(), MyRuntimeException.class, "custom runtime failure", "Object whose toString() throws custom unchecked exception"), Arguments.of(createProxyThrowingToString(), IllegalStateException.class, "proxy toString failure", "Dynamic proxy whose toString() throws IllegalStateException"), Arguments.of(new StackOverflowToString(), StackOverflowError.class, "simulated stack overflow", "Object whose toString() throws StackOverflowError"));
    }

    /**
     * Creates a dynamic proxy whose toString() throws IllegalStateException.
     */
    private static Object createProxyThrowingToString() {
        InvocationHandler handler = new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("toString".equals(method.getName())) {
                    throw new IllegalStateException("proxy toString failure");
                }
                // Default behavior for other methods
                return null;
            }
        };
        // Proxy implements no specific interface; use java.lang.Object methods.
        return Proxy.newProxyInstance(StringConversions_toString_Test.class.getClassLoader(), new Class<?>[] {}, handler);
    }

    @Test
    void testToString_NullInput_ReturnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        String result = StringConversions.toString(source, mockConverter);
        // THEN
        assertNull(result, "toString should return null when the source object is null");
    }

    @Test
    void testToString_Object_ReturnsToString() {
        // GIVEN
        // Integer overrides toString()
        Integer source = 42;
        // WHEN
        String result = StringConversions.toString(source, mockConverter);
        // THEN
        assertEquals("42", result, "toString should return the result of Integer.toString()");
    }

    @Test
    void testToString_CustomObject_ReturnsCustomToString() {
        // GIVEN
        class Custom {

            @Override
            public String toString() {
                return "custom-value";
            }
        }
        Custom source = new Custom();
        // WHEN
        String result = StringConversions.toString(source, mockConverter);
        // THEN
        assertEquals("custom-value", result, "toString should return the custom object's overridden toString() value");
    }

    @Test
    void testToString_DoesNotUseConverter() {
        // GIVEN
        String source = "plain string";
        // WHEN
        String result = StringConversions.toString(source, mockConverter);
        // THEN
        assertEquals("plain string", result, "toString should simply delegate to the object's toString() implementation");
        // The @AfterEach verification already ensures no interaction with the mock.
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("normalReturnCases")
    void testToString_NormalReturns(Object from, String expected, String description) {
        // WHEN
        String result = StringConversions.toString(from, mockConverter);
        // THEN
        assertEquals(expected, result, description);
    }

    @Test
    void testToString_IntArray_ReturnsIdentityString() {
        // GIVEN
        int[] array = new int[] { 1, 2, 3 };
        // WHEN
        String result = StringConversions.toString(array, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null for int[]");
        assertTrue(result.startsWith("[I@"), "int[] default toString() should start with \"[I@\" (actual: " + result + ")");
    }

    @Test
    void testToString_ThrowingConverter_IsIgnored() {
        // GIVEN
        // normal toString()
        Object source = new CustomToString();
        // Converter that would throw on any method call
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("converter used");
        });
        // WHEN
        String result = StringConversions.toString(source, throwingConverter);
        // THEN
        assertEquals("custom-result", result, "Method must ignore the converter even if it would throw");
        // Verify the converter was never touched.
        verifyNoInteractions(throwingConverter);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("exceptionCases")
    void testToString_ExceptionsPropagate(Object from, Class<? extends Throwable> exClass, String expectedMessage, String description) {
        // WHEN / THEN
        Throwable thrown = assertThrows(exClass, () -> StringConversions.toString(from, mockConverter), description);
        assertEquals(expectedMessage, thrown.getMessage(), "Exception message should match expected");
    }
}
