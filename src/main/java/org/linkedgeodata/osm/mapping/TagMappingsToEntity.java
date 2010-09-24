package org.linkedgeodata.osm.mapping;

import org.linkedgeodata.osm.mapping.impl.IOneOneTagMapperVisitor;
import org.linkedgeodata.osm.mapping.impl.RegexTextTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleDataTypeTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleObjectPropertyTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleTextTagMapper;
import org.linkedgeodata.tagmapping.client.entity.AbstractTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.RegexTextTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleDataTypeTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleObjectPropertyTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleTagPattern;
import org.linkedgeodata.tagmapping.client.entity.SimpleTextTagMapperState;

public class TagMappingsToEntity
	implements IOneOneTagMapperVisitor<AbstractTagMapperState>
{
	@Override
	public AbstractTagMapperState accept(SimpleDataTypeTagMapper mapper)
	{
		return new SimpleDataTypeTagMapperState(mapper.getProperty(), new SimpleTagPattern(mapper.getTagPattern().getKey(), mapper.getTagPattern().getValue()), mapper.getDataType(), mapper.describesOSMEntity());
	}

	@Override
	public AbstractTagMapperState accept(SimpleTextTagMapper mapper)
	{
		return new SimpleTextTagMapperState(mapper.getProperty(), new SimpleTagPattern(mapper.getTagPattern().getKey(), mapper.getTagPattern().getValue()), mapper.getLanguageTag(), mapper.describesOSMEntity());
	}

	@Override
	public AbstractTagMapperState accept(SimpleObjectPropertyTagMapper mapper)
	{
		return  new SimpleObjectPropertyTagMapperState(mapper.getProperty(), mapper.getObject(), mapper.isObjectAsPrefix(), new SimpleTagPattern(mapper.getTagPattern().getKey(), mapper.getTagPattern().getValue()), mapper.describesOSMEntity());
	}

	@Override
	public AbstractTagMapperState accept(RegexTextTagMapper mapper)
	{
		return  new RegexTextTagMapperState(mapper.getProperty(), mapper.getKeyPattern().toString(), mapper.describesOSMEntity());
	}
}
