package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_containsKey_Test {

    /**
     * Mocked backing map used to verify delegation.
     */
    @Mock
    private ConcurrentMap<Object, Object> internalMapMock;

    /**
     * Concrete instance of the abstract class under test.
     */
    private TestMap<String, String> testMap;

    /**
     * Simple concrete subclass used only for testing purposes.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
    }

    /**
     * Subclass that uses a real {@link ConcurrentHashMap} as backing store.
     */
    private static class RealMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        RealMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    /**
     * Subclass that deliberately passes {@code null} as the backing map – used to trigger NPE.
     */
    private static class NullInternalMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        NullInternalMap() {
            super(null);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test so that interactions are isolated.
        testMap = new TestMap<>(internalMapMock);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that no unexpected interactions occurred.
        verifyNoMoreInteractions(internalMapMock);
    }

    @Test
    void containsKey_delegatesToInternalMap_forNonNullKey() {
        // GIVEN
        String key = "nonNullKey";
        when(internalMapMock.containsKey(key)).thenReturn(true);
        // WHEN
        boolean result = testMap.containsKey(key);
        // THEN
        assertTrue(result, "containsKey should return the value from the internal map");
        verify(internalMapMock).containsKey(key);
    }

    @Test
    void containsKey_delegatesToInternalMap_forNullKey() {
        // GIVEN
        // The sentinel used for a null key is internal; we only care that a *non‑null* object
        // is passed to the backing map.
        when(internalMapMock.containsKey(any())).thenReturn(false);
        // WHEN
        boolean result = testMap.containsKey(null);
        // THEN
        assertFalse(result, "containsKey should return the value from the internal map");
        // Capture the argument that was forwarded to the internal map.
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(internalMapMock).containsKey(captor.capture());
        Object forwardedKey = captor.getValue();
        assertNotNull(forwardedKey, "The masked key for a null argument must not be null");
        // The actual sentinel type is an implementation detail; we only assert that it is not the
        // original null reference.
        assertNotEquals(null, forwardedKey, "The forwarded key should be the null‑sentinel, not null");
    }

    @ParameterizedTest(name = "[{index}] key=''{0}'' → expected={1}")
    @CsvSource({ "'alpha', true", "'beta', false" })
    void containsKey_delegatesToInternalMap_forVariousNonNullKeys(String key, boolean expected) {
        // GIVEN
        when(internalMapMock.containsKey(key)).thenReturn(expected);
        // WHEN
        boolean result = testMap.containsKey(key);
        // THEN
        assertEquals(expected, result, () -> "containsKey should return " + expected + " for key '" + key + "'");
        verify(internalMapMock).containsKey(key);
    }

    @Test
    void containsKey_returnsTrueWhenNullKeyPresent() {
        // GIVEN a real map that stores a null key
        AbstractConcurrentNullSafeMap<String, String> map = new RealMap<>();
        map.put(null, "nullValue");
        // WHEN
        boolean result = map.containsKey(null);
        // THEN
        assertTrue(result, "containsKey(null) should be true after a null key has been put");
    }

    @Test
    void containsKey_returnsFalseWhenNullKeyAbsent() {
        // GIVEN a real map without a null key
        AbstractConcurrentNullSafeMap<String, String> map = new RealMap<>();
        // WHEN
        boolean result = map.containsKey(null);
        // THEN
        assertFalse(result, "containsKey(null) should be false when no null key is present");
    }

    @Test
    void containsKey_returnsFalseAfterKeyRemoval() {
        // GIVEN a map where a key is inserted and then removed
        AbstractConcurrentNullSafeMap<String, String> map = new RealMap<>();
        map.put("gamma", "value");
        map.remove("gamma");
        // WHEN
        boolean result = map.containsKey("gamma");
        // THEN
        assertFalse(result, "containsKey should return false after the key has been removed");
    }

    @Test
    void containsKey_throwsClassCastExceptionForIncompatibleKeyType() {
        // GIVEN a map whose generic key type is String
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> testMap.containsKey(123), "Passing an Integer to a String‑keyed map should throw ClassCastException");
    }

    @Test
    void containsKey_throwsNullPointerExceptionWhenInternalMapIsNull() {
        // GIVEN a map whose internal backing map is deliberately null
        AbstractConcurrentNullSafeMap<String, String> map = new NullInternalMap<>();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.containsKey("any"), "Calling containsKey when internalMap is null should throw NullPointerException");
    }
}
