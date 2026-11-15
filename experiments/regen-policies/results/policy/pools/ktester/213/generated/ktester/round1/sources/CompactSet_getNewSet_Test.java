package com.cedarsoftware.util;

import java.util.*;
import java.util.stream.Stream;
// import org.junit.runner.RunWith;
// import org.junit.runners.Parameterized;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@ExtendWith(MockitoExtension.class)
@RunWith(Parameterized.class)
class CompactSet_getNewSet_Test {

    private TestCompactSet testSet;

    /* --------------------------------------------------------------------- *
     *  Parameter fields – injected by JUnit‑4
     * --------------------------------------------------------------------- */
    private final Map<String, Object> config;

    private final Object elementToAdd;

    private final Class<? extends Throwable> expectedException;

    private final Class<?> expectedSetClass;

    private final String description;

    //     @RunWith(Parameterized.class)
    public static class ParameterisedGetNewSetBehaviour {

        private final Map<String, Object> config;

        private final Object elementToAdd;

        private final Class<? extends Throwable> expectedException;

        private final Class<?> expectedSetClass;

        private final String description;

        private TestCompactSet testSet;

        public ParameterisedGetNewSetBehaviour(Map<String, Object> config, Object elementToAdd, Class<? extends Throwable> expectedException, Class<?> expectedSetClass, String description) {
            this.config = config;
            this.elementToAdd = elementToAdd;
            this.expectedException = expectedException;
            this.expectedSetClass = expectedSetClass;
            this.description = description;
        }
        //         @Parameterized.Parameters(name = "{index}: {4}")
        //         public static Collection<Object[]> data() {
        //             List<Object[]> rows = new ArrayList<>();
        //             // Helper to create a mutable copy of a map (may be null)
        //             java.util.function.Function<Map<String, Object>, Map<String, Object>> copy = m -> {
        //                 if (m == null)
        //                     return null;
        //                 return new HashMap<>(m);
        //             };
        //             // 1. Default behaviour (no config) – HashSet
        //             rows.add(new Object[]{null, null, null, HashSet.class,
        //                     "Default behavior (no config) – HashSet"});
        //             // 2. Sorted ordering (natural) – TreeSet
        //             rows.add(new Object[]{copy.apply(mapOf("ordering", "sorted", "caseSensitive", true)), null, null, TreeSet.class,
        //                     "Sorted ordering (natural) – TreeSet"});
        //             // 3. Insertion ordering – LinkedHashSet
        //             rows.add(new Object[]{copy.apply(mapOf("ordering", "insertion", "caseSensitive", true)), null, null, LinkedHashSet.class,
        //                     "Insertion ordering – LinkedHashSet"});
        //             // 4. Case‑insensitive sorted set – TreeSet with case‑insensitive comparator
        //             rows.add(new Object[]{copy.apply(mapOf("ordering", "sorted", "caseSensitive", false)), null, null, TreeSet.class,
        //                     "Case‑insensitive sorted set – TreeSet"});
        //             // 5. Case‑insensitive insertion ordering – LinkedHashSet (case‑sensitivity ignored)
        //             rows.add(new Object[]{copy.apply(mapOf("ordering", "insertion", "caseSensitive", false)), null, null, LinkedHashSet.class,
        //                     "Case‑insensitive insertion ordering – LinkedHashSet"});
        //             // 6. Invalid ordering falls back to HashSet
        //             rows.add(new Object[]{copy.apply(mapOf("ordering", "unknownValue", "caseSensitive", true)), null, null, HashSet.class,
        //                     "Invalid ordering – fallback to HashSet"});
        //             // 7. Explicit initial size hint – still LinkedHashSet
        //             rows.add(new Object[]{copy.apply(mapOf("ordering", "insertion", "caseSensitive", true, "size", 256)), null, null, LinkedHashSet.class,
        //                     "Explicit initial size hint – LinkedHashSet"});
        //             // 8. No configuration map (null) – HashSet
        //             rows.add(new Object[]{null, null, null, HashSet.class,
        //                     "Null config map – HashSet"});
        //             // 9. Empty configuration map – HashSet
        //             rows.add(new Object[]{new HashMap<>(), null, null, HashSet.class,
        //                     "Empty config map – HashSet"});
        //             // 10. Null ordering value – NullPointerException on creation
        //             rows.add(new Object[]{copy.apply(mapOf("ordering", null, "caseSensitive", true)), null, NullPointerException.class, null,
        //                     "Null ordering value – NPE"});
        //             // 11. Negative size hint – IllegalArgumentException on creation
        //             rows.add(new Object[]{copy.apply(mapOf("ordering", "insertion", "caseSensitive", true, "size", -10)), null, IllegalArgumentException.class, null,
        //                     "Negative size hint – IllegalArgumentException"});
        //             // 12. Non‑boolean caseSensitive value – ClassCastException on creation
        //             rows.add(new Object[]{copy.apply(mapOf("ordering", "sorted", "caseSensitive", "yes")), null, ClassCastException.class, null,
        //                     "Non‑boolean caseSensitive – ClassCastException"});
        //             // 13. Non‑string ordering value – ClassCastException on creation
        //             rows.add(new Object[]{copy.apply(mapOf("ordering", 123, "caseSensitive", true)), null, ClassCastException.class, null,
        //                     "Non‑string ordering – ClassCastException"});
        //             // 14. Sorted set with non‑Comparable element – ClassCastException on add
        //             rows.add(new Object[]{copy.apply(mapOf("ordering", "sorted", "caseSensitive", true)), 42, ClassCastException.class, TreeSet.class,
        //                     "Sorted set with non‑Comparable element – add fails"});
        //             // 15. Case‑insensitive sorted set with non‑String element – ClassCastException on add
        //             rows.add(new Object[]{copy.apply(mapOf("ordering", "sorted", "caseSensitive", false)), 100, ClassCastException.class, TreeSet.class,
        //                     "Case‑insensitive sorted set with non‑String element – add fails"});
        //             return rows;
        //         }
        //         @Before
        //         public void setUp() {
        //             testSet = new TestCompactSet();
        //             testSet.withConfig(config);
        //         }
        //         @org.junit.Test
        //         public void runBehaviourTest() {
        //             // -----------------------------------------------------------------
        //             // 1) Expect an exception while creating the Set
        //             // -----------------------------------------------------------------
        //             if (expectedException != null && elementToAdd == null) {
        //                 try {
        //                     testSet.exposeGetNewSet();
        //                     org.junit.Assert.fail("Expected exception of type " + expectedException.getName() + " but none was thrown. [" + description + "]");
        //                 } catch (Throwable t) {
        //                     if (!expectedException.isInstance(t)) {
        //                         org.junit.Assert.fail("Expected exception of type " + expectedException.getName() + " but got " + t.getClass().getName() + ". [" + description + "]");
        //                     }
        //                 }
        //                 return;
        //             }
        //             // -----------------------------------------------------------------
        //             // 2) Create the Set – should succeed
        //             // -----------------------------------------------------------------
        //             Set<String> resultSet;
        //             try {
        //                 resultSet = testSet.exposeGetNewSet();
        //             } catch (Throwable t) {
        //                 org.junit.Assert.fail("Unexpected exception while creating set: " + t + ". [" + description + "]");
        //                 return;
        //             }
        //             org.junit.Assert.assertNotNull("Resulting set should not be null. [" + description + "]", resultSet);
        //             org.junit.Assert.assertEquals("Resulting set should start empty. [" + description + "]", 0, resultSet.size());
        //             org.junit.Assert.assertTrue("Resulting set should be empty. [" + description + "]", resultSet.isEmpty());
        //             org.junit.Assert.assertTrue("Resulting set should be of expected type. [" + description + "]", expectedSetClass.isInstance(resultSet));
        //             // -----------------------------------------------------------------
        //             // 3) If an elementToAdd is supplied, we expect an exception on add
        //             // -----------------------------------------------------------------
        //             if (expectedException != null && elementToAdd != null) {
        //                 @SuppressWarnings("unchecked")
        //                 Set rawSet = resultSet; // raw to allow adding non‑String
        //                 try {
        //                     rawSet.add(elementToAdd);
        //                     org.junit.Assert.fail("Expected exception of type " + expectedException.getName() + " on add, but none was thrown. [" + description + "]");
        //                 } catch (Throwable t) {
        //                     if (!expectedException.isInstance(t)) {
        //                         org.junit.Assert.fail("Expected exception of type " + expectedException.getName() + " on add, but got " + t.getClass().getName() + ". [" + description + "]");
        //                     }
        //                 }
        //                 return;
        //             }
        //             // -----------------------------------------------------------------
        //             // 4) Normal behavioural checks for the successful cases
        //             // -----------------------------------------------------------------
        //             if (expectedSetClass == HashSet.class) {
        //                 // basic add/contains semantics
        //                 org.junit.Assert.assertTrue(resultSet.add("alpha"));
        //                 org.junit.Assert.assertFalse(resultSet.add("alpha"));
        //                 org.junit.Assert.assertTrue(resultSet.contains("alpha"));
        //                 org.junit.Assert.assertEquals(1, resultSet.size());
        //             } else if (expectedSetClass == LinkedHashSet.class) {
        //                 // verify insertion order
        //                 org.junit.Assert.assertTrue(resultSet.add("first"));
        //                 org.junit.Assert.assertTrue(resultSet.add("second"));
        //                 Iterator<String> it = resultSet.iterator();
        //                 org.junit.Assert.assertEquals("first", it.next());
        //                 org.junit.Assert.assertEquals("second", it.next());
        //             } else if (expectedSetClass == TreeSet.class && !testSet.isCaseInsensitive()) {
        //                 // natural ordering
        //                 org.junit.Assert.assertTrue(resultSet.add("b"));
        //                 org.junit.Assert.assertTrue(resultSet.add("a"));
        //                 Iterator<String> it = resultSet.iterator();
        //                 org.junit.Assert.assertEquals("a", it.next());
        //                 org.junit.Assert.assertEquals("b", it.next());
        //             } else if (expectedSetClass == TreeSet.class && testSet.isCaseInsensitive()) {
        //                 // case‑insensitive comparator behaviour
        //                 org.junit.Assert.assertTrue(resultSet.add("alpha"));
        //                 org.junit.Assert.assertFalse(resultSet.add("ALPHA"));
        //                 org.junit.Assert.assertEquals(1, resultSet.size());
        //                 resultSet.clear();
        //                 resultSet.add("beta");
        //                 resultSet.add("Alpha");
        //                 resultSet.add("gamma");
        //                 Iterator<String> it = resultSet.iterator();
        //                 // Expected order: Alpha, beta, gamma (case‑insensitive)
        //                 org.junit.Assert.assertEquals("Alpha", it.next());
        //                 org.junit.Assert.assertEquals("beta", it.next());
        //                 org.junit.Assert.assertEquals("gamma", it.next());
        //             }
        //         }
    }

