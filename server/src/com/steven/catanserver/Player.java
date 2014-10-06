package com.steven.catanserver;

import java.util.*;

public abstract class Player {
	
	private PlayerColor color;
	private transient Board board;
	private String id = null;
	private int turnOrder;
	private int numSoldiersPlayed = 0;
	private int longestConsecutiveRoads = 0;
	
	private int roadsInHand = 15;
	private int settlementsInHand = 5;
	private int citiesInHand = 4;
	
	private HashMap<DevelopmentCard, Integer> devCards = new HashMap<DevelopmentCard, Integer>();
	private DataContainer.KeyedRelation<Intersection> ownedIntersections = null;
	private DataContainer.KeyedRelation<Edge> ownedEdges = null;
	private transient HashSet<HarborType> harborsOwned;
	public final transient Boolean isSync = true; 
	
	Player(PlayerColor pc, Board board, int turnOrder) {
		this(pc, board, turnOrder, null);
	}
	
	Player(PlayerColor pc, Board board, int turnOrder, String id) {
		this.color = pc;
		this.board = board;
		this.turnOrder = turnOrder;
		if (id == null)
			id = UUID.randomUUID().toString();
		this.id = id;
	}
	
	Player(Board board, Player p) {
		this(p.getPlayerColor(), board, p.getTurnOrder(), p.getId());
		for (Intersection inter : p.getOwnedIntersections().getAll())
			this.getOwnedIntersections().add(inter.getId());
		for (Edge e : p.getOwnedEdges().getAll())
			this.getOwnedEdges().add(e.getId());
		this.longestConsecutiveRoads = p.longestConsecutiveRoads;
		this.numSoldiersPlayed = p.numSoldiersPlayed;
		this.citiesInHand = p.citiesInHand;
		this.roadsInHand = p.roadsInHand;
		this.settlementsInHand = p.settlementsInHand;
		assert (this.getOwnedEdges().getAll().size() == 0 ||
				this.getOwnedEdges().getAll().get(0) ==  this.getBoard().getEdgeData().getEdge(this.getOwnedEdges().getAll().get(0).getId()));
	}

	String getId() {
		return this.id;
	}
	
	HashMap<DevelopmentCard, Integer> getDevCards() {
		return this.devCards;
	}
	
	protected DataContainer.KeyedRelation<Intersection> getOwnedIntersections() {
		if (this.ownedIntersections == null)
			this.ownedIntersections = new DataContainer.KeyedRelation<Intersection>(this.getBoard().getIntersectionData());
		if (this.ownedIntersections.getRawData() == null)
			this.ownedIntersections.setup(this.getBoard().getIntersectionData());
		return this.ownedIntersections;
	}
	
	protected DataContainer.KeyedRelation<Edge> getOwnedEdges() {
		if (this.ownedEdges == null)
			this.ownedEdges = new DataContainer.KeyedRelation<Edge>(this.getBoard().getEdgeData());
		if (this.ownedEdges.getRawData() == null)
			this.ownedEdges.setup(this.getBoard().getEdgeData());
		return this.ownedEdges;
	}
	
	void setBoard(BoardModel bm) {
		if (this.board != null)
			System.out.println("BAADD... tried to set board model on player when not already null");
		this.board = bm;
	}
	
	Set<HarborType> getHarborsOwned() {
		if (this.harborsOwned == null) {
			this.harborsOwned = new HashSet<HarborType>();
			for (Intersection inter : this.getOwnedIntersections().getAll())
				if (inter.getHarbor() != null)
					this.harborsOwned.add(inter.getHarbor());
		}
		return this.harborsOwned;
	}
	
	int getTradeRatio(CardType ct) {
		HarborType ht = ct.getHarborType();
		if (this.getHarborsOwned().contains(ht))
			return ht.getTradeRatio();
		if (this.getHarborsOwned().contains(HarborType.GENERIC))
			return HarborType.GENERIC.getTradeRatio();
		return HarborType.DEFAULT_TRADE_RATIO;		
	}
	
	Board getBoard() {
		return this.board;
	}
	
	int getTurnOrder() {
		return this.turnOrder;
	}
	
	void setTurnOrder(int turnNum) {
		this.turnOrder = turnNum;
	}
	
	CardCollection getHand() {
		return this.getBoard().getHandData().getHand(this.getPlayerColor());
	}
	
	PlayerColor getPlayerColor() {
		return this.color;
	}
	
	void addPlayedSoldier() {
		++this.numSoldiersPlayed;
		this.getBoard().checkLargestArmy(this);
	}
	
	int getNumSoldierPlayed() {
		return this.numSoldiersPlayed;
	}
	
	int getCitiesInHand() {
		return this.citiesInHand;
	}
	
