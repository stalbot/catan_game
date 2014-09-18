package com.steven.catanserver;

public class ComputerPlayer extends Player {

	public ComputerPlayer(PlayerColor pc, BoardModel board, int turnOrder) {
		super(pc, board, turnOrder);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Boolean doTurn() {
		// TODO: implement
		return true;
	}
	
	static int getTotalProbability(Intersection inter) {
		int sum = 0;
		for (Hex hex : inter.getHexes())
			sum += hex.getRollProbability();
		return sum;
	}

	@Override
	public Boolean doSetupTurn() {
		System.out.println(this.getPlayerColor() + " doing setup.");
		// Really the dumbest thing we can do here. 
		// Just get something working, and probably not the worst strategy.
		int maxProb = 0;
		for (Intersection inter : this.getBoard().getIntersections()) {
			if (getTotalProbability(inter) >  maxProb && inter.canPlaceSettlement())
				this.placeSettlement(inter);
		}	
		return true;
	}

}
