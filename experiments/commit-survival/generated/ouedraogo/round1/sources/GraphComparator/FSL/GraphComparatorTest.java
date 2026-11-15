package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.*;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link GraphComparator}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Delta equality / hashCode</li>
 *   <li>Delta.Command enum lookup</li>
 *   <li>Array resize and element set</li>
 *   <li>List resize and element set</li>
 *   <li>Set add / remove</li>
 *   <li>Map put / remove</li>
 *   <li>Object field assignment</li>
 *   <li>Object field type‑change handling (error path)</li>
 *   <li>Missing field handling (error path)</li>
 *   <li>DeltaError string representation</li>
 * </ul>
 * <p>
 * All tests use only the public API that is present in the supplied source file.
 */
public class GraphComparatorTest {

    /* ---------------------------------------------------------------------- */
    /* Helper classes used throughout the test suite                           */
    /* ---------------------------------------------------------------------- */

    /** Simple bean that contains a variety of fields used by the processor. */
    public static class Bean {
        public Long id;                     // used as the logical identifier
        public int[] intArray;
        public String[] strArray;
        public List<String> nameList;
        public Set<String> tagSet;
        public Map<String, Integer> scoreMap;
        public String name;
        public Object misc;                // generic field for OBJECT_ASSIGN_FIELD tests

        public Bean(Long id) {
            this.id = id;
        }
    }

