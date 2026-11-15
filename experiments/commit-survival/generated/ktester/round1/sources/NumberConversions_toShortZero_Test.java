package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigInteger;
import java.util.Date;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toShortZero_Test {

    /**
     * A mock {@link Converter}. The real implementation is not used by the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Supplies the arguments for {@link #toShortZero_parameterized(String, Object, Converter)}.
     */
    private static Stream<Arguments> toShortZeroTestCases() {
        // A mock converter for the cases that need a non‑null Converter.
        Converter dummyConverter = org.mockito.Mockito.mock(Converter.class);
        return Stream.of(// 1. both arguments null
        arguments("both arguments null", null, null), // 2. from = Integer
        arguments("from is Integer 12345", 12345, null), // 3. from = String
        arguments("from is String \"some string\"", "some string", null), // 4. from = custom object, converter = custom object (mocked)
        arguments("both parameters are custom non‑null objects", new Object(), dummyConverter), // 5. from = Boolean true
        arguments("from is Boolean true", Boolean.TRUE, null), // 6. from = int array
        arguments("from is int array [1,2,3]", new int[] { 1, 2, 3 }, null), // 7. from = plain Object instance
        arguments("from is plain Object instance", new Object(), null), // 8. from = BigInteger
        arguments("from is BigInteger 12345678901234567890", new BigInteger("12345678901234567890"), null), // 9. from = Date (epoch 0)
        arguments("from is Date epoch 0", new Date(0L), null), // 10. from = null, converter non‑null
        arguments("from null, converter non‑null", null, dummyConverter), // 11. from = numeric zero (int)
        arguments("from is numeric zero (int)", 0, null));
    }

    /**
     * Verify that {@code toShortZero} always returns {@code CommonValues.SHORT_ZERO}
     * regardless of the supplied {@code from} argument.
     */
    @Test
    void toShortZero_returnsConstantZero_ForVariousInputs() {
        // GIVEN
        Object[] testValues = { null, (byte) 7, (short) -3, // Integer
        42, // Long
        123L, // Float
        3.14f, // Double
        2.71828, // String
        "123", // Arbitrary object
        new Object() };
        // WHEN / THEN
        for (Object from : testValues) {
            Short result = NumberConversions.toShortZero(from, mockConverter);
            assertEquals(Short.valueOf((short) 0), result, () -> "Expected SHORT_ZERO for input: " + (from == null ? "null" : from.getClass().getSimpleName()));
            // The constant should be the same instance (cached) – optional stricter check:
            assertSame(CommonValues.SHORT_ZERO, result, () -> "Expected the cached SHORT_ZERO instance for input: " + from);
        }
    }

    /**
     * Verify that the method does not interact with the provided {@link Converter}.
     * This guards against future accidental usage of the converter inside the method.
     */
    @Test
    void toShortZero_doesNotUseConverter() {
        // GIVEN
        Object dummy = "dummy";
        // WHEN
        Short result = NumberConversions.toShortZero(dummy, mockConverter);
        // THEN
        assertEquals(Short.valueOf((short) 0), result);
        // No interactions with the mock converter should have occurred.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Parameterized test that covers the exhaustive JSON test matrix.
     * Each case supplies a different {@code from} value (including null) and
     * optionally a non‑null {@link Converter}.  The expected result is always
     * {@code CommonValues.SHORT_ZERO}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("toShortZeroTestCases")
    void toShortZero_parameterized(String description, Object from, Converter converter) {
        // WHEN
        Short result = NumberConversions.toShortZero(from, converter);
        // THEN
        assertSame(CommonValues.SHORT_ZERO, result, () -> "Expected SHORT_ZERO for case: " + description);
        // Ensure the method never touches the converter, even when it is non‑null.
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }
}
