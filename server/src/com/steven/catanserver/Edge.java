package com.steven.catanserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class Edge implements DataContainer.Keyable {
	
	private transient HashSet<Hex> hexes;
	
	private transient HashMap<Integer, Intersection> intersections = null;
	
	private int id;
	private PlayerColor color = null;
	private transient EdgeData parent;
	private DataContainer.KeyedRelation<Intersection> neighboringIntersections = 
			(this.parent != null && this.neighboringIntersections == null) ?
					new DataContainer.KeyedRelation<Intersection>(this.parent.getIntersectionData()) : 
						null;
	
	Edge(int id, Collection<Hex> hexes) {
		this.hexes = new HashSet<Hex>(hexes);
		this.id = id;
	}
	
	private DataContainer.KeyedRelation<Intersection> getNeighboringIntersections() {
		assert (this.parent != null);
		if (this.neighboringIntersections == null)
			this.neighboringIntersections = new DataContainer.KeyedRelation<Intersection>(this.parent.getIntersectionData());
		if (this.neighboringIntersections.getRawData() == null)
			this.neighboringIntersections.setup(this.parent.getIntersectionData());
		return this.neighboringIntersections;
	}

	public int getId() {
		// TODO Auto-generated method stub
		return this.id;
	}
	
	Iterable<Intersection> getIntersections() {
		return this.getNeighboringIntersections().getAll();
	}
	
	void addIntersection(Intersection inter) {
		this.getNeighboringIntersections().add(inter);
	}
	
	void setParent(EdgeData edged) {
		this.parent = edged;
	}

	public void placeRoad(Player player) {
		System.out.println("Placing road on edge " + this.getId() + " with intersection neighbors " + this.getNeighboringIntersections().ids);
		this.color = player.getPlayerColor();
	}
	
}
