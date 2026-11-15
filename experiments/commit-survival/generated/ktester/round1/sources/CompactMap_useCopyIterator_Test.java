package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.stubbing.Answer;
import org.mockito.invocation.InvocationOnMock;

@ExtendWith(MockitoExtension.class)
class CompactMap_useCopyIterator_Test {

    private TestCompactMap compactMap;

    /**
     * Simple concrete subclass that lets us inject the map returned by {@code getNewMap()}
     */
    private static class TestCompactMap extends CompactMap<String, String> {

        private Map<String, String> mapToReturn;

        void setMapToReturn(Map<String, String> map) {
            this.mapToReturn = map;
        }

        @Override
        protected Map<String, String> getNewMap() {
            return mapToReturn;
        }

        // The abstract/required members of CompactMap – provide minimal implementations.
        @Override
        protected Map<String, String> getNewMap(int size) {
            // Not used by the tests for useCopyIterator()
            return mapToReturn;
        }

        @Override
        protected int compactSize() {
            // any value >= 2 satisfies the constructor check
            return 2;
        }

        @Override
        protected boolean isCaseInsensitive() {
            return false;
        }

        @Override
        protected K getSingleValueKey() {
            return null;
        }

        @Override
        protected LogicalValueType getLogicalValueType() {
            return null;
        }
    }

    /**
     * A simple subclass of TreeMap to verify that subclasses are still recognised as SortedMap.
     */
    private static class MyTreeMap<K, V> extends TreeMap<K, V> {

        private static final long serialVersionUID = 1L;
    }

    @BeforeEach
    void setupBeforeEach() {
        compactMap = new TestCompactMap();
    }

    @AfterEach
    void teardownAfterEach() {
        compactMap = null;
    }

    @Test
    void testUseCopyIteratorReturnsTrueForSortedMap() {
        // GIVEN a SortedMap (TreeMap) will be returned by getNewMap()
        Map<String, String> sortedMap = new TreeMap<>();
        compactMap.setMapToReturn(sortedMap);
        // WHEN
        boolean result = compactMap.useCopyIterator();
        // THEN
        assertTrue(result, "useCopyIterator should return true when the underlying map is a SortedMap");
    }

    @Test
    void testUseCopyIteratorReturnsFalseForHashMap() {
        // GIVEN a regular HashMap (not a SortedMap) will be returned by getNewMap()
        Map<String, String> hashMap = new HashMap<>();
        compactMap.setMapToReturn(hashMap);
        // WHEN
        boolean result = compactMap.useCopyIterator();
        // THEN
        assertFalse(result, "useCopyIterator should return false when the underlying map is not a SortedMap");
    }

    @Test
    void testUseCopyIteratorHandlesCaseInsensitiveMapWrappingSortedMap(@Mock CaseInsensitiveMap<String, String> ciMap) {
        // GIVEN a CaseInsensitiveMap that wraps a SortedMap
        Map<String, String> wrappedSortedMap = new TreeMap<>();
        when(ciMap.getWrappedMap()).thenReturn(wrappedSortedMap);
        // The mock must also be an instance of Map, which it already is because CaseInsensitiveMap extends Map
        compactMap.setMapToReturn(ciMap);
        // WHEN
        boolean result = compactMap.useCopyIterator();
        // THEN
        assertTrue(result, "useCopyIterator should unwrap a CaseInsensitiveMap and return true for a wrapped SortedMap");
        // Verify that getWrappedMap() was indeed called
        verify(ciMap, times(1)).getWrappedMap();
    }

    @Test
    void testUseCopyIteratorReturnsFalseWhenMapIsNull() {
        // GIVEN getNewMap() returns null
        compactMap.setMapToReturn(null);
        // WHEN
        boolean result = compactMap.useCopyIterator();
        // THEN
        assertFalse(result, "useCopyIterator should return false when getNewMap() returns null");
    }

