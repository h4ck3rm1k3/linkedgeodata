package org.linkedgeodata.jtriplify.mapping;

public interface IOneOneTagMapperVisitor<T>
{
	T accept(SimpleClassTagMapper mapper);
	T accept(SimpleDataTypeTagMapper mapper);
	T accept(SimpleTextTagMapper mapper);
	T accept(SimpleObjectPropertyTagMapper mapper);
}
