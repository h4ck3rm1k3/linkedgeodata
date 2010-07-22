package org.linkedgeodata.osm.mapping;

import java.util.regex.Pattern;

import org.linkedgeodata.osm.mapping.impl.RegexTextTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleClassTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleDataTypeTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleObjectPropertyTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleTagPattern;
import org.linkedgeodata.osm.mapping.impl.SimpleTextTagMapper;
import org.linkedgeodata.tagmapping.client.entity.IEntity;
import org.linkedgeodata.tagmapping.client.entity.IEntityVisitor;
import org.linkedgeodata.tagmapping.client.entity.RegexTextTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleClassTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleDataTypeTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleObjectPropertyTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleTextTagMapperState;



public class TagMapperInstantiator
	implements IEntityVisitor<IOneOneTagMapper>
{
	private TagMapperInstantiator()
	{
	}
	
	private static TagMapperInstantiator instance = null;
	
	public static TagMapperInstantiator getInstance()
	{
		if(instance == null)
			instance = new TagMapperInstantiator();
		
		return instance;
	}
	
	public IOneOneTagMapper instantiate(IEntity entity)
	{
		return entity.accept(this);
	}
	
	@Override
	public IOneOneTagMapper visit(SimpleClassTagMapperState state)
	{
		return new SimpleClassTagMapper(
				state.getResource(),
				new SimpleTagPattern(
						state.getTagPattern().getKey(), 
						state.getTagPattern().getValue()),
				state.describesOSMEntity());
	}

	@Override
	public IOneOneTagMapper visit(SimpleDataTypeTagMapperState state)
	{
		return new SimpleDataTypeTagMapper(
				state.getResource(),
				new SimpleTagPattern(
						state.getTagPattern().getKey(), 
						state.getTagPattern().getValue()),
				state.getDataType(),
				state.describesOSMEntity());
	}

	@Override
	public IOneOneTagMapper visit(SimpleObjectPropertyTagMapperState state)
	{
		return new SimpleObjectPropertyTagMapper(
				state.getResource(),
				state.getObject(),
				new SimpleTagPattern(
						state.getTagPattern().getKey(), 
						state.getTagPattern().getValue()),
				state.describesOSMEntity());
	}

	@Override
	public IOneOneTagMapper visit(SimpleTextTagMapperState state)
	{
		return new SimpleTextTagMapper(
				state.getResource(),
				new SimpleTagPattern(
						state.getTagPattern().getKey(), 
						state.getTagPattern().getValue()),
				state.getLanguageTag(),
				state.describesOSMEntity());
	}

	@Override
	public IOneOneTagMapper visit(RegexTextTagMapperState state)
	{
		return new RegexTextTagMapper(
				state.getProperty(),
				Pattern.compile(state.getKeyPattern()),
				state.getDescribesOSMEntity());
	}
}
