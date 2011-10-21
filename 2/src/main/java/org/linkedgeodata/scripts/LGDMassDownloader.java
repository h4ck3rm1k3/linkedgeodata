package org.linkedgeodata.scripts;

import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.Iterator;

import org.linkedgeodata.access.ILGDDAO;
import org.linkedgeodata.access.IURLBuilder;
import org.linkedgeodata.access.LGDDAOImpl;
import org.linkedgeodata.access.RectIterator2;
import org.linkedgeodata.access.URLBuilderImpl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


public class LGDMassDownloader
{
	
	
	public static void main(String[] args)
	{
		IURLBuilder urlBuilder = new URLBuilderImpl("http://localhost:7000/triplify");
		ILGDDAO dao = new LGDDAOImpl(urlBuilder);
		
		Model model = ModelFactory.createDefaultModel();
		RectangularShape rect = new Rectangle2D.Double(13.0, 50.0, 1.0, 1.0);

		Iterator<RectangularShape> it = new RectIterator2(rect, 4, 2);
		
		while(it.hasNext()) {
			RectangularShape r = it.next();
			//System.out.println(r);
			
			dao.getData(r, model);
			
			System.out.printf("Model now contains %d statements\n", model.size());
		}
		
		
	}
}
