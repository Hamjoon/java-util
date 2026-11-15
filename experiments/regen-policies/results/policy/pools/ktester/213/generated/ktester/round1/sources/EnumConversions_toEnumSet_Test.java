package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EnumConversions_toEnumSet_Test {

    /**
     * Provides arguments for successful conversions (collection or array).
     */
    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// collection of two distinct enum values
        Arguments.of(Arrays.asList(Color.RED, Color.BLUE), Color.class, EnumSet.of(Color.RED, Color.BLUE), "Valid enum target with collection of enum values"), // array of two distinct enum values
        Arguments.of(new Color[] { Color.GREEN, Color.BLUE }, Color.class, EnumSet.of(Color.GREEN, Color.BLUE), "Valid enum target with array of enum values"), // empty collection
        Arguments.of(Collections.emptyList(), Color.class, EnumSet.noneOf(Color.class), "Valid enum target with empty collection"), // empty array
        Arguments.of(new Color[] {}, Color.class, EnumSet.noneOf(Color.class), "Valid enum target with empty array"), // collection with duplicate values
        Arguments.of(Arrays.asList(Color.RED, Color.RED, Color.BLUE), Color.class, EnumSet.of(Color.RED, Color.BLUE), "Collection with duplicate enum values"));
    }

    /**
     * Provides arguments for conversions that must fail with a specific exception type.
     */
    private static Stream<Arguments> failureConversionProvider() {
        return Stream.of(// invalid source type (single enum constant)
        Arguments.of(Color.RED, Color.class, IllegalArgumentException.class, "Source must be a Collection or Array", "Invalid source type (single enum, not collection/array)"), // null source argument
        Arguments.of(null, Color.class, NullPointerException.class, // message not asserted for NPE
        null, "Null source argument"), // null target argument
        Arguments.of(Collections.emptyList(), null, NullPointerException.class, null, "Null target argument"), // collection containing null element
        Arguments.of(Arrays.asList(Color.RED, null, Color.BLUE), Color.class, NullPointerException.class, null, "Collection containing null element"), // collection containing non‑enum element
        Arguments.of(Arrays.asList(Color.RED, "GREEN", Color.BLUE), Color.class, ClassCastException.class, null, "Collection containing non‑enum element"), // array containing non‑enum element
        Arguments.of(new Object[] { Color.RED, "GREEN", Color.BLUE }, Color.class, ClassCastException.class, null, "Array containing non‑enum element"), // array containing null element
        Arguments.of(new Color[] { Color.RED, null, Color.BLUE }, Color.class, NullPointerException.class, null, "Array containing null element"), // primitive array source (int[])
        Arguments.of(new int[] { 1, 2, 3 }, Color.class, IllegalArgumentException.class, "Source must be a Collection or Array", "Primitive array source (int[])"));
    }

    @Test
    void toEnumSet_FromCollection_ReturnsEnumSet() {
        // GIVEN
        Collection<Day> source = Arrays.asList(Day.MON, Day.WED, Day.FRI);
        // WHEN
        EnumSet<Day> result = EnumConversions.toEnumSet(source, Day.class);
        // THEN
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsAll(source));
    }

    @Test
    void toEnumSet_FromArray_ReturnsEnumSet() {
        // GIVEN
        Day[] source = new Day[] { Day.TUE, Day.THU, Day.SAT };
        // WHEN
        EnumSet<Day> result = EnumConversions.toEnumSet(source, Day.class);
        // THEN
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsAll(Arrays.asList(source)));
    }

    @Test
    void toEnumSet_TargetNotEnum_ThrowsIllegalArgumentException() {
        // GIVEN
        Collection<String> source = List.of("A", "B");
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EnumConversions.toEnumSet(source, String.class));
        assertTrue(ex.getMessage().contains("must be an Enum"));
    }

    @Test
    void toEnumSet_SourceNotCollectionOrArray_ThrowsIllegalArgumentException() {
        // GIVEN
        Object source = "not a collection or array";
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EnumConversions.toEnumSet(source, Day.class));
        assertTrue(ex.getMessage().contains("Source must be a Collection or Array"));
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("successfulConversionProvider")
    void toEnumSet_SuccessfulConversions(Object from, Class<?> target, EnumSet<?> expected, String description) {
        // WHEN
        EnumSet<?> result = EnumConversions.toEnumSet(from, target);
        // THEN
        assertNotNull(result, description);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{5}")
    @MethodSource("failureConversionProvider")
    void toEnumSet_FailureScenarios(Object from, Class<?> target, Class<? extends Throwable> expectedException, String expectedMessageFragment, String description) {
        // WHEN / THEN
        Throwable ex = assertThrows(expectedException, () -> EnumConversions.toEnumSet(from, target), description);
        // If a message fragment is supplied, verify it appears in the exception message.
        if (expectedMessageFragment != null) {
            assertTrue(ex.getMessage().contains(expectedMessageFragment), () -> "Expected message to contain [" + expectedMessageFragment + "] but was [" + ex.getMessage() + "]");
        }
    }
}
