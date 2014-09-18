package com.steven.catanserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class IntersectionData {

	private HashMap<Integer, Intersection> intersections = new HashMap<Integer, Intersection>();
	private HashMap<Integer, Edge> edges = new HashMap<Integer, Edge>();
	private int radius;
	private transient BoardModel board;
	
	IntersectionData(int radius, BoardModel board) {
		this.radius = radius;
		this.board = board;
	}
	
	BoardModel getBoard() {
		return this.board;
	}
	
	void addIntersection(Intersection inter) {
		inter.setParent(this);
		intersections.put(inter.getId(), inter);
	}
	
	void addEdge(Edge edge) {
		edges.put(edge.getId(), edge);
	}
	
	Edge getEdge(int id) {
		return this.edges.get(id);
	}
	
	Iterable<Edge> getEdges() {
		return this.edges.values();
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
		for (Edge e : this.getEdges())
			e.setParent(this);
	}
	
}
