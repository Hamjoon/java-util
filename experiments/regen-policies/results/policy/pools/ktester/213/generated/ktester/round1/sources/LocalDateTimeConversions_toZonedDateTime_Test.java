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
class LocalDateTimeConversions_toZonedDateTime_Test {

    @Mock
    private Converter converter;

    @Mock
    private ConverterOptions options;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock Converter to return the mocked options
        when(converter.getOptions()).thenReturn(options);
        // Use a deterministic ZoneId for repeatable tests
        when(options.getZoneId()).thenReturn(ZoneId.of("America/New_York"));
    }

    /**
     * Configures the mock {@code converter} to return a {@code ZoneId} from {@code options}.
     */
    private void configureConverterWithZone(ZoneId zone) {
        when(options.getZoneId()).thenReturn(zone);
    }

    /**
     * Configures the mock {@code converter} to return {@code null} for {@code getOptions()}.
     */
    private void configureConverterWithNullOptions() {
        when(converter.getOptions()).thenReturn(null);
    }

    /**
     * Configures the mock {@code converter} to throw the supplied exception when {@code getOptions()} is called.
     */
    private void configureConverterThrowingOnGetOptions(RuntimeException ex) {
        when(converter.getOptions()).thenThrow(ex);
    }

    /**
     * Configures the mock {@code options} to throw the supplied exception when {@code getZoneId()} is called.
     */
    private void configureOptionsThrowingOnGetZoneId(RuntimeException ex) {
        when(options.getZoneId()).thenThrow(ex);
    }

    private static Stream<Arguments> explicitZoneIdProvider() {
        return Stream.of(Arguments.of(LocalDateTime.of(2023, 5, 10, 12, 30, 45), ZoneId.of("America/New_York"), ZonedDateTime.of(LocalDateTime.of(2023, 5, 10, 12, 30, 45), ZoneId.of("America/New_York"))), Arguments.of(LocalDateTime.of(2020, 2, 29, 0, 0), ZoneId.of("UTC"), ZonedDateTime.of(LocalDateTime.of(2020, 2, 29, 0, 0), ZoneId.of("UTC"))));
    }

    private static Stream<Arguments> extremeValuesProvider() {
        return Stream.of(Arguments.of(LocalDateTime.MIN, ZoneId.of("UTC")), Arguments.of(LocalDateTime.MAX, ZoneId.of("UTC")));
    }

    @Test
    void testToZonedDateTime_ConvertsLocalDateTimeWithZone() {
        // given
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 15, 30, 45);
        ZonedDateTime expected = ZonedDateTime.of(ldt, ZoneId.of("America/New_York"));
        // when
        ZonedDateTime actual = LocalDateTimeConversions.toZonedDateTime(ldt, converter);
        // then
        assertEquals(expected, actual, "The ZonedDateTime should preserve the LocalDateTime fields and use the Converter's ZoneId");
    }

    @Test
    void testToZonedDateTime_InvalidSourceThrowsClassCastException() {
        // given
        Object notALocalDateTime = "2023-01-01T12:00:00";
        // when / then
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toZonedDateTime(notALocalDateTime, converter), "A non‑LocalDateTime source should result in a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] from={0} zone={1}")
    @MethodSource("explicitZoneIdProvider")
    void testValidConversionWithExplicitZoneId(LocalDateTime from, ZoneId zone, ZonedDateTime expected) {
        // given
        configureConverterWithZone(zone);
        // when
        ZonedDateTime actual = LocalDateTimeConversions.toZonedDateTime(from, converter);
        // then
        assertEquals(expected, actual, "ZonedDateTime should be created with the supplied ZoneId");
    }

    @Test
    void testValidConversionWithSystemDefaultZoneId() {
        // given
        LocalDateTime now = LocalDateTime.now();
        ZoneId systemDefault = ZoneId.systemDefault();
        configureConverterWithZone(systemDefault);
        ZonedDateTime expected = ZonedDateTime.of(now, systemDefault);
        // when
        ZonedDateTime actual = LocalDateTimeConversions.toZonedDateTime(now, converter);
        // then
        assertEquals(expected, actual, "Should use the system default ZoneId supplied by the converter");
    }

    @Test
    void testInvalidInput_StringThrowsClassCastException() {
        // given
        Object notALocalDateTime = "2023-05-10T12:30:45";
        configureConverterWithZone(ZoneId.of("UTC"));
        // when / then
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toZonedDateTime(notALocalDateTime, converter), "A non‑LocalDateTime source should result in a ClassCastException");
    }

    @Test
    void testInvalidInput_DateThrowsClassCastException() {
        // given
        Object notALocalDateTime = new java.util.Date();
        configureConverterWithZone(ZoneId.of("UTC"));
        // when / then
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toZonedDateTime(notALocalDateTime, converter), "A java.util.Date source should also result in a ClassCastException");
    }

    @Test
    void testNullFromArgumentThrowsNullPointerException() {
        // given
        configureConverterWithZone(ZoneId.of("UTC"));
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toZonedDateTime(null, converter), "Null source should cause a NullPointerException when cast to LocalDateTime");
    }

    @Test
    void testConverterReturnsNullZoneIdThrowsNullPointerException() {
        // given
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 0, 0);
        // zoneId is null
        when(options.getZoneId()).thenReturn(null);
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toZonedDateTime(ldt, converter), "ZonedDateTime.of throws NPE when ZoneId is null");
    }

    @Test
    void testConversionWithFixedZoneOffset() {
        // given
        LocalDateTime ldt = LocalDateTime.of(2023, 8, 15, 10, 20, 30, 123_456_789);
        ZoneOffset offset = ZoneOffset.ofHours(-3);
        configureConverterWithZone(offset);
        ZonedDateTime expected = ZonedDateTime.of(ldt, offset);
        // when
        ZonedDateTime actual = LocalDateTimeConversions.toZonedDateTime(ldt, converter);
        // then
        assertEquals(expected, actual, "Fixed ZoneOffset should be applied and nanoseconds retained");
    }

    @Test
    void testConversionDuringDstGap_SpringForward() {
        // given
        // non‑existent time in NY
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 12, 2, 30);
        ZoneId ny = ZoneId.of("America/New_York");
        configureConverterWithZone(ny);
        ZonedDateTime actual = LocalDateTimeConversions.toZonedDateTime(ldt, converter);
        // when
        // The expected ZonedDateTime is the one produced by ZonedDateTime.of – it resolves to the later offset (EDT)
        ZonedDateTime expected = ZonedDateTime.of(ldt, ny);
        // then
        assertEquals(expected, actual, "Method should not throw during DST gap and should resolve to the later offset");
    }

    @Test
    void testConversionDuringDstOverlap_FallBack() {
        // given
        // ambiguous time in NY
        LocalDateTime ldt = LocalDateTime.of(2023, 11, 5, 1, 30);
        ZoneId ny = ZoneId.of("America/New_York");
        configureConverterWithZone(ny);
        ZonedDateTime actual = LocalDateTimeConversions.toZonedDateTime(ldt, converter);
        // when
        // selects the earlier offset (EST)
        ZonedDateTime expected = ZonedDateTime.of(ldt, ny);
        // then
        assertEquals(expected, actual, "Method should resolve ambiguous time to the earlier offset");
    }

    @Test
    void testConversionWithUtcAndMaxNanoseconds() {
        // given
        LocalDateTime ldt = LocalDateTime.of(2021, 12, 31, 23, 59, 59, 999_999_999);
        ZoneId utc = ZoneId.of("Z");
        configureConverterWithZone(utc);
        ZonedDateTime expected = ZonedDateTime.of(ldt, utc);
        // when
        ZonedDateTime actual = LocalDateTimeConversions.toZonedDateTime(ldt, converter);
        // then
        assertEquals(expected, actual, "UTC conversion should preserve the maximum nanosecond value");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("extremeValuesProvider")
    void testExtremeLocalDateTimeValues(LocalDateTime from, ZoneId zone) {
        // given
        configureConverterWithZone(zone);
        ZonedDateTime expected = ZonedDateTime.of(from, zone);
        // when
        ZonedDateTime actual = LocalDateTimeConversions.toZonedDateTime(from, converter);
        // then
        assertEquals(expected, actual, "Extreme LocalDateTime values should be converted correctly");
    }

    @Test
    void testNullConverterArgumentThrowsNullPointerException() {
        // given
        LocalDateTime ldt = LocalDateTime.of(2022, 1, 1, 0, 0);
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toZonedDateTime(ldt, null), "Passing a null Converter should result in a NullPointerException");
    }

    @Test
    void testConverterReturnsNullOptionsThrowsNullPointerException() {
        // given
        LocalDateTime ldt = LocalDateTime.of(2022, 6, 15, 12, 0);
        configureConverterWithNullOptions();
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toZonedDateTime(ldt, converter), "Null Options from converter should cause NPE when getZoneId() is invoked");
    }

    @Test
    void testImmutabilityOriginalLocalDateTimeUnchanged() {
        // given
        LocalDateTime original = LocalDateTime.of(2024, 4, 4, 4, 4, 4);
        // reference for later comparison
        LocalDateTime copyBefore = original;
        configureConverterWithZone(ZoneId.of("Europe/Paris"));
        ZonedDateTime expected = ZonedDateTime.of(original, ZoneId.of("Europe/Paris"));
        // when
        ZonedDateTime actual = LocalDateTimeConversions.toZonedDateTime(original, converter);
        // then
        assertEquals(expected, actual, "Converted ZonedDateTime should match expected value");
        assertEquals(copyBefore, original, "Original LocalDateTime must remain unchanged (immutability)");
    }

    @Test
    void testConverterThrowsOnGetOptionsPropagatesException() {
        // given
        RuntimeException boom = new RuntimeException("boom");
        configureConverterThrowingOnGetOptions(boom);
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 12, 30, 45);
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> LocalDateTimeConversions.toZonedDateTime(ldt, converter), "Exception from converter.getOptions() should propagate");
        assertSame(boom, ex, "The thrown exception should be the same instance as the mock exception");
    }

    @Test
    void testOptionsThrowsOnGetZoneIdPropagatesException() {
        // given
        IllegalStateException broken = new IllegalStateException("zone broken");
        configureOptionsThrowingOnGetZoneId(broken);
        LocalDateTime ldt = LocalDateTime.now();
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> LocalDateTimeConversions.toZonedDateTime(ldt, converter), "Exception from options.getZoneId() should propagate");
        assertSame(broken, ex, "The thrown exception should be the same instance as the mock exception");
    }
}
