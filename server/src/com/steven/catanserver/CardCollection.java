package com.steven.catanserver;

import java.util.HashMap;
import java.util.Random;
import java.util.Map.Entry;

public class CardCollection {
	private HashMap<CardType, Integer> cardCounts;
	
	CardCollection() {
		this.cardCounts = new HashMap<CardType, Integer>();
	}
	
	CardCollection(CardType[] cardTypes, int[] numCards) {
		// This method useful for easy static instantiation
		assert(cardTypes.length == numCards.length);
		this.cardCounts = new HashMap<CardType, Integer>();
		for (int i=0; i<cardTypes.length; i++) {
			this.cardCounts.put(cardTypes[i], numCards[i]);
		}
	}
	
	public int getCardCount(CardType card) {
		return this.cardCounts.get(card);
	}
	
	public HashMap<CardType, Integer> getCards() {
		// TODO: maybe don't keep this
		return this.cardCounts;
	}
	
	public Boolean canPurchase(CardCollection cards) {
		for (Entry<CardType, Integer> e : cards.getCards().entrySet()) {
			Integer numCards = this.cardCounts.get(e.getValue());
			if (numCards == null)
				return false;
			if (e.getValue() > numCards)
				return false;
		}
		return true;
	}
	
	public CardType popRandom() {
		// Trying to do this without instantiating new data structure.
		int totalCards = 0;
		// OK, this does make a new data structure, but it woudln't have to if Java were better.
		for (Integer i : this.cardCounts.values()) 
			totalCards += i;
		int rand = new Random().nextInt(totalCards);
		int totalSeen = 0;
		for (Entry<CardType, Integer> e : this.cardCounts.entrySet()) {
			totalSeen += e.getValue();
			if (totalSeen >= rand) {
				e.setValue(e.getValue() - 1);
				return e.getKey();
			}
		}
		assert(false);
		return null;
	}
	
	public void subtract(CardCollection cards) {
		for (Entry<CardType, Integer> e : cards.getCards().entrySet()) {
			int newCount = this.cardCounts.get(e.getKey()) -  e.getValue();
			assert(newCount >= 0);
			this.cardCounts.put(e.getKey(), newCount);
		}
	}
	
	public int takeAll(CardType ct) {
		Integer current = this.cardCounts.get(ct);
		this.cardCounts.put(ct, 0);
		return (current == null) ? 0 : current;
	}
	
	public void add(CardCollection cards) {
		for (Entry<CardType, Integer> e : cards.getCards().entrySet()) {
			int newCount = this.cardCounts.get(e.getKey()) +  e.getValue();
			this.cardCounts.put(e.getKey(), newCount);
		}
	}
	
	public void add(CardType ct, int numCards) {
		Integer current = this.cardCounts.get(ct);
		current = (current == null) ? 0 : current;
		this.cardCounts.put(ct, current + numCards);
	}
	
	public void addOne(CardType ct) {
		int newVal = 1;
		if (this.cardCounts.get(ct) != null)
			newVal = this.cardCounts.get(ct) + 1;
		this.cardCounts.put(ct, newVal);
	}
}
