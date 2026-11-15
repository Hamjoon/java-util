package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringConversions_toBoolean_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared Converter instance used by most tests.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – no special configuration needed for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    private static Stream<Arguments> booleanResultProvider() {
        // Helper converter instances
        Converter mockConverter = org.mockito.Mockito.mock(Converter.class);
        Converter dummyConverter = new Converter(new ConverterOptions());
        return Stream.of(// ----- Exact literals -----
        arguments("true", defaultConverter, true), arguments("false", defaultConverter, false), // ----- Case‑insensitive true variants -----
        arguments("TRUE", defaultConverter, true), arguments("TrUe", defaultConverter, true), arguments("t", defaultConverter, true), arguments("1", defaultConverter, true), arguments("y", defaultConverter, true), arguments("\"true\"", defaultConverter, true), // ----- Quoted true variants (different case) -----
        arguments("\"TRUE\"", defaultConverter, true), // ----- Uppercase Y -----
        arguments("Y", defaultConverter, true), // ----- Negative / false cases -----
        arguments("no", defaultConverter, false), // exact "false" handled earlier, this is upper‑case
        arguments("FALSE", defaultConverter, false), arguments("0", defaultConverter, false), // ----- Null and empty / whitespace -----
        arguments(null, defaultConverter, false), arguments("", defaultConverter, false), arguments(" ", defaultConverter, false), arguments("\t\n", defaultConverter, false), // ----- Mixed‑case false variants -----
        arguments("False", defaultConverter, false), arguments("fAlSe", defaultConverter, false), // ----- Numeric non‑true values -----
        arguments("2", defaultConverter, false), arguments("-1", defaultConverter, false), arguments("123", defaultConverter, false), // ----- Other false strings -----
        arguments("yes", defaultConverter, false), arguments("on", defaultConverter, false), arguments("null", defaultConverter, false), arguments("TrueFalse", defaultConverter, false), // ----- Using a custom (mock) converter -----
        arguments("true", mockConverter, true), arguments("true", dummyConverter, true));
    }

    private static Stream<Arguments> exceptionResultProvider() {
        // Non‑String objects that trigger the unchecked cast
        return Stream.of(arguments(123, defaultConverter, ClassCastException.class), arguments(Boolean.TRUE, defaultConverter, ClassCastException.class), arguments(new Object(), null, ClassCastException.class));
    }

    @Test
    void toBoolean_exactTrueString_returnsTrue() {
        assertTrue(StringConversions.toBoolean("true", converter));
    }

    @Test
    void toBoolean_trueIgnoreCase_returnsTrue() {
        assertTrue(StringConversions.toBoolean("TrUe", converter));
    }

    @Test
    void toBoolean_singleLetterT_returnsTrue() {
        assertTrue(StringConversions.toBoolean("t", converter));
    }

    @Test
    void toBoolean_oneDigit_returnsTrue() {
        assertTrue(StringConversions.toBoolean("1", converter));
    }

    @Test
    void toBoolean_letterY_returnsTrue() {
        assertTrue(StringConversions.toBoolean("y", converter));
    }

    @Test
    void toBoolean_quotedTrue_returnsTrue() {
        assertTrue(StringConversions.toBoolean("\"true\"", converter));
    }

    @Test
    void toBoolean_exactFalseString_returnsFalse() {
        assertFalse(StringConversions.toBoolean("false", converter));
    }

    @Test
    void toBoolean_falseIgnoreCase_returnsFalse() {
        assertFalse(StringConversions.toBoolean("FaLsE", converter));
    }

    @Test
    void toBoolean_unrecognisedString_returnsFalse() {
        assertFalse(StringConversions.toBoolean("maybe", converter));
    }

    @Test
    void toBoolean_emptyString_returnsFalse() {
        assertFalse(StringConversions.toBoolean("", converter));
    }

    @Test
    void toBoolean_nullInput_throwsClassCastException() {
        // The implementation casts the Object to String without a null check,
        // so a NullPointerException (actually ClassCastException) is expected.
        assertThrows(ClassCastException.class, () -> StringConversions.toBoolean(null, converter));
    }

    @Test
    void toBoolean_nonStringObject_throwsClassCastException() {
        // Passing a non‑String should also result in a ClassCastException
        assertThrows(ClassCastException.class, () -> StringConversions.toBoolean(123, converter));
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1} => expected={2}")
    @MethodSource("booleanResultProvider")
    void toBoolean_returnsExpectedBoolean(Object from, Converter converter, Boolean expected) {
        Boolean actual = StringConversions.toBoolean(from, converter);
        assertEquals(expected, actual, () -> String.format("toBoolean(%s, %s) should be %s", from, converter, expected));
    }

    @ParameterizedTest(name = "[{index}] from={0} (type={1}) should throw {2}")
    @MethodSource("exceptionResultProvider")
    void toBoolean_throwsExpectedException(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> StringConversions.toBoolean(from, converter), () -> String.format("toBoolean(%s, %s) should throw %s", from, converter, expectedException.getSimpleName()));
    }
}
