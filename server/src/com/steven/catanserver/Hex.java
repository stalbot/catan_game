package com.steven.catanserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Hex {
	
	public static final int[][] directions = new int[][] {
		// 2nd index (z values) can just be ignored
		{-1, 1, 0}, {0, 1, -1}, {1, 0, -1}, {1, -1, 0}, {0, -1, 1}, {-1, 0, 1},
	};

	private Integer rollNumber;
	private HexType hexType;
	private int xPosition;
	private int yPosition;
	private int id;
	private transient HexData parent;
	
	private ArrayList<Integer> edgeIds = new ArrayList<Integer>(6);
	private ArrayList<Integer> interIds = new ArrayList<Integer>(6);
	
	private HarborType harborType = null;
	private ArrayList<Integer> harborInterIds = null;
	
	private transient ArrayList<Edge> edges = new ArrayList<Edge>(6);
	private transient ArrayList<Intersection> inters = new ArrayList<Intersection>(6); 
	
	Hex(int id, Integer rollNumber, HexType hexType, int xPosition, int yPosition) {
		this.rollNumber = rollNumber;
		this.hexType = hexType;
		this.xPosition = xPosition;
		this.yPosition = yPosition;
		this.id = id;
		for (int i=0; i<6; i++) {
			edges.add(null); inters.add(null);
		}
	}
	
	int getId() {
		return this.id;
	}
	
	void addEdge(Edge edge, int num) {
		this.edgeIds.add(num, edge.getId());
		this.edges.add(num, edge);
	}
	
	void addIntersection(Intersection inter, int num) {
		this.interIds.add(num, inter.getId());
		this.inters.add(num, inter);
	}
	
	HarborType getHarborType() {
		return this.harborType;
	}
	
	void setHarborType(HarborType ht) {
		this.harborType = ht;
	}
	
	int getX() {
		return this.xPosition;
	}
	
	int getY() {
		return this.yPosition;
	}
	
	int getZ() {
		return -(this.xPosition + this.yPosition);
	}
	
	int getDirectionToCenter() {
		// Finds the "closest" hex to the center as defined by number of edges needed
		// to get to the center hex. (but not implemented that way)
		// Currently used for harbor set up... maybe try to make faster if needed in more performant section 
		int[] startVals = {this.getX(), this.getY(), this.getZ()};
		ArrayList<Integer> newVals = new ArrayList<Integer>(3);
		ArrayList<Integer> bestVals = new ArrayList<Integer>(3);
		for (int i=0; i<3; i++) {
			bestVals.add(Math.abs(startVals[i]));
		}
		Collections.sort(bestVals, Collections.reverseOrder());
		int ret = -1;
		for (int k=0; k<6; k++) {
			newVals.clear();
			for (int i=0; i<3; i++) 
				newVals.add(i, Math.abs(startVals[i] + Hex.directions[k][i]));
			Collections.sort(newVals, Collections.reverseOrder());
			
			Boolean absSmaller = false;
			// We want to choose the point with the smallest largest numbers, 
			// e.g. favor 2,1,1 over 2,2,0
			for (int i=0; i<3; i++) 
				if (newVals.get(i) > bestVals.get(i))
					break;
				else if (newVals.get(i) < bestVals.get(i))
					absSmaller = true;
			if (absSmaller) {
				// Save an allocation! (stupid?)
				ArrayList<Integer> temp = bestVals;
				bestVals = newVals;
				newVals = temp;
				
				ret = k;
			}
		}
//			System.out.println("xPos: " + this.xPosition + ", yPos: " + this.yPosition + 
//					" dirs: " + Hex.directions[ret][0] + "," + Hex.directions[ret][1]);
    	// Should never ever happen
		assert(false);
		if (ret < 0)
			System.out.println("Bad stuff in getDirectionToCenter()");
		return ret;
	}
	
	Hex getNeighbor(int i) {
		// TODO: check i < 6?
		int x = directions[i][0], y = directions[i][1];
		return this.parent.getByXYCoords(this.xPosition + x, this.yPosition + y);
	}
	
	ArrayList<Hex> getNeighbors() {
		ArrayList<Hex> neighbors = new ArrayList<Hex>(6);
		for (int i=0; i<6; i++) {
				neighbors.add(this.getNeighbor(i));
		}
		return neighbors;
	}
	
	HexData.IJCoords getIJCoords() {
		return this.parent.convertFromXY(this.xPosition, this.yPosition);
	}
	
	void setParent(HexData hd) {
		this.parent = hd;
	}
	
	public Intersection getIntersection(int index) {
		return this.inters.get(index);
	}

	public List<Intersection> getIntersections() {
		return this.inters;
	}
	
	public Edge getEdge(int index) {
		return this.edges.get(index);
	}

	public List<Edge> getEdges() {
		return this.edges;
	}
	
	public int getRollNumber() {
		return this.rollNumber;
	}
	
	public int getRollProbability() {
		// returns roll probability out of 36
		return 6 - Math.abs(7 - this.rollNumber);
	}
	
}
