package org.jboss.cache.util;

import java.util.HashSet;
import java.util.Set;

public class HashSetDiff<T>
		extends CollectionDiff<T, Set<T>>
{
	public HashSetDiff()
	{
		super(new HashSet<T>(), new HashSet<T>(), new HashSet<T>());
	}
}