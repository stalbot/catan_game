package com.steven.catanserver;

import java.util.*;
import java.util.Map.Entry;

import com.steven.catanserver.Purchases.PurchaseType;

public class StateSearchingCPUPlayer extends StateAwareCPUPlayer {
	
	private static final int MAX_TURNS_TO_LOOK = 500;
	private static final double DEV_CARD_VP_VAL = 0.5; 
	private static final double ROAD_VP_VAL = 0.2;
	private static final double SETTLEMENT_VP_VAL = 0.9; // reflective of needing a road
	private static final double CITY_VP_VAL = 0.9; // reflective of needing a settlement
	private static final HashMap<PurchaseType, Double> heuristicPurchaseValues = new HashMap<PurchaseType, Double>();
	static {
		heuristicPurchaseValues.put(PurchaseType.DEVELOPMENT_CARD, DEV_CARD_VP_VAL);
		heuristicPurchaseValues.put(PurchaseType.SETTLEMENT, SETTLEMENT_VP_VAL);
		heuristicPurchaseValues.put(PurchaseType.CITY, CITY_VP_VAL);
		heuristicPurchaseValues.put(PurchaseType.ROAD, ROAD_VP_VAL);
	}

	public StateSearchingCPUPlayer(PlayerColor pc, Board board, int turnOrder) {
		super(pc, board, turnOrder);
	}
	
	public StateSearchingCPUPlayer(Board board, StateSearchingCPUPlayer player) {
		super(board, player);
	}
	
	ComputerPlayerType getPlayerType() {
		return ComputerPlayerType.STATE_SEARCHING_CPU_PLAYER;
	}
	
	protected static HashMap<CardType, Double> subtractHand(HashMap<CardType, Double> arg0, HashMap<CardType, Integer> arg1) {
		for (Entry<CardType, Integer> e : arg1.entrySet()) {
			arg0.put(e.getKey(), arg0.get(e.getKey()) -  e.getValue());
			assert(arg0.get(e.getKey()) >= 0); 
		}
		return arg0;
	}
	
	protected int getExpectedTurnsToWinStupid(Player p) {
		// As the name suggests, a very dumb way to try to determine how many turns to win.
		// Not only dumb in that it is a bad heuristic, but it's not even that easy to calculate.
		HashMap<CardType, Double> expectedCards = new HashMap<CardType, Double>();
		for (Entry<CardType, Integer> e : p.getHand().getCards().entrySet()) {
			expectedCards.put(e.getKey(), (double) e.getValue());
		}
		CardCollection fakeHand = new CardCollection();
		
		int numTurnsInFuture = 0;
		double currentVPs = (double) p.getNumVPs();
		HashMap<CardType, Double> expectedCardsPerTurn = new HashMap<CardType, Double>();
		for (CardType ct : CardType.values())
			expectedCardsPerTurn.put(ct, 0d);
		for (Intersection i : p.getOwnedIntersections().getAll())
			for (Hex h : i.getHexes()) {
				CardType ct = h.getType().getCardType();
				if (ct == null)
					continue;
				double incrementalVal = (i.getIsCity() ? 2 : 1) * h.getRollProbability() / 36;
				double oldVal = expectedCardsPerTurn.get(ct);
				double newVal = oldVal + incrementalVal;
				expectedCardsPerTurn.put(ct, newVal);
			}
		do {
			fakeHand.getCards().clear();
			for (Entry<CardType, Double> e : expectedCards.entrySet())
				fakeHand.getCards().put(e.getKey(), e.getValue().intValue());
			for (PurchaseType pt : PurchaseType.values()) {
				if (fakeHand.canPurchase(pt.getPrice())) {
					expectedCards = subtractHand(expectedCards, pt.getPrice().getCards());
				}
				currentVPs += heuristicPurchaseValues.get(pt);
			}
			if (currentVPs >= 10)
				return numTurnsInFuture;
			numTurnsInFuture += 1;
			for (Entry<CardType, Double> e : expectedCardsPerTurn.entrySet()) 
				expectedCards.put(e.getKey(), expectedCards.get(e.getKey()) + e.getValue());
		} while(numTurnsInFuture <= MAX_TURNS_TO_LOOK);
		return MAX_TURNS_TO_LOOK;
	}

	protected double getStateValue(State state) {
		double stateVal = -getExpectedTurnsToWinStupid(state.getPlayer());
		state.setValue(stateVal);
		return stateVal;
	}
	
}
