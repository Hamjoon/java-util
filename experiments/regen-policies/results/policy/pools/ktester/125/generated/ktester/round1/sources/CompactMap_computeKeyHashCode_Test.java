package com.cedarsoftware.util;

// import org.junit.*;
// import org.junit.runner.RunWith;
import java.util.*;

// @ExtendWith(MockitoExtension.class)
class CompactMap_computeKeyHashCode_Test {

    private TestCompactMap caseSensitiveMap;

    private TestCompactMap caseInsensitiveMap;

    /* --------------------------------------------------------------- */
    private static class TestCompactMap extends CompactMap<Object, Object> {

        private final boolean caseInsensitive;

        TestCompactMap(boolean caseInsensitive) {
            this.caseInsensitive = caseInsensitive;
        }

        /**
         * Expose the protected method for the tests.
         */
        int callComputeKeyHashCode(Object key) {
            return super.computeKeyHashCode(key);
        }

        @Override
        protected int compactSize() {
            // any value ≥ 2 is acceptable
            return 3;
        }

        @Override
        protected Map<Object, Object> getNewMap() {
            return new LinkedHashMap<>();
        }

        @Override
        protected boolean isCaseInsensitive() {
            return caseInsensitive;
        }
    }

    /**
     * Subclass that forces {@code isCaseInsensitive()} to throw an exception.
     */
    private static class ExceptionThrowingMap extends TestCompactMap {

        ExceptionThrowingMap() {
            super(true);
        }

        @Override
        protected boolean isCaseInsensitive() {
            throw new IllegalStateException("forced for test");
        }
    }

    // @RunWith(Parameterized.class)
    public static class NormalCases {
        // ... (unchanged, still commented out)
    }

    //     @BeforeEach
    void setupBeforeEach() {
        caseSensitiveMap = new TestCompactMap(false);
        caseInsensitiveMap = new TestCompactMap(true);
    }
}
