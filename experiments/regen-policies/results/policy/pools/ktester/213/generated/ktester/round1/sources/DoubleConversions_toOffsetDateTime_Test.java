package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.MockedStatic;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toOffsetDateTime_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock converter to return a mock options instance
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions after each test to avoid cross‑test contamination
        clearInvocations(mockConverter, mockOptions);
    }

    /**
     * Verify that an {@link Instant} is correctly converted to an {@link OffsetDateTime}
     * using the zone supplied by {@link ConverterOptions#getZoneId()}.
     */
    @Test
    void toOffsetDateTime_fromInstant_usesConverterZone() {
        // given
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        ZoneId utc = ZoneId.of("UTC");
        when(mockOptions.getZoneId()).thenReturn(utc);
        // when
        OffsetDateTime result = DoubleConversions.toOffsetDateTime(instant, mockConverter);
        // then
        OffsetDateTime expected = instant.atZone(utc).toOffsetDateTime();
        assertEquals(expected, result);
        // ensure the converter was consulted for its options
        verify(mockConverter).getOptions();
        // ensure the zone ID was retrieved
        verify(mockOptions).getZoneId();
    }

    /**
     * Verify conversion when the converter is configured with a non‑UTC zone.
     */
    @Test
    void toOffsetDateTime_fromInstant_withNonUtcZone() {
        // given
        Instant instant = Instant.parse("2023-06-15T08:00:00Z");
        // UTC‑05:00 (or ‑04:00 during DST)
        ZoneId newYork = ZoneId.of("America/New_York");
        when(mockOptions.getZoneId()).thenReturn(newYork);
        // when
        OffsetDateTime result = DoubleConversions.toOffsetDateTime(instant, mockConverter);
        // then
        OffsetDateTime expected = instant.atZone(newYork).toOffsetDateTime();
        assertEquals(expected, result);
    }

    /**
     * Passing {@code null} as the source object should result in a {@code NullPointerException}
     * because {@code toInstant(null, …)} returns {@code null} and the subsequent call to
     * {@code atZone(...)} cannot be performed.
     */
    @Test
    void toOffsetDateTime_nullSource_throwsNPE() {
        // given
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // when / then
        assertThrows(NullPointerException.class, () -> DoubleConversions.toOffsetDateTime(null, mockConverter));
    }

    /**
     * When the {@code converter} argument itself is {@code null},
     * the call to {@code converter.getOptions()} triggers a {@link NullPointerException}.
     */
    @Test
    void toOffsetDateTime_nullConverter_throwsNPE() {
        // given
        String isoString = "2023-01-01T12:00:00Z";
        // when / then
        assertThrows(NullPointerException.class, () -> DoubleConversions.toOffsetDateTime(isoString, null));
    }

    /**
     * If {@code converter.getOptions()} returns {@code null},
     * the subsequent call to {@code .getZoneId()} causes a {@link NullPointerException}.
     */
    @Test
    void toOffsetDateTime_nullOptions_throwsNPE() {
        // given
        when(mockConverter.getOptions()).thenReturn(null);
        String isoString = "2023-01-01T12:00:00Z";
        // when / then
        assertThrows(NullPointerException.class, () -> DoubleConversions.toOffsetDateTime(isoString, mockConverter));
    }

    /**
     * When {@code converter.getOptions().getZoneId()} returns {@code null},
     * {@link Instant#atZone(ZoneId)} throws a {@link NullPointerException}.
     */
    @Test
    void toOffsetDateTime_nullZoneId_throwsNPE() {
        // given
        when(mockOptions.getZoneId()).thenReturn(null);
        Instant instant = Instant.parse("2023-01-01T12:00:00Z");
        // when / then
        assertThrows(NullPointerException.class, () -> DoubleConversions.toOffsetDateTime(instant, mockConverter));
    }

    /**
     * If the delegated {@code toInstant} method returns {@code null},
     * the subsequent {@code atZone(...)} call on a {@code null} {@link Instant}
     * results in a {@link NullPointerException}.
     */
    @Test
    void toOffsetDateTime_toInstantReturnsNull_throwsNPE() {
        // given
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        Object anyValidValue = "2023-01-01T12:00:00Z";
        // Mock only the static {@code toInstant} method; all other static methods keep their real behavior.
        try (MockedStatic<DoubleConversions> mocked = mockStatic(DoubleConversions.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> DoubleConversions.toInstant(any(), any())).thenReturn(// simulate a converter that cannot produce an Instant
            null);
            // when / then
            assertThrows(NullPointerException.class, () -> DoubleConversions.toOffsetDateTime(anyValidValue, mockConverter));
        }
    }

    /**
     * When {@code toInstant} throws an {@link IllegalArgumentException} because the source type
     * is unsupported, the exception should propagate unchanged.
     */
    @Test
    void toOffsetDateTime_unsupportedFromType_propagatesIllegalArgumentException() {
        // given
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // a type that {@code toInstant} cannot handle
        Object unsupported = new Object();
        try (MockedStatic<DoubleConversions> mocked = mockStatic(DoubleConversions.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> DoubleConversions.toInstant(any(), any())).thenThrow(new IllegalArgumentException("Unsupported type"));
            // when / then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toOffsetDateTime(unsupported, mockConverter));
            assertEquals("Unsupported type", ex.getMessage());
        }
    }
}
