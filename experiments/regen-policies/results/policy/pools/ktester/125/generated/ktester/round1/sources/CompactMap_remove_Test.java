package com.cedarsoftware.util;

import java.util.*;
// import org.junit.Test;
// import static org.junit.Assert.*;

class CompactMap_remove_Test {

//     @Test
//     void removeFromEmptyMapReturnsNull() {
//         Map<String, Object> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
//         };
//         assertTrue(map.isEmpty());
//         assertNull(map.remove("anyKey"));
//         assertEquals(0, map.size());
//     }

//     @Test
//     void removeSingleEntryReturnsValueAndLeavesMapEmpty() {
//         Map<String, Object> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
//         };
//         assertNull(map.put("value", 42));
//         assertEquals(1, map.size());
//         Object removed = map.remove("value");
//         assertEquals(42, removed);
//         assertEquals(0, map.size());
//         assertTrue(map.isEmpty());
//     }

//     @Test
//     void removeFromCompactArrayWorksCorrectly() {
//         Map<String, Object> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
//         };
//         assertNull(map.put("a", 1));
//         assertNull(map.put("b", 2));
//         assertEquals(2, map.size());
//         Object removed = map.remove("a");
//         assertEquals(1, removed);
//         assertEquals(1, map.size());
//         assertTrue(map.containsKey("b"));
//         assertFalse(map.containsKey("a"));
//     }

//     @Test
//     void removeFromBackingMapWorksCorrectly() {
//         Map<String, Object> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 return new HashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
//         };
//         map.put("k1", "v1");
//         map.put("k2", "v2");
//         map.put("k3", "v3");
//         map.put("k4", "v4");
//         assertEquals(4, map.size());
//         Object removed = map.remove("k2");
//         assertEquals("v2", removed);
//         assertEquals(3, map.size());
//         assertFalse(map.containsKey("k2"));
//         assertNull(map.remove("doesNotExist"));
//         assertEquals(3, map.size());
//     }

//     @Test
//     void removeNonExistentKeyReturnsNullRegardlessOfState() {
//         Map<String, Object> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
//         };
//         // empty
//         assertNull(map.remove("x"));
//         // single entry
//         map.put("value", "single");
//         assertNull(map.remove("x"));
//         map.remove("value");
//         // compact array (2 entries)
//         map.put("a", 1);
//         map.put("b", 2);
//         assertNull(map.remove("x"));
//         assertEquals(2, map.size());
//         // backing map (> compactSize)
//         map.put("c", 3);
//         map.put("d", 4);
//         assertNull(map.remove("x"));
//         assertEquals(4, map.size());
//     }

//     @Test
//     void removeFromCompactArrayExistingAndMissingKey() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 4;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
//         };
//         map.put("alpha", "A");
//         map.put("beta", "B");
//         assertEquals(2, map.size());
//         assertEquals("A", map.remove("alpha"));
//         assertEquals(1, map.size());
//         assertFalse(map.containsKey("alpha"));
//         assertTrue(map.containsKey("beta"));
//         assertNull(map.remove("gamma"));
//         assertEquals(1, map.size());
//         assertTrue(map.containsKey("beta"));
//     }

//     @Test
//     void removeNullKeyFromEmptyMapReturnsNull() {
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
//                 return "value";
//             }
//         };
//         assertNull(map.remove(null));
//         assertTrue(map.isEmpty());
//     }

//     @Test
//     void removeNullKeyWhenPresent() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 4;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
//         };
//         map.put(null, "nullValue");
//         assertEquals(1, map.size());
//         assertEquals("nullValue", map.remove(null));
//         assertTrue(map.isEmpty());
//     }

//     @Test
//     void removeSingleEntryExistingAndMissingKey() {
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
//                 return "solo";
//             }
//         };
//         map.put("solo", "value1");
//         assertEquals(1, map.size());
//         assertEquals("value1", map.remove("solo"));
//         assertTrue(map.isEmpty());
//         assertNull(map.remove("solo"));
//     }

//     @Test
//     void removeFromLargeBackingMapAndMissingKey() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new HashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
//         };
//         for (int i = 0; i < 10; i++) {
//             map.put("key" + i, "val" + i);
//         }
//         assertEquals(10, map.size());
//         assertEquals("val5", map.remove("key5"));
//         assertEquals(9, map.size());
//         assertNull(map.remove("nonexistent"));
//         assertEquals(9, map.size());
//         assertNull(map.remove(null));
//         assertEquals(9, map.size());
//     }

//     @Test
//     void transitionFromMapToCompactArrayAfterRemoval() {
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
//                 return "value";
//             }
//         };
//         map.put("k1", "v1");
//         map.put("k2", "v2");
//         map.put("k3", "v3");
//         map.put("trigger", "valTrigger");
//         assertEquals(4, map.size());
//         assertEquals("valTrigger", map.remove("trigger"));
//         assertEquals(3, map.size());
//         assertEquals("v2", map.remove("k2"));
//         assertEquals(2, map.size());
//         assertFalse(map.containsKey("k2"));
//     }

//     @Test
//     void transitionFromCompactArrayToEmptyAfterLastRemoval() {
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
//                 return "value";
//             }
//         };
//         map.put("first", "valFirst");
//         map.put("last", "valLast");
//         assertEquals(2, map.size());
//         map.remove("first");
//         assertEquals(1, map.size());
//         assertEquals("valLast", map.remove("last"));
//         assertTrue(map.isEmpty());
//         assertEquals(0, map.size());
//         assertNull(map.remove("last"));
//     }

//     @Test
//     void removeWithKeyTypeMismatchOnTreeMapThrowsClassCastException() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new TreeMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
//         };
//         map.put("a", "A");
//         map.put("b", "B");
//         map.put("c", "C");
//         assertEquals(3, map.size());
//         assertThrows(ClassCastException.class, () -> map.remove(123));
//     }
}