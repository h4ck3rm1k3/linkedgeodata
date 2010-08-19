package org.linkedgeodata.util;

public interface IDiff<T>
{
	T getAdded();
	T getRemoved();
	T getRetained();
}
