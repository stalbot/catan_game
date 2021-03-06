package com.steven.catanserver;

public enum TurnEvent {
	TURN_START, TURN_SETUP_START, INTERSECTION_CHANGE, EDGE_CHANGE, TRADE_EVENT, TRADE_PROPOSAL_EVENT, 
		DEV_CARD_PULL_EVENT, WIN_EVENT, ROBBER_MOVE_EVENT, DEV_CARD_PLAY_EVENT, ROLL_EVENT;

	/* Simple classes to handle packaging of event data. */
	
	public static class TurnStartEvent {
		String eventType = TURN_START.toString();
		Player player;
		
		TurnStartEvent(Player p) {
			this.player = p;
		}
	}
	
	public static class TurnSetupStartEvent {
		String eventType = TURN_SETUP_START.toString();
		Player player;
		
		TurnSetupStartEvent(Player p) {
			this.player = p;
		}
	}
	
	public static class IntersectionChangeEvent {
		String eventType = INTERSECTION_CHANGE.toString();
		Player player;
		Intersection intersection;
		
		IntersectionChangeEvent(Player p, Intersection i) {
			this.player = p;
			this.intersection = i;
		}
	}

	public static class EdgeChangeEvent {
		String eventType = EDGE_CHANGE.toString();
		Player player;
		Edge edge;
		
		EdgeChangeEvent(Player p, Edge e) {
			this.player = p;
			this.edge = e;
		}
	}
	
	public static class TradeEvent {
		String eventType = TRADE_EVENT.toString();
		Player proposer;
		Player partner;
		CardCollection given;
		CardCollection received;
		
		TradeEvent(Player p0, Player p1, CardCollection c0, CardCollection c1) {
			this.proposer = p0;
			this.partner = p1;
			this.given = c0;
			this.received = c1;
		}
	}
	
	public static class TradeProposalEvent {
		String eventType = TRADE_PROPOSAL_EVENT.toString();
		Player proposer;
		Player partner;
		CardCollection given;
		CardCollection received;
		
		TradeProposalEvent(Player p0, Player p1, CardCollection c0, CardCollection c1) {
			this.proposer = p0;
			this.partner = p1;
			this.given = c0;
			this.received = c1;
		}
	}

	public static class DevCardPullEvent {
		String eventType = DEV_CARD_PULL_EVENT.toString();
		Player player;
		DevCardPullEvent(Player p) {
			this.player = p;
		}
	}
	
	public static class WinEvent {
		String eventType = WIN_EVENT.toString();
		Player player;
		WinEvent(Player p) {
			this.player = p;
		}
	}
	
	public static class RobberMoveEvent {
		String eventType = ROBBER_MOVE_EVENT.toString();
		Player robbingPlayer;
		Hex newHex;
		Player robbedPlayer;
		RobberMoveEvent(Hex movedTo, Player robbing, Player robbed) {
			this.newHex = movedTo;
			this.robbingPlayer = robbing;
			this.robbedPlayer = robbed;
		}
	}
	
	public static class DevCardPlayEvent {
		String eventType = DEV_CARD_PLAY_EVENT.toString();
		Player player;
		DevelopmentCard devCard;
		DevCardPlayEvent(Player p, DevelopmentCard dc) {
			this.player = p;
			this.devCard = dc;
		}
	}

	public static class RollEvent {
		String eventType = ROLL_EVENT.toString();
		int numberRolled;
		RollEvent(int n) {
			this.numberRolled = n;
		}
	}

	
}
