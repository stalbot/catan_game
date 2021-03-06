package com.steven.catanserver;

import java.util.*;

import com.google.gson.Gson;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Pipeline;

public class BoardModel extends Board {
	
	public BoardModel(int numPlayers) {
		super(numPlayers);
	}

	public BoardModel(int numPlayers, String boardId, HexData hexes,
			IntersectionData inters, EdgeData edges, HandData hands,
			VictoryPointData vps, DevelopmentCard[] devCards, int turnNumber) {
		super(numPlayers, boardId, hexes, inters, edges, hands, vps, devCards, turnNumber);
	}

	public static final String TURN_KEY = "turn";
	public static final String HEX_KEY = "hexes";
	public static final String INTERSECTION_KEY = "intersections";
	public static final String EDGE_KEY = "edges";
	public static final String HAND_KEY = "hands";
	public static final String ROBBER_KEY = "robber";
	public static final String DEV_CARD_KEY = "dev_cards";
	public static final String VP_KEY = "victory_points";
	public static final String HUMAN_PLAYERS_KEY = "humanPlayers";
	public static final String MOST_RECENT_MESSAGE_KEY = "mostRecentMessage";
	public static final String NUM_CPU_PLAYERS_KEY = "numCpuPlayers";
	
	public static final String EVENT_TYPE = "eventType";
	
	public static final int SLEEP_BETWEEN_TURNS_MS = 25;

	static String makeRedisKey(String boardId) {
		return "board:" + boardId;
	}
	
	static String makeComputerPlayerKey(ComputerPlayer.ComputerPlayerType cpt, int playerNum) {
		return cpt.toString() + "_" + playerNum;
	}
	
	public static BoardModel getFromDB(String boardId) {
		Map<String, String> rawBoardData = CatanServer.getRedisClient().hgetAll(makeRedisKey(boardId));
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
		ArrayList<ComputerPlayer> computerPlayers = new ArrayList<ComputerPlayer>();
		int totalCpuPlayers = Integer.parseInt(rawBoardData.get(NUM_CPU_PLAYERS_KEY));
		for (ComputerPlayer.ComputerPlayerType cpt : ComputerPlayer.ComputerPlayerType.values()) {
			for (int playerNum=0; playerNum<totalCpuPlayers; playerNum++) {
				String data = rawBoardData.get(makeComputerPlayerKey(cpt, playerNum));
				if (data == null)
					continue;
				
				Class<? extends ComputerPlayer> cpuClass = ComputerPlayer.getCPUPlayerByType(cpt);
				ComputerPlayer cp = gson.fromJson(data, cpuClass); 
				computerPlayers.add(cp);
			}
		}
		int numPlayers = humanPlayers.size() + computerPlayers.size();
		int turnNumber = Integer.parseInt(rawBoardData.get(TURN_KEY));
		
		BoardModel board = new BoardModel(numPlayers, boardId, hexes, inters, edges, hands, vps, devCards, turnNumber);
		for (ComputerPlayer cp : computerPlayers)
			board.addComputerPlayer(cp);
		for (HumanPlayer hp : humanPlayers)
			board.addHumanPlayer(hp);
		// this data must be repaired from the db.
		System.out.println(board.getPlayers().size() + " total players (should be " + numPlayers + ").");
		for (Player p : board.getPlayers())
			p.setBoard(board);

//		System.out.println(String.format("%s %s %s %s %s", board, board.players, board.hexes, board.handData, board.intersections));

		// This does one final bit of setup that we need to get around the GSON automatic instantiation
		inters.finalizeFromDB(board);
		hexes.finalizeFromDB(board);
		edges.finalizeFromDB(board);
		
		return board;
	}
	
	public static BoardModel makeNewBoard(int numPlayers, String userId) {
		int numHumans = 0;  // TODO: for debugging, make better later
		BoardModel b = new BoardModel(numPlayers);
		List<HumanPlayer> humanPlayers = new ArrayList<HumanPlayer>();
		for (int i=0; i<numHumans; i++)
			humanPlayers.add(new HumanPlayer(PlayerColor.values()[i], b, userId, 0));  // needs TLC
		b.setup(numPlayers, userId, humanPlayers);
		b.saveToDB();
		return b;
	}
	
	public void startSetup() {
		new Thread(new RunSetup()).start();
	}
	
	public void startTurnLoop() {
		new Thread(new RunTurnLoop()).start();
	}
	
	HumanPlayer getHumanPlayer(String playerId) {
		// Maybe store humanPlayers as Map... but it's so small...
		for (HumanPlayer p : this.getHumanPlayers()) {
//			System.out.println(String.format("%s == %s", p.getId(), playerId));
			if (p.getId().contentEquals(playerId))
				return p;
		}
		// Spectator mode
		return new HumanPlayer(null, this, playerId, -1);
	}
	
	/* Methods for players to modify the board */
	
