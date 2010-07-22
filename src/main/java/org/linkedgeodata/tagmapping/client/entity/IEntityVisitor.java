package org.linkedgeodata.tagmapping.client.entity;

public interface IEntityVisitor<T>
{
	T visit(SimpleClassTagMapperState state);
	T visit(SimpleDataTypeTagMapperState state);
	T visit(SimpleObjectPropertyTagMapperState state);
	T visit(SimpleTextTagMapperState state);
	
	T visit(RegexTextTagMapperState state);
}
