package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toShort_Test {

    // The Converter is not used by the method under test, but we provide a mock to satisfy the signature.
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> nonAtomicBooleanSources() {
        return Stream.of(arguments("some string"), arguments(Integer.valueOf(5)));
    }

    @Test
    void toShort_whenAtomicBooleanTrue_returnsOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Short result = AtomicBooleanConversions.toShort(source, mockConverter);
        // then
        assertEquals(Short.valueOf((short) 1), result, "AtomicBoolean true should be converted to Short value 1");
    }

    @Test
    void toShort_whenAtomicBooleanFalse_returnsZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Short result = AtomicBooleanConversions.toShort(source, mockConverter);
        // then
        assertEquals(Short.valueOf((short) 0), result, "AtomicBoolean false should be converted to Short value 0");
    }

    @Test
    void toShort_whenSourceIsNull_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toShort(source, mockConverter), "Passing null should result in NullPointerException because the method casts to AtomicBoolean");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("nonAtomicBooleanSources")
    void toShort_whenSourceIsNotAtomicBoolean_throwsClassCastException(Object badSource) {
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toShort(badSource, mockConverter), "A non‑AtomicBoolean source should cause a ClassCastException");
    }

    @Test
    void toShort_withNonNullConverter_ignoresConverter_true() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        Converter nonNullConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> tgt) {
                // should never be called
                fail("Converter.convert should not be invoked");
                return null;
            }
        };
        // when
        Short result = AtomicBooleanConversions.toShort(source, nonNullConverter);
        // then
        assertEquals(Short.valueOf((short) 1), result, "Converter argument must be ignored – true case");
    }

    @Test
    void toShort_withNonNullConverter_ignoresConverter_false() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        Converter nonNullConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> tgt) {
                // should never be called
                fail("Converter.convert should not be invoked");
                return null;
            }
        };
        // when
        Short result = AtomicBooleanConversions.toShort(source, nonNullConverter);
        // then
        assertEquals(Short.valueOf((short) 0), result, "Converter argument must be ignored – false case");
    }

    @Test
    void toShort_withConverterThatThrows_ignoresConverter() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        Converter explodingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> tgt) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        // when
        Short result = AtomicBooleanConversions.toShort(source, explodingConverter);
        // then
        assertEquals(Short.valueOf((short) 1), result, "Even a Converter that throws must not be invoked");
    }

    @Test
    void toShort_returnsCanonicalInstance_true() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Short result = AtomicBooleanConversions.toShort(source, mockConverter);
        // then
        assertSame(CommonValues.SHORT_ONE, result, "Result should be the canonical Short instance SHORT_ONE");
    }

    @Test
    void toShort_returnsCanonicalInstance_false() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Short result = AtomicBooleanConversions.toShort(source, mockConverter);
        // then
        assertSame(CommonValues.SHORT_ZERO, result, "Result should be the canonical Short instance SHORT_ZERO");
    }
}
