package com.steven.catanserver;

import java.util.HashMap;

public class HandData {
	
	private HashMap<PlayerColor, CardCollection> hands;
	
	HandData() {
		this.hands = new HashMap<PlayerColor, CardCollection>();
	}
	
	public CardCollection getHand(PlayerColor player) {
		return this.hands.get(player);
	}
	
}
