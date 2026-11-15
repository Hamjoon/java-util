package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

class ThreadedLRUCacheStrategy_containsValue_Test {

    private ThreadedLRUCacheStrategy<Integer, String> cache;

    /**
     * Simple POJO with proper {@code equals}/{@code hashCode} for testing.
     */
    private static final class Person {

        private final String name;

        private final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Person))
                return false;
            Person p = (Person) o;
            return age == p.age && name.equals(p.name);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + age;
        }
    }

    /**
     * A class whose {@code equals} method always throws a {@link RuntimeException}.
     */
    private static final class BadEquals {

        private final String payload;

        BadEquals(String payload) {
            this.payload = payload;
        }

        @Override
        public boolean equals(Object obj) {
            throw new RuntimeException("BadEquals.equals invoked");
        }

        @Override
        public int hashCode() {
            return payload.hashCode();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // capacity = 5, cleanupDelayMillis = 50 (both satisfy constructor validation)
        cache = new ThreadedLRUCacheStrategy<>(5, 50);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared between tests to avoid cross‑test contamination
        cache.clear();
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> basicFunctionalityProvider() {
        return Stream.of(// null value present
        org.junit.jupiter.params.provider.Arguments.of(null, true, "Cache contains a node with a null value; containsValue(null) should return true."), // matching string
        org.junit.jupiter.params.provider.Arguments.of("test", true, "Cache contains a node whose value equals the supplied string; method should return true."), // non‑matching integer
        org.junit.jupiter.params.provider.Arguments.of(123, false, "Cache does not contain the supplied integer value; method should return false."));
    }

    @Test
    void containsValue_returnsTrueWhenValueIsPresent() {
        // given
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        // when
        boolean result = cache.containsValue("two");
        // then
        assertTrue(result, "containsValue should return true for an existing value");
    }

    @Test
    void containsValue_returnsFalseWhenValueIsAbsent() {
        // given
        cache.put(10, "ten");
        cache.put(20, "twenty");
        // when
        boolean result = cache.containsValue("thirty");
        // then
        assertFalse(result, "containsValue should return false when the value is not in the cache");
    }

    @Test
    void containsValue_handlesNullValue() {
        // given
        cache.put(100, null);
        cache.put(200, "two hundred");
        // when
        boolean containsNull = cache.containsValue(null);
        boolean containsNonNull = cache.containsValue("two hundred");
        // then
        assertTrue(containsNull, "containsValue should correctly identify a stored null value");
        assertTrue(containsNonNull, "containsValue should still work for non‑null values");
    }

    @Test
    void containsValue_returnsFalseAfterRemoval() {
        // given
        cache.put(5, "five");
        cache.put(6, "six");
        // remove the entry with value "five"
        cache.remove(5);
        // when
        boolean result = cache.containsValue("five");
        // then
        assertFalse(result, "containsValue should return false after the associated entry has been removed");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicFunctionalityProvider")
    void containsValue_basicFunctionality(Object queryValue, boolean expectedResult, String description) {
        // given
        // for the null case
        cache.put(1, null);
        // for the string case
        cache.put(2, "test");
        // filler value, not used in the integer case
        cache.put(3, 42);
        // when
        boolean result = cache.containsValue(queryValue);
        // then
        assertEquals(expectedResult, result, description);
    }

    @Test
    void containsValue_emptyCache() {
        // given – cache is freshly created and therefore empty
        // when
        boolean result = cache.containsValue("any");
        // then
        assertFalse(result, "The cache is empty; containsValue should return false for any supplied value.");
    }

    @Test
    void containsValue_multipleEntriesWithMatch() {
        // given
        cache.put(1, "first");
        cache.put(2, "second");
        cache.put(3, "third");
        // when
        boolean result = cache.containsValue("second");
        // then
        assertTrue(result, "The supplied value matches the second entry; method should return true.");
    }

    @Test
    void containsValue_multipleEntriesWithoutMatch() {
        // given
        cache.put(1, 10);
        cache.put(2, 20);
        cache.put(3, 30);
        // when
        boolean result = cache.containsValue("missing");
        // then
        assertFalse(result, "The supplied value is not present among the three entries; method should return false.");
    }

    @Test
    void containsValue_equalButDifferentInstance() {
        // given
        // literal string stored in the cache
        cache.put(1, "test");
        // when
        boolean result = cache.containsValue(new String("test"));
        // then
        assertTrue(result, "Different String instance with same characters should be considered equal.");
    }

    @Test
    void containsValue_numericTypeMismatch() {
        // given
        // store an Integer
        cache.put(1, Integer.valueOf(1));
        // when
        // query with a Long
        boolean result = cache.containsValue(Long.valueOf(1L));
        // then
        assertFalse(result, "Integer 1 and Long 1L are of different types; should not be considered equal.");
    }

    @Test
    void containsValue_customObjectEqualsPositive() {
        // given
        Person alice = new Person("Alice", 30);
        cache.put(1, alice);
        // when
        boolean result = cache.containsValue(new Person("Alice", 30));
        // then
        assertTrue(result, "Different Person instance with identical fields should be equal.");
    }

    @Test
    void containsValue_customObjectEqualsNegative() {
        // given
        Person alice = new Person("Alice", 30);
        cache.put(1, alice);
        // when
        boolean result = cache.containsValue(new Person("Bob", 25));
        // then
        assertFalse(result, "Different Person instance should not be equal.");
    }

    @Test
    void containsValue_nullValueAmongOtherEntries() {
        // given
        cache.put(1, "one");
        cache.put(2, null);
        cache.put(3, "three");
        // when
        boolean result = cache.containsValue(null);
        // then
        assertTrue(result, "Cache holds a null value among other entries; containsValue(null) must return true.");
    }

    @Test
    void containsValue_equalsThrowsException_sameType() {
        // given
        BadEquals bad = new BadEquals("trigger");
        cache.put(1, bad);
        // when / then
        BadEquals query = new BadEquals("any");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.containsValue(query), "Calling containsValue should propagate the RuntimeException thrown by BadEquals.equals");
        assertEquals("BadEquals.equals invoked", ex.getMessage());
    }

    @Test
    void containsValue_equalsThrowsException_differentType() {
        // given
        BadEquals bad = new BadEquals("trigger");
        cache.put(1, bad);
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.containsValue("someString"), "Calling containsValue with a different type should still trigger BadEquals.equals and throw.");
        assertEquals("BadEquals.equals invoked", ex.getMessage());
    }

    @Test
    void containsValue_internalCacheIsNull_throwsNPE() throws Exception {
        // given – use reflection to set the private 'cache' field to null
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        // when / then
        assertThrows(NullPointerException.class, () -> cache.containsValue("any"), "If the internal cache map is null, containsValue should throw NullPointerException.");
    }
}
