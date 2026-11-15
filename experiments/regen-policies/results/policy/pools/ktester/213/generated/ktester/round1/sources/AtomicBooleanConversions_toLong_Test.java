package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toLong_Test {

    /**
     * Mocked Converter – the method under test does not use it, but it is required by the signature.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A minimal dummy implementation of {@link Converter} used in parameterised tests.
     */
    private static class DummyConverter implements Converter {
        // No behaviour needed – the method under test never calls it.
    }

    /**
     * Provides test arguments derived from the JSON specification.
     *
     * Each {@link Arguments} contains:
     * <ul>
     *   <li>the {@code from} object</li>
     *   <li>the {@code Converter} instance (may be {@code null})</li>
     *   <li>the expected result – either a {@link Long} or an exception class</li>
     *   <li>a description for readability</li>
     * </ul>
     */
    private static Stream<Arguments> toLongProvider() {
        return Stream.of(// true AtomicBoolean, null converter → 1L
        Arguments.of(new AtomicBoolean(true), null, CommonValues.LONG_ONE, "AtomicBoolean holds true – method should return LONG_ONE (1L). Converter is unused, passing null should not affect the result."), // false AtomicBoolean, null converter → 0L
        Arguments.of(new AtomicBoolean(false), null, CommonValues.LONG_ZERO, "AtomicBoolean holds false – method should return LONG_ZERO (0L)."), // true AtomicBoolean, non‑null converter → 1L
        Arguments.of(new AtomicBoolean(true), new DummyConverter(), CommonValues.LONG_ONE, "Even when a non‑null Converter implementation is supplied (the method does not use it), a true AtomicBoolean must still yield 1L."), // null source → NPE
        Arguments.of(null, null, NullPointerException.class, "Casting null to AtomicBoolean yields null; calling b.get() on a null reference throws NullPointerException."), // wrong type (plain Boolean) → ClassCastException
        Arguments.of(Boolean.TRUE, null, ClassCastException.class, "If the supplied object is not an AtomicBoolean (e.g., a plain Boolean), the explicit cast fails and a ClassCastException is thrown."));
    }

    @Test
    void toLong_returnsOneWhenAtomicBooleanIsTrue() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Long result = AtomicBooleanConversions.toLong(source, mockConverter);
        // then
        assertEquals(CommonValues.LONG_ONE, result, "AtomicBoolean true should be converted to LONG_ONE (1L)");
    }

    @Test
    void toLong_returnsZeroWhenAtomicBooleanIsFalse() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Long result = AtomicBooleanConversions.toLong(source, mockConverter);
        // then
        assertEquals(CommonValues.LONG_ZERO, result, "AtomicBoolean false should be converted to LONG_ZERO (0L)");
    }

    @Test
    void toLong_throwsNullPointerExceptionWhenSourceIsNull() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toLong(source, mockConverter), "Passing null as the source should result in NPE when .get() is invoked");
    }

    @Test
    void toLong_throwsClassCastExceptionWhenSourceIsNotAtomicBoolean() {
        // given
        Object source = "not an atomic boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toLong(source, mockConverter), "Passing an object that is not an AtomicBoolean should cause a ClassCastException");
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("toLongProvider")
    void toLong_parameterisedTests(Object from, Converter converter, Object expected, String description) {
        // The test logic branches based on the type of the expected value.
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expected an exception.
            Class<? extends Throwable> expectedException = (Class<? extends Throwable>) expected;
            assertThrows(expectedException, () -> AtomicBooleanConversions.toLong(from, converter), description);
        } else {
            // Expected a normal Long result.
            Long result = AtomicBooleanConversions.toLong(from, converter);
            assertEquals(expected, result, description);
        }
    }
}
