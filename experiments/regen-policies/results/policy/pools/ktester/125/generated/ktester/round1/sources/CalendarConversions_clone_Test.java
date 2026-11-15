package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Calendar;
import java.util.GregorianCalendar;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.TimeZone;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_clone_Test {

    @Mock
    Converter // not used by the method but required by the signature
    converter;

    /**
     * A {@link GregorianCalendar} subclass that deliberately returns a {@link String}
     * from its {@code clone()} method, causing a {@link ClassCastException} in the
     * {@code CalendarConversions.clone} implementation.
     */
    static class CustomCalendarWrongClone extends GregorianCalendar {

        @Override
        public Object clone() {
            // Return a wrong type on purpose
            return "not a calendar";
        }
    }

    /**
     * A {@link GregorianCalendar} subclass whose {@code clone()} method throws
     * an {@link IllegalStateException}.  This allows us to verify that runtime
     * exceptions from the underlying {@code clone()} are propagated.
     */
    static class CustomCalendarThrowingClone extends GregorianCalendar {

        @Override
        public Object clone() {
            throw new IllegalStateException("clone failure");
        }
    }

    /**
     * Retrieves the {@code locale} field from a {@link Calendar} instance via reflection.
     * The field is package‑private in {@link GregorianCalendar}, so reflection is required.
     *
     * @param cal the calendar to inspect
     * @return the {@link Locale} stored inside the calendar
     */
    private static Locale getLocale(Calendar cal) {
        try {
            Field localeField = GregorianCalendar.class.getDeclaredField("locale");
            localeField.setAccessible(true);
            return (Locale) localeField.get(cal);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Unable to access locale field via reflection: " + e);
            // unreachable
            return null;
        }
    }

    @Test
    void clone_ShouldReturnNewCalendarWithSameInstant() {
        // given
        Calendar original = new GregorianCalendar();
        // a fixed epoch milli value
        original.setTimeInMillis(1_640_995_200_000L);
        // when
        Calendar cloned = CalendarConversions.clone(original, converter);
        // then
        assertNotNull(cloned, "Cloned calendar should not be null");
        assertNotSame(original, cloned, "Cloned calendar should be a different instance");
        assertEquals(original.getTimeInMillis(), cloned.getTimeInMillis(), "Cloned calendar must represent the same instant as the original");
        // Verify that mutable state is independent
        cloned.add(Calendar.DAY_OF_MONTH, 1);
        assertNotEquals(original.getTimeInMillis(), cloned.getTimeInMillis(), "Modifying the clone must not affect the original");
    }

    @Test
    void clone_NullArgument_ShouldThrowNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CalendarConversions.clone(nullObj, converter), "Cloning a null reference should throw NullPointerException");
    }

    @Test
    void clone_NonCalendarArgument_ShouldThrowClassCastException() {
        // given
        Object notACalendar = "I am not a calendar";
        // when / then
        assertThrows(ClassCastException.class, () -> CalendarConversions.clone(notACalendar, converter), "Cloning a non‑Calendar object should throw ClassCastException");
    }

    @Test
    void clone_ShouldReturnExactCopy_ForStandardGregorianCalendar() {
        // given: a GregorianCalendar set to a known instant (UTC)
        Calendar original = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        // 2023-03-15T10:20:30.000Z
        original.setTimeInMillis(1_680_000_000_000L);
        // when
        Calendar cloned = CalendarConversions.clone(original, converter);
        // then
        assertNotNull(cloned, "Cloned calendar should not be null");
        assertNotSame(original, cloned, "Cloned calendar must be a different instance");
        assertEquals(original.getTimeInMillis(), cloned.getTimeInMillis(), "Cloned calendar must represent the same instant as the original");
        assertEquals(original.isLenient(), cloned.isLenient(), "Leniency flag should be copied");
        assertEquals(original.getTimeZone(), cloned.getTimeZone(), "Time zone should be copied");
        // Locale is not directly accessible; verify via reflection
        assertEquals(getLocale(original), getLocale(cloned), "Locale should be copied");
    }

    @Test
    void clone_IndependenceAfterSourceMutation() {
        // given: a calendar with lenient = false
        Calendar original = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        original.setLenient(false);
        // 2022-12-31T23:59:59.999Z
        original.setTimeInMillis(1_670_000_000_000L);
        // when
        Calendar cloned = CalendarConversions.clone(original, converter);
        // then: verify initial equality
        assertEquals(original.getTimeInMillis(), cloned.getTimeInMillis(), "Clone must initially have same instant");
        assertEquals(original.isLenient(), cloned.isLenient(), "Clone must copy leniency flag");
        // mutate the original after cloning
        // shift by 1 second
        original.add(Calendar.MILLISECOND, 1_000);
        // cloned must stay unchanged
        assertNotEquals(original.getTimeInMillis(), cloned.getTimeInMillis(), "Modifying the original must not affect the clone");
    }

    @Test
    void clone_PreservesCustomTimeZoneAndLocale() {
        // given: a calendar with a non‑default time zone and locale
        TimeZone nyZone = TimeZone.getTimeZone("America/New_York");
        Locale french = Locale.FRENCH;
        Calendar original = new GregorianCalendar(nyZone, french);
        // arbitrary instant
        original.setTimeInMillis(1_600_000_000_000L);
        // when
        Calendar cloned = CalendarConversions.clone(original, converter);
        // then
        assertEquals(nyZone, cloned.getTimeZone(), "Cloned calendar must preserve the original time zone");
        assertEquals(french, getLocale(cloned), "Cloned calendar must preserve the original locale");
    }

    @Test
    void clone_NullConverter_ShouldStillCloneSuccessfully() {
        // given: a simple calendar and a null converter
        Calendar original = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        // 1970-01-01T00:00:00.000Z
        original.setTimeInMillis(0L);
        // when
        Calendar cloned = CalendarConversions.clone(original, null);
        // then
        assertNotNull(cloned, "Clone must not be null even when converter is null");
        assertNotSame(original, cloned, "Clone must be a different instance");
        assertEquals(original.getTimeInMillis(), cloned.getTimeInMillis(), "Clone must have identical instant");
    }

    @Test
    void clone_SubclassCloneReturnsWrongType_ShouldThrowClassCastException() {
        // given: a Calendar subclass whose clone() returns a String
        Calendar badClone = new CustomCalendarWrongClone();
        // when / then
        assertThrows(ClassCastException.class, () -> CalendarConversions.clone(badClone, converter), "If the overridden clone() returns a non‑Calendar type, a ClassCastException must be thrown");
    }

    @Test
    void clone_SubclassCloneThrowsRuntimeException_ShouldPropagate() {
        // given: a Calendar subclass whose clone() throws IllegalStateException
        Calendar throwingClone = new CustomCalendarThrowingClone();
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CalendarConversions.clone(throwingClone, converter), "Runtime exception from clone() must propagate unchanged");
        assertEquals("clone failure", ex.getMessage());
    }
}
