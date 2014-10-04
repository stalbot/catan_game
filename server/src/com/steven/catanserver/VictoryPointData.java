package com.steven.catanserver;

import java.util.HashMap;
import java.util.List;

public class VictoryPointData {
	
	private int totalToWin = 10;
	private HashMap<PlayerColor, Integer> victoryPoints = new HashMap<PlayerColor, Integer>();
	
	VictoryPointData(int totalToWin, List<Player> players) {
		this.totalToWin = totalToWin;
		for (Player p : players) {
			this.victoryPoints.put(p.getPlayerColor(), 0);
		}
	}
	
	VictoryPointData(VictoryPointData vp) {
		this.totalToWin = vp.totalToWin;
		this.victoryPoints = new HashMap<PlayerColor, Integer>(vp.victoryPoints);
	}
	
	Boolean hasPlayerWon(PlayerColor p) {
		return this.victoryPoints.get(p) >= this.totalToWin;
	}

	public void addVictoryPoint(Player player) {
		int currentVPs = this.victoryPoints.get(player.getPlayerColor());
		this.victoryPoints.put(player.getPlayerColor(), currentVPs + 1);
	}
	
}
