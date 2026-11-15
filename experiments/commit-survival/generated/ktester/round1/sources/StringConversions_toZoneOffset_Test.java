package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringConversions_toZoneOffset_Test {

    @Mock
    private Converter // Converter is not used by the method but required for the signature
    mockConverter;

    private static Stream<Arguments> provideBasicCases() {
        return Stream.of(// null input → null
        arguments(null, null, "null input should return null"), // empty string → null
        arguments("", null, "empty string should return null"), // whitespace only → IllegalArgumentException (ZoneOffset.of will reject)
        arguments("   ", IllegalArgumentException.class, "whitespace‑only string should cause IllegalArgumentException"), // valid positive offset
        arguments("+02:00", ZoneOffset.of("+02:00"), "valid positive offset '+02:00'"), // valid negative offset
        arguments("-05:30", ZoneOffset.of("-05:30"), "valid negative offset '-05:30'"), // UTC designator
        arguments("Z", ZoneOffset.UTC, "literal 'Z' should be converted to ZoneOffset.UTC"), // offset without minutes
        arguments("+02", ZoneOffset.of("+02"), "offset without minutes '+02' is accepted"), // completely invalid string
        arguments("invalid", IllegalArgumentException.class, "non‑parseable string should cause IllegalArgumentException"));
    }

    private static Stream<Arguments> provideAdditionalCases() {
        return Stream.of(// explicit zero offset with hours/minutes
        arguments("+00:00", ZoneOffset.UTC, "explicit '+00:00' maps to UTC"), arguments("-00:00", ZoneOffset.UTC, "explicit '-00:00' maps to UTC"), // zero offset without minutes
        arguments("+00", ZoneOffset.UTC, "zero offset '+00' maps to UTC"), // maximum positive/negative offsets
        arguments("+18:00", ZoneOffset.of("+18:00"), "maximum positive offset"), arguments("-18:00", ZoneOffset.of("-18:00"), "maximum negative offset"), // offsets with seconds component (allowed by ZoneOffset.of)
        arguments("+18:00:00", ZoneOffset.of("+18:00:00"), "maximum positive offset with seconds"), arguments("-18:00:00", ZoneOffset.of("-18:00:00"), "maximum negative offset with seconds"), arguments("+02:00:30", ZoneOffset.of("+02:00:30"), "offset with seconds component"), arguments("-05:30:45", ZoneOffset.of("-05:30:45"), "negative offset with seconds component"), // leading whitespace – illegal
        arguments(" +02:00", IllegalArgumentException.class, "leading whitespace should cause IllegalArgumentException"), // trailing whitespace – illegal
        arguments("+02:00 ", IllegalArgumentException.class, "trailing whitespace should cause IllegalArgumentException"), // missing sign – illegal
        arguments("02:00", IllegalArgumentException.class, "missing sign should cause IllegalArgumentException"), // minute out of range – illegal
        arguments("+02:60", IllegalArgumentException.class, "minute >59 should cause IllegalArgumentException"), // second out of range – illegal
        arguments("+02:30:60", IllegalArgumentException.class, "second >59 should cause IllegalArgumentException"), // non‑String Integer – ClassCastException
        arguments(123, ClassCastException.class, "non‑String Integer input should cause ClassCastException"), // non‑String Boolean – ClassCastException
        arguments(true, ClassCastException.class, "non‑String Boolean input should cause ClassCastException"));
    }

    private static Stream<Arguments> provideEdgeErrorCases() {
        return Stream.of(// hour exceeds +18
        arguments("+19:00", IllegalArgumentException.class, "hour > +18 should cause IllegalArgumentException"), // hour exceeds -18
        arguments("-19:00", IllegalArgumentException.class, "hour < -18 should cause IllegalArgumentException"), // +18 with non‑zero minutes – illegal
        arguments("+18:30", IllegalArgumentException.class, "+18 with minutes should be illegal"), // -18 with non‑zero minutes – illegal
        arguments("-18:30", IllegalArgumentException.class, "-18 with minutes should be illegal"), // seconds not allowed at absolute limit
        arguments("+18:00:30", IllegalArgumentException.class, "seconds with hour +18 should be illegal"), arguments("-18:00:30", IllegalArgumentException.class, "seconds with hour -18 should be illegal"), // double sign
        arguments("++02:00", IllegalArgumentException.class, "double sign should be illegal"), // trailing sign character
        arguments("+02:00+", IllegalArgumentException.class, "trailing sign character should be illegal"), // double colon
        arguments("+02::00", IllegalArgumentException.class, "double colon should be illegal"), // lower‑case 'z' – not recognized
        arguments("z", IllegalArgumentException.class, "lower‑case 'z' should be illegal"), // sign only
        arguments("+", IllegalArgumentException.class, "sign without digits should be illegal"), // missing hour before colon
        arguments("+:00", IllegalArgumentException.class, "missing hour digits should be illegal"), // non‑String CharSequence (StringBuilder) – ClassCastException
        arguments(new StringBuilder("+02:00"), ClassCastException.class, "StringBuilder input should cause ClassCastException"));
    }

    @Test
    @DisplayName("toZoneOffset returns null for null or empty string")
    void testNullOrEmptyStringReturnsNull() {
        // null input
        ZoneOffset resultNull = StringConversions.toZoneOffset(null, mockConverter);
        assertNull(resultNull, "Expected null when input is null");
        // empty string input
        ZoneOffset resultEmpty = StringConversions.toZoneOffset("", mockConverter);
        assertNull(resultEmpty, "Expected null when input is empty");
    }

    @Test
    @DisplayName("toZoneOffset correctly parses valid offset strings")
    void testValidOffsetStrings() {
        // Positive offset
        ZoneOffset pos = StringConversions.toZoneOffset("+02:00", mockConverter);
        assertEquals(ZoneOffset.ofHoursMinutes(2, 0), pos, "Incorrect ZoneOffset for '+02:00'");
        // Negative offset
        ZoneOffset neg = StringConversions.toZoneOffset("-05:30", mockConverter);
        assertEquals(ZoneOffset.ofHoursMinutes(-5, -30), neg, "Incorrect ZoneOffset for '-05:30'");
        // UTC designator
        ZoneOffset utc = StringConversions.toZoneOffset("Z", mockConverter);
        assertEquals(ZoneOffset.UTC, utc, "Incorrect ZoneOffset for 'Z'");
    }

    @Test
    @DisplayName("toZoneOffset throws IllegalArgumentException for malformed offset")
    void testInvalidOffsetThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toZoneOffset("not-a-zone", mockConverter), "Expected IllegalArgumentException for malformed offset");
        assertTrue(ex.getMessage().contains("Unknown time-zone offset"), "Exception message should indicate unknown time-zone offset");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideBasicCases")
    @DisplayName("Basic toZoneOffset scenarios")
    void testBasicCases(Object from, Object expected, String description) {
        if (expected instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> StringConversions.toZoneOffset(from, mockConverter), description);
        } else {
            // Expected a ZoneOffset (or null)
            ZoneOffset result = StringConversions.toZoneOffset(from, mockConverter);
            if (expected == null) {
                assertNull(result, description);
            } else {
                assertEquals(expected, result, description);
            }
        }
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideAdditionalCases")
    @DisplayName("Additional toZoneOffset scenarios")
    void testAdditionalCases(Object from, Object expected, String description) {
        if (expected instanceof Class) {
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> StringConversions.toZoneOffset(from, mockConverter), description);
        } else {
            ZoneOffset result = StringConversions.toZoneOffset(from, mockConverter);
            if (expected == null) {
                assertNull(result, description);
            } else {
                assertEquals(expected, result, description);
            }
        }
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideEdgeErrorCases")
    @DisplayName("Edge error toZoneOffset scenarios")
    void testEdgeErrorCases(Object from, Object expected, String description) {
        if (expected instanceof Class) {
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> StringConversions.toZoneOffset(from, mockConverter), description);
        } else {
            // For completeness – none of the edge cases expect a successful result
            fail("Edge case should not return a value: " + description);
        }
    }
}
