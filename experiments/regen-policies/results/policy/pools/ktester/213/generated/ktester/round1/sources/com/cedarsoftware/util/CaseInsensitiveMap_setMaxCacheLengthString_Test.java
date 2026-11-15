package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
// import org.junit.After;
// import org.junit.AfterClass;
// import org.junit.Before;
// import org.junit.Test;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_setMaxCacheLengthString_Test {

    /**
     * The default cache length defined by the library.
     */
    private static final int DEFAULT_MAX_CACHE_LENGTH = 100;

    /**
     * A fresh cache instance used for every test to keep the global state isolated.
     */
    private static LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> freshCache;

    //     @RunWith(Parameterized.class)
    public static class InvalidLengthsParameterizedTest {

        //         @Parameterized.Parameter(0)
        public int length;
        //         @Parameterized.Parameters(name = "setMaxCacheLengthString({0}) throws IllegalArgumentException")
        //         public static Collection<Object[]> data() {
        //             return Arrays.asList(new Object[][] { { 5 }, { -1 }, { 9 } });
        //         }
        //         @Test
        //         public void setMaxCacheLengthString_invalidLengths_throwException() {
        //             try {
        //                 CaseInsensitiveMap.setMaxCacheLengthString(length);
        //                 fail("Length " + length + " should trigger IllegalArgumentException.");
        //             } catch (IllegalArgumentException ex) {
        //                 assertTrue("Exception message must mention the minimum allowed length.", ex.getMessage().contains("at least 10"));
        //             }
        //         }
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Restore the default maximum length and replace the cache with a brand‑new instance.
        CaseInsensitiveMap.setMaxCacheLengthString(DEFAULT_MAX_CACHE_LENGTH);
        freshCache = new LRUCache<>(5000, LRUCache.StrategyType.THREADED);
        CaseInsensitiveMap.replaceCache(freshCache);
    }

