package org.jboss.cache.util;

import java.util.Collection;
import java.util.Map;

public interface IBulkMap<K, V>
	extends Map<K, V>
{
	Map<K, V> getAll(Collection<?> keys);
	void removeAll(Collection<?> keys);
}
