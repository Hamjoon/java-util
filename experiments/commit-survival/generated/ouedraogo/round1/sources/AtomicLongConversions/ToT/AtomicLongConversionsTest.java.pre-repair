package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class AtomicLongConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Converter requires a ConverterOptions instance; using the default constructor.
        // If a no‑arg constructor is not available, replace with the appropriate factory.
        this.converter = new Converter(new ConverterOptions());
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /*** Tests for toAtomicLong ***/

    @Test
    public void toAtomicLong_typicalValue_returnsNewAtomicLongWithSameValue() {
        AtomicLong source = new AtomicLong(12345L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(source, converter);

        // Verify a new instance is created
        assertNotSame("Result should be a different instance", source, result);
        // Verify the numeric value is preserved
        assertEquals("Result should contain the same long value", source.get(), result.get());
    }

    @Test
    public void toAtomicLong_negativeAndLargeValues_preservesExactValue() {
        AtomicLong source = new AtomicLong(Long.MAX_VALUE);
        AtomicLong resultMax = AtomicLongConversions.toAtomicLong(source, converter);
        assertEquals(Long.MAX_VALUE, resultMax.get());

        source = new AtomicLong(Long.MIN_VALUE);
        AtomicLong resultMin = AtomicLongConversions.toAtomicLong(source, converter);
        assertEquals(Long.MIN_VALUE, resultMin.get());
    }

    @Test
    public void toAtomicLong_originalIsModifiedLater_resultRemainsUnchanged() {
        AtomicLong source = new AtomicLong(10L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(source, converter);

        // Change the original after conversion
        source.set(999L);

        // The result must still hold the original value
        assertEquals("Result must remain unchanged after source modification", 10L, result.get());
    }

    @Test
    public void toAtomicLong_nullSource_throwsNullPointerException() {
        thrown.expect(NullPointerException.class);
        AtomicLongConversions.toAtomicLong(null, converter);
    }

    @Test
    public void toAtomicLong_wrongType_throwsClassCastException() {
        thrown.expect(ClassCastException.class);
        AtomicLongConversions.toAtomicLong("not an AtomicLong", converter);
    }

    /*** Tests for toLocalTime ***/

    @Test
    public void toLocalTime_typicalValue_matchesLongConversionsResult() {
        // Use a known epoch‑millisecond value that maps to a LocalTime.
        // For demonstration we use 0 (midnight) and 12:34:56.789.
        long[] testValues = {0L, 12_345_678L, 86_399_999L}; // milliseconds within a day

        for (long millis : testValues) {
            AtomicLong source = new AtomicLong(millis);
            LocalTime expected = LongConversions.toLocalTime(millis, converter);
            LocalTime actual = AtomicLongConversions.toLocalTime(source, converter);
            assertEquals("Converted LocalTime must match LongConversions for millis = " + millis,
                         expected, actual);
        }
    }

    @Test
    public void toLocalTime_negativeMillis_behavesConsistently() {
        // Negative values represent times before midnight of the reference day.
        long negativeMillis = -1_000L; // 1 second before midnight
        AtomicLong source = new AtomicLong(negativeMillis);
        LocalTime expected = LongConversions.toLocalTime(negativeMillis, converter);
        LocalTime actual = AtomicLongConversions.toLocalTime(source, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void toLocalTime_nullSource_throwsNullPointerException() {
        thrown.expect(NullPointerException.class);
        AtomicLongConversions.toLocalTime(null, converter);
    }

    @Test
    public void toLocalTime_wrongType_throwsClassCastException() {
        thrown.expect(ClassCastException.class);
        AtomicLongConversions.toLocalTime("not an AtomicLong", converter);
    }
}