    /**
     * Concrete subclass that makes {@code getNewSet()} visible for testing.
     */
    private static class TestCompactSet extends CompactSet<String> {

        /**
         * Exposes the protected method as public for the tests.
         */
        public Set<String> exposeGetNewSet() {
            return super.getNewSet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        testSet = new TestCompactSet();
    }

    @AfterEach
    void teardownAfterEach() {
        testSet = null;
    }

    private static Stream<Arguments> getNewSetTestCases() {
        List<Arguments> args = new ArrayList<>();
        // Helper to create a mutable map (may be null)
        java.util.function.Function<Map<String, Object>, Map<String, Object>> copy = m -> {
            if (m == null)
                return null;
            return new HashMap<>(m);
        };
        // 1. Default behavior (no config) – HashSet
        args.add(Arguments.of(null, null, null, HashSet.class, "Default behavior (no config) – HashSet"));
        // 2. Sorted ordering (natural) – TreeSet
        args.add(Arguments.of(copy.apply(mapOf("ordering", "sorted", "caseSensitive", true)), null, null, TreeSet.class, "Sorted ordering (natural) – TreeSet"));
        // 3. Insertion ordering – LinkedHashSet
        args.add(Arguments.of(copy.apply(mapOf("ordering", "insertion", "caseSensitive", true)), null, null, LinkedHashSet.class, "Insertion ordering – LinkedHashSet"));
        // 4. Case‑insensitive sorted set – TreeSet with case‑insensitive comparator
        args.add(Arguments.of(copy.apply(mapOf("ordering", "sorted", "caseSensitive", false)), null, null, TreeSet.class, "Case‑insensitive sorted set – TreeSet"));
        // 5. Case‑insensitive insertion ordering – LinkedHashSet (case‑sensitivity ignored)
        args.add(Arguments.of(copy.apply(mapOf("ordering", "insertion", "caseSensitive", false)), null, null, LinkedHashSet.class, "Case‑insensitive insertion ordering – LinkedHashSet"));
        // 6. Invalid ordering falls back to HashSet
        args.add(Arguments.of(copy.apply(mapOf("ordering", "unknownValue", "caseSensitive", true)), null, null, HashSet.class, "Invalid ordering – fallback to HashSet"));
        // 7. Explicit initial size hint – still LinkedHashSet
        args.add(Arguments.of(copy.apply(mapOf("ordering", "insertion", "caseSensitive", true, "size", 256)), null, null, LinkedHashSet.class, "Explicit initial size hint – LinkedHashSet"));
        // 8. No configuration map (null) – HashSet
        args.add(Arguments.of(null, null, null, HashSet.class, "Null config map – HashSet"));
        // 9. Empty configuration map – HashSet
        args.add(Arguments.of(new HashMap<>(), null, null, HashSet.class, "Empty config map – HashSet"));
        // 10. Null ordering value – NullPointerException on creation
        args.add(Arguments.of(copy.apply(mapOf("ordering", null, "caseSensitive", true)), null, NullPointerException.class, null, "Null ordering value – NPE"));
        // 11. Negative size hint – IllegalArgumentException on creation
        args.add(Arguments.of(copy.apply(mapOf("ordering", "insertion", "caseSensitive", true, "size", -10)), null, IllegalArgumentException.class, null, "Negative size hint – IllegalArgumentException"));
        // 12. Non‑boolean caseSensitive value – ClassCastException on creation
        args.add(Arguments.of(copy.apply(mapOf("ordering", "sorted", "caseSensitive", "yes")), null, ClassCastException.class, null, "Non‑boolean caseSensitive – ClassCastException"));
        // 13. Non‑string ordering value – ClassCastException on creation
        args.add(Arguments.of(copy.apply(mapOf("ordering", 123, "caseSensitive", true)), null, ClassCastException.class, null, "Non‑string ordering – ClassCastException"));
        // 14. Sorted set with non‑Comparable element – ClassCastException on add
        args.add(Arguments.of(copy.apply(mapOf("ordering", "sorted", "caseSensitive", true)), 42, ClassCastException.class, TreeSet.class, "Sorted set with non‑Comparable element – add fails"));
        // 15. Case‑insensitive sorted set with non‑String element – ClassCastException on add
        args.add(Arguments.of(copy.apply(mapOf("ordering", "sorted", "caseSensitive", false)), 100, ClassCastException.class, TreeSet.class, "Case‑insensitive sorted set with non‑String element – add fails"));
        return args.stream();
    }

    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("getNewSetTestCases")
    void getNewSetBehaviour(Map<String, Object> config, Object elementToAdd, Class<? extends Throwable> expectedException, Class<?> expectedSetClass, String description) {
        // Apply configuration (null config is allowed)
        testSet.withConfig(config);
        // -----------------------------------------------------------------
        // 1) Expect an exception while creating the Set
        // -----------------------------------------------------------------
        if (expectedException != null && elementToAdd == null) {
            try {
                testSet.exposeGetNewSet();
                Assert.fail("Expected exception of type " + expectedException.getName() + " but none was thrown. [" + description + "]");
            } catch (Throwable t) {
                if (!expectedException.isInstance(t)) {
                    Assert.fail("Expected exception of type " + expectedException.getName() + " but got " + t.getClass().getName() + ". [" + description + "]");
                }
                // exception as expected – test passes
                return;
            }
        }
        // -----------------------------------------------------------------
        // 2) Create the Set – should succeed
        // -----------------------------------------------------------------
        Set<String> resultSet;
        try {
            resultSet = testSet.exposeGetNewSet();
        } catch (Throwable t) {
            Assert.fail("Unexpected exception while creating set: " + t + ". [" + description + "]");
            // unreachable, but keeps compiler happy
            return;
        }
        // Verify the concrete Set implementation and that it starts empty
        Assert.assertNotNull("Resulting set should not be null. [" + description + "]", resultSet);
        Assert.assertEquals("Resulting set should start empty. [" + description + "]", 0, resultSet.size());
        Assert.assertTrue("Resulting set should be empty. [" + description + "]", resultSet.isEmpty());
        Assert.assertTrue("Resulting set should be of expected type. [" + description + "]", expectedSetClass.isInstance(resultSet));
        // -----------------------------------------------------------------
        // 3) If an elementToAdd is supplied, we expect an exception on add
        // -----------------------------------------------------------------
        if (expectedException != null && elementToAdd != null) {
            @SuppressWarnings("unchecked")
            Set // raw to allow adding non‑String
            rawSet = resultSet;
            try {
                rawSet.add(elementToAdd);
                Assert.fail("Expected exception of type " + expectedException.getName() + " on add, but none was thrown. [" + description + "]");
            } catch (Throwable t) {
                if (!expectedException.isInstance(t)) {
                    Assert.fail("Expected exception of type " + expectedException.getName() + " on add, but got " + t.getClass().getName() + ". [" + description + "]");
                }
                // exception as expected – test passes
                return;
            }
        }
        // -----------------------------------------------------------------
        // 4) Normal behavioural checks for the successful cases
        // -----------------------------------------------------------------
        if (expectedSetClass == HashSet.class) {
            // basic add/contains semantics
            Assert.assertTrue(resultSet.add("alpha"));
            Assert.assertFalse(resultSet.add("alpha"));
            Assert.assertTrue(resultSet.contains("alpha"));
            Assert.assertEquals(1, resultSet.size());
        } else if (expectedSetClass == LinkedHashSet.class) {
            // verify insertion order
            Assert.assertTrue(resultSet.add("first"));
            Assert.assertTrue(resultSet.add("second"));
            Iterator<String> it = resultSet.iterator();
            Assert.assertEquals("first", it.next());
            Assert.assertEquals("second", it.next());
        } else if (expectedSetClass == TreeSet.class && !testSet.isCaseInsensitive()) {
            // natural ordering
            Assert.assertTrue(resultSet.add("b"));
            Assert.assertTrue(resultSet.add("a"));
            Iterator<String> it = resultSet.iterator();
            Assert.assertEquals("a", it.next());
            Assert.assertEquals("b", it.next());
        } else if (expectedSetClass == TreeSet.class && testSet.isCaseInsensitive()) {
            // case‑insensitive comparator behaviour
            Assert.assertTrue(resultSet.add("alpha"));
            Assert.assertFalse(resultSet.add("ALPHA"));
            Assert.assertEquals(1, resultSet.size());
            resultSet.clear();
            resultSet.add("beta");
            resultSet.add("Alpha");
            resultSet.add("gamma");
            Iterator<String> it = resultSet.iterator();
            // Expected order: Alpha, beta, gamma (case‑insensitive)
            Assert.assertEquals("Alpha", it.next());
            Assert.assertEquals("beta", it.next());
            Assert.assertEquals("gamma", it.next());
        }
    }

