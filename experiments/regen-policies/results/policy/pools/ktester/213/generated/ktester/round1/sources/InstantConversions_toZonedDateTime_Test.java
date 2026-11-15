package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toZonedDateTime_Test {

    private static Converter converter;

    private static final ZoneId TEST_ZONE = ZoneId.of("America/New_York");

    /**
     * Simple holder for a single test scenario.
     */
    private static final class TestCase {

        final String description;

        // the value passed as the first argument
        final Object from;

        // the Converter (may be a mock or null)
        final Converter converter;

        // expected result for successful cases
        final ZonedDateTime expected;

        // non‑null for error cases
        final Class<? extends Throwable> expectedException;

        TestCase(String description, Object from, Converter converter, ZonedDateTime expected, Class<? extends Throwable> expectedException) {
            this.description = description;
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.expectedException = expectedException;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions is a mutable holder for options; we only need the zone setting.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(TEST_ZONE);
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        converter = null;
    }

    /**
     * Provides all test scenarios defined in the JSON specification.
     */
    private static Stream<Arguments> provideTestCases() {
        // Helper to create a real Converter with the supplied ZoneId (may be null)
        java.util.function.Function<ZoneId, Converter> realConverter = zoneId -> {
            ConverterOptions options = new ConverterOptions();
            // allow null zoneId for specific tests
            options.setZoneId(zoneId);
            return new Converter(options);
        };
        // Helper to create a mock Converter whose getOptions() returns null
        Converter converterWithNullOptions = mock(Converter.class);
        when(converterWithNullOptions.getOptions()).thenReturn(null);
        return Stream.of(// -----------------------------------------------------------------
        // Positive conversion cases
        // -----------------------------------------------------------------
        Arguments.of(new TestCase("Converts a valid Instant to ZonedDateTime using UTC zone", Instant.parse("2023-01-01T12:34:56Z"), realConverter.apply(ZoneId.of("UTC")), Instant.parse("2023-01-01T12:34:56Z").atZone(ZoneId.of("UTC")), null)), Arguments.of(new TestCase("Converts a valid Instant to ZonedDateTime using America/New_York", Instant.parse("2023-06-15T08:00:00Z"), realConverter.apply(ZoneId.of("America/New_York")), Instant.parse("2023-06-15T08:00:00Z").atZone(ZoneId.of("America/New_York")), null)), Arguments.of(new TestCase("Converts the epoch instant to Asia/Tokyo (+09:00)", Instant.EPOCH, realConverter.apply(ZoneId.of("Asia/Tokyo")), Instant.EPOCH.atZone(ZoneId.of("Asia/Tokyo")), null)), Arguments.of(new TestCase("Converts Instant.MAX to UTC", Instant.MAX, realConverter.apply(ZoneId.of("UTC")), Instant.MAX.atZone(ZoneId.of("UTC")), null)), Arguments.of(new TestCase("Converts Instant.MIN to UTC", Instant.MIN, realConverter.apply(ZoneId.of("UTC")), Instant.MIN.atZone(ZoneId.of("UTC")), null)), Arguments.of(new TestCase("Instant just before DST spring‑forward in Europe/Paris (offset +01:00)", Instant.parse("2023-03-26T00:30:00Z"), realConverter.apply(ZoneId.of("Europe/Paris")), Instant.parse("2023-03-26T00:30:00Z").atZone(ZoneId.of("Europe/Paris")), null)), Arguments.of(new TestCase("Instant just after DST fall‑back in Europe/Paris (offset +02:00)", Instant.parse("2023-10-29T00:30:00Z"), realConverter.apply(ZoneId.of("Europe/Paris")), Instant.parse("2023-10-29T00:30:00Z").atZone(ZoneId.of("Europe/Paris")), null)), Arguments.of(new TestCase("Fixed offset zone (+02:00) – instant shifted by two hours", Instant.parse("2023-01-01T12:00:00Z"), realConverter.apply(ZoneId.of("+02:00")), Instant.parse("2023-01-01T12:00:00Z").atZone(ZoneId.of("+02:00")), null)), // -----------------------------------------------------------------
        // Negative / exceptional cases
        // -----------------------------------------------------------------
        Arguments.of(new TestCase("Null 'from' argument triggers NullPointerException", null, realConverter.apply(ZoneId.of("UTC")), null, NullPointerException.class)), Arguments.of(new TestCase("Non‑Instant 'from' (String) triggers ClassCastException", "2023-01-01T12:34:56Z", realConverter.apply(ZoneId.of("UTC")), null, ClassCastException.class)), Arguments.of(new TestCase("Converter with null ZoneId causes NullPointerException", Instant.now(), // options.zoneId == null
        realConverter.apply(null), null, NullPointerException.class)), Arguments.of(new TestCase("Null Converter argument triggers NullPointerException", Instant.now(), null, null, NullPointerException.class)), Arguments.of(new TestCase("Converter whose getOptions() returns null triggers NullPointerException", Instant.now(), converterWithNullOptions, null, NullPointerException.class)));
    }

    @Test
    void toZonedDateTime_convertsInstantUsingConverterZone() {
        // GIVEN
        Instant instant = Instant.parse("2023-03-15T12:34:56Z");
        // WHEN
        ZonedDateTime result = InstantConversions.toZonedDateTime(instant, converter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(TEST_ZONE, result.getZone(), "Zone should match the one configured in ConverterOptions");
        // The instant represents the same instant in time, so the epoch second must be unchanged.
        assertEquals(instant.getEpochSecond(), result.toInstant().getEpochSecond(), "Resulting ZonedDateTime must represent the same instant");
    }

    /**
     * Executes each {@link TestCase}.  Successful conversions are verified by comparing
     * the resulting {@link ZonedDateTime} with the expected value (same instant and zone).
     * Expected exception cases are verified with {@link Assertions#assertThrows}.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideTestCases")
    void toZonedDateTime_behaviour(TestCase tc) {
        if (tc.expectedException != null) {
            // Exception‑expectation branch
            assertThrows(tc.expectedException, () -> InstantConversions.toZonedDateTime(tc.from, tc.converter), tc.description);
        } else {
            // Normal conversion branch
            ZonedDateTime actual = InstantConversions.toZonedDateTime(tc.from, tc.converter);
            assertNotNull(actual, "Result must not be null – " + tc.description);
            assertEquals(tc.expected.getZone(), actual.getZone(), "Zone mismatch – " + tc.description);
            assertEquals(tc.expected.toInstant(), actual.toInstant(), "Instant (epoch second/nano) mismatch – " + tc.description);
        }
    }
}
