package org.linkedgeodata.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.commons.collections.MapUtils;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.core.QuadPattern;
import com.hp.hpl.jena.sparql.core.Var;




public class QuadUtils
{
	
	/**
	 * Substitutes the keys in the map
	 * 
	 * @param <K>
	 * @param <V>
	 * @param original
	 * @param map
	 * @return
	 */
	public static <K, V> Map<K, V> copySubstitute(Map<K, V> original, Map<K, K> map)
	{
		Map<K, V> result = new HashMap<K, V>();
		for(Entry<K, V> entry : original.entrySet()) {
			result.put(MapUtils.getOrElse(map, entry.getKey(), entry.getKey()), entry.getValue());
		}
		
		return result;
	}
	
	
	
	public static Quad copySubstitute(Quad quad, Map<Node, Node> map)
	{
		return new Quad(
				MapUtils.getOrElse(map, quad.getGraph(), quad.getGraph()),
				MapUtils.getOrElse(map, quad.getSubject(), quad.getSubject()),
				MapUtils.getOrElse(map, quad.getPredicate(), quad.getPredicate()),
				MapUtils.getOrElse(map, quad.getObject(), quad.getObject()));
	}


	/*
	public static QuadPattern copySubstitute(QuadPattern quadPattern, Binding map)
	{
		map.ge
	}*/
	
	public static QuadPattern copySubstitute(QuadPattern quadPattern, Map<Node, Node> map)
	{
		QuadPattern result = new QuadPattern();
		for(Quad quad : quadPattern) {
			result.add(copySubstitute(quad, map));
		}
		
		return result;
	}
	
	
	
	
	
	public static Quad listToQuad(List<Node> nodes) {
		return new Quad(nodes.get(0), nodes.get(1), nodes.get(2), nodes.get(3));
	}
	
	public static List<Node> quadToList(Quad quad)
	{
		List<Node> result = new ArrayList<Node>();
		result.add(quad.getGraph());
		result.add(quad.getSubject());
		result.add(quad.getPredicate());
		result.add(quad.getObject());
	
		return result;
	}

	public static Set<Var> getVarsMentioned(QuadPattern quadPattern)
	{
		Set<Var> result = new HashSet<Var>();
		for(Quad quad : quadPattern) {
			result.addAll(getVarsMentioned(quad));
		}
		
		return result;
	}
	
	
	public static Set<Var> getVarsMentioned(Quad quad)
	{
		return PatternUtils.getVarsMentioned(QuadUtils.quadToList(quad));
	}

	
	public static Map<Node, Node> getVarMapping(Quad a, Quad b)
	{
		List<Node> nAs = quadToList(a);
		List<Node> nBs = quadToList(b);
		
		Map<Node, Node> result = new HashMap<Node, Node>();
		for(int i = 0; i < 4; ++i) {
			Node nA = nAs.get(i);
			Node nB = nBs.get(i);
			
			if(nA.isVariable()) {
				Map<Node, Node> newEntry = Collections.singletonMap(nA, nB);		
				
				//MapUtils.isCompatible(result, newEntry); 
				
				result.putAll(newEntry);
			} else {
				if(!nA.equals(nB)) {
					return null;
				}
			}
		}
		
		return result;
	}

}
