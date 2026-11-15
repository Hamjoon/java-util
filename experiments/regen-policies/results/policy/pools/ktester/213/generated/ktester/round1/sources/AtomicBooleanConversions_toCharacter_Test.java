package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toCharacter_Test {

    @Mock
    private Converter // mocked Converter
    mockConverter;

    @Mock
    private ConverterOptions // mocked ConverterOptions
    mockOptions;

    /**
     * Simple {@link Converter} implementation that returns a fixed {@link ConverterOptions}.
     */
    private static class TestConverter implements Converter {

        private final ConverterOptions options;

        TestConverter() {
            this('T', 'F');
        }

        TestConverter(char trueChar, char falseChar) {
            this.options = new TestOptions(trueChar, falseChar);
        }

        TestConverter(ConverterOptions options) {
            this.options = options;
        }

        @Override
        public ConverterOptions getOptions() {
            return options;
        }
    }

    /**
     * Simple {@link ConverterOptions} implementation used by {@link TestConverter}.
     */
    private static class TestOptions implements ConverterOptions {

        private final char trueChar;

        private final char falseChar;

        TestOptions(char trueChar, char falseChar) {
            this.trueChar = trueChar;
            this.falseChar = falseChar;
        }

        @Override
        public char trueChar() {
            return trueChar;
        }

        @Override
        public char falseChar() {
            return falseChar;
        }
    }

    /**
     * Converter whose {@code getOptions()} throws an {@link IllegalStateException}.
     */
    private static class TestConverterThrowingGetOptions implements Converter {

        @Override
        public ConverterOptions getOptions() {
            throw new IllegalStateException("Unable to obtain options");
        }
    }

    /**
     * Converter whose {@code trueChar()} throws an {@link IllegalArgumentException}.
     */
    private static class TestConverterThrowingTrueChar implements Converter {

        private final ConverterOptions options = new ConverterOptions() {

            @Override
            public char trueChar() {
                throw new IllegalArgumentException("trueChar failure");
            }

            @Override
            public char falseChar() {
                return 'F';
            }
        };

        @Override
        public ConverterOptions getOptions() {
            return options;
        }
    }

    /**
     * Converter whose {@code falseChar()} throws an {@link IllegalArgumentException}.
     */
    private static class TestConverterThrowingFalseChar implements Converter {

        private final ConverterOptions options = new ConverterOptions() {

            @Override
            public char trueChar() {
                return 'T';
            }

            @Override
            public char falseChar() {
                throw new IllegalArgumentException("falseChar failure");
            }
        };

        @Override
        public ConverterOptions getOptions() {
            return options;
        }
    }

    /**
     * Initialise Mockito annotations before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        MockitoAnnotations.openMocks(this);
        // default stub: true -> 'T', false -> 'F'
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.trueChar()).thenReturn('T');
        when(mockOptions.falseChar()).thenReturn('F');
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// true -> 'T' (default options)
        Arguments.of(new AtomicBoolean(true), new TestConverter('T', 'F'), 'T'), // false -> 'N' (custom options)
        Arguments.of(new AtomicBoolean(false), new TestConverter('Y', 'N'), 'N'));
    }

    @Test
    void toCharacter_returnsTrueChar_whenAtomicBooleanIsTrue() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(true);
        // (stubbing already performed in setupBeforeEach)
        // WHEN
        Character result = AtomicBooleanConversions.toCharacter(source, mockConverter);
        // THEN
        assertEquals('T', result);
    }

    @Test
    void toCharacter_returnsFalseChar_whenAtomicBooleanIsFalse() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(false);
        // (stubbing already performed in setupBeforeEach)
        // WHEN
        Character result = AtomicBooleanConversions.toCharacter(source, mockConverter);
        // THEN
        assertEquals('F', result);
    }

    @Test
    void toCharacter_usesCustomTrueAndFalseChars() {
        // GIVEN
        when(mockOptions.trueChar()).thenReturn('Y');
        when(mockOptions.falseChar()).thenReturn('N');
        AtomicBoolean trueSource = new AtomicBoolean(true);
        AtomicBoolean falseSource = new AtomicBoolean(false);
        // WHEN
        Character trueResult = AtomicBooleanConversions.toCharacter(trueSource, mockConverter);
        Character falseResult = AtomicBooleanConversions.toCharacter(falseSource, mockConverter);
        // THEN
        assertEquals('Y', trueResult);
        assertEquals('N', falseResult);
    }

    @Test
    void toCharacter_throwsClassCastException_whenSourceIsNotAtomicBoolean() {
        // GIVEN
        Object notAtomic = "not an atomic boolean";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toCharacter(notAtomic, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] from={0}, expected={2}")
    @MethodSource("validConversionProvider")
    void toCharacter_validConversions(AtomicBoolean from, Converter converter, char expected) {
        // WHEN
        Character result = AtomicBooleanConversions.toCharacter(from, converter);
        // THEN
        assertEquals(expected, result);
    }

    @Test
    void toCharacter_throwsClassCastException_whenSourceIsNotAtomicBoolean_2() {
        // GIVEN
        // java.lang.Boolean, not AtomicBoolean
        Object notAtomic = Boolean.TRUE;
        Converter converter = new TestConverter('1', '0');
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toCharacter(notAtomic, converter));
    }

    @Test
    void toCharacter_throwsNullPointerException_whenSourceIsNull() {
        // GIVEN
        Converter converter = new TestConverter('A', 'B');
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toCharacter(null, converter));
    }

    @Test
    void toCharacter_throwsNullPointerException_whenConverterIsNull() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(true);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toCharacter(source, null));
    }

    @Test
    void toCharacter_throwsNullPointerException_whenConverterOptionsIsNull() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(false);
        // options == null
        Converter converter = new TestConverter((ConverterOptions) null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toCharacter(source, converter));
    }

    @Test
    void toCharacter_returnsMaxUnicodeChar_whenTrueCharIsMax() {
        // GIVEN
        char max = '\uFFFF';
        Converter converter = new TestConverter(max, '\u0000');
        AtomicBoolean source = new AtomicBoolean(true);
        // WHEN
        Character result = AtomicBooleanConversions.toCharacter(source, converter);
        // THEN
        assertEquals(max, result);
    }

    @Test
    void toCharacter_returnsMinUnicodeChar_whenFalseCharIsMin() {
        // GIVEN
        char min = '\u0000';
        Converter converter = new TestConverter('\uFFFF', min);
        AtomicBoolean source = new AtomicBoolean(false);
        // WHEN
        Character result = AtomicBooleanConversions.toCharacter(source, converter);
        // THEN
        assertEquals(min, result);
    }

    @Test
    void toCharacter_returnsSameChar_whenTrueAndFalseCharsIdentical_trueCase() {
        // GIVEN
        Converter converter = new TestConverter('X', 'X');
        AtomicBoolean source = new AtomicBoolean(true);
        // WHEN
        Character result = AtomicBooleanConversions.toCharacter(source, converter);
        // THEN
        assertEquals('X', result);
    }

    @Test
    void toCharacter_returnsSameChar_whenTrueAndFalseCharsIdentical_falseCase() {
        // GIVEN
        Converter converter = new TestConverter('X', 'X');
        AtomicBoolean source = new AtomicBoolean(false);
        // WHEN
        Character result = AtomicBooleanConversions.toCharacter(source, converter);
        // THEN
        assertEquals('X', result);
    }

    @Test
    void toCharacter_doesNotMutateAtomicBoolean_trueCase() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(true);
        Converter converter = new TestConverter('T', 'F');
        // WHEN
        AtomicBooleanConversions.toCharacter(source, converter);
        // THEN
        assertTrue(source.get(), "Source AtomicBoolean must remain true after conversion");
    }

    @Test
    void toCharacter_doesNotMutateAtomicBoolean_falseCase() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(false);
        Converter converter = new TestConverter('T', 'F');
        // WHEN
        AtomicBooleanConversions.toCharacter(source, converter);
        // THEN
        assertFalse(source.get(), "Source AtomicBoolean must remain false after conversion");
    }

    @Test
    void toCharacter_defaultOptions_trueMapsToT() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(true);
        // defaults to 'T'/'F'
        Converter converter = new TestConverter();
        // WHEN
        Character result = AtomicBooleanConversions.toCharacter(source, converter);
        // THEN
        assertEquals('T', result);
    }

    @Test
    void toCharacter_defaultOptions_falseMapsToF() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(false);
        // defaults to 'T'/'F'
        Converter converter = new TestConverter();
        // WHEN
        Character result = AtomicBooleanConversions.toCharacter(source, converter);
        // THEN
        assertEquals('F', result);
    }

    @Test
    void toCharacter_propagatesException_whenGetOptionsThrows() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(true);
        Converter converter = new TestConverterThrowingGetOptions();
        // WHEN / THEN
        assertThrows(IllegalStateException.class, () -> AtomicBooleanConversions.toCharacter(source, converter));
    }

    @Test
    void toCharacter_propagatesException_whenTrueCharThrows() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(false);
        Converter converter = new TestConverterThrowingTrueChar();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> AtomicBooleanConversions.toCharacter(source, converter));
    }

    @Test
    void toCharacter_propagatesException_whenFalseCharThrows() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(true);
        Converter converter = new TestConverterThrowingFalseChar();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> AtomicBooleanConversions.toCharacter(source, converter));
    }
}
