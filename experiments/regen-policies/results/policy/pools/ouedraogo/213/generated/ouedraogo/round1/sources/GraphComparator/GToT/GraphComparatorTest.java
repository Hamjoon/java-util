package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link GraphComparator}.
 *
 * The test class follows the workflow described in the prompt:
 * <ol>
 *   <li>List all public methods (including inner‑class public APIs).</li>
 *   <li>Provide a basic test for each method.</li>
 *   <li>Identify edge‑cases / exception scenarios.</li>
 *   <li>Write dedicated tests for those scenarios.</li>
 *   <li>Combine everything into a single test file.</li>
 * </ol>
 *
 * All tests use only the public API that is present in the supplied source
 * file – no external mocking frameworks are employed.
 */
public class GraphComparatorTest {

    /** Simple POJO used by several tests. */
    public static class Bean {
        public int[] intArray;
        public List<Integer> intList;
        public Set<String> stringSet;
        public Map<String, Integer> stringMap;
        public String name;

        public Bean() { }

        public Bean(int[] intArray) {
            this.intArray = intArray;
        }

        public Bean(List<Integer> intList) {
            this.intList = intList;
        }

        public Bean(Set<String> stringSet) {
            this.stringSet = stringSet;
        }

        public Bean(Map<String, Integer> stringMap) {
            this.stringMap = stringMap;
        }

        public Bean(String name) {
            this.name = name;
        }
    }

    /** ID fetcher that simply returns the object itself as the identifier. */
    private final GraphComparator.ID identityIdFetcher = new GraphComparator.ID() {
        @Override
        public Object getId(Object objectToId) {
            return objectToId;
        }
    };

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* --------------------------------------------------------------------- */
    /* 1.  Public API of GraphComparator (extracted for reference)           */
    /* --------------------------------------------------------------------- */
    //   static List<Delta> compare(Object source, Object target, ID idFetcher)
    //   static List<DeltaError> applyDelta(Object source,
    //                                      List<Delta> commands,
    //                                      ID idFetcher,
    //                                      DeltaProcessor deltaProcessor,
    //                                      boolean... failFast)
    //   static DeltaProcessor getJavaDeltaProcessor()
    //
    //   public interface ID { Object getId(Object objectToId); }
    //
    //   public static class Delta implements Serializable {
    //       public Delta(Object id, String fieldName, String srcPtr,
    //                    Object srcValue, Object targetValue, Object optKey);
    //       // getters / setters / equals / hashCode / toString
    //   }
    //   public static class DeltaError extends Delta { ... }
    //   public enum Command { ...; static Command fromName(String); String getName(); }
    //   public interface DeltaProcessor { ... }
    //   private static class JavaDeltaProcessor implements DeltaProcessor { ... }

