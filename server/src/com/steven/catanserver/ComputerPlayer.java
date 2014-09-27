package com.steven.catanserver;

public class ComputerPlayer extends Player {

	public ComputerPlayer(PlayerColor pc, BoardModel board, int turnOrder) {
		super(pc, board, turnOrder);
	}

	@Override
	public Boolean doTurn() {
		// TODO: implement
		
		// don't take down the system if this gets called.
		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {}
		
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
		Intersection bestInter = null;
		for (Intersection inter : this.getBoard().getIntersections()) {
			int currentProb = getTotalProbability(inter);
			if (currentProb >  maxProb && inter.canPlaceSettlement()) {
				maxProb = currentProb;
				bestInter = inter;
			}
		}
		this.placeSettlement(bestInter);
		
		// totally stupid about this
		this.placeRoad(bestInter.getEdges().iterator().next());
		// for debugging
//		for (Edge e : bestInter.getEdges())
//			this.placeRoad(e);
		
		return true;
	}

	@Override
	public TradeResponse repondToTrade(CardCollection askedFor, CardCollection offered, Boolean allowCounter) {
		return TradeResponse.rejectTrade();
	}

	@Override
	public RobberResponse doMoveRobber() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CardType chooseMonopoly() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CardType chooseResource() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean chooseRoadPlacement() {
		// TODO Auto-generated method stub
		return false;
	}

}
