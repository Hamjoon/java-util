package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toUUID_Test {

    /**
     * The Converter is not used by the current implementation of {@code toUUID},
     * but we provide a mock to keep the signature realistic and to guard against future
     * changes that might start using it.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Test a normal positive {@link BigInteger} that already has 32 hex digits.
     * The expected UUID string is built manually to match the algorithm in
     * {@code BigIntegerConversions.toUUID}.
     */
    @Test
    void toUUID_WithFullLengthBigInteger_ReturnsCorrectUUID() {
        // 0x0123456789abcdef0123456789abcdef (32 hex chars)
        BigInteger bigInt = new BigInteger("0123456789abcdef0123456789abcdef", 16);
        UUID result = BigIntegerConversions.toUUID(bigInt, mockConverter);
        String expectedUuidString = "01234567-89ab-cdef-0123-456789abcdef";
        assertEquals(UUID.fromString(expectedUuidString), result, "The UUID produced from the full‑length BigInteger is not as expected");
    }

    /**
     * Test that a small positive {@link BigInteger} is correctly padded with leading zeros
     * before being formatted as a UUID.
     */
    @Test
    void toUUID_WithSmallBigInteger_IsPaddedAndConverted() {
        // decimal 1 -> hex "1"
        BigInteger bigInt = BigInteger.ONE;
        UUID result = BigIntegerConversions.toUUID(bigInt, mockConverter);
        // Expected after padding to 32 hex chars:
        // highBitsHex = "0000000000000000"
        // lowBitsHex  = "0000000000000001"
        String expectedUuidString = "00000000-0000-0000-0000-000000000001";
        assertEquals(UUID.fromString(expectedUuidString), result, "The UUID for a small BigInteger should be padded with zeros");
    }

    /**
     * Verify that a negative {@link BigInteger} causes an {@link IllegalArgumentException}
     * as defined by the implementation.
     */
    @Test
    void toUUID_WithNegativeBigInteger_ThrowsIllegalArgumentException() {
        BigInteger negative = new BigInteger("-12345");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toUUID(negative, mockConverter));
        assertTrue(ex.getMessage().contains("Cannot convert a negative number"), "Exception message should indicate that negative numbers are not allowed");
    }

    /**
     * Convert {@link BigInteger#ZERO} – the result must be the all‑zero UUID.
     */
    @Test
    void toUUID_WithZeroBigInteger_ReturnsAllZeroUUID() {
        UUID result = BigIntegerConversions.toUUID(BigInteger.ZERO, mockConverter);
        String expected = "00000000-0000-0000-0000-000000000000";
        assertEquals(UUID.fromString(expected), result, "Zero should be converted to the all‑zero UUID");
    }

    /**
     * Convert {@link BigInteger#valueOf(Long.MAX_VALUE)} – only the low 64‑bits are set.
     */
    @Test
    void toUUID_WithLongMaxValue_ProducesCorrectHighLowSplit() {
        // 0x7FFFFFFFFFFFFFFF
        BigInteger bigInt = BigInteger.valueOf(Long.MAX_VALUE);
        UUID result = BigIntegerConversions.toUUID(bigInt, mockConverter);
        String expected = "00000000-0000-0000-7fff-ffffffffffff";
        assertEquals(UUID.fromString(expected), result, "Long.MAX_VALUE should occupy only the low part of the UUID");
    }

    /**
     * Convert the maximum unsigned 128‑bit value (2^128‑1).
     */
    @Test
    void toUUID_WithMaximum128BitValue_ReturnsAllOnesUUID() {
        BigInteger max128 = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);
        UUID result = BigIntegerConversions.toUUID(max128, mockConverter);
        String expected = "ffffffff-ffff-ffff-ffff-ffffffffffff";
        assertEquals(UUID.fromString(expected), result, "The UUID for 2^128‑1 should be the canonical all‑f UUID");
    }

    /**
     * Verify truncation behaviour when the input exceeds 128 bits (e.g., 2^130).
     */
    @Test
    void toUUID_WithOverflowBeyond128Bits_TruncatesToMostSignificant128Bits() {
        // 2^130 = 0x4 followed by 32 zeros (33 hex digits)
        BigInteger overflow = BigInteger.ONE.shiftLeft(130);
        UUID result = BigIntegerConversions.toUUID(overflow, mockConverter);
        // Expected UUID is built from the first 32 hex characters: 0x40000000000000000000000000000000
        String expected = "40000000-0000-0000-0000-000000000000";
        assertEquals(UUID.fromString(expected), result, "Values larger than 128 bits should be truncated to the most‑significant 128 bits");
    }

    /**
     * Passing a non‑BigInteger (String) should raise a {@link ClassCastException}.
     */
    @Test
    void toUUID_WithNonBigIntegerInput_ThrowsClassCastException() {
        assertThrows(ClassCastException.class, () -> BigIntegerConversions.toUUID("12345", mockConverter), "A non‑BigInteger input must cause a ClassCastException");
    }

    /**
     * Passing {@code null} as the source object should raise a {@link NullPointerException}.
     */
    @Test
    void toUUID_WithNullInput_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toUUID(null, mockConverter), "Null input should result in a NullPointerException");
    }
}
