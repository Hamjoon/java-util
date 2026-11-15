package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
// Allows non‑static @MethodSource that can use @Mock fields
@TestInstance(Lifecycle.PER_CLASS)
class NumberConversions_toByteZero_Test {

    @Mock
    Converter // Converter is not used by the method but required for the signature
    mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Verify that the method under test never interacts with the regular mock
        verifyNoInteractions(mockConverter);
    }

    /**
     * Supplies the arguments for {@link #toByteZero_VariousInputs_ReturnsSharedZeroByte}.
     *
     * <p>Each {@link Arguments} entry contains:
     * <ul>
     *   <li>the {@code from} value (may be {@code null})</li>
     *   <li>the {@code Converter} instance (may be {@code null}, the injected {@code mockConverter},
     *       or a specially crafted throwing mock)</li>
     *   <li>a human‑readable description for the test case</li>
     * </ul>
     *
     * @return a stream of arguments covering all JSON test cases
     */
    Stream<Arguments> toByteZeroArguments() {
        // A mock that would throw on any method call – to prove the method never invokes it
        Converter throwingMockConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be called");
        });
        return Stream.of(// 1. both arguments null
        Arguments.of(null, null, "Both arguments null – should still return BYTE_ZERO"), // 2. Integer source, null converter
        Arguments.of(Integer.valueOf(123), null, "Integer source with null converter – ignore source"), // 3. String source, regular mock converter
        Arguments.of("some string", mockConverter, "String source with regular mock converter – ignore both"), // 4. Boolean source, regular mock converter
        Arguments.of(Boolean.TRUE, mockConverter, "Boolean source with regular mock converter – ignore both"), // 5. Custom object, regular mock converter
        Arguments.of(new Object(), mockConverter, "Custom object source with regular mock converter – ignore both"), // 6. Byte wrapper (value 5), null converter
        Arguments.of(Byte.valueOf((byte) 5), null, "Byte wrapper source with null converter – ignore source value"), // 7. Byte wrapper (minimum value), regular mock converter
        Arguments.of(Byte.valueOf(Byte.MIN_VALUE), mockConverter, "Byte wrapper with min value and regular mock converter – ignore source"), // 8. int array source, null converter
        Arguments.of(new int[] { 1, 2, 3 }, null, "int[] source with null converter – ignore array"), // 9. List of strings source, regular mock converter
        Arguments.of(java.util.List.of("a", "b", "c"), mockConverter, "List<String> source with regular mock converter – ignore collection"), // 10. null source, regular mock converter
        Arguments.of(null, mockConverter, "Null source with regular mock converter – still return BYTE_ZERO"), // 11. String source, throwing mock converter
        Arguments.of("irrelevant", throwingMockConverter, "String source with throwing mock converter – ensure no exception"));
    }

    @Test
    void toByteZero_WithNullInput_ReturnsZeroByte() {
        Byte result = NumberConversions.toByteZero(null, mockConverter);
        assertNotNull(result, "Result should never be null");
        assertEquals(Byte.valueOf((byte) 0), result, "Expected Byte zero");
    }

    @Test
    void toByteZero_WithIntegerInput_ReturnsZeroByte() {
        Integer input = 123;
        Byte result = NumberConversions.toByteZero(input, mockConverter);
        assertNotNull(result);
        assertEquals(Byte.valueOf((byte) 0), result);
    }

    @Test
    void toByteZero_WithStringInput_ReturnsZeroByte() {
        String input = "any string";
        Byte result = NumberConversions.toByteZero(input, mockConverter);
        assertNotNull(result);
        assertEquals(Byte.valueOf((byte) 0), result);
    }

    @Test
    void toByteZero_WithCustomObject_ReturnsZeroByte() {
        Object input = new Object();
        Byte result = NumberConversions.toByteZero(input, mockConverter);
        assertNotNull(result);
        assertEquals(Byte.valueOf((byte) 0), result);
    }

    /**
     * Parameterized test that covers all scenarios described in the JSON test case group.
     *
     * @param from       the source object passed to {@code toByteZero}
     * @param converter  the {@link Converter} instance passed to {@code toByteZero}
     * @param description textual description of the test case (used as display name)
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("toByteZeroArguments")
    void toByteZero_VariousInputs_ReturnsSharedZeroByte(Object from, Converter converter, String description) {
        // Act
        Byte result = NumberConversions.toByteZero(from, converter);
        // Assert – the method must never return null
        assertNotNull(result, "Result must never be null");
        // Assert – the returned instance must be the shared constant
        assertSame(CommonValues.BYTE_ZERO, result, "Method should return the exact shared BYTE_ZERO constant");
        // Additional sanity check – the value must be zero
        assertEquals((byte) 0, result.byteValue(), "The constant BYTE_ZERO must represent the numeric value 0");
    }
}
