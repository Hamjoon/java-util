package com.cedarsoftware.util;

// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;
import java.util.HashMap;

// @ExtendWith(MockitoExtension.class)
class CompactMap_computeValueHashCode_Test {

    private TestCompactMap map;

    /**
     * Simple concrete subclass that makes the protected method public.
     */
    private static class TestCompactMap extends CompactMap<Object, Object> {

        // The CompactMap constructor checks that compactSize() >= 2.
        @Override
        protected int compactSize() {
            return 3;
        }

        @Override
        protected java.util.Map<Object, Object> getNewMap() {
            return new HashMap<>();
        }

        /**
         * Expose the protected method for testing.
         */
        @Override
        public int computeValueHashCode(Object value) {
            return super.computeValueHashCode(value);
        }
    }

    /**
     * Helper class whose {@code hashCode()} throws {@link NullPointerException}.
     */
    private static class HashCodeThrowsNPE {

        @Override
        public int hashCode() {
            throw new NullPointerException("Deliberate NPE from hashCode()");
        }
    }

    /**
     * Helper class whose {@code hashCode()} throws {@link IllegalArgumentException}.
     */
    private static class HashCodeThrowsIAE {

        @Override
        public int hashCode() {
            throw new IllegalArgumentException("Deliberate IAE from hashCode()");
        }
    }

    /**
     * Helper class whose {@code hashCode()} throws a generic {@link RuntimeException}.
     */
    private static class HashCodeThrowsRuntime {

        @Override
        public int hashCode() {
            throw new RuntimeException("Deliberate RuntimeException from hashCode()");
        }
    }

//     @BeforeEach
    void setupBeforeEach() {
        map = new TestCompactMap();
    }

//     @AfterEach
    void teardownAfterEach() {
        map = null;
    }

//     private static Stream<Arguments> normalValueProvider() {
//         Object customNeg12345 = new Object() {
// 
//             @Override
//             public int hashCode() {
//                 return -12345;
//             }
//         };
//         Object customMinInt = new Object() {
// 
//             @Override
//             public int hashCode() {
//                 return Integer.MIN_VALUE;
//             }
//         };
//         Object customZero = new Object() {
// 
//             @Override
//             public int hashCode() {
//                 return 0;
//             }
//         };
//         return Stream.of(arguments((Object) null, 0), arguments(42, 42), arguments(customNeg12345, -12345), arguments("test", "test".hashCode()), arguments(Boolean.TRUE, Boolean.TRUE.hashCode()), arguments(customMinInt, Integer.MIN_VALUE), arguments(customZero, 0));
//     }

//     private static Stream<Arguments> exceptionValueProvider() {
//         return Stream.of(arguments(new HashCodeThrowsNPE(), NullPointerException.class), arguments(new HashCodeThrowsIAE(), IllegalArgumentException.class), arguments(new HashCodeThrowsRuntime(), RuntimeException.class));
//     }

//     @Test
//     void computeValueHashCode_returns17WhenValueIsMapItself() {
//         int hash = map.computeValueHashCode(map);
//         assertEquals("When the value is the map instance itself the method should return 17", 17, hash);
//     }

//     @Test
//     void computeValueHashCode_returnsZeroWhenValueIsNull() {
//         int hash = map.computeValueHashCode(null);
//         assertEquals("When the value is null the method should return 0", 0, hash);
//     }

//     @Test
//     void computeValueHashCode_delegatesToValueHashCodeWhenNonNullAndNotSelf() {
//         Object value = new Object() {
// 
//             @Override
//             public int hashCode() {
//                 return 42;
//             }
//         };
//         int hash = map.computeValueHashCode(value);
//         assertEquals("For a non‑null, non‑self value the method should return value.hashCode()", 42, hash);
//     }

//     @Test
//     void computeValueHashCode_usesStandardHashCodeForCommonTypes() {
//         assertEquals("hello".hashCode(), map.computeValueHashCode("hello"));
//         Integer i = 12345;
//         assertEquals(i.hashCode(), map.computeValueHashCode(i));
//         Double d = 3.14159;
//         assertEquals(d.hashCode(), map.computeValueHashCode(d));
//     }

//     @ParameterizedTest(name = "{index}: value={0} → expectedHash={1}")
//     @MethodSource("normalValueProvider")
//     void computeValueHashCode_returnsExpectedHash(Object input, int expectedHash) {
//         int actual = map.computeValueHashCode(input);
//         assertEquals(expectedHash, actual, () -> "Expected hash " + expectedHash + " but got " + actual + " for input: " + input);
//     }

//     @ParameterizedTest(name = "{index}: value throws {1}")
//     @MethodSource("exceptionValueProvider")
//     void computeValueHashCode_propagatesException(Object input, Class<? extends Throwable> expectedException) {
//         assertThrows(expectedException, () -> map.computeValueHashCode(input), () -> "Expected " + expectedException.getSimpleName() + " to be propagated");
//     }

//     @Test
//     void computeValueHashCode_customNegativeHash() {
//         Object customNeg12345 = new Object() {
// 
//             @Override
//             public int hashCode() {
//                 return -12345;
//             }
//         };
//         assertEquals(-12345, map.computeValueHashCode(customNeg12345));
//     }

//     @Test
//     void computeValueHashCode_customMinIntHash() {
//         Object customMinInt = new Object() {
// 
//             @Override
//             public int hashCode() {
//                 return Integer.MIN_VALUE;
//             }
//         };
//         assertEquals(Integer.MIN_VALUE, map.computeValueHashCode(customMinInt));
//     }

//     @Test
//     void computeValueHashCode_customZeroHash() {
//         Object customZero = new Object() {
// 
//             @Override
//             public int hashCode() {
//                 return 0;
//             }
//         };
//         assertEquals(0, map.computeValueHashCode(customZero));
//     }

//     @Test
//     void computeValueHashCode_stringHash() {
//         String s = "test";
//         assertEquals(s.hashCode(), map.computeValueHashCode(s));
//     }

//     @Test
//     void computeValueHashCode_booleanTrueHash() {
//         assertEquals(Boolean.TRUE.hashCode(), map.computeValueHashCode(Boolean.TRUE));
//     }

//     @Test
//     void computeValueHashCode_integerHash() {
//         assertEquals(Integer.valueOf(42).hashCode(), map.computeValueHashCode(42));
//     }

//     @Test
//     void computeValueHashCode_propagatesNullPointerException() {
//         map.computeValueHashCode(new HashCodeThrowsNPE());
//     }

//     @Test
//     void computeValueHashCode_propagatesIllegalArgumentException() {
//         map.computeValueHashCode(new HashCodeThrowsIAE());
//     }

//     @Test
//     void computeValueHashCode_propagatesRuntimeException() {
//         map.computeValueHashCode(new HashCodeThrowsRuntime());
//     }

//     @Before
//     public void setUp() {
//         map = new TestCompactMap();
//     }

//     @After
//     public void tearDown() {
//         map = null;
//     }
}