package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toLocalDateTime_Test {

    @Mock
    private Converter // mocked Converter
    mockConverter;

    @Mock
    private ConverterOptions // mocked ConverterOptions returned by the Converter
    mockOptions;

    /**
     * Minimal {@link ConverterOptions} implementation exposing only {@code getZoneId()}.
     */
    private static class SimpleOptions implements ConverterOptions {

        private final ZoneId zoneId;

        SimpleOptions(ZoneId zoneId) {
            this.zoneId = zoneId;
        }

        @Override
        public ZoneId getZoneId() {
            return zoneId;
        }
        // The real interface contains many other methods – they are not needed here.
    }

    /**
     * Simple {@link Converter} that returns a {@link SimpleOptions} instance.
     */
    private static class TestConverter implements Converter {

        private final ConverterOptions options;

        TestConverter(ZoneId zoneId) {
            this.options = new SimpleOptions(zoneId);
        }

        @Override
        public ConverterOptions getOptions() {
            return options;
        }

        // All other methods of the real Converter are irrelevant for these tests.
        // Throwing UnsupportedOperationException makes any accidental call obvious.
        @Override
        public Object convert(Object from, Class<?> toClass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object convert(Object from, Class<?> toClass, ConverterOptions options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T convert(Object from, Class<T> toClass, T defaultValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T convert(Object from, Class<T> toClass, T defaultValue, ConverterOptions options) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Converter whose {@code getOptions()} returns {@code null}.
     */
    private static class ConverterWithNullOptions implements Converter {

        @Override
        public ConverterOptions getOptions() {
            return null;
        }

        @Override
        public Object convert(Object from, Class<?> toClass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object convert(Object from, Class<?> toClass, ConverterOptions options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T convert(Object from, Class<T> toClass, T defaultValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T convert(Object from, Class<T> toClass, T defaultValue, ConverterOptions options) {
            throw new UnsupportedOperationException();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy: converter.getOptions() -> mockOptions
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference
        reset(mockConverter, mockOptions);
    }

    private static Stream<Arguments> normalConversionCases() {
        return Stream.of(// New York → Paris (6‑hour offset)
        Arguments.of(ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("America/New_York")), new TestConverter(ZoneId.of("Europe/Paris")), LocalDateTime.of(2023, 5, 15, 16, 30), "Converts a ZonedDateTime from New York to Paris zone; the resulting LocalDateTime should reflect the same instant expressed in the target zone (6‑hour offset)."), // UTC → UTC (no change)
        Arguments.of(ZonedDateTime.of(2022, 12, 31, 23, 0, 0, 0, ZoneId.of("UTC")), new TestConverter(ZoneId.of("UTC")), LocalDateTime.of(2022, 12, 31, 23, 0), "When the source and target zones are identical, the LocalDateTime returned should be identical to the original's local part."));
    }

    private static Stream<Arguments> edgeCases() {
        return Stream.of(// Spring‑forward DST (NY 01:30 EST → UTC 06:30)
        Arguments.of(ZonedDateTime.of(2023, 3, 12, 1, 30, 0, 0, ZoneId.of("America/New_York")), new TestConverter(ZoneId.of("UTC")), LocalDateTime.of(2023, 3, 12, 6, 30), "Spring‑forward DST transition"), // Fall‑back DST (NY 01:30 EDT → UTC 05:30)
        Arguments.of(ZonedDateTime.of(2023, 11, 5, 1, 30, 0, 0, ZoneId.of("America/New_York")), new TestConverter(ZoneId.of("UTC")), LocalDateTime.of(2023, 11, 5, 5, 30), "Fall‑back DST transition"), // Minimum supported year
        Arguments.of(ZonedDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), new TestConverter(ZoneId.of("UTC")), LocalDateTime.of(1, 1, 1, 0, 0), "Earliest supported ISO‑8601 year (0001)"), // Maximum supported year
        Arguments.of(ZonedDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999, ZoneId.of("UTC")), new TestConverter(ZoneId.of("UTC")), LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999), "Maximum supported ISO‑8601 year (9999)"), // Maximum positive offset (+14h) → UTC previous day
        Arguments.of(ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("Pacific/Kiritimati")), new TestConverter(ZoneId.of("UTC")), LocalDateTime.of(2023, 5, 14, 20, 30), "Source zone with the maximum positive offset (+14 hours)"), // Same zone but distinct ZoneId instances
        Arguments.of(ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("Europe/Paris")), new TestConverter(ZoneId.of("Europe/Paris")), LocalDateTime.of(2023, 5, 15, 10, 30), "Source and target zones are the same but represented by distinct ZoneId instances"));
    }

    private static Stream<Arguments> invalidInputCases() {
        return Stream.of(// Null 'from'
        Arguments.of(null, new TestConverter(ZoneId.of("UTC")), NullPointerException.class, "Passing a null 'from' argument should cause a NullPointerException when the method attempts to cast."), // Null converter
        Arguments.of(ZonedDateTime.now(), null, NullPointerException.class, "A null converter leads to a NullPointerException when accessing converter.getOptions()."), // Wrong type: Instant
        Arguments.of(Instant.now(), new TestConverter(ZoneId.of("UTC")), ClassCastException.class, "Providing an object of another type (Instant) should result in a ClassCastException."), // Wrong type: String
        Arguments.of("2023-05-15T10:30:00Z", new TestConverter(ZoneId.of("UTC")), ClassCastException.class, "Supplying a String instead of a ZonedDateTime should also trigger a ClassCastException."));
    }

    @Test
    void toLocalDateTime_AdjustsToConverterZone() {
        // GIVEN – a ZonedDateTime in New York and a Converter whose target zone is UTC
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 10, 15, 30, 0, 0, ZoneId.of("America/New_York"));
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN – the conversion method is invoked
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, mockConverter);
        // THEN – the result is the same instant expressed in UTC's local date‑time
        LocalDateTime expected = source.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_NoZoneChangeWhenZonesMatch() {
        // GIVEN – a ZonedDateTime and a Converter whose target zone matches the source zone
        ZonedDateTime source = ZonedDateTime.of(2022, 12, 1, 8, 45, 0, 0, ZoneId.of("Europe/Paris"));
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("Europe/Paris"));
        // WHEN
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, mockConverter);
        // THEN – the local date‑time is unchanged
        LocalDateTime expected = source.toLocalDateTime();
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("normalConversionCases")
    void toLocalDateTime_NormalConversions(ZonedDateTime source, Converter converter, LocalDateTime expected, String description) {
        // WHEN
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, converter);
        // THEN
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("edgeCases")
    void toLocalDateTime_EdgeCases(ZonedDateTime source, Converter converter, LocalDateTime expected, String description) {
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidInputCases")
    void toLocalDateTime_InvalidInputs(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        // Use assertThrows to verify the expected exception type
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toLocalDateTime(from, converter), description);
    }

    @Test
    void toLocalDateTime_OriginalZonedDateTimeRemainsUnchanged() {
        // GIVEN – a source ZonedDateTime and a converter to a different zone
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("America/New_York"));
        Converter converter = new TestConverter(ZoneId.of("Europe/Paris"));
        // Capture a copy for later comparison
        ZonedDateTime before = source;
        // WHEN – conversion is performed
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, converter);
        // THEN – result is as expected and source is unchanged
        LocalDateTime expected = source.withZoneSameInstant(ZoneId.of("Europe/Paris")).toLocalDateTime();
        assertEquals(expected, result);
        assertEquals(before, source, "The original ZonedDateTime must remain unchanged after conversion.");
    }

    @Test
    void toLocalDateTime_ConverterOptionsNull_ThrowsNPE() {
        // GIVEN – a source ZonedDateTime and a converter whose getOptions() returns null
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("UTC"));
        Converter converter = new ConverterWithNullOptions();
        // WHEN / THEN – NullPointerException because options are null
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void toLocalDateTime_ZoneIdNull_ThrowsNPE() {
        // GIVEN – a converter that provides options with a null ZoneId
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("UTC"));
        // options.getZoneId() == null
        Converter converter = new TestConverter(null);
        // WHEN / THEN – NullPointerException when withZoneSameInstant(null) is invoked
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    void toLocalDateTime_AdjustsToConverterZone_WithMocks() {
        // GIVEN – a ZonedDateTime in New York and a mocked Converter whose target zone is UTC
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 10, 15, 30, 0, 0, ZoneId.of("America/New_York"));
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN – the conversion method is invoked
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, mockConverter);
        // THEN – the result is the same instant expressed in UTC's local date‑time
        LocalDateTime expected = source.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_NoZoneChangeWhenZonesMatch_WithMocks() {
        // GIVEN – a ZonedDateTime and a mocked Converter whose target zone matches the source zone
        ZonedDateTime source = ZonedDateTime.of(2022, 12, 1, 8, 45, 0, 0, ZoneId.of("Europe/Paris"));
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("Europe/Paris"));
        // WHEN
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, mockConverter);
        // THEN – the local date‑time is unchanged
        LocalDateTime expected = source.toLocalDateTime();
        assertEquals(expected, result);
    }
}
