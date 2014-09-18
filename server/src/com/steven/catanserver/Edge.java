package com.steven.catanserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class Edge {
	
	private transient HashSet<Hex> hexes;
	
	private transient HashMap<Integer, Intersection> intersections = null;
	private ArrayList<Integer> intersectionIds = new ArrayList<Integer>(2);
	
	private int id;
	private PlayerColor color = null;
	private transient IntersectionData parent;
	
	Edge(int id, Collection<Hex> hexes) {
		this.hexes = new HashSet<Hex>(hexes);
		this.id = id;
	}

	public Integer getId() {
		// TODO Auto-generated method stub
		return this.id;
	}
	
	HashMap<Integer, Intersection> getIntersectionMap() {
		if (this.intersections == null) {
			this.intersections = new HashMap<Integer, Intersection>(2);
			for (Integer id : this.intersectionIds)
				this.intersections.put(id, this.parent.getIntersection(id));
		}
		return this.intersections;
	}
	
	Iterable<Intersection> getIntersections() {
		return this.getIntersectionMap().values();
	}
	
	void addIntersection(Intersection inter) {
		this.getIntersectionMap().put(inter.getId(), inter);
		assert (this.intersections.size() <= 2);
	}
	
	void setParent(IntersectionData interd) {
		this.parent = interd;
	}
	
}
