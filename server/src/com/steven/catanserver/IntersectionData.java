package com.steven.catanserver;

import java.util.*;
import java.util.Map.Entry;

public class IntersectionData implements DataContainer<Intersection> {

	private HashMap<Integer, Intersection> intersections = new HashMap<Integer, Intersection>();
	private transient Board board;
	
	IntersectionData(Board board) {
		this.board = board;
	}
	
	IntersectionData(Board board, IntersectionData toCopy) {
		this.board = board;
		this.intersections = new HashMap<Integer, Intersection>();
		for (Entry<Integer, Intersection> e : toCopy.intersections.entrySet())
			this.intersections.put(e.getKey(), new Intersection(e.getValue(), this));
	}
	
	Board getBoard() {
		return this.board;
	}
	
	void addIntersection(Intersection inter) {
		inter.setParent(this);
		intersections.put(inter.getId(), inter);
	}
	
	Iterable<Intersection> getIntersections() {
		return this.intersections.values();
	}
	
	Intersection getIntersection(int id) {
		return this.intersections.get(id);
	}

	public void finalizeFromDB(BoardModel board) {
		this.board = board;
		for (Intersection i : this.getIntersections())
			i.setParent(this);
	}

	@Override
	public Intersection getElement(Integer id) {
		return this.intersections.get(id);
	}
	
	public DataContainer<Edge> getEdgeData() {
		return this.board.getEdgeData();
	}
	
}
