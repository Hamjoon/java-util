package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_equals_Test {

    private static final String FORMAT = "yyyy/MM/dd";

    private SafeSimpleDateFormat safeSdf;

    private SimpleDateFormat plainSdf;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh SafeSimpleDateFormat and the equivalent SimpleDateFormat for comparison
        safeSdf = new SafeSimpleDateFormat(FORMAT);
        plainSdf = SafeSimpleDateFormat.getDateFormat(FORMAT);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; no external resources to clean up
        safeSdf = null;
        plainSdf = null;
    }

    /**
     * Uses reflection to set the private {@code _format} field of {@link SafeSimpleDateFormat}.
     *
     * @param instance the SafeSimpleDateFormat instance to modify
     * @param value    the value to assign to the private field (may be {@code null})
     * @throws Exception if reflection fails
     */
    private static void setPrivateFormatField(SafeSimpleDateFormat instance, String value) throws Exception {
        Field field = SafeSimpleDateFormat.class.getDeclaredField("_format");
        field.setAccessible(true);
        field.set(instance, value);
    }

    @Test
    void equals_sameUnderlyingFormat_returnsTrue() {
        // given
        Object other = plainSdf;
        // when
        boolean result = safeSdf.equals(other);
        // then
        assertTrue(result, "SafeSimpleDateFormat should be equal to its underlying SimpleDateFormat");
    }

    @Test
    void equals_differentUnderlyingFormat_returnsFalse() {
        // given
        SimpleDateFormat differentSdf = new SimpleDateFormat("dd-MM-yyyy");
        Object other = differentSdf;
        // when
        boolean result = safeSdf.equals(other);
        // then
        assertFalse(result, "SafeSimpleDateFormat should not be equal to a SimpleDateFormat with a different pattern");
    }

    @Test
    void equals_sameSafeSimpleDateFormatInstance_returnsTrue() {
        // given
        SafeSimpleDateFormat another = new SafeSimpleDateFormat(FORMAT);
        Object other = another;
        // when
        boolean result = safeSdf.equals(other);
        // then
        // Delegates to SimpleDateFormat.equals, which returns true for equal patterns
        assertTrue(result, "Two SafeSimpleDateFormat instances with the same pattern should be equal");
    }

    @Test
    void equals_null_returnsFalse() {
        // given
        Object other = null;
        // when
        boolean result = safeSdf.equals(other);
        // then
        assertFalse(result, "SafeSimpleDateFormat should not be equal to null");
    }

    @Test
    void equals_differentClass_returnsFalse() {
        // given
        // any object that is not a DateFormat
        Object other = new Date();
        // when
        boolean result = safeSdf.equals(other);
        // then
        assertFalse(result, "SafeSimpleDateFormat should not be equal to an object of a different class");
    }

    @Test
    void equals_self_returnsTrue() {
        // given
        Object other = safeSdf;
        // when
        boolean result = safeSdf.equals(other);
        // then
        assertTrue(result, "SafeSimpleDateFormat should be equal to itself");
    }

    @Test
    void equals_anotherSafeSimpleDateFormatSamePattern_returnsTrue() {
        // given: a distinct SafeSimpleDateFormat with the same pattern
        SafeSimpleDateFormat other = new SafeSimpleDateFormat(FORMAT);
        // when
        boolean result = safeSdf.equals(other);
        // then
        assertTrue(result, "Two distinct SafeSimpleDateFormat instances with the same pattern should be equal");
    }

    @Test
    void equals_anotherSafeSimpleDateFormatDifferentPattern_returnsFalse() {
        // given: a SafeSimpleDateFormat with a different pattern
        SafeSimpleDateFormat other = new SafeSimpleDateFormat("MM/dd/yyyy");
        // when
        boolean result = safeSdf.equals(other);
        // then
        assertFalse(result, "SafeSimpleDateFormat objects with different patterns must not be equal");
    }

    @Test
    void equals_simpleDateFormatSamePattern_returnsTrue() {
        // given: a plain SimpleDateFormat that shares the exact same pattern
        SimpleDateFormat other = new SimpleDateFormat(FORMAT);
        Object obj = other;
        // when
        boolean result = safeSdf.equals(obj);
        // then
        assertTrue(result, "A SafeSimpleDateFormat should be equal to a SimpleDateFormat with the same pattern");
    }

    @Test
    void equals_simpleDateFormatDifferentPattern_returnsFalse() {
        // given: a plain SimpleDateFormat with a different pattern
        SimpleDateFormat other = new SimpleDateFormat("dd-MM-yyyy");
        Object obj = other;
        // when
        boolean result = safeSdf.equals(obj);
        // then
        assertFalse(result, "Even though both objects are date formatters, differing patterns cause equals to return false");
    }

    @Test
    void equals_unrelatedObjectString_returnsFalse() {
        // given: an object of unrelated type
        Object other = "some random string";
        // when
        boolean result = safeSdf.equals(other);
        // then
        assertFalse(result, "equals should return false when the argument is not a DateFormat (or SimpleDateFormat) instance");
    }

    @Test
    void equals_afterChangingLenientFlag_returnsFalse() {
        // given: modify lenient flag on the first instance
        safeSdf.setLenient(false);
        // default lenient = true
        SafeSimpleDateFormat other = new SafeSimpleDateFormat(FORMAT);
        // when
        boolean result = safeSdf.equals(other);
        // then
        assertFalse(result, "Changing lenient on SafeSimpleDateFormat does not affect the SimpleDateFormat used inside equals, so objects are not equal");
    }

    @Test
    void equals_afterChangingTimeZone_returnsFalse() {
        // given: change the time zone on the first instance
        safeSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        // default time zone of JVM
        SafeSimpleDateFormat other = new SafeSimpleDateFormat(FORMAT);
        // when
        boolean result = safeSdf.equals(other);
        // then
        assertFalse(result, "Altering the time zone of the SafeSimpleDateFormat instance does not affect the SimpleDateFormat used inside equals, leading to inequality");
    }

    @Test
    void equals_nullFormatString_throwsNullPointerException() throws Exception {
        // given: construct a SafeSimpleDateFormat with a valid pattern and then force the internal format to null
        SafeSimpleDateFormat faulty = new SafeSimpleDateFormat(FORMAT);
        setPrivateFormatField(faulty, null);
        // when / then
        assertThrows(NullPointerException.class, () -> faulty.equals(new Object()), "SafeSimpleDateFormat constructed with a null format string should throw NPE when equals() is invoked");
    }

    @Test
    void equals_invalidPattern_throwsIllegalArgumentException() {
        // given: an illegal pattern that SimpleDateFormat cannot parse
        SafeSimpleDateFormat faulty = new SafeSimpleDateFormat("yyyy-MM-!!");
        // when / then
        assertThrows(IllegalArgumentException.class, () -> faulty.equals(new Object()), "SafeSimpleDateFormat constructed with an illegal pattern should throw IllegalArgumentException when equals() is invoked");
    }
}
