package com.steven.catanserver;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class EdgeData implements DataContainer<Edge> {

	private HashMap<Integer, Edge> edges = new HashMap<Integer, Edge>();
	private transient BoardModel board;
	
	EdgeData(BoardModel board) {
		this.board = board;
	}
	
	@Override
	public Edge getElement(Integer id) {
		return this.edges.get(id);
	}

	void addEdge(Edge edge) {
		edge.setParent(this);
		edges.put(edge.getId(), edge);
	}
	
	Edge getEdge(int id) {
		return this.edges.get(id);
	}
	
	Collection<Edge> getEdges() {
		return this.edges.values();
	}
	
	public void finalizeFromDB(BoardModel board) {
		for (Edge e : this.getEdges())
			e.setParent(this);
		this.board = board;
	}
	
	public DataContainer<Intersection> getIntersectionData() {
		return this.board.getIntersectionData();
	}
	
}
