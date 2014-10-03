package com.steven.catanserver;

import java.util.*;

public class HandData {
	
	private HashMap<PlayerColor, CardCollection> hands;
	
	HandData(List<Player> players) {
		this.hands = new HashMap<PlayerColor, CardCollection>();
		for (Player p : players) {
			this.hands.put(p.getPlayerColor(), new CardCollection());
		}
	}
	
	public CardCollection getHand(PlayerColor player) {
		return this.hands.get(player);
	}
	
}
