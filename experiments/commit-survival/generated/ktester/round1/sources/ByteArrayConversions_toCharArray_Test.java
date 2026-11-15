package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

class ByteArrayConversions_toCharArray_Test {

    /**
     * The Converter instance used for all tests.
     */
    private static Converter converter;

    /**
     * The static {@code Converter} used by the simple “happy‑path” tests.
     */
    private static Converter defaultConverter;

    /**
     * Identity converter – returns the input unchanged.
     */
    private static class IdentityConverter implements Converter {

        @Override
        public Object convert(Object from) {
            return from;
        }
    }

    /**
     * Calls {@code String.valueOf} on the input.
     */
    private static class ToStringConverter implements Converter {

        @Override
        public Object convert(Object from) {
            return String.valueOf(from);
        }
    }

    /**
     * Returns a literal "null" string irrespective of the input.
     */
    private static class NullSafeConverter implements Converter {

        @Override
        public Object convert(Object from) {
            return "null";
        }
    }

    /**
     * Converter that always throws a RuntimeException.
     */
    private static class ThrowingConverter implements Converter {

        @Override
        public Object convert(Object from) {
            throw new RuntimeException("forced");
        }
    }

    /**
     * Converter that returns {@code null}.
     */
    private static class NullReturningConverter implements Converter {

        @Override
        public Object convert(Object from) {
            return null;
        }
    }

    /**
     * Converter that returns an Integer instead of a String.
     */
    private static class WrongTypeConverter implements Converter {

        @Override
        public Object convert(Object from) {
            return 42;
        }
    }

    /**
     * Converter that throws an AssertionError (subclass of Error).
     */
    private static class ErrorThrowingConverter implements Converter {

        @Override
        public Object convert(Object from) {
            throw new AssertionError("error");
        }
    }

    /**
     * Simple Person POJO used for a custom conversion test.
     */
    private static class Person {

        final String name;

        final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    /**
     * Converter that formats a {@link Person} as "name age".
     */
    private static class PersonConverter implements Converter {

        @Override
        public Object convert(Object from) {
            Person p = (Person) from;
            return p.name + " " + p.age;
        }
    }

    /**
     * Converter that builds a String from a byte array using the default charset.
     */
    private static class ByteArrayToStringConverter implements Converter {

        @Override
        public Object convert(Object from) {
            return new String((byte[]) from);
        }
    }

    /**
     * Container for a single test case's data.
     */
    private static class ArgumentsHolder {

        final Object from;

        final Converter converter;

        final char[] expected;

        final String description;

        ArgumentsHolder(Object from, Converter converter, char[] expected, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.description = description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance – the default constructor provides
        // a sensible, empty configuration suitable for the unit‑tests.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the test tidy.
        converter = null;
    }

