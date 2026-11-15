package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import static com.cedarsoftware.util.CompactMap.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompactMap_validateAndFinalizeOptions_Test {

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

//     public static boolean isPublic(Method method) {
//         if (member instanceof Method)
//             return isPublic((Method) member);
//         if (member instanceof Constructor)
//             return isPublic((Constructor<?>) member);
//         if (member instanceof Field)
//             return isPublic((Field) member);
//         if (member instanceof Class)
//             return isPublic((Class<?>) member);
//         return false;
//     }

//     public static boolean isStatic(Method method) {
//         if (member instanceof Method)
//             return isStatic((Method) member);
//         if (member instanceof Constructor)
//             return isStatic((Constructor<?>) member);
//         if (member instanceof Field)
//             return isStatic((Field) member);
//         if (member instanceof Class)
//             return isStatic((Class<?>) member);
//         return false;
//     }

//     public static boolean isAbstract(Method method) {
//         return Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface();
//     }

//     public static boolean isPublicOrProtected(Method method) {
//         if (member instanceof Method)
//             return isPublicOrProtected((Method) member);
//         if (member instanceof Constructor)
//             return isPublicOrProtected((Constructor<?>) member);
//         if (member instanceof Field)
//             return isPublicOrProtected((Field) member);
//         if (member instanceof Class)
//             return isPublicOrProtected((Class<?>) member);
//         return false;
//     }

    public static boolean isPublic(Class<?> clazz) {
        return Modifier.isPublic(clazz.getModifiers());
    }

    public static boolean isStatic(Class<?> clazz) {
        return Modifier.isStatic(clazz.getModifiers());
    }

    public static boolean isAbstract(Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface();
    }

    public static boolean isPublicOrProtected(Class<?> clazz) {
        int mods = clazz.getModifiers();
        return Modifier.isPublic(mods) || Modifier.isProtected(mods);
    }

    public static boolean isPublic(Field field) {
        return Modifier.isPublic(field.getModifiers());
    }

    public static boolean isStatic(Field field) {
        return Modifier.isStatic(field.getModifiers());
    }

    public static boolean isPublicOrProtected(Field field) {
        int mods = field.getModifiers();
        return Modifier.isPublic(mods) || Modifier.isProtected(mods);
    }

    public static boolean isPublic(Constructor<?> constructor) {
        return Modifier.isPublic(constructor.getModifiers());
    }

    public static boolean isStatic(Constructor<?> constructor) {
        return Modifier.isStatic(constructor.getModifiers());
    }

    public static boolean isPublicOrProtected(Constructor<?> constructor) {
        int mods = constructor.getModifiers();
        return Modifier.isPublic(mods) || Modifier.isProtected(mods);
    }

    public static boolean isAbstractOrInterface(Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface();
    }

    // future NoSuchMethodError should the engine call them.
    public static boolean isPublicOrProtected(Object member) {
        if (member instanceof Method)
            return isPublicOrProtected((Method) member);
        if (member instanceof Constructor)
            return isPublicOrProtected((Constructor<?>) member);
        if (member instanceof Field)
            return isPublicOrProtected((Field) member);
        if (member instanceof Class)
            return isPublicOrProtected((Class<?>) member);
        return false;
    }

    public static boolean isStatic(Object member) {
        if (member instanceof Method)
            return isStatic((Method) member);
        if (member instanceof Constructor)
            return isStatic((Constructor<?>) member);
        if (member instanceof Field)
            return isStatic((Field) member);
        if (member instanceof Class)
            return isStatic((Class<?>) member);
        return false;
    }

    public static boolean isPublic(Object member) {
        if (member instanceof Method)
            return isPublic((Method) member);
        if (member instanceof Constructor)
            return isPublic((Constructor<?>) member);
        if (member instanceof Field)
            return isPublic((Field) member);
        if (member instanceof Class)
            return isPublic((Class<?>) member);
        return false;
    }

    private static Stream<Arguments> invalidTypeCases() {
        return Stream.of(Arguments.of(COMPACT_SIZE, "large", ClassCastException.class), Arguments.of(ORDERING, 456, ClassCastException.class), Arguments.of(CASE_SENSITIVE, "no", ClassCastException.class));
    }

    @Test
    void testValidateAndFinalizeOptions_Defaults() {
        Map<String, Object> options = new HashMap<>();
        CompactMap.validateAndFinalizeOptions(options);
        // defaults must be present
        assertTrue(options.containsKey(MAP_TYPE), "MAP_TYPE should be present");
        assertEquals(HashMap.class, options.get(MAP_TYPE), "Default MAP_TYPE is HashMap");
        // DEFAULT_COMPACT_SIZE = 70
        assertEquals(70, options.get(COMPACT_SIZE));
        // DEFAULT_CASE_SENSITIVE = true
        assertEquals(true, options.get(CASE_SENSITIVE));
    }

    @Test
    void testValidateAndFinalizeOptions_InvalidCompactSize() {
        Map<String, Object> options = new HashMap<>();
        // illegal value
        options.put(COMPACT_SIZE, 1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CompactMap.validateAndFinalizeOptions(options));
        assertEquals("compactSize must be >= 2", ex.getMessage());
    }

    @Test
    void testValidateAndFinalizeOptions_OrderingConflict() {
        Map<String, Object> options = new HashMap<>();
        options.put(ORDERING, INSERTION);
        options.put(SOURCE_MAP, new TreeMap<>());
        try (MockedStatic<MapUtilities> utilities = mockStatic(MapUtilities.class)) {
            utilities.when(() -> MapUtilities.detectMapOrdering(any())).thenReturn(SORTED);
            utilities.when(() -> MapUtilities.getMapStructureString(any())).thenReturn("TreeMap structure");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CompactMap.validateAndFinalizeOptions(options));
            String msg = ex.getMessage();
            assertTrue(msg.contains("conflicts with source map's ordering"), "Exception message should mention ordering conflict");
            assertTrue(msg.contains(INSERTION), "Message should contain the requested ordering");
            assertTrue(msg.contains(SORTED), "Message should contain the source map's ordering");
        }
    }

