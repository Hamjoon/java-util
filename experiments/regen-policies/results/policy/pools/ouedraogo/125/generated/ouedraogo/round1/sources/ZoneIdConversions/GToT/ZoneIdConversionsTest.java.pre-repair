package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.Map;
import java.util.TimeZone;

/**
 * Comprehensive JUnit‑4 test suite for {@link ZoneIdConversions}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>basic happy‑path conversions for each static method</li>
 *   <li>null input handling (expecting {@link NullPointerException})</li>
 *   <li>invalid type handling (expecting {@link ClassCastException})</li>
 *   <li>edge cases such as offset‑only ZoneIds and DST‑sensitive zones</li>
 * </ul>
 *
 * <p>All methods in {@code ZoneIdConversions} are package‑private static, therefore the test class
 * resides in the same package to gain access without reflection.
 *
 * <p>Note: the {@code Converter} argument is not used by the implementation, so we safely pass {@code null}
 * (or a dummy instance if desired).  The class under test does not require any special setup.
 */
public class ZoneIdConversionsTest {

    // -------------------------------------------------------------------------
    //  Shared test fixtures
    // -------------------------------------------------------------------------

    private Converter dummyConverter;   // not used by ZoneIdConversions

    @Before
    public void setUp() {
        // The Converter constructor accepts a ConverterOptions argument that may be null.
        // Passing null is sufficient because the methods under test never touch the instance.
        dummyConverter = new Converter(null);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // -------------------------------------------------------------------------
    //  toMap(...) tests
    // -------------------------------------------------------------------------

    @Test
    public void testToMap_basicUtc() {
        ZoneId utc = ZoneId.of("UTC");
        Map<String, Object> result = ZoneIdConversions.toMap(utc, dummyConverter);

        // The map must contain exactly one entry: "zone" -> "UTC"
        assert result != null : "Result map must not be null";
        assert result.size() == 1 : "Result map should contain exactly one entry";
        assert "UTC".equals(result.get("zone")) : "Map should contain the zone ID string";
    }

    @Test
    public void testToMap_offsetOnlyZone() {
        // ZoneOffset implements ZoneId – this exercises a non‑named zone.
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(5, 30); // +05:30
        Map<String, Object> result = ZoneIdConversions.toMap(offset, dummyConverter);

        assert result != null;
        assert result.size() == 1;
        assert "+05:30".equals(result.get("zone"));
    }

    @Test
    public void testToMap_nullFrom_throwsNPE() {
        thrown.expect(NullPointerException.class);
        ZoneIdConversions.toMap(null, dummyConverter);
    }

    @Test
    public void testToMap_invalidType_throwsClassCastException() {
        thrown.expect(ClassCastException.class);
        ZoneIdConversions.toMap("not a ZoneId", dummyConverter);
    }

    // -------------------------------------------------------------------------
    //  toTimeZone(...) tests
    // -------------------------------------------------------------------------

    @Test
    public void testToTimeZone_basicUtc() {
        ZoneId utc = ZoneId.of("UTC");
        TimeZone tz = ZoneIdConversions.toTimeZone(utc, dummyConverter);

        assert tz != null;
        assert "UTC".equals(tz.getID());
        assert tz.getRawOffset() == 0;
    }

    @Test
    public void testToTimeZone_offsetOnlyZone() {
        ZoneOffset offset = ZoneOffset.ofHours(2); // +02:00
        TimeZone tz = ZoneIdConversions.toTimeZone(offset, dummyConverter);

        assert tz != null;
        // TimeZone ID for a fixed offset is formatted as "GMT+02:00"
        assert "GMT+02:00".equals(tz.getID());
        assert tz.getRawOffset() == 2 * 60 * 60 * 1000;
    }

    @Test
    public void testToTimeZone_nullFrom_throwsNPE() {
        thrown.expect(NullPointerException.class);
        ZoneIdConversions.toTimeZone(null, dummyConverter);
    }

    @Test
    public void testToTimeZone_invalidType_throwsClassCastException() {
        thrown.expect(ClassCastException.class);
        ZoneIdConversions.toTimeZone(12345, dummyConverter);
    }

    // -------------------------------------------------------------------------
    //  toZoneOffset(...) tests
    // -------------------------------------------------------------------------

    @Test
    public void testToZoneOffset_basicUtc() {
        ZoneId utc = ZoneId.of("UTC");
        ZoneOffset offset = ZoneIdConversions.toZoneOffset(utc, dummyConverter);

        assert offset != null;
        assert ZoneOffset.UTC.equals(offset);
    }

    @Test
    public void testToZoneOffset_offsetOnlyZone() {
        ZoneOffset expected = ZoneOffset.ofHoursMinutes(-4, -15); // -04:15
        ZoneOffset actual = ZoneIdConversions.toZoneOffset(expected, dummyConverter);

        assert actual != null;
        assert expected.equals(actual);
    }

    @Test
    public void testToZoneOffset_dstSensitiveZone() {
        // America/New_York switches between -05:00 and -04:00 depending on DST.
        ZoneId ny = ZoneId.of("America/New_York");
        ZoneOffset expected = ny.getRules().getOffset(Instant.now());
        ZoneOffset actual   = ZoneIdConversions.toZoneOffset(ny, dummyConverter);

        assert expected.equals(actual) : "Offset should match the current rules for New York";
    }

    @Test
    public void testToZoneOffset_nullFrom_throwsNPE() {
        thrown.expect(NullPointerException.class);
        ZoneIdConversions.toZoneOffset(null, dummyConverter);
    }

    @Test
    public void testToZoneOffset_invalidType_throwsClassCastException() {
        thrown.expect(ClassCastException.class);
        ZoneIdConversions.toZoneOffset(new Object(), dummyConverter);
    }
}
