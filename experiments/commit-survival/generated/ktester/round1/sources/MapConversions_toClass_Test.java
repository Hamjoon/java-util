package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toClass_Test {

    @Mock
    Converter // mock is injected by MockitoExtension
    converter;

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock before each test to avoid cross‑test interference
        reset(converter);
    }

    /**
     * Verify that a map containing the key {@code "class"} with a fully‑qualified class name
     * is correctly converted to the corresponding {@link Class} object.
     */
    @Test
    void toClass_WithClassKey_ReturnsExpectedClass() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("class", "java.lang.String");
        // the Converter is expected to be asked to convert the string value to a Class
        when(converter.convert(eq("java.lang.String"), eq(Class.class))).thenReturn(String.class);
        // WHEN
        Class<?> result = MapConversions.toClass(map, converter);
        // THEN
        assertEquals(String.class, result);
        verify(converter, times(1)).convert(eq("java.lang.String"), eq(Class.class));
    }

    /**
     * Verify that a map containing the generic {@code "value"} key is also handled.
     * This mirrors the fallback behaviour used by other Map‑conversion methods.
     */
    @Test
    void toClass_WithValueKey_ReturnsExpectedClass() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("value", "java.lang.Integer");
        when(converter.convert(eq("java.lang.Integer"), eq(Class.class))).thenReturn(Integer.class);
        // WHEN
        Class<?> result = MapConversions.toClass(map, converter);
        // THEN
        assertEquals(Integer.class, result);
        verify(converter, times(1)).convert(eq("java.lang.Integer"), eq(Class.class));
    }

    /**
     * Verify that a {@code null} source returns {@code null}.
     */
    @Test
    void toClass_NullSource_ReturnsNull() {
        // GIVEN
        // (no map, source is null)
        // WHEN
        Class<?> result = MapConversions.toClass(null, converter);
        // THEN
        assertNull(result);
        // No interaction with the converter should happen for a null source
        verifyNoInteractions(converter);
    }

    /**
     * Verify that a map without any recognised keys yields {@code null}.
     */
    @Test
    void toClass_MapWithoutClassOrValueKey_ReturnsNull() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("unrelated", "something");
        // WHEN
        Class<?> result = MapConversions.toClass(map, converter);
        // THEN
        assertNull(result);
        // The converter should not be invoked because there is nothing to convert
        verifyNoInteractions(converter);
    }

    @Test
    void toClass_WhenSourceIsAlreadyClass_ReturnsSameInstance() {
        // GIVEN
        Class<?> source = String.class;
        // WHEN
        Class<?> result = MapConversions.toClass(source, converter);
        // THEN
        assertSame(String.class, result);
        // No conversion should be attempted
        verifyNoInteractions(converter);
    }

    @Test
    void toClass_MapWithClassKeyHoldingClassObject_ReturnsThatClass() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", java.util.Date.class);
        Class<?> result = MapConversions.toClass(map, converter);
        assertEquals(java.util.Date.class, result);
        verifyNoInteractions(converter);
    }

    @Test
    void toClass_MapWithValueKeyHoldingClassObject_ReturnsThatClass() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", java.time.LocalDate.class);
        Class<?> result = MapConversions.toClass(map, converter);
        assertEquals(java.time.LocalDate.class, result);
        verifyNoInteractions(converter);
    }

    @Test
    void toClass_MapWithClassKeyStringThatDoesNotExist_ThrowsClassNotFoundException() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", "com.example.DoesNotExist");
        when(converter.convert(eq("com.example.DoesNotExist"), eq(Class.class))).thenThrow(new ClassNotFoundException("com.example.DoesNotExist"));
        assertThrows(ClassNotFoundException.class, () -> MapConversions.toClass(map, converter));
        verify(converter, times(1)).convert(eq("com.example.DoesNotExist"), eq(Class.class));
    }

    @Test
    void toClass_EmptyMap_ThrowsIllegalArgumentException() {
        Map<String, Object> map = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toClass(map, converter));
        verifyNoInteractions(converter);
    }

    @Test
    void toClass_NullSource_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toClass(null, null));
    }

    @Test
    void toClass_MapWithBothClassAndValue_ClassWins() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", "java.lang.String");
        map.put("value", "java.util.HashMap");
        when(converter.convert(eq("java.lang.String"), eq(Class.class))).thenReturn(String.class);
        // The converter must NOT be asked for the "value" entry
        Class<?> result = MapConversions.toClass(map, converter);
        assertEquals(String.class, result);
        verify(converter, times(1)).convert(eq("java.lang.String"), eq(Class.class));
        verify(converter, never()).convert(eq("java.util.HashMap"), any());
    }

    @Test
    void toClass_ClassKeyStringWithWhitespace_IsTrimmedBeforeConversion() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", "  java.util.Date  ");
        when(converter.convert(eq("java.util.Date"), eq(Class.class))).thenReturn(java.util.Date.class);
        Class<?> result = MapConversions.toClass(map, converter);
        assertEquals(java.util.Date.class, result);
        verify(converter).convert(eq("java.util.Date"), eq(Class.class));
    }

    @Test
    void toClass_ClassKeyPrimitiveName_ReturnsPrimitiveClass() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", "int");
        when(converter.convert(eq("int"), eq(Class.class))).thenReturn(int.class);
        Class<?> result = MapConversions.toClass(map, converter);
        assertEquals(int.class, result);
        verify(converter).convert(eq("int"), eq(Class.class));
    }

    @Test
    void toClass_ClassKeyArrayType_ReturnsArrayClass() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", "java.lang.String[]");
        when(converter.convert(eq("java.lang.String[]"), eq(Class.class))).thenReturn(String[].class);
        Class<?> result = MapConversions.toClass(map, converter);
        assertEquals(String[].class, result);
        verify(converter).convert(eq("java.lang.String[]"), eq(Class.class));
    }

    @Test
    void toClass_ClassKeyHoldingPrimitiveClassObject_ReturnsSamePrimitiveClass() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", int.class);
        Class<?> result = MapConversions.toClass(map, converter);
        assertEquals(int.class, result);
        verifyNoInteractions(converter);
    }

    @Test
    void toClass_ClassKeyNullValue_ThrowsIllegalArgumentException() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", null);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toClass(map, converter));
        verifyNoInteractions(converter);
    }

    @Test
    void toClass_ValueKeyInvalidType_ThrowsClassCastException() {
        Map<String, Object> map = new HashMap<>();
        // Integer, not String nor Class
        map.put("value", 12345);
        assertThrows(ClassCastException.class, () -> MapConversions.toClass(map, converter));
        verifyNoInteractions(converter);
    }

    @Test
    void toClass_BothKeysInvalid_ThrowsClassNotFoundException() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", "com.fake.Missing");
        map.put("value", 999);
        when(converter.convert(eq("com.fake.Missing"), eq(Class.class))).thenThrow(new ClassNotFoundException("com.fake.Missing"));
        assertThrows(ClassNotFoundException.class, () -> MapConversions.toClass(map, converter));
        verify(converter).convert(eq("com.fake.Missing"), eq(Class.class));
    }

    @Test
    void toClass_UnrecognisedKeyOnly_ThrowsIllegalArgumentException() {
        Map<String, Object> map = new HashMap<>();
        map.put("unknown", "java.lang.String");
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toClass(map, converter));
        verifyNoInteractions(converter);
    }

    @Test
    void toClass_SourceIsNeitherClassNorMap_ThrowsIllegalArgumentException() {
        String source = "just-a-string";
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toClass(source, converter));
        verifyNoInteractions(converter);
    }

    @Test
    void toClass_NullConverter_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toClass(String.class, null));
    }

    @Test
    void toClass_ClassKeyEmptyString_ThrowsClassNotFoundException() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", "");
        when(converter.convert(eq(""), eq(Class.class))).thenThrow(new ClassNotFoundException("Empty class name"));
        assertThrows(ClassNotFoundException.class, () -> MapConversions.toClass(map, converter));
        verify(converter).convert(eq(""), eq(Class.class));
    }

    @Test
    void toClass_ClassKeyWhitespaceOnly_ThrowsClassNotFoundException() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", "   ");
        // The implementation is expected to trim first, resulting in empty string
        when(converter.convert(eq(""), eq(Class.class))).thenThrow(new ClassNotFoundException("Whitespace only"));
        assertThrows(ClassNotFoundException.class, () -> MapConversions.toClass(map, converter));
        verify(converter).convert(eq(""), eq(Class.class));
    }

    @Test
    void toClass_MalformedClassName_ThrowsClassNotFoundException() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", "java..lang.String");
        when(converter.convert(eq("java..lang.String"), eq(Class.class))).thenThrow(new ClassNotFoundException("Malformed name"));
        assertThrows(ClassNotFoundException.class, () -> MapConversions.toClass(map, converter));
        verify(converter).convert(eq("java..lang.String"), eq(Class.class));
    }

    @Test
    void toClass_ClassKeyNullAndValueValid_ThrowsIllegalArgumentException() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", null);
        map.put("value", "java.util.HashMap");
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toClass(map, converter));
        verifyNoInteractions(converter);
    }

    @Test
    void toClass_ValueKeyNull_ThrowsIllegalArgumentException() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", null);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toClass(map, converter));
        verifyNoInteractions(converter);
    }

    @Test
    void toClass_ValueKeyNonExistentClass_ThrowsClassNotFoundException() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "com.example.MissingClass");
        when(converter.convert(eq("com.example.MissingClass"), eq(Class.class))).thenThrow(new ClassNotFoundException("Missing class"));
        assertThrows(ClassNotFoundException.class, () -> MapConversions.toClass(map, converter));
        verify(converter).convert(eq("com.example.MissingClass"), eq(Class.class));
    }

    @Test
    void toClass_ClassKeyPrimitiveArray_NotSupported_ThrowsClassNotFoundException() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", "int[]");
        when(converter.convert(eq("int[]"), eq(Class.class))).thenThrow(new ClassNotFoundException("Primitive array not supported"));
        assertThrows(ClassNotFoundException.class, () -> MapConversions.toClass(map, converter));
        verify(converter).convert(eq("int[]"), eq(Class.class));
    }
}
