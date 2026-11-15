package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toAtomicLong_Test {

    /**
     * Mocked {@link Converter} – the method under test does not use it, but it must be supplied.
     */
    @Mock
    Converter converter;

    private static Stream<Arguments> provideAtomicBooleanAndConverter() {
        // Normal mock (no‑op)
        Converter mockConverter = mock(Converter.class);
        // Throwing mock – should never be called
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("Unexpected call");
        });
        return Stream.of(arguments(new AtomicBoolean(true), 1L, "null converter", null), arguments(new AtomicBoolean(false), 0L, "null converter", null), arguments(new AtomicBoolean(true), 1L, "no‑op mock", mockConverter), arguments(new AtomicBoolean(false), 0L, "no‑op mock", mockConverter), arguments(new AtomicBoolean(true), 1L, "throwing mock", throwingConverter), arguments(new AtomicBoolean(false), 0L, "throwing mock", throwingConverter));
    }

    @Test
    void toAtomicLong_whenAtomicBooleanTrue_returnsOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, converter);
        // then
        assertNotNull(result, "Result should never be null");
        assertEquals(1L, result.get(), "AtomicBoolean true should map to AtomicLong(1)");
    }

    @Test
    void toAtomicLong_whenAtomicBooleanFalse_returnsZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, converter);
        // then
        assertNotNull(result, "Result should never be null");
        assertEquals(0L, result.get(), "AtomicBoolean false should map to AtomicLong(0)");
    }

    @Test
    void toAtomicLong_whenNullSource_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toAtomicLong(source, converter), "Passing null should result in NullPointerException due to cast");
    }

    @Test
    void toAtomicLong_whenWrongType_throwsClassCastException() {
        // given
        Object source = "not an AtomicBoolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toAtomicLong(source, converter), "Passing a non‑AtomicBoolean should result in ClassCastException");
    }

    @Test
    void toAtomicLong_whenConverterIsNull_ignoresConverter() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // explicit null
        Converter nullConverter = null;
        // when
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, nullConverter);
        // then
        assertEquals(1L, result.get(), "Converter is ignored – true should still produce AtomicLong(1)");
    }

    @Test
    void toAtomicLong_whenMockConverterProvided_ignoresConverter() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // 'converter' mock is already injected by MockitoExtension
        // when
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, converter);
        // then
        assertEquals(0L, result.get(), "Mock Converter is ignored – false should still produce AtomicLong(0)");
    }

    @Test
    void toAtomicLong_whenThrowingConverterProvided_ignoresConverter() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // Create a mock that throws on any method invocation – the method under test must not call it.
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be invoked");
        });
        // when
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, throwingConverter);
        // then
        assertEquals(1L, result.get(), "Even a throwing Converter must be ignored – true should still produce AtomicLong(1)");
    }

    @Test
    void toAtomicLong_whenSourceMutatedAfterConversion_returnsOriginalValue() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, converter);
        // mutate the original AtomicBoolean after conversion
        source.set(false);
        // then
        assertEquals(1L, result.get(), "Returned AtomicLong must retain the value captured at call time, even if source changes later");
    }

    @ParameterizedTest(name = "{index}: from={0}, converter={2} => expected={1}")
    @MethodSource("provideAtomicBooleanAndConverter")
    void toAtomicLong_parameterised(AtomicBoolean from, long expectedValue, String converterDesc, Converter providedConverter) {
        // when
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(from, providedConverter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedValue, result.get(), () -> String.format("AtomicBoolean %s with %s should yield %d", from.get(), converterDesc, expectedValue));
    }
}
