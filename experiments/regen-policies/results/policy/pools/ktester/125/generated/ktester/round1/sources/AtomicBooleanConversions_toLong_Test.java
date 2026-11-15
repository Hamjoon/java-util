package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toLong_Test {

    /**
     * Converter instance required by the method signature – the implementation does not use it.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter constructor needs a ConverterOptions instance.
        // No special configuration is required for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps GC in larger test suites.
        converter = null;
    }

    /**
     * Supplies the arguments for {@link #toLong_variousInputs_returnsExpectedLong}.
     *
     * @return a stream of {@link Arguments} containing:
     *         - an {@link AtomicBoolean} instance,
     *         - a flag indicating whether the converter argument should be {@code null},
     *         - the expected {@link Long} result.
     */
    private static Stream<Arguments> provideAtomicBooleanAndConverter() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), true, CommonValues.LONG_ONE), Arguments.of(new AtomicBoolean(false), true, CommonValues.LONG_ZERO), Arguments.of(new AtomicBoolean(true), false, CommonValues.LONG_ONE), Arguments.of(new AtomicBoolean(false), false, CommonValues.LONG_ZERO));
    }

    @Test
    void toLong_whenAtomicBooleanTrue_returnsOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Long result = AtomicBooleanConversions.toLong(source, converter);
        // then
        assertEquals(Long.valueOf(1L), result, "AtomicBoolean true should be converted to Long value 1");
    }

    @Test
    void toLong_whenAtomicBooleanFalse_returnsZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Long result = AtomicBooleanConversions.toLong(source, converter);
        // then
        assertEquals(Long.valueOf(0L), result, "AtomicBoolean false should be converted to Long value 0");
    }

    @Test
    void toLong_whenNullSource_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toLong(source, converter), "Passing null should result in a NullPointerException");
    }

    @Test
    void toLong_whenNonAtomicBooleanSource_throwsClassCastException() {
        // given
        Object source = "not an atomic boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toLong(source, converter), "Passing a non‑AtomicBoolean should result in a ClassCastException");
    }

    /**
     * Parameterized test covering the four valid conversion scenarios:
     * <ul>
     *   <li>AtomicBoolean true with a {@code null} converter → 1L</li>
     *   <li>AtomicBoolean false with a {@code null} converter → 0L</li>
     *   <li>AtomicBoolean true with a non‑null converter → 1L</li>
     *   <li>AtomicBoolean false with a non‑null converter → 0L</li>
     * </ul>
     *
     * The non‑null converter is a minimal stub that simply returns the source
     * object; the conversion logic does not depend on it.
     */
    @ParameterizedTest(name = "[{index}] from={0}, converterIsNull={1} => expected={2}")
    @MethodSource("provideAtomicBooleanAndConverter")
    void toLong_variousInputs_returnsExpectedLong(AtomicBoolean from, boolean converterIsNull, Long expected) {
        // given
        Converter usedConverter = converterIsNull ? null : new Converter(new ConverterOptions()) {

            @Override
            public Object convert(Object src, Class<?> target) {
                // Stub implementation – simply returns the source.
                return src;
            }
        };
        // when
        Long result = AtomicBooleanConversions.toLong(from, usedConverter);
        // then
        assertEquals(expected, result, () -> String.format("AtomicBoolean %s with %sconverter should produce %s", from.get(), converterIsNull ? "null " : "non‑null ", expected));
    }
}
