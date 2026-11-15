package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_unmaskNullValue_Test {

    private TestMap map;

    private StringMap stringMap;

    private ObjectMap objectMap;

    /**
     * Simple concrete implementation used only for testing the protected methods.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    /**
     * Concrete map with <String, String> – used for the majority of tests where V = String.
     */
    private static class StringMap extends AbstractConcurrentNullSafeMap<String, String> {

        StringMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    /**
     * Concrete map with <Object, Object> – used to verify that non‑String values are returned unchanged.
     */
    private static class ObjectMap extends AbstractConcurrentNullSafeMap<Object, Object> {

        ObjectMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    /**
     * Simple user‑defined class for custom‑object tests.
     */
    private static class CustomClass {

        final String name;

        CustomClass(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomClass) && ((CustomClass) o).name.equals(this.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        stringMap = new StringMap();
        objectMap = new ObjectMap();
    }

    private static Stream<Arguments> provideStringMapCases() {
        return Stream.of(Arguments.of("NULL_VALUE sentinel should be unmasked to null", AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE, null), Arguments.of("Regular string should be returned unchanged", "regularString", "regularString"), Arguments.of("Literal null argument should be returned as null", null, null), // The following case will throw ClassCastException because NULL_KEY cannot be cast to String
        Arguments.of("NULL_KEY sentinel should cause ClassCastException", AbstractConcurrentNullSafeMap.NullSentinel.NULL_KEY, ClassCastException.class), Arguments.of("Integer value should cause ClassCastException", 42, ClassCastException.class));
    }

    private static Stream<Arguments> provideObjectMapCases() {
        return Stream.of(Arguments.of("Plain Object should be returned unchanged (same reference)", new Object(), "sameObject"), Arguments.of("Boolean.TRUE should be returned unchanged", Boolean.TRUE, true), Arguments.of("int[] should be returned unchanged (same reference)", new int[] { 1, 2, 3 }, "intArray"), Arguments.of("CustomClass instance should be returned unchanged (same reference)", new CustomClass("test"), "customClassInstance"));
    }

    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(Arguments.of("Object instance should cause ClassCastException", new Object()), Arguments.of("Integer should cause ClassCastException", 42), Arguments.of("int[] should cause ClassCastException", new int[] { 1, 2, 3 }), Arguments.of("CustomClass should cause ClassCastException", new CustomClass("test")));
    }

    @Test
    void unmaskNullValue_returnsNull_whenValueIsNullSentinel() {
        // GIVEN a sentinel representing a null value
        Object sentinel = AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE;
        // WHEN unmaskNullValue is invoked with the sentinel
        String result = map.unmaskNullValue(sentinel);
        // THEN the result should be null
        assertNull(result, "unmaskNullValue should return null for NULL_VALUE sentinel");
    }

    @Test
    void unmaskNullValue_returnsOriginalValue_whenValueIsNotSentinel() {
        // GIVEN a regular (non‑null) value
        String original = "someValue";
        // WHEN unmaskNullValue is invoked with that value
        String result = map.unmaskNullValue(original);
        // THEN the same value should be returned unchanged
        assertSame(original, result, "unmaskNullValue should return the original object when not a sentinel");
    }

    @Test
    void unmaskNullValue_behavesCorrectlyThroughPutAndGet() {
        // GIVEN a map where we put a null value (which will be masked internally)
        map.put("key1", null);
        map.put("key2", "value2");
        // WHEN we retrieve the entries
        String val1 = map.get("key1");
        String val2 = map.get("key2");
        // THEN the null value is correctly unmasked to null,
        // and the non‑null value is returned unchanged.
        assertNull(val1, "Retrieving a key associated with a null value should yield null");
        assertEquals("value2", val2, "Retrieving a key with a non‑null value should yield that value");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideStringMapCases")
    void unmaskNullValue_stringMap(String description, Object input, Object expected) {
        if (expected instanceof Class && ((Class<?>) expected).equals(ClassCastException.class)) {
            assertThrows(ClassCastException.class, () -> stringMap.unmaskNullValue(input), description);
        } else {
            // Expected is either null or a specific sentinel/value
            String result = stringMap.unmaskNullValue(input);
            assertEquals(expected, result, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideObjectMapCases")
    void unmaskNullValue_objectMap(String description, Object input, Object expected) {
        Object result = objectMap.unmaskNullValue(input);
        // For the "sameObject" expectation we compare by reference
        if ("sameObject".equals(expected)) {
            assertSame(input, result, description);
        } else if ("intArray".equals(expected)) {
            assertSame(input, result, description);
            assertTrue(result instanceof int[], "Result should be an int[]");
        } else if ("customClassInstance".equals(expected)) {
            assertSame(input, result, description);
            assertTrue(result instanceof CustomClass, "Result should be a CustomClass");
        } else {
            assertEquals(expected, result, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideExceptionCases")
    void unmaskNullValue_exceptions(String description, Object input) {
        assertThrows(ClassCastException.class, () -> stringMap.unmaskNullValue(input), description);
    }
}
