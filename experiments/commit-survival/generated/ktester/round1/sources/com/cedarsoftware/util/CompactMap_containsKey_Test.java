package com.cedarsoftware.util;

import java.lang.reflect.Field;

// import org.junit.*;
// import static org.junit.Assert.*;
// import org.junit.runner.RunWith;
// import org.junit.runners.Parameterized;
// import org.junit.runners.Parameterized.Parameters;
class CompactMap_containsKey_Test {

    /**
     * The map under test – created fresh for each test method.
     */
    private CompactMap<String, Integer> map;

    //     @RunWith(Parameterized.class)
    public static class ArrayRepresentationParameterizedTest {

        private final Object[] internalArray;

        private final Object lookupKey;

        private final boolean expected;

        public ArrayRepresentationParameterizedTest(Object[] internalArray, Object lookupKey, boolean expected) {
            this.internalArray = internalArray;
            this.lookupKey = lookupKey;
            this.expected = expected;
        }
        //         @Parameters(name = "{index}: array={0}, key={1} => {2}")
        //         public static Collection<Object[]> data() {
        //             return Arrays.asList(new Object[][] { { new Object[] { "alpha", 10, "beta", 20 }, "alpha", true }, { new Object[] { "alpha", 10, "beta", 20 }, "beta", true }, { new Object[] { "alpha", 10, "beta", 20 }, "gamma", false }, { new Object[] { null, "v1", "alpha", "v2" }, null, true }, { new Object[] { null, "v1", "alpha", "v2" }, "alpha", true }, { new Object[] { null, "v1", "alpha", "v2" }, "beta", false } });
        //         }
        //         @Test
        //         public void containsKey_arrayRepresentation_parameterised() throws Exception {
        //             CompactMap<?, ?> map = new CompactMap<>();
        //             setInternalVal(map, internalArray);
        //             assertEquals(expected, map.containsKey(lookupKey));
        //         }
    }

    /**
     * Subclass that forces case‑insensitive behaviour.
     */
    private static class CaseInsensitiveCompactMap extends CompactMap<String, Integer> {

        @Override
        protected boolean isCaseInsensitive() {
            return true;
        }

//         @Override
//         protected Map<String, Integer> getNewMap() {
//             // Use a normal HashMap – case‑insensitivity is handled by compareKeys()
//             return new HashMap<>();
//         }
    }

    //     @BeforeEach
    void setupBeforeEach() {
        map = new CompactMap<>();
    }

    //     @AfterEach
    void teardownAfterEach() {
        map = null;
    }

    /**
     * Uses reflection to replace the private {@code val} field of {@link CompactMap}
     * with a custom object (Object[] or Map). This allows us to force the internal
     * representation without exercising the public mutating methods.
     */
    private static void setInternalVal(CompactMap<?, ?> target, Object newVal) {
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(target, newVal);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}