//     @AfterEach
    void teardownAfterEach() {
        // Ensure that any modification performed by a test does not leak to the next one.
        CaseInsensitiveMap.setMaxCacheLengthString(DEFAULT_MAX_CACHE_LENGTH);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

//     @AfterAll
    static void teardownAfterAll() {
        // Clean‑up the static state for any other test suites that might run after this one.
        CaseInsensitiveMap.setMaxCacheLengthString(DEFAULT_MAX_CACHE_LENGTH);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    /* --------------------------------------------------------------------- */
    private static int getMaxCacheLengthViaReflection() throws Exception {
        Field f = CaseInsensitiveMap.CaseInsensitiveString.class.getDeclaredField("maxCacheLengthString");
        f.setAccessible(true);
        return (int) f.get(null);
    }

//     @Test
//     void setMaxCacheLengthString_acceptsValidValue() {
//         try {
//             CaseInsensitiveMap.setMaxCacheLengthString(20);
//         } catch (Exception e) {
//             fail("Setting a valid max cache length should not throw: " + e);
//         }
//     }

//     @Test
//     void setMaxCacheLengthString_rejectsTooSmallValue() {
//         try {
//             CaseInsensitiveMap.setMaxCacheLengthString(9);
//             fail("Values lower than 10 must cause IllegalArgumentException.");
//         } catch (IllegalArgumentException ex) {
//             assertTrue("Exception message should mention the minimum.", ex.getMessage().contains("at least 10"));
//         }
//     }

//     @Test
//     void cacheBehaviourChangesAccordingToMaxLength() {
//         // 1. Use a short string (well below the limit) – it should be cached.
//         String shortStr = "shortKey";
//         CaseInsensitiveMap.CaseInsensitiveString s1 = CaseInsensitiveMap.CaseInsensitiveString.of(shortStr);
//         CaseInsensitiveMap.CaseInsensitiveString s2 = CaseInsensitiveMap.CaseInsensitiveString.of(shortStr);
//         assertSame("Short strings should be retrieved from the cache (same instance).", s1, s2);
//         // 2. Reduce the cache length so that the same short string becomes uncached.
//         // shortStr length = 8 > 5
//         CaseInsensitiveMap.setMaxCacheLengthString(5);
//         CaseInsensitiveMap.CaseInsensitiveString s3 = CaseInsensitiveMap.CaseInsensitiveString.of(shortStr);
//         CaseInsensitiveMap.CaseInsensitiveString s4 = CaseInsensitiveMap.CaseInsensitiveString.of(shortStr);
//         assertNotSame("When length exceeds the max cache limit, instances must not be cached.", s3, s4);
//         // 3. Verify that a very long string is never cached, regardless of the limit.
//         String longStr = new String(new char[150]).replace('\0', 'a');
//         CaseInsensitiveMap.CaseInsensitiveString l1 = CaseInsensitiveMap.CaseInsensitiveString.of(longStr);
//         CaseInsensitiveMap.CaseInsensitiveString l2 = CaseInsensitiveMap.CaseInsensitiveString.of(longStr);
//         assertNotSame("Strings longer than the max cache length must not be cached.", l1, l2);
//     }

    /* --------------------------------------------------------------------- */
//     @ParameterizedTest(name = "setMaxCacheLengthString({0}) throws IllegalArgumentException")
//     @CsvSource({ "5", "-1", "9" })
//     void setMaxCacheLengthString_invalidLengths_throwException(int length) {
//         int[] badLengths = { 5, -1, 9 };
//         for (int length : badLengths) {
//             try {
//                 CaseInsensitiveMap.setMaxCacheLengthString(length);
//                 fail("Length " + length + " should trigger IllegalArgumentException.");
//             } catch (IllegalArgumentException ex) {
//                 assertTrue("Exception message must mention the minimum allowed length.", ex.getMessage().contains("at least 10"));
//             }
//         }
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void setMaxCacheLengthString_updatesStaticField() throws Exception {
//         int newLength = 20;
//         CaseInsensitiveMap.setMaxCacheLengthString(newLength);
//         assertEquals(newLength, getMaxCacheLengthViaReflection(), "The static field maxCacheLengthString must reflect the new value.");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void setMaxCacheLengthString_minimumAccepted_andFieldUpdated() throws Exception {
//         int min = 10;
//         CaseInsensitiveMap.setMaxCacheLengthString(min);
//         assertEquals(min, getMaxCacheLengthViaReflection(), "Static field should be set to the exact minimum (10).");
//     }

//     @Test
//     void cacheWorksWhenStringLengthEqualsLimit() {
//         int limit = 10;
//         CaseInsensitiveMap.setMaxCacheLengthString(limit);
//         // length == 10
//         String tenChars = "abcdefghij";
//         CaseInsensitiveMap.CaseInsensitiveString a = CaseInsensitiveMap.CaseInsensitiveString.of(tenChars);
//         CaseInsensitiveMap.CaseInsensitiveString b = CaseInsensitiveMap.CaseInsensitiveString.of(tenChars);
//         assertSame("String whose length equals the limit must be cached.", a, b);
//     }

//     @Test
//     void cacheBypassesWhenStringLengthExceedsLimitByOne() {
//         int limit = 10;
//         CaseInsensitiveMap.setMaxCacheLengthString(limit);
//         // length == 11
//         String elevenChars = "abcdefghijk";
//         CaseInsensitiveMap.CaseInsensitiveString a = CaseInsensitiveMap.CaseInsensitiveString.of(elevenChars);
//         CaseInsensitiveMap.CaseInsensitiveString b = CaseInsensitiveMap.CaseInsensitiveString.of(elevenChars);
//         assertNotSame("String longer than the limit must not be cached.", a, b);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void setMaxCacheLengthString_veryLargeValue_updatesField() throws Exception {
//         int huge = 10_000;
//         CaseInsensitiveMap.setMaxCacheLengthString(huge);
//         assertEquals(huge, getMaxCacheLengthViaReflection(), "Static field must hold the very large limit value.");
//     }

//     @Test
//     void cacheStillWorksWithVeryLargeLimit() {
//         int huge = 10_000;
//         CaseInsensitiveMap.setMaxCacheLengthString(huge);
//         String shortStr = "short";
//         CaseInsensitiveMap.CaseInsensitiveString a = CaseInsensitiveMap.CaseInsensitiveString.of(shortStr);
//         CaseInsensitiveMap.CaseInsensitiveString b = CaseInsensitiveMap.CaseInsensitiveString.of(shortStr);
//         assertSame("Even with a huge limit, short strings should be cached.", a, b);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void setMaxCacheLengthString_15_nonCachingForLength16() {
//         int limit = 15;
//         CaseInsensitiveMap.setMaxCacheLengthString(limit);
//         String sixteen = new String(new char[16]).replace('\0', 'a');
//         CaseInsensitiveMap.CaseInsensitiveString a = CaseInsensitiveMap.CaseInsensitiveString.of(sixteen);
//         CaseInsensitiveMap.CaseInsensitiveString b = CaseInsensitiveMap.CaseInsensitiveString.of(sixteen);
//         assertNotSame("String longer than the limit must not be cached.", a, b);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void setMaxCacheLengthString_100_cachesShortString() {
//         int limit = 100;
//         CaseInsensitiveMap.setMaxCacheLengthString(limit);
//         String shortStr = "test";
//         CaseInsensitiveMap.CaseInsensitiveString a = CaseInsensitiveMap.CaseInsensitiveString.of(shortStr);
//         CaseInsensitiveMap.CaseInsensitiveString b = CaseInsensitiveMap.CaseInsensitiveString.of(shortStr);
//         assertSame("With a high limit, a short string must be cached.", a, b);
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void statePersistsWhenExceptionThrownAfterValidSetting() throws Exception {
//         // valid first
//         CaseInsensitiveMap.setMaxCacheLengthString(25);
//         assertEquals(25, getMaxCacheLengthViaReflection(), "Field should be 25 after the first valid call.");
//         // invalid second – should throw but NOT change the field
//         try {
//             CaseInsensitiveMap.setMaxCacheLengthString(3);
//             fail("Setting an invalid length must throw.");
//         } catch (IllegalArgumentException ex) {
//             assertTrue(ex.getMessage().contains("at least 10"));
//         }
//         assertEquals(25, getMaxCacheLengthViaReflection(), "Field must remain 25 after the failed attempt.");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void idempotentCallsDoNotChangeState() throws Exception {
//         int val = 30;
//         // first call
//         CaseInsensitiveMap.setMaxCacheLengthString(val);
//         assertEquals(val, getMaxCacheLengthViaReflection(), "Field should be set to 30 after first call.");
//         // second identical call
//         CaseInsensitiveMap.setMaxCacheLengthString(val);
//         assertEquals(val, getMaxCacheLengthViaReflection(), "Field must stay 30 after the second identical call.");
//     }

//     @Test
//     void cacheBehaviourAfterIdempotentCalls() {
//         int limit = 30;
//         CaseInsensitiveMap.setMaxCacheLengthString(limit);
//         String longerThanLimit = new String(new char[31]).replace('\0', 'a');
//         CaseInsensitiveMap.CaseInsensitiveString a = CaseInsensitiveMap.CaseInsensitiveString.of(longerThanLimit);
//         CaseInsensitiveMap.CaseInsensitiveString b = CaseInsensitiveMap.CaseInsensitiveString.of(longerThanLimit);
//         assertNotSame("After idempotent setting, strings longer than the limit must not be cached.", a, b);
//     }
}