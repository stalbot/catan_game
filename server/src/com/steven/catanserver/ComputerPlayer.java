package com.steven.catanserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.steven.catanserver.Purchases.PurchaseType;

public class ComputerPlayer extends Player {
	
	static final int HARBOR_ADJUSTMENT = 2;
	static final HashMap<CardType, Integer> maximumCardsInPurchase = new HashMap<CardType, Integer>();
	
	private static final int MINIMUM_MONOPOLY_CARDS = 2; 
	
	static {
		for (CardType ct : CardType.values()) {
			int maxVal = 0;
			for (PurchaseType pt : PurchaseType.values()) {
				int numCardsInPrice = pt.getPrice().getCardCount(ct);
				if (numCardsInPrice > maxVal)
					maxVal = numCardsInPrice;
			}
			maximumCardsInPurchase.put(ct, maxVal);
		}
	}

	public static int getValueOfPurchases(Collection<Purchases.PurchaseType> pts) {
		int cardVal = 0;
		for (Purchases.PurchaseType pt : pts)
			cardVal += pt.getPrice().getTotalCards();
		return cardVal;
	}
	
	public ComputerPlayer(PlayerColor pc, BoardModel board, int turnOrder) {
		super(pc, board, turnOrder);
	}
	
	protected static class PurchaseTypeEffectSorter implements Comparator<PurchaseType> {
		/* 
		 * Sorts purchase types based on the order at which they will effect each other on the board.
		 * Building a road could allow you to build a settlement, and building a settlement could
		 * allow you to build a city. (You can't play development cards on the turn you pull them,
		 * so they don't affect anything).
		 */

		private static final PurchaseType[] ordering = 
			{PurchaseType.ROAD, PurchaseType.SETTLEMENT, PurchaseType.CITY, PurchaseType.DEVELOPMENT_CARD};
		private static final HashMap<PurchaseType, Integer> lkup = new HashMap<PurchaseType, Integer>();
		static {
			for (int i=0; i<ordering.length; i++)
				lkup.put(ordering[i], i);
		}
		
		@Override
		public int compare(PurchaseType arg0, PurchaseType arg1) {
			return lkup.get(arg0) - lkup.get(arg1);
		}
		
	}
	
	private void doAllPossiblePurchases() {
		List<List<Purchases.PurchaseType>> allPts = this.getPossiblePurchases();
		ArrayList<Object> results;
		int bestSpend = 0;
		List<PurchaseType> bestPurchases = null;
		List<Object> bestResults = null;
		for (List<PurchaseType> purchases : allPts) {
			boolean canFulfillPurchases = true;
			int totalCardsSpent = 0;
			Collections.sort(purchases, new PurchaseTypeEffectSorter());
			results = new ArrayList<Object>();
			for (PurchaseType pt : purchases) {
				totalCardsSpent += pt.getPrice().getTotalCards();
				Object result = null;
				switch(pt) {
				case DEVELOPMENT_CARD:
					if (this.getBoard().getRemainingDevCards() > 0)
						result = true;
					break;
				case SETTLEMENT:
					result = this.getBestIntersectionForSettlement();
					break;
				case CITY:
					result = this.getBestIntersectionForCity();
					break;
				case ROAD:
					result = this.chooseRoadLocation();
					assert (result == null || ((Edge) result).canPlaceRoad());
					break;
				}
				if (result == null)
					canFulfillPurchases = false;
				results.add(result);
			}
			assert(purchases.size() == results.size());
			if (canFulfillPurchases) {
				this.executePurchases(purchases, results);
				return;
			}
			else {
				if (totalCardsSpent > bestSpend) {
					bestSpend = totalCardsSpent;
					bestPurchases = purchases;
					bestResults = results;
				}
			}
		}
		if (bestPurchases != null) {
			this.executePurchases(bestPurchases, bestResults);
		}
	}
	
	private void executePurchases(List<PurchaseType> purchases, List<Object> results) {
		assert(purchases.size() == results.size());
		for (int i=0, k=purchases.size(); i<k; i++) {
			Object purchaseObj = results.get(i);
			if (purchaseObj == null)
				// We weren't able to complete this purchase, but this is the best set we've got.
				continue;
			switch(purchases.get(i)) {
			case DEVELOPMENT_CARD:
				this.buyDevCard();
				break;
			case SETTLEMENT:
				this.buySettlement((Intersection) purchaseObj);
				break;
			case CITY:
				this.buyCity((Intersection) purchaseObj);
				break;
			case ROAD:
				assert(((Edge) purchaseObj).canPlaceRoad());
				this.buyRoad((Edge) purchaseObj);
				break;
			}
		}
	}

