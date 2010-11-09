package org.jboss.cache.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections15.multimap.MultiHashMap;

public class SetMultiHashMap<K, V>
		extends MultiHashMap<K, V>
{
	@Override
	protected Set<V> createCollection(Collection<? extends V> col)
	{
		return (col == null) ? new HashSet<V>() : new HashSet<V>(col);
	}
}
