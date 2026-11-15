package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Year;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class YearConversions_toShort_Test {

    /**
     * The Converter used for all test cases.
     */
    private Converter converter;

    /**
     * Mocked ConverterOptions – the real Converter only needs the overrides map.
     */
    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Stub the options to return an empty overrides map.
        when(mockOptions.getConverterOverrides()).thenReturn(Collections.emptyMap());
        // Create a real Converter instance using the mocked options.
        converter = new Converter(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions for a clean state between tests.
        reset(mockOptions);
    }

    private static Stream<Arguments> validNumericProvider() {
        return Stream.of(// Integer
        Arguments.of(Integer.valueOf(123), (short) 123), // Short (negative)
        Arguments.of(Short.valueOf((short) -45), (short) -45), // Long within short range
        Arguments.of(Long.valueOf(32760L), (short) 32760), // BigDecimal
        Arguments.of(BigDecimal.valueOf(200), (short) 200), // Byte
        Arguments.of(Byte.valueOf((byte) 7), (short) 7));
    }

    private static Stream<Arguments> stringConversionProvider() {
        return Stream.of(Arguments.of("-123", (short) -123), Arguments.of("32767", Short.MAX_VALUE), // overflow just above Short.MAX_VALUE -> cast to short yields -32768
        Arguments.of("32768", (short) -32768), // non‑numeric string
        Arguments.of("notANumber", NumberFormatException.class), // empty string
        Arguments.of("", NumberFormatException.class), // whitespace‑padded string (Integer.parseInt does not trim)
        Arguments.of("  42  ", NumberFormatException.class), // value exceeding Integer.MAX_VALUE
        Arguments.of("2147483648", NumberFormatException.class));
    }

    @Test
    void toShort_fromYear_returnsYearValueAsShort() {
        Year year = Year.of(2021);
        short actual = YearConversions.toShort(year, converter);
        assertEquals((short) 2021, actual, "Year.of(2021) should be converted to short 2021");
    }

    @Test
    void toShort_fromInteger_returnsSameValueAsShort() {
        Integer value = 12_345;
        short actual = YearConversions.toShort(value, converter);
        assertEquals((short) 12_345, actual);
    }

    @Test
    void toShort_fromStringNumeric_returnsParsedShort() {
        // Short.MAX_VALUE
        String numeric = "32767";
        short actual = YearConversions.toShort(numeric, converter);
        assertEquals(Short.MAX_VALUE, actual);
    }

    @Test
    void toShort_fromStringOverflow_behavesLikeCastToShort() {
        // 40_000 does not fit in a short; the cast truncates the high-order bits.
        String overflow = "40000";
        short actual = YearConversions.toShort(overflow, converter);
        // Expected value is -25536
        assertEquals((short) 40_000, actual);
    }

    @Test
    void toShort_fromNull_returnsZero() {
        short actual = YearConversions.toShort(null, converter);
        assertEquals((short) 0, actual);
    }

    @Test
    void toShort_fromLong_returnsCastedShort() {
        Long value = 9_876L;
        short actual = YearConversions.toShort(value, converter);
        assertEquals((short) 9_876, actual);
    }

    @Test
    void toShort_fromBigDecimal_returnsCastedShort() {
        java.math.BigDecimal bd = new java.math.BigDecimal("12345");
        short actual = YearConversions.toShort(bd, converter);
        assertEquals((short) 12_345, actual);
    }

    @Test
    void toShort_fromBoolean_true_returnsOne() {
        Boolean bool = Boolean.TRUE;
        short actual = YearConversions.toShort(bool, converter);
        assertEquals((short) 1, actual);
    }

    @Test
    void toShort_fromBoolean_false_returnsZero() {
        Boolean bool = Boolean.FALSE;
        short actual = YearConversions.toShort(bool, converter);
        assertEquals((short) 0, actual);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("validNumericProvider")
    void toShort_validNumericConversions(Object from, short expected) {
        short actual = YearConversions.toShort(from, converter);
        assertEquals(expected, actual, () -> "Conversion of " + from + " should yield " + expected);
    }

    @ParameterizedTest(name = "[{index}] string=\"{0}\" → expected={1}")
    @MethodSource("stringConversionProvider")
    void toShort_stringConversions(String input, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expect an exception
            assertThrows((Class<? extends Throwable>) expected, () -> YearConversions.toShort(input, converter), "Parsing \"" + input + "\" should throw " + ((Class<?>) expected).getSimpleName());
        } else {
            short actual = YearConversions.toShort(input, converter);
            assertEquals(((Number) expected).shortValue(), actual, "Parsing \"" + input + "\" should yield " + expected);
        }
    }

    @Test
    void toShort_fromNull_throwsNullPointerException() {
        // According to the JSON spec, a null source should cause NPE.
        assertThrows(NullPointerException.class, () -> YearConversions.toShort(null, converter), "Passing null as the source should throw NullPointerException");
    }

    @Test
    void toShort_fromUnsupportedObject_throwsIllegalArgumentException() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toShort(unsupported, converter), "Unsupported source type should trigger IllegalArgumentException");
    }

    @Test
    void toShort_customConverterHandlesNonNumericString() {
        // Create a custom Converter that knows how to convert the literal "custom" to 42.
        Converter customConverter = new Converter(mockOptions) {

            @Override
            public Object convert(Object from, Class<?> toClass) {
                if ("custom".equals(from) && toClass == Short.class) {
                    return Short.valueOf((short) 42);
                }
                // delegate to the normal conversion for everything else
                return super.convert(from, toClass);
            }
        };
        short actual = YearConversions.toShort("custom", customConverter);
        assertEquals((short) 42, actual, "Custom converter should convert \"custom\" to short 42");
    }

    @Test
    void toShort_fromBigInteger_returnsCastedShort() {
        BigInteger bi = new BigInteger("12345");
        short actual = YearConversions.toShort(bi, converter);
        assertEquals((short) 12345, actual);
    }
}
