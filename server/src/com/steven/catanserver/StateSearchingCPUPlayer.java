package com.steven.catanserver;

import java.util.*;
import java.util.Map.Entry;

import com.steven.catanserver.Purchases.PurchaseType;

public class StateSearchingCPUPlayer extends StateAwareCPUPlayer {
	
	private static final double CARDS_PER_VP = 6;
	
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

	protected double getStateValue(State state) {
//		double stateVal = -this.getExpectedTurnsToWinSimple(state, 0) + 1000;
		double stateVal = -this.getExpectedTurnsToWinBetter(state) + 1000;
		state.setValue(stateVal);
//		System.out.println(state);
		return stateVal;
	}

	protected double getExpectedTurnsToWinSimple(State state, double bonus) {
		Player player = state.getPlayer();
		Board b = state.getBoard();
		// A simple, but not admissible, heuristic that guesses the total needed to win
		// based on a fixed card -> VP tradeoff
		// It is not admissible because it could in theory overestimate the turns needed to win,
		// not only because you could get lucky and pull a bunch of Victory Point development cards,
		// but because it does not take into account the fact that purchasing more settlements 
		// increases your resource acquisition rate.
		double startingVPValue = player.getNumVPs() + (bonus + state.getExpectedExtraCards() + player.getHand().getTotalCards()) / CARDS_PER_VP;
		double numVPsNeeded = b.getVPData().getTotalToWin() - startingVPValue;
//		System.out.println("VPs to win " + numVPsNeeded);
		if (numVPsNeeded <= 0)
			return 0;
		
		double expectedCardsPerTurn = 0;
		for (Intersection i : player.getOwnedIntersections().getAll())
			for (Hex h : i.getHexes())
				expectedCardsPerTurn += (i.getIsCity() ? 2 : 1) * h.getRollProbability();
		expectedCardsPerTurn /= 36;
		
		double turnsPerVP = CARDS_PER_VP / expectedCardsPerTurn;
//		System.out.println("Estimated " + (numVPsNeeded * turnsPerVP) + " turns needed to win for player " + player.getPlayerColor());
		return numVPsNeeded * turnsPerVP;
	}
	
	protected double getExpectedTurnsToWinBetter(State state) {
		int freeInters = 0;
		Player player = state.getPlayer();
		HashSet<Integer> interIds = new HashSet<Integer>();
		for (Edge e : player.getOwnedEdges().getAll()) {
			for (Intersection i : e.getIntersections())
				if (!interIds.add(i.getId()) && i.canPlaceCity())
					freeInters++;
		}
		
		double bonus = 0;
		bonus += freeInters * 2.5;
		bonus += player.getNumDevCards() * 3.5;
		return this.getExpectedTurnsToWinSimple(state, bonus);
	}
	
}
