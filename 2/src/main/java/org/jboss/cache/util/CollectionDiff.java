package org.jboss.cache.util;

import java.util.Collection;

import org.linkedgeodata.util.Diff;

public abstract class CollectionDiff<T, C extends Collection<T>>
		extends Diff<C>
{
	public CollectionDiff(C added, C removed, C retained)
	{
		super(added, removed, retained);
	}

	public void add(T item)
	{
		getRemoved().remove(item);
		getAdded().add(item);
	}

	public void remove(T item)
	{
		getAdded().remove(item);
		getRemoved().add(item);
	}

	public void clear()
	{
		getAdded().clear();
		getRemoved().clear();
	}

	public int size()
	{
		return getAdded().size() + getRemoved().size();
	}
}