    /** ID implementation that simply returns the {@code id} field of the bean. */
    private static final GraphComparator.ID ID_FETCHER = new GraphComparator.ID() {
        @Override
        public Object getId(Object objectToId) {
            try {
                Field f = objectToId.getClass().getField("id");
                return f.get(objectToId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };

    /** Convenience method to obtain a {@link Field} and make it accessible. */
    private static Field getField(Class<?> clazz, String name) {
        try {
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 1. Delta equality / hashCode                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeltaEqualsAndHashCode() {
        GraphComparator.Delta d1 = new GraphComparator.Delta(1L, "field", "ptr", null, null, null);
        GraphComparator.Delta d2 = new GraphComparator.Delta(2L, "other", "ptr", null, null, null);
        GraphComparator.Delta d3 = new GraphComparator.Delta(3L, "field", "different", null, null, null);

        // same srcPtr → equal
        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());

        // different srcPtr → not equal
        assertNotEquals(d1, d3);
    }

    /* ---------------------------------------------------------------------- */
    /* 2. Delta.Command enum lookup                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCommandFromName() {
        assertEquals(GraphComparator.Delta.Command.ARRAY_SET_ELEMENT,
                GraphComparator.Delta.Command.fromName("array.setelement"));
        assertEquals(GraphComparator.Delta.Command.MAP_REMOVE,
                GraphComparator.Delta.Command.fromName("MAP.REMOVE"));

        try {
            GraphComparator.Delta.Command.fromName("unknown.command");
            fail("Expected IllegalArgumentException for unknown command");
        } catch (IllegalArgumentException ignored) { }
    }

    /* ---------------------------------------------------------------------- */
    /* 3. Array resize and element set                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testArrayResizeAndSetElement() throws Exception {
        Bean bean = new Bean(1L);
        bean.intArray = new int[] { 10, 20, 30 };

        GraphComparator.Delta resize = new GraphComparator.Delta(bean.id, "intArray", "root", bean.intArray, null, 5);
        resize.setCmd(GraphComparator.Delta.Command.ARRAY_RESIZE);

        GraphComparator.Delta setElem = new GraphComparator.Delta(bean.id, "intArray", "root[2]", bean.intArray, 99, 2);
        setElem.setCmd(GraphComparator.Delta.Command.ARRAY_SET_ELEMENT);

        List<GraphComparator.Delta> cmds = Arrays.asList(resize, setElem);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(bean, cmds, ID_FETCHER,
                GraphComparator.getJavaDeltaProcessor());

        assertTrue(errors.isEmpty());

        // verify resize
        assertEquals(5, bean.intArray.length);
        assertEquals(10, bean.intArray[0]);
        assertEquals(20, bean.intArray[1]);
        assertEquals(30, bean.intArray[2]); // will be overwritten by setElem
        // verify set element
        assertEquals(99, bean.intArray[2]);
        // new slots should be default 0
        assertEquals(0, bean.intArray[3]);
        assertEquals(0, bean.intArray[4]);
    }

    /* ---------------------------------------------------------------------- */
    /* 4. List resize and element set                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testListResizeAndSetElement() throws Exception {
        Bean bean = new Bean(2L);
        bean.nameList = new ArrayList<>(Arrays.asList("alpha", "beta"));

        GraphComparator.Delta resize = new GraphComparator.Delta(bean.id, "nameList", "root", bean.nameList, null, 4);
        resize.setCmd(GraphComparator.Delta.Command.LIST_RESIZE);

        GraphComparator.Delta setElem = new GraphComparator.Delta(bean.id, "nameList", "root{2}", bean.nameList, "gamma", 2);
        setElem.setCmd(GraphComparator.Delta.Command.LIST_SET_ELEMENT);

        List<GraphComparator.Delta> cmds = Arrays.asList(resize, setElem);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(bean, cmds, ID_FETCHER,
                GraphComparator.getJavaDeltaProcessor());

        assertTrue(errors.isEmpty());

        // verify resize
        assertEquals(4, bean.nameList.size());
        assertEquals("alpha", bean.nameList.get(0));
        assertEquals("beta", bean.nameList.get(1));
        assertNull(bean.nameList.get(2)); // will be set by setElem
        assertNull(bean.nameList.get(3));

        // verify set element
        assertEquals("gamma", bean.nameList.get(2));
    }

    /* ---------------------------------------------------------------------- */
    /* 5. Set add / remove                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSetAddAndRemove() throws Exception {
        Bean bean = new Bean(3L);
        bean.tagSet = new HashSet<>(Arrays.asList("red"));

        GraphComparator.Delta add = new GraphComparator.Delta(bean.id, "tagSet", "root.add(OBJ)", null, "blue", null);
        add.setCmd(GraphComparator.Delta.Command.SET_ADD);

        GraphComparator.Delta remove = new GraphComparator.Delta(bean.id, "tagSet", "root.remove(OBJ)", "red", null, null);
        remove.setCmd(GraphComparator.Delta.Command.SET_REMOVE);

        List<GraphComparator.Delta> cmds = Arrays.asList(add, remove);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(bean, cmds, ID_FETCHER,
                GraphComparator.getJavaDeltaProcessor());

        assertTrue(errors.isEmpty());

        assertTrue(bean.tagSet.contains("blue"));
        assertFalse(bean.tagSet.contains("red"));
        assertEquals(1, bean.tagSet.size());
    }

    /* ---------------------------------------------------------------------- */
    /* 6. Map put / remove                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMapPutAndRemove() throws Exception {
        Bean bean = new Bean(4L);
        bean.scoreMap = new HashMap<>();
        bean.scoreMap.put("one", 1);

        // change existing key
        GraphComparator.Delta putChange = new GraphComparator.Delta(bean.id, "scoreMap",
                "root['one']", 1, 42, "one");
        putChange.setCmd(GraphComparator.Delta.Command.MAP_PUT);

        // remove key
        GraphComparator.Delta remove = new GraphComparator.Delta(bean.id, "scoreMap",
                "root['one']", 42, null, "one");
        remove.setCmd(GraphComparator.Delta.Command.MAP_REMOVE);

        List<GraphComparator.Delta> cmds = Arrays.asList(putChange, remove);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(bean, cmds, ID_FETCHER,
                GraphComparator.getJavaDeltaProcessor());

        assertTrue(errors.isEmpty());

        // after putChange, value should be 42
        assertEquals(Integer.valueOf(42), bean.scoreMap.get("one"));
        // after remove, key should be gone
        assertFalse(bean.scoreMap.containsKey("one"));
    }

    /* ---------------------------------------------------------------------- */
    /* 7. Object field assignment                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testObjectAssignField() throws Exception {
        Bean bean = new Bean(5L);
        bean.name = "oldName";

        GraphComparator.Delta assign = new GraphComparator.Delta(bean.id, "name", "root", bean.name, "newName", null);
        assign.setCmd(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(bean,
                Collections.singletonList(assign), ID_FETCHER,
                GraphComparator.getJavaDeltaProcessor());

        assertTrue(errors.isEmpty());
        assertEquals("newName", bean.name);
    }

    /* ---------------------------------------------------------------------- */
    /* 8. OBJECT_FIELD_TYPE_CHANGED – processor throws RuntimeException       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testObjectFieldTypeChangedError() throws Exception {
        Bean bean = new Bean(6L);
        bean.misc = "string";

        GraphComparator.Delta typeChange = new GraphComparator.Delta(bean.id, "misc", "root", bean.misc, 123, null);
        typeChange.setCmd(GraphComparator.Delta.Command.OBJECT_FIELD_TYPE_CHANGED);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(bean,
                Collections.singletonList(typeChange), ID_FETCHER,
                GraphComparator.getJavaDeltaProcessor());

        assertEquals(1, errors.size());
        GraphComparator.DeltaError err = errors.get(0);
        assertTrue(err.getError().contains("failed"));
        assertEquals(typeChange, err.getError().contains(typeChange.cmd.toString()) ? err.getDelta() : null);
    }

    /* ---------------------------------------------------------------------- */
    /* 9. Missing field – should produce a DeltaError                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMissingFieldError() throws Exception {
        Bean bean = new Bean(7L);
        bean.name = "something";

        GraphComparator.Delta missing = new GraphComparator.Delta(bean.id, "nonexistent", "root", null, "value", null);
        missing.setCmd(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(bean,
                Collections.singletonList(missing), ID_FETCHER,
                GraphComparator.getJavaDeltaProcessor());

        assertEquals(1, errors.size());
        GraphComparator.DeltaError err = errors.get(0);
        assertTrue(err.getError().contains("field name missing"));
        assertEquals(missing, err.getDelta());
    }

    /* ---------------------------------------------------------------------- */
    /* 10. DeltaError toString includes error and wrapped Delta info         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeltaErrorToString() {
        GraphComparator.Delta base = new GraphComparator.Delta(8L, "field", "ptr", "src", "tgt", null);
        base.setCmd(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);
        GraphComparator.DeltaError err = new GraphComparator.DeltaError("boom", base);

        String s = err.toString();
        assertTrue(s.contains("boom"));
        assertTrue(s.contains("Delta {"));
        assertTrue(s.contains("fieldName='field'"));
    }

    /* ---------------------------------------------------------------------- */
    /* 11. Verify that isLogicalPrimitive logic is exercised via compare()     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComparePrimitives() {
        // Primitive wrapper comparison – should yield OBJECT_ASSIGN_FIELD delta when values differ
        Integer src = 5;
        Integer tgt = 9;
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        assertEquals(1, deltas.size());
        assertEquals(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, deltas.get(0).getCmd());

        // Same value – no delta
        deltas = GraphComparator.compare(42, 42, ID_FETCHER);
        assertTrue(deltas.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 12. Verify that compare() correctly handles array element differences   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComparePrimitiveArrayDifferences() {
        int[] src = {1, 2, 3};
        int[] tgt = {1, 9, 3, 4};

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Expected: one ARRAY_RESIZE (to length 4) and one ARRAY_SET_ELEMENT (index 1)
        boolean hasResize = false, hasSet = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == GraphComparator.Delta.Command.ARRAY_RESIZE) {
                hasResize = true;
                assertEquals(4, d.getOptionalKey());
            } else if (d.getCmd() == GraphComparator.Delta.Command.ARRAY_SET_ELEMENT) {
                hasSet = true;
                assertEquals(1, d.getOptionalKey()); // index
                assertEquals(2, d.getSourceValue());
                assertEquals(9, d.getTargetValue());
            }
        }
        assertTrue(hasResize);
        assertTrue(hasSet);
    }

    /* ---------------------------------------------------------------------- */
    /* 13. Verify that compare() correctly handles Set add / remove           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCompareSetAddRemove() {
        Set<String> src = new HashSet<>(Arrays.asList("a", "b"));
        Set<String> tgt = new HashSet<>(Arrays.asList("b", "c"));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Expect one SET_REMOVE for "a" and one SET_ADD for "c"
        boolean removedA = false, addedC = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == GraphComparator.Delta.Command.SET_REMOVE) {
                removedA = "a".equals(d.getSourceValue());
            } else if (d.getCmd() == GraphComparator.Delta.Command.SET_ADD) {
                addedC = "c".equals(d.getTargetValue());
            }
        }
        assertTrue(removedA);
        assertTrue(addedC);
    }

    /* ---------------------------------------------------------------------- */
    /* 14. Verify that compare() correctly handles Map put / remove           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCompareMapPutRemove() {
        Map<String, Integer> src = new HashMap<>();
        src.put("x", 1);
        src.put("y", 2);
        Map<String, Integer> tgt = new HashMap<>();
        tgt.put("y", 2);          // unchanged
        tgt.put("z", 3);          // new entry
        // "x" removed

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        boolean removedX = false, putZ = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == GraphComparator.Delta.Command.MAP_REMOVE) {
                removedX = "x".equals(d.getOptionalKey());
            } else if (d.getCmd() == GraphComparator.Delta.Command.MAP_PUT) {
                putZ = "z".equals(d.getOptionalKey()) && Integer.valueOf(3).equals(d.getTargetValue());
            }
        }
        assertTrue(removedX);
        assertTrue(putZ);
    }

    /* ---------------------------------------------------------------------- */
    /* 15. Verify that compare() correctly handles List resize and set element */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCompareListResizeAndSetElement() {
        List<String> src = new ArrayList<>(Arrays.asList("a", "b"));
        List<String> tgt = new ArrayList<>(Arrays.asList("a", "c", "d"));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        boolean hasResize = false, hasSet = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == GraphComparator.Delta.Command.LIST_RESIZE) {
                hasResize = true;
                assertEquals(3, d.getOptionalKey());
            } else if (d.getCmd() == GraphComparator.Delta.Command.LIST_SET_ELEMENT) {
                hasSet = true;
                assertEquals(1, d.getOptionalKey()); // index 1 changed from "b" → "c"
                assertEquals("b", d.getSourceValue());
                assertEquals("c", d.getTargetValue());
            }
        }
        assertTrue(hasResize);
        assertTrue(hasSet);
    }
}