	int getSettlementsInHand() {
		return this.settlementsInHand;
	}
	
	int getRoadsInHand() {
		return this.roadsInHand;
	}
	
	public void buyCity(int interId) {
		this.getHand().subtract(Purchases.PurchaseType.CITY.getPrice());
		this.placeCity(interId);
	}
	
	protected void placeCity(int interId) {
		assert (this.citiesInHand > 0);
		this.citiesInHand--;
		this.getBoard().addVictoryPoint(this);
		this.getBoard().placeCity(interId, this.getPlayerColor());
	}
	
	public void buySettlement(int interId) {
		this.getHand().subtract(Purchases.PurchaseType.SETTLEMENT.getPrice());
		this.placeSettlement(interId);
	}
	
	protected void placeSettlement(int interId) {
		assert (this.settlementsInHand > 0);
		this.settlementsInHand--;
		this.getBoard().addVictoryPoint(this);
		this.getOwnedIntersections().add(interId);
		this.getBoard().placeSettlement(interId, this.getPlayerColor());
	}
	
	public void buyRoad(int edgeId) {
		this.getHand().subtract(Purchases.PurchaseType.ROAD.getPrice());
		this.placeRoad(edgeId);
	}
	
	int getConsecutiveRoads() {
		return this.longestConsecutiveRoads;
	}
	
	protected void placeRoad(int edgeId) {
		assert (this.roadsInHand > 0);
		this.roadsInHand--;
		this.getOwnedEdges().add(edgeId);
		this.getBoard().placeRoad(edgeId, this.getPlayerColor());
		this.updateConsecutiveRoadCount(edgeId);
		this.getBoard().checkLongestRoad(this);
	}
	
	protected List<Edge> getChainedEdges(Edge e) {
		return this.getChainedEdges(e, new ArrayList<Edge>(), null);
	}
	
	protected List<Edge> getChainedEdges(Edge e, List<Edge> reusedList, Set<Integer> intersToExclude) {
		reusedList.clear();
		List<Edge> ret = reusedList;
		for (Intersection i : e.getIntersections())
			if (intersToExclude == null || !intersToExclude.contains(i.getId()))
				for (Edge eNeighbor : i.getEdges())
					if (eNeighbor.getId() != e.getId() && eNeighbor.getPlayerColor() == this.color)
						ret.add(eNeighbor);
		return ret;
	}
	
	private void updateConsecutiveRoadCount(Integer addedEdgeId) {
		// checks to see if the addition of a road has created a longest road segment.
		HashSet<Integer> visited = new HashSet<Integer>();
		ArrayList<Edge> reusedContainer = new ArrayList<Edge>();
		Edge startingEdge = this.getBoard().getEdgeData().getEdge(addedEdgeId);
		int numConsecutive = 1; 
		roadFindHelper(startingEdge, visited, numConsecutive, reusedContainer);
	}
	
	private void roadFindHelper(Edge e, Set<Integer> visited, int numConsecutive, List<Edge> reusedContainer) {
		/* 
		 * This is an instance of the longest path problem. 
		 * http://en.wikipedia.org/wiki/Longest_path_problem
		 * 
		 * The algorithm works by traversing the 'graph' of the edges and intersections (i.e. vertices), 
		 * remembering the intersections that it visits, and recursively branching at every split in a 
		 * path, only following paths that don't cross visited intersections.
		 * 
		 * Technically the longest path problem is generally NP-hard, and this algorithm here
		 * is theoretically exponential in the number of edges time-complexity wise, but...
		 * (1) We're limited to 15 roads
		 * (2) We don't search parts of the "graph" not connected to the newest road placed (b/c they 
		 * 		couldn't have created a new longest road)
		 * (3) If players are playing at all rationally, the branching factor should be pretty limited.
		 * 
		 * In practice, this seems to be perfectly fast.
		 * */
		List<Edge> nexts = this.getChainedEdges(e, reusedContainer, visited);
		if (nexts.size() == 0) {
			if (numConsecutive > this.longestConsecutiveRoads) {
				this.longestConsecutiveRoads = numConsecutive;
				System.out.println(this.getPlayerColor() + " has " + this.longestConsecutiveRoads + " longest road length");
			}
			return;
		}
		for (Intersection i: e.getIntersections())
			visited.add(i.getId());
		numConsecutive++;
		if (nexts.size() == 1) {
			// This is almost the same as else condition below, but there's the matter of saving some allocations.
			roadFindHelper(nexts.get(0), visited, numConsecutive, reusedContainer);
		}
		else {
			for (Edge next : new ArrayList<Edge>(nexts))
				roadFindHelper(next, new HashSet<Integer>(visited), numConsecutive, reusedContainer);
		}
	}
	
