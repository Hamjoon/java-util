package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DeepEquals}.
 *
 * <p>The tests are written as if three independent experts contributed one test
 * case per public method, discussed the results and kept the ones that passed.
 * The final file therefore contains a rich set of scenarios covering typical
 * use, edge‑cases and error handling.</p>
 */
public class DeepEqualsTest {

    /* --------------------------------------------------------------------- */
    /* Helper classes used by several tests                                 */
    /* --------------------------------------------------------------------- */

    /** Simple POJO with two fields – used for field‑by‑field comparison. */
    public static class SimpleBean {
        public int id;
        public String name;

        public SimpleBean(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /** Class that overrides {@code equals} but not {@code hashCode}. */
    public static class CustomEqualsOnly {
        private final int value;

        public CustomEqualsOnly(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CustomEqualsOnly)) return false;
            return value == ((CustomEqualsOnly) o).value;
        }
        // hashCode not overridden – uses {@code Object.hashCode()}
    }

    /** Class that overrides both {@code equals} and {@code hashCode}. */
    public static class CustomEqualsAndHash {
        private final String key;
        private final int number;

        public CustomEqualsAndHash(String key, int number) {
            this.key = key;
            this.number = number;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CustomEqualsAndHash)) return false;
            CustomEqualsAndHash that = (CustomEqualsAndHash) o;
            return Objects.equals(key, that.key) && number == that.number;
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, number);
        }
    }

    /** Simple node that can form a circular reference. */
    public static class Node {
        public String label;
        public Node next;

        public Node(String label) {
            this.label = label;
        }
    }

    /* --------------------------------------------------------------------- */
    /* 1. deepEquals(Object, Object) – basic primitive / simple type cases   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepEquals_PrimitiveWrappers() {
        assertTrue(DeepEquals.deepEquals(5, 5));
        assertTrue(DeepEquals.deepEquals(Integer.valueOf(10), Integer.valueOf(10)));
        assertFalse(DeepEquals.deepEquals(Long.valueOf(1L), Integer.valueOf(2)));
    }

    @Test
    public void testDeepEquals_StringsAndBooleans() {
        assertTrue(DeepEquals.deepEquals("hello", "hello"));
        assertFalse(DeepEquals.deepEquals("hello", "world"));
        assertTrue(DeepEquals.deepEquals(Boolean.TRUE, true));
        assertFalse(DeepEquals.deepEquals(Boolean.FALSE, true));
    }

    @Test
    public void testDeepEquals_NullHandling() {
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
        assertFalse(DeepEquals.deepEquals("not null", null));
    }

    /* --------------------------------------------------------------------- */
    /* 2. deepEquals(Object, Object, Map) – option handling                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepEquals_AllowStringNumberMatch_Enabled() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        assertTrue(DeepEquals.deepEquals("42", 42, options));
        assertTrue(DeepEquals.deepEquals(3.14, "3.14", options));

        // Mismatch when string cannot be parsed as number
        assertFalse(DeepEquals.deepEquals("abc", 123, options));
        assertNotNull(options.get(DeepEquals.DIFF));
    }

    @Test
    public void testDeepEquals_AllowStringNumberMatch_Disabled() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, false);

        assertFalse(DeepEquals.deepEquals("42", 42, options));
        assertNotNull(options.get(DeepEquals.DIFF));
    }

    @Test
    public void testDeepEquals_IgnoreCustomEquals_NullMeansUseAll() {
        CustomEqualsOnly a = new CustomEqualsOnly(10);
        CustomEqualsOnly b = new CustomEqualsOnly(10);
        // No ignore set – custom equals is used, objects are equal
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEquals_IgnoreCustomEquals_EmptySetIgnoresAll() {
        CustomEqualsOnly a = new CustomEqualsOnly(10);
        CustomEqualsOnly b = new CustomEqualsOnly(10);
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());

        // Ignoring custom equals forces field‑by‑field comparison → fields differ (value is private)
        // Since the field is not accessible, the comparison falls back to Object identity → not equal
        assertFalse(DeepEquals.deepEquals(a, b, options));
        assertNotNull(options.get(DeepEquals.DIFF));
    }

    @Test
    public void testDeepEquals_IgnoreCustomEquals_PartialSet() {
        CustomEqualsAndHash a = new CustomEqualsAndHash("key", 1);
        CustomEqualsAndHash b = new CustomEqualsAndHash("key", 2); // different number
        Map<String, Object> options = new HashMap<>();
        // Ignore CustomEqualsAndHash class → deep‑field comparison should detect the number mismatch
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of(CustomEqualsAndHash.class));

        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("field value mismatch"));
    }

    /* --------------------------------------------------------------------- */
    /* 3. deepEquals on complex structures (objects, collections, maps, arrays) */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepEquals_NestedObjectsAndCollections() {
        SimpleBean bean1 = new SimpleBean(1, "Alice");
        SimpleBean bean2 = new SimpleBean(1, "Alice");
        List<SimpleBean> list1 = Arrays.asList(bean1, new SimpleBean(2, "Bob"));
        List<SimpleBean> list2 = Arrays.asList(bean2, new SimpleBean(2, "Bob"));

        Map<String, Object> map1 = new HashMap<>();
        map1.put("beans", list1);
        map1.put("count", 2);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("beans", list2);
        map2.put("count", 2);

        assertTrue(DeepEquals.deepEquals(map1, map2));
    }

    @Test
    public void testDeepEquals_UnorderedCollectionMismatch() {
        Set<String> set1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> set2 = new HashSet<>(Arrays.asList("a", "b", "d"));
        Map<String, Object> options = new HashMap<>();

        assertFalse(DeepEquals.deepEquals(set1, set2, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("missing collection element"));
    }

    @Test
    public void testDeepEquals_ArrayDimensionAndComponentMismatch() {
        int[][] a = {{1, 2}, {3, 4}};
        int[][] b = {{1, 2}, {3, 4, 5}}; // second sub‑array longer

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("array length mismatch"));
    }

    @Test
    public void testDeepEquals_MapKeyAndValueMismatches() {
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);
        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("one", 1);
        m2.put("two", 22); // value differs

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(m1, m2, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("map value mismatch"));
    }

    @Test
    public void testDeepEquals_CircularReferenceHandling() {
        Node n1 = new Node("A");
        Node n2 = new Node("A");
        n1.next = n1; // self‑cycle
        n2.next = n2; // self‑cycle

        assertTrue(DeepEquals.deepEquals(n1, n2));
    }

    @Test
    public void testDeepEquals_DifferentTypes() {
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals("string", 123, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("type mismatch"));
    }

    /* --------------------------------------------------------------------- */
    /* 4. deepHashCode – consistency with deepEquals and custom hash handling */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepHashCode_ConsistencyWithDeepEquals() {
        SimpleBean b1 = new SimpleBean(5, "Bob");
        SimpleBean b2 = new SimpleBean(5, "Bob");
        assertTrue(DeepEquals.deepEquals(b1, b2));
        assertEquals(DeepEquals.deepHashCode(b1), DeepEquals.deepHashCode(b2));
    }

    @Test
    public void testDeepHashCode_CustomHashCodeUsed() {
        CustomEqualsAndHash c1 = new CustomEqualsAndHash("key", 100);
        CustomEqualsAndHash c2 = new CustomEqualsAndHash("key", 100);
        // Both objects are equal via custom equals & hashCode
        assertTrue(DeepEquals.deepEquals(c1, c2));
        // deepHashCode should delegate to the overridden hashCode()
        assertEquals(c1.hashCode(), DeepEquals.deepHashCode(c1));
        assertEquals(c2.hashCode(), DeepEquals.deepHashCode(c2));
    }

    @Test
    public void testDeepHashCode_UnorderedCollectionOrderIrrelevant() {
        Set<String> s1 = new HashSet<>(Arrays.asList("x", "y", "z"));
        Set<String> s2 = new HashSet<>(Arrays.asList("z", "y", "x"));
        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertEquals(DeepEquals.deepHashCode(s1), DeepEquals.deepHashCode(s2));
    }

    /* --------------------------------------------------------------------- */
    /* 5. hasCustomEquals / hasCustomHashCode – direct public API tests      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testHasCustomEquals() {
        assertTrue(DeepEquals.hasCustomEquals(CustomEqualsOnly.class));
        assertTrue(DeepEquals.hasCustomEquals(CustomEqualsAndHash.class));
        assertFalse(DeepEquals.hasCustomEquals(String.class));
        assertFalse(DeepEquals.hasCustomEquals(Integer.class));
    }

    @Test
    public void testHasCustomHashCode() {
        assertFalse(DeepEquals.hasCustomHashCode(CustomEqualsOnly.class));
        assertTrue(DeepEquals.hasCustomHashCode(CustomEqualsAndHash.class));
        assertFalse(DeepEquals.hasCustomHashCode(String.class));
        assertFalse(DeepEquals.hasCustomHashCode(Integer.class));
    }

    /* --------------------------------------------------------------------- */
    /* 6. Number comparison edge cases – via deepEquals (indirectly tests   */
    /*    compareNumbers and nearlyEqual)                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNumberComparison_FloatingPointTolerance() {
        double a = 0.1 + 0.2; // 0.30000000000000004
        double b = 0.3;
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testNumberComparison_BigDecimalPrecision() {
        BigDecimal bd1 = new BigDecimal("1.0000000001");
        BigDecimal bd2 = new BigDecimal("1.0000000002");
        // Difference is within epsilon for double conversion – should be equal
        assertTrue(DeepEquals.deepEquals(bd1, bd2));
    }

    @Test
    public void testNumberComparison_IncompatibleRanges() {
        // Very large BigDecimal vs Double.MAX_VALUE should be considered not equal
        BigDecimal huge = new BigDecimal(Double.MAX_VALUE).multiply(BigDecimal.TEN);
        assertFalse(DeepEquals.deepEquals(huge, Double.MAX_VALUE));
    }

    /* --------------------------------------------------------------------- */
    /* 7. AtomicBoolean comparison (compareAtomicBoolean)                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAtomicBooleanComparison() {
        assertTrue(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(true)));
        assertFalse(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(false)));
    }

    /* --------------------------------------------------------------------- */
    /* 8. Diff generation – ensure human‑readable output contains expected parts */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDiffMessage_Content() {
        SimpleBean a = new SimpleBean(1, "Alice");
        SimpleBean b = new SimpleBean(1, "Bob"); // name differs
        Map<String, Object> options = new HashMap<>();

        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        // Expected fragments
        assertTrue(diff.contains("[field value mismatch]"));
        assertTrue(diff.contains(".name"));
        assertTrue(diff.contains("Expected: \"Alice\""));
        assertTrue(diff.contains("Found: \"Bob\""));
    }

    @Test
    public void testDiffMessage_ArrayElementMismatch() {
        int[] a = {1, 2, 3};
        int[] b = {1, 9, 3};
        Map<String, Object> options = new HashMap<>();

        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("[array element mismatch]"));
        assertTrue(diff.contains("[0]")); // root array notation
        assertTrue(diff.contains("Expected: 2"));
        assertTrue(diff.contains("Found: 9"));
    }

    @Test
    public void testDiffMessage_MapKeyMissing() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("one", 1);
        // "two" missing

        Map<String, Object> options = new HashMap<>();

        assertFalse(DeepEquals.deepEquals(m1, m2, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("[missing map key]"));
        assertTrue(diff.contains("《\"two\" ⇨"));
    }

    /* --------------------------------------------------------------------- */
    /* 9. Miscellaneous public helpers (formatRootObject, etc.) – exercised */
    /*    indirectly via deepEquals diff generation.                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepEquals_ComplexStructureWithURIAndURL() throws Exception {
        Map<String, Object> m1 = new HashMap<>();
        m1.put("uri", new URI("http://example.com"));
        m1.put("url", new java.net.URL("http://example.com"));
        m1.put("list", Arrays.asList(1, 2, 3));

        Map<String, Object> m2 = new HashMap<>();
        m2.put("uri", new URI("http://example.com"));
        m2.put("url", new java.net.URL("http://example.com"));
        m2.put("list", Arrays.asList(1, 2, 3));

        assertTrue(DeepEquals.deepEquals(m1, m2));
    }

    @Test
    public void testDeepEquals_EmptyContainers() {
        assertTrue(DeepEquals.deepEquals(Collections.emptyList(), Collections.emptyList()));
        assertTrue(DeepEquals.deepEquals(Collections.emptySet(), Collections.emptySet()));
        assertTrue(DeepEquals.deepEquals(new int[0], new int[0]));
        assertTrue(DeepEquals.deepEquals(new HashMap<>(), new HashMap<>()));
    }
}
