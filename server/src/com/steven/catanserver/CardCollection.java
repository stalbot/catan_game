package com.steven.catanserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
		Integer count = this.cardCounts.get(card);
		if (count == null)
			count = 0;
		return count;
	}
	
	public HashMap<CardType, Integer> getCards() {
		// TODO: maybe don't keep this
		return this.cardCounts;
	}
	
	public Boolean canPurchase(CardCollection cards) {
		for (Entry<CardType, Integer> e : cards.getCards().entrySet()) {
			Integer numCards = this.cardCounts.get(e.getKey());
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
	
	public CardCollection subtract(CardCollection cards) {
		for (Entry<CardType, Integer> e : cards.getCards().entrySet()) {
			int newCount = this.cardCounts.get(e.getKey()) -  e.getValue();
			assert(newCount >= 0);
			this.cardCounts.put(e.getKey(), newCount);
		}
		return this;
	}
	
	public CardCollection subtract(CardType ct, int numCards) {
		Integer current = this.cardCounts.get(ct);
		assert (current >= numCards);
		current = (current == null) ? 0 : current;
		this.cardCounts.put(ct, current - numCards);
		return this;
	}
	
	public int takeAll(CardType ct) {
		Integer current = this.cardCounts.get(ct);
		this.cardCounts.put(ct, 0);
		return (current == null) ? 0 : current;
	}
	
	public CardCollection add(CardCollection cards) {
		for (Entry<CardType, Integer> e : cards.getCards().entrySet()) {
			int newCount = this.cardCounts.get(e.getKey()) +  e.getValue();
			this.cardCounts.put(e.getKey(), newCount);
		}
		return this;
	}
	
	public CardCollection add(CardType ct, int numCards) {
		Integer current = this.cardCounts.get(ct);
		current = (current == null) ? 0 : current;
		this.cardCounts.put(ct, current + numCards);
		return this;
	}
	
	public CardCollection addOne(CardType ct) {
		int newVal = 1;
		if (this.cardCounts.get(ct) != null)
			newVal = this.cardCounts.get(ct) + 1;
		this.cardCounts.put(ct, newVal);
		return this;
	}
	
	public CardCollection cloneHand() {
		CardCollection newHand = new CardCollection();
		newHand.cardCounts = new HashMap<CardType, Integer>(this.cardCounts);
		return newHand;
	}	
	
	public int getTotalCards() {
		int sum = 0;
		for (Entry<CardType, Integer> e : this.cardCounts.entrySet())
			sum += e.getValue();
		return sum;
	}
	
	public Collection<CardType> getLeastCommonCards() {
		Integer minVal = null;
		ArrayList<CardType> cts = new ArrayList<CardType>();
		for (Entry<CardType, Integer> e : this.cardCounts.entrySet()) {
			int value = (e.getValue() == null) ? 0 : e.getValue();
			if (minVal == null || value < minVal) {
				minVal = value;
				cts.clear();
				cts.add(e.getKey());
			}
			else if (value == minVal)
				cts.add(e.getKey());
		}
		return cts;
	}
	
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this.cardCounts).toString();
	}
}