	@Override
	public Boolean doTurn() {
		System.out.println(this.getPlayerColor() + " starting turn.");
		System.out.println(this.getHand());
		
		this.doAllPossiblePurchases();
		
		// For each card in hand, trade down if possible to the maximum amount we can spend on single purchase.
		for (Entry<CardType, Integer> ctEntry : this.getHand().getCards().entrySet()) {
			while (this.getTradeRatio(ctEntry.getKey()) + maximumCardsInPurchase.get(ctEntry.getKey()) <= ctEntry.getValue()) {
				CardType tradeTarget = this.getHand().getLeastCommonCards().iterator().next();
				this.doTradeWithSelf(ctEntry.getKey(), tradeTarget);
			}
		}
		
		// maybe we lucked into something, try one more time.
		this.doAllPossiblePurchases();
		return true;
	}
	
	static int getTotalProbability(Intersection inter) {
		int sum = 0;
		for (Hex hex : inter.getHexes())
			sum += hex.getRollProbability();
		return sum;
	}
	
	static int getTotalAdjustedProbability(Intersection inter) {
		return getTotalProbability(inter) + (inter.getHarbor() != null ? HARBOR_ADJUSTMENT : 0);
	}
	
	Intersection getBestIntersectionForCity() {
		int maxProb = 0;
		Intersection bestInter = null;
		for (Intersection inter : this.getOwnedIntersections().getAll()) {
			// For cities, harbors don't matter, don't adjust.
			int currentProb = getTotalProbability(inter);
			if (currentProb >  maxProb && inter.canPlaceSettlement()) {
				maxProb = currentProb;
				bestInter = inter;
			}
		}
		return bestInter;
	}
	
	Intersection getBestIntersectionForSettlement() {
		int maxProb = 0;
		Intersection bestInter = null;
		for (Intersection inter : this.getBoard().getIntersections()) {
			// For settlements, harbors matter so adjust prob.
			int currentProb = getTotalAdjustedProbability(inter);
			if (currentProb >  maxProb && inter.canPlaceSettlement()) {
				maxProb = currentProb;
				bestInter = inter;
			}
		}
		return bestInter;
	}

	@Override
	public Boolean doSetupTurn() {
		System.out.println(this.getPlayerColor() + " doing setup.");
		// Really the dumbest thing we can do here. 
		// Just get something working, and probably not the worst strategy.
		Intersection bestInter = this.getBestIntersectionForSettlement();
		this.placeSettlement(bestInter);
		
		// totally stupid about this
		this.placeRoad(bestInter.getEdges().iterator().next());
		// for debugging
//		for (Edge e : bestInter.getEdges())
//			this.placeRoad(e);
		
		return true;
	}

	@Override
	public TradeResponse repondToTrade(CardCollection askedFor, CardCollection offered, Boolean allowCounter) {
		// Doesn't make counter offers, that's too hard for this dumb one.
		int currentValue = ComputerPlayer.getValueOfPurchases(this.getBestPossiblePurchases());
		CardCollection newHand = this.getHand().cloneHand().subtract(askedFor).add(offered);
		int newValue = ComputerPlayer.getValueOfPurchases(this.getBestPossiblePurchases(newHand));
		if (newValue > currentValue)
			return TradeResponse.acceptTrade();
		return TradeResponse.rejectTrade();
	}

	@Override
	public RobberResponse doMoveRobber() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private class CardSorter implements Comparator<Entry<CardType, Integer>> {

		@Override
		public int compare(Entry<CardType, Integer> arg0,
				Entry<CardType, Integer> arg1) {
			return arg0.getValue() - arg1.getValue();
		}
		
	}
	
	protected List<Entry<CardType, Integer>> getOtherPlayerCardTotalsSorted() {
		HashMap<CardType, Integer> counts = new HashMap<CardType, Integer>();
		for (Player p : this.getBoard().getPlayers()) {
			if (p.getPlayerColor() == this.getPlayerColor())
				continue;
			for (Entry<CardType, Integer> e : p.getHand().getCards().entrySet()) {
				if (!counts.containsKey(e.getKey()))
					counts.put(e.getKey(), 0);
				counts.put(e.getKey(), counts.get(e.getKey()) + e.getValue());
			}
		}
		ArrayList<Entry<CardType, Integer>> cardCounts = new ArrayList<Entry<CardType, Integer>>(counts.entrySet());
		Collections.sort(cardCounts, new CardSorter());
		Collections.reverse(cardCounts);
		return cardCounts;
	}

