package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Converter_isSimpleTypeConversionSupported_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use a minimal, empty options implementation – the defaults are sufficient
        ConverterOptions options = new ConverterOptions() {

            @Override
            public <T> T getCustomOption(String name) {
                return null;
            }
        };
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test
        converter = null;
    }

    /**
     * Provides a matrix of source/target pairs and the expected result.
     */
    private static Stream<Arguments> simpleTypeSupportProvider() {
        return Stream.of(// 1. Array source – always false
        Arguments.of(int[].class, String.class, false), // 2. Collection source – always false
        Arguments.of(List.class, String.class, false), // 3. Collection target – always false
        Arguments.of(String.class, Set.class, false), // 4. Number.class special case – supported when target is Long (built‑in conversion)
        Arguments.of(Number.class, Long.class, true), // 5. Number.class special case – unsupported for unrelated target
        Arguments.of(Number.class, String.class, false), // 6. Direct conversion present in the built‑in map (String → Integer)
        Arguments.of(String.class, Integer.class, true), // 7. Direct conversion present (Boolean → String)
        Arguments.of(Boolean.class, String.class, true), // 8. Inherited conversion – Integer extends Number, conversion to Number is supported
        Arguments.of(Integer.class, Number.class, true), // 9. Inherited conversion – Enum subclass to its superclass (DayOfWeek → Enum)
        Arguments.of(java.time.DayOfWeek.class, Enum.class, true), // 10. No conversion – unrelated types
        Arguments.of(UUID.class, Calendar.class, false));
    }

    /**
     * Provides the full matrix of source/target pairs that return a boolean result.
     */
    private static Stream<Arguments> booleanSupportProvider() {
        return Stream.of(// -----------------------------------------------------------------
        // 1. Array / Collection handling – always false
        // -----------------------------------------------------------------
        // array source
        Arguments.of(int[].class, String.class, false), // collection source
        Arguments.of(List.class, String.class, false), // array target
        Arguments.of(String.class, int[].class, false), // collection target
        Arguments.of(String.class, Set.class, false), // -----------------------------------------------------------------
        // 2. Number.class special handling
        // -----------------------------------------------------------------
        // Number → Long (direct map)
        Arguments.of(Number.class, Long.class, true), // Number → String (via Long→String)
        Arguments.of(Number.class, String.class, true), // Number → Object (unsupported)
        Arguments.of(Number.class, Object.class, false), // -----------------------------------------------------------------
        // 3. Direct conversions present in the built‑in map
        // -----------------------------------------------------------------
        // String → Integer
        Arguments.of(String.class, Integer.class, true), // Boolean → String
        Arguments.of(Boolean.class, String.class, true), // -----------------------------------------------------------------
        // 4. Inherited conversions (sub‑class → super‑class or via super‑class)
        // -----------------------------------------------------------------
        // Integer extends Number
        Arguments.of(Integer.class, Number.class, true), // Integer → String via Number→String
        Arguments.of(Integer.class, String.class, true), // Enum subclass → Enum
        Arguments.of(java.time.DayOfWeek.class, Enum.class, true), // LocalDate → Date (inheritance path)
        Arguments.of(java.time.LocalDate.class, Date.class, true), // identity
        Arguments.of(java.time.LocalDateTime.class, java.time.LocalDateTime.class, true), // -----------------------------------------------------------------
        // 5. Identity conversion (source equals target)
        // -----------------------------------------------------------------
        Arguments.of(String.class, String.class, true), Arguments.of(java.time.LocalDateTime.class, java.time.LocalDateTime.class, true), // -----------------------------------------------------------------
        // 6. Primitive ↔ wrapper conversions
        // -----------------------------------------------------------------
        // primitive → wrapper
        Arguments.of(int.class, Integer.class, true), // wrapper → primitive
        Arguments.of(Integer.class, int.class, true), // primitive boolean → String
        Arguments.of(boolean.class, String.class, true), // -----------------------------------------------------------------
        // 7. Unsupported primitive to collection
        // -----------------------------------------------------------------
        Arguments.of(int.class, List.class, false), // -----------------------------------------------------------------
        // 8. Unrelated types – should be false
        // -----------------------------------------------------------------
        Arguments.of(UUID.class, Calendar.class, false), Arguments.of(String.class, Thread.class, false), Arguments.of(java.time.Instant.class, java.math.BigDecimal.class, false));
    }

    @ParameterizedTest
    @MethodSource("simpleTypeSupportProvider")
    void isSimpleTypeConversionSupported(Class<?> source, Class<?> target, boolean expected) {
        assertEquals(expected, converter.isSimpleTypeConversionSupported(source, target), () -> String.format("source=%s, target=%s", source.getName(), target.getName()));
    }

    /**
     * Parameterized test that verifies the boolean result of
     * {@link Converter#isSimpleTypeConversionSupported(Class, Class)} for all
     * non‑exception test cases.
     */
    @ParameterizedTest(name = "[{index}] {0} → {1} = {2}")
    @MethodSource("booleanSupportProvider")
    void isSimpleTypeConversionSupported_boolean(Class<?> source, Class<?> target, boolean expected) {
        assertEquals(expected, converter.isSimpleTypeConversionSupported(source, target), () -> String.format("source=%s, target=%s", source, target));
    }

    /**
     * Verifies that a {@link NullPointerException} is thrown when either
     * {@code source} or {@code target} is {@code null}.
     */
    @Test
    void isSimpleTypeConversionSupported_nullSource_throwsNPE() {
        assertThrows(NullPointerException.class, () -> converter.isSimpleTypeConversionSupported(null, String.class), "Expected NullPointerException when source is null");
    }

    @Test
    void isSimpleTypeConversionSupported_nullTarget_throwsNPE() {
        assertThrows(NullPointerException.class, () -> converter.isSimpleTypeConversionSupported(String.class, null), "Expected NullPointerException when target is null");
    }
}
