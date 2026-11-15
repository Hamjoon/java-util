package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toShortZero_Test {

    /**
     * A mock {@link Converter} that can be reused across tests.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Supplies a stream of arguments covering the cases described in the JSON test data.
     */
    private static Stream<Arguments> provideInputsForToShortZero() {
        return Stream.of(// No arguments (both null) – matches the first JSON case.
        Arguments.of(null, null), // Explicit null for 'from' and a mock converter – second JSON case.
        // The mock will be injected by JUnit; we pass null here because the
        // test method will receive the field mockConverter via the test instance.
        Arguments.of(null, null), // Non‑null object with a mock converter – third JSON case.
        Arguments.of("123", null), // Additional interesting variations to increase coverage.
        Arguments.of(new Object(), null), Arguments.of(42, null), Arguments.of(3.14, null));
    }

    @Test
    void toShortZero_returnsZeroForNullInput() {
        Short result = NumberConversions.toShortZero(null, mockConverter);
        assertNotNull(result, "Result should never be null");
        assertEquals(Short.valueOf((short) 0), result, "Result must be zero");
        assertSame(CommonValues.SHORT_ZERO, result, "Should return the shared constant");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toShortZero_returnsZeroForNonNullInput() {
        Object someObject = new Object();
        Short result = NumberConversions.toShortZero(someObject, mockConverter);
        assertNotNull(result);
        assertEquals(Short.valueOf((short) 0), result);
        assertSame(CommonValues.SHORT_ZERO, result);
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toShortZero_returnsSameInstanceAsCommonValues() {
        Short result = NumberConversions.toShortZero("any", mockConverter);
        // The method is specified to return CommonValues.SHORT_ZERO, which is a shared constant.
        // Verify reference equality to ensure the constant is used.
        assertSame(CommonValues.SHORT_ZERO, result);
    }

    @Test
    void toShortZero_ignoresConverterBehaviour() {
        // Stub the mock converter to return a non‑zero value if it were called.
        when(mockConverter.convert(any(), any())).thenReturn(Short.valueOf((short) 123));
        Short result = NumberConversions.toShortZero(42, mockConverter);
        assertEquals(Short.valueOf((short) 0), result);
        assertSame(CommonValues.SHORT_ZERO, result);
        // The method must not invoke the converter at all.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Parameterized test that supplies a variety of {@code from} objects and {@link Converter}
     * instances (including {@code null}) and asserts that the method always returns the
     * shared {@link CommonValues#SHORT_ZERO} constant.
     */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("provideInputsForToShortZero")
    void toShortZero_alwaysReturnsConstant(Object from, Converter converter) {
        // Act
        Short result = NumberConversions.toShortZero(from, converter);
        // Assert: result is never null and equals the constant zero value.
        assertNotNull(result, "Result must never be null");
        assertEquals(Short.valueOf((short) 0), result, "Result must be a Short with value 0");
        // Assert: the exact same instance from CommonValues is returned.
        assertSame(CommonValues.SHORT_ZERO, result, "Method should return the shared constant instance");
        // Verify that the converter (if any) was never interacted with.
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }
}
