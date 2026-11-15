package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.MonthDay;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.util.stream.Stream;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class MapConversions_toMonthDay_Test {

    @Mock
    Converter // injected by Mockito
    converter;

    /**
     * Helper holder for the expected outcome of a test case.
     */
    private static final class ExpectedResult {

        final MonthDay value;

        final Class<? extends Throwable> exception;

        ExpectedResult(MonthDay value) {
            this.value = value;
            this.exception = null;
        }

        ExpectedResult(Class<? extends Throwable> exception) {
            this.value = null;
            this.exception = exception;
        }
    }

    private static Stream<Arguments> standardCases() {
        return Stream.of(// 1. both keys present – integer values
        Arguments.of(Map.of("month", 5, "day", 15), (Consumer<Converter>) c -> {
            when(c.convert(5, int.class)).thenReturn(5);
            when(c.convert(15, int.class)).thenReturn(15);
        }, new ExpectedResult(MonthDay.of(5, 15))), // 2. both keys present – numeric strings
        Arguments.of(Map.of("month", "12", "day", "31"), (Consumer<Converter>) c -> {
            when(c.convert("12", int.class)).thenReturn(12);
            when(c.convert("31", int.class)).thenReturn(31);
        }, new ExpectedResult(MonthDay.of(12, 31))), // 3. invalid date (Feb 30) – should throw DateTimeException
        Arguments.of(Map.of("month", 2, "day", 30), (Consumer<Converter>) c -> {
            when(c.convert(2, int.class)).thenReturn(2);
            when(c.convert(30, int.class)).thenReturn(30);
        }, new ExpectedResult(DateTimeException.class)), // 4. missing month key – fallback (returns null)
        Arguments.of(Map.of("day", 10), (Consumer<Converter>) c -> {
            when(c.convert(any(), eq(MonthDay.class))).thenReturn(null);
        }, new ExpectedResult((MonthDay) null)), // 5. missing day key – fallback (returns null)
        Arguments.of(Map.of("month", 7), (Consumer<Converter>) c -> {
            when(c.convert(any(), eq(MonthDay.class))).thenReturn(null);
        }, new ExpectedResult((MonthDay) null)), // 6. month value is null – fallback (returns null)
        Arguments.of(new HashMap<String, Object>() {

            {
                put("month", null);
                put("day", 5);
            }
        }, (Consumer<Converter>) c -> {
            when(c.convert(any(), eq(MonthDay.class))).thenReturn(null);
        }, new ExpectedResult((MonthDay) null)), // 7. day value is null – fallback (returns null)
        Arguments.of(new HashMap<String, Object>() {

            {
                put("month", 3);
                put("day", null);
            }
        }, (Consumer<Converter>) c -> {
            when(c.convert(any(), eq(MonthDay.class))).thenReturn(null);
        }, new ExpectedResult((MonthDay) null)), // 8. extra unrelated entries – should be ignored
        Arguments.of(Map.of("month", 1, "day", 1, "extra", "ignored"), (Consumer<Converter>) c -> {
            when(c.convert(1, int.class)).thenReturn(1);
            when(c.convert(1, int.class)).thenReturn(1);
        }, new ExpectedResult(MonthDay.of(1, 1))));
    }

    private static Stream<Arguments> edgeCases() {
        return Stream.of(// 9. null source object – NullPointerException from cast
        Arguments.of(null, (Consumer<Converter>) c -> {
        }, /* no interactions */
        new ExpectedResult(NullPointerException.class), "Null source should cause NullPointerException"), // 10. source is not a Map – ClassCastException
        Arguments.of("just a string", (Consumer<Converter>) c -> {
        }, /* no interactions */
        new ExpectedResult(ClassCastException.class), "Non‑Map source should cause ClassCastException"), // 11. month = 0 (invalid) – DateTimeException
        Arguments.of(Map.of("month", 0, "day", 1), (Consumer<Converter>) c -> {
            when(c.convert(0, int.class)).thenReturn(0);
            when(c.convert(1, int.class)).thenReturn(1);
        }, new ExpectedResult(DateTimeException.class), "Month value 0 is invalid"), // 12. month = 13 (invalid) – DateTimeException
        Arguments.of(Map.of("month", 13, "day", 10), (Consumer<Converter>) c -> {
            when(c.convert(13, int.class)).thenReturn(13);
            when(c.convert(10, int.class)).thenReturn(10);
        }, new ExpectedResult(DateTimeException.class), "Month value 13 is invalid"), // 13. month = -1 (invalid) – DateTimeException
        Arguments.of(Map.of("month", -1, "day", 5), (Consumer<Converter>) c -> {
            when(c.convert(-1, int.class)).thenReturn(-1);
            when(c.convert(5, int.class)).thenReturn(5);
        }, new ExpectedResult(DateTimeException.class), "Negative month is invalid"), // 14. valid leap day – February 29
        Arguments.of(Map.of("month", 2, "day", 29), (Consumer<Converter>) c -> {
            when(c.convert(2, int.class)).thenReturn(2);
            when(c.convert(29, int.class)).thenReturn(29);
        }, new ExpectedResult(MonthDay.of(2, 29)), "Leap‑day February 29 should be accepted"), // 15. BigDecimal values – conversion via intValueExact()
        Arguments.of(new HashMap<String, Object>() {

            {
                put("month", new BigDecimal(5));
                put("day", new BigDecimal(10));
            }
        }, (Consumer<Converter>) c -> {
            when(c.convert(new BigDecimal(5), int.class)).thenReturn(5);
            when(c.convert(new BigDecimal(10), int.class)).thenReturn(10);
        }, new ExpectedResult(MonthDay.of(5, 10)), "BigDecimal values should be converted to int"), // 16. String values with whitespace – should be trimmed before parsing
        Arguments.of(Map.of("month", " 7 ", "day", " 15 "), (Consumer<Converter>) c -> {
            when(c.convert(" 7 ", int.class)).thenReturn(7);
            when(c.convert(" 15 ", int.class)).thenReturn(15);
        }, new ExpectedResult(MonthDay.of(7, 15)), "Whitespace‑padded strings should be trimmed and parsed"), // 17. Non‑numeric month string – converter throws IllegalArgumentException
        Arguments.of(Map.of("month", "abc", "day", "10"), (Consumer<Converter>) c -> {
            when(c.convert("abc", int.class)).thenThrow(new IllegalArgumentException("cannot parse month"));
            when(c.convert("10", int.class)).thenReturn(10);
        }, // day conversion is never reached, but we can stub it anyway
        new ExpectedResult(IllegalArgumentException.class), "Non‑numeric month string should cause IllegalArgumentException"), // 18. CompactLinkedMap source – behaves like a normal Map
        Arguments.of(new CompactLinkedMap<String, Object>() {

            {
                put("month", 4);
                put("day", 20);
            }
        }, (Consumer<Converter>) c -> {
            when(c.convert(4, int.class)).thenReturn(4);
            when(c.convert(20, int.class)).thenReturn(20);
        }, new ExpectedResult(MonthDay.of(4, 20)), "CompactLinkedMap should be handled like any other Map"));
    }

    private static Stream<Arguments> exceptionScenarios() {
        return Stream.of(// 19. Converter argument is null – NPE when method tries to call convert()
        Arguments.of(Map.of("month", 5, "day", 10), (Consumer<Converter>) c -> {
        }, /* no mock – converter will be null */
        new ExpectedResult(NullPointerException.class), "Null converter should cause NullPointerException"), // 20. Converter returns null for month – unboxing NPE
        Arguments.of(Map.of("month", 5, "day", 10), (Consumer<Converter>) c -> {
            when(c.convert(5, int.class)).thenReturn(null);
            when(c.convert(10, int.class)).thenReturn(10);
        }, new ExpectedResult(NullPointerException.class), "Converter returning null for month triggers NPE on unboxing"), // 21. Converter throws IllegalArgumentException while converting day
        Arguments.of(Map.of("month", 5, "day", 10), (Consumer<Converter>) c -> {
            when(c.convert(5, int.class)).thenReturn(5);
            when(c.convert(10, int.class)).thenThrow(new IllegalArgumentException("bad day"));
        }, new ExpectedResult(IllegalArgumentException.class), "Converter exception on day conversion should propagate"), // 22. Invalid day value 0 – DateTimeException
        Arguments.of(Map.of("month", 5, "day", 0), (Consumer<Converter>) c -> {
            when(c.convert(5, int.class)).thenReturn(5);
            when(c.convert(0, int.class)).thenReturn(0);
        }, new ExpectedResult(DateTimeException.class), "Day value 0 is invalid"), // 23. Negative day value – DateTimeException
        Arguments.of(Map.of("month", 5, "day", -1), (Consumer<Converter>) c -> {
            when(c.convert(5, int.class)).thenReturn(5);
            when(c.convert(-1, int.class)).thenReturn(-1);
        }, new ExpectedResult(DateTimeException.class), "Negative day value is invalid"), // 24. April 31 – DateTimeException
        Arguments.of(Map.of("month", 4, "day", 31), (Consumer<Converter>) c -> {
            when(c.convert(4, int.class)).thenReturn(4);
            when(c.convert(31, int.class)).thenReturn(31);
        }, new ExpectedResult(DateTimeException.class), "April 31 does not exist"));
    }

    @Test
    void testToMonthDay_WithMonthAndDayKeys() {
        // Arrange – map contains explicit month and day entries.
        Map<String, Object> map = new HashMap<>();
        map.put("month", 5);
        map.put("day", 20);
        // The converter is expected to be asked to convert the raw objects to int.
        when(converter.convert(5, int.class)).thenReturn(5);
        when(converter.convert(20, int.class)).thenReturn(20);
        // Act
        MonthDay result = MapConversions.toMonthDay(map, converter);
        // Assert
        assertEquals(MonthDay.of(5, 20), result, "MonthDay should be built from the converted month and day values");
        verify(converter).convert(5, int.class);
        verify(converter).convert(20, int.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void testToMonthDay_FallbackUsingValueKey() {
        // Arrange – map does NOT contain month/day, but provides a generic "value".
        Map<String, Object> map = new HashMap<>();
        Object rawValue = "any representation";
        map.put("value", rawValue);
        MonthDay expected = MonthDay.of(12, 31);
        // The fallback path should ask the converter to turn the raw value into a MonthDay.
        when(converter.convert(rawValue, MonthDay.class)).thenReturn(expected);
        // Act
        MonthDay result = MapConversions.toMonthDay(map, converter);
        // Assert
        assertEquals(expected, result, "Fallback conversion should return the MonthDay supplied by the converter");
        verify(converter).convert(rawValue, MonthDay.class);
        verifyNoMoreInteractions(converter);
    }

    /**
     * -------------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("standardCases")
    void testToMonthDay_StandardCases(Map<String, Object> map, Consumer<Converter> mockSetup, ExpectedResult expected) {
        // Arrange
        mockSetup.accept(converter);
        // Act & Assert
        if (expected.exception != null) {
            assertThrows(expected.exception, () -> MapConversions.toMonthDay(map, converter), "Expected exception: " + expected.exception.getSimpleName());
        } else {
            MonthDay result = MapConversions.toMonthDay(map, converter);
            assertEquals(expected.value, result, "Expected MonthDay does not match the actual result");
        }
        verifyNoMoreInteractions(converter);
    }

    /**
     * -------------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("edgeCases")
    void testToMonthDay_EdgeCases(Object from, Consumer<Converter> mockSetup, ExpectedResult expected, String description) {
        // Arrange
        mockSetup.accept(converter);
        // Act & Assert
        if (expected.exception != null) {
            assertThrows(expected.exception, () -> MapConversions.toMonthDay(from, converter), description);
        } else {
            MonthDay result = MapConversions.toMonthDay(from, converter);
            assertEquals(expected.value, result, description);
        }
        verifyNoMoreInteractions(converter);
    }

    /**
     * -------------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("exceptionScenarios")
    void testToMonthDay_ExceptionScenarios(Object from, Consumer<Converter> mockSetup, ExpectedResult expected, String description) {
        // Arrange
        mockSetup.accept(converter);
        // Act & Assert
        if (expected.exception != null) {
            assertThrows(expected.exception, () -> MapConversions.toMonthDay(from, converter), description);
        } else {
            // No scenario in this group expects a successful value.
            fail("Test case mis‑configured: expected an exception");
        }
        verifyNoMoreInteractions(converter);
    }
}
