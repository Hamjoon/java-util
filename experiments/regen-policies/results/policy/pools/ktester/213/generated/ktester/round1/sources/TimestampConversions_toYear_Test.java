package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toYear_Test {

    /**
     * Converter used for all tests – created once for the whole test class.
     */
    private static Converter converter;

    /**
     * Converter used for the normal conversion tests – created once for the whole test class.
     */
    private static Converter defaultConverter;

    class OptionsStub {

        ZoneId getZoneId() {
            return null;
        }
    }

    class OptionsStub {

        ZoneId getZoneId() {
            return null;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the Converter with the default options (system default zone).
        defaultConverter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper that builds a {@link Timestamp} from a date string and time components.
     */
    private static Timestamp createTimestamp(String dateStr, int hour, int minute, int second, int nanos) {
        return Timestamp.from(LocalDate.parse(dateStr).atTime(hour, minute, second, nanos).atZone(ZoneId.systemDefault()).toInstant());
    }

    private static java.util.stream.Stream<NormalCase> normalCases() {
        // 1. Standard conversion – UTC
        Timestamp ts1 = Timestamp.valueOf("2023-05-15 10:30:00");
        // 2. Year roll‑over when using America/New_York (UTC‑05:00)
        Timestamp ts2 = Timestamp.valueOf("2023-01-01 00:30:00");
        // 3. Leap‑year date – zone does not affect the year
        Timestamp ts3 = Timestamp.valueOf("2020-02-29 12:00:00");
        return java.util.stream.Stream.of(new NormalCase(ts1, ZoneId.of("UTC"), Year.of(2023), "Standard conversion where the timestamp is interpreted in UTC; the local date is 2023‑05‑15, so the resulting Year should be 2023."), new NormalCase(ts2, ZoneId.of("America/New_York"), Year.of(2022), "Timestamp is just after midnight UTC on 2023‑01‑01. In America/New_York the local time is 2022‑12‑31 19:30, therefore the local date belongs to year 2022."), new NormalCase(ts3, ZoneId.of("Asia/Tokyo"), Year.of(2020), "Leap‑year date; regardless of zone the local date remains 2020‑02‑29, so the Year should be 2020."));
    }

    @Test
    void testToYearVariousDates() {
        // Classic historic date
        Timestamp ts1 = createTimestamp("1888-01-02", 9, 15, 30, 333_333_333);
        assertEquals(Year.of(1888), TimestampConversions.toYear(ts1, defaultConverter), "1888‑01‑02 should map to Year 1888");
        // End‑of‑year date
        Timestamp ts2 = createTimestamp("1969-12-31", 23, 59, 59, 999_999_999);
        assertEquals(Year.of(1969), TimestampConversions.toYear(ts2, defaultConverter), "1969‑12‑31 should map to Year 1969");
        // The Unix epoch start (still year 1970)
        Timestamp ts3 = createTimestamp("1970-01-01", 0, 0, 0, 0);
        assertEquals(Year.of(1970), TimestampConversions.toYear(ts3, defaultConverter), "1970‑01‑01 should map to Year 1970");
        // A recent date
        Timestamp ts4 = createTimestamp("2023-06-15", 21, 5, 55, 888_888_888);
        assertEquals(Year.of(2023), TimestampConversions.toYear(ts4, defaultConverter), "2023‑06‑15 should map to Year 2023");
    }

    @Test
    void testToYearWithDifferentTimeZones() {
        // Override the Converter's zone to UTC and verify conversion still yields the correct year.
        Converter utcConverter = new Converter(new DefaultConverterOptions().withZoneId(// assuming DefaultConverterOptions supports withZoneId
        ZoneId.of("UTC")));
        Timestamp ts = createTimestamp("2000-12-31", 23, 0, 0, 0);
        assertEquals(Year.of(2000), TimestampConversions.toYear(ts, utcConverter), "With UTC zone the year should still be 2000");
    }

    @Test
    void testToYearNullInput() {
        // According to the implementation a ClassCastException is thrown
        // when the method attempts to cast null to Timestamp.
        assertThrows(ClassCastException.class, () -> TimestampConversions.toYear(null, defaultConverter), "Passing null should result in a ClassCastException because the method expects a Timestamp");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalCases")
    void testToYearNormalConversions(NormalCase c) {
        // Build a Converter that uses the required ZoneId.
        Converter conv = new Converter(new DefaultConverterOptions().withZoneId(c.zone()));
        Year result = TimestampConversions.toYear(c.ts(), conv);
        assertEquals(c.expected(), result, c.description());
    }

    @Test
    void testToYearWrongType() {
        // Passing a java.util.Date (not a Timestamp) should cause a ClassCastException.
        assertThrows(ClassCastException.class, () -> TimestampConversions.toYear(new java.util.Date(), defaultConverter), "Passing a java.util.Date should cause a ClassCastException");
    }

    /* --------------------------------------------------------------
     *  Null converter handling
     * -------------------------------------------------------------- */
    @Test
    void testToYearNullConverter() {
        Timestamp ts = Timestamp.valueOf("2023-01-01 00:00:00");
        assertThrows(NullPointerException.class, () -> TimestampConversions.toYear(ts, null), "Calling toYear with a null Converter should throw NullPointerException");
    }

    /* --------------------------------------------------------------
     *  Converter with null options
     * -------------------------------------------------------------- */
    @Test
    void testToYearConverterOptionsNull() {
        // Mock a Converter whose getOptions() returns null.
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(null);
        Timestamp ts = Timestamp.valueOf("2023-06-15 12:00:00");
        assertThrows(NullPointerException.class, () -> TimestampConversions.toYear(ts, mockConverter), "If converter.getOptions() returns null, a NullPointerException should be thrown");
    }

    /* --------------------------------------------------------------
     *  Converter with null ZoneId
     * -------------------------------------------------------------- */
    @Test
    void testToYearConverterZoneIdNull() {
        // Mock the options object to return a null ZoneId.
        Object mockOptions = mock(Object.class);
        // Assuming the options class has a method getZoneId()
        // We use a generic mock and configure the method via reflection.
        // For simplicity, we create a concrete stub that returns null.
        class OptionsStub {

            ZoneId getZoneId() {
                return null;
            }
        }
        OptionsStub stub = new OptionsStub();
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(stub);
        Timestamp ts = Timestamp.valueOf("2023-12-31 23:59:59");
        assertThrows(NullPointerException.class, () -> TimestampConversions.toYear(ts, mockConverter), "When converter.getOptions().getZoneId() returns null, a NullPointerException should be thrown");
    }
}
