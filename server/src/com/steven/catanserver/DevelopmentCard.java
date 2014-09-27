package com.steven.catanserver;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public enum DevelopmentCard {
	SOLDIER(14) {
		public Boolean play(Player player) {
			// TODO: increment largest army count
			return player.moveRobber();
		}
	}, 
	MONOPOLY(2) {
		public Boolean play(Player player) {
			return player.doMonopoly();
		}
	}, 
	VICTORY_POINT(5) {
		public void receive(Player player) {
			player.getBoard().addVictoryPoint(player);
		}
	}, 
	YEAR_OF_PLENTY(2) {
		public Boolean play(Player player) {
			// looks a little weird, but it is just choosing two resources, honoring clients
			if (player.doChooseResource())
				return player.doChooseResource();
			return false;
		}
	}, ROAD_BUILDING(2) {
		// same as above
		public Boolean play(Player player) {
			// looks a little weird, but it is just choosing two resources, honoring clients
			if (player.chooseRoadPlacement())
				return player.chooseRoadPlacement();
			return false;
		}
	};
	
	public static List<DevelopmentCard> getNewDevCardStack() {
		LinkedList<DevelopmentCard> devStack = new LinkedList<DevelopmentCard>();
		for (int i=0; i<DevelopmentCard.values().length; i++) {
			DevelopmentCard d = DevelopmentCard.values()[i];
			for (int j=0; j<d.total; j++)
				devStack.add(d);
		}
		Collections.shuffle(devStack);
		return devStack;
	}
	
	public final int total;
	
	public Boolean play(Player player) {
		return true;
	}
	
	public void receive(Player player) {}
	
	DevelopmentCard(int total) {
		this.total = total;
	}
}
