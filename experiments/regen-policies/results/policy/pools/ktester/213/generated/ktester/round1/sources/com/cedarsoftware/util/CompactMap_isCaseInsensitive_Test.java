import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_isCaseInsensitive_Test {

//     class InconsistentMap extends CompactMap<String, String> {

//         @Override
//         protected int compactSize() {
//             // must be >= 2
//             return 3;
//         }

//         @Override
//         protected Map<String, String> getNewMap() {
//             // TreeMap with a case‑insensitive comparator
//             return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
//         }
    }

//     class ConsistentMap extends CompactMap<String, String> {

//         @Override
//         protected int compactSize() {
//             return 3;
//         }

//         @Override
//         protected Map<String, String> getNewMap() {
//             return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
//         }

//         @Override
//         protected boolean isCaseInsensitive() {
//             // Align the configuration with the comparator
//             return true;
//         }
    }

//     class ExposedMap extends CompactMap<String, String> {

//         @Override
//         protected int compactSize() {
//             return 3;
//         }

//         @Override
//         protected Map<String, String> getNewMap() {
//             return new HashMap<>();
//         }

        /**
         * Public wrapper that simply forwards to the protected implementation.
         */
//         public boolean isCaseInsensitivePublic() {
//             return super.isCaseInsensitive();
//         }
    }

//     class ExposedMap extends CompactMap<String, String> {

        @Override
        protected int compactSize() {
            return 3;
        }

        @Override
        protected Map<String, String> getNewMap() {
            return new HashMap<>();
        }

        public boolean isCaseInsensitivePublic() {
            return super.isCaseInsensitive();
        }
    }

//     class InconsistentMap extends CompactMap<String, String> {

        @Override
        protected int compactSize() {
            // must be >= 2
            return 3;
        }

        @Override
        protected Map<String, String> getNewMap() {
            // TreeMap with a case‑insensitive comparator
            return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }
    }

//     class ConsistentMap extends CompactMap<String, String> {

        @Override
        protected int compactSize() {
            return 3;
        }

        @Override
        protected Map<String, String> getNewMap() {
            return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }

        @Override
        protected boolean isCaseInsensitive() {
            // Align the configuration with the comparator
            return true;
        }
    }

//     class ExposedMap extends CompactMap<String, String> {

        @Override
        protected int compactSize() {
            return 3;
        }

        @Override
        protected Map<String, String> getNewMap() {
            return new HashMap<>();
        }

        /**
         * Public wrapper that simply forwards to the protected implementation.
         */
        public boolean isCaseInsensitivePublic() {
            return super.isCaseInsensitive();
        }
    }

//     class InconsistentMap extends CompactMap<String, String> {

        @Override
        protected int compactSize() {
            // must be >= 2
            return 3;
        }

        @Override
        protected Map<String, String> getNewMap() {
            // TreeMap with a case‑insensitive comparator
            return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }
    }

//     class ConsistentMap extends CompactMap<String, String> {

        @Override
        protected int compactSize() {
            return 3;
        }

        @Override
        protected Map<String, String> getNewMap() {
            return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }

        @Override
        protected boolean isCaseInsensitive() {
            // Align the configuration with the comparator
            return true;
        }
    }

//     class ExposedMap extends CompactMap<String, String> {

        @Override
        protected int compactSize() {
            return 3;
        }

        @Override
        protected Map<String, String> getNewMap() {
            return new HashMap<>();
        }

        /**
         * Public wrapper that simply forwards to the protected implementation.
         */
        public boolean isCaseInsensitivePublic() {
            return super.isCaseInsensitive();
        }
    }

