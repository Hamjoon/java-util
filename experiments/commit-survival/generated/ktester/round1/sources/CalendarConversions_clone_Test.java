package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_clone_Test {

    @Mock
    private Converter // Converter is not used by the method, but required by the signature
    mockConverter;

    /**
     * Provides a variety of {@link Calendar} instances that represent the
     * different basic scenarios described in the JSON test cases.
     */
    private static Stream<Arguments> basicCloningProvider() {
        // 1. GregorianCalendar with a specific instant
        Calendar c1 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        // 2023‑01‑01T12:34:56.789Z
        c1.setTimeInMillis(1672570496789L);
        // 2. GregorianCalendar with default locale & timezone
        Calendar c2 = new GregorianCalendar();
        // 3. GregorianCalendar in America/New_York
        Calendar c3 = new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
        c3.setTimeInMillis(1672570496789L);
        // 4. GregorianCalendar with custom fields set
        Calendar c4 = new GregorianCalendar();
        c4.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
        c4.set(Calendar.WEEK_OF_YEAR, 15);
        c4.setTimeInMillis(1672570496789L);
        // 5. BuddhistCalendar (sub‑class of Calendar)
        Calendar c5 = new BuddhistCalendar();
        // Buddhist year 2564‑12‑31
        c5.set(2564, Calendar.DECEMBER, 31);
        // force internal fields to be computed
        c5.setTimeInMillis(c5.getTimeInMillis());
        return Stream.of(Arguments.of(c1, "GregorianCalendar with explicit UTC instant"), Arguments.of(c2, "GregorianCalendar with default locale & timezone"), Arguments.of(c3, "GregorianCalendar with America/New_York timezone"), Arguments.of(c4, "GregorianCalendar with custom DAY_OF_WEEK and WEEK_OF_YEAR"), Arguments.of(c5, "BuddhistCalendar subclass"));
    }

    private static Stream<Arguments> edgeCaseProvider() {
        // 1. Epoch zero in UTC
        Calendar epochZero = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        epochZero.setTimeInMillis(0L);
        // 2. Negative epoch (1960‑05‑15T08:30:00Z) in GMT+2
        TimeZone gmtPlus2 = TimeZone.getTimeZone("GMT+02:00");
        Calendar negative = new GregorianCalendar(gmtPlus2);
        // corresponds to 1960‑05‑15T08:30:00Z
        negative.setTimeInMillis(-315619800000L);
        // 3. Lenient = false, firstDayOfWeek = SUNDAY
        Calendar nonDefaultConfig = new GregorianCalendar();
        nonDefaultConfig.setLenient(false);
        nonDefaultConfig.setFirstDayOfWeek(Calendar.SUNDAY);
        nonDefaultConfig.setTimeInMillis(1672570496789L);
        // 4. Custom SimpleTimeZone (GMT+5:30) with manually set fields
        SimpleTimeZone customTz = new SimpleTimeZone(5 * 60 * 60 * 1000 + 30 * 60 * 1000, "GMT+05:30");
        Calendar customZone = new GregorianCalendar(customTz);
        customZone.set(2022, Calendar.JUNE, 10, 12, 0, 0);
        customZone.set(Calendar.MILLISECOND, 0);
        // 5. Locale.JAPAN with a misbehaving converter (throws if any method called)
        Calendar localeJapan = new GregorianCalendar(Locale.JAPAN);
        localeJapan.setTimeInMillis(1672570496789L);
        // 6. GregorianCalendar that has been cleared
        Calendar cleared = new GregorianCalendar();
        // removes all fields; timezone & locale stay the same
        cleared.clear();
        return Stream.of(Arguments.of(epochZero, "Epoch zero (1970‑01‑01T00:00:00Z) in UTC"), Arguments.of(negative, "Negative epoch (1960‑05‑15) in GMT+2"), Arguments.of(nonDefaultConfig, "Lenient=false and firstDayOfWeek=SUNDAY"), Arguments.of(customZone, "Custom SimpleTimeZone GMT+5:30 with manually set fields"), Arguments.of(localeJapan, "Locale.JAPAN with a misbehaving converter"), Arguments.of(cleared, "Cleared GregorianCalendar"));
    }

    @Test
    void clone_ShouldReturnDifferentInstanceWithSameTime() {
        // given
        Calendar original = new GregorianCalendar();
        // deterministic epoch millis
        original.setTimeInMillis(123_456_789L);
        // when
        Calendar cloned = CalendarConversions.clone(original, mockConverter);
        // then
        assertNotSame(original, cloned, "clone() must return a new Calendar instance");
        assertEquals(original.getTimeInMillis(), cloned.getTimeInMillis(), "Cloned calendar must represent the same instant as the original");
        // Verify that other mutable fields (e.g., timezone) are also equal
        assertEquals(original.getTimeZone(), cloned.getTimeZone(), "Cloned calendar must have the same TimeZone as the original");
    }

    @Test
    void clone_WithNonCalendarObject_ShouldThrowClassCastException() {
        // given
        Object notACalendar = new Object();
        // when / then
        assertThrows(ClassCastException.class, () -> CalendarConversions.clone(notACalendar, mockConverter), "Passing a non‑Calendar object must result in ClassCastException");
    }

    @Test
    void clone_WithNull_ShouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.clone(null, mockConverter), "Passing null should cause a NullPointerException because of the cast");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("basicCloningProvider")
    void clone_BasicScenarios_ShouldDuplicateAllState(Calendar original, String description) {
        // when
        Calendar cloned = CalendarConversions.clone(original, mockConverter);
        // then – generic assertions that apply to all basic scenarios
        assertNotSame(original, cloned, "Clone must be a different instance");
        assertEquals(original.getClass(), cloned.getClass(), "Clone must preserve the concrete Calendar subclass");
        assertEquals(original.getTimeInMillis(), cloned.getTimeInMillis(), "Clone must have the same epoch milliseconds");
        assertEquals(original.getTimeZone(), cloned.getTimeZone(), "Clone must preserve the TimeZone");
        assertEquals(original.getLocale(), cloned.getLocale(), "Clone must preserve the Locale");
        assertEquals(original.isLenient(), cloned.isLenient(), "Clone must preserve the lenient flag");
        assertEquals(original.getFirstDayOfWeek(), cloned.getFirstDayOfWeek(), "Clone must preserve the firstDayOfWeek setting");
        // Verify that custom fields (if any) are duplicated.
        // We compare a selection of fields that are commonly set.
        int[] fields = { Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND, Calendar.DAY_OF_WEEK, Calendar.WEEK_OF_YEAR };
        for (int field : fields) {
            assertEquals(original.get(field), cloned.get(field), "Field " + field + " should be identical after cloning");
        }
        // Ensure the mock converter was never touched.
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("edgeCaseProvider")
    void clone_EdgeCases_ShouldPreserveAllState(Calendar original, String description) {
        // For case 5 we need a converter that throws on any interaction.
        Converter misbehavingConverter = mock(Converter.class);
        doAnswer(invocation -> {
            throw new AssertionError("Converter should not be invoked");
        }).when(misbehavingConverter).convert(any(), any());
        Converter converterToUse = (description.contains("misbehaving")) ? misbehavingConverter : mockConverter;
        // when
        Calendar cloned = CalendarConversions.clone(original, converterToUse);
        // then – generic checks
        assertNotSame(original, cloned, "Clone must be a distinct instance");
        assertEquals(original.getClass(), cloned.getClass(), "Clone must preserve the concrete Calendar subclass");
        assertEquals(original.getTimeInMillis(), cloned.getTimeInMillis(), "Clone must have the same epoch milliseconds");
        assertEquals(original.getTimeZone(), cloned.getTimeZone(), "Clone must preserve the TimeZone");
        assertEquals(original.getLocale(), cloned.getLocale(), "Clone must preserve the Locale");
        assertEquals(original.isLenient(), cloned.isLenient(), "Clone must preserve the lenient flag");
        assertEquals(original.getFirstDayOfWeek(), cloned.getFirstDayOfWeek(), "Clone must preserve the firstDayOfWeek setting");
        // Verify that custom fields (if any) are duplicated.
        // For the cleared calendar we expect all fields to be the same default (0).
        int[] fields = { Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND, Calendar.DAY_OF_WEEK, Calendar.WEEK_OF_YEAR };
        for (int field : fields) {
            assertEquals(original.get(field), cloned.get(field), "Field " + field + " should be identical after cloning");
        }
        // Ensure the converter argument was never used.
        verifyNoInteractions(converterToUse);
    }
}
