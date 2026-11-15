package com.cedarsoftware.util.convert;

import java.time.ZoneId;
import java.util.TimeZone;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ZoneIdConversions_toTimeZone_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    private static Stream<Arguments> validZoneIdProvider() {
        return Stream.of(// region based IDs
        arguments(ZoneId.of("America/New_York"), "America/New_York"), arguments(ZoneId.of("UTC"), "UTC"), arguments(ZoneId.of("Asia/Tokyo"), "Asia/Tokyo"), arguments(ZoneId.of("Europe/Paris"), "Europe/Paris"), arguments(ZoneId.of("GMT"), "GMT"), arguments(ZoneId.of("Europe/London"), "Europe/London"), // fixed offset IDs – the string representation of the offset is used as the TimeZone ID
        arguments(ZoneId.ofOffset("+02", ZoneOffset.ofHours(2)), "+02:00"), arguments(ZoneId.ofOffset("UTC", ZoneOffset.ofHours(-5)), "-05:00"), arguments(ZoneId.ofOffset("UTC", ZoneOffset.ofHoursMinutes(5, 30)), "+05:30"));
    }

    @Test
    void toTimeZone_WithValidZoneId_ReturnsCorrespondingTimeZone() {
        // given
        ZoneId zoneId = ZoneId.of("America/New_York");
        // when
        TimeZone result = ZoneIdConversions.toTimeZone(zoneId, mockConverter);
        // then
        assertNotNull(result, "Resulting TimeZone should not be null");
        assertEquals(zoneId.getId(), result.getID(), "TimeZone ID should match the original ZoneId ID");
    }

    @Test
    void toTimeZone_WithInvalidObject_ThrowsClassCastException() {
        // given
        Object notAZoneId = "I am not a ZoneId";
        // when / then
        assertThrows(ClassCastException.class, () -> ZoneIdConversions.toTimeZone(notAZoneId, mockConverter), "Passing a non‑ZoneId object should result in ClassCastException");
    }

    @Test
    void toTimeZone_WithNullFrom_ThrowsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class, () -> ZoneIdConversions.toTimeZone(null, mockConverter), "Casting null to ZoneId should result in NullPointerException from TimeZone.getTimeZone");
    }

    @Test
    void toTimeZone_WithStringFrom_ThrowsClassCastException() {
        // given
        Object notAZoneId = "Europe/Paris";
        // when / then
        assertThrows(ClassCastException.class, () -> ZoneIdConversions.toTimeZone(notAZoneId, mockConverter), "Passing a non‑ZoneId object should result in ClassCastException");
    }

    @Test
    void toTimeZone_WithArbitraryObject_ThrowsClassCastException() {
        // given
        Object notAZoneId = new Object();
        // when / then
        assertThrows(ClassCastException.class, () -> ZoneIdConversions.toTimeZone(notAZoneId, mockConverter), "Any object that does not implement ZoneId should cause ClassCastException");
    }

    @Test
    void toTimeZone_WithNonNullConverter_IgnoresConverter() {
        // a Converter that would fail loudly if its convert method were invoked
        Converter hostileConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new AssertionError("Converter should not be used by toTimeZone");
            }
        };
        ZoneId zoneId = ZoneId.of("America/Los_Angeles");
        // when
        TimeZone result = ZoneIdConversions.toTimeZone(zoneId, hostileConverter);
        // then
        assertNotNull(result);
        assertEquals("America/Los_Angeles", result.getID(), "Result should be correct even when a hostile Converter is supplied");
    }

    @Test
    void toTimeZone_WithSystemDefaultZoneId_ReturnsMatchingTimeZone() {
        ZoneId systemDefault = ZoneId.systemDefault();
        TimeZone result = ZoneIdConversions.toTimeZone(systemDefault, mockConverter);
        assertNotNull(result);
        assertEquals(systemDefault.getId(), result.getID(), "TimeZone ID should match the system default ZoneId ID");
    }

    @Test
    void toTimeZone_WithOffsetZoneId_VerifyRawOffset() {
        // +05:30
        ZoneId offsetZone = ZoneId.ofOffset("UTC", ZoneOffset.ofHoursMinutes(5, 30));
        TimeZone tz = ZoneIdConversions.toTimeZone(offsetZone, mockConverter);
        // raw offset is expressed in milliseconds
        // 5h30m -> 19800000 ms
        int expectedOffsetMillis = (5 * 60 + 30) * 60 * 1000;
        assertEquals(expectedOffsetMillis, tz.getRawOffset(), "Raw offset of the TimeZone should match the ZoneId offset (+05:30)");
    }

    @Test
    void toTimeZone_WithCustomZoneIdReturningNullId_ThrowsNullPointerException() {
        ZoneId malformed = new ZoneId() {

            @Override
            public String getId() {
                // deliberately broken
                return null;
            }

            @Override
            public java.time.zone.ZoneRules getRules() {
                throw new UnsupportedOperationException("Not needed for this test");
            }
        };
        assertThrows(NullPointerException.class, () -> ZoneIdConversions.toTimeZone(malformed, mockConverter), "TimeZone.getTimeZone(null) should propagate NullPointerException");
    }
}
