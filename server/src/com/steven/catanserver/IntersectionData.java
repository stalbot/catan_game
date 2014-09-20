package com.steven.catanserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class IntersectionData implements DataContainer<Intersection> {

	private HashMap<Integer, Intersection> intersections = new HashMap<Integer, Intersection>();
	private transient BoardModel board;
	
	IntersectionData(BoardModel board) {
		this.board = board;
	}
	
	BoardModel getBoard() {
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
