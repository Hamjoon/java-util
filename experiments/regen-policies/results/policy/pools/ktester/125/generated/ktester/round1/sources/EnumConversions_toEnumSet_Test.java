package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EnumConversions_toEnumSet_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The real Converter is required because EnumConversions delegates element
        // conversion to it (e.g., converting a String or an Integer to the Enum).
        converter = new Converter(new ConverterOptions());
    }

    @Test
    void shouldConvertArrayContainingEnumStringAndOrdinalToEnumSet() {
        // given
        Object[] source = { Day.MONDAY, "WEDNESDAY", 4, null };
        // 4 is the ordinal of FRIDAY
        // when
        @SuppressWarnings("unchecked")
        EnumSet<Day> result = EnumConversions.toEnumSet(source, converter, Day.class);
        // then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsAll(EnumSet.of(Day.MONDAY, Day.WEDNESDAY, Day.FRIDAY)));
    }

    @Test
    void shouldConvertCollectionContainingStringAndEnumToEnumSetSkippingNulls() {
        // given
        List<Object> source = Arrays.asList("TUESDAY", Day.SUNDAY, null, "FRIDAY");
        // when
        @SuppressWarnings("unchecked")
        EnumSet<Day> result = EnumConversions.toEnumSet(source, converter, Day.class);
        // then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsAll(EnumSet.of(Day.TUESDAY, Day.FRIDAY, Day.SUNDAY)));
    }

    @Test
    void shouldThrowWhenTargetIsNotAnEnum() {
        // given
        Object[] source = { "MONDAY" };
        Class<?> notAnEnum = String.class;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EnumConversions.toEnumSet(source, converter, notAnEnum));
        assertTrue(ex.getMessage().contains("must be an Enum"));
    }

    @Test
    void shouldThrowWhenSourceIsNeitherCollectionNorArray() {
        // given
        String source = "MONDAY";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EnumConversions.toEnumSet(source, converter, Day.class));
        assertTrue(ex.getMessage().contains("Source must be a Collection or Array"));
    }

    @Test
    void shouldConvertCollectionOfEnumInstances() {
        // given
        List<Color> source = Arrays.asList(Color.RED, Color.BLUE);
        // when
        @SuppressWarnings("unchecked")
        EnumSet<Color> result = EnumConversions.toEnumSet(source, converter, Color.class);
        // then
        assertEquals(EnumSet.of(Color.RED, Color.BLUE), result);
    }

    @Test
    void shouldConvertCollectionOfStringNames() {
        // given
        List<String> source = Arrays.asList("RED", "GREEN");
        // when
        @SuppressWarnings("unchecked")
        EnumSet<Color> result = EnumConversions.toEnumSet(source, converter, Color.class);
        // then
        assertEquals(EnumSet.of(Color.RED, Color.GREEN), result);
    }

    @Test
    void shouldReturnEmptyEnumSetForEmptyCollection() {
        // given
        List<Object> source = Collections.emptyList();
        // when
        @SuppressWarnings("unchecked")
        EnumSet<Color> result = EnumConversions.toEnumSet(source, converter, Color.class);
        // then
        assertEquals(EnumSet.noneOf(Color.class), result);
    }

    @Test
    void shouldConvertArrayOfEnumInstances() {
        // given
        Color[] source = { Color.GREEN, Color.BLUE };
        // when
        @SuppressWarnings("unchecked")
        EnumSet<Color> result = EnumConversions.toEnumSet(source, converter, Color.class);
        // then
        assertEquals(EnumSet.of(Color.GREEN, Color.BLUE), result);
    }

    @Test
    void shouldConvertArrayOfStringNames() {
        // given
        String[] source = { "BLUE", "RED" };
        // when
        @SuppressWarnings("unchecked")
        EnumSet<Color> result = EnumConversions.toEnumSet(source, converter, Color.class);
        // then
        assertEquals(EnumSet.of(Color.BLUE, Color.RED), result);
    }

    @Test
    void shouldReturnEmptyEnumSetForEmptyArray() {
        // given
        Color[] source = {};
        // when
        @SuppressWarnings("unchecked")
        EnumSet<Color> result = EnumConversions.toEnumSet(source, converter, Color.class);
        // then
        assertEquals(EnumSet.noneOf(Color.class), result);
    }

    @Test
    void shouldThrowWhenCollectionContainsNullElement() {
        // given
        List<Object> source = Arrays.asList(Color.RED, null, Color.BLUE);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EnumConversions.toEnumSet(source, converter, Color.class));
        assertTrue(ex.getMessage().toLowerCase().contains("null"));
    }

    @Test
    void shouldThrowWhenArrayContainsNullElement() {
        // given
        Color[] source = { Color.GREEN, null };
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EnumConversions.toEnumSet(source, converter, Color.class));
        assertTrue(ex.getMessage().toLowerCase().contains("null"));
    }

    @Test
    void shouldThrowWhenCollectionHasInvalidEnumName() {
        // given
        // PURPLE does not exist
        List<String> source = Arrays.asList("RED", "PURPLE");
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EnumConversions.toEnumSet(source, converter, Color.class));
        assertTrue(ex.getMessage().contains("No enum constant"));
    }

    @Test
    void shouldThrowWhenTargetClassIsNull() {
        // given
        List<Object> source = Collections.emptyList();
        // when / then
        assertThrows(NullPointerException.class, () -> EnumConversions.toEnumSet(source, converter, null));
    }

    @Test
    void shouldThrowWhenSourceIsNull() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> EnumConversions.toEnumSet(source, converter, Color.class));
    }

    @Test
    void shouldThrowWhenCollectionHasNonConvertibleElement() {
        // given
        List<Object> source = Arrays.asList(Color.RED, Integer.valueOf(5));
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EnumConversions.toEnumSet(source, converter, Color.class));
        assertTrue(ex.getMessage().toLowerCase().contains("cannot convert"));
    }

    @Test
    void shouldThrowWhenArrayIsOfPrimitiveType() {
        // given
        int[] source = { 1, 2 };
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EnumConversions.toEnumSet(source, converter, Color.class));
        // The message contains the internal name of the primitive array class, e.g. "[I"
        assertTrue(ex.getMessage().contains("[I"));
    }

    @Test
    void shouldThrowWhenCollectionContainsMismatchedEnumType() {
        // given
        List<OtherEnum> source = Collections.singletonList(OtherEnum.VALUE);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> EnumConversions.toEnumSet(source, converter, Color.class));
        assertTrue(ex.getMessage().toLowerCase().contains("cannot convert"));
    }
}
