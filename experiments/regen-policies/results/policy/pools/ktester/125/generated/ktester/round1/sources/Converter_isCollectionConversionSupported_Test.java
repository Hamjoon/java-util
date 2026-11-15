package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.time.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Converter_isCollectionConversionSupported_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // ConverterOptions is an interface with default methods – an anonymous
        // implementation with no overrides is sufficient for the tests.
        ConverterOptions options = new ConverterOptions() {
        };
        converter = new Converter(options);
    }

    private static Stream<Arguments> provideSupportedScenarios() {
        return Stream.of(// Invalid source types
        Arguments.of(String.class, Integer.class, false), // Target EnumSet throws exception (array source)
        Arguments.of(String[].class, EnumSet.class, IllegalArgumentException.class), // Target EnumSet throws exception (collection source)
        Arguments.of(ArrayList.class, EnumSet.class, IllegalArgumentException.class), // Target is an Enum type – allowed from array or collection
        Arguments.of(String[].class, DayOfWeek.class, true), Arguments.of(HashSet.class, DayOfWeek.class, true), // Source is EnumSet
        Arguments.of(EnumSet.class, String[].class, true), Arguments.of(EnumSet.class, ArrayList.class, true), Arguments.of(EnumSet.class, Integer.class, false), // Source is generic Collection
        Arguments.of(ArrayList.class, Double[].class, true), Arguments.of(LinkedHashSet.class, TreeSet.class, true), Arguments.of(ArrayList.class, Boolean.class, false), // Source is array (primitive int[])
        Arguments.of(int[].class, ArrayList.class, true), Arguments.of(int[].class, Long[].class, true), Arguments.of(int[].class, int[].class, false), Arguments.of(int[].class, String.class, false), // Source array to Enum target
        Arguments.of(DayOfWeek[].class, DayOfWeek.class, true), Arguments.of(String[].class, Month.class, false), // Target EnumSet throws exception with source EnumSet
        Arguments.of(EnumSet.class, EnumSet.class, IllegalArgumentException.class), // Null source type – should NPE
        Arguments.of(null, ArrayList.class, NullPointerException.class), // Null target type – should NPE
        Arguments.of(ArrayList.class, null, NullPointerException.class), // Both null – NPE
        Arguments.of(null, null, NullPointerException.class));
    }

    /* ------------------------------------------------------------
       Parameterized test covering the “happy‑path” boolean results.
       ------------------------------------------------------------ */
    @ParameterizedTest
    @MethodSource("provideSupportedScenarios")
    void isCollectionConversionSupported_returnsExpected(Class<?> source, Class<?> target, boolean expected) {
        // Expected can be Boolean or a Class indicating an exception type.
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // exception expected
            assertThrows((Class<? extends Throwable>) expected, () -> converter.isCollectionConversionSupported(source, target), () -> String.format("Expected %s for source=%s, target=%s", ((Class<?>) expected).getSimpleName(), source, target));
        } else {
            boolean exp = (Boolean) expected;
            assertEquals(exp, converter.isCollectionConversionSupported(source, target), () -> String.format("source=%s, target=%s", source, target));
        }
    }

    /* ------------------------------------------------------------
       Test that requesting conversion to an EnumSet throws the expected
       IllegalArgumentException.
       ------------------------------------------------------------ */
    @Test
    void isCollectionConversionSupported_targetEnumSet_throwsException() {
        // any supported source type
        Class<?> source = Day[].class;
        // direct EnumSet target triggers exception
        Class<?> target = EnumSet.class;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.isCollectionConversionSupported(source, target));
        assertTrue(ex.getMessage().contains("To convert to EnumSet"));
    }

    /* ------------------------------------------------------------
       Additional explicit tests for the three “early‑exit” branches.
       ------------------------------------------------------------ */
    @Test
    void sourceNotArrayCollectionOrEnumSet_returnsFalse() {
        assertFalse(converter.isCollectionConversionSupported(String.class, List.class));
        assertFalse(converter.isCollectionConversionSupported(Integer.class, Integer[].class));
    }

    @Test
    void targetIsEnum_andSourceIsArrayOrCollection_returnsTrue() {
        assertTrue(converter.isCollectionConversionSupported(DayOfWeek[].class, DayOfWeek.class));
        assertTrue(converter.isCollectionConversionSupported(ArrayList.class, DayOfWeek.class));
    }

    @Test
    void sourceIsEnumSet_andTargetIsArrayOrCollection_returnsTrue() {
        assertTrue(converter.isCollectionConversionSupported(EnumSet.class, DayOfWeek[].class));
        assertTrue(converter.isCollectionConversionSupported(EnumSet.class, HashSet.class));
    }

    @Test
    void targetEnumSetAlwaysThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> converter.isCollectionConversionSupported(String[].class, EnumSet.class));
        assertTrue(ex.getMessage().contains("To convert to EnumSet"));
    }
}
