package org.jboss.cache.util;

import java.util.Collection;
import java.util.Map;

public interface IBulkMap<K, V>
	extends Map<K, V>
{
	Map<K, V> getAll(Collection<?> keys);
	
	// This method should actually be replaced with Map.keySet().removeAll(...);
	void removeAll(Collection<?> keys);
}
