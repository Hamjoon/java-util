package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Proxy;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassName_Test {

    class LocalClass {
    }

    /**
     * Simple user‑defined class used for testing.
     */
    static class DummyClass {
    }

    // ---------- Standard objects ----------
    private static Stream<TestCase> standardObjectsProvider() {
        return Stream.of(new TestCase("sample", String.class.getName(), "A regular String instance should return its fully‑qualified class name."), new TestCase(Integer.valueOf(42), Integer.class.getName(), "An Integer wrapper object should return java.lang.Integer."), new TestCase(Double.valueOf(3.14), Double.class.getName(), "A Double wrapper object should return java.lang.Double."));
    }

    // ---------- Array objects ----------
    private static Stream<TestCase> arrayObjectsProvider() {
        return Stream.of(new TestCase(new int[] { 1, 2, 3 }, "[I", "A primitive int array should return the JVM internal name \"[I\"."), new TestCase(new String[] { "a", "b" }, "[Ljava.lang.String;", "An object array (String[]) should return the internal name \"[Ljava.lang.String;\"."), new TestCase(new int[2][3], "[[I", "A multidimensional primitive array should return the internal name \"[[I\"."));
    }

    private static Stream<TestCase> customClassProvider() {
        return Stream.of(new TestCase(new DummyClass(), DummyClass.class.getName(), "An instance of a user‑defined class should return its fully‑qualified name."));
    }

    private static Stream<TestCase> enumProvider() {
        return Stream.of(new TestCase(Day.MONDAY, Day.class.getName(), "An enum constant should return the enum type's fully‑qualified name."));
    }

    // ---------- Proxy object ----------
    private static Stream<TestCase> proxyProvider() {
        Object proxy = Proxy.newProxyInstance(ReflectionUtils.class.getClassLoader(), new Class<?>[] { java.io.Serializable.class }, (p, m, args) -> null);
        // The generated proxy class name is implementation‑specific but always starts with "$Proxy".
        // We capture the actual name at runtime and use it as the expected value.
        return Stream.of(new TestCase(proxy, proxy.getClass().getName(), "A dynamic proxy instance should return the generated proxy class name (e.g., \"$Proxy0\")."));
    }

    @Test
    void getClassName_returnsNullStringWhenObjectIsNull() {
        assertEquals("null", ReflectionUtils.getClassName(null), "When the argument is null the method should return the literal string \"null\"");
    }

    @Test
    void getClassName_returnsClassNameForSimpleObject() {
        String result = ReflectionUtils.getClassName("hello");
        assertEquals(String.class.getName(), result, "The class name of a String instance should be java.lang.String");
    }

    @Test
    void getClassName_returnsClassNameForCustomClass() {
        class LocalClass {
        }
        LocalClass instance = new LocalClass();
        String result = ReflectionUtils.getClassName(instance);
        assertEquals(LocalClass.class.getName(), result, "The method should return the fully‑qualified name of the custom class");
    }

    @Test
    void getClassName_returnsClassNameForArrayObject() {
        int[] array = new int[] { 1, 2, 3 };
        String result = ReflectionUtils.getClassName(array);
        assertEquals(int[].class.getName(), result, "Array class names include the leading '[' character and the component type");
    }

    @Test
    void getClassName_returnsClassNameForPrimitiveWrapper() {
        Integer wrapper = Integer.valueOf(42);
        String result = ReflectionUtils.getClassName(wrapper);
        assertEquals(Integer.class.getName(), result, "Wrapper objects should yield their boxed type name");
    }

    @ParameterizedTest(name = "[{index}] {0.description}")
    @MethodSource("standardObjectsProvider")
    void getClassName_standardObjects(TestCase tc) {
        // When the argument is a standard object, the method should return its class name.
        assertEquals(tc.expected, ReflectionUtils.getClassName(tc.input), tc.description);
    }

    @ParameterizedTest(name = "[{index}] {0.description}")
    @MethodSource("arrayObjectsProvider")
    void getClassName_arrayObjects(TestCase tc) {
        assertEquals(tc.expected, ReflectionUtils.getClassName(tc.input), tc.description);
    }

    @ParameterizedTest(name = "[{index}] {0.description}")
    @MethodSource("customClassProvider")
    void getClassName_customClass(TestCase tc) {
        assertEquals(tc.expected, ReflectionUtils.getClassName(tc.input), tc.description);
    }

    @ParameterizedTest(name = "[{index}] {0.description}")
    @MethodSource("enumProvider")
    void getClassName_enumConstant(TestCase tc) {
        assertEquals(tc.expected, ReflectionUtils.getClassName(tc.input), tc.description);
    }

    @ParameterizedTest(name = "[{index}] {0.description}")
    @MethodSource("proxyProvider")
    void getClassName_proxyObject(TestCase tc) {
        assertEquals(tc.expected, ReflectionUtils.getClassName(tc.input), tc.description);
    }
}
