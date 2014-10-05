package com.steven.catanserver;

import java.util.*;
import java.util.Map.Entry;

public class EdgeData implements DataContainer<Edge> {

	private HashMap<Integer, Edge> edges = new HashMap<Integer, Edge>();
	private transient Board board;
	
	EdgeData(Board board) {
		this.board = board;
	}
	
	EdgeData(Board board, EdgeData toCopy) {
		this.board = board;
		this.edges = new HashMap<Integer, Edge>();
		for (Entry<Integer, Edge> e : toCopy.edges.entrySet())
			this.edges.put(e.getKey(), new Edge(e.getValue(), this));
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
	
	Board getBoard() {
		return this.board;
	}
	
}
