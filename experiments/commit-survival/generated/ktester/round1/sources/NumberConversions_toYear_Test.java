package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Year;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toYear_Test {

    @Mock
    Converter // Converter is not used by toYear, but required by signature
    mockConverter;

    /**
     * Provides all valid conversion test cases extracted from the JSON description.
     */
    Stream<Arguments> validConversionProvider() {
        return Stream.of(// Group: Valid Number conversions
        Arguments.of("Convert a positive Integer within the typical year range.", Integer.valueOf(2020), null, Year.of(2020)), Arguments.of("Convert a Short value.", Short.valueOf((short) 1999), null, Year.of(1999)), Arguments.of("Convert a Long value; shortValue truncates to the low‑order 16 bits, which for 1850 fits exactly.", Long.valueOf(1850L), null, Year.of(1850)), Arguments.of("Convert a Double; shortValue truncates the fractional part, yielding 2022.", Double.valueOf(2022.99), null, Year.of(2022)), Arguments.of("Convert a BigDecimal; shortValue returns the integer part.", BigDecimal.valueOf(1975), null, Year.of(1975)), Arguments.of("Convert a negative Byte value, ensuring negative years are handled.", Byte.valueOf((byte) -44), null, Year.of(-44)), Arguments.of("Convert the maximum short value; still within Year limits.", Short.valueOf(Short.MAX_VALUE), null, Year.of(32767)), Arguments.of("Convert the minimum short value; verifies handling of the most negative short.", Short.valueOf(Short.MIN_VALUE), null, Year.of(-32768)), // Group: Additional valid Number conversions
        Arguments.of("Convert a zero Byte; shortValue yields 0, resulting in Year 0.", Byte.valueOf((byte) 0), null, Year.of(0)), Arguments.of("Convert a zero Short; verifies handling of the lower bound of short.", Short.valueOf((short) 0), null, Year.of(0)), Arguments.of("Convert a Float with fractional part; shortValue truncates the fraction.", Float.valueOf(1234.56f), null, Year.of(1234)), Arguments.of("Convert Float.NaN; Float.intValue() returns 0, so shortValue is 0, yielding Year 0.", Float.valueOf(Float.NaN), null, Year.of(0)), Arguments.of("Convert Float.POSITIVE_INFINITY; intValue() becomes Integer.MAX_VALUE, shortValue wraps to -1, resulting in Year -1.", Float.valueOf(Float.POSITIVE_INFINITY), null, Year.of(-1)), Arguments.of("Convert an AtomicInteger; confirms that subclasses of Number are accepted.", new AtomicInteger(2021), null, Year.of(2021)), Arguments.of("Convert an AtomicLong; verifies handling of larger atomic numeric types.", new AtomicLong(1995L), null, Year.of(1995)), Arguments.of("Convert a large BigInteger; shortValue keeps low‑order 16 bits (123456789 & 0xFFFF = 52501).", new BigInteger("123456789"), null, Year.of(52501)), Arguments.of("Convert a negative Integer whose shortValue wraps to a positive year (‑40000 → 25536).", Integer.valueOf(-40000), null, Year.of(25536)), Arguments.of("Convert a Long equal to 0xFFFF; shortValue becomes -1, testing the wrap‑around at the upper short limit.", Long.valueOf(65535L), null, Year.of(-1)), Arguments.of("Convert Short.MIN_VALUE with a non‑null Converter instance to ensure the converter argument is ignored.", Short.valueOf(Short.MIN_VALUE), mockConverter, Year.of(-32768)), // Edge‑case: overflow beyond short range
        Arguments.of("Integer 40000 exceeds the short range; shortValue wraps to -25536 (40000 - 65536). The method still returns a Year object for that value.", Integer.valueOf(40000), null, Year.of(-25536)));
    }

    /**
     * Provides all invalid‑input test cases extracted from the JSON description.
     */
    Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of("Passing a non‑Number object should cause a ClassCastException when the method attempts to cast.", "not a number", null, ClassCastException.class), Arguments.of("A null 'from' argument leads to a NullPointerException during the cast to Number.", null, null, NullPointerException.class));
    }

    @Test
    void toYear_withInteger_returnsCorrectYear() {
        Integer source = 2023;
        Year result = NumberConversions.toYear(source, mockConverter);
        assertEquals(Year.of(2023), result);
    }

    @Test
    void toYear_withShort_returnsCorrectYear() {
        Short source = (short) 1999;
        Year result = NumberConversions.toYear(source, mockConverter);
        assertEquals(Year.of(1999), result);
    }

    @Test
    void toYear_withLong_returnsCorrectYear() {
        Long source = 2000L;
        Year result = NumberConversions.toYear(source, mockConverter);
        assertEquals(Year.of(2000), result);
    }

    @Test
    void toYear_withDouble_truncatesToShortValue() {
        // shortValue() drops the fraction
        Double source = 2021.9;
        Year result = NumberConversions.toYear(source, mockConverter);
        assertEquals(Year.of(2021), result);
    }

    @Test
    void toYear_withBigDecimal_truncatesToShortValue() {
        BigDecimal source = new BigDecimal("2022");
        Year result = NumberConversions.toYear(source, mockConverter);
        assertEquals(Year.of(2022), result);
    }

    @Test
    void toYear_withNegativeNumber_returnsNegativeYear() {
        Integer source = -5;
        Year result = NumberConversions.toYear(source, mockConverter);
        assertEquals(Year.of(-5), result);
    }

    @Test
    void toYear_withNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toYear(null, mockConverter));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversionProvider")
    void toYear_validConversions(String description, Object from, Converter converter, Year expected) {
        // Act
        Year result = NumberConversions.toYear(from, converter);
        // Assert
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidInputProvider")
    void toYear_invalidInputs(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // Act & Assert
        assertThrows(expectedException, () -> NumberConversions.toYear(from, converter), description);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toYear_customNumberThrowsIllegalStateException() {
        Number badNumber = new Number() {

            @Override
            public int intValue() {
                return 0;
            }

            @Override
            public long longValue() {
                return 0L;
            }

            @Override
            public float floatValue() {
                return 0f;
            }

            @Override
            public double doubleValue() {
                return 0d;
            }

            @Override
            public byte byteValue() {
                return 0;
            }

            @Override
            public short shortValue() {
                throw new IllegalStateException("shortValue not supported");
            }
        };
        assertThrows(IllegalStateException.class, () -> NumberConversions.toYear(badNumber, null), "A custom Number subclass throws an exception from shortValue(); toYear should propagate the IllegalStateException.");
    }
}
