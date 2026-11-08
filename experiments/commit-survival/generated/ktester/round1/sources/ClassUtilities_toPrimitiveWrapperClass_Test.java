package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link ClassUtilities#toPrimitiveWrapperClass(Class)}.
 *
 * The method converts a primitive {@code Class} object to its corresponding wrapper class.
 * If the supplied class ClassUtilities_toPrimitiveWrapperClass_Test, it is returned unchanged.
 * A {@code NullPointerException} is expected when {@code null} is passed.
 */
@ExtendWith(MockitoExtension.class)
class ClassUtilities_toPrimitiveWrapperClass_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void primitiveIntIsConvertedToIntegerWrapper() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class));
    }

    @Test
    void primitiveBooleanIsConvertedToBooleanWrapper() {
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class));
    }

    @Test
    void primitiveCharIsConvertedToCharacterWrapper() {
        assertEquals(Character.class, ClassUtilities.toPrimitiveWrapperClass(char.class));
    }

    @Test
    void primitiveLongIsConvertedToLongWrapper() {
        assertEquals(Long.class, ClassUtilities.toPrimitiveWrapperClass(long.class));
    }

    @Test
    void primitiveFloatIsConvertedToFloatWrapper() {
        assertEquals(Float.class, ClassUtilities.toPrimitiveWrapperClass(float.class));
    }

    @Test
    void primitiveDoubleIsConvertedToDoubleWrapper() {
        assertEquals(Double.class, ClassUtilities.toPrimitiveWrapperClass(double.class));
    }

    @Test
    void primitiveByteIsConvertedToByteWrapper() {
        assertEquals(Byte.class, ClassUtilities.toPrimitiveWrapperClass(byte.class));
    }

    @Test
    void primitiveShortIsConvertedToShortWrapper() {
        assertEquals(Short.class, ClassUtilities.toPrimitiveWrapperClass(short.class));
    }

    @Test
    void primitiveVoidIsConvertedToVoidWrapper() {
        assertEquals(Void.class, ClassUtilities.toPrimitiveWrapperClass(void.class));
    }

    @Test
    void wrapperClassIsReturnedUnchanged() {
        assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(Integer.class));
        assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(Boolean.class));
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
    }

    @Test
    void nonPrimitiveClassIsReturnedUnchanged() {
        assertEquals(String.class, ClassUtilities.toPrimitiveWrapperClass(String.class));
        assertEquals(Object.class, ClassUtilities.toPrimitiveWrapperClass(Object.class));
        assertEquals(ClassUtilities.class, ClassUtilities.toPrimitiveWrapperClass(ClassUtilities.class));
    }

    @Test
    void nullArgumentThrowsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> ClassUtilities.toPrimitiveWrapperClass(null));
    }
}