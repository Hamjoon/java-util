package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class Converter_convert2byte_Test {

    @Nested
    @DisplayName("Valid Conversions – Primitive and Wrapper Types")
    class PrimitiveAndWrapperTests {

        static Stream<Arguments> primitiveAndWrapperProvider() {
            return Stream.of(// Integer within range
            Arguments.of(5, (byte) 5), Arguments.of(123, (byte) 123), // Integer overflow – Java casts to low‑order 8 bits
            Arguments.of(300, (byte) 44), // Double – fractional part discarded
            Arguments.of(5.9, (byte) 5), // Boolean true / false
            Arguments.of(true, (byte) 1), Arguments.of(false, (byte) 0), // Numeric strings (trimmed, sign, etc.)
            Arguments.of("7", (byte) 7), Arguments.of("127", (byte) 127), Arguments.of("-128", (byte) -128), Arguments.of(" 42 ", (byte) 42), Arguments.of("+7", (byte) 7), // Edge numeric strings
            Arguments.of("255", (byte) -1), Arguments.of("0", (byte) 0));
        }

        @ParameterizedTest(name = "convert2byte({0}) => {1}")
        @MethodSource("primitiveAndWrapperProvider")
        void convert2byte_validPrimitiveOrString(Object input, byte expected) {
            byte result = Converter.convert2byte(input);
            assertEquals(expected, result, () -> "Expected conversion of " + input + " to be " + expected);
        }
    }

    @Nested
    @DisplayName("Valid Conversions – Byte Wrapper")
    class ByteWrapperTests {

        @Test
        void convert2byte_fromByteWrapper_returnsSameValue() {
            Byte source = Byte.valueOf((byte) 12);
            byte result = Converter.convert2byte(source);
            assertEquals((byte) 12, result);
        }
    }

    @Nested
    @DisplayName("Invalid Conversions – Non‑numeric / Non‑boolean Types")
    class InvalidNonNumericTests {

        @Test
        void convert2byte_nonNumericString_throwsNumberFormatException() {
            assertThrows(NumberFormatException.class, () -> Converter.convert2byte("not-a-number"), "Non‑numeric string should cause NumberFormatException");
        }

        @Test
        void convert2byte_arbitraryObject_throwsIllegalArgumentException() {
            Object obj = new Object();
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2byte(obj), "Arbitrary object without conversion should cause IllegalArgumentException");
        }

        @Test
        void convert2byte_list_throwsIllegalArgumentException() {
            List<Integer> list = Arrays.asList(1, 2, 3);
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2byte(list), "List should not be convertible to byte");
        }
    }

    @Nested
    @DisplayName("Numeric Types – Long and Short Conversions")
    class LongShortTests {

        @Test
        void convert2byte_fromLongOverflow() {
            // 300 -> low 8 bits = 44
            Long source = 300L;
            byte result = Converter.convert2byte(source);
            assertEquals((byte) 44, result);
        }

        @Test
        void convert2byte_fromShortUnderflow() {
            // -130 -> 126 after casting
            Short source = (short) -130;
            byte result = Converter.convert2byte(source);
            assertEquals((byte) 126, result);
        }
    }

    @Nested
    @DisplayName("Numeric Types – BigDecimal and BigInteger")
    class BigNumberTests {

        @Test
        void convert2byte_fromBigDecimal_discardsFraction() {
            BigDecimal bd = new BigDecimal("123.456");
            byte result = Converter.convert2byte(bd);
            assertEquals((byte) 123, result);
        }

        @Test
        void convert2byte_fromBigInteger_exact256_wrapsToZero() {
            BigInteger bi = new BigInteger("256");
            byte result = Converter.convert2byte(bi);
            assertEquals((byte) 0, result);
        }
    }

    @Nested
    @DisplayName("Atomic Number Types")
    class AtomicNumberTests {

        @Test
        void convert2byte_fromAtomicInteger_fitsInByte() {
            AtomicInteger ai = new AtomicInteger(7);
            byte result = Converter.convert2byte(ai);
            assertEquals((byte) 7, result);
        }

        @Test
        void convert2byte_fromAtomicLong_underflowWraps() {
            // -129 -> 127 after casting
            AtomicLong al = new AtomicLong(-129L);
            byte result = Converter.convert2byte(al);
            assertEquals((byte) 127, result);
        }
    }

    @Nested
    @DisplayName("Character and Char Wrapper Conversions")
    class CharacterTests {

        @Test
        void convert2byte_fromCharacter_A() {
            Character ch = 'A';
            byte result = Converter.convert2byte(ch);
            assertEquals((byte) 65, result);
        }

        @Test
        void convert2byte_fromCharacter_255() {
            // Unicode code point 255 (0xFF) should become -1 when cast to byte
            Character ch = '\u00FF';
            byte result = Converter.convert2byte(ch);
            assertEquals((byte) -1, result);
        }
    }

    @Nested
    @DisplayName("Boolean Wrapper Conversions")
    class BooleanWrapperTests {

        @Test
        void convert2byte_fromBooleanWrapperTrue() {
            Boolean b = Boolean.TRUE;
            byte result = Converter.convert2byte(b);
            assertEquals((byte) 1, result);
        }

        @Test
        void convert2byte_fromBooleanWrapperFalse() {
            Boolean b = Boolean.FALSE;
            byte result = Converter.convert2byte(b);
            assertEquals((byte) 0, result);
        }
    }

    @Nested
    @DisplayName("String Edge Cases – Whitespace and Sign")
    class StringEdgeTests {

        @Test
        void convert2byte_stringWithWhitespace() {
            byte result = Converter.convert2byte(" 42 ");
            assertEquals((byte) 42, result);
        }

        @Test
        void convert2byte_stringWithPlusSign() {
            byte result = Converter.convert2byte("+7");
            assertEquals((byte) 7, result);
        }

        @Test
        void convert2byte_emptyString_throwsNumberFormatException() {
            assertThrows(NumberFormatException.class, () -> Converter.convert2byte(""), "Empty string should cause NumberFormatException");
        }

        @Test
        void convert2byte_invalidNumericString_throwsNumberFormatException() {
            assertThrows(NumberFormatException.class, () -> Converter.convert2byte("12a"), "String containing non‑numeric characters should cause NumberFormatException");
        }
    }

    @Nested
    @DisplayName("Unsupported Types – Buffer, Date, Temporal, Concurrency, Collections, UUID")
    class UnsupportedTypesTests {

        @Test
        void convert2byte_ByteBuffer_throwsIllegalArgumentException() {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2byte(buffer), "ByteBuffer should not be convertible to byte");
        }

        @Test
        void convert2byte_CharBuffer_throwsIllegalArgumentException() {
            CharBuffer cb = CharBuffer.wrap("ABC");
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2byte(cb), "CharBuffer should not be convertible to byte");
        }

        @Test
        void convert2byte_StringBuilder_throwsIllegalArgumentException() {
            StringBuilder sb = new StringBuilder("42");
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2byte(sb), "StringBuilder should not be convertible to byte");
        }

        @Test
        void convert2byte_Date_throwsIllegalArgumentException() {
            Date date = new Date();
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2byte(date), "Date should not be convertible to byte");
        }

        @Test
        void convert2byte_Timestamp_throwsIllegalArgumentException() {
            java.sql.Timestamp ts = java.sql.Timestamp.valueOf("2023-01-01 12:34:56.789");
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2byte(ts), "Timestamp should not be convertible to byte");
        }

        @Test
        void convert2byte_LocalDate_throwsIllegalArgumentException() {
            java.time.LocalDate ld = java.time.LocalDate.of(2023, 1, 1);
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2byte(ld), "LocalDate should not be convertible to byte");
        }

        @Test
        void convert2byte_LocalDateTime_throwsIllegalArgumentException() {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.of(2023, 1, 1, 12, 34, 56);
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2byte(ldt), "LocalDateTime should not be convertible to byte");
        }

        @Test
        void convert2byte_ZonedDateTime_throwsIllegalArgumentException() {
            java.time.ZonedDateTime zdt = java.time.ZonedDateTime.parse("2023-01-01T12:34:56+01:00[Europe/Paris]");
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2byte(zdt), "ZonedDateTime should not be convertible to byte");
        }

        @Test
        void convert2byte_AtomicBoolean_throwsIllegalArgumentException() {
            AtomicBoolean ab = new AtomicBoolean(true);
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2byte(ab), "AtomicBoolean should not be convertible to byte");
        }

        @Test
        void convert2byte_AtomicLongWithoutConversion_throwsIllegalArgumentException() {
            AtomicLong al = new AtomicLong(123L);
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2byte(al), "AtomicLong (without explicit conversion) should not be convertible to byte");
        }

        @Test
        void convert2byte_byteArray_throwsIllegalArgumentException() {
            byte[] arr = new byte[] { 1, 2, 3 };
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2byte(arr), "byte[] should not be convertible to a single byte");
        }

        @Test
        void convert2byte_set_throwsIllegalArgumentException() {
            Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2byte(set), "Set should not be convertible to byte");
        }

        @Test
        void convert2byte_map_throwsIllegalArgumentException() {
            Map<String, Integer> map = Map.of("a", 1, "b", 2);
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2byte(map), "Map should not be convertible to byte");
        }

        @Test
        void convert2byte_UUID_throwsIllegalArgumentException() {
            UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2byte(uuid), "UUID should not be convertible to byte");
        }
    }

    @Test
    void convert2byte_nullInput_returnsZero() {
        byte result = Converter.convert2byte(null);
        assertEquals((byte) 0, result, "Null input should be converted to 0");
    }

    @Test
    void convert2byte_fromInteger_returnsCorrespondingByte() {
        Integer source = 42;
        byte result = Converter.convert2byte(source);
        assertEquals((byte) 42, result);
    }

    @Test
    void convert2byte_fromLong_returnsCorrespondingByte() {
        Long source = 127L;
        byte result = Converter.convert2byte(source);
        assertEquals((byte) 127, result);
    }

    @Test
    void convert2byte_fromStringNumeric_returnsParsedByte() {
        String source = "7";
        byte result = Converter.convert2byte(source);
        assertEquals((byte) 7, result);
    }

    @Test
    void convert2byte_fromBooleanTrue_returnsOne() {
        Boolean source = Boolean.TRUE;
        byte result = Converter.convert2byte(source);
        assertEquals((byte) 1, result);
    }

    @Test
    void convert2byte_fromBooleanFalse_returnsZero() {
        Boolean source = Boolean.FALSE;
        byte result = Converter.convert2byte(source);
        assertEquals((byte) 0, result);
    }

    @Test
    void convert2byte_fromNumberOutOfRange_wrapsAccordingToByteOverflow() {
        // 300 = 256 + 44, so the expected byte value is 44
        Integer source = 300;
        byte result = Converter.convert2byte(source);
        assertEquals((byte) 44, result);
    }

    @Test
    void convert2byte_fromChar_returnsCharCodeAsByte() {
        // ASCII 65
        Character source = 'A';
        byte result = Converter.convert2byte(source);
        assertEquals((byte) 65, result);
    }
}
