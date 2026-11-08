import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_toString_Test {

    class KeyObj {

        @Override
        public String toString() {
            return "KeyObj";
        }
    }

    /* -------------------------------------------------
       Helper classes used in a few tests
       -------------------------------------------------
    */
    class KeyObj {

        @Override
        public String toString() {
            return "KeyObj";
        }
    }

    class BadKey {

        @Override
        public String toString() {
            throw new RuntimeException("Key failure");
        }
    }

    class BadValue {

        @Override
        public String toString() {
            throw new RuntimeException("Value failure");
        }
    }

    class KeyObj {

        @Override
        public String toString() {
            return "KeyObj";
        }
    }

    /* -------------------------------------------------
    Helper classes used in a few tests
    -------------------------------------------------
     */
    static Stream<Arguments> nullKeyValueProvider() {
        return Stream.of(Arguments.of("null key and null value", new CompactMap<Object, Object>() {

            {
                put(null, null);
            }
        }, "{null=null}"), Arguments.of("null key with non‑null value", new CompactMap<Object, Object>() {

            {
                put(null, "val");
            }
        }, "{null=val}"), Arguments.of("non‑null key with null value", new CompactMap<Object, Object>() {

            {
                put("key", null);
            }
        }, "{key=null}"));
    }

    @Test
    void toStringEmptyMap() {
        CompactMap<String, Object> map = new CompactMap<>();
        assertTrue(map.isEmpty(), "Map should be empty before calling toString()");
        assertEquals("{}", map.toString(), "Empty map should be represented as {}");
    }

    @Test
    void toStringSingleEntry() {
        CompactMap<String, Object> map = new CompactMap<String, Object>() {

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected int compactSize() {
                // any value >=2 is fine
                return 3;
            }

            @Override
            protected Map<String, Object> getNewMap() {
                // Not used for a single entry, but required by the abstract class.
                return new LinkedHashMap<>();
            }
        };
        map.put("value", 10.0);
        assertEquals("{value=10.0}", map.toString(), "Map with a single entry should be represented using the standard {k=v} format");
    }

    @Test
    void toStringInsertionOrder() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().compactSize(5).build();
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);
        assertEquals("{first=1, second=2, third=3}", map.toString(), "Insertion‑ordered map should preserve entry order in toString()");
    }

    @Test
    void toStringSortedOrder() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().sortedOrder().compactSize(5).build();
        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("{a=1, b=2, c=3}", map.toString(), "Sorted map should list entries in key order in toString()");
    }

    @Test
    void toStringAfterModifications() {
        CompactMap<String, String> map = new CompactMap<String, String>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 4;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        // 1. empty
        assertEquals("{}", map.toString());
        // 2. add one entry (single‑value optimisation)
        map.put("id", "123");
        assertEquals("{id=123}", map.toString());
        // 3. add another entry – map transitions to compact array/backing map
        map.put("name", "Alice");
        assertEquals("{id=123, name=Alice}", map.toString());
        // 4. remove an entry
        map.remove("id");
        assertEquals("{name=Alice}", map.toString());
        // 5. clear the map
        map.clear();
        assertEquals("{}", map.toString());
    }

    // ---------- Null key / value cases ----------
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("nullKeyValueProvider")
    void toStringNullKeyOrValue(String description, Map<Object, Object> map, String expected) {
        assertEquals(expected, map.toString(), description);
    }

    // ---------- Case‑insensitive map ----------
    @Test
    void toStringCaseInsensitiveMap() {
        CompactMap<String, String> map = CompactMap.<String, String>builder().caseSensitive(false).insertionOrder().compactSize(5).build();
        map.put("Apple", "red");
        map.put("banana", "yellow");
        assertEquals("{Apple=red, banana=yellow}", map.toString(), "Case‑insensitive map should retain original key spelling");
    }

    // ---------- Reverse ordering ----------
    @Test
    void toStringReverseOrder() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().reverseOrder().compactSize(5).build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals("{c=3, b=2, a=1}", map.toString(), "Reverse‑ordered map must list entries in descending key order");
    }

    // ---------- Single‑key shortcut map ----------
    @Test
    void toStringSingleKeyShortcut() {
        CompactMap<String, String> map = CompactMap.<String, String>builder().singleValueKey("singleKey").compactSize(5).build();
        map.put("singleKey", "onlyValue");
        assertEquals("{singleKey=onlyValue}", map.toString(), "Map configured with SINGLE_KEY must render the single pair");
    }

    // ---------- Duplicate key replacement ----------
    @Test
    void toStringDuplicateKeyReplacement() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("a", 2);
        assertEquals("{a=2}", map.toString(), "Second put with same key must replace the first value");
    }

    // ---------- Removal effect ----------
    @Test
    void toStringRemovalEffect() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.remove("a");
        assertEquals("{b=2}", map.toString(), "After removal only remaining entries should appear");
    }

    // ---------- Clear after entries ----------
    @Test
    void toStringClearAfterEntries() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("x", 10);
        map.put("y", 20);
        map.clear();
        assertEquals("{}", map.toString(), "Clearing a populated map must yield an empty representation");
    }

    // ---------- Mixed type keys ----------
    @Test
    void toStringMixedTypeKeys() {
        CompactMap<Object, Object> map = new CompactMap<>();
        map.put(1, "one");
        map.put("two", 2);
        assertEquals("{1=one, two=2}", map.toString(), "Heterogeneous key types must be rendered in insertion order");
    }

    // ---------- Custom object as key ----------
    @Test
    void toStringCustomObjectKey() {
        class KeyObj {

            @Override
            public String toString() {
                return "KeyObj";
            }
        }
        CompactMap<Object, String> map = new CompactMap<>();
        map.put(new KeyObj(), "val");
        assertEquals("{KeyObj=val}", map.toString(), "Custom key object's toString() result must appear in map string");
    }

    // ---------- Collection value ----------
    @Test
    void toStringCollectionValue() {
        List<Integer> list = Arrays.asList(1, 2);
        CompactMap<String, List<Integer>> map = new CompactMap<>();
        map.put("list", list);
        assertEquals("{list=[1, 2]}", map.toString(), "The collection's own toString() should be embedded");
    }

    // ---------- Case‑sensitive distinct keys ----------
    @Test
    void toStringCaseSensitiveDistinctKeys() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("Key", 1);
        map.put("key", 2);
        String result = map.toString();
        assertTrue(result.equals("{Key=1, key=2}") || result.equals("{key=2, Key=1}"), "Case‑sensitive map must contain both entries with their distinct keys");
    }

    // ---------- Builder with explicit INSERTION ordering ----------
    @Test
    void toStringBuilderExplicitInsertion() {
        CompactMap<String, String> map = CompactMap.<String, String>builder().insertionOrder().compactSize(5).build();
        map.put("x", "X");
        map.put("y", "Y");
        assertEquals("{x=X, y=Y}", map.toString(), "Explicit insertion ordering must be reflected");
    }

    // ---------- Larger map (five entries) ----------
    @Test
    void toStringLargerMapFormatting() {
        CompactMap<String, Integer> unordered = new CompactMap<>();
        unordered.put("a", 1);
        unordered.put("b", 2);
        unordered.put("c", 3);
        unordered.put("d", 4);
        unordered.put("e", 5);
        CompactMap<String, Integer> ordered = CompactMap.<String, Integer>builder().insertionOrder().compactSize(10).build();
        ordered.putAll(unordered);
        assertEquals("{a=1, b=2, c=3, d=4, e=5}", ordered.toString(), "Five‑entry map must format commas and spaces correctly");
    }

    // ---------- Concurrent modification during toString ----------
    @Test
    void toStringConcurrentModification() throws Exception {
        CompactMap<Integer, Integer> map = CompactMap.<Integer, Integer>builder().compactSize(2).build();
        for (int i = 0; i < 5; i++) {
            map.put(i, i);
        }
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<String> future = exec.submit(() -> map.toString());
        Thread.sleep(10);
        map.put(99, 99);
        ExecutionException ee = assertThrows(ExecutionException.class, future::get, "Concurrent modification should cause CME inside toString()");
        assertTrue(ee.getCause() instanceof ConcurrentModificationException, "Root cause must be ConcurrentModificationException");
        exec.shutdownNow();
    }

    // ---------- Self‑referential map entry ----------
    @Test
    void toStringSelfReferentialEntry() {
        CompactMap<String, Object> map = new CompactMap<>();
        map.put("self", map);
        assertThrows(StackOverflowError.class, map::toString, "Putting the map itself as a value should cause StackOverflowError");
    }

    // ---------- Key toString throws RuntimeException ----------
    @Test
    void toStringKeyToStringThrows() {
        CompactMap<Object, String> map = new CompactMap<>();
        map.put(new BadKey(), "value");
        RuntimeException ex = assertThrows(RuntimeException.class, map::toString, "Exception from key.toString() must propagate");
        assertEquals("Key failure", ex.getMessage());
    }

    // ---------- Value toString throws RuntimeException ----------
    @Test
    void toStringValueToStringThrows() {
        CompactMap<String, Object> map = new CompactMap<>();
        map.put("key", new BadValue());
        RuntimeException ex = assertThrows(RuntimeException.class, map::toString, "Exception from value.toString() must propagate");
        assertEquals("Value failure", ex.getMessage());
    }
}