	void placeSettlement(Intersection inter, Player p) {
		super.placeSettlement(inter, p);
		this.saveModifiedToDB();
		String message = new Gson().toJson(new TurnEvent.IntersectionChangeEvent(p, inter));
		this.sendMessage(message);
	}
	
	public void placeRoad(Edge edge, Player player) {
		super.placeRoad(edge, player);
		this.saveModifiedToDB();
		String message = new Gson().toJson(new TurnEvent.EdgeChangeEvent(player, edge));
		this.sendMessage(message);
	}
	
	void placeCity(Intersection inter, Player p) {
		super.placeCity(inter, p);
		this.saveModifiedToDB();
		String message = new Gson().toJson(new TurnEvent.IntersectionChangeEvent(p, inter));
		this.sendMessage(message);
	}
	
	void pullCard(Player p) {
		super.pullCard(p);
		this.saveModifiedToDB();
		String message = new Gson().toJson(new TurnEvent.DevCardPullEvent(p));
		this.sendMessage(message);
	}
	
	public void addVictoryPoint(Player player) {
		super.addVictoryPoint(player);
		this.saveModifiedToDB();
	}
	
	public void moveAndRob(Hex movedToHex, Player robbingPlayer, Player robbedPlayer) {
		super.moveAndRob(movedToHex, robbingPlayer, robbedPlayer);
		this.saveModifiedToDB();
		String message = new Gson().toJson(new TurnEvent.RobberMoveEvent(movedToHex, robbingPlayer, robbedPlayer));
		this.sendMessage(message);
	}
	
	public void playDevCard(Player player, DevelopmentCard dc) {
		super.playDevCard(player, dc);
		String message = new Gson().toJson(new TurnEvent.DevCardPlayEvent(player, dc));
		this.sendMessage(message);
	}
	
	public boolean checkLargestArmy(Player player) {
		boolean hasChanged = super.checkLargestArmy(player);
		if (hasChanged)
			this.saveModifiedToDB();
		return hasChanged;
	}
	
	public boolean checkLongestRoad(Player player) {
		boolean hasChanged = super.checkLongestRoad(player);
		if (hasChanged) 
			this.saveModifiedToDB();
		return hasChanged;
	}
	
	/* Various Redis methods */
	
