package com.steven.catanserver;

import java.util.HashMap;

public class VictoryPointData {
	
	private int totalToWin = 10;
	private HashMap<PlayerColor, Integer> victoryPoints = new HashMap<PlayerColor, Integer>();
	
	VictoryPointData(int totalToWin) {
		this.totalToWin = totalToWin;
	}
	
	Boolean hasPlayerWon(PlayerColor p) {
		return this.victoryPoints.get(p) >= this.totalToWin;
	}
	
}
