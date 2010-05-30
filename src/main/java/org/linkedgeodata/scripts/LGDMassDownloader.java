package org.linkedgeodata.scripts;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.Rectangle2D.Double;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.linkedgeodata.util.PrefetchIterator;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

interface IURLBuilder
{
	String getData(RectangularShape rect);
}

class URLBuilderImpl
	implements IURLBuilder
{
	private String baseURI;
	
	public URLBuilderImpl(String baseURI)
	{
		this.baseURI = baseURI;
	}

	@Override
	public String getData(RectangularShape rect)
	{
		return
			baseURI + "near/" + 
			rect.getMinY() + "-" + rect.getMaxY() + "," +
			rect.getMinX() + "-" + rect.getMaxX();
	}
}

interface ILGDDAO
{
	Model getData(RectangularShape rect, Model model);
}

class LGDDAOImpl
	implements ILGDDAO
{
	private IURLBuilder urlBuilder;
	
	public LGDDAOImpl(IURLBuilder urlBuilder)
	{
		this.urlBuilder = urlBuilder;
	}
	
	/**
	 * 
	 * 
	 * @param rect
	 * @param model The model to store the data in. If null, a default model
	 *        will be created.
	 * @return
	 */
	@Override
	public Model getData(RectangularShape rect, Model model)
	{
		return readModel(urlBuilder.getData(rect), model);
	}

	private Model readModel(String url, Model result)
	{
		if(result == null) {
			result = ModelFactory.createDefaultModel();
		}
		
		System.out.println("Reading model from: " + url);
		result.read(url);
		
		return result;
	}
}



class RectIterator
	implements Iterator<RectangularShape>
{
	private Iterator<RectangularShape> it;
	private RectangularShape current = null;
	
	public RectIterator(Iterator<RectangularShape> it)
	{
		this.it = it;
	}
	
	public IterableRect descend()
	{
		return split(0.5, 0.5);
	}
	
	public IterableRect split(double rx, double ry)
	{
		if(current == null) {
			throw new IllegalStateException();
		}

		RectangularShape rect = current;
		
		double cx = rect.getMinX() + rx * rect.getWidth();
		double cy = rect.getMinY() + ry * rect.getHeight();
		
		RectangularShape[] rs = {
				new Rectangle2D.Double(rect.getMinX(), rect.getMinY(), cx, cy),
				new Rectangle2D.Double(cx, rect.getMinY(), rect.getMaxX(), cy),
				new Rectangle2D.Double(rect.getMinX(), cy, cx, rect.getMaxY()),
				new Rectangle2D.Double(cx, cy, rect.getMaxX(), rect.getMaxY())
		};
		
		return new IterableRect(Arrays.asList(rs));
	}

	@Override
	public boolean hasNext()
	{
		return it.hasNext();
	}

	@Override
	public RectangularShape next()
	{
		current = it.next();
		
		return current;
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}
	
}


class IterableRect
{
	private Collection<RectangularShape> rects;
	
	public IterableRect(RectangularShape rect)
	{
		this.rects = Collections.singleton(rect);
	}
	
	public IterableRect(Collection<RectangularShape> rects)
	{
		this.rects = rects;
	}

	public RectIterator iterator()
	{
		return new RectIterator(rects.iterator());
	}
}


class RectIterator2
	implements Iterator<RectangularShape>
{
	private RectangularShape bounds;
	
	private double x;
	private double y;
	private double stepWidth;
	private double stepHeight;
	
	
	public RectIterator2(RectangularShape bounds, int chunksX, int chunksY)
	{
		this.bounds = bounds;
		
		x = bounds.getMinX();
		y = bounds.getMinY();
		
		stepWidth = bounds.getWidth() / (double)chunksX;
		stepHeight = bounds.getHeight() / (double)chunksY;
	}
	
	@Override
	public boolean hasNext()
	{
		//return (x < bounds.getMaxX() && y > bounds.getMaxY());
		return (x < bounds.getMaxX() && y < bounds.getMaxY());
	}

	@Override
	public RectangularShape next()
	{
		if(!hasNext()) {
			return null;
		}
		
		RectangularShape result =
			new Rectangle2D.Double(x, y, stepWidth, stepHeight);
		
		x += stepWidth;
		if(x >= bounds.getMaxX()) {
			x = bounds.getMinX();
			y += stepHeight;
		}
		
		return result;
	}
	
	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}	
}

/*
class QuadTileIterator<T>
	extends PrefetchIterator<T>
{
	@Override
	protected Iterator<T> prefetch()
		throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	// Descends into the children of the current node
	public void descend()
	{
		
	}
}
*/

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
