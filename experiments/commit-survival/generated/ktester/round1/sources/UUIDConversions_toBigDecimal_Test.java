package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UUIDConversions_toBigDecimal_Test {

    /**
     * Converter used for the tests – created once because it is stateless.
     */
    private static Converter converter;

    /**
     * Mocked options to satisfy the {@link Converter} constructor.
     */
    @Mock
    private static ConverterOptions mockOptions;

    /**
     * Real {@link Converter} used by the original test cases (stateless).
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter constructor expects a ConverterOptions instance.
        // We mock it and make getConverterOverrides() return an empty map.
        // This prevents any external configuration from interfering with the test.
        org.mockito.Mockito.when(mockOptions.getConverterOverrides()).thenReturn(Collections.emptyMap());
        converter = new Converter(mockOptions);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none here).
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// String that can be parsed as a numeric value.
        Arguments.of("12345678901234567890", new BigInteger("12345678901234567890")), // UUID instance – the test pretends the Converter extracts the low‑order bits.
        Arguments.of(new UUID(0L, 1L), new BigInteger("1")), // Direct BigInteger input – the Converter simply echoes it.
        Arguments.of(new BigInteger("98765432109876543210"), new BigInteger("98765432109876543210")));
    }

    private static Stream<Arguments> zeroAndNegativeProvider() {
        return Stream.of(Arguments.of("0", BigInteger.ZERO), Arguments.of("-42", new BigInteger("-42")));
    }

    /**
     * Verify that a {@link UUID} is correctly converted to a {@link BigDecimal}.
     * The expected value is derived from the UUID's 128‑bit hexadecimal representation.
     */
    @Test
    void toBigDecimal_convertsUuidToBigDecimal() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        // Expected BigDecimal = new BigDecimal( new BigInteger( uuid as 32‑hex‑digit string, 16 ) )
        // 32‑hex‑digit string
        String hex = uuid.toString().replace("-", "");
        BigInteger expectedBigInt = new BigInteger(hex, 16);
        BigDecimal expected = new BigDecimal(expectedBigInt);
        // Act
        BigDecimal actual = UUIDConversions.toBigDecimal(uuid, realConverter);
        // Assert
        assertEquals(expected, actual, () -> "BigDecimal conversion of UUID " + uuid + " did not match expected value.");
    }

    /**
     * Verify that passing a {@code null} source object results in a {@link NullPointerException}.
     */
    @Test
    void toBigDecimal_nullFrom_throwsNpe() {
        assertThrows(NullPointerException.class, () -> UUIDConversions.toBigDecimal(null, realConverter), "Calling toBigDecimal with a null source should throw NullPointerException");
    }

    /**
     * Verify that passing a {@code null} {@link Converter} results in a {@link NullPointerException}.
     */
    @Test
    void toBigDecimal_nullConverter_throwsNpe() {
        // Passing a null Converter should cause a NPE when the method attempts to use it.
        UUID uuid = UUID.randomUUID();
        assertThrows(NullPointerException.class, () -> UUIDConversions.toBigDecimal(uuid, null));
    }

    /**
     * Valid conversion scenarios where the {@link Converter} returns a {@link BigInteger}
     * that should be wrapped into a {@link BigDecimal}.
     */
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("validConversionProvider")
    void toBigDecimal_validConversions(Object from, BigInteger converterResult) {
        // Arrange – mock a Converter that returns the supplied BigInteger.
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(from), eq(BigInteger.class))).thenReturn(converterResult);
        // Expected value.
        BigDecimal expected = new BigDecimal(converterResult);
        // Act.
        BigDecimal actual = UUIDConversions.toBigDecimal(from, mockConverter);
        // Assert.
        assertEquals(expected, actual, () -> "Conversion of " + from + " did not produce the expected BigDecimal.");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("zeroAndNegativeProvider")
    void toBigDecimal_zeroAndNegative(Object from, BigInteger converterResult) {
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(from), eq(BigInteger.class))).thenReturn(converterResult);
        BigDecimal expected = new BigDecimal(converterResult);
        BigDecimal actual = UUIDConversions.toBigDecimal(from, mockConverter);
        assertEquals(expected, actual, () -> "Zero/negative conversion failed for source: " + from);
    }

    @Test
    void toBigDecimal_veryLargeNumber() {
        // Arrange
        String from = "veryLarge";
        BigInteger huge = new BigInteger("1234567890123456789012345678901234567890");
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(from), eq(BigInteger.class))).thenReturn(huge);
        // Act
        BigDecimal result = UUIDConversions.toBigDecimal(from, mockConverter);
        // Assert
        assertEquals(new BigDecimal(huge), result, "Conversion of a very large number should preserve full precision.");
    }

    @Test
    void toBigDecimal_nullFrom_propagatesConverterException() {
        // The mock Converter throws NPE when asked to convert a null source.
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(isNull(), eq(BigInteger.class))).thenThrow(new NullPointerException("source is null"));
        NullPointerException ex = assertThrows(NullPointerException.class, () -> UUIDConversions.toBigDecimal(null, mockConverter));
        assertEquals("source is null", ex.getMessage());
    }

    @Test
    void toBigDecimal_invalidType_propagatesConverterException() {
        // Converter throws IllegalArgumentException for unsupported types.
        Object unsupported = new Object();
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(unsupported), eq(BigInteger.class))).thenThrow(new IllegalArgumentException("Unsupported type"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UUIDConversions.toBigDecimal(unsupported, mockConverter));
        assertEquals("Unsupported type", ex.getMessage());
    }

    @Test
    void toBigDecimal_converterReturnsNull_throwsNpeFromBigDecimalConstructor() {
        // Converter returns null → new BigDecimal(null) throws NPE.
        String from = "0";
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(from), eq(BigInteger.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> UUIDConversions.toBigDecimal(from, mockConverter));
    }

    @Test
    void toBigDecimal_converterThrowsUncheckedException_propagatesIt() {
        // Converter throws IllegalStateException – should be propagated unchanged.
        String from = "any";
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(from), eq(BigInteger.class))).thenThrow(new IllegalStateException("conversion failed"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> UUIDConversions.toBigDecimal(from, mockConverter));
        assertEquals("conversion failed", ex.getMessage());
    }

    @Test
    void toBigDecimal_converterReturnsNonBigInteger_throwsClassCastException() {
        // Converter returns a String – the cast to BigInteger inside toBigInteger will fail.
        String from = "123";
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(from), eq(BigInteger.class))).thenReturn("not a BigInteger");
        assertThrows(ClassCastException.class, () -> UUIDConversions.toBigDecimal(from, mockConverter));
    }

    @Test
    void toBigDecimal_converterThrowsError_propagatesError() {
        // Converter throws an AssertionError – should be propagated unchanged.
        String from = "any";
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(from), eq(BigInteger.class))).thenThrow(new AssertionError("assertion failed"));
        AssertionError err = assertThrows(AssertionError.class, () -> UUIDConversions.toBigDecimal(from, mockConverter));
        assertEquals("assertion failed", err.getMessage());
    }
}
