package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toShort_Test {

    /**
     * A mock {@link Converter}. The method under test does not use it, but the signature requires it.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Resets the mock before each test to avoid interaction bleed‑through.
     */
    @BeforeEach
    void setupBeforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Provides a stream of arguments: the source {@link Number},
     * the expected {@code short} result and a short description.
     */
    private static Stream<Arguments> validNumberProvider() {
        return Stream.of(// Integer exceeding Short.MAX_VALUE – overflow semantics
        Arguments.of(Integer.valueOf(12345), (short) 12345, "Integer within int range but exceeds short max; verifies overflow behavior of shortValue()."), // Long just above Short.MAX_VALUE – wraps to Short.MIN_VALUE
        Arguments.of(Long.valueOf(32768L), (short) -32768, "Long value just above Short.MAX_VALUE; expects wrap‑around to Short.MIN_VALUE."), // Direct Short instance
        Arguments.of(Short.valueOf((short) 100), (short) 100, "Direct Short instance; should return the same value."), // Byte promotion
        Arguments.of(Byte.valueOf((byte) 10), (short) 10, "Byte instance; shortValue() promotes the byte correctly."), // Double with fractional part – truncates toward zero
        Arguments.of(Double.valueOf(12.9), (short) 12, "Double with fractional part; shortValue() truncates toward zero."), // BigDecimal with fractional part – discards fraction
        Arguments.of(new BigDecimal("123.456"), (short) 123, "BigDecimal; shortValue() discards fractional part."), // Minimum short value
        Arguments.of(Short.valueOf((short) -32768), (short) -32768, "Minimum short value; verifies that toShort returns the exact same value without alteration."), // Float just below Short.MAX_VALUE – truncates
        Arguments.of(Float.valueOf(32767.9f), (short) 32767, "Float just below Short.MAX_VALUE with fractional part; shortValue() truncates toward zero."), // Float just below Short.MIN_VALUE – overflow semantics
        Arguments.of(Float.valueOf(-32768.1f), (short) -32768, "Float just below Short.MIN_VALUE; truncation yields Short.MIN_VALUE due to overflow semantics of shortValue()."), // BigInteger within range (negative)
        Arguments.of(new BigInteger("-12345"), (short) -12345, "Negative BigInteger within short range; conversion should preserve sign."), // BigDecimal below Short.MIN_VALUE – overflow wraps to Short.MAX_VALUE
        Arguments.of(new BigDecimal("-32769.99"), (short) 32767, "BigDecimal below Short.MIN_VALUE; shortValue() wraps due to overflow, resulting in Short.MAX_VALUE."), // Byte minimum value – sign extension
        Arguments.of(Byte.valueOf((byte) -128), (short) -128, "Byte minimum value; ensures correct sign extension when converting to short."), // BigInteger within range (positive)
        Arguments.of(BigInteger.valueOf(5000), (short) 5000, "BigInteger implements Number; conversion should succeed."));
    }

    @Test
    void toShort_fromInteger_returnsShortValue() {
        // shortValue() will truncate to (short)12345
        Integer source = 12345;
        short expected = source.shortValue();
        short result = NumberConversions.toShort(source, mockConverter);
        assertEquals(expected, result, "Conversion from Integer should produce the same short value as Number.shortValue()");
    }

    @Test
    void toShort_fromLong_returnsShortValue() {
        // exceeds Short.MAX_VALUE, wraps to -32768
        Long source = 32768L;
        short expected = source.shortValue();
        short result = NumberConversions.toShort(source, mockConverter);
        assertEquals(expected, result, "Conversion from Long should follow Java's short truncation rules");
    }

    @Test
    void toShort_fromShort_returnsSameValue() {
        Short source = 42;
        short expected = source.shortValue();
        short result = NumberConversions.toShort(source, mockConverter);
        assertEquals(expected, result, "Conversion from Short should return the original value");
    }

    @Test
    void toShort_fromBigDecimal_returnsShortValue() {
        java.math.BigDecimal source = new java.math.BigDecimal("123.456");
        short expected = source.shortValue();
        short result = NumberConversions.toShort(source, mockConverter);
        assertEquals(expected, result, "Conversion from BigDecimal should use its shortValue() implementation");
    }

    @Test
    void toShort_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toShort(null, mockConverter), "Passing null as the source should throw NullPointerException");
    }

    @Test
    void toShort_nonNumberSource_throwsClassCastException() {
        String source = "not a number";
        assertThrows(ClassCastException.class, () -> NumberConversions.toShort(source, mockConverter), "Passing a non‑Number object should result in a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validNumberProvider")
    void toShort_validNumbers_returnsExpectedShort(Object source, short expected, String description) {
        // Act
        short result = NumberConversions.toShort(source, mockConverter);
        // Assert
        assertEquals(expected, result, () -> "Failed conversion: " + description);
    }

    @Test
    void toShort_atomicIntegerSource_throwsClassCastException() {
        java.util.concurrent.atomic.AtomicInteger source = new java.util.concurrent.atomic.AtomicInteger(5);
        assertThrows(ClassCastException.class, () -> NumberConversions.toShort(source, mockConverter), "AtomicInteger does not implement Number; expect ClassCastException");
    }

    @Test
    void toShort_customNumberSubclass_throwsIllegalStateException() {
        Number badNumber = new Number() {

            @Override
            public int intValue() {
                return 0;
            }

            @Override
            public long longValue() {
                return 0L;
            }

            @Override
            public float floatValue() {
                return 0f;
            }

            @Override
            public double doubleValue() {
                return 0d;
            }

            @Override
            public short shortValue() {
                throw new IllegalStateException("test");
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> NumberConversions.toShort(badNumber, mockConverter), "The overridden shortValue() should propagate its IllegalStateException");
        assertEquals("test", ex.getMessage(), "Exception message should match the one thrown by the custom Number subclass");
    }
}
