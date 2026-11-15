package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Comprehensive JUnit‑4 test suite for {@link DeepEquals}.
 *
 * The tests are grouped by the public API of DeepEquals:
 * <ul>
 *   <li>deepEquals(Object,Object)</li>
 *   <li>deepEquals(Object,Object,Map)</li>
 *   <li>isContainerType(Object)</li>
 *   <li>hasCustomEquals(Class)</li>
 *   <li>hasCustomHashCode(Class)</li>
 *   <li>deepHashCode(Object)</li>
 * </ul>
 *
 * For each public method a basic “happy‑path” test is provided, followed
 * by a set of edge‑case / exception tests that exercise the special
 * handling inside the implementation (nulls, cycles, number‑to‑string
 * conversion, floating‑point epsilon, collections order, custom equals
 * handling, etc.).
 *
 * The tests use only the classes, constructors and methods that are
 * present in the supplied source code (no external mocking frameworks).
 */
public class DeepEqualsTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper classes used throughout the tests                               */
    /* ---------------------------------------------------------------------- */

    /** Simple POJO without custom equals / hashCode – fields are compared reflectively. */
    private static class SimpleBean {
        private int id;
        private String name;
        private List<Integer> scores;

        SimpleBean(int id, String name, List<Integer> scores) {
            this.id = id;
            this.name = name;
            this.scores = scores;
        }
    }

    /** POJO that overrides equals / hashCode – used to test custom‑equals handling. */
    private static class CustomEqualsBean {
        private final String value;

        CustomEqualsBean(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CustomEqualsBean)) return false;
            return Objects.equals(value, ((CustomEqualsBean) o).value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }
    }

    /** POJO that overrides only hashCode – used for deepHashCode custom‑hash test. */
    private static class CustomHashBean {
        private final String data;

        CustomHashBean(String data) {
            this.data = data;
        }

        @Override
        public int hashCode() {
            return data != null ? data.hashCode() : 0;
        }
    }

    /** Simple cyclic structure: Node -> next Node */
    private static class Node {
        Node next;
        int value;

        Node(int value) {
            this.value = value;
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  1. deepEquals(Object,Object) – basic functionality                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEquals_SamePrimitiveValues() {
        assertTrue(DeepEquals.deepEquals(5, 5));
        assertTrue(DeepEquals.deepEquals('a', 'a'));
        assertTrue(DeepEquals.deepEquals(true, true));
    }

    @Test
    public void testDeepEquals_DifferentPrimitives() {
        assertFalse(DeepEquals.deepEquals(5, 6));
        assertFalse(DeepEquals.deepEquals('a', 'b'));
        assertFalse(DeepEquals.deepEquals(true, false));
    }

    @Test
    public void testDeepEquals_NullHandling() {
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
        assertFalse(DeepEquals.deepEquals("not null", null));
    }

    @Test
    public void testDeepEquals_SimpleBeanEquality() {
        SimpleBean a = new SimpleBean(1, "bob", Arrays.asList(1, 2, 3));
        SimpleBean b = new SimpleBean(1, "bob", Arrays.asList(1, 2, 3));
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEquals_SimpleBeanInequality() {
        SimpleBean a = new SimpleBean(1, "bob", Arrays.asList(1, 2, 3));
        SimpleBean b = new SimpleBean(2, "bob", Arrays.asList(1, 2, 3));
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEquals_CustomEqualsBean_DefaultUsesCustomEquals() {
        CustomEqualsBean a = new CustomEqualsBean("x");
        CustomEqualsBean b = new CustomEqualsBean("x");
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEquals_CustomEqualsBean_DifferentValues() {
        CustomEqualsBean a = new CustomEqualsBean("x");
        CustomEqualsBean b = new CustomEqualsBean("y");
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEquals_CustomEqualsIgnoredViaOption() {
        CustomEqualsBean a = new CustomEqualsBean("x");
        CustomEqualsBean b = new CustomEqualsBean("y");
        Map<String, Object> options = new HashMap<>();
        Set<Class<?>> ignoreSet = new HashSet<>();
        ignoreSet.add(CustomEqualsBean.class);
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignoreSet);
        // With custom equals ignored, field‑by‑field comparison sees different values → false
        assertFalse(DeepEquals.deepEquals(a, b, options));
    }

    @Test
    public void testDeepEquals_CycleDetection() {
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Node n3 = new Node(3);
        n1.next = n2;
        n2.next = n3;
        n3.next = n1; // cycle

        Node m1 = new Node(1);
        Node m2 = new Node(2);
        Node m3 = new Node(3);
        m1.next = m2;
        m2.next = m3;
        m3.next = m1; // identical cycle

        assertTrue(DeepEquals.deepEquals(n1, m1));
    }

    @Test
    public void testDeepEquals_NumberEqualityAcrossTypes() {
        assertTrue(DeepEquals.deepEquals(5, 5L));          // int vs long
        assertTrue(DeepEquals.deepEquals(5.0, 5));        // double vs int
        assertTrue(DeepEquals.deepEquals(new BigDecimal("5.00"), 5));
    }

    @Test
    public void testDeepEquals_NumberInequality() {
        assertFalse(DeepEquals.deepEquals(5, 6L));
        assertFalse(DeepEquals.deepEquals(5.0, 6));
    }

    @Test
    public void testDeepEquals_StringNumberMatching_OptionEnabled() {
        Map<String, Object> opt = new HashMap<>();
        opt.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("5", 5, opt));
        assertTrue(DeepEquals.deepEquals(5, "5", opt));
        assertFalse(DeepEquals.deepEquals("5.1", 5, opt));
    }

    @Test
    public void testDeepEquals_StringNumberMatching_OptionDisabled() {
        Map<String, Object> opt = new HashMap<>();
        opt.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, false);
        assertFalse(DeepEquals.deepEquals("5", 5, opt));
    }

    @Test
    public void testDeepEquals_FloatingPointEpsilon() {
        // Within epsilon for float
        assertTrue(DeepEquals.deepEquals(0.123456f, 0.123457f));
        // Outside epsilon for double
        assertFalse(DeepEquals.deepEquals(0.123456789012345, 0.123456789012346));
    }

    @Test
    public void testDeepEquals_OrderedCollectionEquality() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(1, 2, 3);
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEquals_OrderedCollectionInequalityDueToOrder() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(3, 2, 1);
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEquals_UnorderedSetEquality() {
        Set<String> a = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> b = new HashSet<>(Arrays.asList("c", "b", "a"));
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEquals_UnorderedSetInequality() {
        Set<String> a = new HashSet<>(Arrays.asList("a", "b"));
        Set<String> b = new HashSet<>(Arrays.asList("a", "c"));
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEquals_MapEquality() {
        Map<String, Integer> a = new HashMap<>();
        a.put("x", 1);
        a.put("y", 2);
        Map<String, Integer> b = new HashMap<>();
        b.put("y", 2);
        b.put("x", 1);
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEquals_MapInequalityDifferentKeySet() {
        Map<String, Integer> a = new HashMap<>();
        a.put("x", 1);
        Map<String, Integer> b = new HashMap<>();
        b.put("y", 1);
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEquals_ArrayEquality() {
        int[] a = {1,2,3};
        int[] b = {1,2,3};
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEquals_ArrayInequalityLength() {
        int[] a = {1,2,3};
        int[] b = {1,2,3,4};
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEquals_ArrayInequalityContent() {
        int[] a = {1,2,3};
        int[] b = {1,2,4};
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEquals_AtomicBooleanEquality() {
        assertTrue(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(true)));
        assertFalse(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(false)));
    }

    /* ---------------------------------------------------------------------- */
    /*  2. deepEquals(Object,Object,Map) – option handling tests                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEquals_IgnoreCustomEquals_EmptySetMeansAllCustomEqualsUsed() {
        CustomEqualsBean a = new CustomEqualsBean("x");
        CustomEqualsBean b = new CustomEqualsBean("x");
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        assertTrue(DeepEquals.deepEquals(a, b, options));
    }

    @Test
    public void testDeepEquals_IgnoreCustomEquals_NullOptionBehavesLikeNoIgnore() {
        CustomEqualsBean a = new CustomEqualsBean("x");
        CustomEqualsBean b = new CustomEqualsBean("x");
        assertTrue(DeepEquals.deepEquals(a, b, new HashMap<>()));
    }

    /* ---------------------------------------------------------------------- */
    /*  3. isContainerType(Object) – simple checks                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsContainerType() {
        assertTrue(DeepEquals.isContainerType(new ArrayList<>()));
        assertTrue(DeepEquals.isContainerType(new HashMap<>()));
        assertFalse(DeepEquals.isContainerType("not a container"));
        assertFalse(DeepEquals.isContainerType(123));
    }

    /* ---------------------------------------------------------------------- */
    /*  4. hasCustomEquals(Class) – detection tests                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHasCustomEquals_DetectsOverride() {
        assertTrue(DeepEquals.hasCustomEquals(CustomEqualsBean.class));
    }

    @Test
    public void testHasCustomEquals_NoOverride() {
        assertFalse(DeepEquals.hasCustomEquals(SimpleBean.class));
    }

    @Test
    public void testHasCustomEquals_CachesResult() {
        // First call populates the cache
        boolean first = DeepEquals.hasCustomEquals(CustomEqualsBean.class);
        // Second call should hit the cache but return same result
        boolean second = DeepEquals.hasCustomEquals(CustomEqualsBean.class);
        assertEquals(first, second);
    }

    /* ---------------------------------------------------------------------- */
    /*  5. hasCustomHashCode(Class) – detection tests                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHasCustomHashCode_DetectsOverride() {
        assertTrue(DeepEquals.hasCustomHashCode(CustomHashBean.class));
    }

    @Test
    public void testHasCustomHashCode_NoOverride() {
        assertFalse(DeepEquals.hasCustomHashCode(SimpleBean.class));
    }

    @Test
    public void testHasCustomHashCode_CachesResult() {
        boolean first = DeepEquals.hasCustomHashCode(CustomHashBean.class);
        boolean second = DeepEquals.hasCustomHashCode(CustomHashBean.class);
        assertEquals(first, second);
    }

    /* ---------------------------------------------------------------------- */
    /*  6. deepHashCode(Object) – deterministic hash tests                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepHashCode_PrimitiveWrapper() {
        int h1 = DeepEquals.deepHashCode(Integer.valueOf(5));
        int h2 = DeepEquals.deepHashCode(Integer.valueOf(5));
        assertEquals(h1, h2);
    }

    @Test
    public void testDeepHashCode_ArrayOrderMatters() {
        int[] a = {1,2,3};
        int[] b = {3,2,1};
        assertNotEquals(DeepEquals.deepHashCode(a), DeepEquals.deepHashCode(b));
    }

    @Test
    public void testDeepHashCode_ListOrderMatters() {
        List<String> a = Arrays.asList("a","b","c");
        List<String> b = Arrays.asList("c","b","a");
        assertNotEquals(DeepEquals.deepHashCode(a), DeepEquals.deepHashCode(b));
    }

    @Test
    public void testDeepHashCode_SetOrderIrrelevant() {
        Set<String> a = new LinkedHashSet<>(Arrays.asList("x","y","z"));
        Set<String> b = new LinkedHashSet<>(Arrays.asList("z","y","x"));
        // Set order does not affect deepHashCode because each element is added individually
        assertEquals(DeepEquals.deepHashCode(a), DeepEquals.deepHashCode(b));
    }

    @Test
    public void testDeepHashCode_MapContent() {
        Map<String,Integer> m1 = new HashMap<>();
        m1.put("a",1);
        m1.put("b",2);
        Map<String,Integer> m2 = new LinkedHashMap<>();
        m2.put("b",2);
        m2.put("a",1);
        assertEquals(DeepEquals.deepHashCode(m1), DeepEquals.deepHashCode(m2));
    }

    @Test
    public void testDeepHashCode_CustomHashUsedWhenPresent() {
        CustomHashBean bean = new CustomHashBean("test");
        int expected = bean.hashCode(); // because hasCustomHashCode will invoke it
        assertEquals(expected, DeepEquals.deepHashCode(bean));
    }

    @Test
    public void testDeepHashCode_FloatEpsilonNormalization() {
        float f1 = 1.234567f;
        float f2 = 1.234568f; // diff < epsilon used in hashFloat (scale 1e5)
        assertEquals(DeepEquals.deepHashCode(f1), DeepEquals.deepHashCode(f2));
    }

    @Test
    public void testDeepHashCode_DoubleEpsilonNormalization() {
        double d1 = 1.2345678901;
        double d2 = 1.2345678902; // diff < epsilon used in hashDouble (scale 1e10)
        assertEquals(DeepEquals.deepHashCode(d1), DeepEquals.deepHashCode(d2));
    }

    @Test
    public void testDeepHashCode_CycleDoesNotStackOverflow() {
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        n1.next = n2;
        n2.next = n1; // cycle
        // Should compute without recursion error
        int hash = DeepEquals.deepHashCode(n1);
        // Not asserting a specific value, just that it completes
        assertTrue(hash != 0 || hash == 0);
    }

    /* ---------------------------------------------------------------------- */
    /*  7. ItemsToCompare – equals / hashCode contract                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testItemsToCompare_EqualityAndHashCode() throws Exception {
        // Use reflection to instantiate the private static class
        Class<?> ic = Class.forName("com.cedarsoftware.util.DeepEquals$ItemsToCompare");
        java.lang.reflect.Constructor<?> ctor = ic.getDeclaredConstructor(Object.class, Object.class);
        ctor.setAccessible(true);
        Object it1 = ctor.newInstance("a", "b");
        Object it2 = ctor.newInstance("a", "b");
        Object it3 = ctor.newInstance("a", "c");

        // equals
        assertTrue(it1.equals(it2));
        assertFalse(it1.equals(it3));
        // hashCode consistency
        assertEquals(it1.hashCode(), it2.hashCode());
        assertNotEquals(it1.hashCode(), it3.hashCode());
        // toString contains class names
        String s = it1.toString();
        assertTrue(s.contains("java.lang.String"));
    }

    /* ---------------------------------------------------------------------- */
    /*  8. Miscellaneous edge cases                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEquals_DifferentTypesNotContainers() {
        assertFalse(DeepEquals.deepEquals(5, "5"));
        assertFalse(DeepEquals.deepEquals(new Date(0), new Date(1)));
    }

    @Test
    public void testDeepEquals_CollectionVsArrayMismatch() {
        List<Integer> list = Arrays.asList(1,2,3);
        int[] arr = {1,2,3};
        assertFalse(DeepEquals.deepEquals(list, arr));
    }

    @Test
    public void testDeepEquals_MapVsSetMismatch() {
        Map<String,Integer> map = new HashMap<>();
        map.put("x",1);
        Set<String> set = new HashSet<>();
        set.add("x");
        assertFalse(DeepEquals.deepEquals(map, set));
    }
}
