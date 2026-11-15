package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.YearMonth;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class YearMonthConversions_toMap_Test {

    /**
     * The Converter is not used by {@code YearMonthConversions.toMap}, but the method signature requires it.
     */
    @Mock
    private Converter mockConverter;

    /* --------------------------------------------------------------
     *  Helper holder class for parameterized test arguments.
     * -------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final YearMonth yearMonth;

        final int expectedYear;

        final int expectedMonth;

        final String description;

        ArgumentsHolder(YearMonth yearMonth, int expectedYear, int expectedMonth, String description) {
            this.yearMonth = yearMonth;
            this.expectedYear = expectedYear;
            this.expectedMonth = expectedMonth;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for the normal and edge‑case conversions.
     * -------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> validYearMonthProvider() {
        return Stream.of(new ArgumentsHolder(YearMonth.of(2023, 5), 2023, 5, "Convert a normal YearMonth (2023‑05) to a map; verify year and month."), new ArgumentsHolder(YearMonth.of(1999, 12), 1999, 12, "Convert a YearMonth at the end of a year to ensure month value 12 is correct."), new ArgumentsHolder(YearMonth.of(0, 1), 0, 1, "Convert a YearMonth with year zero (Year 0) to verify handling of non‑positive years."), new ArgumentsHolder(YearMonth.of(-500, 7), -500, 7, "Convert a YearMonth with a negative year to ensure negative years are preserved."));
    }

    @Test
    void toMap_givenValidYearMonth_returnsCorrectMap() {
        // GIVEN
        YearMonth input = holder.yearMonth;
        // WHEN
        Map<String, Object> result = YearMonthConversions.toMap(input, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(2, result.size(), "Map should contain exactly two entries");
        assertEquals(holder.expectedYear, result.get("year"), "Year value should match expected");
        assertEquals(holder.expectedMonth, result.get("month"), "Month value should match expected");
        // Verify the concrete map implementation (optional but useful for regression)
        assertTrue(result instanceof CompactLinkedMap, "Implementation should be CompactLinkedMap");
    }

    @Test
    void toMap_givenNullFrom_throwsNullPointerException() {
        // GIVEN
        Object input = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> YearMonthConversions.toMap(input, mockConverter), "Calling toMap with a null 'from' argument should throw NPE");
    }

    @Test
    void toMap_givenInvalidType_throwsClassCastException() {
        // GIVEN
        // Not a YearMonth
        Object input = "2023-05";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> YearMonthConversions.toMap(input, mockConverter), "Calling toMap with a non‑YearMonth object should throw ClassCastException");
    }
}
