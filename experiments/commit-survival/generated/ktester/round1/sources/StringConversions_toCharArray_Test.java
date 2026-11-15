package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.ArrayUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static com.cedarsoftware.util.ArrayUtilities.EMPTY_CHAR_ARRAY;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toCharArray_Test {

    @Mock
    Converter // Not used by the method but required for the signature
    mockConverter;

    /* --------------------------------------------------------------
     *  Method sources for successful cases
     * -------------------------------------------------------------- */
    private static Stream<Arguments> successfulCases() {
        return Stream.of(// Non‑empty inputs
        Arguments.of("abc", null, "abc".toCharArray(), "A plain non‑empty String should be converted to a char array containing the same characters."), Arguments.of(new StringBuilder("XYZ"), null, "XYZ".toCharArray(), "StringBuilder's toString() returns a non‑empty string; conversion must match characters."), Arguments.of(new Object() {

            @Override
            public String toString() {
                return "  space  ";
            }
        }, null, "  space  ".toCharArray(), "Custom object returning whitespace‑filled string must preserve all spaces."), // Empty inputs
        Arguments.of("", null, EMPTY_CHAR_ARRAY, "Empty string must trigger EMPTY_CHAR_ARRAY branch."), Arguments.of(new Object() {

            @Override
            public String toString() {
                return "";
            }
        }, null, EMPTY_CHAR_ARRAY, "Object whose toString() returns empty string must return EMPTY_CHAR_ARRAY."), // Primitive wrapper inputs
        Arguments.of(Integer.valueOf(42), null, "42".toCharArray(), "Integer.toString() yields \"42\"; expect char array {'4','2'}."), Arguments.of(Boolean.TRUE, null, "true".toCharArray(), "Boolean.TRUE.toString() yields \"true\"; verify characters."), // Object with null toString result
        Arguments.of(new Object() {

            @Override
            public String toString() {
                return null;
            }
        }, null, EMPTY_CHAR_ARRAY, "toString() returning null is treated as empty; must return EMPTY_CHAR_ARRAY."), // Unicode and special characters
        Arguments.of("éâΩ\uD83D\uDE00", null, "éâΩ\uD83D\uDE00".toCharArray(), "String contains accented, Greek, and emoji surrogate pair characters."), Arguments.of("a\u0000b", null, "a\u0000b".toCharArray(), "String includes an embedded null character; it must be preserved."), // Converter argument usage (converter is non‑null but ignored)
        Arguments.of("test", new Converter() {

            @Override
            public <T> T convert(Object src, Class<T> target) {
                // dummy implementation
                return null;
            }
        }, "test".toCharArray(), "Non‑null Converter must not affect conversion result."), // Single‑character and whitespace variations
        Arguments.of("Z", null, "Z".toCharArray(), "Single‑character string should produce a length‑1 array."), Arguments.of(" ", null, " ".toCharArray(), "Single space is non‑empty; array must contain that space."), Arguments.of("\t\n\r", null, "\t\n\r".toCharArray(), "Tab, newline and carriage‑return characters must be preserved."));
    }

    /* --------------------------------------------------------------
     *  Method sources for exception‑propagation cases
     * -------------------------------------------------------------- */
    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// Null input handling
        Arguments.of(null, null, NullPointerException.class, "Passing null as `from` should cause NPE when toString() is invoked."), // toString() throws IllegalArgumentException
        Arguments.of(new Object() {

            @Override
            public String toString() {
                throw new IllegalArgumentException("bad");
            }
        }, null, IllegalArgumentException.class, "toString() throwing IllegalArgumentException must be propagated."), // toString() throws generic RuntimeException
        Arguments.of(new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("boom");
            }
        }, null, RuntimeException.class, "toString() throwing RuntimeException must be propagated."));
    }

    @Test
    void toCharArray_returnsCharArrayForNonEmptyString() {
        // GIVEN
        String source = "HelloWorld";
        // WHEN
        char[] result = StringConversions.toCharArray(source, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertArrayEquals(source.toCharArray(), result, "The returned char array must match the source string's characters");
    }

    @Test
    void toCharArray_returnsEmptyCharArrayWhenStringIsEmpty() {
        // GIVEN
        String source = "";
        // WHEN
        char[] result = StringConversions.toCharArray(source, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertSame(EMPTY_CHAR_ARRAY, result, "An empty source string must return the shared EMPTY_CHAR_ARRAY constant");
    }

    @Test
    void toCharArray_usesObjectToStringWhenObjectIsNotString() {
        // GIVEN
        Object source = new Object() {

            @Override
            public String toString() {
                return "ObjectString";
            }
        };
        // WHEN
        char[] result = StringConversions.toCharArray(source, mockConverter);
        // THEN
        assertArrayEquals("ObjectString".toCharArray(), result);
    }

    @Test
    void toCharArray_throwsNullPointerExceptionWhenSourceIsNull() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toCharArray(source, mockConverter), "Calling toCharArray with a null source should throw NPE");
    }

    /* --------------------------------------------------------------
     *  Successful conversion test cases (return a char[])
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("successfulCases")
    void toCharArray_success(Object from, Converter converter, char[] expected, String description) {
        // WHEN
        char[] result = StringConversions.toCharArray(from, converter);
        // THEN
        assertNotNull(result, "Result must never be null");
        if (expected == EMPTY_CHAR_ARRAY) {
            // For empty‑string scenarios we must verify the *reference* equality
            assertSame(EMPTY_CHAR_ARRAY, result, "Empty input must return the shared EMPTY_CHAR_ARRAY constant");
        } else {
            assertArrayEquals(expected, result, "The char array does not match the expected characters");
        }
    }

    /* --------------------------------------------------------------
     *  Exception‑propagation test cases
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionCases")
    void toCharArray_exceptions(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> StringConversions.toCharArray(from, converter), "Expected exception was not thrown");
    }
}
