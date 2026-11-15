package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toLongZero_Test {

    /**
     * A mock Converter – the method under test does not use it, but the signature requires it.
     */
    @Mock
    Converter mockConverter;

    /**
     * A mock that throws on any interaction – used to prove the method never calls the converter.
     */
    Converter throwingConverter;

    @BeforeEach
    void initThrowingConverter() {
        // Any method call on this mock will throw, making the test fail if the method under test uses it.
        throwingConverter = mock(Converter.class, invocation -> {
            throw new AssertionError("Converter should not be called by toLongZero");
        });
    }

    /**
     * Supplies the exhaustive list of inputs described in the JSON test cases.
     */
    private static Stream<Arguments> provideInputs() {
        return Stream.of(// from = null, converter = null
        Arguments.of(null, null), // from = null, converter = mock (non‑null)
        Arguments.of(null, mock(Converter.class)), // Various non‑null objects with a normal mock converter
        Arguments.of("any string", mock(Converter.class)), Arguments.of(12345L, mock(Converter.class)), Arguments.of(new java.util.Date(), mock(Converter.class)), Arguments.of(new java.math.BigInteger("123"), mock(Converter.class)), Arguments.of(Boolean.TRUE, mock(Converter.class)), Arguments.of(new java.math.BigDecimal("9876.54321"), mock(Converter.class)), Arguments.of(new java.util.concurrent.atomic.AtomicLong(42L), mock(Converter.class)), Arguments.of(java.util.Calendar.getInstance(), mock(Converter.class)), Arguments.of(java.time.LocalDate.now(), mock(Converter.class)), Arguments.of(java.time.Instant.now(), mock(Converter.class)), Arguments.of(new java.sql.Timestamp(System.currentTimeMillis()), mock(Converter.class)), Arguments.of("", mock(Converter.class)), // Generic object with a converter that would throw if used
        Arguments.of(new Object(), mock(Converter.class, invocation -> {
            throw new RuntimeException("Should not be called");
        })));
    }

    /**
     * Verify that {@code NumberConversions.toLongZero} always returns a {@code Long}
     * representing zero, regardless of the supplied {@code from} argument.
     */
    @Test
    @DisplayName("toLongZero returns 0L for a null source")
    void toLongZero_returnsZero_forNull() {
        Long result = NumberConversions.toLongZero(null, mockConverter);
        assertNotNull(result, "Result must not be null");
        assertEquals(0L, result.longValue(), "Result should be zero");
    }

    @Test
    @DisplayName("toLongZero returns 0L for an Integer source")
    void toLongZero_returnsZero_forInteger() {
        Long result = NumberConversions.toLongZero(Integer.valueOf(123), mockConverter);
        assertEquals(0L, result);
    }

    @Test
    @DisplayName("toLongZero returns 0L for a String source")
    void toLongZero_returnsZero_forString() {
        Long result = NumberConversions.toLongZero("456", mockConverter);
        assertEquals(0L, result);
    }

    @Test
    @DisplayName("toLongZero returns 0L for a BigDecimal source")
    void toLongZero_returnsZero_forBigDecimal() {
        Long result = NumberConversions.toLongZero(new java.math.BigDecimal("789.01"), mockConverter);
        assertEquals(0L, result);
    }

    @Test
    @DisplayName("toLongZero returns 0L for a custom object source")
    void toLongZero_returnsZero_forCustomObject() {
        Object custom = new Object();
        Long result = NumberConversions.toLongZero(custom, mockConverter);
        assertEquals(0L, result);
    }

    @Test
    @DisplayName("toLongZero always returns the same Long instance (CommonValues.LONG_ZERO)")
    void toLongZero_returnsSameInstance() {
        Long first = NumberConversions.toLongZero("any", mockConverter);
        Long second = NumberConversions.toLongZero(42, mockConverter);
        assertSame(first, second, "Both calls should return the same Long instance representing zero");
    }

    @ParameterizedTest(name = "{index}: from={0}, converter={1}")
    @MethodSource("provideInputs")
    @DisplayName("toLongZero always returns CommonValues.LONG_ZERO")
    void toLongZero_returnsConstant(Object from, Converter converter) {
        // Act
        Long result = NumberConversions.toLongZero(from, converter);
        // Assert – the returned value must be the predefined constant.
        assertNotNull(result, "Result must not be null");
        assertSame(CommonValues.LONG_ZERO, result, "Method must return the exact CommonValues.LONG_ZERO instance");
        assertEquals(0L, result.longValue(), "The constant must represent zero");
        // Verify that the converter (if any) was never interacted with.
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }

    @Test
    @DisplayName("toLongZero returns the same instance on repeated calls")
    void toLongZero_returnsSameInstanceOnRepeatedCalls() {
        Long first = NumberConversions.toLongZero("first", mockConverter);
        Long second = NumberConversions.toLongZero("second", mockConverter);
        assertSame(first, second, "Both calls should return the identical Long instance");
        assertSame(CommonValues.LONG_ZERO, first, "The instance must be CommonValues.LONG_ZERO");
    }

    @Test
    @DisplayName("toLongZero never invokes a throwing converter")
    void toLongZero_neverInvokesThrowingConverter() {
        // Act
        Long result = NumberConversions.toLongZero("any", throwingConverter);
        // Assert
        assertSame(CommonValues.LONG_ZERO, result, "Even with a converter that throws on use, the method must return LONG_ZERO");
        // No need for explicit verify – any interaction would have thrown already.
    }
}
