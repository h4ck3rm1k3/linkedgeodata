package org.linkedgeodata.jtriplify.methods;

/**
 * Returns a method which is capable of converting an object of type source to target.
 * Null if no such method exists.
 * 
 * If multiple methods exists, and all should be returned, then this must be handled in
 * a different class. Objects using this class rely that there is always just a single solution.
 * 
 * @author raven
 *
 */
interface ICoercionManager
{
	IInvocable lookup(Class<?> source, Class<?> target);
}
