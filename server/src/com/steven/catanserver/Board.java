package com.steven.catanserver;

import java.util.*;

public class Board {
	public static final int VPS_TO_WIN = 10;
	
	public static Board makeNewBoard(int numPlayers, String userId) {
		return makeNewBoard(numPlayers, userId, new LinkedList<HumanPlayer>());
	}
	
	public static Board makeNewBoard(int numPlayers, String userId, List<HumanPlayer> humanPlayers) {
		System.out.println("Making new this with " + numPlayers + " players");
		Board board = new Board(numPlayers);
		board.setup(numPlayers, userId, humanPlayers);
		
		return board;
	}
	
	public static Board makeFakeBoard(Board template) {
		Board fake = new Board(template.players.size());
		fake.hexes = template.hexes;  // immutable
		fake.intersections = new IntersectionData(fake, template.intersections);
		fake.edges = new EdgeData(fake, template.edges);
		fake.id = template.id;
		
		for (HumanPlayer hp : template.getHumanPlayers())
			fake.addHumanPlayer(new HumanPlayer(fake, hp));
		for (ComputerPlayer cp: template.getComputerPlayers()) {
			fake.addComputerPlayer(cp.fakeCopy(fake));
		}
		
		fake.devCards = new LinkedList<DevelopmentCard>(template.devCards);  // cards immutable, list is mutable
		fake.handData = new HandData(template.handData);  // should not be used
		fake.victoryPoints = new VictoryPointData(template.victoryPoints);
		fake.turnNumber = template.turnNumber;
		return fake;
	}
	
	void setup(int numPlayers, String userId, List<HumanPlayer> humanPlayers) {
		this.hexes = HexData.generateHexes(this);
		this.intersections = new IntersectionData(this);
		this.edges = new EdgeData(this);
		this.hexes.setupIntersections(this.intersections, this.edges);
		this.id = UUID.randomUUID().toString();
		this.devCards = DevelopmentCard.getNewDevCardStack();
		
		LinkedList<Integer> turnOrders = new LinkedList<Integer>();
		for (int i=0; i<numPlayers; i++)
			turnOrders.add(i);
		Collections.shuffle(turnOrders);
		// For now, only one human player
		for (HumanPlayer hp : humanPlayers) {
			hp.setTurnOrder(turnOrders.remove());
			this.addHumanPlayer(hp);
		}
		for (int i=humanPlayers.size(); i<numPlayers; i++) {
			PlayerColor pc = PlayerColor.values()[i];
			if (pc == PlayerColor.ORANGE || pc == PlayerColor.RED)
				this.addComputerPlayer(new StateAwareCPUPlayer(PlayerColor.values()[i], this, turnOrders.remove()));
			else
				this.addComputerPlayer(new StateSearchingCPUPlayer(PlayerColor.values()[i], this, turnOrders.remove()));
		}
		this.handData = new HandData(this.players);
		this.victoryPoints = new VictoryPointData(VPS_TO_WIN, this.players);
		this.turnNumber = -2 * numPlayers;
	}
	
	public Board(int numPlayers) {
		this.players = new ArrayList<Player>(numPlayers);
		this.playerMap = new HashMap<String, Player>(numPlayers);
		for (int i = 0; i<numPlayers; i++)
			this.players.add(null);
	}
	