	@Override
	public CardType chooseMonopoly() {
		List<Entry<CardType, Integer>> cardCounts =  this.getOtherPlayerCardTotalsSorted();
		if (cardCounts.size() == 0)
			return null;
		Entry<CardType, Integer> bestChoice = cardCounts.get(0);
		if (bestChoice.getValue() < MINIMUM_MONOPOLY_CARDS)
			return null;
		return bestChoice.getKey();
	}

	@Override
	public CardType chooseResource() {
		// also being dumb, this is the dumb AI after all.
		return this.getHand().getLeastCommonCards().iterator().next();
	}
	
	protected Collection<Edge> getPossibleRoadPlacements() {
		LinkedList<Edge> edges = new LinkedList<Edge>();
		for (Edge e : this.getOwnedEdges().getAll())
			for (Intersection i : e.getIntersections())
				if (i.getSettlementColor() == null || i.getSettlementColor() == this.getPlayerColor())
					for (Edge eNeighbor : i.getAllNeighboringEdges())
						if (eNeighbor.canPlaceRoad())
							edges.add(eNeighbor);
		return edges;
	}
	
	protected Edge chooseRoadLocation() {
		// yes, not the most efficient, but this is the dumb one
		Edge anyEdge = null;
		// basically, go through all the edges on which we can place a road,
		// if we find one that could lead to a settlement being placed, do that
		// otherwise just return anything. (or null, if we found no placements)
		for (Edge e : this.getPossibleRoadPlacements()) {
			boolean neighborsSettlement = false;
			for (Intersection i : e.getIntersections())
				if (!i.canPlaceSettlement())
					neighborsSettlement = true;
			if (!neighborsSettlement)
				return e;
			anyEdge = e;
		}
		return anyEdge;
	}

	@Override
	public boolean chooseAndPlaceRoad() {
		Edge e = this.chooseRoadLocation();
		this.placeRoad(e);
		return true;
	}

	protected List<List<Purchases.PurchaseType>> getPossiblePurchases() {
		return this.getPossiblePurchases(this.getHand());
	}
	
	protected class PurchaseSearchState {
		CardCollection hand;
		LinkedList<Purchases.PurchaseType> purchases = new LinkedList<Purchases.PurchaseType>();
		
		PurchaseSearchState(CardCollection hand) {
			this.hand = hand.cloneHand();
		}
		
		PurchaseSearchState newWithPurchase(Purchases.PurchaseType pt) {
			PurchaseSearchState newPss = new PurchaseSearchState(this.hand);
			newPss.hand.subtract(pt.getPrice());
			newPss.purchases = new LinkedList<Purchases.PurchaseType>(this.purchases);
			newPss.purchases.add(pt);
			return newPss;
		}
	}
	
	protected List<List<Purchases.PurchaseType>> getPossiblePurchases(CardCollection hand) {
		// Use depth-first search to enumerate all the possible maximal combos of purchases.
		// E.g. if it is possible to buy two roads, the implied possibility of buying just one road
		// will not be enumerated.
		PurchaseSearchState startState = new PurchaseSearchState(this.getHand());
		LinkedList<PurchaseSearchState> frontier = new LinkedList<PurchaseSearchState>();
		LinkedList<List<Purchases.PurchaseType>> possibilities = new LinkedList<List<Purchases.PurchaseType>>();
		frontier.add(startState);
		while (frontier.size() > 0) {
			PurchaseSearchState currentState = frontier.removeLast();
			// Assuming Purchases.PurchaseType is enumerated in terms of total cost
			boolean isLeafNode = true;
			boolean pastMostRecent = false;
			for (Purchases.PurchaseType pt : Purchases.PurchaseType.values()) {
				// to avoid repeated states, only allow ourselves to look at things 
				// we have not already had a chance to add to hand (since order of 
				// steps doesn't matter in results, we can choose a single order)
				pastMostRecent = pastMostRecent || (currentState.purchases.size() == 0 || pt == currentState.purchases.getLast());
				if (!pastMostRecent) {
					continue;
				}
				if (currentState.hand.canPurchase(pt.getPrice())) {
					frontier.add(currentState.newWithPurchase(pt));
					isLeafNode = false;
				}
			}
			if (isLeafNode)
				possibilities.add(currentState.purchases);
		}
		return possibilities;
	}
	
	protected List<Purchases.PurchaseType> getBestPossiblePurchases() {
		return this.getPossiblePurchases().get(0);
	}
	
	protected List<Purchases.PurchaseType> getBestPossiblePurchases(CardCollection hand) {
		return this.getPossiblePurchases(hand).get(0);
	}
}