//     @Test
//     void testValidateAndFinalizeOptions_CaseInsensitiveWrapping() {
//         Map<String, Object> options = new HashMap<>();
//         options.put(CASE_SENSITIVE, false);
//         // unordered ordering
//         options.put(ORDERING, UNORDERED);
//         CompactMap.validateAndFinalizeOptions(options);
//         // The method should replace MAP_TYPE with CaseInsensitiveMap and keep the original type in INNER_MAP_TYPE
//         assertEquals(CaseInsensitiveMap.class, options.get(MAP_TYPE), "MAP_TYPE should be wrapped");
//         assertEquals(HashMap.class, options.get(INNER_MAP_TYPE), "INNER_MAP_TYPE should hold the original map type");
//     }

    @Test
    void testValidateAndFinalizeOptions_NoWrapWhenSorted() {
        Map<String, Object> options = new HashMap<>();
        options.put(CASE_SENSITIVE, false);
        // sorted ordering – wrapping is not allowed
        options.put(ORDERING, SORTED);
        CompactMap.validateAndFinalizeOptions(options);
        // MAP_TYPE must be a concrete map type (TreeMap, HashMap, …) but never CaseInsensitiveMap
        assertNotEquals(CaseInsensitiveMap.class, options.get(MAP_TYPE), "Sorted maps must not be wrapped in CaseInsensitiveMap");
    }

    @Test
    void testValidateAndFinalizeOptions_ExplicitMapTypePreserved() {
        Map<String, Object> options = new HashMap<>();
        options.put(MAP_TYPE, LinkedHashMap.class);
        options.put(ORDERING, UNORDERED);
        options.put(CASE_SENSITIVE, true);
        CompactMap.validateAndFinalizeOptions(options);
        assertEquals(LinkedHashMap.class, options.get(MAP_TYPE), "Explicit MAP_TYPE should be preserved");
    }

    @Test
    void testValidateAndFinalizeOptions_SortedOrdering_DefaultCaseSensitive() {
        Map<String, Object> options = new HashMap<>();
        options.put(ORDERING, SORTED);
        CompactMap.validateAndFinalizeOptions(options);
        assertEquals(TreeMap.class, options.get(MAP_TYPE), "Sorted ordering should resolve to TreeMap");
        // DEFAULT_COMPACT_SIZE
        assertEquals(70, options.get(COMPACT_SIZE));
        // DEFAULT_CASE_SENSITIVE
        assertEquals(true, options.get(CASE_SENSITIVE));
    }

//     @Test
//     void testValidateAndFinalizeOptions_ExplicitCompactSizeAndCaseInsensitive() {
//         Map<String, Object> options = new HashMap<>();
//         options.put(COMPACT_SIZE, 25);
//         options.put(CASE_SENSITIVE, false);
//         options.put(ORDERING, UNORDERED);
//         CompactMap.validateAndFinalizeOptions(options);
//         assertEquals(25, options.get(COMPACT_SIZE));
//         assertEquals(CaseInsensitiveMap.class, options.get(MAP_TYPE));
//         assertEquals(HashMap.class, options.get(INNER_MAP_TYPE));
//         assertEquals(false, options.get(CASE_SENSITIVE));
//     }

    @Test
    void testValidateAndFinalizeOptions_NullOptions() {
        assertThrows(NullPointerException.class, () -> CompactMap.validateAndFinalizeOptions(null));
    }

    @Test
    void testValidateAndFinalizeOptions_InvalidOrderingType() {
        Map<String, Object> options = new HashMap<>();
        // Integer instead of String
        options.put(ORDERING, 123);
        assertThrows(ClassCastException.class, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    @Test
    void testValidateAndFinalizeOptions_InvalidCompactSizeType() {
        Map<String, Object> options = new HashMap<>();
        // String instead of int
        options.put(COMPACT_SIZE, "large");
        assertThrows(ClassCastException.class, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    @Test
    void testValidateAndFinalizeOptions_InvalidCaseSensitiveType() {
        Map<String, Object> options = new HashMap<>();
        // String instead of boolean
        options.put(CASE_SENSITIVE, "yes");
        assertThrows(ClassCastException.class, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    @Test
    void testValidateAndFinalizeOptions_SourceNotAMap() {
        Map<String, Object> options = new HashMap<>();
        options.put(SOURCE_MAP, "not a map");
        assertThrows(ClassCastException.class, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    @Test
    void testValidateAndFinalizeOptions_NullOrderingValue() {
        Map<String, Object> options = new HashMap<>();
        // explicit null
        options.put(ORDERING, null);
        assertThrows(NullPointerException.class, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    @Test
    void testValidateAndFinalizeOptions_UnsupportedOrderingValue() {
        Map<String, Object> options = new HashMap<>();
        options.put(ORDERING, "unknownOrdering");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CompactMap.validateAndFinalizeOptions(options));
        assertNotNull(ex.getMessage());
    }

    @ParameterizedTest(name = "Invalid type for {0} should throw {2}")
    @MethodSource("invalidTypeCases")
    void testValidateAndFinalizeOptions_InvalidTypes(String key, Object badValue, Class<? extends Throwable> expected) {
        Map<String, Object> options = new HashMap<>();
        options.put(key, badValue);
        assertThrows(expected, () -> CompactMap.validateAndFinalizeOptions(options));
    }
}