	public void buyDevCard() {
		this.getHand().subtract(Purchases.PurchaseType.DEVELOPMENT_CARD.getPrice());
		this.pullCard();
	}
	
	protected void pullCard() {
		this.getBoard().pullCard(this);
	}

	protected void addCard(DevelopmentCard devCard) {
		Integer numCards = this.devCards.get(devCard);
		if (numCards == null)
			numCards = 0;
		this.devCards.put(devCard, ++numCards);
	}
	
	protected void playCard(DevelopmentCard devCard) {
		assert (this.devCards.get(devCard) > 0);
		Integer curVal = this.devCards.get(devCard);
		if (--curVal == 0)
			this.devCards.remove(devCard);
		this.devCards.put(devCard, curVal);
		this.getBoard().playDevCard(this, devCard);
	}
	
	// intention for this: returns true if we should keep going, false means terminate,
	// we are waiting on some non-CPU player to make a move
	public abstract Boolean doTurn();

	public abstract Boolean doSetupTurn();
	
	/* Trade logic */
	
	public static class TradeResponse {
		
		public static TradeResponse rejectTrade() {
			TradeResponse tr = new TradeResponse();
			tr.wasRejected = true;
			return tr;
		}
		
		public static TradeResponse acceptTrade() {
			return new TradeResponse();
		}
		
		private Boolean isSync = true;
		private Boolean wasRejected = false;
		private CardCollection counterAsk = null;
		private CardCollection counterOffer = null;
		private String message = null;
		
		private TradeResponse() {} // the default, accept trade
		
		TradeResponse(String rejectMessage) {
			this.message = rejectMessage;
			this.wasRejected = true;
		}
		
		TradeResponse(Boolean isSync) {
			this.isSync = isSync;
		}
		
		Boolean shouldBreakExecution() {
			return !this.isSync;
		}
		
		Boolean wasTradeAccepted() {
			return (!wasRejected && this.counterOffer == null);
		}
		
		CardCollection getCounterAsk() {
			return this.counterAsk;
		}
		
		CardCollection getCounterOffer() {
			return this.counterOffer;
		}
		
		String getMessage() {
			return this.message;
		}
	}
	
	public Boolean askForTrade(Player partner, CardCollection askedFor, CardCollection offered) {
		return this.askForTrade(partner, askedFor, offered, true);
	}
	
	public Boolean askForTrade(Player partner, CardCollection askedFor, CardCollection offered, Boolean allowCounter) {
		TradeResponse response = partner.repondToTrade(askedFor, offered, allowCounter);
		if (response.shouldBreakExecution()) {
			// We are waiting on the other player to decide.
			// TODO: register with board state that we are waiting here.
			return false;
		}
		if (response.wasTradeAccepted()) {
			this.getHand().subtract(offered);
			this.getHand().add(askedFor);
			partner.getHand().add(offered);
			partner.getHand().subtract(askedFor);
		}
		else if (allowCounter)
			return partner.askForTrade(this, response.getCounterAsk(), response.getCounterOffer(), false);
		return this.isSync;
	}
	
	public abstract TradeResponse repondToTrade(CardCollection askedFor, CardCollection offered, Boolean allowCounter);
	
	protected static class RobberResponse {
		protected Player robbedPlayer = null;
		protected Hex movedToHex = null;
		protected Boolean shouldContinue = true;
		
		RobberResponse(Hex h, Player p) {
			this.robbedPlayer = p; this.movedToHex = h;
		}
		
		RobberResponse() {
			this.shouldContinue = false;
		}
	}
	
	public Boolean moveRobber() {
		Hex oldHex = this.getBoard().getHexData().getRobberHex();
		RobberResponse rr = this.doMoveRobber();
		assert(oldHex.getId() != rr.movedToHex.getId());
		if (!rr.shouldContinue)
			// HumanPlayer
			return false;
		// TODO: if they are equal, raise a "Client broke rules" error
		this.getBoard().moveAndRob(rr.movedToHex, this, rr.robbedPlayer);
		return true;
	}
	
	public abstract RobberResponse doMoveRobber();

	public Boolean doMonopoly() {
		CardType ct = this.chooseMonopoly();
		if (ct == null)
			// humanPlayer
			return false;
		this.getBoard().takeAll(ct, this);
		return true;
	}
	
	public abstract CardType chooseMonopoly();
	
	public boolean doChooseResource() {
		CardType ct = this.chooseResource();
		if (ct == null)
			return false;
		this.getHand().add(ct, 1);
		return true;
	}
	
	public abstract CardType chooseResource();
	
	public abstract boolean chooseAndPlaceRoad();
	
	protected void doTradeWithSelf(CardType tradingIn, CardType gettingBack) {
		this.getHand().subtract(tradingIn, this.getTradeRatio(tradingIn)).addOne(gettingBack);
	}
}