	private String getPublishChannel() {
		return String.format("publish_channel:%s", this.getId());
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
	
	protected void sendWinMessage(Player player) {
		String message = new Gson().toJson(new TurnEvent.WinEvent(player));
		this.sendMessage(message);
	}
	
	private void sendMessage(String message) {
		CatanServer.getRedisClient().publish(getPublishChannel(), message);
		this.saveMostRecentMessage(message);
	}
	
	public void registerChannelListener(JedisPubSub listener) {
		new Thread(new RunRegisterListener(listener)).start();
	}
	
	private String getRedisKey() {
		return makeRedisKey(this.getId());
	}
	
	/* Redis save methods */
	
	private void saveCpuPlayers(Jedis client, Gson gson) {
		for (int i=0; i<this.getComputerPlayers().size(); i++) {
			ComputerPlayer cp = this.getComputerPlayers().get(i);
			client.hset(this.getRedisKey(), 
					BoardModel.makeComputerPlayerKey(cp.getPlayerType(), i), 
					gson.toJson(cp));
		}
	}
	
	void saveToDB() {
		Jedis client = CatanServer.getRedisClient();
		Pipeline pipe = client.pipelined();
		Gson gson = new Gson();
		
		// deliberately not saving TURN_KEY right now
		client.hset(this.getRedisKey(), HEX_KEY, gson.toJson(this.getHexData()));
		client.hset(this.getRedisKey(), HUMAN_PLAYERS_KEY, gson.toJson(this.getHumanPlayers()));
		client.hset(this.getRedisKey(), NUM_CPU_PLAYERS_KEY, ((Integer) this.getComputerPlayers().size()).toString());
		this.saveCpuPlayers(client, gson);
		client.hset(this.getRedisKey(), HAND_KEY, gson.toJson(this.getHandData()));
		client.hset(this.getRedisKey(), TURN_KEY, String.valueOf(this.getTurnNumber()));
		client.hset(this.getRedisKey(), INTERSECTION_KEY, gson.toJson(this.getIntersectionData()));
		client.hset(this.getRedisKey(), EDGE_KEY, gson.toJson(this.getEdgeData()));
		client.hset(this.getRedisKey(), VP_KEY, gson.toJson(this.getVPData()));
		client.hset(this.getRedisKey(), DEV_CARD_KEY, gson.toJson(this.getDevCards()));
		pipe.sync();
//		System.out.println(client.hgetAll(this.getRedisKey()));
//		System.out.println(client.keys("*"));
		System.out.println("Save successful");
	}
	
	void saveModifiedToDB() { 
		Jedis client = CatanServer.getRedisClient();
		Pipeline pipe = client.pipelined();
		Gson gson = new Gson();
		client.hset(this.getRedisKey(), HAND_KEY, gson.toJson(this.getHandData()));
		client.hset(this.getRedisKey(), TURN_KEY, String.valueOf(this.getTurnNumber()));
		client.hset(this.getRedisKey(), INTERSECTION_KEY, gson.toJson(this.getIntersectionData()));
		client.hset(this.getRedisKey(), EDGE_KEY, gson.toJson(this.getEdgeData()));
		client.hset(this.getRedisKey(), HUMAN_PLAYERS_KEY, gson.toJson(this.getHumanPlayers()));
		this.saveCpuPlayers(client, gson);
		client.hset(this.getRedisKey(), VP_KEY, gson.toJson(this.getVPData()));
		client.hset(this.getRedisKey(), DEV_CARD_KEY, gson.toJson(this.getDevCards()));
		pipe.sync();
//		System.out.println(client.hgetAll(this.getRedisKey()));
//		System.out.println(client.keys("*"));
		System.out.println("Save of modified successful");
	}
	
	private void saveMostRecentMessage(String message) {
		// Save the message so that if the client somehow misses their cue, they can get it again.
		Jedis client = CatanServer.getRedisClient();
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
		assert(this.getTurnNumber() < 0);
		ArrayList<Player> playersInOrder = new ArrayList<Player>(this.getPlayers());
		Collections.reverse(playersInOrder);
		playersInOrder.addAll(0, this.getPlayers());
		System.out.println("Doing setupTurns(), turn number " + this.getTurnNumber());
		System.out.println("Doing setupTurns(), players " + new Gson().toJson(playersInOrder));
		
		int totalSetupTurns = playersInOrder.size();
		for (int i=totalSetupTurns + this.getTurnNumber(); i<totalSetupTurns; i++) {
			Player player = playersInOrder.get(i);
			this.notifySetupTurnStart(player.getId());
			if (!player.doSetupTurn())
				return;
			if (this.getTurnNumber() < -this.getPlayers().size())
				// i.e. is this the first pass through
				for (Hex h : player.getOwnedIntersections().getAll().get(0).getHexes()) {
					CardType ct = h.getType().getCardType();
					if (ct != null)
						player.getHand().addOne(ct);
				}
			System.out.println("(end of setup) Player " + player.getPlayerColor() +  " hand " + player.getHand());
			// the CPU took care of the turn, so we save it now.
			this.incrementTurnNumber();
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
		assert(this.getTurnNumber() >= 0);
		
		Boolean hasPlayerWon = false;
		int numPlayers = this.getPlayers().size();
		Player player;
				
		do {
			// Sleeping 
			try { Thread.sleep(SLEEP_BETWEEN_TURNS_MS); } catch (InterruptedException e) {return;}
			player = this.getPlayers().get(this.getTurnNumber() % numPlayers);
			this.doRollAndGrantCards(player);
			this.notifyTurnStart(player.getId());
			System.out.println("(begin turn) Player " + player.getPlayerColor() +  " hand " + player.getHand());
			if (!player.doTurn())
				return;
			// the CPU took care of the turn, so we save it now.
			this.incrementTurnNumber();
			System.out.println("(end turn) Player " + player.getPlayerColor() +  " hand " + player.getHand());
			// TODO: consider trying to consolidate saves
			this.saveModifiedToDB();
			hasPlayerWon = this.hasPlayerWon(player.getPlayerColor());
			// For debugging bad CPUs
			if (this.getTurnNumber() > 800) {
				System.out.println("Fail, nobody wins");
				return;
			}
		} while (!hasPlayerWon);
		this.sendWinMessage(player);
		System.out.println(player.getPlayerColor().toString() + " has won the game! (in " + this.getTurnNumber() + " turns, type " + player.getClass().getSimpleName() + ").");
		System.out.println(player.getPlayerColor().toString() + " had dev cards " + player.getDevCards() + " and had played " + player.getNumSoldierPlayed() + " soldiers.");
		System.out.println(new Gson().toJson(this.getVPData()));
	}
	
	private void doRollAndGrantCards(Player player) {
		Random rand = new Random();
		int rollNum = (rand.nextInt(6) + 1) + (rand.nextInt(6) + 1);
		String message = new Gson().toJson(new TurnEvent.RollEvent(rollNum));
		this.sendMessage(message);
		System.out.println(rollNum + " was rolled");
		if (rollNum == 7) {
			for (Player p : this.getPlayers())
				if (p.getHand().getTotalCards() > 7) {
					CardCollection cardsToDiscard = p.chooseDiscardCards();
					System.out.println("Taking the following from " + p.getPlayerColor() + ": " + cardsToDiscard);
					assert(cardsToDiscard.getTotalCards() >= (p.getHand().getTotalCards() / 2));
					p.getHand().subtract(cardsToDiscard);
				}
			player.moveRobber();
			return;
		}
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
