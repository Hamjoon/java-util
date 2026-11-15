package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_containsKey_Test {

    @Mock
    private ConcurrentMap<Object, Object> internalMapMock;

    private TestableConcurrentNullSafeMap<String, Integer> mapUnderTest;

    /**
     * Concrete subclass used only for testing the abstract class.
     */
    private static class TestableConcurrentNullSafeMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestableConcurrentNullSafeMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh instance before each test.
        mapUnderTest = new TestableConcurrentNullSafeMap<>(internalMapMock);
        mapUnderTest = new TestableConcurrentNullSafeMap<>(internalMapMock);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid interaction leakage between tests.
        reset(internalMapMock);
        reset(internalMapMock);
    }

    /* --------------------------------------------------------------------- */
    private static Object getNullKeySentinel() throws Exception {
        // The abstract class defines a protected static final sentinel (named NULL_KEY in the original library).
        // If the name differs, adjust accordingly.
        Field sentinelField = AbstractConcurrentNullSafeMap.class.getDeclaredField("NULL_KEY");
        sentinelField.setAccessible(true);
        return sentinelField.get(null);
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> nullKeyScenarios() throws Exception {
        Object sentinel = getNullKeySentinel();
        return Stream.of(// sentinel present → containsKey(null) should return true
        Arguments.of(sentinel, true, "sentinel present, expect true"), // sentinel absent → containsKey(null) should return false
        Arguments.of(sentinel, false, "sentinel absent, expect false"));
    }

    @Test
    void containsKey_delegatesToInternalMap_andReturnsTrue() {
        // GIVEN
        String key = "existingKey";
        when(internalMapMock.containsKey(key)).thenReturn(true);
        // WHEN
        boolean result = mapUnderTest.containsKey(key);
        // THEN
        assertTrue(result, "containsKey should return true when internal map reports true");
        verify(internalMapMock, times(1)).containsKey(key);
    }

    @Test
    void containsKey_delegatesToInternalMap_andReturnsFalse() {
        // GIVEN
        String key = "missingKey";
        when(internalMapMock.containsKey(key)).thenReturn(false);
        // WHEN
        boolean result = mapUnderTest.containsKey(key);
        // THEN
        assertFalse(result, "containsKey should return false when internal map reports false");
        verify(internalMapMock, times(1)).containsKey(key);
    }

    @Test
    void containsKey_handlesNullKey_usingMasking() {
        // GIVEN
        // The abstract class masks null keys with a sentinel object.
        // We don't know the sentinel, but we can capture the argument passed to the internal map.
        when(internalMapMock.containsKey(any())).thenReturn(false);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        // WHEN
        boolean result = mapUnderTest.containsKey(null);
        // THEN
        assertFalse(result, "containsKey should return the value from the internal map for null keys");
        verify(internalMapMock, times(1)).containsKey(captor.capture());
        Object passedKey = captor.getValue();
        assertNotNull(passedKey, "The key passed to internalMap must be a non‑null sentinel when the original key is null");
        // The sentinel should not be the literal null; this confirms masking took place.
        assertNotEquals(null, passedKey, "Sentinel object for null key should differ from null");
    }

    @ParameterizedTest(name = "containsKey(null) with sentinel {2}")
    @MethodSource("nullKeyScenarios")
    void containsKey_nullKeyMasking(Object sentinel, boolean internalResult, String _desc) throws Exception {
        // Arrange: internalMap will receive the sentinel (masked null) as the key.
        when(internalMapMock.containsKey(sentinel)).thenReturn(internalResult);
        // Act
        boolean result = mapUnderTest.containsKey(null);
        // Assert
        assertEquals(internalResult, result, "containsKey(null) must return the value supplied by internalMap for the masked sentinel");
        // Verify that the exact sentinel object was passed to the delegate.
        verify(internalMapMock, times(1)).containsKey(sentinel);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_directSentinel_present() throws Exception {
        Object sentinel = getNullKeySentinel();
        when(internalMapMock.containsKey(sentinel)).thenReturn(true);
        boolean result = mapUnderTest.containsKey(sentinel);
        assertTrue(result, "Directly passing the sentinel should delegate unchanged and return true");
        verify(internalMapMock, times(1)).containsKey(sentinel);
    }

    @Test
    void containsKey_directSentinel_absent() throws Exception {
        Object sentinel = getNullKeySentinel();
        when(internalMapMock.containsKey(sentinel)).thenReturn(false);
        boolean result = mapUnderTest.containsKey(sentinel);
        assertFalse(result, "Directly passing the sentinel when not present should return false");
        verify(internalMapMock, times(1)).containsKey(sentinel);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_wrongTypeObject_throwsClassCastException() {
        // not a String, the map's K is String
        Object wrongKey = new Object();
        assertThrows(ClassCastException.class, () -> mapUnderTest.containsKey(wrongKey), "Passing a key of incompatible type must cause ClassCastException");
        // No interaction with internalMap should have occurred.
        verifyNoInteractions(internalMapMock);
    }

    @Test
    void containsKey_wrongTypeBoolean_throwsClassCastException() {
        Boolean wrongKey = Boolean.TRUE;
        assertThrows(ClassCastException.class, () -> mapUnderTest.containsKey(wrongKey), "Passing a Boolean key to a String‑key map must cause ClassCastException");
        verifyNoInteractions(internalMapMock);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_internalMapNull_nullKey_throwsNPE() throws Exception {
        // Use reflection to set the internalMap field to null after construction.
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(mapUnderTest, null);
        assertThrows(NullPointerException.class, () -> mapUnderTest.containsKey(null), "When internalMap is null, containsKey(null) must throw NPE");
    }

    @Test
    void containsKey_internalMapNull_nonNullKey_throwsNPE() throws Exception {
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(mapUnderTest, null);
        assertThrows(NullPointerException.class, () -> mapUnderTest.containsKey("anyKey"), "When internalMap is null, containsKey(non‑null) must throw NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_existingStringKey_returnsTrue() {
        String key = "alpha";
        when(internalMapMock.containsKey(key)).thenReturn(true);
        assertTrue(mapUnderTest.containsKey(key));
        verify(internalMapMock).containsKey(key);
    }

    @Test
    void containsKey_existingIntegerKey_returnsTrue() {
        // The map is declared with String keys, but we can still test the raw behaviour
        // by casting the map to a raw type to avoid compile‑time generics checks.
        @SuppressWarnings("unchecked")
        TestableConcurrentNullSafeMap rawMap = new TestableConcurrentNullSafeMap(internalMapMock);
        Integer key = 123;
        when(internalMapMock.containsKey(key)).thenReturn(true);
        assertTrue(rawMap.containsKey(key));
        verify(internalMapMock).containsKey(key);
    }

    @Test
    void containsKey_missingStringKey_returnsFalse() {
        String key = "beta";
        when(internalMapMock.containsKey(key)).thenReturn(false);
        assertFalse(mapUnderTest.containsKey(key));
        verify(internalMapMock).containsKey(key);
    }

    @Test
    void containsKey_missingIntegerKey_returnsFalse() {
        @SuppressWarnings("unchecked")
        TestableConcurrentNullSafeMap rawMap = new TestableConcurrentNullSafeMap(internalMapMock);
        Integer key = 456;
        when(internalMapMock.containsKey(key)).thenReturn(false);
        assertFalse(rawMap.containsKey(key));
        verify(internalMapMock).containsKey(key);
    }
}
