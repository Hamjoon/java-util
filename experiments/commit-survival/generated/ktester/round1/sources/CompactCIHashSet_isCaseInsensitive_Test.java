package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.lang.reflect.Method;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactCIHashSet_isCaseInsensitive_Test {

    /**
     * Simple concrete subclass that exposes the protected method for testing.
     */
    private static class TestableCompactCIHashSet<E> extends CompactCIHashSet<E> {

        /**
         * Makes the protected method publicly accessible.
         */
        public boolean isCaseInsensitivePublic() {
            return super.isCaseInsensitive();
        }
    }

    /**
     * Supplies sets that have been mutated in different ways.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> mutationScenarios() {
        // 1. Empty set (baseline)
        TestableCompactCIHashSet<String> empty = new TestableCompactCIHashSet<>();
        // 2. After adding elements
        TestableCompactCIHashSet<String> afterAdd = new TestableCompactCIHashSet<>();
        afterAdd.add("a");
        afterAdd.add("b");
        // 3. After adding null
        TestableCompactCIHashSet<String> afterNull = new TestableCompactCIHashSet<>();
        afterNull.add(null);
        // 4. After clear()
        TestableCompactCIHashSet<String> afterClear = new TestableCompactCIHashSet<>();
        afterClear.add("x");
        afterClear.clear();
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("empty", empty), org.junit.jupiter.params.provider.Arguments.of("afterAdd", afterAdd), org.junit.jupiter.params.provider.Arguments.of("afterNull", afterNull), org.junit.jupiter.params.provider.Arguments.of("afterClear", afterClear));
    }

    @Test
    @DisplayName("isCaseInsensitive() should always return true")
    void testIsCaseInsensitiveReturnsTrue() {
        // given: a fresh instance of the testable subclass
        TestableCompactCIHashSet<String> set = new TestableCompactCIHashSet<>();
        // when: invoking the exposed method
        boolean result = set.isCaseInsensitivePublic();
        // then: the result must be true
        assertTrue(result, "CompactCIHashSet.isCaseInsensitive() should return true");
    }

    /* -------------------------------------------------------------
     *  After mutating the set (adding elements) the flag must stay true.
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("isCaseInsensitive() remains true after adding elements")
    void testIsCaseInsensitiveAfterAddingElements() {
        TestableCompactCIHashSet<String> set = new TestableCompactCIHashSet<>();
        set.add("Alpha");
        set.add("Beta");
        set.add("Gamma");
        assertTrue(set.isCaseInsensitivePublic(), "isCaseInsensitive() must stay true after adding elements");
    }

    /* -------------------------------------------------------------
     *  Adding a null element must not affect the flag.
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("isCaseInsensitive() remains true after adding null")
    void testIsCaseInsensitiveAfterAddingNull() {
        TestableCompactCIHashSet<String> set = new TestableCompactCIHashSet<>();
        // CompactCIHashSet (through CompactSet) should accept null like a normal Set.
        set.add(null);
        assertTrue(set.isCaseInsensitivePublic(), "isCaseInsensitive() must stay true after adding a null element");
    }

    /* -------------------------------------------------------------
     *  After clearing the set the flag must still be true.
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("isCaseInsensitive() remains true after clear()")
    void testIsCaseInsensitiveAfterClear() {
        TestableCompactCIHashSet<String> set = new TestableCompactCIHashSet<>();
        set.add("One");
        set.add("Two");
        set.clear();
        assertTrue(set.isCaseInsensitivePublic(), "isCaseInsensitive() must stay true after clear()");
    }

    /* -------------------------------------------------------------
     *  Verify type‑agnostic behavior by using a different generic type.
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("isCaseInsensitive() returns true for Integer generic type")
    void testIsCaseInsensitiveWithIntegerGeneric() {
        TestableCompactCIHashSet<Integer> intSet = new TestableCompactCIHashSet<>();
        intSet.add(42);
        intSet.add(7);
        assertTrue(intSet.isCaseInsensitivePublic(), "isCaseInsensitive() should be true for CompactCIHashSet<Integer>");
    }

    /* -------------------------------------------------------------
     *  Access the protected method via reflection – it must still return true.
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("isCaseInsensitive() via reflection returns true")
    void testIsCaseInsensitiveViaReflection() throws Exception {
        TestableCompactCIHashSet<String> set = new TestableCompactCIHashSet<>();
        Method method = CompactCIHashSet.class.getDeclaredMethod("isCaseInsensitive");
        method.setAccessible(true);
        Object result = method.invoke(set);
        assertTrue(result instanceof Boolean && (Boolean) result, "Reflection call to isCaseInsensitive() should return true");
    }

    /* -------------------------------------------------------------
     *  Serialize a populated set, deserialize it, and verify the flag.
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("isCaseInsensitive() remains true after serialization round‑trip")
    void testIsCaseInsensitiveAfterSerialization() throws IOException, ClassNotFoundException {
        TestableCompactCIHashSet<String> original = new TestableCompactCIHashSet<>();
        original.add("first");
        original.add("second");
        // include a null to ensure it survives serialization
        original.add(null);
        // Serialize
        byte[] serialized;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(original);
            out.flush();
            serialized = bos.toByteArray();
        }
        // Deserialize
        TestableCompactCIHashSet<String> deserialized;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
            ObjectInputStream in = new ObjectInputStream(bis)) {
            @SuppressWarnings("unchecked")
            TestableCompactCIHashSet<String> tmp = (TestableCompactCIHashSet<String>) in.readObject();
            deserialized = tmp;
        }
        assertTrue(deserialized.isCaseInsensitivePublic(), "isCaseInsensitive() must remain true after serialization/deserialization");
    }

    /* -------------------------------------------------------------
     *  Parameterized test that runs the same assertions for several
     *  mutation scenarios, demonstrating reuse of the same verification.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => scenario=''{0}''")
    @MethodSource("mutationScenarios")
    @DisplayName("isCaseInsensitive() stays true across various mutations")
    void testIsCaseInsensitiveAcrossMutations(String scenario, TestableCompactCIHashSet<String> set) {
        assertTrue(set.isCaseInsensitivePublic(), "isCaseInsensitive() must stay true after mutation scenario: " + scenario);
    }
}
