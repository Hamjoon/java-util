package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class Converter_convertToByte_Test {

    static Stream<org.junit.jupiter.params.provider.Arguments> numericWithinRange() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("Byte instance should be returned unchanged", Byte.valueOf((byte) 5), Byte.valueOf((byte) 5)), org.junit.jupiter.params.provider.Arguments.of("Integer within range", Integer.valueOf(42), Byte.valueOf((byte) 42)), org.junit.jupiter.params.provider.Arguments.of("Long within range", Long.valueOf(100L), Byte.valueOf((byte) 100)), org.junit.jupiter.params.provider.Arguments.of("Short within range", Short.valueOf((short) -12), Byte.valueOf((byte) -12)), org.junit.jupiter.params.provider.Arguments.of("Double truncated toward zero", Double.valueOf(7.9), Byte.valueOf((byte) 7)), org.junit.jupiter.params.provider.Arguments.of("BigDecimal integral value", BigDecimal.valueOf(55), Byte.valueOf((byte) 55)), org.junit.jupiter.params.provider.Arguments.of("BigInteger at upper bound", BigInteger.valueOf(127), Byte.valueOf((byte) 127)));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> numericOutOfRange() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("Integer larger than 127", Integer.valueOf(200), IllegalArgumentException.class), org.junit.jupiter.params.provider.Arguments.of("Long smaller than -128", Long.valueOf(-200L), IllegalArgumentException.class), org.junit.jupiter.params.provider.Arguments.of("BigInteger outside byte range", BigInteger.valueOf(300), IllegalArgumentException.class), org.junit.jupiter.params.provider.Arguments.of("Double truncates to 128 (out of range)", Double.valueOf(128.0), IllegalArgumentException.class));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> stringConversions() {
        return Stream.of(// successful parses
        org.junit.jupiter.params.provider.Arguments.of("Numeric string within range", "64", Byte.valueOf((byte) 64)), org.junit.jupiter.params.provider.Arguments.of("Lower bound string", "-128", Byte.valueOf((byte) -128)), org.junit.jupiter.params.provider.Arguments.of("String with surrounding whitespace", "  15  ", Byte.valueOf((byte) 15)), org.junit.jupiter.params.provider.Arguments.of("String with explicit plus sign", "+10", Byte.valueOf((byte) 10)), org.junit.jupiter.params.provider.Arguments.of("String with leading zeros", "0012", Byte.valueOf((byte) 12)), // expected failures
        org.junit.jupiter.params.provider.Arguments.of("String numeric value outside range", "129", NumberFormatException.class), org.junit.jupiter.params.provider.Arguments.of("Non‑numeric string", "notANumber", NumberFormatException.class), org.junit.jupiter.params.provider.Arguments.of("Hexadecimal notation not supported", "0x10", NumberFormatException.class), org.junit.jupiter.params.provider.Arguments.of("Empty string", "", NumberFormatException.class), org.junit.jupiter.params.provider.Arguments.of("String containing only whitespace", "   ", NumberFormatException.class), org.junit.jupiter.params.provider.Arguments.of("String \"true\" is not numeric", "true", NumberFormatException.class), org.junit.jupiter.params.provider.Arguments.of("Decimal string not allowed", "12.34", NumberFormatException.class), org.junit.jupiter.params.provider.Arguments.of("Scientific notation unsupported", "1e2", NumberFormatException.class), org.junit.jupiter.params.provider.Arguments.of("Underscore in numeric string not allowed", "1_2", NumberFormatException.class));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> booleanAndCharacterConversions() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("Boolean true → 1", Boolean.TRUE, Byte.valueOf((byte) 1)), org.junit.jupiter.params.provider.Arguments.of("Boolean false → 0", Boolean.FALSE, Byte.valueOf((byte) 0)), org.junit.jupiter.params.provider.Arguments.of("Character 'A' → 65", Character.valueOf('A'), Byte.valueOf((byte) 65)), org.junit.jupiter.params.provider.Arguments.of("Character '\\u00FF' overflows to -1", Character.valueOf('\u00FF'), Byte.valueOf((byte) -1)), org.junit.jupiter.params.provider.Arguments.of("Null character maps to 0", Character.valueOf('\u0000'), Byte.valueOf((byte) 0)), org.junit.jupiter.params.provider.Arguments.of("Character '\\u1234' lower 8 bits = 0x34 (52)", Character.valueOf('\u1234'), Byte.valueOf((byte) 0x34)));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> additionalNumericEdgeValues() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("Byte.MIN_VALUE unchanged", Byte.valueOf(Byte.MIN_VALUE), Byte.valueOf(Byte.MIN_VALUE)), org.junit.jupiter.params.provider.Arguments.of("Byte.MAX_VALUE unchanged", Byte.valueOf(Byte.MAX_VALUE), Byte.valueOf(Byte.MAX_VALUE)), org.junit.jupiter.params.provider.Arguments.of("Short smaller than -128", Short.valueOf((short) -200), IllegalArgumentException.class), org.junit.jupiter.params.provider.Arguments.of("Float truncated toward zero", Float.valueOf(9.9f), Byte.valueOf((byte) 9)), org.junit.jupiter.params.provider.Arguments.of("BigDecimal 127.9 → 127", BigDecimal.valueOf(127.9), Byte.valueOf((byte) 127)), org.junit.jupiter.params.provider.Arguments.of("BigDecimal 128.1 → out of range", BigDecimal.valueOf(128.1), IllegalArgumentException.class), org.junit.jupiter.params.provider.Arguments.of("BigDecimal -128.5 → -128", BigDecimal.valueOf(-128.5), Byte.valueOf((byte) -128)));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> atomicWrapperConversions() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("AtomicInteger 7 → 7", new AtomicInteger(7), Byte.valueOf((byte) 7)), org.junit.jupiter.params.provider.Arguments.of("AtomicLong -120 → -120", new AtomicLong(-120L), Byte.valueOf((byte) -120)), org.junit.jupiter.params.provider.Arguments.of("AtomicBoolean true → 1", new AtomicBoolean(true), Byte.valueOf((byte) 1)), org.junit.jupiter.params.provider.Arguments.of("AtomicBoolean false → 0", new AtomicBoolean(false), Byte.valueOf((byte) 0)));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> unsupportedTypes() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("Date instance unsupported", new Date()), org.junit.jupiter.params.provider.Arguments.of("java.util.Calendar unsupported", Calendar.getInstance()), org.junit.jupiter.params.provider.Arguments.of("LocalDate unsupported", LocalDate.now()), org.junit.jupiter.params.provider.Arguments.of("LocalDateTime unsupported", LocalDateTime.now()), org.junit.jupiter.params.provider.Arguments.of("ZonedDateTime unsupported", ZonedDateTime.now()), org.junit.jupiter.params.provider.Arguments.of("java.sql.Timestamp unsupported", java.sql.Timestamp.valueOf("2023-01-01 12:34:56")), org.junit.jupiter.params.provider.Arguments.of("UUID unsupported", UUID.randomUUID()), org.junit.jupiter.params.provider.Arguments.of("Plain Object unsupported", new Object()), org.junit.jupiter.params.provider.Arguments.of("Optional unsupported", Optional.of(5)), org.junit.jupiter.params.provider.Arguments.of("AtomicReference unsupported", new java.util.concurrent.atomic.AtomicReference<>(Byte.valueOf((byte) 5))), org.junit.jupiter.params.provider.Arguments.of("Instant unsupported", java.time.Instant.now()), org.junit.jupiter.params.provider.Arguments.of("java.sql.Date unsupported", java.sql.Date.valueOf(LocalDate.now())), org.junit.jupiter.params.provider.Arguments.of("Enum value unsupported", java.time.DayOfWeek.MONDAY), org.junit.jupiter.params.provider.Arguments.of("byte[] array unsupported", new byte[] { 1, 2, 3 }), org.junit.jupiter.params.provider.Arguments.of("short[] array unsupported", new short[] { 10, 20 }));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> floatingPointSpecialValues() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("Float NaN", Float.valueOf(Float.NaN)), org.junit.jupiter.params.provider.Arguments.of("Float +Infinity", Float.valueOf(Float.POSITIVE_INFINITY)), org.junit.jupiter.params.provider.Arguments.of("Float -Infinity", Float.valueOf(Float.NEGATIVE_INFINITY)), org.junit.jupiter.params.provider.Arguments.of("Double NaN", Double.valueOf(Double.NaN)), org.junit.jupiter.params.provider.Arguments.of("Double +Infinity", Double.valueOf(Double.POSITIVE_INFINITY)), org.junit.jupiter.params.provider.Arguments.of("Double -Infinity", Double.valueOf(Double.NEGATIVE_INFINITY)));
    }

    @Test
    void convertFromByte_returnsSameValue() {
        Byte source = 42;
        Byte result = Converter.convertToByte(source);
        assertEquals(source, result);
    }

    @Test
    void convertFromInteger_isNarrowedToByte() {
        // 123 fits in a byte (value 123)
        Integer source = 123;
        Byte result = Converter.convertToByte(source);
        assertEquals(Byte.valueOf(source.byteValue()), result);
    }

    @Test
    void convertFromLong_isNarrowedToByte() {
        // 200 fits in a byte (value -56 due to overflow)
        Long source = 200L;
        Byte result = Converter.convertToByte(source);
        assertEquals(Byte.valueOf(source.byteValue()), result);
    }

    @Test
    void convertFromShort_isNarrowedToByte() {
        Short source = 7;
        Byte result = Converter.convertToByte(source);
        assertEquals(Byte.valueOf(source.byteValue()), result);
    }

    @Test
    void convertFromDouble_isNarrowedToByte() {
        // truncates toward zero
        Double source = 9.99;
        Byte result = Converter.convertToByte(source);
        assertEquals(Byte.valueOf(source.byteValue()), result);
    }

    @Test
    void convertFromBigDecimal_isNarrowedToByte() {
        BigDecimal source = new BigDecimal("15");
        Byte result = Converter.convertToByte(source);
        assertEquals(Byte.valueOf(source.byteValue()), result);
    }

    @Test
    void convertFromBigInteger_isNarrowedToByte() {
        BigInteger source = new BigInteger("127");
        Byte result = Converter.convertToByte(source);
        assertEquals(Byte.valueOf(source.byteValue()), result);
    }

    @Test
    void convertFromStringNumeric_isParsed() {
        String source = "64";
        Byte result = Converter.convertToByte(source);
        assertEquals(Byte.valueOf((byte) 64), result);
    }

    @Test
    void convertFromBooleanTrue_returnsOne() {
        Boolean source = Boolean.TRUE;
        Byte result = Converter.convertToByte(source);
        assertEquals(Byte.valueOf((byte) 1), result);
    }

    @Test
    void convertFromBooleanFalse_returnsZero() {
        Boolean source = Boolean.FALSE;
        Byte result = Converter.convertToByte(source);
        assertEquals(Byte.valueOf((byte) 0), result);
    }

    @Test
    void convertFromNull_returnsNull() {
        assertNull(Converter.convertToByte(null));
    }

    @Test
    void convertFromUnsupportedType_throwsException() {
        // No conversion defined for plain Object
        Object source = new Object();
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToByte(source));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numericWithinRange")
    void numeric_within_range(String description, Object input, Byte expected) {
        Byte actual = Converter.convertToByte(input);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numericOutOfRange")
    void numeric_out_of_range(String description, Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToByte(input), description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringConversions")
    void string_conversions(String description, Object input, Object expected) {
        if (expected instanceof Class) {
            // Expected exception
            Class<? extends Throwable> ex = (Class<? extends Throwable>) expected;
            assertThrows(ex, () -> Converter.convertToByte(input), description);
        } else {
            // Expected Byte result
            Byte actual = Converter.convertToByte(input);
            assertEquals(expected, actual, description);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("booleanAndCharacterConversions")
    void boolean_and_character_conversions(String description, Object input, Byte expected) {
        Byte actual = Converter.convertToByte(input);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("additionalNumericEdgeValues")
    void additional_numeric_edge_values(String description, Object input, Object expected) {
        if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> Converter.convertToByte(input), description);
        } else {
            Byte actual = Converter.convertToByte(input);
            assertEquals(expected, actual, description);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("atomicWrapperConversions")
    void atomic_wrapper_conversions(String description, Object input, Byte expected) {
        Byte actual = Converter.convertToByte(input);
        assertEquals(expected, actual, description);
    }

    @org.junit.jupiter.api.Test
    void null_input_returns_null() {
        assertNull(Converter.convertToByte(null));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unsupportedTypes")
    void unsupported_types_throw(String description, Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToByte(input), description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("floatingPointSpecialValues")
    void floating_point_special_values(String description, Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToByte(input), description);
    }
}
