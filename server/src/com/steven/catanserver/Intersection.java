package com.steven.catanserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class Intersection {
	
	private int id;
	private ArrayList<Integer> hexIds = new ArrayList<Integer>(3);
	private ArrayList<Integer> edgeIds = new ArrayList<Integer>(3);
	private transient HashSet<Hex> hexes = null;
	private transient HashMap<Integer, Edge> edges = null;
	private Boolean isCity = false;
	private PlayerColor color = null;
	private transient Player player;
	private HarborType harborType;
	private transient HashMap<Integer, Intersection> neighborIntersections = null;
	private transient IntersectionData parent;
	
	Intersection(int id) {
		this.id = id;
	}
	
	void setParent(IntersectionData interd) {
		this.parent = interd;
	}
	
	public void setHarborType(HarborType ht) {
		this.harborType = ht;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void setHexes(Collection<Hex> hexes) {
		this.hexes = new HashSet<Hex>(hexes);
		for (Hex hex : this.hexes)
			if (hex != null)  
				// perfectly valid for this to be null along edge
				this.hexIds.add(hex.getId());
	}
	
	public Iterable<Hex> getHexes() {
		if (this.hexes == null) {
			this.hexes = new HashSet<Hex>(3);
			for (Integer i : this.hexIds) {
				BoardModel b = this.parent.getBoard();
				this.hexes.add(b.getHex(i));
			}
		}
		return this.hexes;
	}
	
	public void addEdge(Edge e) {
		this.getEdgeMap().put(e.getId(), e);
		this.edgeIds.add(e.getId());
		assert (this.edges.size() <= 3);
	}
	
	private HashMap<Integer, Edge> getEdgeMap() {
		if (this.edges == null) {
			this.edges = new HashMap<Integer, Edge>(3);
			for (Integer edgeId : this.edgeIds)
				this.edges.put(id, this.parent.getEdge(edgeId));
		}
		return this.edges;
	}
	
	public Iterable<Edge> getEdges() {
		return this.getEdgeMap().values();
	}
	
	public Boolean hasSettlement() {
		return this.color != null;
	}
	
	private HashMap<Integer, Intersection> getNeighborIntersections() {
		if (this.neighborIntersections == null) {
			this.neighborIntersections = new HashMap<Integer, Intersection>(3);
			for (Edge e : this.getEdges())
				for (Intersection i : e.getIntersections())
					if (i.getId() != this.getId())
						this.neighborIntersections.put(i.getId(), i);
		}
		return this.neighborIntersections;
	}
	
	public Boolean canPlaceSettlement() {
		for(Intersection inter : this.getNeighborIntersections().values())
			if (inter.hasSettlement())
				return false;
		return true;
	}
	
	public void placeSettlement(Player p) {
		this.color = p.getPlayerColor();
	}

}
