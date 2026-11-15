package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactCILinkedMap}.
 *
 * The tests verify:
 * <ul>
 *   <li>Case‑insensitive key handling (put, get, containsKey, remove).</li>
 *   <li>Insertion order is preserved (LinkedHashMap semantics).</li>
 *   <li>Construction from another map copies entries correctly.</li>
 *   <li>Standard {@link Map} operations such as putAll, clear, equals, hashCode.</li>
 *   <li>Behaviour of the overridden configuration methods via the public API.</li>
 * </ul>
 *
 * The test class uses only the public constructors and methods exposed by
 * {@code CompactCILinkedMap} and its superclass {@code CompactMap}. No mocking or
 * subclassing of library types is performed.
 */
public class CompactCILinkedMapTest
{
    private CompactCILinkedMap<String, Integer> map;

    @Before
    public void setUp()
    {
        map = new CompactCILinkedMap<>();
    }

    /* -------------------------------------------------------------
     *  Basic case‑insensitive put / get / containsKey behaviour
     * ------------------------------------------------------------- */
    @Test
    public void testPutAndGetCaseInsensitive()
    {
        map.put("Foo", 1);
        // Different capitalisations must resolve to the same entry
        assertEquals(Integer.valueOf(1), map.get("foo"));
        assertEquals(Integer.valueOf(1), map.get("FOO"));
        assertTrue(map.containsKey("fOo"));
        assertEquals(1, map.size());
    }

    @Test
    public void testOverwriteWithDifferentCase()
    {
        map.put("Bar", 2);
        // Overwrite using a key that differs only by case
        map.put("bAr", 3);
        assertEquals(Integer.valueOf(3), map.get("BAR"));
        // Size must remain 1 because the key is considered the same
        assertEquals(1, map.size());
    }

    /* -------------------------------------------------------------
     *  Removal is also case‑insensitive
     * ------------------------------------------------------------- */
    @Test
    public void testRemoveCaseInsensitive()
    {
        map.put("Key", 5);
        assertTrue(map.containsKey("key"));
        Integer removed = map.remove("kEy");
        assertEquals(Integer.valueOf(5), removed);
        assertFalse(map.containsKey("KEY"));
        assertTrue(map.isEmpty());
    }

    /* -------------------------------------------------------------
     *  Insertion order must be preserved (LinkedHashMap semantics)
     * ------------------------------------------------------------- */
    @Test
    public void testInsertionOrderPreserved()
    {
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        // The iterator of keySet() must follow insertion order
        Iterator<String> keyIter = map.keySet().iterator();
        assertEquals("first", keyIter.next());
        assertEquals("second", keyIter.next());
        assertEquals("third", keyIter.next());
        assertFalse(keyIter.hasNext());

        // The values iterator must follow the same order
        Iterator<Integer> valIter = map.values().iterator();
        assertEquals(Integer.valueOf(1), valIter.next());
        assertEquals(Integer.valueOf(2), valIter.next());
        assertEquals(Integer.valueOf(3), valIter.next());
        assertFalse(valIter.hasNext());
    }

    /* -------------------------------------------------------------
     *  Construction from another map (copy‑constructor)
     * ------------------------------------------------------------- */
    @Test
    public void testConstructorFromMap()
    {
        // Use a LinkedHashMap to guarantee a known iteration order
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("Alpha", 10);
        source.put("Beta", 20);
        source.put("Gamma", 30);

        CompactCILinkedMap<String, Integer> copy = new CompactCILinkedMap<>(source);

        // The copy must contain the same entries (case‑insensitive)
        assertEquals(3, copy.size());
        assertEquals(Integer.valueOf(10), copy.get("alpha"));
        assertEquals(Integer.valueOf(20), copy.get("BETA"));
        assertEquals(Integer.valueOf(30), copy.get("gAmMa"));

        // Insertion order must be preserved
        Iterator<String> it = copy.keySet().iterator();
        assertEquals("Alpha", it.next());
        assertEquals("Beta", it.next());
        assertEquals("Gamma", it.next());
    }

