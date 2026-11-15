package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class VoidConversions_toCharacter_Test {

    /**
     * Supplies all test cases for {@link #toCharacter_alwaysReturnsMinValue}.
     *
     * <p>Each {@link Arguments} entry mirrors one entry from the provided JSON.
     * New mocks are created inline so that every case gets its own isolated mock
     * instance.</p>
     */
    static Stream<Arguments> provideToCharacterCases() {
        return Stream.of(// 1. Both arguments are null
        Arguments.of("Both arguments are null – the method should ignore them and always return Character.MIN_VALUE.", null, null), // 2. Non‑null String, null converter
        Arguments.of("Non‑null `from` (String) with a null converter – still returns Character.MIN_VALUE.", "some string", null), // 3. Integer source, null converter
        Arguments.of("`from` is an Integer; the implementation does not inspect the value, so the result is Character.MIN_VALUE.", 12345, null), // 4. Generic Object, null converter
        Arguments.of("`from` is a generic Object instance; method should still return Character.MIN_VALUE.", new Object(), null), // 5. Null source, mock converter
        Arguments.of("Null `from` with a non‑null Converter mock – the converter is never invoked, result is Character.MIN_VALUE.", null, mock(Converter.class)), // 6. Non‑null source, mock converter
        Arguments.of("Non‑null `from` and a Converter mock – method does not call the converter and returns Character.MIN_VALUE.", "test", mock(Converter.class)), // 7. StringBuilder source, null converter
        Arguments.of("`from` is a StringBuilder instance and `converter` is null – the method should still return Character.MIN_VALUE.", new StringBuilder("abc"), null), // 8. StringBuilder source, mock converter
        Arguments.of("`from` is a StringBuilder and a Converter mock – the method must not invoke the converter and must return Character.MIN_VALUE.", new StringBuilder("abc"), mock(Converter.class)), // 9. Character object source, null converter
        Arguments.of("`from` is a Character object; `converter` is null – result is still Character.MIN_VALUE.", Character.valueOf('A'), null), // 10. Boolean.TRUE source, mock converter
        Arguments.of("`from` is Boolean.TRUE and the converter mock would throw if called – the method does not call the converter and returns Character.MIN_VALUE.", Boolean.TRUE, mock(Converter.class)), // 11. int array source, null converter
        Arguments.of("`from` is an int array; `converter` is null – the method returns Character.MIN_VALUE.", new int[] { 1, 2, 3 }, null), // 12. Object array source, mock converter
        Arguments.of("`from` is an Object array; a Converter mock is supplied – the method ignores the converter and returns Character.MIN_VALUE.", new Object[] { new Object() }, mock(Converter.class)), // 13. Empty List source, null converter
        Arguments.of("`from` is an empty List; `converter` is null – still returns Character.MIN_VALUE.", Collections.emptyList(), null), // 14. Empty Map source, mock converter
        Arguments.of("`from` is an empty Map; a Converter mock is provided – method must not call the converter and must return Character.MIN_VALUE.", new HashMap<>(), mock(Converter.class)));
    }

    /**
     * Verify that {@code toCharacter} returns {@code Character.MIN_VALUE}
     * for a {@code null} source and a {@code null} converter.
     */
    @Test
    void toCharacter_returnsMinValue_whenSourceAndConverterAreNull() {
        // given
        Object source = null;
        Converter converter = null;
        // when
        Character result = VoidConversions.toCharacter(source, converter);
        // then
        assertEquals(Character.MIN_VALUE, result);
    }

    /**
     * Verify that {@code toCharacter} returns {@code Character.MIN_VALUE}
     * for a non‑null source and a real {@link Converter} instance.
     */
    @Test
    void toCharacter_returnsMinValue_forNonNullSourceAndConverter() {
        // given
        Object source = "any value";
        // default options
        ConverterOptions options = new ConverterOptions();
        Converter converter = new Converter(options);
        // when
        Character result = VoidConversions.toCharacter(source, converter);
        // then
        assertEquals(Character.MIN_VALUE, result);
    }

    /**
     * Verify that {@code toCharacter} returns {@code Character.MIN_VALUE}
     * when a mock {@link Converter} is supplied.
     */
    @Test
    void toCharacter_returnsMinValue_withMockConverter() {
        // given
        Object source = 123;
        Converter mockConverter = mock(Converter.class);
        // when
        Character result = VoidConversions.toCharacter(source, mockConverter);
        // then
        assertEquals(Character.MIN_VALUE, result);
        // No interaction with the mock is expected because the method does not use it.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Parameterized test that covers every scenario described in the JSON
     * test‑case list.  Each argument tuple consists of:
     *
     * <ul>
     *   <li>a human‑readable description (used as the test name),</li>
     *   <li>the {@code from} argument (any object or {@code null}),</li>
     *   <li>the {@link Converter} argument (either {@code null} or a Mockito mock).</li>
     * </ul>
     *
     * The method under test must always return {@link Character#MIN_VALUE}
     * and must never invoke the supplied {@link Converter}.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideToCharacterCases")
    void toCharacter_alwaysReturnsMinValue(String description, Object from, Converter converter) {
        // when
        Character result = VoidConversions.toCharacter(from, converter);
        // then
        assertEquals(Character.MIN_VALUE, result, () -> "Expected Character.MIN_VALUE for case: " + description);
        // If a mock converter was supplied we assert that it was never used.
        if (converter != null && mockingDetails(converter).isMock()) {
            verifyNoInteractions(converter);
        }
    }
}
