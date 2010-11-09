package org.jboss.cache.util;

import java.util.Set;

import com.google.common.collect.Sets;

public class SetDiff<T>
	extends CollectionDiff<T, Set<T>>
{
	public SetDiff(Set<T> newItems, Set<T> oldItems)
	{
		super(
				Sets.difference(newItems, oldItems),
				Sets.difference(oldItems, newItems),
				Sets.intersection(newItems, oldItems)
		);
	}
	
	public SetDiff(Set<T> added, Set<T> removed, Set<T> retained)
	{
		super(added, removed, retained);
	}

}
