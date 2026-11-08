package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import java.util.Map;

@DisplayName("Tests for ClassUtilities.toPrimitiveWrapperClass")
class ClassUtilities_toPrimitiveWrapperClass_Test {

    /* -----------------------------------------------------------------
     *  4) Defensive test – simulate a missing entry in the internal map.
     *
     *  The {@code PRIMITIVE_TO_WRAPPER} map is private static final.
     *  We use reflection to temporarily remove an entry and verify that
     *  {@code IllegalArgumentException} is thrown.
     * ----------------------------------------------------------------- */
    @Nested
    @DisplayName("Missing map entry scenarios")
    class MissingMapEntryTests {

        private Map<Class<?>, Class<?>> primitiveToWrapperMap;

        private Field mapField;

        @BeforeEach
        void openMap() throws Exception {
            // Obtain the private static field and make it accessible.
            mapField = ClassUtilities.class.getDeclaredField("PRIMITIVE_TO_WRAPPER");
            mapField.setAccessible(true);
            // The field holds a Map; retrieve the actual instance.
            primitiveToWrapperMap = (Map<Class<?>, Class<?>>) mapField.get(null);
        }

        @AfterEach
        void restoreMap() throws Exception {
            // Ensure that the map is restored to its original state after each test.
            // (We simply put back the known entries that we may have removed.)
            primitiveToWrapperMap.put(int.class, Integer.class);
            primitiveToWrapperMap.put(void.class, Void.class);
        }

        @Test
        @DisplayName("Missing entry for int → IllegalArgumentException")
        void missingIntEntryThrows() {
            // Remove the mapping for int.class
            Class<?> removed = primitiveToWrapperMap.remove(int.class);
            assertNotNull(removed, "Pre‑condition: int mapping should exist before removal");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.toPrimitiveWrapperClass(int.class));
            assertTrue(ex.getMessage().contains("int"), "Exception message should contain the primitive class name");
        }

        @Test
        @DisplayName("Missing entry for void → IllegalArgumentException with class name")
        void missingVoidEntryThrows() {
            // Remove the mapping for void.class
            Class<?> removed = primitiveToWrapperMap.remove(void.class);
            assertNotNull(removed, "Pre‑condition: void mapping should exist before removal");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.toPrimitiveWrapperClass(void.class));
            assertTrue(ex.getMessage().contains("void"), "Exception message should contain the primitive class name 'void'");
        }
    }

    /**
     * Helper that returns the primitive {@code Class} object for a given name.
     */
    private static Class<?> getPrimitiveClassByName(String name) {
        switch(name) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "char":
                return char.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "void":
                return void.class;
            default:
                throw new IllegalArgumentException("Unsupported primitive name: " + name);
        }
    }

    /* -----------------------------------------------------------------
     *  1) Primitive types are correctly converted to their wrapper classes.
     * ----------------------------------------------------------------- */
    @Test
    @DisplayName("Primitive int -> Integer")
    void primitiveIntIsConvertedToInteger() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
    }

    @Test
    @DisplayName("Primitive boolean -> Boolean")
    void primitiveBooleanIsConvertedToBoolean() {
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
    }

    @Test
    @DisplayName("Primitive double -> Double")
    void primitiveDoubleIsConvertedToDouble() {
        assertEquals(Double.class, ClassUtilities.toPrimitiveWrapperClass(double.class));
    }

    @Test
    @DisplayName("Primitive char -> Character")
    void primitiveCharIsConvertedToCharacter() {
        assertEquals(Character.class, ClassUtilities.toPrimitiveWrapperClass(char.class));
    }

    @Test
    @DisplayName("Primitive long -> Long")
    void primitiveLongIsConvertedToLong() {
        assertEquals(Long.class, ClassUtilities.toPrimitiveWrapperClass(long.class));
    }

    @Test
    @DisplayName("Primitive short -> Short")
    void primitiveShortIsConvertedToShort() {
        assertEquals(Short.class, ClassUtilities.toPrimitiveWrapperClass(short.class));
    }

    @Test
    @DisplayName("Primitive byte -> Byte")
    void primitiveByteIsConvertedToByte() {
        assertEquals(Byte.class, ClassUtilities.toPrimitiveWrapperClass(byte.class));
    }

    @Test
    @DisplayName("Primitive float -> Float")
    void primitiveFloatIsConvertedToFloat() {
        assertEquals(Float.class, ClassUtilities.toPrimitiveWrapperClass(float.class));
    }

    @Test
    @DisplayName("Primitive void -> Void")
    void primitiveVoidIsConvertedToVoid() {
        assertEquals(Void.class, ClassUtilities.toPrimitiveWrapperClass(void.class));
    }

    /* -----------------------------------------------------------------
     *  2) Non‑primitive classes are returned unchanged.
     * ----------------------------------------------------------------- */
    @Test
    @DisplayName("Non‑primitive class is returned unchanged")
    void nonPrimitiveIsReturnedUnchanged() {
        assertSame(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
        assertSame(Object.class, ClassUtilities.toPrimitiveWrapperClass(Object.class));
        // wrapper, not primitive
        assertSame(Integer.class, ClassUtilities.toPrimitiveWrapperClass(Integer.class));
        // array type
        assertSame(int[].class, ClassUtilities.toPrimitiveWrapperClass(int[].class));
    }

    /* -----------------------------------------------------------------
     *  3) Null argument results in NullPointerException (as per contract of
     *     Class.isPrimitive()).
     * ----------------------------------------------------------------- */
    @Test
    @DisplayName("Null argument throws NullPointerException")
    void nullArgumentThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ClassUtilities.toPrimitiveWrapperClass(null));
    }

    /* -----------------------------------------------------------------
     *  4) Defensive test: if the internal map were missing an entry,
     *     the method would throw IllegalArgumentException.  We cannot
     *     manipulate the private map directly, but we can verify the
     *     exception type for a known primitive that is *not* a primitive
     *     (e.g., a custom primitive‑like class).  This test documents the
     *     expected behaviour without breaking encapsulation.
     * ----------------------------------------------------------------- */
    @Test
    @DisplayName("Non‑primitive class that is not primitive does not throw")
    void nonPrimitiveDoesNotThrow() {
        // The method should simply return the argument; no exception expected.
        assertDoesNotThrow(() -> ClassUtilities.toPrimitiveWrapperClass(ClassUtilities.class));
    }

    /* -----------------------------------------------------------------
     *  1) Primitive → Wrapper conversion (parameterized).
     * ----------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] primitive {0} → wrapper {1}")
    @CsvSource({ "boolean, java.lang.Boolean", "byte,    java.lang.Byte", "char,    java.lang.Character", "short,   java.lang.Short", "int,     java.lang.Integer", "long,    java.lang.Long", "float,   java.lang.Float", "double,  java.lang.Double", "void,    java.lang.Void" })
    @DisplayName("Primitive types are correctly converted to their wrapper classes")
    void primitiveToWrapper_isConverted(String primitiveName, String expectedWrapperFqn) throws ClassNotFoundException {
        // Resolve the primitive class (e.g., "int" → int.class)
        Class<?> primitiveClass = getPrimitiveClassByName(primitiveName);
        Class<?> expectedWrapper = Class.forName(expectedWrapperFqn);
        assertEquals(expectedWrapper, ClassUtilities.toPrimitiveWrapperClass(primitiveClass), () -> "Expected wrapper for primitive " + primitiveName);
    }
}
