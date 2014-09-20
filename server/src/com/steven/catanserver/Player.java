package com.steven.catanserver;

import java.util.UUID;

public abstract class Player {
	
	private PlayerColor color;
	private transient BoardModel board;
	private String id = null;
	private int turnOrder;
	
	Player(PlayerColor pc, BoardModel board, int turnOrder) {
		this(pc, board, turnOrder, null);
	}
	
	Player(PlayerColor pc, BoardModel board, int turnOrder, String id) {
		this.color = pc;
		this.board = board;
		this.turnOrder = turnOrder;
		if (id == null)
			id = UUID.randomUUID().toString();
		this.id = id;
	}

	String getId() {
		return this.id;
	}
	
	void setBoard(BoardModel bm) {
		if (this.board != null)
			System.out.println("BAADD... tried to set board model on player when not null");
		this.board = bm;
	}
	
	BoardModel getBoard() {
		return this.board;
	}
	
	int getTurnOrder() {
		return this.turnOrder;
	}
	
	PlayerColor getPlayerColor() {
		return this.color;
	}
	
	void placeSettlement(Intersection inter) {
		this.getBoard().placeSettlement(inter, this);
	}
	
	void placeRoad(Edge edge) {
		this.getBoard().placeRoad(edge, this);
	}

	// intention for this: returns true if we should keep going, false means terminate,
	// we are waiting on some non-CPU player to make a move
	public abstract Boolean doTurn();

	public abstract Boolean doSetupTurn();
}
