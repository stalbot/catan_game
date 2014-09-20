package com.steven.catanserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class BoardModel {
	
	public static final String TURN_KEY = "turn";
	public static final String HEX_KEY = "hexes";
	public static final String INTERSECTION_KEY = "intersections";
	public static final String EDGE_KEY = "edges";
	public static final String HAND_KEY = "hands";
	public static final String HUMAN_PLAYERS_KEY = "humanPlayers";
	public static final String COMPUTER_PLAYERS_KEY = "computerPlayers";
	public static final String MOST_RECENT_MESSAGE_KEY = "mostRecentMessage";
	
	public static final String EVENT_TYPE = "eventType";

	static String makeRedisKey(String boardId) {
		return "board:" + boardId;
	}
	
	public static BoardModel getFromDB(String boardId) {
		Map<String, String> rawBoardData = CatanServer.getRedisClient().hgetAll(makeRedisKey(boardId));
		System.out.println(rawBoardData.get(COMPUTER_PLAYERS_KEY));
		Gson gson = new Gson();
		HandData hands = gson.fromJson(rawBoardData.get(HAND_KEY), HandData.class);
		HexData hexes = gson.fromJson(rawBoardData.get(HEX_KEY), HexData.class);
		IntersectionData inters = gson.fromJson(rawBoardData.get(INTERSECTION_KEY), IntersectionData.class);
		EdgeData edges = gson.fromJson(rawBoardData.get(EDGE_KEY), EdgeData.class);
//		System.out.println("Recovered edges from db, size: " + edges.getEdges().size());
//		System.out.println("Here are all the edges:\n" + rawBoardData.get(EDGE_KEY));
		
		HumanPlayer[] humanPlayersArr = gson.fromJson(rawBoardData.get(HUMAN_PLAYERS_KEY), HumanPlayer[].class);
		ArrayList<HumanPlayer> humanPlayers = new ArrayList<HumanPlayer>(humanPlayersArr.length);
		for (int i=0; i<humanPlayersArr.length; i++) {
			humanPlayers.add(humanPlayersArr[i]);
		}
        ComputerPlayer[] computerPlayersArr = gson.fromJson(rawBoardData.get(COMPUTER_PLAYERS_KEY), ComputerPlayer[].class);
		ArrayList<ComputerPlayer> computerPlayers = new ArrayList<ComputerPlayer>(computerPlayersArr.length);
		for (int i=0; i<computerPlayersArr.length; i++) {
			computerPlayers.add(computerPlayersArr[i]);
		}
		int numPlayers = humanPlayers.size() + computerPlayers.size();
			
		BoardModel board = new BoardModel(numPlayers, boardId, hexes, inters, edges, hands);
		for (ComputerPlayer cp : computerPlayers)
			board.addComputerPlayer(cp);
		for (HumanPlayer hp : humanPlayers)
			board.addHumanPlayer(hp);
		// this data must be repaired from the db.
		System.out.println(board.players.size() + " total players (should be " + numPlayers + ").");
		for (Player p : board.players)
			p.setBoard(board);
		
		board.turnNumber = Integer.parseInt(rawBoardData.get(TURN_KEY));

//		System.out.println(String.format("%s %s %s %s %s", board, board.players, board.hexes, board.handData, board.intersections));

		// This does one final bit of setup that we need to get around the GSON automatic instantiation
		inters.finalizeFromDB(board);
		hexes.finalizeFromDB(board);
		edges.finalizeFromDB(board);
		
		return board;
	}
	
	public static BoardModel makeNewBoard(int numPlayers, String userId) {
		System.out.println("Making new board with " + numPlayers + " players");
		BoardModel board = new BoardModel(numPlayers);
		board.hexes = HexData.generateHexes(board);
		HexData.EdgeIntersectionContainer eic = board.hexes.setupIntersections();
		board.intersections = eic.intersectionData;
		board.edges = eic.edgeData;
		board.id = UUID.randomUUID().toString();
		board.victoryPoints = new VictoryPointData(10);
		
		LinkedList<Integer> turnOrders = new LinkedList<Integer>();
		for (int i=0; i<numPlayers; i++)
			turnOrders.add(i);
		Collections.shuffle(turnOrders);
		// For now, only one human player
		board.addHumanPlayer(new HumanPlayer(PlayerColor.values()[0], board, userId, turnOrders.remove()));
		for (int i=0; i<numPlayers - 1; i++) {
			board.addComputerPlayer(new ComputerPlayer(PlayerColor.values()[i + 1], board, turnOrders.remove()));
		}
		board.turnNumber = -2 * numPlayers;
		
		board.saveToDB();
		return board;
	}
	
	public void startSetup() {
		new Thread(new RunSetup()).start();
	}
	
	private void addComputerPlayer(ComputerPlayer computerPlayer) {
		this.addPlayer(computerPlayer);
		this.computerPlayers.add(computerPlayer);
	}

	private void addHumanPlayer(HumanPlayer humanPlayer) {
		this.addPlayer(humanPlayer);
		this.humanPlayers.add(humanPlayer);
	}
	
	private void addPlayer(Player player) {
		this.players.set(player.getTurnOrder(), player);
		this.playerMap.put(player.getId(), player);
	}

	private HexData hexes;
	private IntersectionData intersections;
	private EdgeData edges;
	private String id;
	private int turnNumber = 0;
	private ArrayList<Player> players;
	private transient ArrayList<HumanPlayer> humanPlayers = new ArrayList<HumanPlayer>();
	private transient ArrayList<ComputerPlayer> computerPlayers = new ArrayList<ComputerPlayer>();
	private transient HashMap<String, Player> playerMap;
	private transient HandData handData;
	private transient VictoryPointData victoryPoints;

	public Hex getHex(Integer i) {
		return this.hexes.getById(i);
	}
	
	Boolean hasPlayerWon(PlayerColor p) {
		return this.victoryPoints.hasPlayerWon(p);
	}
	
	HumanPlayer getHumanPlayer(String playerId) {
		// Maybe store humanPlayers as Map... but it's so small...
		for (HumanPlayer p : this.humanPlayers) {
//			System.out.println(String.format("%s == %s", p.getId(), playerId));
			if (p.getId().contentEquals(playerId))
				return p;
		}
		return null;
	}
	
	Player getPlayer(String playerId) {
		return this.playerMap.get(playerId);
	}

	public BoardModel(int numPlayers) {
		this.players = new ArrayList<Player>(numPlayers);
		this.playerMap = new HashMap<String, Player>(numPlayers);
		for (int i = 0; i<numPlayers; i++)
			this.players.add(null);
	}
	
	public BoardModel(int numPlayers, String id, HexData hexes, IntersectionData intersects, EdgeData edges, HandData hands) {
		this(numPlayers);
		this.id = id;
		this.hexes = hexes;
		this.intersections = intersects;
		this.edges = edges;
		this.handData = hands;
	}
	
	public Iterable<Hex> getHexes() {
		return this.hexes.getAllHexes();
	}
	
	public Iterable<Intersection> getIntersections() {
		return this.intersections.getIntersections();
	}
	
	public IntersectionData getIntersectionData() {
		return this.intersections;
	}
	
	public EdgeData getEdgeData() {
		return this.edges;
	}
	
	/* Methods for players to modify the board */
	
	void placeSettlement(Intersection inter, Player p) {
		inter.placeSettlement(p);
		this.saveModifiedToDB();
		String message = new Gson().toJson(new TurnEvent.IntersectionChangeEvent(p, inter));
		this.sendMessage(message);
	}
	
	public void placeRoad(Edge edge, Player player) {
		System.out.println("Placing road.");
		edge.placeRoad(player);
		this.saveModifiedToDB();
		String message = new Gson().toJson(new TurnEvent.EdgeChangeEvent(player, edge));
		this.sendMessage(message);
	}
	
	/* Various Redis methods */
	
	private String getPublishChannel() {
		return String.format("publish_channel:%s", this.id);
	}

	public void notifyTurnStart(String playerId) {
		Player player = this.getPlayer(playerId);
		String message = new Gson().toJson(new TurnEvent.TurnStartEvent(player));
		this.sendMessage(message);
	}
	
	public void notifySetupTurnStart(String playerId) {
		Player player = this.getPlayer(playerId);
		String message = new Gson().toJson(new TurnEvent.TurnSetupStartEvent(player));
		this.sendMessage(message);
	}
	
	private String getTurnStartMessage(String playerId) {
		Player player = this.getPlayer(playerId);
		return new Gson().toJson(new TurnEvent.TurnStartEvent(player));
	}
	
	private void sendMessage(String message) {
		CatanServer.getRedisClient().publish(getPublishChannel(), message);
		this.saveMostRecentMessage(message);
	}
	
	public void registerChannelListener(JedisPubSub listener) {
		new Thread(new RunRegisterListener(listener)).start();
	}
	
	private String getRedisKey() {
		return makeRedisKey(this.id);
	}
	
	/* Redis save methods */
	
	void saveToDB() {
		// TODO: don't save full thing if not necessary
		Jedis client = CatanServer.getRedisClient();
		Gson gson = new Gson();
		
		// deliberately not saving TURN_KEY right now
		client.hset(this.getRedisKey(), HAND_KEY, gson.toJson(this.handData));
		client.hset(this.getRedisKey(), TURN_KEY, String.valueOf(this.turnNumber));
		client.hset(this.getRedisKey(), INTERSECTION_KEY, gson.toJson(this.intersections));
		client.hset(this.getRedisKey(), EDGE_KEY, gson.toJson(this.edges));
		client.hset(this.getRedisKey(), HEX_KEY, gson.toJson(this.hexes));
		client.hset(this.getRedisKey(), HUMAN_PLAYERS_KEY, gson.toJson(this.humanPlayers));
		client.hset(this.getRedisKey(), COMPUTER_PLAYERS_KEY, gson.toJson(this.computerPlayers));
//		System.out.println(client.hgetAll(this.getRedisKey()));
//		System.out.println(client.keys("*"));
		System.out.println("Save successful");
	}
	
	void saveModifiedToDB() {
		Jedis client = CatanServer.getRedisClient();
		Gson gson = new Gson();
		client.hset(this.getRedisKey(), TURN_KEY, String.valueOf(this.turnNumber));
		client.hset(this.getRedisKey(), HAND_KEY, gson.toJson(this.handData));
		client.hset(this.getRedisKey(), INTERSECTION_KEY, gson.toJson(this.intersections));
		client.hset(this.getRedisKey(), EDGE_KEY, gson.toJson(this.edges));
		client.hset(this.getRedisKey(), HUMAN_PLAYERS_KEY, gson.toJson(this.humanPlayers));
		client.hset(this.getRedisKey(), COMPUTER_PLAYERS_KEY, gson.toJson(this.computerPlayers));
//		System.out.println(client.hgetAll(this.getRedisKey()));
//		System.out.println(client.keys("*"));
		System.out.println("Save of modified successful");
	}
	
	private void saveMostRecentMessage(String message) {
		// Save the message so that if the client somehow misses their cue, they can get it again.
		Jedis client = CatanServer.getRedisClient();
		Gson gson = new Gson();
		client.hset(this.getRedisKey(), MOST_RECENT_MESSAGE_KEY, message);
	}
	
	/* Asynchronously runnable methods and their wrappers */ 

	public class RunSetup implements Runnable {

		@Override
		public void run() {
			doSetupTurns();
		}
		
	}

	private void doSetupTurns() {
		assert(this.turnNumber < 0);
		ArrayList<Player> playersInOrder = new ArrayList<Player>(this.players);
		Collections.reverse(playersInOrder);
		playersInOrder.addAll(0, this.players);
		System.out.println("Doing setupTurns(), turn number " + this.turnNumber);
		System.out.println("Doing setupTurns(), players " + new Gson().toJson(players));
		
		int totalSetupTurns = playersInOrder.size();
		for (int i=totalSetupTurns + this.turnNumber; i<totalSetupTurns; i++) {
			Player player = playersInOrder.get(i);
			this.notifySetupTurnStart(player.getId());
			if (!player.doSetupTurn())
				return;
			// the CPU took care of the turn, so we save it now.
			this.turnNumber++;
			this.saveModifiedToDB();
		}
	}
	
	public class RunRegisterListener implements Runnable {
		
		JedisPubSub listener;
		
		RunRegisterListener(JedisPubSub listener) {
			this.listener = listener;
		}

		@Override
		public void run() {
			doRegisterListener(this.listener);
		}
		
	}
	
	private void doRegisterListener(JedisPubSub listener) {
		System.out.println("Subscribing to channel " + this.getPublishChannel());
		CatanServer.getRedisClient().subscribe(listener, this.getPublishChannel());
		System.out.println("Unsubscribing from channel " + this.getPublishChannel());
	}
	
	// This is for debugging if instances are getting cleaned up.
//	public void finalize() {
//		System.out.println("GC successful on a board.");
//		
//		try {super.finalize();}
//		catch(Throwable t) {
//			
//		}
//	}
}
