package org.linkedgeodata.util;

public class Diff<T>
	implements IDiff<T>
{
	private T added;
	private T removed;
	private T retained;
	
	
	public Diff(T added, T removed, T retained)
	{
		this.added = added;
		this.removed = removed;
		this.retained = retained;
	}
	
	@Override
	public T getAdded()
	{
		return added;
	}

	@Override
	public T getRemoved()
	{
		return removed;
	}

	@Override
	public T getRetained()
	{
		return retained;
	}
}
