package org.linkedgeodata.osm.osmosis.plugins;

import org.linkedgeodata.util.Diff;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;


public class RDFDiff
	extends Diff<Model>
{	
	public RDFDiff() {
		super(
				ModelFactory.createDefaultModel(),
				ModelFactory.createDefaultModel(),
				ModelFactory.createDefaultModel());
	}
	
	
	/**
	 * Basically adds a statement to the set of added items.
	 * However, if the statement is marked as removed, it is
	 * removed from the removal-set, but not added to added-set.
	 * 
	 * @param stmt
	 */
	public void add(Statement stmt) {
		if(getRemoved().contains(stmt)) {
			getRemoved().remove(stmt);
		} else {
			getRemoved().remove(stmt);
			getAdded().add(stmt);
		}
	}
	
	
	/**
	 * Adds an item to the set of removed items, unless an equal statement is
	 * contained in the added-set. In this case the statement is removed from the
	 * added set.
	 * 
	 * @param stmt
	 */
	public void remove(Statement stmt) {
		getAdded().remove(stmt);
		getRemoved().add(stmt);
	}
	
	public void add(Model model) {
		for(Statement stmt : model.listStatements().toList()) {
			add(stmt);
		}
	}
	
	public void remove(Model model) {
		for(Statement stmt : model.listStatements().toList()) {
			remove(stmt);
		}
	}
	
	public void clear() {
		getAdded().removeAll();
		getRemoved().removeAll();
	}

	/*
	public void add(Statement stmt) {
		getRemoved().remove(stmt);
		getAdded().add(stmt);
	}
	
	public void remove(Statement stmt) {
		getAdded().remove(stmt);
		getRemoved().add(stmt);
	}
	
	public void add(Model model) {
		getRemoved().remove(model);
		getAdded().add(model);
	}
	
	public void remove(Model model) {
		getAdded().remove(model);
		getRemoved().add(model);
	}
	
	public void clear() {
		getAdded().removeAll();
		getRemoved().removeAll();
	}*/
}
