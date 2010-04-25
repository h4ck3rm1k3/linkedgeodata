package org.linkedgeodata.jtriplify.methods;


public interface IFunctionContainer
{
	// Must return a function which satisfies all the arguments (respecting inheritance)
	// Use Object.class as a wildcard (e.g. if there is no constraint on the return type
	IInvocable lookup(Class<?> returnType, Class<?>... paramTypes);

	//IInvokable lookup(Class<?> returnType, /List<?> asList);
}
