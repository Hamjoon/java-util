package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneId;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toZoneId_Test {

    /**
     * A mock Converter – the implementation does not depend on it.
     */
    @Mock
    private Converter mockConverter;

    private static class ArgumentsHolder {

        final Object from;

        // always null in our tests, kept for completeness
        final Converter converter;

        final String description;

        ArgumentsHolder(Object from, Converter converter, String description) {
            this.from = from;
            this.converter = converter;
            this.description = description;
        }
    }

    /**
     * Provides inputs that should result in {@code null}.
     */
    private static Stream<ArgumentsHolder> emptyOrNullProvider() {
        return Stream.of(new ArgumentsHolder("", null, "empty string"), new ArgumentsHolder(null, null, "null source"));
    }

    /**
     * Provides inputs that are invalid and should cause an IllegalArgumentException.
     */
    private static Stream<String> invalidIdProvider() {
        return Stream.of("invalid/zone", "This/DoesNotExist");
    }

    @Test
    void toZoneId_returnsNullWhenStringIsEmpty() {
        // given
        String empty = "";
        // when
        ZoneId result = StringConversions.toZoneId(empty, mockConverter);
        // then
        assertNull(result, "Empty string should result in null");
    }

    @Test
    void toZoneId_convertsValidZoneId() {
        // given
        String zone = "America/New_York";
        // when
        ZoneId result = StringConversions.toZoneId(zone, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(ZoneId.of(zone), result);
    }

    @Test
    void toZoneId_fallsBackToTimeZoneWhenZoneIdIsUnknownButTimeZoneExists() {
        // given
        // "PST" is not a standard ZoneId identifier but is recognised by TimeZone.
        String tzId = "PST";
        // when
        ZoneId result = StringConversions.toZoneId(tzId, mockConverter);
        // then
        assertNotNull(result);
        // The fallback should be the same as TimeZone.getTimeZone("PST").toZoneId()
        ZoneId expected = TimeZone.getTimeZone(tzId).toZoneId();
        assertEquals(expected, result);
    }

    @Test
    void toZoneId_throwsWhenBothZoneIdAndTimeZoneAreUnknown() {
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toZoneId(unknown, mockConverter));
        // The message must contain the marker used in the implementation.
        assertTrue(ex.getMessage().contains("Unknown time-zone ID"), "Exception message should mention unknown time-zone ID");
        // The cause should be the original exception from ZoneId.of()
        assertNotNull(ex.getCause(), "Cause should be the original exception from ZoneId.of");
    }

    @ParameterizedTest(name = "toZoneId returns null for {2}")
    @MethodSource("emptyOrNullProvider")
    void toZoneId_returnsNullWhenInputIsEmptyOrNull(ArgumentsHolder holder) {
        // given
        Object from = holder.from;
        // when
        ZoneId result = StringConversions.toZoneId(from, mockConverter);
        // then
        assertNull(result, "Expected null for " + holder.description);
    }
}