    /* --------------------------------------------------------------------- *
     *  Helper to invoke the private static toString method via reflection.
     * --------------------------------------------------------------------- */
    private static String invokeToString(Object from, Converter conv) {
        try {
            Method m = ByteArrayConversions.class.getDeclaredMethod("toString", Object.class, Converter.class);
            m.setAccessible(true);
            return (String) m.invoke(null, from, conv);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Normal conversions – the converter returns a proper {@code String}.
     */
    private static Stream<ArgumentsHolder> normalConversions() {
        return Stream.of(new ArgumentsHolder("Hello World", new IdentityConverter(), "Hello World".toCharArray(), "plain String, identity converter"), new ArgumentsHolder(12345, new ToStringConverter(), "12345".toCharArray(), "Integer converted via toString converter"), new ArgumentsHolder(new Person("John", 30), new PersonConverter(), "John 30".toCharArray(), "custom Person object"));
    }

    /**
     * Null handling cases.
     */
    private static Stream<ArgumentsHolder> nullHandling() {
        return Stream.of(new ArgumentsHolder(null, new NullSafeConverter(), "null".toCharArray(), "null source → converter returns literal \"null\""), new // expected exception is handled in test
        ArgumentsHolder(// expected exception is handled in test
        "test", // expected exception is handled in test
        null, // expected exception is handled in test
        null, "non‑null source but null converter"));
    }

    /**
     * Exception handling cases.
     */
    private static Stream<ArgumentsHolder> exceptionHandling() {
        return Stream.of(new ArgumentsHolder("oops", new ThrowingConverter(), null, "converter throws RuntimeException"), new ArgumentsHolder("any", new NullReturningConverter(), null, "converter returns null → NPE"), new ArgumentsHolder(null, new NullReturningConverter(), null, "null source + converter returns null → NPE"), new ArgumentsHolder("any", new WrongTypeConverter(), null, "converter returns non‑String → ClassCastException"), new ArgumentsHolder(new Object(), new IdentityConverter(), null, "identity converter returns Object → ClassCastException"), new ArgumentsHolder("trigger", new ErrorThrowingConverter(), null, "converter throws AssertionError"));
    }

    /**
     * Edge‑case conversions (empty, large, Unicode).
     */
    private static Stream<ArgumentsHolder> edgeCases() {
        String large = "a".repeat(10_000);
        return Stream.of(new ArgumentsHolder("", new IdentityConverter(), new char[0], "empty string"), new ArgumentsHolder("😀", new IdentityConverter(), new char[] { '\uD83D', '\uDE00' }, "emoji surrogate pair"), new ArgumentsHolder(large, new IdentityConverter(), large.toCharArray(), "large 10 000‑character string"));
    }

    /**
     * CharSequence specific cases.
     */
    private static Stream<ArgumentsHolder> charSequenceCases() {
        return Stream.of(new ArgumentsHolder(new StringBuilder("Builder"), new ToStringConverter(), "Builder".toCharArray(), "StringBuilder via toString converter"), new ArgumentsHolder(CharBuffer.wrap("CharBuf"), new IdentityConverter(), "CharBuf".toCharArray(), "CharBuffer via identity converter"));
    }

    /**
     * Byte‑array conversion case.
     */
    private static Stream<ArgumentsHolder> byteArrayCase() {
        return Stream.of(new ArgumentsHolder(new byte[] { 65, 66, 67 }, new ByteArrayToStringConverter(), "ABC".toCharArray(), "byte[] → \"ABC\""));
    }

    @Test
    void toCharArray_givenString_returnsCorrectCharArray() {
        // GIVEN
        String source = "JUnit5";
        // WHEN
        char[] result = ByteArrayConversions.toCharArray(source, converter);
        // THEN
        assertNotNull(result, "Resulting char array should not be null");
        assertArrayEquals(source.toCharArray(), result, "Char array produced from a String should match the source characters");
    }

    @Test
    void toCharArray_givenByteArray_returnsCorrectCharArray() {
        // GIVEN
        // default charset
        byte[] source = "byte‑array".getBytes();
        // WHEN
        char[] result = ByteArrayConversions.toCharArray(source, converter);
        // THEN
        assertNotNull(result, "Resulting char array should not be null");
        assertArrayEquals(new String(source).toCharArray(), result, "Char array produced from a byte[] should match the String representation");
    }

    @Test
    void toCharArray_givenCharBuffer_returnsCorrectCharArray() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("charBuffer");
        // WHEN
        char[] result = ByteArrayConversions.toCharArray(source, converter);
        // THEN
        assertNotNull(result, "Resulting char array should not be null");
        assertArrayEquals(source.toString().toCharArray(), result, "Char array produced from a CharBuffer should match its contents");
    }

    @Test
    void toCharArray_givenNull_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toCharArray(source, converter), "Passing null as the source object should result in NPE");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("normalConversions")
    void toCharArray_normalConversions(ArgumentsHolder holder) {
        // GIVEN
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(any())).thenAnswer(invocation -> holder.converter.convert(holder.from));
        // WHEN
        char[] result = ByteArrayConversions.toCharArray(holder.from, mock);
        // THEN
        assertArrayEquals(holder.expected, result, "Conversion result must match the expected char array");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullHandling")
    void toCharArray_nullHandling(ArgumentsHolder holder) {
        if (holder.converter == null) {
            // null converter → NPE from inside toString()
            assertThrows(NullPointerException.class, () -> ByteArrayConversions.toCharArray(holder.from, null), "Passing a null Converter must raise NPE");
            return;
        }
        // GIVEN – mock that returns the configured string
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(any())).thenAnswer(invocation -> holder.converter.convert(holder.from));
        // WHEN
        char[] result = ByteArrayConversions.toCharArray(holder.from, mock);
        // THEN
        assertArrayEquals(holder.expected, result, "When converter supplies a literal string, the char array must reflect it");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionHandling")
    void toCharArray_exceptionHandling(ArgumentsHolder holder) {
        // GIVEN – mock that delegates to the custom converter which may throw or return null
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(any())).thenAnswer(invocation -> holder.converter.convert(holder.from));
        // WHEN / THEN
        if (holder.converter instanceof ThrowingConverter) {
            assertThrows(RuntimeException.class, () -> ByteArrayConversions.toCharArray(holder.from, mock), "Converter‑thrown RuntimeException must propagate");
        } else if (holder.converter instanceof NullReturningConverter) {
            assertThrows(NullPointerException.class, () -> ByteArrayConversions.toCharArray(holder.from, mock), "Null result from converter must cause NPE when toCharArray() is called");
        } else if (holder.converter instanceof WrongTypeConverter) {
            assertThrows(ClassCastException.class, () -> ByteArrayConversions.toCharArray(holder.from, mock), "Returning a non‑String should raise ClassCastException");
        } else if (holder.converter instanceof ErrorThrowingConverter) {
            assertThrows(AssertionError.class, () -> ByteArrayConversions.toCharArray(holder.from, mock), "Error (AssertionError) must propagate unchanged");
        } else {
            fail("Unexpected converter type in exception handling test");
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("edgeCases")
    void toCharArray_edgeCases(ArgumentsHolder holder) {
        // GIVEN – identity converter (returns the argument unchanged)
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(any())).thenAnswer(invocation -> holder.converter.convert(holder.from));
        // WHEN
        char[] result = ByteArrayConversions.toCharArray(holder.from, mock);
        // THEN
        assertArrayEquals(holder.expected, result, "Edge case conversion must produce the exact expected char array");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("charSequenceCases")
    void toCharArray_charSequenceCases(ArgumentsHolder holder) {
        // GIVEN – mock that forwards to the supplied converter
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(any())).thenAnswer(invocation -> holder.converter.convert(holder.from));
        // WHEN
        char[] result = ByteArrayConversions.toCharArray(holder.from, mock);
        // THEN
        assertArrayEquals(holder.expected, result, "CharSequence conversion must match expected characters");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("byteArrayCase")
    void toCharArray_byteArrayCase(ArgumentsHolder holder) {
        // GIVEN – mock that uses the ByteArrayToStringConverter logic
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(any())).thenAnswer(invocation -> holder.converter.convert(holder.from));
        // WHEN
        char[] result = ByteArrayConversions.toCharArray(holder.from, mock);
        // THEN
        assertArrayEquals(holder.expected, result, "Byte array conversion must produce the expected char array");
    }
}
