package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Convention_throwIfKeyExists_Test {

    /**
     * Custom key class with proper equals/hashCode for map lookup tests.
     */
    static final class CustomKey {

        private final int id;

        CustomKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomKey) && ((CustomKey) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    /**
     * A deliberately broken Map implementation whose containsKey throws IllegalStateException.
     */
    static class FaultyMap<K, V> implements Map<K, V> {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object key) {
            throw new IllegalStateException("faulty containsKey");
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public V get(Object key) {
            return null;
        }

        @Override
        public V put(K key, V value) {
            return null;
        }

        @Override
        public V remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
        }

        @Override
        public void clear() {
        }

        @Override
        public Set<K> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Collection<V> values() {
            return Collections.emptyList();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }
    }

    @ParameterizedTest
    @NullSource
    void testThrowIfKeyExists_whenMapIsNull_throwsException(Map<?, ?> map) {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, "anyKey", "map cannot be null")).withMessageContaining("map cannot be null");
    }

    @ParameterizedTest
    @NullSource
    void testThrowIfKeyExists_whenKeyIsNull_throwsException(String key) {
        Map<String, String> map = new HashMap<>();
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, key, "key cannot be null")).withMessageContaining("key cannot be null");
    }

    @Test
    void testThrowIfKeyExists_whenKeyExists_throwsException() {
        Map<String, String> map = new HashMap<>();
        map.put("existingKey", "value");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, "existingKey", "duplicate key")).withMessageContaining("duplicate key");
    }

    @Test
    void testThrowIfKeyExists_whenKeyDoesNotExist_doesNotThrow() {
        Map<String, String> map = new HashMap<>();
        map.put("someKey", "value");
        assertThatNoException().isThrownBy(() -> Convention.throwIfKeyExists(map, "differentKey", "should not matter"));
    }

    @Test
    void testThrowIfKeyExists_nullMessageWhenKeyExists_throwsExceptionWithNullMessage() {
        Map<String, String> map = new HashMap<>() {

            {
                put("existingKey", "value");
            }
        };
        // Message is null – the IllegalArgumentException should be created with a null message.
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, "existingKey", null)).hasMessage(null);
    }

    @Test
    void testThrowIfKeyExists_nullMessageWhenKeyAbsent_doesNotThrow() {
        Map<String, String> map = Collections.emptyMap();
        assertThatNoException().isThrownBy(() -> Convention.throwIfKeyExists(map, "anyKey", null));
    }

    @Test
    void testThrowIfKeyExists_nullValueInMap_keyExists_throwsWithProvidedMessage() {
        Map<String, String> map = new HashMap<>() {

            {
                put("existingKey", null);
            }
        };
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, "existingKey", "null value entry")).withMessageContaining("null value entry");
    }

    @Test
    void testThrowIfKeyExists_unmodifiableMap_keyExists_throwsWithMessage() {
        Map<String, String> mutable = new HashMap<>() {

            {
                put("k", "v");
            }
        };
        Map<String, String> map = Collections.unmodifiableMap(mutable);
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, "k", "unmodifiable map")).withMessageContaining("unmodifiable map");
    }

    @Test
    void testThrowIfKeyExists_emptyStringMessageWhenKeyAbsent_doesNotThrow() {
        Map<String, String> map = new HashMap<>() {

            {
                put("present", "v");
            }
        };
        assertThatNoException().isThrownBy(() -> Convention.throwIfKeyExists(map, "absent", ""));
    }

    @Test
    void testThrowIfKeyExists_emptyStringMessageWhenKeyExists_throwsWithEmptyMessage() {
        Map<String, String> map = new HashMap<>() {

            {
                put("present", "v");
            }
        };
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, "present", "")).hasMessage("");
    }

    @Test
    void testThrowIfKeyExists_treeMapKeyExists_throwsWithMessage() {
        Map<String, Integer> map = new TreeMap<>() {

            {
                put("a", 1);
                put("b", 2);
            }
        };
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, "b", "tree map case")).withMessageContaining("tree map case");
    }

    @Test
    void testThrowIfKeyExists_customKeyEquality_keyExists_throwsWithMessage() {
        Map<CustomKey, String> map = new HashMap<>() {

            {
                put(new CustomKey(1), "one");
            }
        };
        // Different instance but equal according to equals()/hashCode()
        CustomKey lookupKey = new CustomKey(1);
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, lookupKey, "custom equality")).withMessageContaining("custom equality");
    }

    @Test
    void testThrowIfKeyExists_sideEffectCheck_mapSizeUnchanged() {
        Map<String, String> map = new HashMap<>() {

            {
                put("x", "y");
            }
        };
        int originalSize = map.size();
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, "x", "check side‑effect")).withMessageContaining("check side‑effect");
        // Verify that the map was not mutated by the method.
        assertThat(map).hasSize(originalSize).containsEntry("x", "y");
    }

    @Test
    void testThrowIfKeyExists_treeMapIncompatibleKey_propagatesClassCastException() {
        Map<String, Integer> map = new TreeMap<>() {

            {
                put("a", 1);
            }
        };
        // TreeMap expects a String key; passing an Integer triggers ClassCastException inside containsKey.
        assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, 1, "incompatible key type"));
    }

    @Test
    void testThrowIfKeyExists_faultyMap_containsKeyThrows_propagatesIllegalStateException() {
        Map<String, String> map = new FaultyMap<>();
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, "anyKey", "unused"));
    }
}