    /* -------------------------------------------------------------
     *  putAll must respect case‑insensitivity and preserve order
     * ------------------------------------------------------------- */
    @Test
    public void testPutAll()
    {
        Map<String, Integer> other = new LinkedHashMap<>();
        other.put("One", 1);
        other.put("Two", 2);
        other.put("Three", 3);

        map.putAll(other);

        // All entries are present
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertEquals(Integer.valueOf(2), map.get("TWO"));
        assertEquals(Integer.valueOf(3), map.get("ThReE"));

        // Order is the same as the source map
        Iterator<String> it = map.keySet().iterator();
        assertEquals("One", it.next());
        assertEquals("Two", it.next());
        assertEquals("Three", it.next());
    }

    /* -------------------------------------------------------------
     *  Clear operation
     * ------------------------------------------------------------- */
    @Test
    public void testClear()
    {
        map.put("A", 100);
        map.put("B", 200);
        assertFalse(map.isEmpty());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get("a"));
    }

    /* -------------------------------------------------------------
     *  equals() and hashCode() contract
     * ------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCode()
    {
        CompactCILinkedMap<String, Integer> m1 = new CompactCILinkedMap<>();
        CompactCILinkedMap<String, Integer> m2 = new CompactCILinkedMap<>();

        m1.put("X", 5);
        m1.put("Y", 6);

        // Different capitalisation but same logical entries
        m2.put("x", 5);
        m2.put("y", 6);

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());

        // Changing a value makes the maps unequal
        m2.put("Y", 99);
        assertNotEquals(m1, m2);
    }

    /* -------------------------------------------------------------
     *  minus() and plus() – thin wrappers around Map operations
     * ------------------------------------------------------------- */
    @Test
    public void testMinusAndPlus()
    {
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);

        // minus should return a new map without the supplied key (case‑insensitive)
        Map<String, Integer> withoutB = map.minus("b");
        assertEquals(2, withoutB.size());
        assertFalse(withoutB.containsKey("B"));
        assertTrue(withoutB.containsKey("A"));
        assertTrue(withoutB.containsKey("C"));

        // plus should return a new map with the supplied entry added (or replaced)
        Map<String, Integer> plusD = map.plus(Collections.singletonMap("d", 4));
        assertEquals(4, plusD.size());
        assertEquals(Integer.valueOf(4), plusD.get("D"));
        // original map must stay unchanged
        assertEquals(3, map.size());
        assertTrue(map.containsKey("A"));
    }

    /* -------------------------------------------------------------
     *  Verify that the map reports itself as case‑insensitive via
     *  the protected hook (exposed through getConfig()).
     * ------------------------------------------------------------- */
    @Test
    public void testConfigReportsCaseInsensitive()
    {
        Map<String, Object> config = map.getConfig();
        assertTrue("Config must contain CASE_SENSITIVE flag set to false",
                Boolean.FALSE.equals(config.get(CompactMap.CASE_SENSITIVE)));
        assertTrue("Config must contain MAP_TYPE flag indicating a LinkedHashMap",
                LinkedHashMap.class.isAssignableFrom((Class<?>) config.get(CompactMap.MAP_TYPE)));
    }

    /* -------------------------------------------------------------
     *  Builder usage – ensure that the fluent builder creates a
     *  correctly configured instance.
     * ------------------------------------------------------------- */
    @Test
    public void testBuilderCreatesCaseInsensitiveLinkedMap()
    {
        CompactCILinkedMap<String, String> built = CompactCILinkedMap.<String, String>builder()
                .put("Hello", "World")
                .put("Foo", "Bar")
                .build();

        // Verify case‑insensitive get
        assertEquals("World", built.get("hello"));
        assertEquals("Bar", built.get("FOO"));
        // Verify insertion order
        Iterator<String> it = built.keySet().iterator();
        assertEquals("Hello", it.next());
        assertEquals("Foo", it.next());
    }
}
