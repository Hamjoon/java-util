package com.cedarsoftware.util;

// import org.junit.*;
// @ExtendWith(MockitoExtension.class)
class CompactMap_computeKeyHashCode_Test {

    private TestCompactMap caseSensitiveMap;

    private TestCompactMap caseInsensitiveMap;

    /**
     * Subclass that throws a RuntimeException from {@code isCaseInsensitive()}.
     */
    private static class ExceptionThrowingMap extends TestCompactMap {

        ExceptionThrowingMap() {
            super(false);
        }

        @Override
        protected boolean isCaseInsensitive() {
            throw new RuntimeException("forced exception from isCaseInsensitive");
        }
    }

    /**
     * Subclass that overrides {@code hashCode()} to throw an exception.
     */
    private static class MapWithThrowingHashCode extends TestCompactMap {

        MapWithThrowingHashCode() {
            super(false);
        }

        @Override
        public int hashCode() {
            throw new IllegalStateException("hashCode should not be called");
        }
    }

    /**
     * Custom key whose {@code hashCode()} throws an IllegalStateException.
     */
    private static final class HashCodeThrowingKey {

        @Override
        public int hashCode() {
            throw new IllegalStateException("forced exception from hashCode");
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof HashCodeThrowingKey;
        }
    }

    /**
     * Simple concrete subclass that makes {@code computeKeyHashCode} public
     * for testing purposes.
     */
    private static class TestCompactMap extends CompactMap<Object, Object> {

        private final boolean caseInsensitive;

        TestCompactMap(boolean caseInsensitive) {
            this.caseInsensitive = caseInsensitive;
        }

        /**
         * expose the protected method
         */
        public int computeKeyHashCodePublic(Object key) {
            return computeKeyHashCode(key);
        }

        @Override
        protected boolean isCaseInsensitive() {
            return caseInsensitive;
        }

        /* The following overrides satisfy the abstract contract of
           {@link CompactMap} for the purposes of this demo – they are not
           exercised by {@code computeKeyHashCode}. */
        @Override
        protected int compactSize() {
            return 10;
        }

//         @Override
//         protected Map<Object, Object> getNewMap() {
//             return new HashMap<>();
//         }
    }

    /**
     * Custom key with a deterministic overridden {@code hashCode}.
     */
    private static final class CustomKey {

        private final String id;

        CustomKey(String id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id.hashCode() * 31;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomKey) && ((CustomKey) o).id.equals(this.id);
        }
    }

    /**
     * Custom key that deliberately returns {@code 0} from {@code hashCode()}.
     */
    private static final class ZeroHashKey {

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ZeroHashKey;
        }
    }

//     @BeforeEach
    void setupBeforeEach() {
        caseSensitiveMap = new TestCompactMap(false);
        caseInsensitiveMap = new TestCompactMap(true);
    }

//     @AfterEach
    void teardownAfterEach() {
        caseSensitiveMap = null;
        caseInsensitiveMap = null;
    }

    /* ------------------------------------------------------------
     *  Parameterized tests for String keys (case‑sensitive / insensitive)
     * ------------------------------------------------------------ */
//     private static Stream<Arguments> stringKeyProvider() {
//         return Stream.of(Arguments.of(true, "TestString", StringUtilities.hashCodeIgnoreCase("TestString")), Arguments.of(false, "TestString", "TestString".hashCode()), Arguments.of(true, "", StringUtilities.hashCodeIgnoreCase("")), Arguments.of(true, "ÄßçÜö", StringUtilities.hashCodeIgnoreCase("ÄßçÜö")));
//     }

    /* ------------------------------------------------------------
     *  Parameterized tests for non‑String keys
     * ------------------------------------------------------------ */
//     private static Stream<Arguments> nonStringKeyProvider() {
//         return Stream.of(Arguments.of(12345, Integer.valueOf(12345).hashCode()), Arguments.of(new StringBuilder("builderKey"), new StringBuilder("builderKey").hashCode()), Arguments.of(new CustomKey("abc"), new CustomKey("abc").hashCode()), Arguments.of(new ZeroHashKey(), 0));
//     }

    public static void main(String[] args) {
        // -----------------------------------------------------------------
        //  Basic sanity checks
        // -----------------------------------------------------------------
        TestCompactMap caseSensitiveMap = new TestCompactMap(false);
        TestCompactMap caseInsensitiveMap = new TestCompactMap(true);
        // 1. Null key → 0
        assert caseSensitiveMap.computeKeyHashCodePublic(null) == 0 : "Null key must produce hash code 0";
        // 2. The map itself used as a key → constant 37
        int selfHash = caseSensitiveMap.computeKeyHashCodePublic(caseSensitiveMap);
        assert selfHash == 37 : "Self‑referential key should return 37, got " + selfHash;
        // 3. Ordinary (non‑String) keys delegate to their own hashCode()
        CustomKey ck = new CustomKey("abc");
        int ckHash = caseSensitiveMap.computeKeyHashCodePublic(ck);
        assert ckHash == ck.hashCode() : "CustomKey hash mismatch";
        ZeroHashKey zhk = new ZeroHashKey();
        int zhkHash = caseSensitiveMap.computeKeyHashCodePublic(zhk);
        assert zhkHash == 0 : "ZeroHashKey should produce hash 0";
        // 4. String keys respect case sensitivity
        String s = "TeStKey";
        int expectedSensitive = s.hashCode();
        int actualSensitive = caseSensitiveMap.computeKeyHashCodePublic(s);
        assert actualSensitive == expectedSensitive : "Case‑sensitive map should use String.hashCode()";
        // For case‑insensitive maps we mimic the behaviour of
        // StringUtilities.hashCodeIgnoreCase by using a lower‑cased version.
        // This avoids a dependency on the original utility class.
        String s2 = "TeStKey";
        int expectedInsensitive = s2.toLowerCase().hashCode();
        int actualInsensitive = caseInsensitiveMap.computeKeyHashCodePublic(s2);
        assert actualInsensitive == expectedInsensitive : "Case‑insensitive map should ignore case when hashing";
        System.out.println("All computeKeyHashCode checks passed.");
    }
}