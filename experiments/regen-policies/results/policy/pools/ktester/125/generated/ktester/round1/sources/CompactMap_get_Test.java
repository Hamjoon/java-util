package com.cedarsoftware.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.lang.reflect.Field;
import java.util.TreeMap;
import static org.mockito.Mockito.*;
// import org.junit.Test;
// import static org.junit.Assert.*;

class CompactMap_get_Test {

    /* --------------------------------------------------------------------- */
    private static void setInternalVal(CompactMap<?, ?> map, Object newVal) {
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(map, newVal);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getFromEmptyMapReturnsNull() {
//         CompactMap<String, Integer> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 // any value >= 2
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "single";
//             }
//         };
//         assertTrue(map.isEmpty());
//         assertNull(map.get("anyKey"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getFromSingleEntryMap() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
//         };
//         // put a single entry
//         assertNull(map.put("alpha", "value1"));
//         assertEquals(1, map.size());
//         // successful look‑up
//         assertEquals("value1", map.get("alpha"));
//         // lookup for a non‑existing key must return null
//         assertNull(map.get("beta"));
//         // null key as the sole key
//         CompactMap<String, String> mapWithNullKey = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
//         };
//         assertNull(mapWithNullKey.put(null, "nullKeyValue"));
//         assertEquals("nullKeyValue", mapWithNullKey.get(null));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getFromCompactArray() {
//         CompactMap<String, Integer> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 // keep us in the array zone for up to 5 entries
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "key";
//             }
//         };
//         // Fill the map with three entries – still inside the compact array range
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         assertEquals(3, map.size());
//         // Verify that each key is found correctly
//         assertEquals(Integer.valueOf(1), map.get("a"));
//         assertEquals(Integer.valueOf(2), map.get("b"));
//         assertEquals(Integer.valueOf(3), map.get("c"));
//         // Missing key → null
//         assertNull(map.get("missing"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getFromBackingMap() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 // after 2 entries we force a backing map
//                 return 2;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
//         };
//         // Add three entries – the third addition should trigger the switch
//         map.put("x", "X");
//         map.put("y", "Y");
//         // now internal val is a Map
//         map.put("z", "Z");
//         assertEquals(3, map.size());
//         // All look‑ups must work via the backing map
//         assertEquals("X", map.get("x"));
//         assertEquals("Y", map.get("y"));
//         assertEquals("Z", map.get("z"));
//         assertNull(map.get("unknown"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getWithCaseInsensitiveKeys() {
//         CompactMap<String, Integer> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 // TreeMap with case‑insensitive comparator to emulate case‑insensitive mode
//                 return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
//             }
// 
//             @Override
//             protected boolean isCaseInsensitive() {
//                 return true;
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "key";
//             }
//         };
//         map.put("KeyOne", 10);
//         map.put("KeyTwo", 20);
//         // Different case must still resolve to the stored value
//         assertEquals(Integer.valueOf(10), map.get("keyone"));
//         assertEquals(Integer.valueOf(20), map.get("KEYTWO"));
//         assertNull(map.get("keythree"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getFromCompactArray_TwoEntries() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "key";
//             }
//         };
//         // two entries stay inside the compact array representation
//         map.put("first", "one");
//         map.put("second", "two");
//         assertEquals(2, map.size());
//         assertEquals("one", map.get("first"));
//         assertEquals("two", map.get("second"));
//         assertNull(map.get("missing"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getFromBackingMap_WhenSizeExceedsCompactSize() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             // trigger map after 2 entries
//             @Override
//             protected int compactSize() {
//                 return 2;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
//         };
//         map.put("x", "X");
//         map.put("y", "Y");
//         // third entry forces transition to a Map
//         map.put("z", "Z");
//         assertEquals(3, map.size());
//         assertEquals("X", map.get("x"));
//         assertEquals("Y", map.get("y"));
//         assertEquals("Z", map.get("z"));
//         assertNull(map.get("unknown"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getWithDefaultCaseSensitiveEquality() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "key";
//             }
//         };
//         map.put("Key", "upper");
//         assertEquals("upper", map.get("Key"));
//         // different case → not equal
//         assertNull(map.get("key"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getWithCaseInsensitiveOption() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 // TreeMap with case‑insensitive comparator
//                 return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
//             }
// 
//             @Override
//             protected boolean isCaseInsensitive() {
//                 return true;
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "key";
//             }
//         };
//         map.put("MiXeD", "caseInsensitiveValue");
//         assertEquals("caseInsensitiveValue", map.get("MiXeD"));
//         assertEquals("caseInsensitiveValue", map.get("mixed"));
//         assertEquals("caseInsensitiveValue", map.get("MIXED"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getHandlesNullKeyAlongsideOtherEntries() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "key";
//             }
//         };
//         map.put(null, "nullValue");
//         map.put("someKey", "someValue");
//         assertEquals("nullValue", map.get(null));
//         assertEquals("someValue", map.get("someKey"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getAfterRemovalBehavesCorrectly() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "key";
//             }
//         };
//         map.put("toRemove", "oldValue");
//         map.put("stillThere", "stillValue");
//         // remove one key
//         assertEquals("oldValue", map.remove("toRemove"));
//         assertNull(map.get("toRemove"));
//         // other key must stay accessible
//         assertEquals("stillValue", map.get("stillThere"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getThrowsArrayIndexOutOfBoundsWhenInternalArrayIsCorrupted() throws Exception {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "key";
//             }
//         };
//         // Manually inject a malformed Object[] (odd length)
//         Object[] malformed = new Object[] { "k1", "v1", "k2" };
//         setInternalVal(map, malformed);
//         // The loop in get will attempt to read entries[i+1] for i=2 and fail
//         try {
//             map.get("anyKey");
//             fail("Expected ArrayIndexOutOfBoundsException");
//         } catch (ArrayIndexOutOfBoundsException e) {
//             // expected
//         }
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getThrowsNullPointerExceptionWhenInternalValIsNull() throws Exception {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "key";
//             }
//         };
//         // Force `val` to null
//         setInternalVal(map, null);
//         // The method will fall through to single‑entry logic that dereferences internal state
//         try {
//             map.get("anyKey");
//             fail("Expected NullPointerException");
//         } catch (NullPointerException e) {
//             // expected
//         }
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void getPropagatesExceptionFromBackingMap() throws Exception {
//         // Create a mock Map that throws IllegalStateException on get()
//         @SuppressWarnings("unchecked")
//         Map<String, String> explodingMap = mock(Map.class);
//         when(explodingMap.get("trigger")).thenThrow(new IllegalStateException("boom"));
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 2;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
//         };
//         // Populate enough entries to force transition to a backing map
//         map.put("a", "A");
//         map.put("b", "B");
//         // now internal val is a Map
//         map.put("c", "C");
//         // Replace the internal map with the exploding mock
//         setInternalVal(map, explodingMap);
//         // The IllegalStateException must bubble up unchanged
//         try {
//             map.get("trigger");
//             fail("Expected IllegalStateException");
//         } catch (IllegalStateException ex) {
//             assertEquals("boom", ex.getMessage());
//         }
//     }
}