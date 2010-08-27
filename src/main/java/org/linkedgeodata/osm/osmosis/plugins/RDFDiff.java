package org.linkedgeodata.osm.osmosis.plugins;

import org.linkedgeodata.util.Diff;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


public class RDFDiff
	extends Diff<Model>
{	
	public RDFDiff() {
		super(
				ModelFactory.createDefaultModel(),
				ModelFactory.createDefaultModel(),
				ModelFactory.createDefaultModel());
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
	}
}
