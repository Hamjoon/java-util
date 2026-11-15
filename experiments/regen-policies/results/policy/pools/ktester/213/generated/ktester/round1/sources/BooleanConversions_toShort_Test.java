package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toShort_Test {

    /**
     * A Converter instance that can be reused for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the default constructor.
        // If a no‑arg constructor is not available, create an empty options object.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    /**
     * Supplies a variety of non‑Boolean objects for the parameterized test above.
     */
    private static Stream<Arguments> nonBooleanInputs() {
        return Stream.of(// Integer
        Arguments.of(42), // String
        Arguments.of("true"), // Plain Object
        Arguments.of(new Object()), // Another String representation
        Arguments.of(Boolean.TRUE.toString()), // Short
        Arguments.of(Short.valueOf((short) 1)));
    }

    @Test
    void toShort_true_returnsOne() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        Short result = BooleanConversions.toShort(source, converter);
        // then
        assertEquals(Short.valueOf((short) 1), result, "Converting Boolean.TRUE should yield Short value 1");
    }

    @Test
    void toShort_false_returnsZero() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        Short result = BooleanConversions.toShort(source, converter);
        // then
        assertEquals(Short.valueOf((short) 0), result, "Converting Boolean.FALSE should yield Short value 0");
    }

    @Test
    void toShort_null_returnsZero() {
        // The method casts the input to Boolean, so passing null would cause a
        // NullPointerException. This test documents that behaviour.
        assertThrows(NullPointerException.class, () -> BooleanConversions.toShort(null, converter));
    }

    @Test
    void toShort_true_withNullConverter_returnsOne() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        Short result = BooleanConversions.toShort(source, null);
        // then
        assertEquals(Short.valueOf((short) 1), result, "Converter argument is ignored; true should still map to 1");
    }

    @Test
    void toShort_false_withNullConverter_returnsZero() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        Short result = BooleanConversions.toShort(source, null);
        // then
        assertEquals(Short.valueOf((short) 0), result, "Converter argument is ignored; false should still map to 0");
    }

    @Test
    void toShort_true_withMockConverter_returnsOne() {
        // given
        Boolean source = Boolean.TRUE;
        Converter mockConverter = Mockito.mock(Converter.class);
        // when
        Short result = BooleanConversions.toShort(source, mockConverter);
        // then
        assertEquals(Short.valueOf((short) 1), result, "Even a mocked Converter must not affect the result");
        // Verify that the mock was never interacted with (method is ignored).
        Mockito.verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("nonBooleanInputs")
    void toShort_nonBooleanInput_throwsClassCastException(Object nonBoolean) {
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toShort(nonBoolean, converter), "Casting a non‑Boolean (" + nonBoolean + ") must throw ClassCastException");
    }
}
