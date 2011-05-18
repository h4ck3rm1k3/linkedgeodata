package org.linkedgeodata.evaluation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.algebra.op.OpGroup;
import com.hp.hpl.jena.sparql.algebra.op.OpJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpLeftJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpMinus;
import com.hp.hpl.jena.sparql.algebra.op.OpOrder;
import com.hp.hpl.jena.sparql.algebra.op.OpQuadPattern;
import com.hp.hpl.jena.sparql.algebra.op.OpSequence;
import com.hp.hpl.jena.sparql.algebra.op.OpService;
import com.hp.hpl.jena.sparql.algebra.op.OpSlice;
import com.hp.hpl.jena.sparql.algebra.op.OpTable;
import com.hp.hpl.jena.sparql.algebra.op.OpUnion;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.core.Var;

public class PatternUtils
{

	public static Collection<Quad> collectQuads(Op op) {
		return collectQuads(op, new HashSet<Quad>());
	}

	public static Collection<Quad> collectQuads(Op op, Collection<Quad> result) {
		if(op instanceof OpLeftJoin) {
			OpLeftJoin x = (OpLeftJoin)op;
			collectQuads(x.getLeft(), result);
			collectQuads(x.getRight(), result);
		} else if(op instanceof OpFilter) {
			OpFilter x = (OpFilter)op;
			collectQuads(x.getSubOp(), result);
		} else if(op instanceof OpJoin) {
			OpJoin x = (OpJoin)op;
			
			collectQuads(x.getLeft(), result);
			collectQuads(x.getRight(), result);
		} else if(op instanceof OpUnion) {
			//System.out.println("Warning: Collecting expressions from unions. Since the same vars may appear within different (parts of) unions, it may be ambiguous to which part the expression refers.");
	
			OpUnion x = (OpUnion)op;
	
			collectQuads(x.getLeft(), result);
			collectQuads(x.getRight(), result);
		} else if(op instanceof OpQuadPattern) {
			OpQuadPattern x = (OpQuadPattern)op;
			result.addAll(x.getPattern().getList());			
		} else if(op instanceof OpSequence) {
			OpSequence x = (OpSequence)op;
			for(Op element : x.getElements()) {
				collectQuads(element, result);
			}			
		} else if(op instanceof OpTable) {
			OpTable x = (OpTable)op;
		} else if(op instanceof OpOrder) {
			OpOrder x = (OpOrder)op;
			collectQuads(x.getSubOp(), result);
		} else if(op instanceof OpMinus) {
			OpMinus x = (OpMinus)op;
			
			collectQuads(x.getLeft(), result);
			collectQuads(x.getRight(), result);
		} else if(op instanceof OpGroup) {
			OpGroup x = (OpGroup)op;
			
			collectQuads(x.getSubOp(), result);
		} else if(op instanceof OpSlice) {
			OpSlice x = (OpSlice)op;
			
			collectQuads(x.getSubOp(), result);
		} else if(op instanceof OpService) {
			OpService x = (OpService)op;
			
			System.err.println("Ignoring service op");
			
			//collectQuads(x.getSubOp(), result);
		} else {
			throw new NotImplementedException("Encountered class: " + op);
		}
		
		return result;
	}

	
	public static Set<Var> getVarsMentioned(Iterable<Node> nodes)
	{
		Set<Var> result = new HashSet<Var>();
		for (Node node : nodes) {
			if (node.isVariable()) {
				result.add((Var)node);
			}
		}
	
		return result;
	}
	
	// Replaced by getVarsMentioned
	@Deprecated
	public static Set<Node> getVariables(Iterable<Node> nodes)
	{
		Set<Node> result = new HashSet<Node>();
		for (Node node : nodes) {
			if (node.isVariable()) {
				result.add(node);
			}
		}
	
		return result;
	}

	// Replaced by getVarsMentioned
	@Deprecated
	public static Set<Node> getVariables(Quad quad)
	{
		return getVariables(QuadUtils.quadToList(quad));
	}

}
