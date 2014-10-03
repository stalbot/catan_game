package com.steven.catanserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.google.gson.Gson;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Pipeline;

public class BoardModel {
	
	public static final int VPS_TO_WIN = 10;
	
	public static final String TURN_KEY = "turn";
	public static final String HEX_KEY = "hexes";
	public static final String INTERSECTION_KEY = "intersections";
	public static final String EDGE_KEY = "edges";
	public static final String HAND_KEY = "hands";
	public static final String ROBBER_KEY = "robber";
	public static final String DEV_CARD_KEY = "dev_cards";
	public static final String VP_KEY = "victory_points";
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
		VictoryPointData vps = gson.fromJson(rawBoardData.get(VP_KEY), VictoryPointData.class);
		DevelopmentCard[] devCards = gson.fromJson(rawBoardData.get(DEV_CARD_KEY), DevelopmentCard[].class);
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
			
		BoardModel board = new BoardModel(numPlayers, boardId, hexes, inters, edges, hands, vps);
		for (ComputerPlayer cp : computerPlayers)
			board.addComputerPlayer(cp);
		for (HumanPlayer hp : humanPlayers)
			board.addHumanPlayer(hp);
		// this data must be repaired from the db.
		System.out.println(board.players.size() + " total players (should be " + numPlayers + ").");
		for (Player p : board.players)
			p.setBoard(board);
		
		board.turnNumber = Integer.parseInt(rawBoardData.get(TURN_KEY));
		board.devCards = new LinkedList<DevelopmentCard>();
		for (int i=0; i<devCards.length; i++)
			board.devCards.add(devCards[i]);

//		System.out.println(String.format("%s %s %s %s %s", board, board.players, board.hexes, board.handData, board.intersections));

		// This does one final bit of setup that we need to get around the GSON automatic instantiation
		inters.finalizeFromDB(board);
		hexes.finalizeFromDB(board);
		edges.finalizeFromDB(board);
		
