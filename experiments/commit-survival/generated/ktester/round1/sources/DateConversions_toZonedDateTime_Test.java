package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toZonedDateTime_Test {

    /**
     * A sample {@link Date} used by the tests.
     */
    private Date sampleDate;

    /**
     * Mocked {@link Converter} that supplies conversion options.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Mocked {@link ConverterOptions} returned by the converter.
     */
    @Mock
    private ConverterOptions mockOptions;

    /**
     * Minimal implementation of {@link Converter} that returns a
     * {@link ConverterOptions} exposing a single {@link ZoneId}.
     */
    private static final class TestConverter implements Converter {

        private final ConverterOptions options;

        TestConverter(ZoneId zone) {
            this.options = (zone == null) ? null : new TestOptions(zone);
        }

        @Override
        public ConverterOptions getOptions() {
            return options;
        }
    }

    /**
     * Minimal implementation of {@link ConverterOptions} exposing only the
     * {@link ZoneId}.  The real library may have many more methods – they are
     * irrelevant for the {@code toZonedDateTime} conversion.
     */
    private static final class TestOptions implements ConverterOptions {

        private final ZoneId zoneId;

        TestOptions(ZoneId zoneId) {
            this.zoneId = zoneId;
        }

        @Override
        public ZoneId getZoneId() {
            return zoneId;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (necessary when not using @InjectMocks).
        MockitoAnnotations.openMocks(this);
        // A deterministic instant: 2023‑01‑01T12:34:56.789Z
        long epochMilli = 1672576496789L;
        sampleDate = new Date(epochMilli);
        // By default the mock will return UTC; individual tests can override.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
    }

    /**
     * Provides all successful conversion scenarios described in the JSON.
     * Each {@link Arguments} contains:
     * <ul>
     *   <li>a {@link Date} (or subclass) instance</li>
     *   <li>a {@link ZoneId} to be returned by the {@link Converter}</li>
     *   <li>the expected {@link ZonedDateTime}</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> validConversionProvider() {
        return Stream.of(// epoch start, UTC
        org.junit.jupiter.params.provider.Arguments.of(new Date(0L), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.of("UTC"))), // specific instant, America/New_York (DST aware)
        org.junit.jupiter.params.provider.Arguments.of(new Date(1678888245123L), ZoneId.of("America/New_York"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(1678888245123L), ZoneId.of("America/New_York"))), // java.sql.Timestamp treated as Date, Europe/Paris
        org.junit.jupiter.params.provider.Arguments.of(new java.sql.Timestamp(1609459200000L), ZoneId.of("Europe/Paris"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(1609459200000L), ZoneId.of("Europe/Paris"))), // Long.MIN_VALUE, UTC
        org.junit.jupiter.params.provider.Arguments.of(new Date(Long.MIN_VALUE), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.MIN_VALUE), ZoneId.of("UTC"))), // Long.MAX_VALUE, UTC
        org.junit.jupiter.params.provider.Arguments.of(new Date(Long.MAX_VALUE), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.MAX_VALUE), ZoneId.of("UTC"))), // -1 ms, UTC
        org.junit.jupiter.params.provider.Arguments.of(new Date(-1L), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(-1L), ZoneId.of("UTC"))), // Fixed‑offset zone +02:00
        org.junit.jupiter.params.provider.Arguments.of(new Date(0L), ZoneId.of("+02:00"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.of("+02:00"))), // Half‑hour offset Asia/Kolkata
        org.junit.jupiter.params.provider.Arguments.of(new Date(1678888245123L), ZoneId.of("Asia/Kolkata"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(1678888245123L), ZoneId.of("Asia/Kolkata"))), // America/Los_Angeles (DST rules)
        org.junit.jupiter.params.provider.Arguments.of(new Date(1678888245123L), ZoneId.of("America/Los_Angeles"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(1678888245123L), ZoneId.of("America/Los_Angeles"))), // java.sql.Date subclass, Europe/London
        org.junit.jupiter.params.provider.Arguments.of(new java.sql.Date(1609459200000L), ZoneId.of("Europe/London"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(1609459200000L), ZoneId.of("Europe/London"))));
    }

    @Test
    void toZonedDateTime_usesUtcZoneByDefault() {
        // GIVEN
        // mockConverter already configured to return UTC in setupBeforeEach()
        // WHEN
        ZonedDateTime result = DateConversions.toZonedDateTime(sampleDate, mockConverter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(ZoneId.of("UTC"), result.getZone(), "Zone should be UTC");
        assertEquals(sampleDate.getTime(), result.toInstant().toEpochMilli(), "Instant milliseconds must match the original Date");
    }

    @Test
    void toZonedDateTime_respectsProvidedZoneId() {
        // GIVEN
        ZoneId newYork = ZoneId.of("America/New_York");
        when(mockOptions.getZoneId()).thenReturn(newYork);
        // WHEN
        ZonedDateTime result = DateConversions.toZonedDateTime(sampleDate, mockConverter);
        // THEN
        assertEquals(newYork, result.getZone(), "Zone should be America/New_York");
        assertEquals(sampleDate.getTime(), result.toInstant().toEpochMilli(), "Instant milliseconds must remain unchanged irrespective of zone");
    }

    @Test
    void toZonedDateTime_nullFrom_throwsNullPointerException() {
        // GIVEN
        Date nullDate = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toZonedDateTime(nullDate, mockConverter), "Passing null as the source Date should result in NPE");
    }

    @Test
    void toZonedDateTime_nonDateObject_throwsClassCastException() {
        Converter converter = new TestConverter(ZoneId.of("UTC"));
        Object notADate = "2023-01-01";
        assertThrows(ClassCastException.class, () -> DateConversions.toZonedDateTime(notADate, converter), "Passing a non‑Date object should cause a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] from={0}, zone={1}")
    @MethodSource("validConversionProvider")
    void toZonedDateTime_successfulConversions(Date from, ZoneId zone, ZonedDateTime expected) {
        // GIVEN
        Converter converter = new TestConverter(zone);
        // WHEN
        ZonedDateTime result = DateConversions.toZonedDateTime(from, converter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result, "Converted ZonedDateTime does not match expected value");
        // The instant (epoch milli) must be identical irrespective of zone.
        assertEquals(from.getTime(), result.toInstant().toEpochMilli(), "Instant milliseconds must be preserved");
    }

    @Test
    void toZonedDateTime_nullConverter_throwsNullPointerException() {
        Date date = new Date(0L);
        assertThrows(NullPointerException.class, () -> DateConversions.toZonedDateTime(date, null), "Passing null Converter should throw NPE");
    }

    @Test
    void toZonedDateTime_nullOptions_throwsNullPointerException() {
        // Converter that returns null options
        Converter converter = new TestConverter(null);
        Date date = new Date(0L);
        assertThrows(NullPointerException.class, () -> DateConversions.toZonedDateTime(date, converter), "Converter returning null options should cause NPE when getZoneId() is accessed");
    }

    @Test
    void toZonedDateTime_invalidZoneId_throwsZoneRulesException() {
        // ZoneId.of will itself throw a DateTimeException (ZoneRulesException) for an unknown ID.
        assertThrows(ZoneRulesException.class, () -> {
            // throws at creation
            ZoneId invalid = ZoneId.of("Invalid/Zone");
            Converter converter = new TestConverter(invalid);
            DateConversions.toZonedDateTime(new Date(0L), converter);
        }, "An invalid ZoneId must result in ZoneRulesException");
    }

    @Test
    void toZonedDateTime_atomicLongObject_throwsClassCastException() {
        Converter converter = new TestConverter(ZoneId.of("UTC"));
        Object atomicLong = new java.util.concurrent.atomic.AtomicLong(123L);
        assertThrows(ClassCastException.class, () -> DateConversions.toZonedDateTime(atomicLong, converter), "Passing an AtomicLong should cause a ClassCastException");
    }
}
