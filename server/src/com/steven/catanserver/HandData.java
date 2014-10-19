package com.steven.catanserver;

import java.util.*;
import java.util.Map.Entry;

public class HandData {
	
	private HashMap<PlayerColor, CardCollection> hands;
	
	HandData(List<Player> players) {
		this.hands = new HashMap<PlayerColor, CardCollection>();
		for (Player p : players) {
			this.hands.put(p.getPlayerColor(), new CardCollection());
		}
	}
	
	public HandData(HandData handData) {
		this.hands = new HashMap<PlayerColor, CardCollection>();
		for (Entry<PlayerColor, CardCollection> e : handData.hands.entrySet())
			this.hands.put(e.getKey(), new CardCollection(e.getValue()));
	}

	public CardCollection getHand(PlayerColor player) {
		return this.hands.get(player);
	}
	
}
