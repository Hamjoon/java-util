package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoidConversions_toNull_Test {

    /**
     * A reusable {@link Converter} instance for the tests.
     */
    private Converter converter;

    /**
     * A reusable {@link Converter} instance for the tests that need a “real” one.
     */
    private Converter realConverter;

    /* --------------------------------------------------------------
     *  Helper class – records whether any of its public methods are called
     * -------------------------------------------------------------- */
    private static class RecordingConverter extends Converter {

        private boolean called = false;

        @Override
        public Object convert(Object src, Class<?> target) {
            called = true;
            return "recorded";
        }

        boolean wasCalled() {
            return called;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The real Converter can be used – its behaviour does not affect the
        // outcome of VoidConversions.toNull().
        realConverter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference; not strictly necessary but keeps the
        // pattern consistent with the template.
        converter = null;
    }

    /* --------------------------------------------------------------
     *  Arguments provider – builds the test matrix from the JSON spec
     * -------------------------------------------------------------- */
    private static Stream<Arguments> toNullArguments() {
        // 1. Both arguments null
        Arguments a1 = Arguments.of(null, null, "Both arguments are null; method should simply return null without throwing.");
        // 2. Non‑null from, null converter
        Arguments a2 = Arguments.of("some string", null, "Non‑null `from` value with a null `converter`; method must ignore the value and return null.");
        // 3. Primitive wrapper as from, dummy converter
        Converter dummyConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return "converted";
            }
        };
        Arguments a3 = Arguments.of(Integer.valueOf(123), dummyConverter, "Primitive wrapper as `from` and a dummy `Converter` implementation; method should still return null and never invoke the converter.");
        // 4. Arbitrary object + Mockito mock
        Converter mockConverter = mock(Converter.class);
        Arguments a4 = Arguments.of(new Object(), mockConverter, "Arbitrary object as `from` and a Mockito mock of `Converter`; verify that toNull returns null and does not call any method on the mock.");
        // 5. Null from + converter that throws on any call
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("should not be called");
            }
        };
        Arguments a5 = Arguments.of(null, throwingConverter, "`from` is null while `converter` is an implementation whose methods throw RuntimeException if invoked; toNull must return null without triggering the exception.");
        // 6. Array object + Mockito mock
        Converter mockConverter2 = mock(Converter.class);
        Arguments a6 = Arguments.of(new int[] { 1, 2, 3 }, mockConverter2, "Array object as `from` with a Mockito mock `Converter`; verify that toNull returns null and does not interact with the mock.");
        // 7. Null from + mock converter
        Converter mockConverter3 = mock(Converter.class);
        Arguments a7 = Arguments.of(null, mockConverter3, "`from` is null while a mock `Converter` is supplied; the method must return null and must not invoke any method on the mock.");
        // 8. Non‑null string + converter that returns a non‑null value
        Converter returningConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return "converted";
            }
        };
        Arguments a8 = Arguments.of("test", returningConverter, "`from` is a non‑null string and the `Converter` implementation returns a non‑null value when its `convert` method is called; toNull must ignore the converter's result and still return null.");
        // 9. Regular object + converter that throws
        Converter exceptionConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("should not be called");
            }
        };
        Arguments a9 = Arguments.of(new Object(), exceptionConverter, "`from` is a regular object and the supplied `Converter` throws an exception if its `convert` method is invoked; toNull must return null without propagating the exception.");
        // 10. StringBuilder + RecordingConverter
        RecordingConverter recordingConverter = new RecordingConverter();
        Arguments a10 = Arguments.of(new StringBuilder("builder"), recordingConverter, "`from` is a `StringBuilder` instance and the `Converter` implementation records whether any of its methods are called; after invoking toNull, the flag must remain false and the method must return null.");
        return Stream.of(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
    }

    @Test
    void toNull_returnsNull_whenFromIsNull() {
        // GIVEN
        Object from = null;
        // WHEN
        Object result = VoidConversions.toNull(from, converter);
        // THEN
        assertNull(result, "VoidConversions.toNull should return null when the source is null");
    }

    @Test
    void toNull_returnsNull_whenFromIsNonNull() {
        // GIVEN
        // any non‑null object
        Object from = new Object();
        // WHEN
        Object result = VoidConversions.toNull(from, converter);
        // THEN
        assertNull(result, "VoidConversions.toNull should return null regardless of the source value");
    }

    @Test
    void toNull_returnsNull_whenConverterIsNull() {
        // GIVEN
        Object from = "any value";
        // WHEN
        Object result = VoidConversions.toNull(from, null);
        // THEN
        assertNull(result, "VoidConversions.toNull should return null even when the Converter argument is null");
    }

    /* --------------------------------------------------------------
     *  Parameterized test covering all JSON‑defined scenarios
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("toNullArguments")
    void toNull_behaviour(Object from, Converter converter, String description) {
        // WHEN
        Object result = VoidConversions.toNull(from, converter);
        // THEN
        assertNull(result, "VoidConversions.toNull must always return null");
        // If a mock or a recording converter was supplied we must ensure that
        // it was never interacted with.
        if (converter != null) {
            if (Mockito.mockingDetails(converter).isMock()) {
                verifyNoInteractions(converter);
            } else if (converter instanceof RecordingConverter) {
                assertFalse(((RecordingConverter) converter).wasCalled(), "RecordingConverter should not have been called");
            }
        }
    }
}
