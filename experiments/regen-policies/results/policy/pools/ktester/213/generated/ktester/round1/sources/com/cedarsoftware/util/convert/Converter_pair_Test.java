package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.convert.Converter.ConversionPair;
// import org.junit.Test;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class Converter_pair_Test {

    /**
     * Supplies the combinations described in the JSON test cases that are expected to succeed.
     */
//     private static Stream<Arguments> validPairProvider() {
//         return // same source and target
//         Stream.// same source and target
//         of(// interface as source, concrete implementation as target
//         Arguments.of(String.class, Integer.class), // array class as source
//         Arguments.of(String.class, String.class), // primitive source, wrapper target
//         Arguments.of(List.class, ArrayList.class), // special primitive type (void) as source
//         Arguments.of(String[].class, List.class), Arguments.of(int.class, Integer.class), Arguments.of(void.class, Object.class));
//     }

//     @Test
//     void pairCreatesConversionPairWithGivenClasses() {
//         // GIVEN
//         Class<?> source = String.class;
//         Class<?> target = Integer.class;
//         // WHEN
//         ConversionPair pair = Converter.pair(source, target);
//         // THEN
//         assertNotNull("pair should not be null", pair);
//         assertEquals("source class should be stored unchanged", source, pair.getSource());
//         assertEquals("target class should be stored unchanged", target, pair.getTarget());
//     }

//     @Test
//     void pairWorksWithPrimitiveTypes() {
//         // GIVEN
//         Class<?> source = int.class;
//         Class<?> target = boolean.class;
//         // WHEN
//         ConversionPair pair = Converter.pair(source, target);
//         // THEN
//         assertNotNull(pair);
//         assertEquals(source, pair.getSource());
//         assertEquals(target, pair.getTarget());
//     }

//     @Test
//     void pairHandlesSameSourceAndTarget() {
//         // GIVEN
//         Class<?> source = Double.class;
//         Class<?> target = Double.class;
//         // WHEN
//         ConversionPair pair = Converter.pair(source, target);
//         // THEN
//         assertNotNull(pair);
//         assertEquals(source, pair.getSource());
//         assertEquals(target, pair.getTarget());
//     }

//     @Test
//     void pairIsImmutableAndHasConsistentHashCodeAndEquals() {
//         // GIVEN
//         ConversionPair p1 = Converter.pair(String.class, Long.class);
//         ConversionPair p2 = Converter.pair(String.class, Long.class);
//         ConversionPair p3 = Converter.pair(Long.class, String.class);
//         // THEN
//         assertEquals("Pairs with identical source/target should be equal", p1, p2);
//         assertEquals("Equal pairs must have same hashCode", p1.hashCode(), p2.hashCode());
//         assertNotEquals("Pairs with different source/target should not be equal", p1, p3);
//     }

//     @Test
//     void pairThrowsNPEWhenSourceIsNull() {
//         // GIVEN
//         Class<?> source = null;
//         Class<?> target = Integer.class;
//         // WHEN
//         Converter.pair(source, target);
//     }

//     @Test
//     void pairThrowsNPEWhenTargetIsNull() {
//         // GIVEN
//         Class<?> source = String.class;
//         Class<?> target = null;
//         // WHEN
//         Converter.pair(source, target);
//     }

//     @Test
//     void pairThrowsNPEWhenBothArgumentsAreNull() {
//         // GIVEN
//         Class<?> source = null;
//         Class<?> target = null;
//         // WHEN
//         Converter.pair(source, target);
//     }

//     @Test
//     void pairReturnsDistinctInstancesForIdenticalArguments() {
//         // GIVEN
//         Class<?> source = String.class;
//         Class<?> target = Integer.class;
//         // WHEN
//         ConversionPair first = Converter.pair(source, target);
//         ConversionPair second = Converter.pair(source, target);
//         // THEN
//         assertNotSame("Each call to pair should return a new ConversionPair instance", first, second);
//         assertEquals("Distinct instances must be equal by value", first, second);
//     }
}