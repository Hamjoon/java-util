package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.convert.MapConversions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneOffset;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.ZONE_OFFSET;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ZoneOffsetConversions_toMap_Test {

    @Mock
    private Converter // Converter is not used by toMap, but required by signature
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito will initialise @Mock fields automatically
        // Ensure mock is clean before each test
        reset(mockConverter);
    }

    private static Stream<Arguments> normalCases() {
        return Stream.of(arguments(ZoneOffset.ofHoursMinutes(2, 0), "+02:00"), arguments(ZoneOffset.UTC, "Z"), arguments(ZoneOffset.ofHoursMinutes(-5, -30), "-05:30"));
    }

    private static Stream<Arguments> boundaryCases() {
        return Stream.of(// max positive
        arguments(ZoneOffset.ofHoursMinutesSeconds(18, 0, 0), "+18:00"), // max negative
        arguments(ZoneOffset.ofHoursMinutesSeconds(-18, 0, 0), "-18:00"), arguments(ZoneOffset.ofHoursMinutesSeconds(5, 15, 30), "+05:15:30"), arguments(ZoneOffset.ofTotalSeconds(0), "Z"));
    }

    @Test
    void toMap_withUtcOffset_returnsMapContainingZ() {
        // given
        // "Z"
        ZoneOffset utc = ZoneOffset.UTC;
        // when
        Map<String, Object> result = ZoneOffsetConversions.toMap(utc, mockConverter);
        // then
        assertNotNull(result, "Result map should not be null");
        assertEquals(1, result.size(), "Result map should contain exactly one entry");
        assertEquals(utc.getId(), result.get(ZONE_OFFSET), "Map should contain the ISO‑8601 ID of the offset");
    }

    @Test
    void toMap_withPositiveOffset_returnsMapContainingFormattedId() {
        // given
        // "+05:30"
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(5, 30);
        // when
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, mockConverter);
        // then
        assertEquals(offset.getId(), result.get(ZONE_OFFSET), "Map should contain the correct formatted offset string");
    }

    @Test
    void toMap_withNegativeOffset_returnsMapContainingFormattedId() {
        // given
        // "-04:00"
        ZoneOffset offset = ZoneOffset.ofHours(-4);
        // when
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, mockConverter);
        // then
        assertEquals(offset.getId(), result.get(ZONE_OFFSET), "Map should contain the correct formatted offset string for negative offsets");
    }

    @Test
    void toMap_ignoresConverterParameter() {
        // given
        ZoneOffset offset = ZoneOffset.ofHours(2);
        // Configure mock to throw if any method is called (should not happen)
        doThrow(new IllegalStateException("Converter should not be used")).when(mockConverter).convert(any(), any());
        // when
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, mockConverter);
        // then
        // No exception means the converter was indeed ignored
        assertEquals(offset.getId(), result.get(ZONE_OFFSET));
        // Verify no interaction with the mock
        verifyNoInteractions(mockConverter);
    }

    /* --------------------------------------------------------------
     *  Normal behavior – positive, negative and UTC offsets
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} => {1}")
    @MethodSource("normalCases")
    void toMap_normalBehavior_returnsExpectedMap(ZoneOffset from, String expectedId) {
        // when
        Map<String, Object> result = ZoneOffsetConversions.toMap(from, mockConverter);
        // then
        assertNotNull(result, "Result map should not be null");
        assertEquals(1, result.size(), "Result map should contain exactly one entry");
        assertEquals(expectedId, result.get(ZONE_OFFSET), "Map should contain the ISO‑8601 ID of the offset");
        // Verify that the converter is never touched
        verifyNoInteractions(mockConverter);
    }

    /* --------------------------------------------------------------
     *  Boundary conditions – extreme offsets and seconds component
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} => {1}")
    @MethodSource("boundaryCases")
    void toMap_boundaryConditions_returnsExpectedMap(ZoneOffset from, String expectedId) {
        // when
        Map<String, Object> result = ZoneOffsetConversions.toMap(from, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(expectedId, result.get(ZONE_OFFSET));
        verifyNoInteractions(mockConverter);
    }

    /* --------------------------------------------------------------
     *  Exception scenarios – null and wrong type arguments
     * -------------------------------------------------------------- */
    @Test
    void toMap_nullFrom_throwsNullPointerException() {
        // given
        ZoneOffset from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> ZoneOffsetConversions.toMap(from, mockConverter), "Passing a null 'from' should raise NullPointerException");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toMap_wrongTypeFrom_throwsClassCastException() {
        // given
        Object from = "some string";
        // when / then
        assertThrows(ClassCastException.class, () -> ZoneOffsetConversions.toMap(from, mockConverter), "Passing an incompatible type should raise ClassCastException");
        verifyNoInteractions(mockConverter);
    }
}