//     class ExposedMap extends CompactMap<String, String> {

        @Override
        protected int compactSize() {
            return 3;
        }

        @Override
        protected Map<String, String> getNewMap() {
            return new HashMap<>();
        }

        public boolean isCaseInsensitivePublic() {
            return super.isCaseInsensitive();
        }
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void defaultIsCaseInsensitiveReturnsFalse() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             // make the compact size valid for the constructor check
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             // simple backing map – not relevant for this test
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new HashMap<>();
//             }
//         };
//         // The protected method can be called from an anonymous subclass
//         assertFalse(map.isCaseInsensitive(), "Default isCaseInsensitive() should be false (case‑sensitive).");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void constructorDetectsInconsistentCaseSensitivity() {
//         class InconsistentMap extends CompactMap<String, String> {
// 
//             @Override
//             protected int compactSize() {
//                 // must be >= 2
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 // TreeMap with a case‑insensitive comparator
//                 return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
//             }
//         }
//         IllegalStateException ex = assertThrows(IllegalStateException.class, InconsistentMap::new, "Expected IllegalStateException because the comparator is case‑insensitive " + "but isCaseInsensitive() returns false.");
//         assertTrue(ex.getMessage().contains("Inconsistent configuration"), "Exception message should indicate an inconsistent configuration.");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void constructorSucceedsWhenCaseSensitivityMatches() {
//         class ConsistentMap extends CompactMap<String, String> {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
//             }
// 
//             @Override
//             protected boolean isCaseInsensitive() {
//                 // Align the configuration with the comparator
//                 return true;
//             }
//         }
//         assertDoesNotThrow(ConsistentMap::new, "Construction should succeed when case‑insensitivity matches the comparator.");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void subclassExposesIsCaseInsensitivePublicly() {
//         class ExposedMap extends CompactMap<String, String> {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new HashMap<>();
//             }
// 
//             /**
//              * Public wrapper that simply forwards to the protected implementation.
//              */
//             public boolean isCaseInsensitivePublic() {
//                 return super.isCaseInsensitive();
//             }
//         }
//         ExposedMap map = new ExposedMap();
//         assertFalse(map.isCaseInsensitivePublic(), "Publicly exposed isCaseInsensitive() should still return false.");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void exposedMethodUnaffectedByMapContents() {
//         class ExposedMap extends CompactMap<String, String> {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new HashMap<>();
//             }
// 
//             public boolean isCaseInsensitivePublic() {
//                 return super.isCaseInsensitive();
//             }
//         }
//         ExposedMap map = new ExposedMap();
//         map.put("KeyOne", "Value1");
//         map.put("KeyTwo", "Value2");
//         map.put("KeyThree", "Value3");
//         assertFalse(map.isCaseInsensitivePublic(), "isCaseInsensitive() must stay false regardless of map entries.");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void reflectionInvokesIsCaseInsensitive() throws Exception {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new HashMap<>();
//             }
//         };
//         Method m = CompactMap.class.getDeclaredMethod("isCaseInsensitive");
//         m.setAccessible(true);
//         boolean result = (boolean) m.invoke(map);
//         assertFalse(result, "Reflection call to isCaseInsensitive() should return false.");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void reflectionCannotAlterDefaultCaseSensitiveConstant() throws Exception {
//         // Obtain the static final field
//         Field field = CompactMap.class.getDeclaredField("DEFAULT_CASE_SENSITIVE");
//         field.setAccessible(true);
//         // Remove the final modifier via reflection (works on most JVMs for test purposes)
//         Field modifiersField = Field.class.getDeclaredField("modifiers");
//         modifiersField.setAccessible(true);
//         modifiersField.setInt(field, field.getModifiers() & (~Modifier.FINAL));
//         // Attempt to set it to false
//         field.setBoolean(null, false);
//         // Now invoke the method via reflection
//         Method m = CompactMap.class.getDeclaredMethod("isCaseInsensitive");
//         m.setAccessible(true);
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new HashMap<>();
//             }
//         };
//         boolean result = (boolean) m.invoke(map);
//         // The method uses the compile‑time constant, so the result must still be false
//         assertFalse(result, "Even after attempting to flip DEFAULT_CASE_SENSITIVE, isCaseInsensitive() must remain false.");
//     }
}