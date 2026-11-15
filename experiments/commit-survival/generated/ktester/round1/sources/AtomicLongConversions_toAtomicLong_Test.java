package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalTime;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(MockitoExtension.class)
class AtomicLongConversions_toAtomicLong_Test {

    /**
     * Mocked Converter – not used by the current implementation but required by the signature.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A sample AtomicLong used as the source value for the conversion.
     */
    private AtomicLong sourceAtomicLong;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh source AtomicLong for each test.
        sourceAtomicLong = new AtomicLong(12345L);
        // Ensure the mock is reset before each test.
        Mockito.reset(mockConverter);
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// value 0L
        Arguments.of(new AtomicLong(0L), 0L), // a regular positive number
        Arguments.of(new AtomicLong(123456789L), 123456789L), // Long.MAX_VALUE edge case
        Arguments.of(new AtomicLong(Long.MAX_VALUE), Long.MAX_VALUE), // Long.MIN_VALUE edge case
        Arguments.of(new AtomicLong(Long.MIN_VALUE), Long.MIN_VALUE));
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(// null input – the implementation dereferences the casted value, leading to NPE
        Arguments.of(null, NullPointerException.class), // incompatible type – results in a ClassCastException at the cast site
        Arguments.of("not an atomic long", ClassCastException.class));
    }

    /**
     * Verifies that {@code toAtomicLong} creates a new {@link AtomicLong}
     * instance whose value equals the value of the supplied {@code AtomicLong}
     * argument, and that the returned instance is distinct from the input.
     */
    @Test
    void shouldReturnNewAtomicLongWithSameValue() {
        // GIVEN
        // the method expects an Object
        Object input = sourceAtomicLong;
        // WHEN
        AtomicLong result = AtomicLongConversions.toAtomicLong(input, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(sourceAtomicLong.get(), result.get(), "Resulting AtomicLong should contain the same value as the source");
        assertNotSame(sourceAtomicLong, result, "Result should be a new instance, not the original AtomicLong");
        // The converter is not used in the current implementation, but verify no interactions.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Ensures that passing a non‑AtomicLong object results in a {@link ClassCastException},
     * matching the behaviour of the cast inside the method.
     */
    @Test
    void shouldThrowClassCastExceptionWhenInputIsNotAtomicLong() {
        // GIVEN
        Object notAtomicLong = "not an atomic long";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> AtomicLongConversions.toAtomicLong(notAtomicLong, mockConverter), "A non‑AtomicLong input should cause a ClassCastException");
        verifyNoInteractions(mockConverter);
    }

    /**
     * Successful conversions – the method should return a new {@link AtomicLong}
     * with the same numeric value as the source.
     */
    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("successfulConversionProvider")
    void shouldConvertAtomicLongValues(Object from, long expectedValue) {
        // WHEN
        AtomicLong result = AtomicLongConversions.toAtomicLong(from, mockConverter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedValue, result.get(), "Returned AtomicLong should contain the expected value");
        // Ensure a *new* instance is created.
        assertNotSame(from, result, "Returned AtomicLong must be a distinct instance from the input");
        verifyNoInteractions(mockConverter);
    }

    /**
     * Cases that are expected to throw an exception.
     */
    @ParameterizedTest(name = "[{index}] from={0} → throws={1}")
    @MethodSource("exceptionConversionProvider")
    void shouldThrowWhenConversionIsInvalid(Object from, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> AtomicLongConversions.toAtomicLong(from, mockConverter), "Conversion should fail with the expected exception type");
        verifyNoInteractions(mockConverter);
    }
}
