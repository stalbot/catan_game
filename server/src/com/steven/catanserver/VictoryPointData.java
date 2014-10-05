package com.steven.catanserver;

import java.util.HashMap;
import java.util.List;

public class VictoryPointData {
	
	public static final int MINIMUM_SOLDIERS = 3;
	public static final int MINIMUM_ROADS = 5;
	public static final int LARGEST_ARMY_VALUE = 2;
	public static final int LONGEST_ROAD_VALUE = 2;
	
	private int totalToWin = 10;
	private PlayerColor largestArmyPlayer = null;
	private PlayerColor longestRoadPlayer = null;
	private HashMap<PlayerColor, Integer> victoryPoints = new HashMap<PlayerColor, Integer>();
	
	VictoryPointData(int totalToWin, List<Player> players) {
		this.totalToWin = totalToWin;
		for (Player p : players) {
			this.victoryPoints.put(p.getPlayerColor(), 0);
		}
	}
	
	VictoryPointData(VictoryPointData vp) {
		this.totalToWin = vp.totalToWin;
		this.largestArmyPlayer = vp.largestArmyPlayer;
		this.longestRoadPlayer = vp.longestRoadPlayer;
		this.victoryPoints = new HashMap<PlayerColor, Integer>(vp.victoryPoints);
	}
	
	Boolean hasPlayerWon(PlayerColor p) {
		return this.victoryPoints.get(p) >= this.totalToWin;
	}

	public void addVictoryPoint(Player player) {
		int currentVPs = this.victoryPoints.get(player.getPlayerColor());
		this.victoryPoints.put(player.getPlayerColor(), currentVPs + 1);
	}
	
	PlayerColor getLargestArmyPlayer() {
		return this.largestArmyPlayer;
	}
	
	void setLargestArmyPlayer(PlayerColor pc) {
		int currentVPs;
		if (this.largestArmyPlayer != null) {
			currentVPs = this.victoryPoints.get(this.largestArmyPlayer);
			this.victoryPoints.put(this.largestArmyPlayer, currentVPs - LARGEST_ARMY_VALUE);
		}
		this.largestArmyPlayer = pc;
		currentVPs = this.victoryPoints.get(this.largestArmyPlayer);
		this.victoryPoints.put(this.largestArmyPlayer, currentVPs + LARGEST_ARMY_VALUE);
	}
	
	PlayerColor getLongestRoadPlayer() {
		return this.longestRoadPlayer;
	}
	
	void setLongestRoadPlayer(PlayerColor pc) {
		int currentVPs;
		if (this.longestRoadPlayer != null) {
			currentVPs = this.victoryPoints.get(this.longestRoadPlayer);
			this.victoryPoints.put(this.longestRoadPlayer, currentVPs - LONGEST_ROAD_VALUE);
		}
		this.longestRoadPlayer = pc;
		currentVPs = this.victoryPoints.get(this.longestRoadPlayer);
		this.victoryPoints.put(this.longestRoadPlayer, currentVPs + LONGEST_ROAD_VALUE);
	}
}
