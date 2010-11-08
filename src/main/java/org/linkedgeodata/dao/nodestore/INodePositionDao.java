package org.linkedgeodata.dao.nodestore;

import java.awt.geom.Point2D;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

public interface INodePositionDao
{

	public abstract Map<Long, Point2D> lookup(Collection<Long> ids)
			throws SQLException;

	public abstract void updateOrInsert(Map<Long, Point2D> idToPosition)
			throws SQLException;

	public abstract void remove(Collection<Long> ids) throws SQLException;

}