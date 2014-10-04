package com.steven.catanserver;

import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public final class HexData implements DataContainer<Hex> {
	
	public static final HashMap<HexType, Integer> defaultHexCounts = new HashMap<HexType, Integer>();
	public static final HashMap<Integer, Integer> defaultNumCounts = new HashMap<Integer, Integer>();
	public static final HashMap<HarborType, Integer> defaultHarborCounts = new HashMap<HarborType, Integer>();
	public static final int defaultRadius = 3;  // see note below about radiuses
	
	static {
		defaultHexCounts.put(HexType.DESERT, 1);
		defaultHexCounts.put(HexType.MOUNTAIN, 3);
		defaultHexCounts.put(HexType.HILL, 3);
		defaultHexCounts.put(HexType.FOREST, 4);
		defaultHexCounts.put(HexType.PASTURE, 4);
		defaultHexCounts.put(HexType.FIELD, 4);
		
		for (int i=0; i<HarborType.values().length; i++) {
			HarborType ht = HarborType.values()[i];
			if (ht != HarborType.GENERIC)
				defaultHarborCounts.put(ht, 1);
		}
		defaultHarborCounts.put(HarborType.GENERIC, 4);
		
		for (int i=2; i<=12; i++) {
			if (i == 7)
				continue;
			if (Math.abs(7 - i) == 5)
				defaultNumCounts.put(i, 1);
			else
				defaultNumCounts.put(i, 2);
		}
	}
	
	public static HexData generateHexes(Board board) {
		return generateHexes(defaultHexCounts, defaultNumCounts, defaultRadius, board);
	}

	public static HexData generateHexes(
			HashMap<HexType, Integer> hexcounts,
			HashMap<Integer, Integer> numcounts,
			int radius, 
			Board board) {
		LinkedList<HexType> hexTypes = new LinkedList<HexType>();
		for (Entry<HexType, Integer> e : hexcounts.entrySet())
			for (int i=0; i<e.getValue(); i++)
				hexTypes.add(e.getKey());
		Collections.shuffle(hexTypes);
		
		LinkedList<Integer> nums = new LinkedList<Integer>();
		for (Entry<Integer, Integer> e : numcounts.entrySet())
			for (int i=0; i<e.getValue(); i++)
				nums.add(e.getKey());
		Collections.shuffle(nums);
		
		HexData hexes = new HexData(radius, board);
		System.out.println(String.format("%d TOTAL hex Types", hexTypes.size()));
		
		for (int x=-radius; x<=radius; x++) {
			for (int y=-radius; y<=radius; y++) {
				// Z constrained by x and y
				int z = -(x + y);
				if (Math.abs(z) > radius)  // These points aren't actually in our board
					continue;
				// With whatever radius we are at, at least one of the coords will be that far out.
				int r = Math.max(Math.abs(z), Math.max(Math.abs(x), Math.abs(y)));
				int id = hexes.hashXYCoords(x, y);
				// The outer edge is all Ocean, everything else is land
//				System.out.println(String.format("%d %d %d %s", x, y, r, nums));
				HexType hType = (r != radius) ? hexTypes.remove() : HexType.OCEAN;
				Integer rollNum = (hType.hasRollNumber()) ? nums.remove() : null;
				hexes.addHex(new Hex(id, rollNum, hType, x, y));
			}
		}
		
		hexes.setupHarbors();
		
		return hexes;
	}


	private HashMap<Integer, Hex> hexes = new HashMap<Integer, Hex>();
	private int radius;
	private int maxHeight = 0;
	private int maxWidth = 0;
	private int minHeight = 0;
	private int minWidth = 0;
	private int robberHexId;
	private transient Board board;
	
	public Board getBoard() {
		return board;
	}
	
	public Hex getRobberHex() {
		return this.hexes.get(robberHexId);
	}
	
	public void setRobberHex(Hex hex) {
		this.robberHexId = hex.getId();
	}
	
	private HexData(int radius, Board board2) {
		this.radius = radius;
		this.board = board2;
	}
	
	public int hashXYCoords(int x, int y) {
		return x * this.radius * this.radius + y;
	}
	
	public Hex getById(int id) {
		return this.hexes.get(id);
	}
	
	public Hex getByXYCoords(int x, int y) {
		return this.hexes.get(this.hashXYCoords(x, y));
	}
	
	public Hex getByXYZCoords(int x, int y, int z) {
		// Can ignore z coord, as it is constrained by x and y.
		return this.getByXYCoords(x, y);
	}
	
	public static class IJCoords {
		public int i;
		public int j;
		
		IJCoords(int i, int j) {
			this.i = i;
			this.j = j;
		}
	}
	
	public Hex getByIJCoords(int i, int j) {
		int z = i;
		int x = j - (i - (i & 1)) / 2;
	    int y = -(x + z);
		return this.getByXYZCoords(x, y, z);
	}
	
	private void addHex(Hex hex) {
		this.hexes.put(this.hashXYCoords(hex.getX(), hex.getY()), hex);
		hex.setParent(this);
		// NOTE: not hex.getIJCoords, since haven't initialized the heights.
		IJCoords ij = HexData.convertFromXYRaw(hex.getX(), hex.getY());
		if (ij.i > this.maxHeight)
			this.maxHeight = ij.i;
		if (ij.j > this.maxWidth)
			this.maxWidth = ij.j;
		if (ij.i < this.minHeight)
			this.minHeight = ij.i;
		if (ij.j < this.minWidth)
			this.minWidth = ij.j;
	}
	
	public Collection<Hex> getAllHexes() {
		return this.hexes.values();
	}
	
	public static class EdgeIntersectionContainer {
		EdgeIntersectionContainer(IntersectionData i, EdgeData e) { 
			this.intersectionData = i; this.edgeData = e;
		}
		public IntersectionData intersectionData;
		public EdgeData edgeData;
	}
	
	EdgeIntersectionContainer setupIntersections(IntersectionData interData, EdgeData edgeData) {
		int interIdCounter = 0;
		int edgeIdCounter = 0;
		HashMap<HashSet<Hex>, Intersection> intersectionCache = new HashMap<HashSet<Hex>, Intersection>();
		HashMap<HashSet<Hex>, Edge> edgeCache = new HashMap<HashSet<Hex>, Edge>();
		for (Hex hex : this.getAllHexes()) {
			ArrayList<Hex> neighbors = hex.getNeighbors();
			int harborDirection = -1;
			if (hex.getHarborType() != null)
				harborDirection = hex.getDirectionToCenter();
			for (int k=0; k<6; k++) {
				// Inefficient, but thorough.
				Hex neighbor1 = neighbors.get(k), neighbor2 = neighbors.get((k + 1) % 6);
				HashSet<Hex> cacheKey = new HashSet<Hex>();
				cacheKey.add(hex); cacheKey.add(neighbor1); cacheKey.add(neighbor2);
				Intersection inter = intersectionCache.get(cacheKey);
				HarborType ht = null;
				// Set the harbor in the two intersections intersecting the hex in the harbor direction.
				if (harborDirection >= 0 && ((harborDirection + 5) % 6 == k || harborDirection == k)) {
					ht = hex.getHarborType(); 
//					System.out.println("k: " + k + " direction " + harborDirection);
				}
				// Create a new intersection only if we didn't find it before.
				if (inter == null) {
					inter = new Intersection(++interIdCounter);
					interData.addIntersection(inter);
					if (neighbor1 != null || neighbor2 != null)
						intersectionCache.put(cacheKey, inter);
					inter.setHexes(cacheKey);
				}
				if (ht != null) 
					inter.setHarborType(ht);
				hex.addIntersection(inter, k);
				HashSet<Hex> edgeCacheKey = new HashSet<Hex>(cacheKey);
				edgeCacheKey.remove(neighbor2); // meh...
				Edge edge = edgeCache.get(edgeCacheKey);
				if (edge == null) {
					edge = new Edge(++edgeIdCounter, edgeCacheKey, edgeData);
					if (neighbor2 != null)
						edgeCache.put(edgeCacheKey, edge);
					edgeData.addEdge(edge);
				}
				hex.addEdge(edge, k);
			}
		}
		
		// more duplication of visits here, but it is probably OK in a setup function like this
		for (Hex hex : this.hexes.values()) {
			for (int i=0; i<6; i++) {
				if (hex.getEdges().get(i) == null)
					System.out.println("Hex "+ hex.getId() + " baad: " + hex.getEdges());
				Edge e = hex.getEdge(i);
				for (int j=i-1; j<i + 1; j++) {
					Intersection inter = hex.getIntersection((j + 6) % 6);  // avoid neg number mod
					e.addIntersection(inter);
					inter.addEdge(e);
				}
			}
		}
		
		return new EdgeIntersectionContainer(interData, edgeData);
	}
	
	private void setupHarbors() {
		this.setupHarbors(defaultHarborCounts);
	}
	
	private void setupHarbors(HashMap<HarborType, Integer> harborCounts) {
		/* Iterates around the edges, setting up harbors on every other hex.
		 * Uses the harborCounts to populate the harborTypes of the oceans.
		 */
		LinkedList<HarborType> harborTypes = new LinkedList<HarborType>();
		for (Entry<HarborType, Integer> e : harborCounts.entrySet())
			for (int i=0; i<e.getValue(); i++)
				harborTypes.add(e.getKey());
		Collections.shuffle(harborTypes);		

		Boolean even = true;
		// Why 4? We have to start that far to be going the right direction... can I prove that right now: no. It just works.
		int x = Hex.directions[4][0] * this.radius, y = Hex.directions[4][1] * this.radius;
		Hex hex = this.getByXYCoords(x, y);
		for (int i=0; i<6; i++)
			for (int j=0; j<this.radius; j++) {
//				System.out.println("I'm at " + hex.getX() + ',' + hex.getY() + " going " + Hex.directions[i][0] + "," + Hex.directions[i][1]);
				if (even)
					hex.setHarborType(harborTypes.remove());
				hex = hex.getNeighbor(i);
				even = !even;
			}
	}
	
	/* Serialization stuff */
	
	public Hex[][] get2DHexArray() {
		Hex[][] hexes = new Hex[this.maxHeight - this.minHeight + 1][this.maxWidth - this.minWidth + 1];
		for (Hex hex : this.getAllHexes()) {
			IJCoords ij = hex.getIJCoords();
//			System.out.println(String.format("i: %d, j: %d, maxH: %d, minH %d, maxW: %d, minW: %d", ij.i, ij.j, maxHeight, minHeight, maxWidth, minWidth));
			hexes[ij.i][ij.j] = hex;
		}
		return hexes;
	}
	
	private static class HexTypeSerializer implements JsonSerializer<HexType> {

		@Override
		public JsonElement serialize(HexType arg0, Type arg1,
				JsonSerializationContext arg2) {
			return new JsonPrimitive(arg0.toString().toLowerCase());
		}
	}
	
	private static class JsonContainer {
		// A very simple container for jsonifying
		@SuppressWarnings("unused")
		private Hex[][] hexes;
		@SuppressWarnings("unused")
		private int robberHexId;
		
		JsonContainer(Hex[][] hexes, int robberHexId) {
			this.hexes = hexes; this.robberHexId = robberHexId;
		}
	}
	
	public JsonElement toJson() {
		GsonBuilder gsonB = new GsonBuilder();
		gsonB.registerTypeAdapter(HexType.class, new HexTypeSerializer());
		Gson gson = gsonB.create();
		return gson.toJsonTree(new JsonContainer(this.get2DHexArray(), this.robberHexId));
	}
	
	public String toString() {
		// NOTE: this is not the same as toJson() (should it be?)
		Gson gson  = new Gson();
		return gson.toJson(this.get2DHexArray());
	}
	
	public IJCoords convertFromXY(int x, int y) {
		int z = -(x + y);
		int i = z;
		int j = x + (z - (z & 1)) / 2;
		return new IJCoords(i - this.minHeight, j - this.minWidth);
	}

	public static IJCoords convertFromXYRaw(int x, int y) {
		int z = -(x + y);
		int i = z;
		int j = x + (z - (z & 1)) / 2;
		return new IJCoords(i, j);
	}

	public void finalizeFromDB(BoardModel board) {
		this.board = board;
		for (Hex hex : this.hexes.values())
			hex.setParent(this);
	}

	@Override
	public Hex getElement(Integer id) {
		return this.hexes.get(id);
	}
}