    @Test
    void testUseCopyIteratorHandlesCaseInsensitiveMapWrappingNonSortedMap(@Mock CaseInsensitiveMap<String, String> ciMap) {
        // GIVEN a CaseInsensitiveMap that wraps a regular HashMap
        Map<String, String> wrappedHashMap = new HashMap<>();
        when(ciMap.getWrappedMap()).thenReturn(wrappedHashMap);
        compactMap.setMapToReturn(ciMap);
        // WHEN
        boolean result = compactMap.useCopyIterator();
        // THEN
        assertFalse(result, "useCopyIterator should unwrap a CaseInsensitiveMap and return false for a non‑SortedMap");
        verify(ciMap, times(1)).getWrappedMap();
    }

    @Test
    void testUseCopyIteratorHandlesCaseInsensitiveMapWrappingNull(@Mock CaseInsensitiveMap<String, String> ciMap) {
        // GIVEN a CaseInsensitiveMap whose getWrappedMap() returns null
        when(ciMap.getWrappedMap()).thenReturn(null);
        compactMap.setMapToReturn(ciMap);
        // WHEN
        boolean result = compactMap.useCopyIterator();
        // THEN
        assertFalse(result, "useCopyIterator should return false when a CaseInsensitiveMap wraps null");
        verify(ciMap, times(1)).getWrappedMap();
    }

    @Test
    void testUseCopyIteratorBothInterfacesWrappedSortedMap() {
        // GIVEN a mock that implements both SortedMap and CaseInsensitiveMap,
        // and whose getWrappedMap() returns a SortedMap.
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, String> ciMap = mock(CaseInsensitiveMap.class, withSettings().extraInterfaces(SortedMap.class));
        Map<String, String> wrappedSortedMap = new TreeMap<>();
        when(((CaseInsensitiveMap<String, String>) ciMap).getWrappedMap()).thenReturn(wrappedSortedMap);
        compactMap.setMapToReturn(ciMap);
        // WHEN
        boolean result = compactMap.useCopyIterator();
        // THEN
        assertTrue(result, "Both‑interface map wrapping a SortedMap should cause useCopyIterator to return true");
        verify(ciMap, times(1)).getWrappedMap();
    }

    @Test
    void testUseCopyIteratorBothInterfacesWrappedNonSortedMap() {
        // GIVEN a mock that implements both SortedMap and CaseInsensitiveMap,
        // and whose getWrappedMap() returns a regular HashMap.
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, String> ciMap = mock(CaseInsensitiveMap.class, withSettings().extraInterfaces(SortedMap.class));
        Map<String, String> wrappedHashMap = new HashMap<>();
        when(((CaseInsensitiveMap<String, String>) ciMap).getWrappedMap()).thenReturn(wrappedHashMap);
        compactMap.setMapToReturn(ciMap);
        // WHEN
        boolean result = compactMap.useCopyIterator();
        // THEN
        assertFalse(result, "Both‑interface map wrapping a non‑SortedMap should cause useCopyIterator to return false");
        verify(ciMap, times(1)).getWrappedMap();
    }

    @Test
    void testUseCopyIteratorBothInterfacesWrappedNull() {
        // GIVEN a mock that implements both SortedMap and CaseInsensitiveMap,
        // and whose getWrappedMap() returns null.
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, String> ciMap = mock(CaseInsensitiveMap.class, withSettings().extraInterfaces(SortedMap.class));
        when(((CaseInsensitiveMap<String, String>) ciMap).getWrappedMap()).thenReturn(null);
        compactMap.setMapToReturn(ciMap);
        // WHEN
        boolean result = compactMap.useCopyIterator();
        // THEN
        assertFalse(result, "Both‑interface map wrapping null should cause useCopyIterator to return false");
        verify(ciMap, times(1)).getWrappedMap();
    }

    @Test
    void testUseCopyIteratorDetectsSubclassOfSortedMap() {
        // GIVEN a custom subclass of TreeMap (which is a SortedMap)
        Map<String, String> myMap = new MyTreeMap<>();
        compactMap.setMapToReturn(myMap);
        // WHEN
        boolean result = compactMap.useCopyIterator();
        // THEN
        assertTrue(result, "A subclass of SortedMap should still be recognised as a SortedMap");
    }
}
