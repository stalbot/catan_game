package com.steven.catanserver;

public enum TurnEvent {
	TURN_START, TURN_SETUP_START, INTERSECTION_CHANGE, EDGE_CHANGE, TRADE_EVENT, TRADE_PROPOSAL_EVENT;

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

}
