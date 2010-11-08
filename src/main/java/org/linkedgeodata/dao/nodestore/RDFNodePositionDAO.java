package org.linkedgeodata.dao.nodestore;

import java.awt.geom.Point2D;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.linkedgeodata.core.ILGDVocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class RDFNodePositionDAO
{
	private INodePositionDao delegate;
	private INodeMapper nodeMapper;
	private ILGDVocab vocab;
	
	private static Pattern pattern = Pattern.compile("node[^0-9]*(\\d+)");

	public static Long resourceToId(Resource res)
	{
		Matcher matcher = pattern.matcher(res.toString());
		if(!matcher.find())
			return null;
		
		return Long.parseLong(matcher.group(1));
	}

	public RDFNodePositionDAO(INodePositionDao delegate, ILGDVocab vocab, INodeMapper nodeMapper)
	{
		this.delegate = delegate;
		this.vocab = vocab;
		this.nodeMapper = nodeMapper;
	}

	
	private Collection<Long> getIds(Collection<Resource> resources)
	{
		Set<Long> result = new HashSet<Long>();
		for(Resource resource : resources) {
			Long id = resourceToId(resource);
			
			if(id != null)
				result.add(id);
		}
		
		return result;
	}
	
	
	public void insert(Model model)
		throws SQLException
	{
		Map<Long, Point2D> map = nodeMapper.extract(model);
		delegate.updateOrInsert(map);
	}
	
	public void remove(Model model)
		throws SQLException
	{
		remove(model.listSubjects().toSet());
	}
	
	public void remove(Collection<Resource> resources)
		throws SQLException
	{
		Collection<Long> ids = getIds(resources);
		
		delegate.remove(ids);
	}
	
	public void lookup(Collection<Resource> resources, Model out)
		throws SQLException
	{
		Collection<Long> ids = getIds(resources);

		Map<Long, Point2D> tmp = delegate.lookup(ids);
		
		//Map<Resource, Point2D> points = new HashMap<Resource, Point2D>();
		nodeMapper.transform(tmp, out);
	}

	public Map<Resource, Point2D> lookup(Collection<Resource> resources)
		throws SQLException
	{
		Collection<Long> ids = getIds(resources);

		Map<Long, Point2D> tmp = delegate.lookup(ids);
		
		
		Map<Resource, Point2D> result = new HashMap<Resource, Point2D>();
		for(Map.Entry<Long, Point2D> entry : tmp.entrySet()) {
			result.put(vocab.createNIRNodeURI(entry.getKey()), entry.getValue()); 
		}
		
		
		return result;
	}
}

