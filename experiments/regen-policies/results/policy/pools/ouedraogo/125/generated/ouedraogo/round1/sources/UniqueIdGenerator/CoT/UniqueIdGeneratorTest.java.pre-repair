package com.cedarsoftware.util;

import org.junit.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link UniqueIdGenerator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public API signatures</li>
 *   <li>Basic functional behaviour (uniqueness, monotonic increase)</li>
 *   <li>Correct extraction of timestamps via {@code getDate}, {@code getDate19},
 *       {@code getInstant} and {@code getInstant19}</li>
 *   <li>Edge‑case handling – negative IDs should raise {@code IllegalArgumentException}</li>
 *   <li>Stress scenario that forces the internal sequence counters to roll over</li>
 * </ul>
 *
 * No external mocking frameworks are used – the tests rely only on the
 * public constructors and methods that exist in the supplied source code.
 */
public class UniqueIdGeneratorTest {

    /* --------------------------------------------------------------------- */
    /*  1.  Public method signatures (extracted for documentation purposes) */
    /* --------------------------------------------------------------------- */
    // public static long getUniqueId()
    // public static long getUniqueId19()
    // public static Date getDate(long uniqueId)
    // public static Date getDate19(long uniqueId)
    // public static Instant getInstant(long uniqueId)
    // public static Instant getInstant19(long uniqueId19)

    /* --------------------------------------------------------------------- */
    /*  2.  Basic functional tests                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetUniqueIdProducesIncreasingValues() {
        long first = UniqueIdGenerator.getUniqueId();
        long second = UniqueIdGenerator.getUniqueId();
        assertTrue("Second ID must be greater than first ID", second > first);
    }

    @Test
    public void testGetUniqueId19ProducesIncreasingValues() {
        long first = UniqueIdGenerator.getUniqueId19();
        long second = UniqueIdGenerator.getUniqueId19();
        assertTrue("Second ID must be greater than first ID", second > first);
    }

    @Test
    public void testMultipleIdsAreMonotonicallyIncreasing() {
        long previous = UniqueIdGenerator.getUniqueId();
        // generate a batch to increase the chance of crossing a millisecond boundary
        for (int i = 0; i < 2_000; i++) {
            long current = UniqueIdGenerator.getUniqueId();
            assertTrue("IDs must be strictly increasing", current > previous);
            previous = current;
        }
    }

    @Test
    public void testMultipleIds19AreMonotonicallyIncreasing() {
        long previous = UniqueIdGenerator.getUniqueId19();
        for (int i = 0; i < 20_000; i++) {
            long current = UniqueIdGenerator.getUniqueId19();
            assertTrue("IDs must be strictly increasing", current > previous);
            previous = current;
        }
    }

    /* --------------------------------------------------------------------- */
    /*  3.  Timestamp extraction tests                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetDateExtractsCorrectTimestamp() {
        long id = UniqueIdGenerator.getUniqueId();
        Date extracted = UniqueIdGenerator.getDate(id);
        long expectedMillis = id / 100_000L;
        assertEquals("Date extracted from ID must match timestamp component",
                expectedMillis, extracted.getTime());
    }

    @Test
    public void testGetDate19ExtractsCorrectTimestamp() {
        long id = UniqueIdGenerator.getUniqueId19();
        Date extracted = UniqueIdGenerator.getDate19(id);
        long expectedMillis = id / 1_000_000L;
        assertEquals("Date19 extracted from ID must match timestamp component",
                expectedMillis, extracted.getTime());
    }

    @Test
    public void testGetInstantExtractsCorrectTimestamp() {
        long id = UniqueIdGenerator.getUniqueId();
        Instant instant = UniqueIdGenerator.getInstant(id);
        long expectedMillis = id / 100_000L;
        assertEquals("Instant extracted from ID must match timestamp component",
                expectedMillis, instant.toEpochMilli());
    }

    @Test
    public void testGetInstant19ExtractsCorrectTimestamp() {
        long id = UniqueIdGenerator.getUniqueId19();
        Instant instant = UniqueIdGenerator.getInstant19(id);
        long expectedMillis = id / 1_000_000L;
        assertEquals("Instant19 extracted from ID must match timestamp component",
                expectedMillis, instant.toEpochMilli());
    }

    /* --------------------------------------------------------------------- */
    /*  4.  Edge‑case / exception handling                                    */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstantRejectsNegativeId() {
        UniqueIdGenerator.getInstant(-1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstant19RejectsNegativeId() {
        UniqueIdGenerator.getInstant19(-123L);
    }

    /* --------------------------------------------------------------------- */
    /*  5.  Stress test that forces sequence rollover (internal)            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSequenceRolloverForGetUniqueId() {
        // The internal sequence for getUniqueId rolls over after 1000 IDs generated
        // within the same millisecond. By generating many IDs in a tight loop we
        // are very likely to hit that condition. The test asserts that no
        // IllegalStateException or dead‑lock occurs and that monotonicity still
        // holds.
        long previous = UniqueIdGenerator.getUniqueId();
        for (int i = 0; i < 5_000; i++) {
            long current = UniqueIdGenerator.getUniqueId();
            assertTrue("IDs must stay monotonic even after sequence rollover", current > previous);
            previous = current;
        }
    }

    @Test
    public void testSequenceRolloverForGetUniqueId19() {
        // getUniqueId19 has a larger capacity (10 000 per ms) – we generate enough
        // IDs to be confident the rollover logic is exercised.
        long previous = UniqueIdGenerator.getUniqueId19();
        for (int i = 0; i < 50_000; i++) {
            long current = UniqueIdGenerator.getUniqueId19();
            assertTrue("IDs must stay monotonic even after sequence rollover", current > previous);
            previous = current;
        }
    }
}
