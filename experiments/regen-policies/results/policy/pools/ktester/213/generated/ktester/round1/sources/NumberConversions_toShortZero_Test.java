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
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toShortZero_Test {

    /**
     * A mock {@link Converter} – the implementation is irrelevant for this method.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Dummy {@link Converter} implementation that does nothing.  It exists
     * solely to satisfy the method signature without influencing the outcome.
     */
    private static class DummyConverter implements Converter {

        @Override
        public Object convert(Object src, Class<?> dest) {
            // No‑op – the method should never be called.
            return null;
        }
    }

    /**
     * Initializes Mockito annotations before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension already initializes @Mock fields, but we keep this method
        // for symmetry with the provided template and future extensibility.
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Supplies a stream of arguments covering all combinations described in the
     * JSON test cases.  The {@code Converter} argument may be:
     * <ul>
     *   <li>{@code null}</li>
     *   <li>a Mockito mock (already injected as {@code mockConverter})</li>
     *   <li>a custom implementation that throws if its {@code convert} method is called</li>
     * </ul>
     */
    private static Stream<Arguments> provideToShortZeroInputs() {
        // A converter that would throw if any conversion is attempted.
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                throw new AssertionError("Converter should never be invoked");
            }
        };
        return Stream.of(// from = null, converter = null
        Arguments.of(null, null), // from = null, converter = mock (verify no interaction later)
        Arguments.of(null, mockConverter()), // from = "some string", converter = new dummy implementation
        Arguments.of("some string", new DummyConverter()), // from = Short with non‑zero value, converter = null
        Arguments.of(Short.valueOf((short) 123), null), // from = Integer, converter = mock
        Arguments.of(Integer.valueOf(456), mockConverter()), // from = BigDecimal, converter = null
        Arguments.of(new BigDecimal("789.01"), null), // from = generic Object, converter = new dummy implementation
        Arguments.of(new Object(), new DummyConverter()), // from = Calendar instance, converter = null
        Arguments.of(Calendar.getInstance(), null), // from = java.util.Date, converter = new dummy implementation
        Arguments.of(new Date(), new DummyConverter()), // from = Boolean.TRUE, converter = new dummy implementation
        Arguments.of(Boolean.TRUE, new DummyConverter()), // from = null, converter = throwingConverter (should not be called)
        Arguments.of(null, throwingConverter), // from = "any string", converter = throwingConverter (should not be called)
        Arguments.of("any string", throwingConverter));
    }

    /**
     * Helper method to obtain the {@code mockConverter} field for the static
     * method source.  The field is injected by Mockito, but static contexts
     * cannot directly reference it, so we expose it via a method.
     */
    private static Converter mockConverter() {
        // This method will be replaced at runtime by the test instance's field.
        // The body will never be executed because JUnit creates a fresh test
        // instance for each invocation and injects the mock before the
        // parameterised test runs.
        return null;
    }

    /**
     * Verifies that {@code toShortZero} returns the constant {@code SHORT_ZERO}
     * when the source object is {@code null}.
     */
    @Test
    void toShortZero_returnsZeroWhenSourceIsNull() {
        // given
        Object source = null;
        // when
        Short result = NumberConversions.toShortZero(source, mockConverter);
        // then
        assertEquals(Short.valueOf((short) 0), result, "Expected SHORT_ZERO for null source");
        // The constant is a shared instance; we can also check identity.
        assertSame(CommonValues.SHORT_ZERO, result, "Expected the exact SHORT_ZERO instance");
    }

    /**
     * Verifies that {@code toShortZero} returns the constant {@code SHORT_ZERO}
     * for a non‑null source of an arbitrary type (e.g., {@link Integer}).
     */
    @Test
    void toShortZero_returnsZeroForIntegerSource() {
        // given
        Object source = Integer.valueOf(12345);
        // when
        Short result = NumberConversions.toShortZero(source, mockConverter);
        // then
        assertEquals(Short.valueOf((short) 0), result, "Expected SHORT_ZERO for Integer source");
        assertSame(CommonValues.SHORT_ZERO, result, "Expected the exact SHORT_ZERO instance");
    }

    /**
     * Verifies that {@code toShortZero} returns the constant {@code SHORT_ZERO}
     * for a non‑null source of a different type (e.g., {@link String}).
     */
    @Test
    void toShortZero_returnsZeroForStringSource() {
        // given
        Object source = "some text";
        // when
        Short result = NumberConversions.toShortZero(source, mockConverter);
        // then
        assertEquals(Short.valueOf((short) 0), result, "Expected SHORT_ZERO for String source");
        assertSame(CommonValues.SHORT_ZERO, result, "Expected the exact SHORT_ZERO instance");
    }

    /**
     * Verifies that {@code toShortZero} does not interact with the provided
     * {@link Converter}. This ensures the method truly ignores the converter.
     */
    @Test
    void toShortZero_doesNotUseConverter() {
        // given
        Object source = new Object();
        // when
        Short result = NumberConversions.toShortZero(source, mockConverter);
        // then
        assertEquals(Short.valueOf((short) 0), result);
        // Verify that no method on the mock converter was called.
        // Since Converter has many methods, we simply verify zero interactions.
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "toShortZero with from={0} and converter={1}")
    @MethodSource("provideToShortZeroInputs")
    void toShortZero_alwaysReturnsConstant(Object from, Converter converter) {
        // when
        Short result = NumberConversions.toShortZero(from, converter);
        // then
        assertSame(CommonValues.SHORT_ZERO, result, "Method must return the shared SHORT_ZERO instance");
        assertEquals(Short.valueOf((short) 0), result, "Returned Short must represent zero");
    }

    /**
     * Verifies that two successive calls with identical null arguments return
     * the exact same {@link Short} instance (reference equality).
     */
    @Test
    void toShortZero_sameInstanceOnRepeatedCalls() {
        // first call
        Short first = NumberConversions.toShortZero(null, null);
        // second call
        Short second = NumberConversions.toShortZero(null, null);
        assertSame(first, second, "Repeated calls should return the same SHORT_ZERO instance");
    }

    /**
     * Ensures that even when a {@link Converter} implementation that throws on
     * usage is supplied, {@code toShortZero} never invokes it.
     */
    @Test
    void toShortZero_neverInvokesThrowingConverter() {
        // Arrange a converter that would throw if its convert method is called.
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                throw new AssertionError("Converter should not be called");
            }
        };
        // Act & Assert – no exception should be thrown.
        assertDoesNotThrow(() -> {
            Short result = NumberConversions.toShortZero("irrelevant", throwingConverter);
            assertSame(CommonValues.SHORT_ZERO, result);
        });
    }
}
