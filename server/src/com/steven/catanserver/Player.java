package com.steven.catanserver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class Player {
	
	private PlayerColor color;
	private transient BoardModel board;
	private String id = null;
	private int turnOrder;
	private HashMap<DevelopmentCard, Integer> devCards = new HashMap<DevelopmentCard, Integer>();
	private DataContainer.KeyedRelation<Intersection> ownedIntersections = null;
	private DataContainer.KeyedRelation<Edge> ownedEdges = null;
	private transient HashSet<HarborType> harborsOwned;
	public final transient Boolean isSync = true; 
	
	Player(PlayerColor pc, BoardModel board, int turnOrder) {
		this(pc, board, turnOrder, null);
	}
	
	Player(PlayerColor pc, BoardModel board, int turnOrder, String id) {
		this.color = pc;
		this.board = board;
		this.turnOrder = turnOrder;
		if (id == null)
			id = UUID.randomUUID().toString();
		this.id = id;
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
	
	BoardModel getBoard() {
		return this.board;
	}
	
	int getTurnOrder() {
		return this.turnOrder;
	}
	
	CardCollection getHand() {
		return this.getBoard().getHandData().getHand(this.getPlayerColor());
	}
	
	PlayerColor getPlayerColor() {
		return this.color;
	}
	
	public void buyCity(Intersection inter) {
		this.getHand().subtract(Purchases.PurchaseType.CITY.getPrice());
		this.placeCity(inter);
	}
	
	protected void placeCity(Intersection inter) {
		assert (inter.canPlaceCity() && inter.getSettlementColor() != null && inter.getSettlementColor()  == this.getPlayerColor());
		this.getBoard().addVictoryPoint(this);
		this.getBoard().placeCity(inter, this);
	}
	
	public void buySettlement(Intersection inter) {
		this.getHand().subtract(Purchases.PurchaseType.SETTLEMENT.getPrice());
		this.placeSettlement(inter);
	}
	
	protected void placeSettlement(Intersection inter) {
		this.getBoard().addVictoryPoint(this);
		this.getOwnedIntersections().add(inter);
		this.getBoard().placeSettlement(inter, this);
	}
	
	public void buyRoad(Edge edge) {
		this.getHand().subtract(Purchases.PurchaseType.ROAD.getPrice());
		this.placeRoad(edge);
	}
	
	protected void placeRoad(Edge edge) {
		this.getOwnedEdges().add(edge);
		this.getBoard().placeRoad(edge, this);
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
	
	protected void doPlayCard(DevelopmentCard devCard) {
		assert (this.devCards.get(devCard) > 0);
		Integer curVal = this.devCards.get(devCard);
		if (--curVal == 0)
			this.devCards.remove(devCard);
		this.devCards.put(devCard, --curVal);
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