    /* --------------------------------------------------------------------- */
    /* 2.  Basic sanity tests for each public method                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetJavaDeltaProcessor_NotNull() {
        GraphComparator.DeltaProcessor processor = GraphComparator.getJavaDeltaProcessor();
        assertNotNull("Processor should not be null", processor);
    }

    @Test
    public void testDeltaEqualsAndHashCode() {
        GraphComparator.Delta d1 = new GraphComparator.Delta(1L, "field", "ptr", "src", "tgt", null);
        GraphComparator.Delta d2 = new GraphComparator.Delta(2L, "other", "ptr", "x", "y", null);
        GraphComparator.Delta d3 = new GraphComparator.Delta(3L, "field", "differentPtr", "a", "b", null);

        assertTrue("Deltas with same srcPtr must be equal", d1.equals(d2));
        assertEquals("HashCode must be based on srcPtr", d1.hashCode(), d2.hashCode());

        assertFalse("Different srcPtr must not be equal", d1.equals(d3));
    }

    @Test
    public void testDeltaToStringContainsAllFields() {
        GraphComparator.Delta d = new GraphComparator.Delta(99L, "myField", "myPtr", "srcVal", "tgtVal", "opt");
        d.setCmd(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);
        String s = d.toString();
        assertTrue(s.contains("99"));
        assertTrue(s.contains("myField"));
        assertTrue(s.contains("myPtr"));
        assertTrue(s.contains("srcVal"));
        assertTrue(s.contains("tgtVal"));
        assertTrue(s.contains("opt"));
        assertTrue(s.contains("OBJECT_ASSIGN_FIELD"));
    }

    @Test
    public void testCommandFromName_Valid() {
        GraphComparator.Delta.Command cmd = GraphComparator.Delta.Command.fromName("array.setelement");
        assertEquals(GraphComparator.Delta.Command.ARRAY_SET_ELEMENT, cmd);
    }

    @Test
    public void testCommandFromName_Invalid_Null() {
        thrown.expect(IllegalArgumentException.class);
        GraphComparator.Delta.Command.fromName(null);
    }

    @Test
    public void testCommandFromName_Invalid_Empty() {
        thrown.expect(IllegalArgumentException.class);
        GraphComparator.Delta.Command.fromName("   ");
    }

    @Test
    public void testCommandFromName_Unknown() {
        thrown.expect(IllegalArgumentException.class);
        GraphComparator.Delta.Command.fromName("unknown.command");
    }

    @Test
    public void testDeltaErrorBehaviour() {
        GraphComparator.Delta base = new GraphComparator.Delta(1L, "f", "p", "src", "tgt", null);
        GraphComparator.DeltaError err = new GraphComparator.DeltaError("boom", base);
        assertEquals("boom", err.getError());
        assertTrue(err.toString().contains("boom"));
        assertTrue(err.toString().contains(base.toString()));
    }

    /* --------------------------------------------------------------------- */
    /* 3.  Edge‑case / exception scenarios                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCompare_NullSourceOrTarget() {
        Bean target = new Bean("target");
        List<GraphComparator.Delta> deltas = GraphComparator.compare(null, target, identityIdFetcher);
        assertEquals(1, deltas.size());
        assertEquals(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, deltas.get(0).getCmd());

        deltas = GraphComparator.compare(target, null, identityIdFetcher);
        assertEquals(1, deltas.size());
        assertEquals(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, deltas.get(0).getCmd());
    }

    @Test
    public void testCompare_LogicalPrimitiveChange() {
        Integer src = 5;
        Integer tgt = 6;
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, identityIdFetcher);
        assertEquals(1, deltas.size());
        assertEquals(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, deltas.get(0).getCmd());
        assertEquals(src, deltas.get(0).getSourceValue());
        assertEquals(tgt, deltas.get(0).getTargetValue());
    }

    @Test
    public void testCompare_ArrayResizeAndSet() {
        int[] src = new int[]{1, 2};
        int[] tgt = new int[]{1, 2, 3};

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, identityIdFetcher);
        // Expect a resize + a set‑element for index 2
        boolean hasResize = false, hasSet = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == GraphComparator.Delta.Command.ARRAY_RESIZE) {
                hasResize = true;
                assertEquals(3, d.getOptionalKey());
            } else if (d.getCmd() == GraphComparator.Delta.Command.ARRAY_SET_ELEMENT) {
                hasSet = true;
                assertEquals(2, d.getOptionalKey());
                assertEquals(3, d.getTargetValue());
            }
        }
        assertTrue("Resize delta missing", hasResize);
        assertTrue("Set‑element delta missing", hasSet);
    }

    @Test
    public void testCompare_SetAddAndRemove() {
        Set<String> src = new HashSet<>(Arrays.asList("a"));
        Set<String> tgt = new HashSet<>(Arrays.asList("a", "b"));
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, identityIdFetcher);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(GraphComparator.Delta.Command.SET_ADD, d.getCmd());
        assertEquals("b", d.getTargetValue());
    }

    @Test
    public void testCompare_MapPutAndRemove() {
        Map<String, Integer> src = new HashMap<>();
        src.put("k1", 1);
        Map<String, Integer> tgt = new HashMap<>();
        tgt.put("k1", 2); // value change
        tgt.put("k2", 3); // new entry

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, identityIdFetcher);
        // Expect two MAP_PUT deltas (k1 changed, k2 added)
        Set<GraphComparator.Delta.Command> cmds = new HashSet<>();
        for (GraphComparator.Delta d : deltas) {
            cmds.add(d.getCmd());
            if (d.getOptionalKey().equals("k1")) {
                assertEquals(2, d.getTargetValue());
            } else if (d.getOptionalKey().equals("k2")) {
                assertEquals(3, d.getTargetValue());
            }
        }
        assertTrue(cmds.contains(GraphComparator.Delta.Command.MAP_PUT));
        assertEquals(2, deltas.size());
    }

    @Test
    public void testApplyDelta_ObjectAssignField() {
        Bean bean = new Bean("old");
        GraphComparator.Delta delta = new GraphComparator.Delta(bean, "name", "ptr", "old", "new", null);
        delta.setCmd(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(
                bean,
                Collections.singletonList(delta),
                identityIdFetcher,
                GraphComparator.getJavaDeltaProcessor()
        );

        assertTrue(errors.isEmpty());
        assertEquals("new", bean.name);
    }

    @Test
    public void testApplyDelta_ArraySetElement() throws NoSuchFieldException {
        Bean bean = new Bean(new int[]{10, 20});
        GraphComparator.Delta delta = new GraphComparator.Delta(bean, "intArray", "ptr[1]", 20, 99, 1);
        delta.setCmd(GraphComparator.Delta.Command.ARRAY_SET_ELEMENT);

        Field field = Bean.class.getField("intArray");
        // Verify the field is indeed an array
        assertTrue(field.getType().isArray());

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(
                bean,
                Collections.singletonList(delta),
                identityIdFetcher,
                GraphComparator.getJavaDeltaProcessor()
        );

        assertTrue(errors.isEmpty());
        assertArrayEquals(new int[]{10, 99}, bean.intArray);
    }

    @Test
    public void testApplyDelta_ArrayResize() throws NoSuchFieldException {
        Bean bean = new Bean(new int[]{1, 2, 3});
        GraphComparator.Delta delta = new GraphComparator.Delta(bean, "intArray", "ptr", bean.intArray, null, 5);
        delta.setCmd(GraphComparator.Delta.Command.ARRAY_RESIZE);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(
                bean,
                Collections.singletonList(delta),
                identityIdFetcher,
                GraphComparator.getJavaDeltaProcessor()
        );

        assertTrue(errors.isEmpty());
        assertEquals(5, bean.intArray.length);
        assertArrayEquals(new int[]{1, 2, 3, 0, 0}, bean.intArray);
    }

    @Test
    public void testApplyDelta_ListResizeAndSetElement() {
        Bean bean = new Bean(new ArrayList<>(Arrays.asList(1, 2)));
        GraphComparator.Delta resize = new GraphComparator.Delta(bean, "intList", "ptr", bean.intList, null, 4);
        resize.setCmd(GraphComparator.Delta.Command.LIST_RESIZE);
        GraphComparator.Delta set = new GraphComparator.Delta(bean, "intList", "ptr{3}", null, 99, 3);
        set.setCmd(GraphComparator.Delta.Command.LIST_SET_ELEMENT);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(
                bean,
                Arrays.asList(resize, set),
                identityIdFetcher,
                GraphComparator.getJavaDeltaProcessor()
        );

        assertTrue(errors.isEmpty());
        assertEquals(4, bean.intList.size());
        assertEquals(Integer.valueOf(99), bean.intList.get(3));
    }

    @Test
    public void testApplyDelta_SetAddAndRemove() {
        Bean bean = new Bean(new HashSet<>(Arrays.asList("a")));
        GraphComparator.Delta add = new GraphComparator.Delta(bean, "stringSet", "ptr", null, "b", null);
        add.setCmd(GraphComparator.Delta.Command.SET_ADD);
        GraphComparator.Delta remove = new GraphComparator.Delta(bean, "stringSet", "ptr", "a", null, null);
        remove.setCmd(GraphComparator.Delta.Command.SET_REMOVE);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(
                bean,
                Arrays.asList(add, remove),
                identityIdFetcher,
                GraphComparator.getJavaDeltaProcessor()
        );

        assertTrue(errors.isEmpty());
        assertTrue(bean.stringSet.contains("b"));
        assertFalse(bean.stringSet.contains("a"));
    }

    @Test
    public void testApplyDelta_MapPutAndRemove() {
        Bean bean = new Bean(new HashMap<>());
        bean.stringMap.put("k1", 1);

        GraphComparator.Delta put = new GraphComparator.Delta(bean, "stringMap", "ptr['k1']", 1, 2, "k1");
        put.setCmd(GraphComparator.Delta.Command.MAP_PUT);
        GraphComparator.Delta remove = new GraphComparator.Delta(bean, "stringMap", "ptr['k2']", null, null, "k2");
        remove.setCmd(GraphComparator.Delta.Command.MAP_REMOVE);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(
                bean,
                Arrays.asList(put, remove),
                identityIdFetcher,
                GraphComparator.getJavaDeltaProcessor()
        );

        assertTrue(errors.isEmpty());
        assertEquals(Integer.valueOf(2), bean.stringMap.get("k1"));
        assertFalse(bean.stringMap.containsKey("k2"));
    }

    @Test
    public void testApplyDelta_ObjectOrphan_NoError() {
        Bean bean = new Bean("orphan");
        GraphComparator.Delta orphan = new GraphComparator.Delta(bean, null, "", null, null, null);
        orphan.setCmd(GraphComparator.Delta.Command.OBJECT_ORPHAN);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(
                bean,
                Collections.singletonList(orphan),
                identityIdFetcher,
                GraphComparator.getJavaDeltaProcessor()
        );

        // Orphan processing is a no‑op; should produce no error.
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testApplyDelta_FailFastStopsAfterFirstError() {
        Bean bean = new Bean("first");
        // First delta will fail because field name does not exist.
        GraphComparator.Delta bad = new GraphComparator.Delta(bean, "nonexistent", "ptr", null, "x", null);
        bad.setCmd(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);
        // Second delta would be fine if reached.
        GraphComparator.Delta good = new GraphComparator.Delta(bean, "name", "ptr", "first", "second", null);
        good.setCmd(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(
                bean,
                Arrays.asList(bad, good),
                identityIdFetcher,
                GraphComparator.getJavaDeltaProcessor(),
                true // failFast = true
        );

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("field name missing"));
        // Ensure the second delta was NOT applied.
        assertEquals("first", bean.name);
    }

    @Test
    public void testApplyDelta_ArraySetElement_IndexOutOfBounds() throws NoSuchFieldException {
        Bean bean = new Bean(new int[]{5});
        GraphComparator.Delta delta = new GraphComparator.Delta(bean, "intArray", "ptr[5]", 5, 99, 5);
        delta.setCmd(GraphComparator.Delta.Command.ARRAY_SET_ELEMENT);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(
                bean,
                Collections.singletonList(delta),
                identityIdFetcher,
                GraphComparator.getJavaDeltaProcessor()
        );

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("index out of bounds"));
    }

    @Test
    public void testApplyDelta_ArrayResize_NegativeSize() {
        Bean bean = new Bean(new int[]{1, 2});
        GraphComparator.Delta delta = new GraphComparator.Delta(bean, "intArray", "ptr", bean.intArray, null, -1);
        delta.setCmd(GraphComparator.Delta.Command.ARRAY_RESIZE);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(
                bean,
                Collections.singletonList(delta),
                identityIdFetcher,
                GraphComparator.getJavaDeltaProcessor()
        );

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("must be a integer value 0 or greater"));
    }

    @Test
    public void testApplyDelta_ListSetElement_IndexOutOfBounds() {
        Bean bean = new Bean(new ArrayList<>(Arrays.asList(1, 2)));
        GraphComparator.Delta delta = new GraphComparator.Delta(bean, "intList", "ptr{5}", null, 99, 5);
        delta.setCmd(GraphComparator.Delta.Command.LIST_SET_ELEMENT);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(
                bean,
                Collections.singletonList(delta),
                identityIdFetcher,
                GraphComparator.getJavaDeltaProcessor()
        );

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("index out of bounds"));
    }

    @Test
    public void testCommandGetNameAndCaseInsensitiveFromName() {
        for (GraphComparator.Delta.Command cmd : GraphComparator.Delta.Command.values()) {
            String name = cmd.getName();
            // name is stored in lower case inside the enum
            assertEquals(name, name.toLowerCase());
            // fromName should be case‑insensitive
            GraphComparator.Delta.Command parsed = GraphComparator.Delta.Command.fromName(name.toUpperCase());
            assertEquals(cmd, parsed);
        }
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods used by several tests                                   */
    /* --------------------------------------------------------------------- */

    private static void assertArrayEquals(int[] expected, int[] actual) {
        assertEquals("Array lengths differ", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Element at index " + i + " differs", expected[i], actual[i]);
        }
    }
}
