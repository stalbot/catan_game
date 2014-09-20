package com.steven.catanserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class Intersection implements DataContainer.Keyable {
	
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
	private DataContainer.KeyedRelation<Edge> neighboringEdges = null;
	
	Intersection(int id) {
		this.id = id;
	}
	
	private DataContainer.KeyedRelation<Edge> getNeighboringEdges() {
//		if (this.parent == null)
//			return null;
		assert (this.parent != null);
		if (this.neighboringEdges == null)
			this.neighboringEdges = new DataContainer.KeyedRelation<Edge>(this.parent.getEdgeData());
		if (this.neighboringEdges.getRawData() == null)
			// if this was instantiated from GSON
			this.neighboringEdges.setup(this.parent.getEdgeData());
		return this.neighboringEdges;
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
		this.getNeighboringEdges().add(e);
	}
	
	public Iterable<Edge> getEdges() {
		return this.getNeighboringEdges().getAll();
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
		if (this.hasSettlement())
			return false;
//		System.out.println("Neighbors " + this.getNeighborIntersections().values());
//		System.out.println("Edges " + this.getEdges());
//		System.out.println("Edge ids" + this.edgeIds);
		for(Intersection inter : this.getNeighborIntersections().values())
			if (inter.hasSettlement())
				return false;
		return true;
	}
	
	public void placeSettlement(Player p) {
		System.out.println("Placing settlement on intersection " + this.getId() + " with edge neighbors " + this.getNeighboringEdges().ids);
		this.color = p.getPlayerColor();
	}

}