		return board;
	}
	
	public static BoardModel makeNewBoard(int numPlayers, String userId) {
		int numHumans = 0;  // for debugging
		System.out.println("Making new board with " + numPlayers + " players");
		BoardModel board = new BoardModel(numPlayers);
		board.hexes = HexData.generateHexes(board);
		board.intersections = new IntersectionData(board);
		board.edges = new EdgeData(board);
		board.hexes.setupIntersections(board.intersections, board.edges);
		board.id = UUID.randomUUID().toString();
		board.devCards = DevelopmentCard.getNewDevCardStack();
		
		LinkedList<Integer> turnOrders = new LinkedList<Integer>();
		for (int i=0; i<numPlayers; i++)
			turnOrders.add(i);
		Collections.shuffle(turnOrders);
		// For now, only one human player
		for (int i=0; i<numHumans; i++)
			// TODO: this needs TLC, but OK for now
			board.addHumanPlayer(new HumanPlayer(PlayerColor.values()[i], board, userId, turnOrders.remove()));
		for (int i=numHumans; i<numPlayers; i++) {
			board.addComputerPlayer(new ComputerPlayer(PlayerColor.values()[i], board, turnOrders.remove()));
		}
		board.handData = new HandData(board.players);
		board.victoryPoints = new VictoryPointData(VPS_TO_WIN, board.players);
		board.turnNumber = -2 * numPlayers;
		
		board.saveToDB();
		return board;
	}
	
	
	
	public void startSetup() {
		new Thread(new RunSetup()).start();
	}
	
	public void startTurnLoop() {
		new Thread(new RunTurnLoop()).start();
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
	private transient HashMap<PlayerColor, Player> playersByColors;
	private transient HandData handData;
	private transient HashMap<Integer, Collection<Hex>> hexesByRollNum = null;
	private VictoryPointData victoryPoints;
	private List<DevelopmentCard> devCards;

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
		// Spectator mode
		return new HumanPlayer(null, this, playerId, -1);
	}
	
	Player getPlayer(String playerId) {
		return this.playerMap.get(playerId);
	}
	
	List<Player> getPlayers() {
		return this.players;
	}

	public BoardModel(int numPlayers) {
		this.players = new ArrayList<Player>(numPlayers);
		this.playerMap = new HashMap<String, Player>(numPlayers);
		for (int i = 0; i<numPlayers; i++)
			this.players.add(null);
	}
	
	public BoardModel(int numPlayers, String id, HexData hexes, IntersectionData intersects, EdgeData edges, HandData hands, VictoryPointData vps) {
		this(numPlayers);
		this.id = id;
		this.hexes = hexes;
		this.intersections = intersects;
		this.edges = edges;
		this.handData = hands;
		this.victoryPoints = vps;
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
	
	public HexData getHexData() {
		return this.hexes;
	}
	
	public HandData getHandData() {
		return this.handData;
	}
	
	public int getRemainingDevCards() {
		return this.devCards.size();
	}
	
	private HashMap<Integer, Collection<Hex>> getHexesByRollNum() {
		if (this.hexesByRollNum == null) {
			this.hexesByRollNum = new HashMap<Integer, Collection<Hex>>();
			for (Hex hex : this.getHexes()) {
				if (this.hexesByRollNum.get(hex.getRollNumber()) == null)
					this.hexesByRollNum.put(hex.getRollNumber(), new ArrayList<Hex>());
				this.hexesByRollNum.get(hex.getRollNumber()).add(hex);
			}
		}
		return this.hexesByRollNum;
	}
	
	private HashMap<PlayerColor, Player> getPlayersByColors() {
		if (this.playersByColors == null) {
			this.playersByColors = new HashMap<PlayerColor, Player>();
			for (Player p : this.players)
				this.playersByColors.put(p.getPlayerColor(), p);
		}
		return this.playersByColors;
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
	
	void placeCity(Intersection inter, Player p) {
		inter.placeCity(p);
		this.saveModifiedToDB();
		String message = new Gson().toJson(new TurnEvent.IntersectionChangeEvent(p, inter));
		this.sendMessage(message);
	}
	
	void pullCard(Player p) {
		DevelopmentCard devCard = this.devCards.remove(0);
		devCard.receive(p);
		p.addCard(devCard);
		this.saveModifiedToDB();
		String message = new Gson().toJson(new TurnEvent.DevCardPullEvent(p));
		this.sendMessage(message);
	}
	
	public void addVictoryPoint(Player player) {
		this.victoryPoints.addVictoryPoint(player);
		this.saveModifiedToDB();
	}
	
	public void moveAndRob(Hex movedToHex, Player robbingPlayer, Player robbedPlayer) {
		CardType ct = robbedPlayer.getHand().popRandom();
		robbingPlayer.getHand().addOne(ct);
		this.hexes.setRobberHex(movedToHex);
		this.saveModifiedToDB();
		// TODO: robber message
	}
	
	public void takeAll(CardType ct, Player player) {
		int total = 0;
		for (Player p : this.players)
			if (p.getId() != player.getId())
				total += p.getHand().takeAll(ct);
		player.getHand().add(ct, total);
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
		client.hset(this.getRedisKey(), VP_KEY, gson.toJson(this.victoryPoints));
		client.hset(this.getRedisKey(), DEV_CARD_KEY, gson.toJson(this.devCards));
//		System.out.println(client.hgetAll(this.getRedisKey()));
//		System.out.println(client.keys("*"));
		System.out.println("Save successful");
	}
	
	void saveModifiedToDB() { 
		Jedis client = CatanServer.getRedisClient();
		Pipeline pipe = client.pipelined();
		Gson gson = new Gson();
		pipe.hset(this.getRedisKey(), TURN_KEY, String.valueOf(this.turnNumber));
		pipe.hset(this.getRedisKey(), HAND_KEY, gson.toJson(this.handData));
		pipe.hset(this.getRedisKey(), INTERSECTION_KEY, gson.toJson(this.intersections));
		pipe.hset(this.getRedisKey(), EDGE_KEY, gson.toJson(this.edges));
		pipe.hset(this.getRedisKey(), HUMAN_PLAYERS_KEY, gson.toJson(this.humanPlayers));
		pipe.hset(this.getRedisKey(), COMPUTER_PLAYERS_KEY, gson.toJson(this.computerPlayers));
		pipe.hset(this.getRedisKey(), VP_KEY, gson.toJson(this.victoryPoints));
		pipe.hset(this.getRedisKey(), DEV_CARD_KEY, gson.toJson(this.devCards));
		pipe.sync();
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
			// if we get here, keep going
			doTurnLoop();
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
			// TODO: consider trying to consolidate saves
			this.saveModifiedToDB();
		}
	}
	
	public class RunTurnLoop implements Runnable {

		@Override
		public void run() {
			doTurnLoop();
		}
		
	}
	
	private void doTurnLoop() {
		assert(this.turnNumber >= 0);
		
		Boolean hasPlayerWon = false;
		int numPlayers = this.players.size();
		Player player;
				
		do {
			player = this.players.get(this.turnNumber % numPlayers);
			this.doRollAndGrantCards();
			this.notifyTurnStart(player.getId());
			if (!player.doTurn())
				return;
			// the CPU took care of the turn, so we save it now.
			this.turnNumber++;
			// TODO: consider trying to consolidate saves
			this.saveModifiedToDB();
			hasPlayerWon = this.hasPlayerWon(player.getPlayerColor());
		} while (!hasPlayerWon);
		System.out.println(player.getPlayerColor().toString() + " has won the game!");
	}
	
	private void doRollAndGrantCards() {
		Random rand = new Random();
		int rollNum = (rand.nextInt(6) + 1) + (rand.nextInt(6) + 1);
		if (rollNum == 7) {
			System.out.println("7 rolled, yay!"); // TODO
			return;
		}
		System.out.println(rollNum + " was rolled");
		for (Hex hex : this.getHexesByRollNum().get(rollNum))
			for (Intersection inter : hex.getIntersections())
				if (inter.getSettlementColor() != null) {
					int numToAdd = inter.getIsCity() ? 2 : 1;
					this.getPlayersByColors().get(inter.getSettlementColor()).getHand().add(hex.getType().getCardType(), numToAdd);
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
