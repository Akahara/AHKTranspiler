package fr.wonder.ahk.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import fr.wonder.commons.utils.Assertions;

public class NonNullableMap<K, V> extends HashMap<K, V> {

	private static final long serialVersionUID = -2858466538697475660L;

	@Override
	public V get(Object key) {
		return Objects.requireNonNull(super.get(key));
	}
	
	@Override
	public V put(K key, V value) {
		return super.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
	}
	
	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> of(Object... pairs) {
		Assertions.assertTrue(pairs.length % 2 == 0);
		Map<K, V> map = new NonNullableMap<>();
		for(int i = 0; i < pairs.length; i += 2)
			map.put((K) pairs[i], (V) pairs[i+1]);
		return map;
	}
	
}
