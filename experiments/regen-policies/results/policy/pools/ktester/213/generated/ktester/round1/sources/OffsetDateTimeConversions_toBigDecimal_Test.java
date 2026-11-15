package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toBigDecimal_Test {

    /**
     * Mocked static holder for {@link InstantConversions}.
     */
    private MockedStatic<InstantConversions> instantConversionsMock;

    /**
     * A dummy {@link Converter} instance – its behaviour is not exercised directly
     *  because the conversion is delegated to {@link InstantConversions}.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock for each test to avoid cross‑test interference.
        instantConversionsMock = mockStatic(InstantConversions.class);
        converter = mock(Converter.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Close the static mock to restore normal behaviour.
        instantConversionsMock.close();
    }

    @Test
    void toBigDecimal_delegatesToInstantConversions_andReturnsResult() {
        // GIVEN
        // An OffsetDateTime representing a known instant (2023‑01‑01T12:34:56.123456789Z)
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2023, 1, 1, 12, 34, 56, 123_456_789, ZoneOffset.UTC);
        Instant expectedInstant = offsetDateTime.toInstant();
        // The value we expect InstantConversions.toBigDecimal to return.
        BigDecimal expectedResult = new BigDecimal("12345.6789");
        // Stub the static method to return the expected result when called with the exact instant
        // and the mock converter.
        instantConversionsMock.when(() -> InstantConversions.toBigDecimal(expectedInstant, converter)).thenReturn(expectedResult);
        // WHEN
        BigDecimal actual = OffsetDateTimeConversions.toBigDecimal(offsetDateTime, converter);
        // THEN
        assertEquals(expectedResult, actual, "The method should return the value supplied by InstantConversions.toBigDecimal");
        // Verify that the static method was invoked exactly once with the correct arguments.
        instantConversionsMock.verify(() -> InstantConversions.toBigDecimal(expectedInstant, converter), times(1));
    }

    @Test
    void toBigDecimal_nullOffsetDateTime_throwsNullPointerException() {
        // GIVEN a null source
        OffsetDateTime nullValue = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toBigDecimal(nullValue, converter), "Casting a null OffsetDateTime should result in NPE because of the cast");
    }

    @Test
    void toBigDecimal_differentOffsets_delegatesCorrectly() {
        // GIVEN an OffsetDateTime with a non‑UTC offset (+05:30)
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2023, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHoursMinutes(5, 30));
        // instant is always UTC
        Instant expectedInstant = offsetDateTime.toInstant();
        BigDecimal expectedResult = new BigDecimal("98765.4321");
        instantConversionsMock.when(() -> InstantConversions.toBigDecimal(expectedInstant, converter)).thenReturn(expectedResult);
        // WHEN
        BigDecimal actual = OffsetDateTimeConversions.toBigDecimal(offsetDateTime, converter);
        // THEN
        assertEquals(expectedResult, actual);
        instantConversionsMock.verify(() -> InstantConversions.toBigDecimal(expectedInstant, converter), times(1));
    }

    @Test
    void toBigDecimal_epochInstant_delegatesCorrectly() {
        // GIVEN the epoch instant (1970‑01‑01T00:00:00Z)
        OffsetDateTime offsetDateTime = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Instant expectedInstant = offsetDateTime.toInstant();
        BigDecimal expectedResult = BigDecimal.ZERO;
        instantConversionsMock.when(() -> InstantConversions.toBigDecimal(expectedInstant, converter)).thenReturn(expectedResult);
        // WHEN
        BigDecimal actual = OffsetDateTimeConversions.toBigDecimal(offsetDateTime, converter);
        // THEN
        assertEquals(expectedResult, actual);
        instantConversionsMock.verify(() -> InstantConversions.toBigDecimal(expectedInstant, converter), times(1));
    }

    @Test
    void toBigDecimal_incompatibleSource_throwsClassCastException() {
        // GIVEN a source that is not an OffsetDateTime (String)
        String wrongType = "2023-05-17T14:30:15Z";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toBigDecimal(wrongType, converter), "Casting a non‑OffsetDateTime should throw ClassCastException");
    }

    @Test
    void toBigDecimal_nullConverter_throwsNullPointerException() {
        // GIVEN a valid OffsetDateTime but a null Converter
        OffsetDateTime offsetDateTime = OffsetDateTime.now();
        Instant instant = offsetDateTime.toInstant();
        // Stub the static method to throw NPE when the converter argument is null
        instantConversionsMock.when(() -> InstantConversions.toBigDecimal(instant, null)).thenThrow(new NullPointerException("converter is null"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toBigDecimal(offsetDateTime, null), "A null Converter should cause a NullPointerException from the delegated call");
        instantConversionsMock.verify(() -> InstantConversions.toBigDecimal(instant, null), times(1));
    }

    @Test
    void toBigDecimal_converterThrowsUncheckedException_propagates() {
        // GIVEN a valid OffsetDateTime and a Converter that would throw,
        // but the method never calls the Converter directly.
        // We simulate the failure by making the static delegation throw.
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2023, 5, 17, 14, 30, 15, 0, ZoneOffset.UTC);
        Instant instant = offsetDateTime.toInstant();
        instantConversionsMock.when(() -> InstantConversions.toBigDecimal(instant, converter)).thenThrow(new IllegalStateException("converter failure"));
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> OffsetDateTimeConversions.toBigDecimal(offsetDateTime, converter), "The unchecked exception from InstantConversions should propagate");
        assertEquals("converter failure", ex.getMessage());
        instantConversionsMock.verify(() -> InstantConversions.toBigDecimal(instant, converter), times(1));
    }
}
