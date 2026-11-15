package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toYearMonth_Test {

    @Mock
    private Converter // the Converter passed to the method under test
    converter;

    @Mock
    private ConverterOptions // the ConverterOptions returned by converter.getOptions()
    options;

    // for closing the Mockito session
    private AutoCloseable mocks;

    /**
     * Minimal stub that implements {@link Converter}.  Only {@link #getOptions()} is needed
     * for the tests – it returns a {@link ConverterOptions} that simply wraps the supplied
     * {@link ZoneId}.  All other methods of {@code Converter} are irrelevant for the
     * {@code toYearMonth} conversion.
     */
    private static final class StubConverter implements Converter {

        private final ConverterOptions options;

        StubConverter(ZoneId zone) {
            this.options = new ConverterOptions(zone);
        }

        @Override
        public ConverterOptions getOptions() {
            return options;
        }

        // The remaining methods of Converter are not used in these tests.
        // They can remain unimplemented (throwing UnsupportedOperationException)
        // because the test never invokes them.
        @Override
        public Object convert(Object source, Class<?> target) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object convert(Object source, Type target) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setOptions(ConverterOptions options) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Simple holder for the zone used by {@link StubConverter}.
     * The real library has a richer implementation; for the purpose of these tests
     * only {@link #getZoneId()} is required.
     */
    private static final class ConverterOptions {

        private final ZoneId zoneId;

        ConverterOptions(ZoneId zoneId) {
            this.zoneId = zoneId;
        }

        ZoneId getZoneId() {
            return zoneId;
        }
    }

    /* --------------------------------------------------------------------- *
     *  Helper holder for parameterised arguments                             *
     * --------------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final OffsetDateTime source;

        final ZoneId targetZone;

        final YearMonth expected;

        final String description;

        ArgumentsHolder(OffsetDateTime source, ZoneId targetZone, YearMonth expected, String description) {
            this.source = source;
            this.targetZone = targetZone;
            this.expected = expected;
            this.description = description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the Mockito annotations for this test instance.
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the Mockito session to avoid memory‑leak warnings.
        mocks.close();
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised tests – valid conversions                              *
     * --------------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> validConversionProvider() {
        return Stream.of(// +02:00 offset, target UTC → instant 08:30Z → same date 2023‑05‑15
        new ArgumentsHolder(OffsetDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneOffset.ofHours(2)), ZoneId.of("UTC"), YearMonth.of(2023, 5), "Converts an OffsetDateTime with +02:00 offset to YearMonth in UTC."), // -02:00 offset, end‑of‑year crossing when shifted to UTC → 2024‑01‑01
        new ArgumentsHolder(OffsetDateTime.of(2023, 12, 31, 23, 0, 0, 0, ZoneOffset.ofHours(-2)), ZoneId.of("UTC"), YearMonth.of(2024, 1), "Cross‑year conversion: -02:00 offset becomes next year in UTC."), // No offset change, target America/New_York (UTC‑05:00) → same local date
        new ArgumentsHolder(OffsetDateTime.of(2022, 2, 28, 23, 59, 59, 0, ZoneOffset.UTC), ZoneId.of("America/New_York"), YearMonth.of(2022, 2), "Same instant converted to America/New_York; month stays February."));
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised tests – DST edge cases                                 *
     * --------------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> dstEdgeCaseProvider() {
        return Stream.of(// Spring‑forward: 2023‑03‑12 01:30‑05:00 → 2023‑03‑12 06:30Z → still March
        new ArgumentsHolder(OffsetDateTime.of(2023, 3, 12, 1, 30, 0, 0, ZoneOffset.ofHours(-5)), ZoneId.of("America/New_York"), YearMonth.of(2023, 3), "Before the spring‑forward DST jump."), // Fall‑back: 2023‑11‑05 01:30‑04:00 → 2023‑11‑05 05:30Z → still November
        new ArgumentsHolder(OffsetDateTime.of(2023, 11, 5, 1, 30, 0, 0, ZoneOffset.ofHours(-4)), ZoneId.of("America/New_York"), YearMonth.of(2023, 11), "Before the fall‑back DST transition."));
    }

    /**
     * Verify that when the source {@link OffsetDateTime} is already in the target zone,
     * the resulting {@link YearMonth} corresponds to the original year‑month.
     */
    @Test
    void toYearMonth_WithSameZone_ReturnsOriginalYearMonth() {
        // given
        ZoneId targetZone = ZoneId.of("UTC");
        when(converter.getOptions()).thenReturn(options);
        when(options.getZoneId()).thenReturn(targetZone);
        OffsetDateTime source = OffsetDateTime.of(// 2023‑05‑20
        2023, // 2023‑05‑20
        5, // 2023‑05‑20
        20, // 12:00:00
        12, // 12:00:00
        0, // 12:00:00
        0, // 12:00:00
        0, // already UTC
        ZoneOffset.UTC);
        // when
        YearMonth result = OffsetDateTimeConversions.toYearMonth(source, converter);
        // then
        assertEquals(YearMonth.of(2023, 5), result, "YearMonth should match the source when zones are identical");
    }

    /**
     * Verify that a conversion across zones that moves the date into the next month
     * yields the correct {@link YearMonth}.
     */
    @Test
    void toYearMonth_WithDifferentZone_ChangesMonthWhenCrossingMidnight() {
        // given
        ZoneId targetZone = ZoneId.of("UTC");
        when(converter.getOptions()).thenReturn(options);
        when(options.getZoneId()).thenReturn(targetZone);
        // Source is 2023‑04‑30 23:30 at offset -05:00.
        // Converting to UTC adds 5 hours → 2023‑05‑01 04:30, i.e., next month.
        OffsetDateTime source = OffsetDateTime.of(2023, 4, 30, 23, 30, 0, 0, ZoneOffset.ofHours(-5));
        // when
        YearMonth result = OffsetDateTimeConversions.toYearMonth(source, converter);
        // then
        assertEquals(YearMonth.of(2023, 5), result, "YearMonth should reflect the month after zone conversion (cross‑month boundary)");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionProvider")
    void toYearMonth_ValidConversions(ArgumentsHolder args) {
        // Arrange – use the stub so we control the returned zone
        Converter stub = new StubConverter(args.targetZone);
        // Act
        YearMonth result = OffsetDateTimeConversions.toYearMonth(args.source, stub);
        // Assert
        assertEquals(args.expected, result, () -> "Expected YearMonth " + args.expected + " but got " + result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("dstEdgeCaseProvider")
    void toYearMonth_DstEdgeCases(ArgumentsHolder args) {
        Converter stub = new StubConverter(args.targetZone);
        YearMonth result = OffsetDateTimeConversions.toYearMonth(args.source, stub);
        assertEquals(args.expected, result);
    }

    /* --------------------------------------------------------------------- *
     *  Exception tests – invalid inputs                                      *
     * --------------------------------------------------------------------- */
    @Test
    void toYearMonth_NullFrom_ThrowsNPE() {
        Converter stub = new StubConverter(ZoneId.of("UTC"));
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toYearMonth(null, stub), "Passing null as the source should raise NPE");
    }

    @Test
    void toYearMonth_WrongTypeFrom_ThrowsClassCastException() {
        Converter stub = new StubConverter(ZoneId.of("UTC"));
        Object notAnOffsetDateTime = "2023-05-15T10:30+02:00";
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toYearMonth(notAnOffsetDateTime, stub), "Passing a String instead of OffsetDateTime should raise ClassCastException");
    }

    @Test
    void toYearMonth_NullConverter_ThrowsNPE() {
        OffsetDateTime source = OffsetDateTime.now();
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toYearMonth(source, null), "Null converter should cause NPE when getOptions() is invoked");
    }

    @Test
    void toYearMonth_NullOptions_ThrowsNPE() {
        // Mock converter to return null options
        when(converter.getOptions()).thenReturn(null);
        OffsetDateTime source = OffsetDateTime.now();
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toYearMonth(source, converter), "Null options should cause NPE when getZoneId() is accessed");
    }

    @Test
    void toYearMonth_NullZoneId_ThrowsNPE() {
        // Mock options to return null ZoneId
        when(converter.getOptions()).thenReturn(options);
        when(options.getZoneId()).thenReturn(null);
        OffsetDateTime source = OffsetDateTime.now();
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toYearMonth(source, converter), "Null ZoneId should cause NPE inside atZoneSameInstant");
    }
}
