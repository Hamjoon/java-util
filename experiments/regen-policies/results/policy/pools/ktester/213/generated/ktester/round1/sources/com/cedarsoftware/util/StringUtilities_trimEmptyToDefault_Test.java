package com.cedarsoftware.util;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
// <-- added import for assertions
import org.junit.jupiter.api.Assertions;

// import static org.junit.Assert.*;
@ExtendWith(MockitoExtension.class)
class StringUtilities_trimEmptyToDefault_Test {

    /**
     * Test data: each element is {value, defaultValue, expectedResult}.
     */
    private static final Object[][] TEST_DATA = { { null, "default", "default" }, { null, "", "" }, { null, null, null }, { "", "default", "default" }, { "", "", "" }, { "", null, null }, { "   ", "default", "default" }, { "\t\n\r", "fallback", "fallback" }, { " \t ", "", "" }, { "   ", null, null }, { "  abc  ", "default", "abc" }, { "\txyz\n", "fallback", "xyz" }, { "  hello  ", "fallback", "hello" }, { "noTrimNeeded", "ignored", "noTrimNeeded" }, { "   ", "  kept  ", "  kept  " }, { null, "  kept  ", "  kept  " }, { null, "", "" }, { null, null, null } };

    /**
     * Provides a matrix of inputs (value, defaultValue) and the expected result.
     */
    private static Stream<Arguments> trimEmptyToDefaultProvider() {
        return Stream.of(arguments(null, "default", "default"), arguments(null, "", ""), arguments(null, null, null), arguments("", "default", "default"), arguments("", "", ""), arguments("", null, null), arguments("   ", "default", "default"), arguments("\t\n\r", "fallback", "fallback"), arguments(" \t ", "", ""), arguments("   ", null, null), arguments("  abc  ", "default", "abc"), arguments("\txyz\n", "fallback", "xyz"), arguments("  hello  ", "fallback", "hello"), arguments("noTrimNeeded", "ignored", "noTrimNeeded"), arguments("   ", "  kept  ", "  kept  "), arguments(null, "  kept  ", "  kept  "), arguments(null, "", ""), arguments(null, null, null));
    }

    @ParameterizedTest
    @MethodSource("trimEmptyToDefaultProvider")
    void testTrimEmptyToDefault(String value, String defaultValue, String expected) {
        String result = StringUtilities.trimEmptyToDefault(value, defaultValue);
        String message = String.format("trimEmptyToDefault(%s, %s) expected <%s> but got <%s>", value, defaultValue, expected, result);
        Assertions.assertEquals(expected, result, message);
    }
}
