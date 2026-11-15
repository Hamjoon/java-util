package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class BigIntegerConversions_toDate_Test{

    /** The Converter used for all test cases. */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – no special configuration required for the conversions we test.
        converter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization needed at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the test tidy.
        converter = null;
    }

    /**
     * Verify that a {@link java.util.Date} instance is returned unchanged (same epoch millis).
     */
    @Test
    void toDate_givenDate_returnsSameInstant() {
        // given
        Date now = new Date();

        // when
        Date result = BigIntegerConversions.toDate(now, converter);

        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(now.getTime(), result.getTime(),
                "Date produced from a Date should represent the same instant");
    }

    /**
     * Verify that an {@link java.time.Instant} is correctly converted to {@link java.util.Date}.
     */
    @Test
    void toDate_givenInstant_returnsCorrespondingDate() {
        // given
        Instant instant = Instant.now();

        // when
        Date result = BigIntegerConversions.toDate(instant, converter);

        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(instant.toEpochMilli(), result.getTime(),
                "Date should represent the same epoch milliseconds as the Instant");
    }

    /**
     * Verify that a {@link java.math.BigInteger} representing epoch milliseconds is converted
     * to the correct {@link java.util.Date}. The conversion logic in {@code BigIntegerConversions}
     * treats values less than a billion as seconds and larger values as milliseconds.
     * Using a value larger than {@code BILLION} ensures the millisecond path is exercised.
     */
    @Test
    void toDate_givenBigIntegerMillis_returnsCorrespondingDate() {
        // given
        long epochMillis = Instant.now().toEpochMilli();
        BigInteger bigInt = BigInteger.valueOf(epochMillis); // definitely > 1_000_000_000

        // when
        Date result = BigIntegerConversions.toDate(bigInt, converter);

        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(epochMillis, result.getTime(),
                "Date should represent the epoch milliseconds encoded in the BigInteger");
    }

    /**
     * Verify that a {@link java.math.BigInteger} representing epoch seconds is converted
     * to the correct {@link java.util.Date}. Values smaller than {@code BILLION}
     * are interpreted as seconds.
     */
    @Test
    void toDate_givenBigIntegerSeconds_returnsCorrespondingDate() {
        // given
        long epochSeconds = Instant.now().getEpochSecond();
        BigInteger bigInt = BigInteger.valueOf(epochSeconds); // < 1_000_000_000 for typical current epoch

        // when
        Date result = BigIntegerConversions.toDate(bigInt, converter);

        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(epochSeconds * 1000L, result.getTime(),
                "Date should represent the epoch seconds (converted to milliseconds) encoded in the BigInteger");
    }
}