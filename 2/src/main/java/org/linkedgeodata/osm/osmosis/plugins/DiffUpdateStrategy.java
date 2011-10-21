package org.linkedgeodata.osm.osmosis.plugins;


/*
 * 
 * 
Whenever triples get added or removed we need to keep track of the overall
changes.

DiffContainer.add(triples)
DiffContainer.remove(triples)
...

Diff.writeDelta();
Diff.clear();



public class DiffUpdateStrategy
{
	private Date currentDate;
	private long diffInterval;
	
	private RDFDiff diff = new RDFDiff();

	@Override
	public void update(Model model)
	{
		
	}

	@Override
	public void complete()
	{
		// TODO Auto-generated method stub
		
	}

}
*/