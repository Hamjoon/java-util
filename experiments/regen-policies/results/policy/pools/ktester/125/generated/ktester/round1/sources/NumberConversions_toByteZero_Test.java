package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toByteZero_Test {

    /**
     * Mocked {@link Converter} – the method under test must not call any of its methods.
     */
    @Mock
    private Converter converter;

    /**
     * Regular mock – the method under test must not call any of its methods.
     */
    @Mock
    private Converter regularConverter;

    /**
     * A mock that throws on any interaction – used to ensure the method truly never touches the converter.
     */
    private static Converter throwingConverter;

    /**
     * Simple user‑defined class used in one of the test scenarios.
     */
    private static class CustomClass {
        // No fields or methods – the content is irrelevant for the test.
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Create a mock that throws if any method is invoked.
        throwingConverter = mock(Converter.class, invocation -> {
            throw new IllegalStateException("Converter should not be called");
        });
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock automatically via @Mock annotation.
        // Ensure no prior interactions.
        clearInvocations(converter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that neither mock was used during the test.
        verifyNoInteractions(regularConverter);
        // The throwing mock cannot be verified for interactions because any call would already
        // have thrown an exception; however, we can still assert that no unexpected calls occurred.
        // (Mockito records interactions even when they throw, so we can safely verify.)
        verifyNoInteractions(throwingConverter);
    }

    /**
     * Supplies all test cases described in the JSON document.
     *
     * @return a stream of arguments: from‑object, converter‑instance, description
     */
    private static Stream<Arguments> provideTestCases() {
        // Helper objects used in multiple cases.
        Object genericObject = new Object();
        byte[] byteArray = new byte[] { 1, 2, 3 };
        BigDecimal bigDecimal = new BigDecimal("123.456");
        AtomicInteger atomicInt = new AtomicInteger(42);
        CustomClass custom = new CustomClass();
        return Stream.of(// 1. both parameters null
        Arguments.of(null, null, "Both parameters are null"), // 2. Integer source, null converter
        Arguments.of(123, null, "Integer source with null converter"), // 3. String source, regular mock converter
        Arguments.of("any string", mock(Converter.class), "String source with mock converter"), // 4. Double source, regular mock converter
        Arguments.of(3.14, mock(Converter.class), "Double source with mock converter"), // 5. Boolean source, null converter
        Arguments.of(Boolean.TRUE, null, "Boolean source with null converter"), // 6. Generic Object source, null converter
        Arguments.of(genericObject, null, "Generic Object source with null converter"), // 7. Primitive byte array source, null converter
        Arguments.of(byteArray, null, "byte[] source with null converter"), // 8. BigDecimal source, null converter
        Arguments.of(bigDecimal, null, "BigDecimal source with null converter"), // 9. AtomicInteger source, null converter
        Arguments.of(atomicInt, null, "AtomicInteger source with null converter"), //10. User‑defined class instance, null converter
        Arguments.of(custom, null, "CustomClass instance with null converter"), //11. Null source, throwing converter (should not be invoked)
        Arguments.of(null, throwingConverter, "Null source with throwing converter"));
    }

    @Test
    void toByteZero_returnsZeroByte_forNullInput() {
        Byte result = NumberConversions.toByteZero(null, converter);
        assertEquals(Byte.valueOf((byte) 0), result, "Expected zero byte for null input");
    }

    @Test
    void toByteZero_returnsZeroByte_forStringInput() {
        Byte result = NumberConversions.toByteZero("123", converter);
        assertEquals(Byte.valueOf((byte) 0), result, "Expected zero byte for String input");
    }

    @Test
    void toByteZero_returnsZeroByte_forNumericInput() {
        Byte result = NumberConversions.toByteZero(42, converter);
        assertEquals(Byte.valueOf((byte) 0), result, "Expected zero byte for numeric input");
    }

    @Test
    void toByteZero_returnsZeroByte_forObjectInput() {
        Object someObject = new Object();
        Byte result = NumberConversions.toByteZero(someObject, converter);
        assertEquals(Byte.valueOf((byte) 0), result, "Expected zero byte for generic Object input");
    }

    @Test
    void toByteZero_returnsSameInstance_asCommonValues() {
        Byte result = NumberConversions.toByteZero("any", converter);
        // The method returns the exact constant from CommonValues, so reference equality should hold.
        assertSame(CommonValues.BYTE_ZERO, result, "Expected the same constant instance from CommonValues");
    }

    /**
     * Parameterized test that iterates over all JSON‑defined scenarios.
     *
     * @param from        the source object passed to {@code toByteZero}
     * @param converter   the {@link Converter} instance (may be {@code null})
     * @param description textual description of the scenario (used only for readability)
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideTestCases")
    void toByteZero_behaviour(Object from, Converter converter, String description) {
        // Act
        Byte result = NumberConversions.toByteZero(from, converter);
        // Assert that the returned value is the constant zero byte.
        assertEquals(Byte.valueOf((byte) 0), result, "Expected zero byte for input: " + description);
        // Additionally assert that the exact same instance from CommonValues is returned.
        assertSame(CommonValues.BYTE_ZERO, result, "Expected the same constant instance from CommonValues for: " + description);
    }
}
