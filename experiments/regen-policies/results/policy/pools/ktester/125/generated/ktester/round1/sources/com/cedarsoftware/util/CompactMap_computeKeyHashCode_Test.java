package com.cedarsoftware.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

// @ExtendWith(MockitoExtension.class)
class CompactMap_computeKeyHashCode_Test {

    private static class CaseInsensitiveStringScenario {

        final boolean caseInsensitive;

        final String key;

        final String description;

        CaseInsensitiveStringScenario(boolean caseInsensitive, String key, String description) {
            this.caseInsensitive = caseInsensitive;
            this.key = key;
            this.description = description;
        }
    }

    /**
     * Helper subclass that exposes {@code computeKeyHashCode} as a public method.
     * Only the abstract configuration methods required by {@link CompactMap}
     * are overridden with simple, deterministic behaviour.
     */
    private static class TestableCompactMap extends CompactMap<Object, Object> {

        private final boolean caseInsensitive;

        TestableCompactMap() {
            this(false);
        }

        TestableCompactMap(boolean caseInsensitive) {
            this.caseInsensitive = caseInsensitive;
        }

        /**
         * Expose the protected method for testing.
         */
        public int computeKeyHashCodePublic(Object key) {
            return computeKeyHashCode(key);
        }

        @Override
        protected int compactSize() {
            // any value >= 2 is acceptable for the constructor validation
            return 3;
        }

        @Override
        protected Map<Object, Object> getNewMap() {
            // simple backing map – the concrete type does not matter for these tests
            return new LinkedHashMap<>();
        }

        @Override
        protected boolean isCaseInsensitive() {
            return caseInsensitive;
        }
    }

    /**
     * Simple user‑defined key that overrides {@code hashCode()} to return a constant.
     */
    private static class CustomKey {

        private final int hash;

        CustomKey(int hash) {
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomKey) && ((CustomKey) o).hash == this.hash;
        }
    }

    private static Stream<CaseInsensitiveStringScenario> stringScenarios() {
        return Stream.of(new CaseInsensitiveStringScenario(false, "HelloWorld", "case‑sensitive"), new CaseInsensitiveStringScenario(true, "HelloWorld", "case‑insensitive"), new CaseInsensitiveStringScenario(true, "", "empty string case‑insensitive"));
    }

//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("stringScenarios")
//     void computeKeyHashCode_stringVariants(CaseInsensitiveStringScenario scenario) {
//         TestableCompactMap map = new TestableCompactMap(scenario.caseInsensitive);
//         int expected = scenario.caseInsensitive ? StringUtilities.hashCodeIgnoreCase(scenario.key) : scenario.key.hashCode();
//         int actual = map.computeKeyHashCodePublic(scenario.key);
//         assertEquals(expected, actual, "String key '" + scenario.key + "' with map caseInsensitive=" + scenario.caseInsensitive + " (" + scenario.description + ") must produce the correct hash code");
//     }
}