    /**
     * Utility to build a map literal in a concise way.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapOf(Object... keyVals) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            map.put((String) keyVals[i], keyVals[i + 1]);
        }
        return map;
    }

    @Before
    public void setUp() {
        testSet = new TestCompactSet();
    }

    @After
    public void tearDown() {
        testSet = null;
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised test data
     * --------------------------------------------------------------------- */
    @Parameters(name = "{index}: {4}")
    public static Collection<Object[]> data() {
        List<Object[]> rows = new ArrayList<>();
        // Helper to create a mutable copy of a map (may be null)
        java.util.function.Function<Map<String, Object>, Map<String, Object>> copy = m -> {
            if (m == null)
                return null;
            return new HashMap<>(m);
        };
        // 1. Default behaviour (no config) – HashSet
        rows.add(new Object[] { null, null, null, HashSet.class, "Default behavior (no config) – HashSet" });
        // 2. Sorted ordering (natural) – TreeSet
        rows.add(new Object[] { copy.apply(mapOf("ordering", "sorted", "caseSensitive", true)), null, null, TreeSet.class, "Sorted ordering (natural) – TreeSet" });
        // 3. Insertion ordering – LinkedHashSet
        rows.add(new Object[] { copy.apply(mapOf("ordering", "insertion", "caseSensitive", true)), null, null, LinkedHashSet.class, "Insertion ordering – LinkedHashSet" });
        // 4. Case‑insensitive sorted set – TreeSet with case‑insensitive comparator
        rows.add(new Object[] { copy.apply(mapOf("ordering", "sorted", "caseSensitive", false)), null, null, TreeSet.class, "Case‑insensitive sorted set – TreeSet" });
        // 5. Case‑insensitive insertion ordering – LinkedHashSet (case‑sensitivity ignored)
        rows.add(new Object[] { copy.apply(mapOf("ordering", "insertion", "caseSensitive", false)), null, null, LinkedHashSet.class, "Case‑insensitive insertion ordering – LinkedHashSet" });
        // 6. Invalid ordering falls back to HashSet
        rows.add(new Object[] { copy.apply(mapOf("ordering", "unknownValue", "caseSensitive", true)), null, null, HashSet.class, "Invalid ordering – fallback to HashSet" });
        // 7. Explicit initial size hint – still LinkedHashSet
        rows.add(new Object[] { copy.apply(mapOf("ordering", "insertion", "caseSensitive", true, "size", 256)), null, null, LinkedHashSet.class, "Explicit initial size hint – LinkedHashSet" });
        // 8. No configuration map (null) – HashSet
        rows.add(new Object[] { null, null, null, HashSet.class, "Null config map – HashSet" });
        // 9. Empty configuration map – HashSet
        rows.add(new Object[] { new HashMap<>(), null, null, HashSet.class, "Empty config map – HashSet" });
        // 10. Null ordering value – NullPointerException on creation
        rows.add(new Object[] { copy.apply(mapOf("ordering", null, "caseSensitive", true)), null, NullPointerException.class, null, "Null ordering value – NPE" });
        // 11. Negative size hint – IllegalArgumentException on creation
        rows.add(new Object[] { copy.apply(mapOf("ordering", "insertion", "caseSensitive", true, "size", -10)), null, IllegalArgumentException.class, null, "Negative size hint – IllegalArgumentException" });
        // 12. Non‑boolean caseSensitive value – ClassCastException on creation
        rows.add(new Object[] { copy.apply(mapOf("ordering", "sorted", "caseSensitive", "yes")), null, ClassCastException.class, null, "Non‑boolean caseSensitive – ClassCastException" });
        // 13. Non‑string ordering value – ClassCastException on creation
        rows.add(new Object[] { copy.apply(mapOf("ordering", 123, "caseSensitive", true)), null, ClassCastException.class, null, "Non‑string ordering – ClassCastException" });
        // 14. Sorted set with non‑Comparable element – ClassCastException on add
        rows.add(new Object[] { copy.apply(mapOf("ordering", "sorted", "caseSensitive", true)), 42, ClassCastException.class, TreeSet.class, "Sorted set with non‑Comparable element – add fails" });
        // 15. Case‑insensitive sorted set with non‑String element – ClassCastException on add
        rows.add(new Object[] { copy.apply(mapOf("ordering", "sorted", "caseSensitive", false)), 100, ClassCastException.class, TreeSet.class, "Case‑insensitive sorted set with non‑String element – add fails" });
        return rows;
    }
}
