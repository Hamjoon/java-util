package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.DateTimeException;

@ExtendWith(MockitoExtension.class)
class AtomicLongConversions_toLocalTime_Test {

    // shared Converter instance for all tests
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Converter for each test – using default options is sufficient.
        converter = new Converter(new ConverterOptions());
    }

    @Test
    void toLocalTime_ValidAtomicLong_DelegatesToLongConversions() {
        // GIVEN
        // a deterministic epoch‑millisecond value
        long epochMilli = 1_620_000_000_000L;
        AtomicLong atomicLong = new AtomicLong(epochMilli);
        // Expected result is produced by the same delegate that the method under test uses.
        LocalTime expected = LongConversions.toLocalTime(epochMilli, converter);
        // WHEN
        LocalTime actual = AtomicLongConversions.toLocalTime(atomicLong, converter);
        // THEN
        assertEquals(expected, actual, "AtomicLongConversions.toLocalTime should delegate to LongConversions.toLocalTime");
    }

    @Test
    void toLocalTime_NullAtomicLong_ThrowsNullPointerException() {
        // GIVEN
        AtomicLong nullAtomic = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> AtomicLongConversions.toLocalTime(nullAtomic, converter), "Passing a null AtomicLong should result in NullPointerException");
    }

    @Test
    void toLocalTime_WrongObjectType_ThrowsClassCastException() {
        // GIVEN
        Object notAtomic = Integer.valueOf(42);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> AtomicLongConversions.toLocalTime(notAtomic, converter), "Passing an object that is not an AtomicLong should cause a ClassCastException");
    }

    /**
     * Null {@code converter} handling:
     * The delegate {@link LongConversions#toLocalTime(long, Converter)} does not accept a null
     * converter, so a {@link NullPointerException} should propagate.
     */
    @Test
    void toLocalTime_NullConverter_ThrowsNullPointerException() {
        // GIVEN
        AtomicLong atomicLong = new AtomicLong(0L);
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> AtomicLongConversions.toLocalTime(atomicLong, nullConverter), "Passing a null Converter should result in NullPointerException");
    }

    /**
     * Invalid long value handling:
     * An {@link AtomicLong} holding a value that cannot be represented as a {@link LocalTime}
     * (e.g., {@code Long.MAX_VALUE}) should cause {@link LongConversions#toLocalTime}
     * to throw a {@link DateTimeException}, which we verify propagates.
     */
    @Test
    void toLocalTime_ValueTooLarge_ThrowsDateTimeException() {
        // GIVEN
        AtomicLong atomicLong = new AtomicLong(Long.MAX_VALUE);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> AtomicLongConversions.toLocalTime(atomicLong, converter), "A value that cannot be converted to LocalTime should throw DateTimeException");
    }
}
