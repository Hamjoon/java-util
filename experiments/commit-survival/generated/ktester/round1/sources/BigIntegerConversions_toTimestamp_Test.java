package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigInteger;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toTimestamp_Test {

    /**
     * Shared mock Converter used for all tests.
     */
    private static Converter mockConverter;

    /**
     * A reusable {@link Instant} instance for stubbing.
     */
    private static final Instant FIXED_INSTANT = Instant.parse("2023-01-01T12:34:56.789Z");

    /**
     * Simple concrete implementation of {@link Converter} for tests that need a non‑null instance.
     */
    private static class MockConverter implements Converter {
        // No behaviour required for these tests – the class exists only to satisfy the API.
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a mock Converter once for the whole test suite.
        mockConverter = mock(Converter.class);
    }

    /**
     * Verifies that {@code toTimestamp} calls {@code toInstant} and correctly wraps the result.
     */
    @Test
    void testToTimestampDelegatesToInstant() {
        // Arrange – stub the static call to toInstant.
        try (MockedStatic<BigIntegerConversions> mocked = Mockito.mockStatic(BigIntegerConversions.class)) {
            mocked.when(() -> BigIntegerConversions.toInstant(any(), eq(mockConverter))).thenReturn(FIXED_INSTANT);
            // Act – invoke the method under test.
            Timestamp result = BigIntegerConversions.toTimestamp("anyObject", mockConverter);
            // Assert – the result must be a Timestamp representing the same instant.
            assertNotNull(result, "Timestamp should not be null");
            assertEquals(Timestamp.from(FIXED_INSTANT), result, "Timestamp should be created from the Instant returned by toInstant");
            // Verify that toInstant was called exactly once with the supplied arguments.
            mocked.verify(() -> BigIntegerConversions.toInstant("anyObject", mockConverter), times(1));
        }
    }

    /**
     * Ensures that a runtime exception thrown by {@code toInstant} propagates unchanged.
     */
    @Test
    void testToTimestampPropagatesExceptionFromToInstant() {
        RuntimeException expected = new IllegalArgumentException("bad conversion");
        try (MockedStatic<BigIntegerConversions> mocked = Mockito.mockStatic(BigIntegerConversions.class)) {
            mocked.when(() -> BigIntegerConversions.toInstant(any(), eq(mockConverter))).thenThrow(expected);
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toTimestamp("badObject", mockConverter), "Exception from toInstant should propagate");
            assertSame(expected, thrown, "The thrown exception should be the same instance");
        }
    }

    /**
     * Checks that a {@code null} source value results in a {@code null} Timestamp
     * when {@code toInstant} returns {@code null}.
     */
    @Test
    void testToTimestampWithNullInstantResult() {
        try (MockedStatic<BigIntegerConversions> mocked = Mockito.mockStatic(BigIntegerConversions.class)) {
            mocked.when(() -> BigIntegerConversions.toInstant(isNull(), eq(mockConverter))).thenReturn(null);
            Timestamp result = BigIntegerConversions.toTimestamp(null, mockConverter);
            assertNull(result, "Timestamp should be null when toInstant returns null");
        }
    }

    /**
     * Passing a {@code null} source object should cause a {@link NullPointerException}
     * because the conversion to {@link Instant} cannot handle a null value.
     */
    @Test
    void testToTimestamp_NullFrom() {
        Converter converter = new MockConverter();
        try (MockedStatic<BigIntegerConversions> mocked = Mockito.mockStatic(BigIntegerConversions.class)) {
            mocked.when(() -> BigIntegerConversions.toInstant(isNull(), eq(converter))).thenThrow(new NullPointerException("source is null"));
            assertThrows(NullPointerException.class, () -> BigIntegerConversions.toTimestamp(null, converter), "Null source should trigger NullPointerException");
        }
    }

    /**
     * Providing a {@code null} {@link Converter} instance should trigger a {@link NullPointerException}.
     */
    @Test
    void testToTimestamp_NullConverter() {
        Instant now = Instant.now();
        try (MockedStatic<BigIntegerConversions> mocked = Mockito.mockStatic(BigIntegerConversions.class)) {
            mocked.when(() -> BigIntegerConversions.toInstant(eq(now), isNull())).thenThrow(new NullPointerException("converter is null"));
            assertThrows(NullPointerException.class, () -> BigIntegerConversions.toTimestamp(now, null), "Null converter should trigger NullPointerException");
        }
    }

    /**
     * Supplying a value of an unsupported type (Boolean) should cause an
     * {@link IllegalArgumentException} because the conversion to {@link Instant}
     * cannot be performed.
     */
    @Test
    void testToTimestamp_UnsupportedType() {
        Converter converter = new MockConverter();
        try (MockedStatic<BigIntegerConversions> mocked = Mockito.mockStatic(BigIntegerConversions.class)) {
            mocked.when(() -> BigIntegerConversions.toInstant(eq(true), eq(converter))).thenThrow(new IllegalArgumentException("Unsupported type: Boolean"));
            assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toTimestamp(true, converter), "Unsupported source type should trigger IllegalArgumentException");
        }
    }

    /**
     * A {@link BigInteger} larger than {@link Long#MAX_VALUE} milliseconds since epoch
     * overflows when converting to {@link Instant}, resulting in an {@link ArithmeticException}.
     */
    @Test
    void testToTimestamp_NumberOverflow() {
        Converter converter = new MockConverter();
        // Long.MAX_VALUE + 1
        BigInteger tooBig = new BigInteger("9223372036854775808");
        try (MockedStatic<BigIntegerConversions> mocked = Mockito.mockStatic(BigIntegerConversions.class)) {
            mocked.when(() -> BigIntegerConversions.toInstant(eq(tooBig), eq(converter))).thenThrow(new ArithmeticException("Overflow while converting BigInteger to Instant"));
            assertThrows(ArithmeticException.class, () -> BigIntegerConversions.toTimestamp(tooBig, converter), "Overflow should trigger ArithmeticException");
        }
    }

    /**
     * {@link Instant#MAX} is outside the range representable by {@link Timestamp};
     * {@code Timestamp.from(Instant.MAX)} throws {@link IllegalArgumentException}.
     * This test verifies that the exception propagates from {@code toTimestamp}.
     */
    @Test
    void testToTimestamp_InstantOutOfRange() {
        Converter converter = new MockConverter();
        Instant outOfRange = Instant.MAX;
        try (MockedStatic<BigIntegerConversions> mocked = Mockito.mockStatic(BigIntegerConversions.class)) {
            // Let toInstant succeed and return the out‑of‑range instant.
            mocked.when(() -> BigIntegerConversions.toInstant(eq(outOfRange), eq(converter))).thenReturn(outOfRange);
            assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toTimestamp(outOfRange, converter), "Timestamp.from should throw IllegalArgumentException for Instant.MAX");
        }
    }
}
