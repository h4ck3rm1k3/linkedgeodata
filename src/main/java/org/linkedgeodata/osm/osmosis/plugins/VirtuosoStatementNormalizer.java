package org.linkedgeodata.osm.osmosis.plugins;

import org.linkedgeodata.util.ITransformer;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Sometimes when sending triples to a virtuoso, they implicitely become
 * transformed. Therefore the set of triples retrieved from a virtuoso store
 * may differ from the set being inserted. 
 * 
 * Applies the following transformations:
 * 
 * "true"^^xsd:boolean -> 1^^xsd:integer
 * "false"^^xsd:boolean -> 0^^xsd:integer
 * 
 * 123.0^^xsd:float -> 123^^xsd:integer
 * 
 * @author raven
 *
 */
public class VirtuosoStatementNormalizer
	implements ITransformer<Model, Model>
{
	private TypeMapper tm = TypeMapper.getInstance();
	
	private RDFDatatype intDataType = tm.getSafeTypeByName(XSD.integer.toString());
	//private RDFDatatype floatDataType = tm.getSafeTypeByName(XSD.float.toString());

	private static final float FLOAT_EPS = 0.00001f;
	
	@Override
	public Model transform(Model out, Model in)
	{
		StmtIterator it = in.listStatements();
		while(it.hasNext()) {
			transformStatement(it.next(), out);
		}
		
		return out;
	}
	
	private void transformStatement(Statement stmt, Model out)
	{
		if(!stmt.getObject().isLiteral()) {
			out.add(stmt);
			return;
		}
			
		
		Literal literal = stmt.getLiteral();

		if(XSD.xboolean.getURI().equals(literal.getDatatypeURI())) {
			int value = (literal.getBoolean() == true) ? 1 : 0;
			
			out.add(stmt.getSubject(), stmt.getPredicate(), Integer.toString(value), intDataType);
		}
		else if(XSD.xint.getURI().equals(literal.getDatatypeURI()) || XSD.integer.getURI().equals(literal.getDatatypeURI())) {
			long value = literal.getLong();
			
			out.add(stmt.getSubject(), stmt.getPredicate(), Long.toString(value), intDataType);			
		}
		else if(XSD.xfloat.getURI().equals(literal.getDatatypeURI())) {
			float value = literal.getFloat();
			
			long ref = Math.round(value);
			
			if(Math.abs(value - ref) < FLOAT_EPS) {
				out.add(stmt.getSubject(), stmt.getPredicate(), Long.toString(ref), intDataType);
			}	
		}
		else {
			out.add(stmt);
		}
		/*
		else if(XSD.xdouble.getURI().equals(literal.getDatatypeURI()) {
			double value = literal.getDouble();
			
			long ref = Math.round(value);
			
			if(Math.abs(value - ref) < FLOAT_EPS))) {
				out.add(stmt.getSubject(), stmt.getPredicate(), Float.toString(ref), intDataType);
			}	
		}*/
	}

	@Override
	public Model transform(Model in)
	{
		Model result = ModelFactory.createDefaultModel();
		
		transform(result, in);
		
		return result;
	}
}

