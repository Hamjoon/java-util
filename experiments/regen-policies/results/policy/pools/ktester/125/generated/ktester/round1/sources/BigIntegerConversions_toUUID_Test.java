package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toUUID_Test {

    /**
     * The Converter is not used by the current implementation of {@code toUUID},
     *  but the method signature requires it, so we provide a mock.
     */
    @Mock
    private static Converter mockConverter;

    private static Stream<Arguments> smallPositiveProvider() {
        return Stream.of(// value 1 → ...0001
        Arguments.of("1", "00000000-0000-0000-0000-000000000001"), // 4660 = 0x1234 → ...1234
        Arguments.of("4660", "00000000-0000-0000-0000-000000001234"));
    }

    private static Stream<Arguments> exact32HexProvider() {
        return Stream.of(// 2^128‑1 = 340282366920938463463374607431768211455 → all f's
        Arguments.of("340282366920938463463374607431768211455", "ffffffff-ffff-ffff-ffff-ffffffffffff"), // 0x1234567890ABCDEF1234567890ABCDEF
        Arguments.of("24197857200151252728969465429440056815", "12345678-90ab-cdef-1234-567890abcdef"));
    }

    @Test
    void toUUID_ZeroValue_ReturnsZeroUuid() {
        BigInteger zero = BigInteger.ZERO;
        UUID result = BigIntegerConversions.toUUID(zero, mockConverter);
        assertEquals(new UUID(0L, 0L), result, "UUID for zero should be all‑zero");
    }

    @Test
    void toUUID_KnownPositiveValue_ReturnsExpectedUuid() {
        // 32‑hex‑digit value: 0123456789abcdef0123456789abcdef
        String hex = "0123456789abcdef0123456789abcdef";
        BigInteger bigInt = new BigInteger(hex, 16);
        UUID expected = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
        UUID actual = BigIntegerConversions.toUUID(bigInt, mockConverter);
        assertEquals(expected, actual, "UUID conversion did not match expected format");
    }

    @Test
    void toUUID_NegativeValue_ThrowsIllegalArgumentException() {
        BigInteger negative = BigInteger.valueOf(-1);
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toUUID(negative, mockConverter), "Converting a negative BigInteger should throw IllegalArgumentException");
    }

    /**
     * Small positive numbers that need left‑padding to reach 32 hex digits.
     */
    @ParameterizedTest(name = "[{index}] from={0} → {1}")
    @MethodSource("smallPositiveProvider")
    void toUUID_SmallPositiveValues_PaddedCorrectly(String decimalValue, String expectedUuid) {
        BigInteger bi = new BigInteger(decimalValue);
        UUID expected = UUID.fromString(expectedUuid);
        UUID actual = BigIntegerConversions.toUUID(bi, mockConverter);
        assertEquals(expected, actual);
    }

    /**
     * Exact 32‑hex‑digit numbers (no padding required).
     */
    @ParameterizedTest(name = "[{index}] from={0} → {1}")
    @MethodSource("exact32HexProvider")
    void toUUID_Exact32HexValues_NoPadding(String decimalValue, String expectedUuid) {
        BigInteger bi = new BigInteger(decimalValue);
        UUID expected = UUID.fromString(expectedUuid);
        UUID actual = BigIntegerConversions.toUUID(bi, mockConverter);
        assertEquals(expected, actual);
    }

    /**
     * Non‑BigInteger input should cause a ClassCastException.
     */
    @Test
    void toUUID_NonBigIntegerInput_ThrowsClassCastException() {
        // a String, not a BigInteger
        Object notABigInt = "123";
        assertThrows(ClassCastException.class, () -> BigIntegerConversions.toUUID(notABigInt, mockConverter), "Passing a non‑BigInteger should throw ClassCastException");
    }

    /**
     * Null `from` argument results in NullPointerException when signum() is called.
     */
    @Test
    void toUUID_NullInput_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toUUID(null, mockConverter), "Null `from` should cause NullPointerException");
    }

    /**
     * High‑bits non‑zero, low‑bits zero.
     */
    @Test
    void toUUID_HighBitsNonZeroLowBitsZero() {
        // Hex string with high 64 bits set, low 64 bits zero
        String hex = "1234567890ABCDEF0000000000000000";
        BigInteger bi = new BigInteger(hex, 16);
        UUID expected = UUID.fromString("12345678-90ab-cdef-0000-000000000000");
        UUID actual = BigIntegerConversions.toUUID(bi, mockConverter);
        assertEquals(expected, actual);
    }

    /**
     * Mid‑range value containing internal groups of leading zeros.
     */
    @Test
    void toUUID_MidRangeWithInternalZeros() {
        String hex = "000012340000567800009ABC0000DEF0";
        BigInteger bi = new BigInteger(hex, 16);
        UUID expected = UUID.fromString("00001234-0000-5678-0000-9abc0000def0");
        UUID actual = BigIntegerConversions.toUUID(bi, mockConverter);
        assertEquals(expected, actual);
    }

    /**
     * Value exceeding 128 bits – extra most‑significant digits are truncated.
     */
    @Test
    void toUUID_ValueExceeding128Bits_TruncatedToMaxUuid() {
        // Decimal representation of (2^128‑1) * 10
        String decimal = "3402823669209384634633746074317682114550";
        BigInteger bi = new BigInteger(decimal);
        UUID expected = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        UUID actual = BigIntegerConversions.toUUID(bi, mockConverter);
        assertEquals(expected, actual);
    }

    /**
     * Maximum signed 127‑bit value (2^127‑1).
     */
    @Test
    void toUUID_MaxSigned127BitValue() {
        // 2^127‑1
        String decimal = "170141183460469231731687303715884105727";
        BigInteger bi = new BigInteger(decimal);
        UUID expected = UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff");
        UUID actual = BigIntegerConversions.toUUID(bi, mockConverter);
        assertEquals(expected, actual);
    }

    /**
     * Low‑bits all ones (2^64‑1).
     */
    @Test
    void toUUID_LowBitsAllOnes() {
        // 2^64‑1
        String decimal = "18446744073709551615";
        BigInteger bi = new BigInteger(decimal);
        UUID expected = UUID.fromString("00000000-0000-0000-ffff-ffffffffffff");
        UUID actual = BigIntegerConversions.toUUID(bi, mockConverter);
        assertEquals(expected, actual);
    }
}
