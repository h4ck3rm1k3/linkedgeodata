package org.linkedgeodata.tagmapping.client.entity;

public interface IEntity
{
	<T> T accept(IEntityVisitor<T> visitor);
}
