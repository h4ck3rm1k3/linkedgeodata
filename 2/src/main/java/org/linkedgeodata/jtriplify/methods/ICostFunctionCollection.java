package org.linkedgeodata.jtriplify.methods;

import java.util.List;

public interface ICostFunctionCollection<I extends IInvocable, CX>
//extends IFunctionContainer
{
	Pair<I, CX> lookupCheapest(Class<?> returnType, Class<?>... paramTypes);
	//Pair<I, C> lookupCheapest(Class<?> returnType, Class<?>... paramTypes);
	//Set<Pair<C, I>> lookupAll(Class<?> returnType, Class<?>... paramTypes);
}

