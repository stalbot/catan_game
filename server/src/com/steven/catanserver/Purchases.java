package com.steven.catanserver;

public final class Purchases {
	private static final CardType[] sCost = {CardType.GRAIN, CardType.WOOD, CardType.WOOL, CardType.BRICK};
	private static final int[] sCosti = {1, 1, 1, 1};
	private static final CardType[] cCost = {CardType.ORE, CardType.GRAIN};
	private static final int[] cCosti = {3, 2};
	private static final CardType[] rCost = {CardType.WOOD, CardType.BRICK};
	private static final int[] rCosti = {1, 1};
	private static final CardType[] dCost = {CardType.ORE, CardType.GRAIN, CardType.WOOL};
	private static final int[] dCosti = {1, 1, 1};
	
	public enum PurchaseType {
		// Put these in sorted order of total cost as a convenience for AI.
		CITY(cCost, cCosti), SETTLEMENT(sCost, sCosti), DEVELOPMENT_CARD(dCost, dCosti), ROAD(rCost, rCosti);

		private CardCollection costs;

		private PurchaseType(CardType[] cardTypes, int[] numCards) {
			this.costs = new CardCollection(cardTypes, numCards);
		}
		
		public CardCollection getPrice() {
			return this.costs;
		}
	}
}
