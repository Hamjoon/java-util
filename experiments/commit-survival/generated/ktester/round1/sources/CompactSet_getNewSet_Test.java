package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactSet_getNewSet_Test {

    /**
     * Simple subclass that makes the protected getNewSet() method public for testing.
     */
    private static class TestCompactSet extends CompactSet<Object> {

        @Override
        protected int compactSize() {
            // overridden in specific tests when needed
            return super.compactSize();
        }

        /**
         * Expose the protected method.
         */
        public Set<Object> exposeGetNewSet() {
            return super.getNewSet();
        }
    }

    /**
     * Subclass that overrides compactSize() to a custom value.
     */
    private static class CustomSizeCompactSet extends CompactSet<Object> {

        private final int customSize;

        CustomSizeCompactSet(int customSize) {
            this.customSize = customSize;
        }

        @Override
        protected int compactSize() {
            return customSize;
        }

        public Set<Object> exposeGetNewSet() {
            return super.getNewSet();
        }
    }

    /**
     * Helper that extracts the internal HashMap capacity of a HashSet via reflection.
     */
    private int getHashSetCapacity(Set<?> set) throws Exception {
        // HashSet has a private field "map" of type HashMap
        Field mapField = HashSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        Object hashMap = mapField.get(set);
        // HashMap has a private field "table" (the bucket array)
        Field tableField = java.util.HashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(hashMap);
        return table == null ? 0 : table.length;
    }

    /**
     * Expected HashMap capacity calculation mirrors the JDK implementation.
     */
    private int expectedHashMapCapacity(int initialCapacity) {
        // HashMap uses a load factor of 0.75 and rounds up to the next power of two.
        final float loadFactor = 0.75f;
        int capacity = 1;
        while (capacity < (int) Math.ceil(initialCapacity / loadFactor)) {
            capacity <<= 1;
        }
        return capacity;
    }

    @Test
    void getNewSet_returnsEmptyHashSet_withDefaultCompactSize() throws Exception {
        TestCompactSet cs = new TestCompactSet();
        Set<Object> newSet = cs.exposeGetNewSet();
        assertNotNull(newSet, "getNewSet() must not return null");
        assertTrue(newSet instanceof HashSet, "Default implementation should be a HashSet");
        assertTrue(newSet.isEmpty(), "New set must be empty");
        // Verify the internal capacity matches compactSize() + 1
        // default compactSize() is 80
        int expectedInitial = cs.compactSize() + 1;
        int actualCapacity = getHashSetCapacity(newSet);
        int expectedCapacity = expectedHashMapCapacity(expectedInitial);
        assertEquals(expectedCapacity, actualCapacity, "HashSet internal capacity should correspond to compactSize()+1");
    }

    @Test
    void getNewSet_returnsEmptyHashSet_withCustomCompactSize() throws Exception {
        // any value >= 2
        int customCompactSize = 5;
        CustomSizeCompactSet cs = new CustomSizeCompactSet(customCompactSize);
        Set<Object> newSet = cs.exposeGetNewSet();
        assertNotNull(newSet, "getNewSet() must not return null");
        assertTrue(newSet instanceof HashSet, "Implementation should be a HashSet");
        assertTrue(newSet.isEmpty(), "New set must be empty");
        // Verify the internal capacity matches custom compactSize() + 1
        int expectedInitial = customCompactSize + 1;
        int actualCapacity = getHashSetCapacity(newSet);
        int expectedCapacity = expectedHashMapCapacity(expectedInitial);
        assertEquals(expectedCapacity, actualCapacity, "HashSet internal capacity should correspond to custom compactSize()+1");
    }

    @Test
    void getNewSet_canAddElementsUpToCompactSize() {
        TestCompactSet cs = new TestCompactSet();
        Set<Object> newSet = cs.exposeGetNewSet();
        // Add exactly compactSize() elements – should succeed without errors.
        for (int i = 0; i < cs.compactSize(); i++) {
            assertTrue(newSet.add("elem-" + i), "Element should be added successfully");
        }
        assertEquals(cs.compactSize(), newSet.size(), "Set size should equal compactSize() after adding that many elements");
    }

    @Test
    void getNewSet_allowsNullElement() {
        TestCompactSet cs = new TestCompactSet();
        Set<Object> newSet = cs.exposeGetNewSet();
        assertTrue(newSet.add(null), "Null element should be accepted");
        assertTrue(newSet.contains(null), "Set should contain the null element");
        assertEquals(1, newSet.size(), "Set size should be 1 after adding null");
    }

    @Test
    void getNewSet_basicBehavior_defaultSubclass_returnsEmptyHashSet() {
        // Invoke getNewSet() on a concrete subclass that does not override compactSize().
        TestCompactSet cs = new TestCompactSet();
        Set<Object> set = cs.exposeGetNewSet();
        assertNotNull(set, "Returned set must not be null");
        assertTrue(set instanceof HashSet, "Returned set should be a HashSet");
        assertEquals(0, set.size(), "Set should be empty (size 0)");
    }

    @Test
    void getNewSet_overriddenCompactSize_smallPositive_returnsDistinctInstances() {
        // Anonymous subclass overriding compactSize() to return 5.
        CompactSet<Object> cs = new CompactSet<>() {

            @Override
            protected int compactSize() {
                return 5;
            }

            @Override
            public Set<Object> exposeGetNewSet() {
                return super.getNewSet();
            }
        };
        Set<Object> first = ((CompactSet<Object>) cs).exposeGetNewSet();
        Set<Object> second = ((CompactSet<Object>) cs).exposeGetNewSet();
        assertNotNull(first);
        assertNotNull(second);
        assertTrue(first instanceof HashSet);
        assertTrue(second instanceof HashSet);
        assertEquals(0, first.size());
        assertEquals(0, second.size());
        assertNotSame(first, second, "Each call should return a distinct HashSet instance");
    }

    @Test
    void getNewSet_overriddenCompactSize_zero_returnsEmptyHashSet() {
        CompactSet<Object> cs = new CompactSet<>() {

            @Override
            protected int compactSize() {
                return 0;
            }

            @Override
            public Set<Object> exposeGetNewSet() {
                return super.getNewSet();
            }
        };
        Set<Object> set = ((CompactSet<Object>) cs).exposeGetNewSet();
        assertNotNull(set);
        assertTrue(set instanceof HashSet);
        assertEquals(0, set.size(), "Even with compactSize 0 the set must be empty");
    }

    @Test
    void getNewSet_overriddenCompactSize_largePositive_returnsEmptyHashSet() {
        CompactSet<Object> cs = new CompactSet<>() {

            @Override
            protected int compactSize() {
                return 1000;
            }

            @Override
            public Set<Object> exposeGetNewSet() {
                return super.getNewSet();
            }
        };
        Set<Object> set = ((CompactSet<Object>) cs).exposeGetNewSet();
        assertNotNull(set);
        assertTrue(set instanceof HashSet);
        assertEquals(0, set.size(), "Set should be empty regardless of large initial capacity");
    }

    @Test
    void getNewSet_overriddenCompactSize_maxInt_throwsIllegalArgumentException() {
        CompactSet<Object> cs = new CompactSet<>() {

            @Override
            protected int compactSize() {
                return Integer.MAX_VALUE;
            }

            @Override
            public Set<Object> exposeGetNewSet() {
                return super.getNewSet();
            }
        };
        // The overflow (MAX_VALUE + 1) leads to a negative initial capacity for HashSet,
        // which results in IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> ((CompactSet<Object>) cs).exposeGetNewSet(), "Expected IllegalArgumentException due to overflowed capacity");
    }

    @Test
    void getNewSet_overriddenCompactSize_negative_throwsIllegalArgumentException() {
        CompactSet<Object> cs = new CompactSet<>() {

            @Override
            protected int compactSize() {
                return -1;
            }

            @Override
            public Set<Object> exposeGetNewSet() {
                return super.getNewSet();
            }
        };
        // compactSize() = -1 => -1 + 1 = 0, HashSet constructor rejects non‑positive capacity.
        assertThrows(IllegalArgumentException.class, () -> ((CompactSet<Object>) cs).exposeGetNewSet(), "Expected IllegalArgumentException for negative compactSize");
    }

    @Test
    void getNewSet_functionalBehavior_ofReturnedSet() {
        // Subclass with compactSize() returning 3.
        CompactSet<Object> cs = new CompactSet<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            public Set<Object> exposeGetNewSet() {
                return super.getNewSet();
            }
        };
        Set<Object> set = ((CompactSet<Object>) cs).exposeGetNewSet();
        // Add three distinct elements.
        assertTrue(set.add("a"));
        assertTrue(set.add("b"));
        assertTrue(set.add("c"));
        assertEquals(3, set.size());
        // Verify contains.
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
        // Remove one element and verify size/contains.
        assertTrue(set.remove("b"));
        assertEquals(2, set.size());
        assertFalse(set.contains("b"));
    }

    @Test
    void getNewSet_compactSizeThrowsRuntimeException_propagates() {
        CompactSet<Object> cs = new CompactSet<>() {

            @Override
            protected int compactSize() {
                throw new RuntimeException("forced");
            }

            @Override
            public Set<Object> exposeGetNewSet() {
                return super.getNewSet();
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ((CompactSet<Object>) cs).exposeGetNewSet(), "RuntimeException from compactSize() should propagate");
        assertEquals("forced", ex.getMessage());
    }
}
