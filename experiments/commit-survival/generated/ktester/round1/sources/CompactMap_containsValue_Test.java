package com.cedarsoftware.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

class CompactMap_containsValue_Test {

    /**
     * CompactMap switches to a delegate {@link Map} when its size exceeds {@code compactSize()}.
     * By overriding {@code compactSize()} to a small number we can force this behaviour
     * without inserting many entries.
     */
    private static class SmallCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            // Switch to a regular Map after 2 entries
            return 2;
        }

        @Override
        protected Map<K, V> getNewMap() {
            return new HashMap<>();
        }
    }

    /**
     * Custom value class that defines equality based on an internal id.
     */
    private static class Custom {

        final int id;

        Custom(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Custom) && ((Custom) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Custom(" + id + ")";
        }
    }

    /**
     * Returns {@code true} if the supplied method returns {@code void}.
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

    /**
     * Returns {@code true} if the supplied method is {@code public}.
     */
    public static boolean isPublic(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }

    /**
     * Returns {@code true} if the supplied method is {@code static}.
     */
    public static boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    /**
     * Returns {@code true} if the supplied method is {@code abstract}.
     */
    public static boolean isAbstract(Method method) {
        return Modifier.isAbstract(method.getModifiers());
    }

    /**
     * Returns {@code true} if the supplied method is synthetic.
     */
    public static boolean isSynthetic(Method method) {
        return method.isSynthetic();
    }

    /**
     * Returns {@code true} if the supplied method is annotated with {@code annotation}.
     */
    public static boolean isAnnotated(Method method, Class<? extends Annotation> annotation) {
        return method.isAnnotationPresent(annotation);
    }

    /**
     * Finds a method with the given name and parameter types, or {@code null} if not found.
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Returns {@code true} if the supplied method is present on the given class (or its super‑classes).
     */
    public static boolean isMethodPresent(Class<?> clazz, Method method) {
        if (method == null) {
            return false;
        }
        return clazz.isAssignableFrom(method.getDeclaringClass()) || method.getDeclaringClass().isAssignableFrom(clazz);
    }

    /**
     * Directly sets the private {@code val} field of a CompactMap instance.
     *
     * @param map the CompactMap whose internal state should be altered
     * @param val the object to store in the {@code val} field
     */
    private static void setInternalVal(CompactMap<?, ?> map, Object val) {
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(map, val);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Stream<Arguments> arrayRepresentationProvider() {
        return Stream.of(Arguments.of("value2", true, "Value 'value2' exists in the internal Object[]"), Arguments.of("missing", false, "Value 'missing' does not exist"), Arguments.of(null, true, "Null value is present in the array"));
    }

    static Stream<Arguments> mapRepresentationProvider() {
        return Stream.of(Arguments.of("alpha", true, "Existing value 'alpha' should be found"), Arguments.of("missing", false, "Non‑existing value should not be found"), Arguments.of(null, true, "Null value stored in map must be found"));
    }

    static Stream<Arguments> singleEntryProvider() {
        return Stream.of(Arguments.of("single", true, "Exact same String instance"), Arguments.of(new String("single"), true, "Different String instance but equal content"), Arguments.of(null, true, "Stored value is null, searching for null"), Arguments.of("other", false, "Different value should not be found"));
    }

    @Test
    void testContainsValueEmpty() {
        CompactMap<String, Integer> map = new CompactMap<>();
        Assertions.assertFalse(map.containsValue(1), "Empty map must not contain any value");
        Assertions.assertFalse(map.containsValue(null), "Empty map must not contain null");
    }

    @Test
    void testContainsValueSingleEntryFound() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        Assertions.assertTrue(map.containsValue(1), "Map should contain the inserted value");
    }

    @Test
    void testContainsValueSingleEntryNotFound() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        Assertions.assertFalse(map.containsValue(2), "Map should not contain a value that was never added");
        Assertions.assertFalse(map.containsValue(null), "Map should not contain null when not added");
    }

    @Test
    void testContainsValueTwoEntriesFound() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        // With default compactSize (>=2) we are now in the Object[] representation
        map.put("two", 2);
        Assertions.assertTrue(map.containsValue(1), "First value must be found");
        Assertions.assertTrue(map.containsValue(2), "Second value must be found");
    }

    @Test
    void testContainsValueTwoEntriesNotFound() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        map.put("two", 2);
        Assertions.assertFalse(map.containsValue(3), "Value that was never added must not be reported as present");
        Assertions.assertFalse(map.containsValue(null), "Null value must not be reported as present when not added");
    }

    @Test
    void testContainsValueMapDelegateFound() {
        CompactMap<String, String> map = new SmallCompactMap<>();
        map.put("a", "alpha");
        map.put("b", "beta");
        // Triggers delegation to the internal HashMap
        map.put("c", "gamma");
        Assertions.assertTrue(map.containsValue("gamma"), "Delegate map must report stored value");
        Assertions.assertTrue(map.containsValue("alpha"), "Delegate map must report earlier stored values");
    }

    @Test
    void testContainsValueMapDelegateNotFound() {
        CompactMap<String, String> map = new SmallCompactMap<>();
        map.put("a", "alpha");
        map.put("b", "beta");
        // Switch to delegate map
        map.put("c", "gamma");
        Assertions.assertFalse(map.containsValue("delta"), "Delegate map must not report a value that was never added");
        Assertions.assertFalse(map.containsValue(null), "Delegate map must not report null when not added");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("arrayRepresentationProvider")
    void testContainsValueArrayRepresentation(Object searchValue, boolean expected, String description) {
        CompactMap<String, Object> map = new CompactMap<>();
        // ["k1","value1","k2","value2"] plus a null entry for the null case
        Object[] internal = new Object[] { "k1", "value1", "k2", "value2", "k3", null };
        setInternalVal(map, internal);
        Assertions.assertEquals(expected, map.containsValue(searchValue), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("mapRepresentationProvider")
    void testContainsValueMapRepresentation(Object searchValue, boolean expected, String description) {
        CompactMap<String, Object> map = new CompactMap<>();
        Map<String, Object> backing = new HashMap<>();
        backing.put("a", "alpha");
        backing.put("b", "beta");
        backing.put("c", null);
        setInternalVal(map, backing);
        Assertions.assertEquals(expected, map.containsValue(searchValue), description);
    }

    @Test
    void testContainsValueMapWithCustomObject() {
        CompactMap<String, Object> map = new CompactMap<>();
        Map<String, Object> backing = new HashMap<>();
        backing.put("x", new Custom(5));
        setInternalVal(map, backing);
        // Search with a different instance that is equal (same id)
        Assertions.assertTrue(map.containsValue(new Custom(5)), "Equal custom object must be found");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("singleEntryProvider")
    void testContainsValueSingleEntry(Object searchValue, boolean expected, String description) {
        CompactMap<String, Object> map = new CompactMap<>();
        // Force the internal representation to a single logical value.
        // the stored value is the literal "single"
        setInternalVal(map, "single");
        // For the null case we need to replace it with null.
        if (searchValue == null) {
            setInternalVal(map, null);
        }
        Assertions.assertEquals(expected, map.containsValue(searchValue), description);
    }

    @Test
    void testContainsValueOddArrayLengthException() {
        CompactMap<String, Object> map = new CompactMap<>();
        // Array with odd number of elements: ["k1","v1","k2"]
        Object[] odd = new Object[] { "k1", "v1", "k2" };
        setInternalVal(map, odd);
        Executable exec = () -> map.containsValue("any");
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, exec, "Odd‑length internal array must cause ArrayIndexOutOfBoundsException");
    }

    @Test
    void testContainsValueNullInternalValueException() {
        CompactMap<String, Object> map = new CompactMap<>();
        // Force the internal field to null
        setInternalVal(map, null);
        Executable exec = () -> map.containsValue("any");
        Assertions.assertThrows(NullPointerException.class, exec, "Null internal 'val' should lead to NullPointerException when falling through to getLogicalSingleValue()");
    }

    @Test
    void testContainsValueMapExceptionPropagation() {
        // Create a mock Map that throws IllegalStateException on containsValue
        @SuppressWarnings("unchecked")
        Map<String, Object> throwingMap = Mockito.mock(Map.class);
        Mockito.when(throwingMap.containsValue(Mockito.any())).thenThrow(new IllegalStateException("trigger"));
        CompactMap<String, Object> map = new CompactMap<>();
        setInternalVal(map, throwingMap);
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, () -> map.containsValue("trigger"), "Exception from delegate map must propagate unchanged");
        Assertions.assertEquals("trigger", ex.getMessage());
    }
}
