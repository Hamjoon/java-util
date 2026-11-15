package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toInstant_Test{

    @Mock
    private Converter converter;   // not used by toInstant, but required by signature

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise
    }

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension already initialises @Mock fields
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void toInstant_WholeSeconds() {
        Double source = 1_620_000_000.0; // exactly 1620000000 seconds since epoch
        Instant expected = Instant.ofEpochSecond(1_620_000_000L, 0L);

        Instant actual = DoubleConversions.toInstant(source, converter);

        assertEquals(expected, actual, "Instant should represent the exact whole‑second value");
    }

    @Test
    void toInstant_WithFractionalNanos() {
        Double source = 1_620_000_000.123456789; // 0.123456789 seconds = 123,456,789 ns
        Instant expected = Instant.ofEpochSecond(1_620_000_000L, 123_456_789L);

        Instant actual = DoubleConversions.toInstant(source, converter);

        assertEquals(expected, actual, "Instant should include correctly calculated nanoseconds");
    }

    @Test
    void toInstant_RoundsFractionalNanos() {
        // 0.1234567895 seconds should round up to 123,456,790 ns
        Double source = 1_620_000_000.1234567895;
        Instant expected = Instant.ofEpochSecond(1_620_000_000L, 123_456_790L);

        Instant actual = DoubleConversions.toInstant(source, converter);

        assertEquals(expected, actual, "Fractional nanoseconds must be rounded to the nearest long");
    }

    @Test
    void toInstant_NegativeValue() {
        Double source = -42.75; // -42 seconds and -0.75 seconds => -42 seconds and -750,000,000 ns
        // Instant.ofEpochSecond handles negative seconds and positive nanos correctly:
        // -42 seconds - 750,000,000 ns == -43 seconds + 250,000,000 ns
        Instant expected = Instant.ofEpochSecond(-43L, 250_000_000L);

        Instant actual = DoubleConversions.toInstant(source, converter);

        assertEquals(expected, actual, "Instant should correctly handle negative double values");
    }

    @Test
    void toInstant_Zero() {
        Double source = 0.0;
        Instant expected = Instant.ofEpochSecond(0L, 0L);

        Instant actual = DoubleConversions.toInstant(source, converter);

        assertEquals(expected, actual, "Zero should map to the epoch instant");
    }

    @Test
    void toInstant_NullInputThrowsClassCastException() {
        // The method casts the input to Double, so a null (or non‑Double) should cause a ClassCastException.
        assertThrows(ClassCastException.class, () -> DoubleConversions.toInstant(null, converter));
    }
}