	public Board(int numPlayers, String id, HexData hexes, IntersectionData intersects, EdgeData edges, HandData hands, VictoryPointData vps, DevelopmentCard[] devCards, int turnNumber) {
		this(numPlayers);
		this.id = id;
		this.hexes = hexes;
		this.intersections = intersects;
		this.edges = edges;
		this.handData = hands;
		this.victoryPoints = vps;
		this.turnNumber = turnNumber;
		this.devCards = new LinkedList<DevelopmentCard>();
		for (DevelopmentCard dc : devCards)
			this.devCards.add(dc);
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
	private LinkedList<DevelopmentCard> devCards;
	
	public String getId() {
		return this.id;
	}
	
	protected void addComputerPlayer(ComputerPlayer computerPlayer) {
		this.addPlayer(computerPlayer);
		this.computerPlayers.add(computerPlayer);
	}

	protected void addHumanPlayer(HumanPlayer humanPlayer) {
		this.addPlayer(humanPlayer);
		this.humanPlayers.add(humanPlayer);
	}
	
	protected void addPlayer(Player player) {
		this.players.set(player.getTurnOrder(), player);
		this.playerMap.put(player.getId(), player);
	}
	
	protected List<ComputerPlayer> getComputerPlayers() {
		return this.computerPlayers;
	}
	
	protected List<HumanPlayer> getHumanPlayers() {
		return this.humanPlayers;
	}
	
	public Hex getHex(Integer i) {
		return this.hexes.getById(i);
	}
	
	Boolean hasPlayerWon(PlayerColor p) {
		return this.victoryPoints.hasPlayerWon(p);
	}
	
	Player getPlayer(String playerId) {
		return this.playerMap.get(playerId);
	}
	
	List<Player> getPlayers() {
		return this.players;
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
	
	public LinkedList<DevelopmentCard> getDevCards() {
		return this.devCards;
	}
	
	public VictoryPointData getVPData() {
		return this.victoryPoints;
	}
	
	public int getTurnNumber() {
		return this.turnNumber;
	}
	
	public int incrementTurnNumber() {
		return ++this.turnNumber;
	}
	
	protected HashMap<Integer, Collection<Hex>> getHexesByRollNum() {
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
	
	protected HashMap<PlayerColor, Player> getPlayersByColors() {
		if (this.playersByColors == null) {
			this.playersByColors = new HashMap<PlayerColor, Player>();
			for (Player p : this.players)
				this.playersByColors.put(p.getPlayerColor(), p);
		}
		return this.playersByColors;
	}
	
	public Player getPlayerByColor(PlayerColor pc) {
		return this.getPlayersByColors().get(pc);
	}
	
	public void takeAll(CardType ct, Player player) {
		int total = 0;
		for (Player p : this.getPlayers())
			if (p.getId() != player.getId())
				total += p.getHand().takeAll(ct);
		player.getHand().add(ct, total);
	}
	
	/* Methods for players to modify the board */
	
	void placeSettlement(int interId, PlayerColor playerColor) {
		Intersection inter = this.getIntersectionData().getElement(interId);
		assert (inter != null);
		Player p = this.getPlayersByColors().get(playerColor);
		this.placeSettlement(inter, p);
	}
	
	void placeSettlement(Intersection inter, Player p) {
		inter.placeSettlement(p);
	}
	
	public void placeRoad(int edgeId, PlayerColor playerColor) {
//		System.out.println("Placing road.");
		Edge edge = this.getEdgeData().getEdge(edgeId);
		Player player = this.getPlayersByColors().get(playerColor);
		assert (player.getOwnedEdges().getAll().contains(edge));
		this.placeRoad(edge, player);
	}
	
	public void placeRoad(Edge edge, Player player) {
		edge.placeRoad(player);
	}
	
	void placeCity(Intersection inter, Player p) {
		assert (inter.canPlaceCity() && inter.getSettlementColor() != null && inter.getSettlementColor()  == p.getPlayerColor());
		inter.placeCity(p);
	}
	
	void placeCity(int interId, PlayerColor playerColor) {
		Intersection inter = this.getIntersectionData().getElement(interId);
		Player p = this.getPlayersByColors().get(playerColor);
		this.placeCity(inter, p);
	}
	
	void pullCard(Player p) {
		DevelopmentCard devCard = this.getDevCards().remove(0);
		devCard.receive(p);
		p.addCard(devCard);
	}
	
	public void playDevCard(Player player, DevelopmentCard devCard) {
		devCard.play(player);
	}
	
	public void addVictoryPoint(Player player) {
		this.getVPData().addVictoryPoint(player);
	}
	
	public void moveAndRob(Hex movedToHex, Player robbingPlayer, Player robbedPlayer) {
		if (robbedPlayer != null) {
			CardType ct = robbedPlayer.getHand().popRandom();
			robbingPlayer.getHand().addOne(ct);
		}
		else
			System.out.println(robbingPlayer.getPlayerColor() +  " didn't rob anybody!");
		this.getHexData().setRobberHex(movedToHex);
	}

	boolean checkLargestArmy(Player player) {
		PlayerColor currentLAP = this.getVPData().getLargestArmyPlayer();
		if (currentLAP == player.getPlayerColor() || player.getNumSoldierPlayed() < VictoryPointData.MINIMUM_SOLDIERS)
			return false;
		Player lap = this.getPlayerByColor(currentLAP);
		if (lap == null || lap.getNumSoldierPlayed() < player.getNumSoldierPlayed()) {
			this.getVPData().setLargestArmyPlayer(player.getPlayerColor());
			return true;
		}
		return false;
	}
	
	boolean checkLongestRoad(Player player) {
		PlayerColor currentLRP = this.getVPData().getLongestRoadPlayer();
		if (currentLRP == player.getPlayerColor() || player.getConsecutiveRoads() < VictoryPointData.MINIMUM_ROADS)
			return false;
		Player lap = this.getPlayerByColor(currentLRP);
		if (lap == null || lap.getConsecutiveRoads() < player.getConsecutiveRoads()) {
			this.getVPData().setLongestRoadPlayer(player.getPlayerColor());
			return true;
		}
		return false;
	}
}
