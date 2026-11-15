package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toUUID_Test {

    @Mock
    private Converter // mock used only as a pass‑through argument
    mockConverter;

    // static mock for the delegate class
    private MockedStatic<BigIntegerConversions> bigIntConvMocked;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh static mock for every test to avoid cross‑test interference
        bigIntConvMocked = mockStatic(BigIntegerConversions.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock – mandatory to avoid memory‑leaks with Mockito
        if (bigIntConvMocked != null) {
            bigIntConvMocked.close();
        }
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// group: Valid conversions
        Arguments.of(new BigDecimal("1"), null, UUID.fromString("00000000-0000-0000-0000-000000000001"), "Converts a simple positive integer BigDecimal to a UUID (value 1)"), Arguments.of(new BigDecimal("340282366920938463463374607431768211455"), null, UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), "Converts the maximum unsigned 128‑bit value (2^128‑1) to an all‑f’s UUID"), Arguments.of(new BigDecimal("123.456"), null, UUID.fromString("00000000-0000-0000-0000-00000000007b"), "Fractional part is discarded; 123 (0x7b) becomes the least‑significant bits"), // group: Edge cases
        Arguments.of(new BigDecimal("0"), null, UUID.fromString("00000000-0000-0000-0000-000000000000"), "Zero BigDecimal yields a UUID of all zeros"), Arguments.of(new BigDecimal("-1"), null, UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), "Negative one maps to a UUID with all bits set (two's‑complement)"), // group: Overflow handling (wrap‑around of the 128‑bit space)
        Arguments.of(new BigDecimal("340282366920938463463374607431768211456"), null, UUID.fromString("00000000-0000-0000-0000-000000000000"), "Value equals 2^128 – wraps around to zero"), Arguments.of(new BigDecimal("340282366920938463463374607431768211461"), null, UUID.fromString("00000000-0000-0000-0000-000000000005"), "2^128 + 5 results in a UUID whose least‑significant bits are 0x5"));
    }

    private static Stream<Arguments> exceptionProvider() {
        // Converter that throws when used – required for the RuntimeException case
        Converter explodingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                throw new RuntimeException("converter used");
            }
        };
        return Stream.of(// group: Error handling – null source
        Arguments.of(null, null, NullPointerException.class, "Passing a null source should raise NullPointerException (cast to BigDecimal)"), // group: Error handling – incompatible type
        Arguments.of("not a BigDecimal", null, ClassCastException.class, "Providing a non‑BigDecimal object should raise ClassCastException"), // group: Error handling – downstream RuntimeException from converter
        Arguments.of(new BigDecimal("123"), explodingConverter, RuntimeException.class, "If the downstream conversion uses the supplied converter and it throws, the exception must propagate"));
    }

    @Test
    void toUUID_convertsBigDecimalToUuid_usingBigIntegerConversions() {
        // GIVEN
        // contains a fractional part
        BigDecimal source = new BigDecimal("12345.678");
        // truncates the fraction
        BigInteger expectedBigInt = source.toBigInteger();
        UUID expectedUuid = UUID.randomUUID();
        // Stub the static delegate method
        bigIntConvMocked.when(() -> BigIntegerConversions.toUUID(expectedBigInt, mockConverter)).thenReturn(expectedUuid);
        // WHEN
        UUID actual = BigDecimalConversions.toUUID(source, mockConverter);
        // THEN
        assertEquals(expectedUuid, actual, "The UUID returned by BigDecimalConversions should be the one supplied by the stubbed BigIntegerConversions");
        bigIntConvMocked.verify(() -> BigIntegerConversions.toUUID(expectedBigInt, mockConverter), times(1));
    }

    @Test
    void toUUID_withWholeNumberBigDecimal() {
        // GIVEN
        BigDecimal source = new BigDecimal("9876543210");
        // same value, no fraction
        BigInteger expectedBigInt = source.toBigInteger();
        UUID expectedUuid = UUID.randomUUID();
        bigIntConvMocked.when(() -> BigIntegerConversions.toUUID(expectedBigInt, mockConverter)).thenReturn(expectedUuid);
        // WHEN
        UUID actual = BigDecimalConversions.toUUID(source, mockConverter);
        // THEN
        assertEquals(expectedUuid, actual);
        bigIntConvMocked.verify(() -> BigIntegerConversions.toUUID(expectedBigInt, mockConverter), times(1));
    }

    @Test
    void toUUID_nullSource_throwsNullPointerException() {
        // GIVEN
        BigDecimal source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toUUID(source, mockConverter), "Passing a null source should result in a NullPointerException because the method casts to BigDecimal");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionProvider")
    void toUUID_validConversions(BigDecimal source, Converter converter, UUID expectedUuid, String description) {
        // Arrange ------------------------------------------------------------
        // fraction is discarded automatically
        BigInteger expectedBigInt = source.toBigInteger();
        // stub the static delegate
        bigIntConvMocked.when(() -> BigIntegerConversions.toUUID(expectedBigInt, converter)).thenReturn(expectedUuid);
        // Act ----------------------------------------------------------------
        UUID actual = BigDecimalConversions.toUUID(source, converter);
        // Assert -------------------------------------------------------------
        assertEquals(expectedUuid, actual, "The UUID returned should match the stubbed value for the calculated BigInteger");
        // verify that the delegate was called exactly once with the expected arguments
        bigIntConvMocked.verify(() -> BigIntegerConversions.toUUID(expectedBigInt, converter), times(1));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionProvider")
    void toUUID_exceptions(Object source, Converter converter, Class<? extends Throwable> expectedException, String description) {
        // Arrange ------------------------------------------------------------
        // For the ClassCastException case we do not need any stubbing because
        // the cast to BigDecimal fails before the static delegate is reached.
        // For the RuntimeException case we stub the delegate to throw.
        if (expectedException == RuntimeException.class) {
            // source is a valid BigDecimal, but the provided converter will cause
            // the downstream conversion to throw; we simulate that by stubbing.
            BigDecimal bd = (BigDecimal) source;
            BigInteger bi = bd.toBigInteger();
            bigIntConvMocked.when(() -> BigIntegerConversions.toUUID(bi, converter)).thenThrow(new RuntimeException("converter used"));
        }
        // Act & Assert -------------------------------------------------------
        assertThrows(expectedException, () -> BigDecimalConversions.toUUID(source, converter), description);
    }

    @Test
    void toUUID_usesProvidedMockConverter() {
        // GIVEN
        BigDecimal source = new BigDecimal("42");
        BigInteger expectedBigInt = source.toBigInteger();
        UUID expectedUuid = UUID.randomUUID();
        // Stub the static delegate with the mockConverter instance
        bigIntConvMocked.when(() -> BigIntegerConversions.toUUID(expectedBigInt, mockConverter)).thenReturn(expectedUuid);
        // WHEN
        UUID actual = BigDecimalConversions.toUUID(source, mockConverter);
        // THEN
        assertEquals(expectedUuid, actual);
        bigIntConvMocked.verify(() -> BigIntegerConversions.toUUID(expectedBigInt, mockConverter), times(1));
    }
}
