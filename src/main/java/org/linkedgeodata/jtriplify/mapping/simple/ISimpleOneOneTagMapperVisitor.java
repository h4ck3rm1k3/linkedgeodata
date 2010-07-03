package org.linkedgeodata.jtriplify.mapping.simple;

public interface ISimpleOneOneTagMapperVisitor<T>
{
	T accept(SimpleClassTagMapper mapper);
	T accept(SimpleDataTypeTagMapper mapper);
	T accept(SimpleTextTagMapper mapper);
	T accept(SimpleObjectPropertyTagMapper mapper);
}
