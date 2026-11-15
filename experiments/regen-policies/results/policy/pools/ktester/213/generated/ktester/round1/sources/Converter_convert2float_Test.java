package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;

class Converter_convert2float_Test {

    @Test
    void convert2float_nullInput_returnsZero() {
        // GIVEN a null source
        Object source = null;
        // WHEN converting to float
        float result = Converter.convert2float(source);
        // THEN the result is the defined zero constant
        assertEquals(CommonValues.FLOAT_ZERO, result, "convert2float(null) should return CommonValues.FLOAT_ZERO");
    }

    @Test
    void convert2float_stringInput_isParsedCorrectly() {
        // GIVEN a numeric string
        String source = "3.14";
        // WHEN converting to float
        float result = Converter.convert2float(source);
        // THEN the float value matches the parsed number
        assertEquals(3.14f, result, 0.000001f, "convert2float(\"3.14\") should return 3.14f");
    }

    @Test
    void convert2float_integerInput_isConvertedToFloat() {
        // GIVEN an Integer instance
        Integer source = 5;
        // WHEN converting to float
        float result = Converter.convert2float(source);
        // THEN the float value equals the integer value as a float
        assertEquals(5.0f, result, 0.000001f, "convert2float(Integer) should return the integer value as a float");
    }

    @Test
    void convert2float_doubleInput_isConvertedToFloat() {
        // GIVEN a Double instance
        Double source = 2.5d;
        // WHEN converting to float
        float result = Converter.convert2float(source);
        // THEN the float value equals the double value truncated to float precision
        assertEquals(2.5f, result, 0.000001f, "convert2float(Double) should return the double value as a float");
    }

    @Test
    void convert2float_booleanTrue_isConvertedToOne() {
        // GIVEN a Boolean true
        Boolean source = Boolean.TRUE;
        // WHEN converting to float
        float result = Converter.convert2float(source);
        // THEN true is interpreted as numeric 1
        assertEquals(1.0f, result, 0.000001f, "convert2float(Boolean.TRUE) should return 1.0f");
    }

    @Test
    void convert2float_booleanFalse_isConvertedToZero() {
        // GIVEN a Boolean false
        Boolean source = Boolean.FALSE;
        // WHEN converting to float
        float result = Converter.convert2float(source);
        // THEN false is interpreted as numeric 0
        assertEquals(0.0f, result, 0.000001f, "convert2float(Boolean.FALSE) should return 0.0f");
    }

    @Test
    void convert2float_invalid_type_throwsIllegalArgumentException() {
        // GIVEN an object of a type that has no conversion registered
        Object source = new Object();
        // WHEN / THEN an IllegalArgumentException is expected
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2float(source), "Converting an unsupported Object should throw IllegalArgumentException");
    }

    @Test
    void convert2float_invalid_string_throwsNumberFormatException() {
        // GIVEN a non‑numeric string
        String source = "abcXYZ";
        // WHEN / THEN a NumberFormatException is expected
        assertThrows(NumberFormatException.class, () -> Converter.convert2float(source), "A non‑numeric string should cause NumberFormatException");
    }

    @Test
    void convert2float_unsupported_boolean_throwsIllegalArgumentException() throws Exception {
        // GIVEN a Boolean true that we want the converter to reject
        Boolean source = Boolean.TRUE;
        // Replace the private static instance with a mock that throws IllegalArgumentException
        Field instanceField = Converter.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        Converter originalInstance = (Converter) instanceField.get(null);
        Converter mockInstance = Mockito.mock(Converter.class);
        // Configure the mock to throw IllegalArgumentException for Boolean input
        Mockito.when(mockInstance.convert(Mockito.eq(source), Mockito.eq(float.class))).thenThrow(new IllegalArgumentException("Boolean to float not supported"));
        // Swap the instance
        instanceField.set(null, mockInstance);
        try {
            // WHEN / THEN the conversion should throw IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2float(source), "Boolean conversion should be rejected with IllegalArgumentException");
        } finally {
            // Restore the original instance to avoid affecting other tests
            instanceField.set(null, originalInstance);
        }
    }

    @Test
    void convert2float_array_input_throwsIllegalArgumentException() {
        // GIVEN an int array (unsupported source type)
        int[] source = new int[] { 1, 2, 3 };
        // WHEN / THEN an IllegalArgumentException is expected
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2float(source), "Array input should trigger IllegalArgumentException");
    }
}
