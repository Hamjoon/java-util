package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_containsKey_Test {

    private TrackingMap<String, String> trackingMap;

    private Map<String, String> backingMap;

    class Person {

        final String name;

        final int age;

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
            return age == p.age && Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    class Person {

        final String name;

        final int age;

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
            return age == p.age && Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a simple HashMap as the underlying map for deterministic behaviour
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
        // Populate with a few entries
        trackingMap.put("alpha", "A");
        trackingMap.put("beta", "B");
        trackingMap.put("gamma", "C");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly necessary but keeps the template tidy
        trackingMap = null;
        backingMap = null;
    }

    /**
     * Provides a mixture of present/absent keys and the expected boolean result.
     */
    private static Stream<Arguments> keyPresenceProvider() {
        return Stream.of(Arguments.of("alpha", true), Arguments.of("beta", true), // not in the map
        Arguments.of("delta", false));
    }

    @Test
    void containsExistingKeyIsCounted() {
        // given
        assertTrue(trackingMap.keysUsed().isEmpty(), "No keys should be recorded before the call");
        // when
        boolean result = trackingMap.containsKey("beta");
        // then
        assertTrue(result, "containsKey should return true for an existing key");
        assertEquals(1, trackingMap.keysUsed().size(), "Exactly one key should be recorded");
        assertTrue(trackingMap.keysUsed().contains("beta"), "The accessed key must be recorded");
    }

    @Test
    void containsNonExistingKeyIsCounted() {
        // given
        assertTrue(trackingMap.keysUsed().isEmpty());
        // when
        // not present
        boolean result = trackingMap.containsKey("delta");
        // then
        assertFalse(result, "containsKey should return false for a missing key");
        assertEquals(1, trackingMap.keysUsed().size(), "Missing key access must still be recorded");
        assertTrue(trackingMap.keysUsed().contains("delta"));
    }

    @Test
    void containsKeyMultipleCallsRecordAllAccesses() {
        // when
        trackingMap.containsKey("alpha");
        trackingMap.containsKey("beta");
        // duplicate access
        trackingMap.containsKey("alpha");
        // then
        assertEquals(2, trackingMap.keysUsed().size(), "Duplicate accesses should not create duplicate entries");
        assertTrue(trackingMap.keysUsed().containsAll(java.util.List.of("alpha", "beta")));
    }

    @Test
    void containsKeyDoesNotAffectMapSizeUntilExpunge() {
        // when
        // accessed
        trackingMap.containsKey("alpha");
        // non‑existent
        trackingMap.containsKey("delta");
        // before expunge, map size should remain unchanged
        assertEquals(3, trackingMap.size());
        // expunge unused entries
        trackingMap.expungeUnused();
        // then
        // only accessed keys remain
        assertEquals(1, trackingMap.size());
        assertTrue(trackingMap.containsKey("alpha"));
        assertFalse(trackingMap.containsKey("beta"));
        assertFalse(trackingMap.containsKey("gamma"));
    }

    @Test
    void containsKeyWithNullKeyIsHandledGracefully() {
        // given
        trackingMap.put(null, "nullValue");
        // when
        boolean result = trackingMap.containsKey(null);
        // then
        assertTrue(result, "containsKey should return true for a present null key");
        assertEquals(1, trackingMap.keysUsed().size());
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    @Test
    void containsKeyWithEqualButDistinctStringInstance() {
        // given
        trackingMap.put("delta", "D");
        // when
        boolean result = trackingMap.containsKey(new String("delta"));
        // then
        assertTrue(result, "containsKey should return true for an equal but distinct String instance");
        assertEquals(1, trackingMap.keysUsed().size());
        // The set treats equal strings as the same entry
        assertTrue(trackingMap.keysUsed().contains("delta"));
    }

    @Test
    void containsKeyRecordsMultipleDistinctKeys() {
        // when
        trackingMap.containsKey("e1");
        trackingMap.containsKey("e2");
        trackingMap.containsKey("e3");
        // then
        assertEquals(3, trackingMap.keysUsed().size());
        assertTrue(trackingMap.keysUsed().containsAll(List.of("e1", "e2", "e3")));
    }

    @Test
    void containsKeyWithWrongTypeThrowsClassCastException() {
        // TrackingMap<String, String> expects String keys.
        assertThrows(ClassCastException.class, () -> trackingMap.containsKey(123));
    }

    @Test
    void containsKeyWithSubclassKeyWorksForNumberGeneric() {
        // TrackingMap<Number, String> where the backing map contains Integer 42
        Map<Number, String> numberMap = new HashMap<>();
        numberMap.put(42, "Answer");
        TrackingMap<Number, String> numberTrackingMap = new TrackingMap<>(numberMap);
        assertTrue(numberTrackingMap.containsKey(Integer.valueOf(42)), "containsKey should accept a subclass (Integer) when generic type is Number");
        assertEquals(1, numberTrackingMap.keysUsed().size());
        assertTrue(numberTrackingMap.keysUsed().contains(Integer.valueOf(42)));
    }

    @Test
    void containsKeyWithCustomObjectKey() {
        // Define a simple Person key with proper equals/hashCode
        class Person {

            final String name;

            final int age;

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
                return age == p.age && Objects.equals(name, p.name);
            }

            @Override
            public int hashCode() {
                return Objects.hash(name, age);
            }
        }
        Map<Person, String> personMap = new HashMap<>();
        Person keyInMap = new Person("Alice", 30);
        personMap.put(keyInMap, "Engineer");
        TrackingMap<Person, String> personTrackingMap = new TrackingMap<>(personMap);
        // equal but distinct instance
        Person lookup = new Person("Alice", 30);
        assertTrue(personTrackingMap.containsKey(lookup), "containsKey should return true for a custom key equal to an existing entry");
        assertEquals(1, personTrackingMap.keysUsed().size());
        assertTrue(personTrackingMap.keysUsed().contains(lookup));
    }

    @Test
    void containsKeyWithHashtableAndNullKeyThrowsNPE() {
        // does not allow null keys
        Map<String, String> hashtable = new Hashtable<>();
        TrackingMap<String, String> htTrackingMap = new TrackingMap<>(hashtable);
        assertThrows(NullPointerException.class, () -> htTrackingMap.containsKey(null));
    }

    @Test
    void containsKeyWithTreeMapAndNonComparableKeyThrowsClassCastException() {
        // TreeMap without a Comparator requires keys to be Comparable
        Map<Object, String> treeMap = new TreeMap<>();
        treeMap.put("a", "value");
        TrackingMap<Object, String> tmTrackingMap = new TrackingMap<>(treeMap);
        assertThrows(ClassCastException.class, () -> tmTrackingMap.containsKey(new Object()));
    }

    @Test
    void containsKeyWhenReadKeysIsUnmodifiableThrowsUnsupportedOperationException() throws Exception {
        // Replace the private readKeys set with an unmodifiable wrapper
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        Set<String> unmodifiable = Collections.unmodifiableSet(new HashSet<>());
        readKeysField.set(trackingMap, unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> trackingMap.containsKey("anyKey"));
    }

    @Test
    void containsKeyWhenInternalMapIsNullThrowsNullPointerException() throws Exception {
        // Nullify the internalMap field via reflection
        Field internalMapField = TrackingMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(trackingMap, null);
        assertThrows(NullPointerException.class, () -> trackingMap.containsKey("foo"));
    }

    @Test
    void containsKeyWhenReadKeysIsNullThrowsNullPointerException() throws Exception {
        // Nullify the readKeys field via reflection
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        readKeysField.set(trackingMap, null);
        // The internal map still works, but the attempt to add to readKeys fails
        assertThrows(NullPointerException.class, () -> trackingMap.containsKey("bar"));
    }

    @ParameterizedTest(name = "containsKey({0}) should return {1}")
    @MethodSource("keyPresenceProvider")
    void parameterizedContainsKey(String key, boolean expected) {
        boolean result = trackingMap.containsKey(key);
        assertEquals(expected, result);
        assertTrue(trackingMap.keysUsed().contains(key), "The key must be recorded regardless of presence");
    }
}
