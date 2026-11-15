package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.GregorianCalendar;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_clone_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter is required by the clone method signature, even though the
        // implementation does not use it.  Use the default options to keep the test
        // simple and deterministic.
        converter = new Converter(new DefaultConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources, if any.
        converter = null;
    }

    /**
     * Provides a stream of {@link Calendar} instances with different time‑zones and dates.
     * The {@code Converter} argument is deliberately set to {@code null} to verify that
     * the method works without it (as per the JSON test case definition).
     */
    private static Stream<Arguments> validCalendars() {
        return Stream.of(// GregorianCalendar with default time‑zone
        Arguments.of(new GregorianCalendar(2023, Calendar.MARCH, 10, 12, 34, 56), null), // GregorianCalendar with a specific time‑zone
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("Europe/Paris")) {

            {
                clear();
                set(2022, Calendar.DECEMBER, 25, 8, 15, 30);
                set(Calendar.MILLISECOND, 123);
            }
        }, null), // Calendar instance created via Calendar.getInstance()
        Arguments.of(Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo")), null));
    }

    /**
     * Verify that {@code CalendarConversions.clone} returns a new Calendar
     * instance that is equal in value to the source but not the same reference.
     * Changing the original after cloning must not affect the cloned copy.
     */
    @Test
    void testCloneCreatesIndependentCopy() {
        // Arrange – create a Calendar with a known timezone and a set of fields
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        Calendar original = Calendar.getInstance(tz);
        original.clear();
        // month is 0‑based
        original.set(2023, Calendar.JUNE, 15, 12, 34, 56);
        original.set(Calendar.MILLISECOND, 789);
        // Act – invoke the method under test
        Calendar cloned = CalendarConversions.clone(original, converter);
        // Assert – the clone is a different object
        assertNotSame(original, cloned, "clone should return a different Calendar instance");
        // The two calendars should represent the same instant in time
        assertEquals(original.getTimeInMillis(), cloned.getTimeInMillis(), "cloned calendar must have the same epoch millis as the original");
        // Changing the original must not affect the clone
        original.set(Calendar.YEAR, 1999);
        assertNotEquals(original.getTimeInMillis(), cloned.getTimeInMillis(), "modifying the original after cloning must not change the cloned instance");
    }

    /**
     * Verify that passing a non‑Calendar object results in a {@link ClassCastException}.
     */
    @Test
    void testCloneWithInvalidSourceThrowsException() {
        Object notACalendar = "I am not a Calendar";
        assertThrows(ClassCastException.class, () -> CalendarConversions.clone(notACalendar, converter), "Cloning a non‑Calendar object should throw ClassCastException");
    }

    /**
     * Verify that passing {@code null} as the source throws a {@link NullPointerException}.
     * The implementation casts the argument to {@code Calendar} before calling {@code clone()},
     * so a null value leads to NPE.
     */
    @Test
    void testCloneWithNullSourceThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            CalendarConversions.clone(null, converter);
        });
    }

    /**
     * Verify that cloning a valid {@link Calendar} returns a new instance that
     * represents the same instant but is a distinct mutable object.
     *
     * <p>This test is parameterized to cover several calendar configurations,
     * matching the {@code clone_validCalendar} group from the supplied JSON.
     */
    @ParameterizedTest(name = "[{index}] clone valid calendar {0}")
    @MethodSource("validCalendars")
    void testCloneValidCalendar(Calendar original, Converter suppliedConverter) {
        // Act
        Calendar cloned = CalendarConversions.clone(original, suppliedConverter);
        // Assert – different reference
        assertNotSame(original, cloned, "clone should return a different Calendar instance");
        // Assert – same epoch milliseconds (i.e., same point in time)
        assertEquals(original.getTimeInMillis(), cloned.getTimeInMillis(), "cloned calendar must have the same epoch millis as the original");
        // Mutate original and ensure clone stays unchanged
        original.add(Calendar.YEAR, 10);
        assertNotEquals(original.getTimeInMillis(), cloned.getTimeInMillis(), "modifying the original after cloning must not affect the clone");
    }

    /**
     * Verify that passing {@code null} as the source triggers a {@link NullPointerException}.
     *
     * <p>This corresponds to the {@code clone_nullFrom} group in the JSON test data.
     */
    @Test
    void testCloneNullFromThrowsException() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.clone(null, null), "Cloning a null source should throw NullPointerException");
    }

    /**
     * Additional sanity check: cloning with a non‑null {@link Converter} behaves
     * identically to cloning with {@code null} because the implementation ignores it.
     */
    @Test
    void testCloneIgnoresConverterArgument() {
        Calendar original = new GregorianCalendar(2021, Calendar.JANUARY, 1, 0, 0, 0);
        original.set(Calendar.MILLISECOND, 0);
        Calendar clonedWithNull = CalendarConversions.clone(original, null);
        Calendar clonedWithConverter = CalendarConversions.clone(original, converter);
        assertEquals(clonedWithNull.getTimeInMillis(), clonedWithConverter.getTimeInMillis(), "Both clones should have identical epoch millis regardless of the Converter argument");
        assertNotSame(clonedWithNull, clonedWithConverter, "Even when using different Converter arguments, the clones must be distinct objects");
    }
}
