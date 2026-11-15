package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toSqlDate_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mockConverter to return the mocked options.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        reset(mockConverter, mockOptions);
    }

    /**
     * Supplies the arguments for {@link #toSqlDate_VariousScenarios}.
     *
     * Each {@code Arguments} entry corresponds to one JSON test case.
     *
     * @return a stream of arguments.
     */
    private static Stream<Arguments> provideToSqlDateCases() {
        return Stream.of(// 1. Simple UTC conversion
        Arguments.of(Instant.parse("2023-01-15T10:20:30Z"), ZoneId.of("UTC"), Date.valueOf("2023-01-15"), null, "Convert an Instant at UTC to a java.sql.Date; verifies correct date extraction."), // 2. Instant with +02:00 offset, zone matches offset – same calendar day
        Arguments.of(Instant.parse("2023-01-15T23:45:00+02:00"), // Europe/Paris is UTC+01 (standard) or +02 (DST); matches offset here
        ZoneId.of("Europe/Paris"), Date.valueOf("2023-01-15"), null, "Instant with offset converted using a ZoneId that matches the offset; date should stay the same."), // 3. UTC instant converted to New York (still same day)
        Arguments.of(Instant.parse("2023-01-15T23:45:00Z"), ZoneId.of("America/New_York"), Date.valueOf("2023-01-15"), null, "Instant at UTC converted to New York time zone; still same calendar day before midnight."), // 4. UTC instant crosses midnight in Tokyo (+09:00)
        Arguments.of(Instant.parse("2023-01-15T23:45:00Z"), ZoneId.of("Asia/Tokyo"), Date.valueOf("2023-01-16"), null, "Instant at UTC crosses midnight when converted to Tokyo time; date should roll to next day."), // 5. Null Instant should cause NPE
        Arguments.of(null, ZoneId.of("UTC"), null, NullPointerException.class, "Passing null as the Instant should result in NPE because the method casts to Instant without null check."), // 6. Non‑Instant input (String) triggers ClassCastException
        Arguments.of(// cast to Object then to Instant to satisfy compile‑time type
        (Instant) (Object) "2023-01-01", ZoneId.of("UTC"), null, ClassCastException.class, "Providing a non‑Instant object triggers a ClassCastException during the cast."), // 7. Epoch instant at UTC
        Arguments.of(Instant.parse("1970-01-01T00:00:00Z"), ZoneId.of("UTC"), Date.valueOf("1970-01-01"), null, "Epoch instant at UTC should map directly to the epoch date."), // 8. Leap‑day handling
        Arguments.of(Instant.parse("2020-02-29T12:00:00Z"), ZoneId.of("UTC"), Date.valueOf("2020-02-29"), null, "Leap‑day instant verifies that the method correctly handles February 29 in a leap year."), // 9. Before DST spring‑forward in Europe/Paris (still same day)
        Arguments.of(Instant.parse("2023-03-26T01:30:00Z"), ZoneId.of("Europe/Paris"), Date.valueOf("2023-03-26"), null, "Instant just before the DST spring‑forward transition in Europe/Paris; the local date should remain the same."), // 10. Before DST fall‑back in Europe/Paris (date rolls back)
        Arguments.of(Instant.parse("2023-10-29T01:30:00Z"), ZoneId.of("Europe/Paris"), Date.valueOf("2023-10-28"), null, "Instant just before the DST fall‑back transition; when converted to Europe/Paris the local time becomes 02:30 on the previous calendar day, so the date rolls back."), // 11. Half‑hour offset (America/St_Johns, UTC‑03:30) moves to previous day
        Arguments.of(Instant.parse("2023-01-01T00:30:00Z"), ZoneId.of("America/St_Johns"), Date.valueOf("2022-12-31"), null, "ZoneId with a half‑hour offset (UTC‑03:30) moves the instant to the previous calendar day."), // 12. Far‑future instant (max java.sql.Date year)
        Arguments.of(Instant.parse("9999-12-31T23:59:59Z"), ZoneId.of("UTC"), Date.valueOf("9999-12-31"), null, "Far‑future instant checks that the method works up to the maximum java.sql.Date range."), // 13. Converter with a null ZoneId should cause NPE
        Arguments.of(Instant.parse("2023-01-01T00:00:00Z"), null, null, NullPointerException.class, "Converter with a null ZoneId should cause a NPE when the method attempts to call atZone(null)."), // 14. Non‑standard offset (+05:45) that does not cross midnight
        Arguments.of(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.ofHoursMinutes(5, 45), Date.valueOf("2023-01-01"), null, "ZoneId with a non‑standard offset (+05:45) that does not cross midnight; date should stay unchanged."));
    }

    @Test
    void toSqlDate_UtcZone_ReturnsCorrectDate() {
        // GIVEN
        Instant instant = Instant.parse("1970-01-01T00:00:00Z");
        when(mockOptions.getZoneId()).thenReturn(ZoneOffset.UTC);
        // WHEN
        Date result = InstantConversions.toSqlDate(instant, mockConverter);
        // THEN
        Date expected = Date.valueOf(LocalDate.of(1970, 1, 1));
        assertEquals(expected, result);
    }

    @Test
    void toSqlDate_NegativeOffsetCrossesMidnight_ReturnsPreviousDate() {
        // GIVEN
        // Instant corresponds to 2023‑03‑15T02:00:00Z.
        // With a -05:00 offset the local time is 2023‑03‑14T21:00:00,
        // so the resulting SQL date should be 2023‑03‑14.
        Instant instant = Instant.parse("2023-03-15T02:00:00Z");
        ZoneId offsetMinusFive = ZoneOffset.ofHours(-5);
        when(mockOptions.getZoneId()).thenReturn(offsetMinusFive);
        // WHEN
        Date result = InstantConversions.toSqlDate(instant, mockConverter);
        // THEN
        Date expected = Date.valueOf(LocalDate.of(2023, 3, 14));
        assertEquals(expected, result);
    }

    /**
     * Parameterised test that runs through all scenarios described in the
     * supplied JSON test cases.
     *
     * @param instant          the {@link Instant} to convert (may be {@code null})
     * @param zoneId           the {@link ZoneId} to be returned by the mocked options (may be {@code null})
     * @param expectedDate     the expected {@link java.sql.Date} result (ignored when an exception is expected)
     * @param expectedException the expected exception class, or {@code null} if no exception should be thrown
     */
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("provideToSqlDateCases")
    void toSqlDate_VariousScenarios(Instant instant, ZoneId zoneId, Date expectedDate, Class<? extends Throwable> expectedException, String description) {
        // Arrange: stub the zoneId that the converter will expose
        when(mockOptions.getZoneId()).thenReturn(zoneId);
        // Act & Assert
        if (expectedException != null) {
            // The method is expected to throw.
            assertThrows(expectedException, () -> InstantConversions.toSqlDate(instant, mockConverter), description);
        } else {
            // Normal conversion – verify the exact java.sql.Date value.
            Date result = InstantConversions.toSqlDate(instant, mockConverter);
            assertEquals(expectedDate, result, description);
        }
    }
}
