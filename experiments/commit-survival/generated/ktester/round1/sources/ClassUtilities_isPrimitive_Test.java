package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_isPrimitive_Test {

    static class MyClass {
    }

    @BeforeEach
    void setupBeforeEach() {
        getPrimsSet().clear();
    }

    private static Stream<Class<?>> primitiveTypes() {
        return Stream.of(int.class, boolean.class, double.class);
    }

    private static Stream<Class<?>> additionalPrimitiveTypes() {
        return Stream.of(byte.class, char.class, short.class, long.class, float.class, void.class);
    }

    private static Stream<Class<?>> nonPrimitiveNotInSet() {
        return Stream.of(String.class, Integer.class);
    }

    private static Stream<Class<?>> nonPrimitiveInSet() {
        return Stream.of(Integer.class, java.util.Date.class, MyClass.class);
    }

    private static Stream<Class<?>> wrapperTypesNotInSet() {
        return Stream.of(Long.class, Float.class, Short.class, Byte.class, Character.class, Boolean.class);
    }

    @Test
    void primitiveClassesAreRecognized() {
        assertTrue(ClassUtilities.isPrimitive(boolean.class), "boolean should be primitive");
        assertTrue(ClassUtilities.isPrimitive(byte.class), "byte should be primitive");
        assertTrue(ClassUtilities.isPrimitive(char.class), "char should be primitive");
        assertTrue(ClassUtilities.isPrimitive(short.class), "short should be primitive");
        assertTrue(ClassUtilities.isPrimitive(int.class), "int should be primitive");
        assertTrue(ClassUtilities.isPrimitive(long.class), "long should be primitive");
        assertTrue(ClassUtilities.isPrimitive(float.class), "float should be primitive");
        assertTrue(ClassUtilities.isPrimitive(double.class), "double should be primitive");
        assertTrue(ClassUtilities.isPrimitive(void.class), "void should be primitive");
    }

    @Test
    void wrapperClassesAreConsideredPrimitiveByUtility() {
        assertTrue(ClassUtilities.isPrimitive(Boolean.class), "Boolean wrapper should be treated as primitive");
        assertTrue(ClassUtilities.isPrimitive(Byte.class), "Byte wrapper should be treated as primitive");
        assertTrue(ClassUtilities.isPrimitive(Character.class), "Character wrapper should be treated as primitive");
        assertTrue(ClassUtilities.isPrimitive(Short.class), "Short wrapper should be treated as primitive");
        assertTrue(ClassUtilities.isPrimitive(Integer.class), "Integer wrapper should be treated as primitive");
        assertTrue(ClassUtilities.isPrimitive(Long.class), "Long wrapper should be treated as primitive");
        assertTrue(ClassUtilities.isPrimitive(Float.class), "Float wrapper should be treated as primitive");
        assertTrue(ClassUtilities.isPrimitive(Double.class), "Double wrapper should be treated as primitive");
    }

    @Test
    void nonPrimitiveClassesReturnFalse() {
        assertFalse(ClassUtilities.isPrimitive(String.class), "String is not primitive");
        assertFalse(ClassUtilities.isPrimitive(Object.class), "Object is not primitive");
        assertFalse(ClassUtilities.isPrimitive(ClassUtilities.class), "Utility class itself is not primitive");
        ???;
    }

    @Test
    void nullArgumentThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ClassUtilities.isPrimitive(null), "Calling isPrimitive with null should throw NPE");
    }

    @SuppressWarnings("unchecked")
    private static Set<Class<?>> getPrimsSet() {
        try {
            Field f = ClassUtilities.class.getDeclaredField("prims");
            f.setAccessible(true);
            return (Set<Class<?>>) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to access ClassUtilities.prims", e);
        }
    }

    @ParameterizedTest(name = "{index}: {0} is primitive")
    @MethodSource("primitiveTypes")
    void primitiveTypesShouldBeRecognized(Class<?> clazz) {
        assertTrue(ClassUtilities.isPrimitive(clazz), clazz.getName() + " should be recognized as primitive");
    }

    @ParameterizedTest(name = "{index}: {0} is primitive")
    @MethodSource("additionalPrimitiveTypes")
    void additionalPrimitiveTypesShouldBeRecognized(Class<?> clazz) {
        assertTrue(ClassUtilities.isPrimitive(clazz), clazz.getName() + " should be recognized as primitive");
    }

    @ParameterizedTest(name = "{index}: {0} is NOT primitive")
    @MethodSource("nonPrimitiveNotInSet")
    void nonPrimitiveTypesNotInSetReturnFalse(Class<?> clazz) {
        assertFalse(ClassUtilities.isPrimitive(clazz), clazz.getName() + " should NOT be considered primitive when not in the set");
    }

    @ParameterizedTest(name = "{index}: {0} added to set → isPrimitive true")
    @MethodSource("nonPrimitiveInSet")
    void nonPrimitiveTypesAddedToSetAreRecognized(Class<?> clazz) throws Exception {
        Set<Class<?>> prims = getPrimsSet();
        prims.add(clazz);
        assertTrue(ClassUtilities.isPrimitive(clazz), clazz.getName() + " should be considered primitive after being added to the internal set");
    }

    @ParameterizedTest(name = "{index}: {0} wrapper not in set → false")
    @MethodSource("wrapperTypesNotInSet")
    void wrapperTypesNotInSetReturnFalse(Class<?> clazz) {
        assertTrue(getPrimsSet().isEmpty(), "prims set should be empty before this assertion");
        assertFalse(ClassUtilities.isPrimitive(clazz), clazz.getName() + " wrapper should NOT be considered primitive when not in the set");
    }

    @Test
    void primitiveClassAlsoAddedToSetStillReturnsTrue() {
        getPrimsSet().add(int.class);
        assertTrue(ClassUtilities.isPrimitive(int.class), "int.class should return true even when explicitly added to the set");
    }

    @Test
    void duplicateAdditionsDoNotAffectResult() {
        Set<Class<?>> prims = getPrimsSet();
        prims.add(java.util.Date.class);
        prims.add(java.util.Date.class);
        assertTrue(ClassUtilities.isPrimitive(java.util.Date.class), "Date should be considered primitive after being added twice");
    }
}
