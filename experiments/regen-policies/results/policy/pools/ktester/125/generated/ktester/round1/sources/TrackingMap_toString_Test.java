package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class TrackingMap_toString_Test {

    /**
     * The TrackingMap instance under test.
     */
    private TrackingMap<String, String> trackingMap;

    /**
     * A mock backing map used to verify delegation of {@code toString()}.
     */
    @Mock
    private Map<String, String> mockBackingMap;

    /**
     * Auto‑closeable for Mockito's open mocks (used when not relying on the @Mock field injection).
     */
    private AutoCloseable mocks;

    class CustomMap extends HashMap<String, String> {

        @Override
        public String toString() {
            return "CUSTOM_MAP_STRING";
        }
    }

    class Key {

        private final String name;

        Key(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Key[" + name + "]";
        }
    }

    class Value {

        private final int number;

        Value(int number) {
            this.number = number;
        }

        @Override
        public String toString() {
            return "Value[" + number + "]";
        }
    }

    class BadMap extends HashMap<String, String> {

        @Override
        public String toString() {
            throw new RuntimeException("bad");
        }
    }

    class NullStringMap extends HashMap<String, String> {

        @Override
        public String toString() {
            return null;
        }
    }

    class NPEMap extends HashMap<String, String> {

        @Override
        public String toString() {
            throw new NullPointerException("intentional NPE");
        }
    }

    class IAExceptionMap extends HashMap<String, String> {

        @Override
        public String toString() {
            throw new IllegalArgumentException("illegal");
        }
    }

    class CMEMap extends AbstractMap<String, String> {

        private final Map<String, String> delegate = new HashMap<>();

        {
            delegate.put("a", "1");
            delegate.put("b", "2");
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            // Return a view that will cause CME when iterated after modification.
            return delegate.entrySet();
        }

        @Override
        public String toString() {
            // Simulate concurrent change during toString().
            Iterator<Entry<String, String>> it = delegate.entrySet().iterator();
            // structural modification
            delegate.put("c", "3");
            // this will trigger CME
            it.next();
            return super.toString();
        }
    }

    class ErrorMap extends HashMap<String, String> {

        @Override
        public String toString() {
            throw new AssertionError("assertion failure");
        }
    }

    class CustomMap extends HashMap<String, String> {

        @Override
        public String toString() {
            return "CUSTOM_MAP_STRING";
        }
    }

    class Key {

        private final String name;

        Key(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Key[" + name + "]";
        }
    }

    class Value {

        private final int number;

        Value(int number) {
            this.number = number;
        }

        @Override
        public String toString() {
            return "Value[" + number + "]";
        }
    }

    class BadMap extends HashMap<String, String> {

        @Override
        public String toString() {
            throw new RuntimeException("bad");
        }
    }

    class NullStringMap extends HashMap<String, String> {

        @Override
        public String toString() {
            return null;
        }
    }

    class NPEMap extends HashMap<String, String> {

        @Override
        public String toString() {
            throw new NullPointerException("intentional NPE");
        }
    }

    class IAExceptionMap extends HashMap<String, String> {

        @Override
        public String toString() {
            throw new IllegalArgumentException("illegal");
        }
    }

    class CMEMap extends AbstractMap<String, String> {

        private final Map<String, String> delegate = new HashMap<>();

        {
            delegate.put("a", "1");
            delegate.put("b", "2");
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            // Return a view that will cause CME when iterated after modification.
            return delegate.entrySet();
        }

        @Override
        public String toString() {
            // Simulate concurrent change during toString().
            Iterator<Entry<String, String>> it = delegate.entrySet().iterator();
            // structural modification
            delegate.put("c", "3");
            // this will trigger CME
            it.next();
            return super.toString();
        }
    }

    class ErrorMap extends HashMap<String, String> {

        @Override
        public String toString() {
            throw new AssertionError("assertion failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Initialise Mockito annotations (necessary when we also want a clean mock per test).
        mocks = MockitoAnnotations.openMocks(this);
        // Configure the mock's toString() behaviour.
        when(mockBackingMap.toString()).thenReturn("{mockKey=mockValue}");
        // Create the TrackingMap that wraps the mock.
        trackingMap = new TrackingMap<>(mockBackingMap);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Verify that no unexpected interactions occurred with the mock.
        verifyNoMoreInteractions(mockBackingMap);
        // Release Mockito resources.
        mocks.close();
    }

    @Test
    void toStringDelegatesToBackingMap() {
        // Exercise the method under test.
        String result = trackingMap.toString();
        // Verify the result matches the mock's toString().
        assertEquals("{mockKey=mockValue}", result, "TrackingMap.toString() should return the backing map's toString() value.");
        // Ensure the backing map's toString() was invoked exactly once.
        verify(mockBackingMap, times(1)).toString();
    }

    @Test
    void toStringReflectsRealBackingMapContent() {
        // Use a real, ordered map to have deterministic toString output.
        Map<String, Integer> realMap = new LinkedHashMap<>();
        realMap.put("a", 1);
        realMap.put("b", 2);
        TrackingMap<String, Integer> realTrackingMap = new TrackingMap<>(realMap);
        // Initial content check.
        assertEquals(realMap.toString(), realTrackingMap.toString(), "Initial toString() should match the backing map's toString().");
        // Mutate the backing map after the TrackingMap has been created.
        realMap.put("c", 3);
        realMap.remove("a");
        // The TrackingMap should reflect the current state of the backing map.
        assertEquals(realMap.toString(), realTrackingMap.toString(), "toString() should reflect changes made to the backing map after construction.");
    }

    /**
     * Empty internal map should render "{}".
     */
    @Test
    void emptyInternalMap() {
        Map<String, String> emptyMap = new HashMap<>();
        TrackingMap<String, String> tm = new TrackingMap<>(emptyMap);
        assertEquals("{}", tm.toString(), "When the wrapped internal map is empty, toString() must return \"{}\".");
    }

    /**
     * Non‑empty map with two entries – order must match the underlying map.
     */
    @Test
    void nonEmptyInternalMapTwoEntries() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("key1", "val1");
        map.put("key2", "val2");
        TrackingMap<String, String> tm = new TrackingMap<>(map);
        assertEquals("{key1=val1, key2=val2}", tm.toString(), "After inserting two entries, toString() must reflect those entries in map order.");
    }

    /**
     * Non‑empty map containing a null key and a null value.
     */
    @Test
    void mapWithNullKeyAndValue() {
        Map<String, String> map = new HashMap<>();
        map.put(null, null);
        TrackingMap<String, String> tm = new TrackingMap<>(map);
        assertEquals("{null=null}", tm.toString(), "Map with null key/value should produce \"{null=null}\".");
    }

    /**
     * Wrapped map overrides toString() with a custom string.
     */
    @Test
    void customMapToStringOverride() {
        class CustomMap extends HashMap<String, String> {

            @Override
            public String toString() {
                return "CUSTOM_MAP_STRING";
            }
        }
        TrackingMap<String, String> tm = new TrackingMap<>(new CustomMap());
        assertEquals("CUSTOM_MAP_STRING", tm.toString(), "TrackingMap should delegate directly to a custom toString() implementation.");
    }

    /**
     * Defensive test – internalMap set to null via reflection.
     */
    @Test
    void internalMapIsNullThrowsNPE() throws Exception {
        // Use a legitimate backing map first.
        TrackingMap<String, String> tm = new TrackingMap<>(new HashMap<>());
        // Break the internalMap field via reflection.
        Field internal = TrackingMap.class.getDeclaredField("internalMap");
        internal.setAccessible(true);
        internal.set(tm, null);
        assertThrows(NullPointerException.class, tm::toString, "Calling toString() when internalMap is null should throw NullPointerException.");
    }

    /**
     * Four distinct entries – ensure full concatenated representation.
     */
    @Test
    void multipleEntriesFour() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        TrackingMap<String, Integer> tm = new TrackingMap<>(map);
        assertEquals("{a=1, b=2, c=3, d=4}", tm.toString(), "Four entries should be represented exactly as the underlying map's toString().");
    }

    /**
     * Duplicate values with different keys.
     */
    @Test
    void duplicateValuesDifferentKeys() {
        Map<String, String> map = new HashMap<>();
        map.put("k1", "shared");
        map.put("k2", "shared");
        map.put("k3", "shared");
        TrackingMap<String, String> tm = new TrackingMap<>(map);
        // HashMap order is unspecified; we compare via Set semantics.
        String result = tm.toString();
        assertTrue(result.matches("\\{(k[1-3]=shared, ?)+\\}"), "All key‑value pairs should be present even when values are duplicated.");
    }

    /**
     * Complex object keys and values with their own toString() implementations.
     */
    @Test
    void complexObjectKeysAndValues() {
        class Key {

            private final String name;

            Key(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                return "Key[" + name + "]";
            }
        }
        class Value {

            private final int number;

            Value(int number) {
                this.number = number;
            }

            @Override
            public String toString() {
                return "Value[" + number + "]";
            }
        }
        Map<Key, Value> map = new LinkedHashMap<>();
        map.put(new Key("A"), new Value(10));
        TrackingMap<Key, Value> tm = new TrackingMap<>(map);
        assertEquals("{Key[A]=Value[10]}", tm.toString(), "Custom objects' toString() results must appear unchanged.");
    }

    /**
     * Wrapping an unmodifiable map should not affect toString().
     */
    @Test
    void unmodifiableWrappedMap() {
        Map<String, Integer> mutable = new HashMap<>();
        mutable.put("x", 100);
        mutable.put("y", 200);
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(mutable);
        TrackingMap<String, Integer> tm = new TrackingMap<>(unmodifiable);
        assertEquals("{x=100, y=200}", tm.toString(), "Unmodifiable wrapper must not interfere with string representation.");
    }

    /**
     * LinkedHashMap preserves insertion order in toString().
     */
    @Test
    void linkedHashMapPreservesInsertionOrder() {
        Map<String, Integer> linked = new LinkedHashMap<>();
        linked.put("first", 1);
        linked.put("second", 2);
        linked.put("third", 3);
        TrackingMap<String, Integer> tm = new TrackingMap<>(linked);
        assertEquals("{first=1, second=2, third=3}", tm.toString(), "Insertion order must be preserved.");
    }

    /**
     * Wrapped map whose toString() throws RuntimeException – should propagate.
     */
    @Test
    void underlyingMapToStringThrowsRuntimeException() {
        class BadMap extends HashMap<String, String> {

            @Override
            public String toString() {
                throw new RuntimeException("bad");
            }
        }
        TrackingMap<String, String> tm = new TrackingMap<>(new BadMap());
        RuntimeException ex = assertThrows(RuntimeException.class, tm::toString, "RuntimeException from underlying map must propagate.");
        assertEquals("bad", ex.getMessage());
    }

    /**
     * Wrapped map whose toString() returns null – result should be null.
     */
    @Test
    void underlyingMapToStringReturnsNull() {
        class NullStringMap extends HashMap<String, String> {

            @Override
            public String toString() {
                return null;
            }
        }
        TrackingMap<String, String> tm = new TrackingMap<>(new NullStringMap());
        assertNull(tm.toString(), "If underlying map returns null from toString(), TrackingMap must also return null.");
    }

    /**
     * Underlying map throws NullPointerException from toString().
     */
    @Test
    void underlyingMapToStringThrowsNullPointerException() {
        class NPEMap extends HashMap<String, String> {

            @Override
            public String toString() {
                throw new NullPointerException("intentional NPE");
            }
        }
        TrackingMap<String, String> tm = new TrackingMap<>(new NPEMap());
        NullPointerException ex = assertThrows(NullPointerException.class, tm::toString, "NullPointerException from underlying map must propagate.");
        assertEquals("intentional NPE", ex.getMessage());
    }

    /**
     * Underlying map throws IllegalArgumentException from toString().
     */
    @Test
    void underlyingMapToStringThrowsIllegalArgumentException() {
        class IAExceptionMap extends HashMap<String, String> {

            @Override
            public String toString() {
                throw new IllegalArgumentException("illegal");
            }
        }
        TrackingMap<String, String> tm = new TrackingMap<>(new IAExceptionMap());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, tm::toString, "IllegalArgumentException must propagate unchanged.");
        assertEquals("illegal", ex.getMessage());
    }

    /**
     * Underlying map throws ConcurrentModificationException from toString().
     */
    @Test
    void underlyingMapToStringThrowsConcurrentModificationException() {
        class CMEMap extends AbstractMap<String, String> {

            private final Map<String, String> delegate = new HashMap<>();

            {
                delegate.put("a", "1");
                delegate.put("b", "2");
            }

            @Override
            public Set<Entry<String, String>> entrySet() {
                // Return a view that will cause CME when iterated after modification.
                return delegate.entrySet();
            }

            @Override
            public String toString() {
                // Simulate concurrent change during toString().
                Iterator<Entry<String, String>> it = delegate.entrySet().iterator();
                // structural modification
                delegate.put("c", "3");
                // this will trigger CME
                it.next();
                return super.toString();
            }
        }
        TrackingMap<String, String> tm = new TrackingMap<>(new CMEMap());
        assertThrows(ConcurrentModificationException.class, tm::toString, "ConcurrentModificationException must be propagated.");
    }

    /**
     * Underlying map throws AssertionError (Error, not Exception).
     */
    @Test
    void underlyingMapToStringThrowsAssertionError() {
        class ErrorMap extends HashMap<String, String> {

            @Override
            public String toString() {
                throw new AssertionError("assertion failure");
            }
        }
        TrackingMap<String, String> tm = new TrackingMap<>(new ErrorMap());
        AssertionError err = assertThrows(AssertionError.class, tm::toString, "AssertionError must bubble up unchanged.");
        assertEquals("assertion failure", err.getMessage());
    }
}
