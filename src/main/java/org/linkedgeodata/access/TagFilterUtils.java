package org.linkedgeodata.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.linkedgeodata.dao.OntologyDAO;
import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.linkedgeodata.osm.mapping.TagMapperInstantiator;
import org.linkedgeodata.osm.mapping.TagMappingDB;
import org.linkedgeodata.osm.mapping.impl.ISimpleOneOneTagMapper;
import org.linkedgeodata.osm.mapping.impl.RegexTextTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleTextTagMapper;
import org.linkedgeodata.tagmapping.client.entity.RegexTextTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.SimpleTextTagMapperState;
import org.linkedgeodata.util.SQLUtil;
import org.linkedgeodata.util.StringUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.vocabulary.RDFS;


/**
 * A collection of methods for building filters on the tags
 * @author raven
 *
 */
public class TagFilterUtils
{
	private static final Logger logger = Logger.getLogger(TagFilterUtils.class);
	
	private String lgdO = "http://linkedgeodata.org/ontology/";
	
	private OntologyDAO ontologyDAO;
	
	
	
	public TagFilterUtils(OntologyDAO ontologyDAO)
	{
		this.ontologyDAO = ontologyDAO; 
	}
	
	public enum MatchMode {
		EQUALS,
		LIKE,
		REGEX
	}
	
	public String restrictByText(String property, String label, String language, MatchMode matchMode, String tableAlias)
		throws Exception
	{	
		//String uri = RDFS.label.toString();
	
		//MultiMap<Tag, IOneOneTagMapper> matches = ontologyDAO.reverseMapResource(uri);
	
		Session session = TagMappingDB.getSession();
		Transaction tx = session.beginTransaction();
		
		//MultiMap<Tag, IOneOneTagMapper> matches = new MultiHashMap<Tag, IOneOneTagMapper>();
		Set<String> keys = new HashSet<String>();
		
		Criteria criteria = session.createCriteria(SimpleTextTagMapperState.class)
			.add(Restrictions.eq("property", property));
		
		if(language != null)
			criteria.add(Restrictions.eq("languageTag", language));

		for(Object o : criteria.list()) {
			SimpleTextTagMapperState state = (SimpleTextTagMapperState)o;
			SimpleTextTagMapper mapper = TagMapperInstantiator.getInstance().visit(state);

			if(language != null && !language.equalsIgnoreCase(mapper.getLanguageTag()))
				continue;
			
			keys.add(state.getTagPattern().getKey());
			
			//matches.put(new Tag(state.getTagPattern().getKey(), state.getTagPattern().getValue()),
			//		mapper);
		}

		logger.debug("Found matches: " + keys);
		

		// Deal with regex mappings
		criteria = session.createCriteria(RegexTextTagMapperState.class)
			.add(Restrictions.eq("property", property));
		for(Object o : criteria.list()) {
			RegexTextTagMapperState state = (RegexTextTagMapperState)o;
			RegexTextTagMapper mapper = TagMapperInstantiator.getInstance().visit(state);
			
			Pattern keyPattern = mapper.getKeyPattern();
			
			List<String> ks = ontologyDAO.getTagDAO().findKeys(keyPattern);
			for(String k : ks) {
				String langTag = mapper.matchLangTag(k);
				
				if(langTag == null)
					continue;
				
				if(language != null && !language.equalsIgnoreCase(langTag))
					continue;
				
				keys.add(k);
			}
		}
		
		tx.commit();
		

		tableAlias = (tableAlias == null)
			? ""
			: tableAlias + ".";
	
		String tabK = tableAlias + "k";
		String tabV = tableAlias + "v";
		String quoteV = SQLUtil.quotePostgres(label);

		String op = "";
		switch(matchMode) {
			case EQUALS: op = " = "; break;
			case LIKE: op = " LIKE "; break;
			case REGEX: op = " ~* "; break;
			default: throw new NotImplementedException();
		}
		
		List<String> constraints = new ArrayList<String>();
		for(String key : keys) {

			String quoteK = SQLUtil.quotePostgres(key);
			
			String constraint
				= "(" + tabK + " = " + quoteK + " AND "
				+ tabV + op + quoteV + ")";

			if(!constraint.isEmpty())
				constraints.add(constraint);
		}
	
	
		String result = StringUtil.implode(" AND ", constraints);
		if(result.isEmpty())
			result = "FALSE";
		
		return result;
	}
	
	
	/**
	 * Creates a filter which only matches direct instances of the specified
	 * class
	 * 
	 * @param className
	 * @return
	 * @throws Exception 
	 */
	public String restrictByObject(String propertyURI, String objectURI, String tableAlias)
		throws Exception
	{
		tableAlias = (tableAlias == null)
			? ""
			: tableAlias + ".";
		
		String tabK = tableAlias + "k";
		String tabV = tableAlias + "v";
		
		
		MultiMap<Tag, IOneOneTagMapper> matches = ontologyDAO.reverseMapResource(propertyURI, objectURI);
		
		List<String> constraints = new ArrayList<String>();
		
		for(Map.Entry<Tag, Collection<IOneOneTagMapper>> entry : matches.entrySet()) {

			Tag tag = entry.getKey();
			String quoteK = SQLUtil.quotePostgres(tag.getKey());
			String quoteV = SQLUtil.quotePostgres(tag.getValue());
			
			for(IOneOneTagMapper mapper : entry.getValue()) {
				String constraint = "";
				
				if(!(mapper instanceof ISimpleOneOneTagMapper))
					continue;
				
				if(tag.getKey() != null && tag.getValue() != null) {
					constraint
						= "(" + tabK + ", " + tabV + ") = "
						+ "(" + quoteK + ", " + quoteV + ")";
				}
				else {
					if(tag.getKey() != null)
						constraint = tabK + " = " + quoteK;
					else if(tag.getValue() != null) 
						constraint = tabV + " = " + quoteV;
				}

				if(!constraint.isEmpty())
					constraints.add(constraint);
			}
		}
	
	
		String result = StringUtil.implode(" AND ", constraints);
		
		if(result.isEmpty())
			result = "FALSE";
		
		return result;
	}
}
