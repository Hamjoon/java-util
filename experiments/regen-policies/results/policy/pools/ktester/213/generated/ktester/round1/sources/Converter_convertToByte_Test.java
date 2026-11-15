package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class Converter_convertToByte_Test {

    static Stream<Arguments> validConversions() {
        return Stream.of(// null → null
        arguments(null, null), // Byte identity
        arguments((byte) 5, Byte.valueOf((byte) 5)), // Integer within range
        arguments(123, Byte.valueOf((byte) 123)), // Long within range
        arguments(10L, Byte.valueOf((byte) 10)), // Double truncated
        arguments(3.14d, Byte.valueOf((byte) 3)), // Numeric String
        arguments("42", Byte.valueOf((byte) 42)), // Minimum Byte as String
        arguments("-128", Byte.valueOf((byte) -128)), // Boolean true / false
        arguments(true, Byte.valueOf((byte) 1)), arguments(false, Byte.valueOf((byte) 0)), // Character 'A' (Unicode 65)
        arguments('A', Byte.valueOf((byte) 65)), // BigDecimal truncated
        arguments(new BigDecimal("7.9"), Byte.valueOf((byte) 7)), // BigInteger within range
        arguments(new BigInteger("9"), Byte.valueOf((byte) 9)));
    }

    static Stream<Arguments> invalidConversions() {
        return Stream.of(// Integer overflow
        arguments(200, IllegalArgumentException.class), // String numeric overflow
        arguments("128", IllegalArgumentException.class), // Non‑numeric String
        arguments("abc", NumberFormatException.class), // Unsupported Date
        arguments(new java.util.Date(), IllegalArgumentException.class), // Completely unrelated object
        arguments(new Object(), IllegalArgumentException.class));
    }

    static Stream<Arguments> boundaryCases() {
        return Stream.of(arguments(127, Byte.valueOf((byte) 127)), arguments(-128, Byte.valueOf((byte) -128)), arguments(new BigDecimal("127"), Byte.valueOf((byte) 127)), arguments(new BigDecimal("-128"), Byte.valueOf((byte) -128)), arguments(new Short((short) 120), Byte.valueOf((byte) 120)), arguments(new Short((short) -120), Byte.valueOf((byte) -120)));
    }

    static Stream<Arguments> additionalNumeric() {
        return Stream.of(// Float truncated
        arguments(3.99f, Byte.valueOf((byte) 3), null), // Float wrapper negative
        arguments(new Float("-45.7"), Byte.valueOf((byte) -45), null), // AtomicInteger within range
        arguments(new AtomicInteger(55), Byte.valueOf((byte) 55), null), // AtomicLong within range
        arguments(new AtomicLong(100L), Byte.valueOf((byte) 100), null), // AtomicLong overflow
        arguments(new AtomicLong(200L), null, IllegalArgumentException.class));
    }

    static Stream<Arguments> stringEdgeCases() {
        return Stream.of(arguments("+42", Byte.valueOf((byte) 42), null), arguments("  9  ", Byte.valueOf((byte) 9), null), arguments("0010", Byte.valueOf((byte) 10), null), arguments("", null, NumberFormatException.class), arguments("  ", null, NumberFormatException.class));
    }

    static Stream<Arguments> booleanCharEdgeCases() {
        return Stream.of(arguments(Boolean.TRUE, Byte.valueOf((byte) 1)), arguments(Boolean.FALSE, Byte.valueOf((byte) 0)), // Unicode of '5'
        arguments('5', Byte.valueOf((byte) 53)), arguments('\u0000', Byte.valueOf((byte) 0)));
    }

    static Stream<Arguments> overflowOtherNumeric() {
        return Stream.of(arguments(new BigInteger("128"), IllegalArgumentException.class), arguments(new BigInteger("-129"), IllegalArgumentException.class), arguments(new Short((short) 200), IllegalArgumentException.class));
    }

    static Stream<Arguments> numericOverflow() {
        return Stream.of(arguments(new BigDecimal("128"), IllegalArgumentException.class), arguments(new BigDecimal("-129"), IllegalArgumentException.class));
    }

    @Test
    void convertNullReturnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Byte result = Converter.convertToByte(source);
        // THEN
        assertNull(result, "Converting null should produce a null Byte");
    }

    @Test
    void convertByteIsIdentity() {
        // GIVEN
        Byte source = (byte) 42;
        // WHEN
        Byte result = Converter.convertToByte(source);
        // THEN
        assertEquals(source, result, "Byte should be returned unchanged");
    }

    @Test
    void convertIntegerToByte() {
        // GIVEN
        Integer source = 123;
        // WHEN
        Byte result = Converter.convertToByte(source);
        // THEN
        assertEquals(Byte.valueOf(source.byteValue()), result);
    }

    @Test
    void convertLongToByte() {
        // GIVEN
        Long source = 7L;
        // WHEN
        Byte result = Converter.convertToByte(source);
        // THEN
        assertEquals(Byte.valueOf(source.byteValue()), result);
    }

    @Test
    void convertStringNumericToByte() {
        // GIVEN
        String source = "64";
        // WHEN
        Byte result = Converter.convertToByte(source);
        // THEN
        assertEquals(Byte.valueOf((byte) 64), result);
    }

    @Test
    void convertStringNonNumericThrowsException() {
        // GIVEN
        String source = "not-a-number";
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> Converter.convertToByte(source), "Non‑numeric String should cause NumberFormatException");
    }

    @Test
    void convertBooleanTrueToByte() {
        // GIVEN
        Boolean source = true;
        // WHEN
        Byte result = Converter.convertToByte(source);
        // THEN
        assertEquals(Byte.valueOf((byte) 1), result);
    }

    @Test
    void convertBooleanFalseToByte() {
        // GIVEN
        Boolean source = false;
        // WHEN
        Byte result = Converter.convertToByte(source);
        // THEN
        assertEquals(Byte.valueOf((byte) 0), result);
    }

    @Test
    void convertCharacterToByte() {
        // GIVEN
        // ASCII 65
        Character source = 'A';
        // WHEN
        Byte result = Converter.convertToByte(source);
        // THEN
        assertEquals(Byte.valueOf((byte) 65), result);
    }

    @Test
    void convertBigDecimalToByte() {
        // GIVEN
        BigDecimal source = new BigDecimal("12");
        // WHEN
        Byte result = Converter.convertToByte(source);
        // THEN
        assertEquals(Byte.valueOf((byte) 12), result);
    }

    @Test
    void convertBigIntegerToByte() {
        // GIVEN
        BigInteger source = new BigInteger("9");
        // WHEN
        Byte result = Converter.convertToByte(source);
        // THEN
        assertEquals(Byte.valueOf((byte) 9), result);
    }

    @Test
    void convertUnsupportedTypeReturnsNull() {
        // GIVEN
        // No conversion path to Byte
        Object source = new Object();
        // WHEN
        Byte result = Converter.convertToByte(source);
        // THEN
        assertNull(result, "Unsupported source types should result in null");
    }

    @ParameterizedTest(name = "[{index}] valid conversion: {0} -> {1}")
    @MethodSource("validConversions")
    void validConversions(Object source, Byte expected) {
        Byte result = Converter.convertToByte(source);
        assertEquals(expected, result, () -> "Expected " + expected + " for source " + source);
    }

    @ParameterizedTest(name = "[{index}] invalid conversion (overflow/unsupported): {0}")
    @MethodSource("invalidConversions")
    void invalidConversions(Object source, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToByte(source), () -> "Expected " + expectedException.getSimpleName() + " for source " + source);
    }

    @ParameterizedTest(name = "[{index}] boundary case: {0}")
    @MethodSource("boundaryCases")
    void boundaryCases(Object source, Byte expected) {
        Byte result = Converter.convertToByte(source);
        assertEquals(expected, result, () -> "Boundary conversion failed for " + source);
    }

    @ParameterizedTest(name = "[{index}] additional numeric: {0}")
    @MethodSource("additionalNumeric")
    void additionalNumeric(Object source, Byte expectedOrException, Class<? extends Throwable> exception) {
        if (exception == null) {
            Byte result = Converter.convertToByte(source);
            assertEquals(expectedOrException, result, () -> "Expected " + expectedOrException + " for source " + source);
        } else {
            assertThrows(exception, () -> Converter.convertToByte(source), () -> "Expected exception " + exception.getSimpleName() + " for source " + source);
        }
    }

    @ParameterizedTest(name = "[{index}] string edge case: \"{0}\"")
    @MethodSource("stringEdgeCases")
    void stringEdgeCases(String source, Byte expectedOrException, Class<? extends Throwable> exception) {
        if (exception == null) {
            Byte result = Converter.convertToByte(source);
            assertEquals(expectedOrException, result, () -> "Expected " + expectedOrException + " for source \"" + source + "\"");
        } else {
            assertThrows(exception, () -> Converter.convertToByte(source), () -> "Expected exception " + exception.getSimpleName() + " for source \"" + source + "\"");
        }
    }

    @ParameterizedTest(name = "[{index}] boolean/char edge: {0}")
    @MethodSource("booleanCharEdgeCases")
    void booleanCharEdgeCases(Object source, Byte expected) {
        Byte result = Converter.convertToByte(source);
        assertEquals(expected, result, () -> "Expected " + expected + " for source " + source);
    }

    @ParameterizedTest(name = "[{index}] overflow other numeric: {0}")
    @MethodSource("overflowOtherNumeric")
    void overflowOtherNumeric(Object source, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToByte(source), () -> "Expected " + expectedException.getSimpleName() + " for source " + source);
    }

    @ParameterizedTest(name = "[{index}] numeric overflow (BigDecimal): {0}")
    @MethodSource("numericOverflow")
    void numericOverflow(BigDecimal source, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToByte(source), () -> "Expected " + expectedException.getSimpleName() + " for source " + source);
    }
}
