package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toUUID_Test {

    @Mock
    private ConverterOptions // mocked options for the Converter
    mockOptions;

    // instance under test
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter constructor expects a ConverterOptions instance.
        // We mock the options to return an empty overrides map.
        when(mockOptions.getConverterOverrides()).thenReturn(Collections.emptyMap());
        converter = new Converter(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the reference – not strictly required but keeps the test tidy.
        converter = null;
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised test data
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> provideToUUIDCases() {
        return Stream.of(// 1. Large integer value, null converter
        Arguments.of("Valid BigDecimal containing a large integer value", new BigDecimal("12345678901234567890"), false, UUID.fromString("11111111-2222-3333-4444-555555555555"), null), // 2. Fractional value, null converter
        Arguments.of("BigDecimal with a fractional part (truncates to 12345)", new BigDecimal("12345.6789"), false, UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"), null), // 3. Null 'from' argument – expect NPE
        Arguments.of("Null 'from' argument", null, false, null, NullPointerException.class), // 4. Wrong type – expect ClassCastException
        Arguments.of("'from' is not a BigDecimal", "not a bigdecimal", false, null, ClassCastException.class), // 5. Zero value with a non‑null Converter
        Arguments.of("Zero value BigDecimal with a non‑null Converter", new BigDecimal("0"), true, UUID.fromString("00000000-0000-0000-0000-000000000000"), null), // 6. Negative integer value, null converter
        Arguments.of("Negative BigDecimal value (-1)", new BigDecimal("-1"), false, UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), null), // 7. Value larger than Long.MAX_VALUE
        Arguments.of("BigDecimal exceeding Long.MAX_VALUE", new BigDecimal(Long.MAX_VALUE).add(BigDecimal.ONE), false, UUID.fromString("deadbeef-dead-beef-dead-beefdeadbeef"), null), // 8. Fraction just below 1, null converter
        Arguments.of("BigDecimal less than 1 but greater than 0", new BigDecimal("0.999999999999999999"), false, UUID.fromString("00000000-0000-0000-0000-000000000001"), null), // 9. Very large BigDecimal with a non‑null Converter
        Arguments.of("Very large BigDecimal (30+ digits) with a non‑null Converter", new BigDecimal("123456789012345678901234567890"), true, UUID.fromString("11111111-2222-3333-4444-555555555555"), null), // 10. Negative fractional value whose absolute value < 1
        Arguments.of("Negative fractional BigDecimal whose absolute value is less than 1", new BigDecimal("-0.0001"), false, UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), null));
    }

    /**
     * Verify that {@code BigDecimalConversions.toUUID} converts the supplied {@link BigDecimal}
     * to a {@link UUID} by delegating to {@link BigIntegerConversions#toUUID(BigInteger, Converter)}.
     */
    @Test
    void testToUUIDDelegatesToBigIntegerConversions() {
        // GIVEN: a random UUID and a corresponding BigDecimal representation
        UUID originalUuid = UUID.randomUUID();
        // Convert the UUID into a 128‑bit unsigned integer (big‑endian) and then to BigDecimal.
        // UUID -> hex string (without dashes) -> BigInteger -> BigDecimal
        String hex = originalUuid.toString().replace("-", "");
        BigInteger uuidAsBigInt = new BigInteger(hex, 16);
        BigDecimal uuidAsBigDecimal = new BigDecimal(uuidAsBigInt);
        // WHEN: converting via the method under test
        UUID actual = BigDecimalConversions.toUUID(uuidAsBigDecimal, converter);
        // THEN: the result must be identical to the direct delegate call
        UUID expected = BigIntegerConversions.toUUID(uuidAsBigInt, converter);
        assertEquals(expected, actual, "BigDecimalConversions.toUUID should delegate to BigIntegerConversions.toUUID");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideToUUIDCases")
    void testToUUID(String description, Object from, boolean useNonNullConverter, UUID expectedUuid, Class<? extends Throwable> expectedException) {
        // Choose the appropriate Converter instance (null or the real one created in @BeforeEach)
        Converter conv = useNonNullConverter ? this.converter : null;
        if (expectedException != null) {
            // The method is expected to throw before delegating to BigIntegerConversions
            assertThrows(expectedException, () -> BigDecimalConversions.toUUID(from, conv), description);
            return;
        }
        // Normal path – we need to verify delegation to the static method.
        // Compute the BigInteger that the method under test will pass to the delegate.
        BigInteger expectedBigInt = ((BigDecimal) from).toBigInteger();
        try (MockedStatic<BigIntegerConversions> mocked = mockStatic(BigIntegerConversions.class)) {
            // Stub the static delegate to return the deterministic UUID for this test case.
            mocked.when(() -> BigIntegerConversions.toUUID(expectedBigInt, conv)).thenReturn(expectedUuid);
            // Invoke the method under test.
            UUID actual = BigDecimalConversions.toUUID(from, conv);
            // Verify the result matches the stubbed UUID.
            assertEquals(expectedUuid, actual, description);
            // Verify that the delegate was called exactly once with the expected arguments.
            mocked.verify(() -> BigIntegerConversions.toUUID(expectedBigInt, conv));
        }
    }
}
