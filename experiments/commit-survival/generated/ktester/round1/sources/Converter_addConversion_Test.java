package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_addConversion_Test {

    private Converter converter;

    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the mock options to return an empty override map.
        when(mockOptions.getConverterOverrides()).thenReturn(new ConcurrentHashMap<>());
        // The other option methods are not used by the constructor,
        // but we provide safe defaults to avoid NullPointerExceptions.
        when(mockOptions.getCustomOption(any())).thenReturn(null);
        when(mockOptions.getCharset()).thenReturn(null);
        when(mockOptions.getZoneId()).thenReturn(null);
        when(mockOptions.trueChar()).thenReturn(null);
        when(mockOptions.falseChar()).thenReturn(null);
        // Create the Converter instance under test.
        converter = new Converter(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to a clean state for the next test.
        MockitoAnnotations.clearAllCaches();
    }

    @Test
    void addConversionStoresConversionAndReturnsPreviousIfAny() {
        // Arrange – a mock conversion that will return a fixed value.
        @SuppressWarnings("unchecked")
        Convert<Object> firstConversion = mock(Convert.class);
        when(firstConversion.convert(any(), any())).thenReturn(42);
        // Act – add the conversion for String → Integer.
        Convert<?> previous = converter.addConversion(String.class, Integer.class, firstConversion);
        // Assert – there was no previous conversion.
        assertNull(previous, "Expected no previous conversion for the first registration");
        // Verify that the conversion is actually used by the public API.
        Integer result = converter.convert("any string", Integer.class);
        assertEquals(42, result, "The registered conversion should be invoked by convert()");
        // Arrange – a second conversion for the same source/target pair.
        @SuppressWarnings("unchecked")
        Convert<Object> secondConversion = mock(Convert.class);
        when(secondConversion.convert(any(), any())).thenReturn(99);
        // Act – register the second conversion.
        Convert<?> returned = converter.addConversion(String.class, Integer.class, secondConversion);
        // Assert – the first conversion should be returned as the previous one.
        assertSame(firstConversion, returned, "addConversion should return the previously registered conversion");
        // Verify that the new conversion now takes effect.
        Integer newResult = converter.convert("another string", Integer.class);
        assertEquals(99, newResult, "The newly registered conversion should replace the old one");
    }

    /* -------------------------------------------------------------
     *  Primitive ↔ Wrapper handling
     * ------------------------------------------------------------- */
    @Test
    void primitiveToPrimitiveConversionIsStoredAsWrapperPair() {
        // dummyConvert1 will be used for int → double (both primitives)
        @SuppressWarnings("unchecked")
        Convert<Object> dummyConvert1 = mock(Convert.class);
        when(dummyConvert1.convert(any(), any())).thenReturn(1.23);
        // First insertion – expect null because the pair is new.
        Convert<?> previous = converter.addConversion(int.class, double.class, dummyConvert1);
        assertNull(previous, "First registration for Integer→Double must return null");
        // Verify that the conversion is reachable via the public API.
        Double result = converter.convert(5, Double.class);
        assertEquals(1.23, result);
        verify(dummyConvert1).convert(any(), eq(Double.class));
    }

    @Test
    void wrapperToWrapperConversionIsStoredWithoutChange() {
        @SuppressWarnings("unchecked")
        Convert<Object> dummyConvert2 = mock(Convert.class);
        when(dummyConvert2.convert(any(), any())).thenReturn(9.87);
        Convert<?> previous = converter.addConversion(Integer.class, Double.class, dummyConvert2);
        assertNull(previous, "First registration for Integer→Double must return null");
        Double result = converter.convert(10, Double.class);
        assertEquals(9.87, result);
        verify(dummyConvert2).convert(any(), eq(Double.class));
    }

    @Test
    void overwriteExistingPrimitiveWrapperPairReturnsPrevious() {
        // First conversion (int → Double)
        @SuppressWarnings("unchecked")
        Convert<Object> dummyConvert1 = mock(Convert.class);
        when(dummyConvert1.convert(any(), any())).thenReturn(100);
        converter.addConversion(int.class, double.class, dummyConvert1);
        // Second conversion for the same logical pair (Integer → Double)
        @SuppressWarnings("unchecked")
        Convert<Object> dummyConvert3 = mock(Convert.class);
        when(dummyConvert3.convert(any(), any())).thenReturn(200);
        Convert<?> previous = converter.addConversion(int.class, Double.class, dummyConvert3);
        assertSame(dummyConvert1, previous, "addConversion should return the previously stored conversion");
        Double result = converter.convert(1, Double.class);
        assertEquals(200, result);
        verify(dummyConvert3).convert(any(), eq(Double.class));
    }

    @Test
    void primitiveSourceAndWrapperTargetBecomeSameWrapperPair() {
        @SuppressWarnings("unchecked")
        Convert<Object> dummyConvert13 = mock(Convert.class);
        when(dummyConvert13.convert(any(), any())).thenReturn(555);
        Convert<?> previous = converter.addConversion(int.class, Integer.class, dummyConvert13);
        assertNull(previous, "First registration for Integer→Integer must return null");
        Integer result = converter.convert(42, Integer.class);
        assertEquals(555, result);
        verify(dummyConvert13).convert(any(), eq(Integer.class));
    }

    @Test
    void overwriteSameLogicalPairReturnsPreviousConversion() {
        @SuppressWarnings("unchecked")
        Convert<Object> dummyConvert13 = mock(Convert.class);
        when(dummyConvert13.convert(any(), any())).thenReturn(111);
        converter.addConversion(int.class, Integer.class, dummyConvert13);
        @SuppressWarnings("unchecked")
        Convert<Object> dummyConvert14 = mock(Convert.class);
        when(dummyConvert14.convert(any(), any())).thenReturn(222);
        Convert<?> previous = converter.addConversion(int.class, Integer.class, dummyConvert14);
        assertSame(dummyConvert13, previous, "Previous conversion should be returned on overwrite");
    }

    @Test
    void wrapperToPrimitiveTargetIsConvertedToWrapper() {
        @SuppressWarnings("unchecked")
        Convert<Object> dummyConvert6 = mock(Convert.class);
        when(dummyConvert6.convert(any(), any())).thenReturn(777);
        Convert<?> previous = converter.addConversion(Integer.class, int.class, dummyConvert6);
        assertNull(previous, "First registration for Integer→Integer must return null");
        Integer result = converter.convert(99, Integer.class);
        assertEquals(777, result);
        verify(dummyConvert6).convert(any(), eq(Integer.class));
    }

    @Test
    void wrapperToPrimitiveBooleanConversion() {
        @SuppressWarnings("unchecked")
        Convert<Object> dummyConvert7 = mock(Convert.class);
        when(dummyConvert7.convert(any(), any())).thenReturn(true);
        Convert<?> previous = converter.addConversion(Boolean.class, boolean.class, dummyConvert7);
        assertNull(previous, "First registration for Boolean→Boolean must return null");
        Boolean result = converter.convert(false, Boolean.class);
        assertTrue(result);
        verify(dummyConvert7).convert(any(), eq(Boolean.class));
    }

    /* -------------------------------------------------------------
     *  Null arguments handling
     * ------------------------------------------------------------- */
    @Test
    void nullConversionFunctionIsAcceptedAndReturnedOnOverwrite() {
        @SuppressWarnings("unchecked")
        Convert<Object> dummyConvert11 = mock(Convert.class);
        when(dummyConvert11.convert(any(), any())).thenReturn("first");
        // First registration – non‑null conversion.
        Convert<?> previous1 = converter.addConversion(Long.class, String.class, dummyConvert11);
        assertNull(previous1, "First registration should return null");
        // Second registration – null conversion function.
        Convert<?> previous2 = converter.addConversion(Long.class, String.class, null);
        assertSame(dummyConvert11, previous2, "Previous non‑null conversion must be returned when overwriting with null");
    }

    @Test
    void nullSourceClassThrowsException() {
        @SuppressWarnings("unchecked")
        Convert<Object> dummyConvert4 = mock(Convert.class);
        assertThrows(NullPointerException.class, () -> converter.addConversion(null, String.class, dummyConvert4), "Passing a null source class should propagate NullPointerException");
    }

    @Test
    void nullTargetClassThrowsException() {
        @SuppressWarnings("unchecked")
        Convert<Object> dummyConvert10 = mock(Convert.class);
        assertThrows(NullPointerException.class, () -> converter.addConversion(String.class, null, dummyConvert10), "Passing a null target class should propagate NullPointerException");
    }

    @Test
    void nullConversionFunctionWhenNoPreviousEntryReturnsNull() {
        // Directly add a null conversion for a fresh pair.
        Convert<?> previous = converter.addConversion(Long.class, String.class, null);
        assertNull(previous, "Adding a null conversion for a new pair should return null");
    }

    /* -------------------------------------------------------------
     *  Array type handling
     * ------------------------------------------------------------- */
    @Test
    void arrayTypesAreStoredAsGivenWithoutWrapperConversion() {
        @SuppressWarnings("unchecked")
        Convert<Object> dummyConvert12 = mock(Convert.class);
        when(dummyConvert12.convert(any(), any())).thenReturn(new Object[0]);
        Convert<?> previous = converter.addConversion(int[].class, Object[].class, dummyConvert12);
        assertNull(previous, "First registration for int[]→Object[] must return null");
        Object[] result = converter.convert(new int[] { 1, 2, 3 }, Object[].class);
        assertNotNull(result, "Conversion result should not be null");
        verify(dummyConvert12).convert(any(), eq(Object[].class));
    }

    /* -------------------------------------------------------------
     *  Identity conversion (same source & target) handling
     * ------------------------------------------------------------- */
    @Test
    void identityConversionFirstInsertReturnsNullAndSecondReturnsPrevious() {
        @SuppressWarnings("unchecked")
        Convert<Object> dummyConvert8 = mock(Convert.class);
        when(dummyConvert8.convert(any(), any())).thenReturn("first");
        Convert<?> first = converter.addConversion(String.class, String.class, dummyConvert8);
        assertNull(first, "First identity conversion should return null");
        @SuppressWarnings("unchecked")
        Convert<Object> dummyConvert9 = mock(Convert.class);
        when(dummyConvert9.convert(any(), any())).thenReturn("second");
        Convert<?> second = converter.addConversion(String.class, String.class, dummyConvert9);
        assertSame(dummyConvert8, second, "Second registration should return the previous conversion");
    }

    /* -------------------------------------------------------------
     *  Primitive target handling for non‑primitive source
     * ------------------------------------------------------------- */
    @Test
    void nonPrimitiveSourceWithPrimitiveTargetIsConvertedToWrapperTarget() {
        @SuppressWarnings("unchecked")
        Convert<Object> dummyConvert5 = mock(Convert.class);
        when(dummyConvert5.convert(any(), any())).thenReturn(123L);
        Convert<?> previous = converter.addConversion(java.util.Date.class, long.class, dummyConvert5);
        assertNull(previous, "First registration for Date→Long must return null");
        Long result = converter.convert(new java.util.Date(), Long.class);
        assertEquals(123L, result);
        verify(dummyConvert5).convert(any(), eq(Long.class));
    }
}
