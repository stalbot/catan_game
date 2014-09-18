package com.steven.catanserver;

import java.util.HashMap;
import java.util.Map.Entry;

public class CardCollection {
	private HashMap<CardType, Integer> cardCounts;
	
	CardCollection() {
		this.cardCounts = new HashMap<CardType, Integer>();
	}
	
	public int getCardCount(CardType card) {
		return this.cardCounts.get(card);
	}
	
	public HashMap<CardType, Integer> getCards() {
		// TODO: maybe don't keep this
		return this.cardCounts;
	}
	
	public Boolean canPurchase(CardCollection cards) {
		for (Entry<CardType, Integer> e : cards.getCards().entrySet())
			if (e.getValue() > this.cardCounts.get(e.getValue()))
				return false;
		return true;
	}
}
