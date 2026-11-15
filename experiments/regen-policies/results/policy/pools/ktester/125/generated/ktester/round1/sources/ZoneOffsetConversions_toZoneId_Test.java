package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZoneOffsetConversions_toZoneId_Test {

    @Mock
    private Converter // not used by the method but required by signature
    converter;

    private static Stream<Arguments> validZoneIdCastingProvider() {
        ZoneId utc = ZoneId.of("UTC");
        ZoneOffset offset2 = ZoneOffset.ofHours(2);
        return Stream.of(Arguments.of("ZoneId instance should be returned unchanged", utc, utc), Arguments.of("ZoneOffset implements ZoneId – should be returned unchanged", offset2, offset2));
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of("String is not a ZoneId – should throw ClassCastException", "Europe/Paris"), Arguments.of("Date is not a ZoneId – should throw ClassCastException", Date.from(java.time.Instant.now())), Arguments.of("String '+02:00' is not a ZoneId – should throw ClassCastException", "+02:00"), Arguments.of("ArrayList is not a ZoneId – should throw ClassCastException", new ArrayList<>()));
    }

    private static Stream<Arguments> converterIgnoredProvider() {
        ZoneId ny = ZoneId.of("America/New_York");
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(5, 30);
        // Converter that would throw if its convert method were called – it must not be called.
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        // A benign converter (does nothing) – also not used.
        Converter benignConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                return null;
            }
        };
        return Stream.of(Arguments.of("Non‑null Converter is ignored – ZoneId instance returned unchanged", ny, ny, benignConverter), Arguments.of("Non‑null Converter is ignored – ZoneOffset instance returned unchanged", offset, offset, throwingConverter));
    }

    private static Stream<Arguments> nonUtcZoneIdProvider() {
        ZoneId europeParis = ZoneId.of("Europe/Paris");
        ZoneId gmtOffset = ZoneId.of("GMT+02:30");
        return Stream.of(Arguments.of("Region based ZoneId (Europe/Paris) should be returned unchanged", europeParis), Arguments.of("GMT offset with minutes (GMT+02:30) should be returned unchanged", gmtOffset));
    }

    @Test
    void toZoneId_givenZoneId_returnsSameInstance() {
        // given
        ZoneId expected = ZoneId.of("UTC");
        // when
        ZoneId actual = ZoneOffsetConversions.toZoneId(expected, converter);
        // then
        assertSame(expected, actual, "The returned ZoneId should be the same instance that was supplied");
    }

    @Test
    void toZoneId_givenNull_returnsNull() {
        // given
        Object source = null;
        // when
        ZoneId result = ZoneOffsetConversions.toZoneId(source, converter);
        // then
        assertNull(result, "Casting null to ZoneId should yield null");
    }

    @Test
    void toZoneId_givenNonZoneId_throwsClassCastException() {
        // given
        Object notAZoneId = "UTC";
        // when / then
        assertThrows(ClassCastException.class, () -> ZoneOffsetConversions.toZoneId(notAZoneId, converter), "Casting a non‑ZoneId object should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validZoneIdCastingProvider")
    void toZoneId_givenValidZoneId_returnsSameInstance(String description, Object input, ZoneId expected) {
        // when
        ZoneId actual = ZoneOffsetConversions.toZoneId(input, converter);
        // then
        assertSame(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidTypeProvider")
    void toZoneId_givenInvalidType_throwsClassCastException(String description, Object input) {
        // when / then
        assertThrows(ClassCastException.class, () -> ZoneOffsetConversions.toZoneId(input, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("converterIgnoredProvider")
    void toZoneId_converterArgumentIgnored(String description, Object input, ZoneId expected, Converter customConverter) {
        // when
        ZoneId actual = ZoneOffsetConversions.toZoneId(input, customConverter);
        // then
        assertSame(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nonUtcZoneIdProvider")
    void toZoneId_givenNonUtcZoneId_returnsSameInstance(String description, ZoneId input) {
        // when
        ZoneId actual = ZoneOffsetConversions.toZoneId(input, converter);
        // then
        assertSame(input, actual, description);
    }

    @Test
    void toZoneId_givenCustomZoneIdSubclass_returnsSameInstance() {
        // given: an anonymous subclass of ZoneId
        ZoneId custom = new ZoneId() {

            @Override
            public String getId() {
                return "Custom/Zone";
            }

            @Override
            public java.time.zone.ZoneRules getRules() {
                // Minimal implementation – rules are not relevant for the cast test
                return java.time.zone.ZoneRules.of(java.time.Instant.EPOCH);
            }
        };
        // when
        ZoneId result = ZoneOffsetConversions.toZoneId(custom, converter);
        // then
        assertSame(custom, result, "Anonymous ZoneId subclass should be returned unchanged");
    }
}
