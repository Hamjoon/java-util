package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toUUID_Test {

    /**
     * The {@link Converter} instance used for the delegation.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the real Converter works fine for the simple delegation we need.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /**
     * Supplies the test data for {@link #toUUID_ValidConversions(BigDecimal, String, String)}.
     */
    private static Stream<Arguments> provideValidConversions() {
        return Stream.of(// Zero value – both most‑ and least‑significant bits are 0.
        Arguments.of(BigDecimal.ZERO, "00000000-0000-0000-0000-000000000000", "Convert a zero BigDecimal to UUID; both most and least significant bits should be 0."), // Positive integer 123 – most bits 0, least bits 0x7b.
        Arguments.of(BigDecimal.valueOf(123), "00000000-0000-0000-0000-00000000007b", "Convert a positive integer BigDecimal (123) to UUID; most bits are 0, least bits are 0x7b."), // Fractional part is truncated by toBigInteger().
        Arguments.of(new BigDecimal("123.456"), "00000000-0000-0000-0000-00000000007b", "Convert a BigDecimal with fractional part; the fractional part is truncated by toBigInteger(), resulting in the same UUID as 123."), // Negative integer – sign‑extension produces all‑1 most bits.
        Arguments.of(BigDecimal.valueOf(-123), "ffffffff-ffff-ffff-ffff-ffffffffff85", "Convert a negative integer BigDecimal; sign‑extension produces most bits of -1 and least bits of -123 (0xFFFFFFFFFFFFFF85)."), // Large value: 2^80 + 5 → high 64 bits = 0x00010000, low 64 bits = 0x5.
        Arguments.of(new BigDecimal(new BigInteger("100000000000000000005")), "00000000-0001-0000-0000-000000000005", "Convert a large BigDecimal whose underlying BigInteger is 2^80 + 5; the high 64 bits become 0x00010000 and low 64 bits become 0x5."));
    }

    /**
     * Verify that a valid {@link BigDecimal} is correctly turned into a {@link UUID}
     * by delegating to {@code BigIntegerConversions.toUUID}.
     */
    @Test
    void toUUID_ValidBigDecimal_ReturnsExpectedUuid() {
        // Arrange – create a 128‑bit value and the expected UUID.
        // 2^128‑1
        BigInteger bigInt = new BigInteger("340282366920938463463374607431768211455");
        // high 64 bits
        long mostSigBits = bigInt.shiftRight(64).longValue();
        // low 64 bits
        long leastSigBits = bigInt.longValue();
        UUID expected = new UUID(mostSigBits, leastSigBits);
        BigDecimal from = new BigDecimal(bigInt);
        // Act
        UUID actual = BigDecimalConversions.toUUID(from, converter);
        // Assert
        assertEquals(expected, actual, "The UUID produced from the BigDecimal should match the expected value.");
    }

    /**
     * Verify that passing {@code null} as the source object results in a {@link NullPointerException}
     * because the implementation casts the argument to {@link BigDecimal} without a null check.
     */
    @Test
    void toUUID_NullSource_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toUUID(null, converter), "Calling toUUID with a null source should throw NullPointerException.");
    }

    /**
     * Verify that passing an object that is not a {@link BigDecimal} results in a {@link ClassCastException}.
     */
    @Test
    void toUUID_NonBigDecimalSource_ThrowsClassCastException() {
        Object notABigDecimal = "not a bigdecimal";
        assertThrows(ClassCastException.class, () -> BigDecimalConversions.toUUID(notABigDecimal, converter), "Calling toUUID with a non‑BigDecimal source should throw ClassCastException.");
    }

    /**
     * Verify that a variety of {@link BigDecimal} values are correctly turned into {@link UUID}s.
     * The expected UUID strings are taken from the specification in the JSON test cases.
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("provideValidConversions")
    void toUUID_ValidConversions(BigDecimal from, String expectedUuidString, String description) {
        // Arrange
        UUID expected = UUID.fromString(expectedUuidString);
        // Act – the converter argument is not needed for the simple delegation,
        // so we pass {@code null} as the JSON test data suggests.
        UUID actual = BigDecimalConversions.toUUID(from, null);
        // Assert
        assertEquals(expected, actual, description);
    }

    /**
     * Additional explicit test for the JSON case where the 'from' argument is {@code null}
     * and the expected outcome is a {@link NullPointerException}.
     */
    @Test
    void toUUID_NullFromArgument_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toUUID(null, null), "Passing a null 'from' argument should cause a NullPointerException.");
    }
}
