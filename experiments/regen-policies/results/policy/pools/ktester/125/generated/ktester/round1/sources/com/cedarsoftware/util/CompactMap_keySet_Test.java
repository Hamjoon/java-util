package com.cedarsoftware.util;

import java.util.*;
// import java.util.concurrent.ConcurrentModificationException;

// import java.util.concurrent.ConcurrentModificationException;
// @ExtendWith(MockitoExtension.class)
class CompactMap_keySet_Test {

    /**
     * Map under test – a small CompactMap that uses a LinkedHashMap as backing store.
     */
    private CompactMap<String, Integer> map;

    /**
     * Cached key‑set view of {@link #map}.
     */
    private Set<String> keySet;

//     @BeforeEach
    void setupBeforeEach() {
        // Create a CompactMap with a compact size of 3 (so we stay in the
        // array/LinkedHashMap region for the small number of entries used
        // in the tests).  The single‑value key is required by the implementation.
        map = new CompactMap<String, Integer>() {

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                // Use a LinkedHashMap to keep insertion order predictable.
                return new LinkedHashMap<>();
            }
        };
        keySet = map.keySet();
    }

//     @AfterEach
    void teardownAfterEach() {
        if (map != null) {
            map.clear();
        }
        map = null;
        keySet = null;
    }

    /* -----------------------------------------------------------------
     *  Helper assertion methods (very small subset of JUnit Assertions)
     * ----------------------------------------------------------------- */
    private static void assertTrue(boolean condition) {
        if (!condition)
            throw new AssertionError("expected true but was false");
    }

    private static void assertFalse(boolean condition) {
        if (condition)
            throw new AssertionError("expected false but was true");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual))
            throw new AssertionError("expected [" + expected + "] but was [" + actual + "]");
    }

    private static void assertNotNull(Object obj) {
        if (obj == null)
            throw new AssertionError("expected non‑null value");
    }

    private static void assertNotEquals(Object unexpected, Object actual) {
        if (Objects.equals(unexpected, actual))
            throw new AssertionError("expected not equal to [" + unexpected + "]");
    }

    private static void assertThrows(Class<? extends Throwable> type, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            if (type.isInstance(t))
                return;
            throw new AssertionError("expected exception of type " + type.getName() + " but got " + t.getClass().getName(), t);
        }
        throw new AssertionError("expected exception of type " + type.getName() + " but no exception was thrown");
    }

    /* -----------------------------------------------------------------
     *  Test lifecycle helpers
     * ----------------------------------------------------------------- */
    private void setup() {
        map = new CompactMap<String, Integer>() {

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        keySet = map.keySet();
    }

    private void teardown() {
        if (map != null) {
            map.clear();
        }
        map = null;
        keySet = null;
    }

    /* -----------------------------------------------------------------
     *  Helper for tests that expect no exception.
     * ----------------------------------------------------------------- */
    private static void assertDoesNotThrow(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            throw new AssertionError("expected no exception, but got " + t, t);
        }
    }

    /* -----------------------------------------------------------------
     *  Main driver – runs every test method and reports success/failure.
     * ----------------------------------------------------------------- */
    public static void main(String[] args) {
        CompactMap_keySet_Test test = new CompactMap_keySet_Test();
        List<Runnable> tests = Arrays.asList(test::keySetReflectsMapSizeAndContains, test::keySetRemoveDeletesEntryFromMap, test::keySetClearEmptiesTheMap, test::keySetRemoveAllRemovesAllSpecifiedKeys, test::keySetRetainAllKeepsOnlySpecifiedKeys, test::iteratorRemoveWorksCorrectly, test::iteratorHasNextAndNextBehaviour, test::removeNullKeyWhenPresent, test::removeNullKeyWhenAbsent, test::retainAllEmptyCollectionClearsMap, test::removeAllEmptyCollectionDoesNotChangeMap, test::removeAllWithDuplicateKeysRemovesOncePerDistinctKey, test::iteratorRemoveBeforeNextThrowsIllegalStateException, test::iteratorDetectsConcurrentModification, test::clearOnEmptyMapIsNoOp, test::retainAllExactMatchReturnsFalse, test::addIsUnsupported, test::removeAllNullCollectionThrowsNPE, test::retainAllNullCollectionThrowsNPE, test::removeAllWithWrongElementTypeThrowsClassCastException, test::retainAllWithWrongElementTypeThrowsClassCastException);
        int passed = 0;
        int failed = 0;
        for (int i = 0; i < tests.size(); i++) {
            test.setup();
            try {
                tests.get(i).run();
                System.out.println("Test " + (i + 1) + " PASSED");
                passed++;
            } catch (AssertionError e) {
                System.err.println("Test " + (i + 1) + " FAILED: " + e.getMessage());
                e.printStackTrace();
                failed++;
            } catch (Throwable t) {
                System.err.println("Test " + (i + 1) + " ERRORED: " + t);
                t.printStackTrace();
                failed++;
            } finally {
                test.teardown();
            }
        }
        System.out.println("\n=== Summary ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

//     @Test
//     void keySetReflectsMapSizeAndContains() {
//         assertTrue(keySet.isEmpty());
//         assertEquals(0, keySet.size());
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         assertEquals(3, keySet.size());
//         assertFalse(keySet.isEmpty());
//         assertTrue(keySet.contains("a"));
//         assertTrue(keySet.contains("b"));
//         assertTrue(keySet.contains("c"));
//         assertFalse(keySet.contains("d"));
//     }

//     @Test
//     void keySetRemoveDeletesEntryFromMap() {
//         map.put("x", 10);
//         map.put("y", 20);
//         assertEquals(2, map.size());
//         assertTrue(keySet.remove("x"));
//         assertEquals(1, map.size());
//         assertFalse(map.containsKey("x"));
//         assertTrue(keySet.contains("y"));
//         assertFalse(keySet.remove("nonexistent"));
//         assertEquals(1, map.size());
//     }

//     @Test
//     void keySetClearEmptiesTheMap() {
//         map.put("one", 1);
//         map.put("two", 2);
//         map.put("three", 3);
//         assertEquals(3, map.size());
//         keySet.clear();
//         assertTrue(map.isEmpty());
//         assertTrue(keySet.isEmpty());
//         assertEquals(0, map.size());
//     }

//     @Test
//     void keySetRemoveAllRemovesAllSpecifiedKeys() {
//         map.put("k1", 1);
//         map.put("k2", 2);
//         map.put("k3", 3);
//         map.put("k4", 4);
//         assertEquals(4, map.size());
//         Collection<String> toRemove = Arrays.asList("k2", "k4", "doesNotExist");
//         assertTrue(keySet.removeAll(toRemove));
//         assertEquals(2, map.size());
//         assertFalse(map.containsKey("k2"));
//         assertFalse(map.containsKey("k4"));
//         assertTrue(map.containsKey("k1"));
//         assertTrue(map.containsKey("k3"));
//         assertFalse(keySet.removeAll(Collections.emptyList()));
//         assertEquals(2, map.size());
//     }

//     @Test
//     void keySetRetainAllKeepsOnlySpecifiedKeys() {
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         map.put("d", 4);
//         assertEquals(4, map.size());
//         Collection<String> toRetain = Arrays.asList("b", "c");
//         assertTrue(keySet.retainAll(toRetain));
//         assertEquals(2, map.size());
//         assertTrue(map.containsKey("b"));
//         assertTrue(map.containsKey("c"));
//         assertFalse(map.containsKey("a"));
//         assertFalse(map.containsKey("d"));
//         assertFalse(keySet.retainAll(toRetain));
//     }

//     @Test
//     void iteratorRemoveWorksCorrectly() {
//         map.put("one", 1);
//         map.put("two", 2);
//         map.put("three", 3);
//         assertEquals(3, map.size());
//         Iterator<String> it = keySet.iterator();
//         while (it.hasNext()) {
//             String key = it.next();
//             if ("two".equals(key)) {
//                 it.remove();
//             }
//         }
//         assertEquals(2, map.size());
//         assertFalse(map.containsKey("two"));
//         assertTrue(map.containsKey("one"));
//         assertTrue(map.containsKey("three"));
//     }

//     @Test
//     void iteratorHasNextAndNextBehaviour() {
//         map.put("alpha", 100);
//         map.put("beta", 200);
//         Iterator<String> it = keySet.iterator();
//         assertTrue(it.hasNext());
//         String first = it.next();
//         assertNotNull(first);
//         assertTrue(first.equals("alpha") || first.equals("beta"));
//         assertTrue(it.hasNext());
//         String second = it.next();
//         assertNotNull(second);
//         assertNotEquals(first, second);
//         assertFalse(it.hasNext());
//         assertThrows(NoSuchElementException.class, it::next);
//     }

    /* --------------------------------------------------------------
     *  remove(null) – behavior when the map contains (or does not contain) a null key
     * -------------------------------------------------------------- */
//     @Test
//     void removeNullKeyWhenPresent() {
//         map.put(null, 42);
//         map.put("present", 1);
//         assertEquals(2, map.size());
//         assertTrue(keySet.remove(null));
//         assertEquals(1, map.size());
//         assertFalse(map.containsKey(null));
//     }

//     @Test
//     void removeNullKeyWhenAbsent() {
//         map.put("only", 5);
//         assertEquals(1, map.size());
//         assertFalse(keySet.remove(null));
//         assertEquals(1, map.size());
//     }

    /* --------------------------------------------------------------
     *  retainAll(emptyCollection) – should clear the map and return true
     * -------------------------------------------------------------- */
//     @Test
//     void retainAllEmptyCollectionClearsMap() {
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         assertEquals(3, map.size());
//         assertTrue(keySet.retainAll(Collections.emptyList()));
//         assertTrue(map.isEmpty());
//         assertEquals(0, keySet.size());
//     }

    /* --------------------------------------------------------------
     *  removeAll(emptyCollection) – should return false and leave map unchanged
     * -------------------------------------------------------------- */
//     @Test
//     void removeAllEmptyCollectionDoesNotChangeMap() {
//         map.put("x", 10);
//         map.put("y", 20);
//         assertEquals(2, map.size());
//         assertFalse(keySet.removeAll(Collections.emptyList()));
//         assertEquals(2, map.size());
//     }

    /* --------------------------------------------------------------
     *  removeAll with duplicate keys – each distinct key removed once,
     *  method returns true because the map changed.
     * -------------------------------------------------------------- */
//     @Test
//     void removeAllWithDuplicateKeysRemovesOncePerDistinctKey() {
//         map.put("dup", 1);
//         map.put("unique", 2);
//         assertEquals(2, map.size());
//         List<String> dupList = Arrays.asList("dup", "dup", "dup");
//         assertTrue(keySet.removeAll(dupList));
//         assertEquals(1, map.size());
//         assertFalse(map.containsKey("dup"));
//         assertTrue(map.containsKey("unique"));
//     }

    /* --------------------------------------------------------------
     *  iterator contract – IllegalStateException when remove() called before next()
     * -------------------------------------------------------------- */
//     @Test
//     void iteratorRemoveBeforeNextThrowsIllegalStateException() {
//         map.put("one", 1);
//         Iterator<String> it = keySet.iterator();
//         assertThrows(IllegalStateException.class, it::remove);
//     }

    /* --------------------------------------------------------------
     *  iterator contract – ConcurrentModificationException after external modification
     * -------------------------------------------------------------- */
//     @Test
//     void iteratorDetectsConcurrentModification() {
//         map.put("a", 1);
//         map.put("b", 2);
//         Iterator<String> it = keySet.iterator();
//         // Structural change outside the iterator.
//         map.put("c", 3);
//         assertThrows(ConcurrentModificationException.class, it::next);
//     }

    /* --------------------------------------------------------------
     *  clear on an already empty map – should be a no‑op without exception
     * -------------------------------------------------------------- */
//     @Test
//     void clearOnEmptyMapIsNoOp() {
//         assertTrue(map.isEmpty());
//         assertDoesNotThrow(() -> keySet.clear());
//         assertTrue(map.isEmpty());
//         assertEquals(0, keySet.size());
//     }

    /* --------------------------------------------------------------
     *  retainAll with exact current key set – should return false (no change)
     * -------------------------------------------------------------- */
//     @Test
//     void retainAllExactMatchReturnsFalse() {
//         map.put("k1", 1);
//         map.put("k2", 2);
//         List<String> exact = Arrays.asList("k1", "k2");
//         assertFalse(keySet.retainAll(exact));
//         assertEquals(2, map.size());
//     }

    /* --------------------------------------------------------------
     *  add operation – unsupported, must throw UnsupportedOperationException
     * -------------------------------------------------------------- */
//     @Test
//     void addIsUnsupported() {
//         assertThrows(UnsupportedOperationException.class, () -> keySet.add("newKey"));
//     }

    /* --------------------------------------------------------------
     *  null collection arguments – should throw NullPointerException
     * -------------------------------------------------------------- */
//     @Test
//     void removeAllNullCollectionThrowsNPE() {
//         assertThrows(NullPointerException.class, () -> keySet.removeAll(null));
//     }

//     @Test
//     void retainAllNullCollectionThrowsNPE() {
//         assertThrows(NullPointerException.class, () -> keySet.retainAll(null));
//     }

    /* --------------------------------------------------------------
     *  type mismatch in collection – ClassCastException expected
     * -------------------------------------------------------------- */
//     @Test
//     void removeAllWithWrongElementTypeThrowsClassCastException() {
//         CompactMap<Integer, String> intMap = new CompactMap<Integer, String>() {
// 
//             @Override
//             protected Integer getSingleValueKey() {
//                 return 0;
//             }
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<Integer, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         Set<Integer> intKeySet = intMap.keySet();
//         intMap.put(1, "one");
//         intMap.put(2, "two");
//         List<String> wrongType = Collections.singletonList("not-an-integer");
//         assertThrows(ClassCastException.class, () -> intKeySet.removeAll(wrongType));
//     }

//     @Test
//     void retainAllWithWrongElementTypeThrowsClassCastException() {
//         CompactMap<Integer, String> intMap = new CompactMap<Integer, String>() {
// 
//             @Override
//             protected Integer getSingleValueKey() {
//                 return 0;
//             }
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<Integer, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         Set<Integer> intKeySet = intMap.keySet();
//         intMap.put(10, "ten");
//         intMap.put(20, "twenty");
//         List<String> wrongType = Collections.singletonList("stringKey");
//         assertThrows(ClassCastException.class, () -> intKeySet.retainAll(wrongType));
//     }
}