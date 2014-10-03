package com.steven.catanserver;

import java.util.*;

public class Edge implements DataContainer.Keyable {
	
	private int id;
	private PlayerColor color = null;
	private transient EdgeData parent;
	private DataContainer.KeyedRelation<Intersection> neighboringIntersections = null;
	private DataContainer.KeyedRelation<Hex> neighboringHexes = null;
	
	Edge(int id, Collection<Hex> hexes, EdgeData parent) {
		this.parent = parent;
		for (Hex hex : hexes)
			if (hex != null)
				this.getNeighboringHexes().add(hex);
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
	
	private DataContainer.KeyedRelation<Hex> getNeighboringHexes() {
		assert (this.parent != null);
		if (this.neighboringHexes == null)
			this.neighboringHexes = new DataContainer.KeyedRelation<Hex>(this.parent.getBoard().getHexData());
		if (this.neighboringHexes.getRawData() == null)
			this.neighboringHexes.setup(this.parent.getBoard().getHexData());
		return this.neighboringHexes;
	}

	public int getId() {
		// TODO Auto-generated method stub
		return this.id;
	}
	
	Iterable<Hex> getHexes() {
		return this.getNeighboringHexes().getAll();
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
	
	public boolean canPlaceRoad() {
		if (this.color != null)
			return false;
		for (Hex hex : this.getNeighboringHexes().getAll())
			if (hex.getType() != HexType.OCEAN)
				return true;
		return false;
	}

	public void placeRoad(Player player) {
		assert (this.canPlaceRoad());
//		System.out.println("Placing road on edge " + this.getId() + " with intersection neighbors " + this.getNeighboringIntersections().ids);
		this.color = player.getPlayerColor();
	}
	
}
