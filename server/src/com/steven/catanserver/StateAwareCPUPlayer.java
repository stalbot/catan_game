package com.steven.catanserver;

import java.util.*;
import java.util.concurrent.locks.*;

import com.steven.catanserver.Purchases.PurchaseType;

public class StateAwareCPUPlayer extends ComputerPlayer {
	
	public static final int NUM_CPUS_TO_USE = 4;
	
	public static final double HAND_CARDS_VAL = 0.1;
	public static final double EXP_CARDS_VAL = 0.4;
	public static final float DC_RESOURCE_EQUIVALENCE = 3.1f;

	public StateAwareCPUPlayer(PlayerColor pc, Board board, int turnOrder) {
		super(pc, board, turnOrder);
	}
	
	public StateAwareCPUPlayer(Board fake, StateAwareCPUPlayer stateAwareCPUPlayer) {
		super(fake, stateAwareCPUPlayer);
	}
	
	ComputerPlayerType getPlayerType() {
		return ComputerPlayerType.STATE_AWARE_CPU_PLAYER;
	}

	protected class TradeStateTransform implements StateTransform {

		@Override
		public Collection<State> transform(State state) {
			List<State> ret = new ArrayList<State>();
			for (List<PurchaseType> purchases : state.getPlayer().getPossiblePurchases()) {
				State newState = null;
				for (PurchaseType pt : purchases) {
					switch(pt) {
					case SETTLEMENT:
						newState = state.getPlayer().chooseSettlementLocation(state, false);
						break;
					case CITY:
						newState = state.getPlayer().chooseCityLocation(state);
						break;
					case DEVELOPMENT_CARD:
						if (state.getBoard().getRemainingDevCards() > 0)
							newState = state.newStateFromAction(new DevCardBuy());
						break;
					case ROAD:
						newState = state.getPlayer().chooseRoadLocation(state);
						break;
					}
				}
				// TODO: dev cards
				if (newState != null)
					ret.add(newState);
			}
			return ret;
		}
		
	}
	
	public Boolean doTurn() {
		State startState = new State(this.getBoard(), this);
		List<State> states = this.getPossibleSelfTradeStates(startState);
		System.out.println("Pre dev card, " + states.size() + " states");
		for (State st : new ArrayList<State>(states)) {
			states.addAll(st.getPlayer().getDevCardPlayStates(st));
		}
		System.out.println("Post dev card, " + states.size() + " states");
		List<State> possibleStates = new MultiThreadedStateReducer(new TradeStateTransform()).reduce(states);
		
		if (possibleStates.size() > 0) {
			State bestState = Collections.max(possibleStates);
			for (Action a : bestState.getActions())
				a.execute(this.getBoard(), this, false);
		}
		else
			System.out.println(this.getPlayerColor() + " didn't do anything!");
		return true;
	}
	
	public Boolean doSetupTurn() {
		State state = new State(this.getBoard(), this);
		State bestState = this.chooseSettlementLocation(state, true);
		assert (bestState.getActions().size() == 1);
		
		assert(bestState.getActions().getLast() instanceof SettlementPlace);
		SettlementPlace sp = (SettlementPlace) bestState.getActions().getLast();
		bestState.getActions().getLast().execute(this.getBoard(), this, false);
		
		Intersection inter = this.getBoard().getIntersectionData().getIntersection(sp.intersectionId);
		List<State> possibleRoadStates = new ArrayList<State>();
		for (Edge e : inter.getEdges())
			if (e.canPlaceRoad())
				possibleRoadStates.add(bestState.newStateFromAction(new RoadPlace(e.getId())));
		State bestRoadState = Collections.max(possibleRoadStates);
		bestRoadState.getActions().getLast().execute(this.getBoard(), this, false);
		return true;
	}
	
	protected List<List<PurchaseType>> getPossiblePurchases(State state) {
		return ((StateAwareCPUPlayer) state.getPlayer()).getPossiblePurchases();
	}
	
	protected double getStateValue(State state) {
		// does it make more sense to call this guy from the 'real' instance of the player
		// or generally to call from the instance of the player encapsulated in a State?
		
		// For now, this is just a rather arbitrary measure of utility of a state
		Player p = state.getPlayer();
		double valueFromHandCards = Math.log(p.getHand().getTotalCards() + state.getExpectedExtraCards()) * HAND_CARDS_VAL;
		valueFromHandCards = Math.max(valueFromHandCards, 0);
		double expectedCardsPerTurn = 0;
		for (Intersection i : p.getOwnedIntersections().getAll())
			for (Hex h : i.getHexes())
				expectedCardsPerTurn += h.getRollProbability() * (i.getIsCity() ? 2 : 1);
		double expectedCardsValue = Math.log(expectedCardsPerTurn) * EXP_CARDS_VAL;
		valueFromHandCards = Math.max(expectedCardsValue, 0);
		double totalValue = p.getNumVPs() + valueFromHandCards + expectedCardsValue;
		state.setValue(totalValue);
		return totalValue;
	}
	
	protected List<State> getDevCardPlayStates(State startState) {
		// TODO: they are not actually playing dev cards
		// Get the powerset! woohoo!
		List<State> states = new ArrayList<State>();
		for (DevelopmentCard dc : this.getPlayableDevCards()) {
			DevCardPlay dcp = new DevCardPlay(dc); // TODO: make sure this can be shared
			for (int i=0, k=states.size(); i<k; i++) {
				// Doing the old fashion way to avoid concurrent modification problem
				State st = states.get(i);
				states.add(st.newStateFromAction(dcp));
			}
			states.add(startState.newStateFromAction(dcp));
		}
		return states;
	}
	
	protected State chooseCityLocation(State state) {
		if (state.getPlayer().getCitiesInHand() <= 0)
			return state;
		double bestStateVal = 0;
		State bestState = state;
		
		for (Intersection i : ((StateAwareCPUPlayer) state.getPlayer()).getPossibleCityPlacements()) {
			State newState = state.newStateFromAction(new CityBuy(i.getId()));
			double stateVal = this.getStateValue(newState);
			if (stateVal > bestStateVal) {
				bestStateVal = stateVal;
				bestState = newState;
			}
		}	
		return bestState;
	}
	
	protected State chooseSettlementLocation(State state, boolean initial) {
		if (state.getPlayer().getSettlementsInHand() <= 0)
			return state;
		double bestStateVal = Double.NEGATIVE_INFINITY;  // Bleh?
		State bestState = state;
		for (Intersection i : ((StateAwareCPUPlayer) state.getPlayer()).getPossibleSettlementPlacements(initial)) {
			State newState;
			if(!initial)
				newState = state.newStateFromAction(new SettlementBuy(i.getId()));
			else
				newState = state.newStateFromAction(new SettlementPlace(i.getId()));
			double stateVal = this.getStateValue(newState);
			System.out.println("Value of old state: " + bestStateVal + ", value of new state " + stateVal);
			if (stateVal > bestStateVal) {
				bestStateVal = stateVal;
				bestState = newState;
			}
		}	
		return bestState;
	}
	
	protected State chooseRoadLocation(State state) {
		if (state.getPlayer().getRoadsInHand() <= 0)
			return state;
		double bestStateVal = 0;
		State bestState = state;
		for (Edge e : ((StateAwareCPUPlayer) state.getPlayer()).getPossibleRoadPlacements()) {
			State newState = state.newStateFromAction(new RoadBuy(e.getId()));
			double stateVal = this.getStateValue(newState);
			if (stateVal > bestStateVal) {
				bestStateVal = stateVal;
				bestState = newState;
			}
		}	
		return bestState;
	}
	
	protected List<State> getPossibleSelfTradeStates(State state) {
		return this.getPossibleSelfTradeStates(state, CardType.values()[0]);
	}
	
	private List<State> getPossibleSelfTradeStates(State state, CardType starter) {
		ArrayList<State> possibleStates = new ArrayList<State>();
		possibleStates.add(state);
		StateAwareCPUPlayer cpPlayer = (StateAwareCPUPlayer) state.getPlayer();
		for (CardType tradingIn : CardType.values()) {
			// Similar trick used elsewhere: preserve ordering to avoid repeated states.
			if (tradingIn.compareTo(starter) <= 0)
				continue;
			if (cpPlayer.getTradeRatio(tradingIn) <= cpPlayer.getHand().getCardCount(tradingIn)) {
				for (CardType tradingFor : CardType.values()) {
					if (tradingFor.compareTo(tradingIn) <= 0)
						continue;
					State newState = state.newStateFromAction(new SelfTrade(tradingIn, tradingFor));
					possibleStates.addAll(this.getPossibleSelfTradeStates(newState, tradingIn));
				}
			}
		}
		System.out.println("Found " + possibleStates.size() + " possible trade states.");
		return possibleStates;
	}
	
	/* A whole series of small classes used to represent the state of the 
	 * board and for the player to do basic reasoning.
	 *  */
	
	protected interface Action {
		// execute() invokes the action on the specified board and player,
		// returning any number of expected cards acquired that the deterministic state
		// infrastructure can't handle
		float execute(Board b, Player p, boolean isFake);
	}
	
	protected class RoadPlace implements Action {
		
		protected Integer edgeId = null;
		
		RoadPlace(Integer edgeId) {
			this.edgeId = edgeId;
		}

		@Override
		public float execute(Board b, Player p, boolean isFake) {
			assert (this.edgeId != null);
			p.placeRoad(edgeId);
			return 0;
		}
		
	}
	
	protected class RoadBuy extends RoadPlace {
		RoadBuy(Integer edgeId) {
			super(edgeId);
		}

		@Override
		public float execute(Board b, Player p, boolean isFake) {
			assert (this.edgeId != null);
			p.buyRoad(edgeId);
			return 0;
		}
		
	}
	
	protected class DevCardBuy implements Action {
		
		DevCardBuy() {
		}

		@Override
		public float execute(Board b, Player p, boolean isFake) {
			// Even more brutal than the DevCardPlay issue.
			// We don't know which dev card we will pull, and can't let the CPU cheat.
			// This state infrastructure also can't handle nondeterminism, so 
			// we just pretend like we're getting some extra resources.
			if (isFake) {
				if (b.getDevCards().size() == 0)
					return 0;
				b.getDevCards().remove();
				return DC_RESOURCE_EQUIVALENCE;
			}
			p.pullCard();
			return 0;
		}
	}
	
	protected class SettlementPlace implements Action {
		
		protected Integer intersectionId = null;
		
		SettlementPlace(Integer intersectionId) {
			this.intersectionId = intersectionId;
		}

		@Override
		public float execute(Board b, Player p, boolean isFake) {
			assert (this.intersectionId != null);
			p.placeSettlement(this.intersectionId);
			return 0;
		}
		
	}
	
	protected class SettlementBuy extends SettlementPlace {

		SettlementBuy(Integer intersectionId) {
			super(intersectionId);
		}

		@Override
		public float execute(Board b, Player p, boolean isFake) {
			assert (this.intersectionId != null);
			p.buySettlement(this.intersectionId);
			return 0;
		}
		
	}
	
	protected class CityBuy implements Action {
		
		private Integer intersectionId = null;
		
		CityBuy(Integer intersectionId) {
			this.intersectionId = intersectionId;
		}

		@Override
		public float execute(Board b, Player p, boolean isFake) {
			assert (this.intersectionId != null);
			p.buyCity(this.intersectionId);
			return 0;
		}
		
	}
	
	protected class DevCardPlay implements Action {
		private DevelopmentCard dc = null;
		
		DevCardPlay(DevelopmentCard dc) {
			this.dc = dc;
		}

		@Override
		public float execute(Board b, Player p, boolean isFake) {
			// Bit of a hack. There is nondeterminism involved in playing a soldier card.
			// This guy can't handle nondeterminism. Therefore, do the following:
			if (dc == DevelopmentCard.SOLDIER && isFake) {
				CardCollection currentHand = new CardCollection(p.getHand());
				p.playCard(dc);
				int totalCardsNow = p.getHand().getTotalCards();
				p.getHand().resetHand(currentHand);
				// In the general case, this will return 1, but possible we didn't rob anyone.
				return totalCardsNow - p.getHand().getTotalCards();
			}
			p.playCard(dc);
			return 0;
		}
		
	}
	
	protected class SelfTrade implements Action {
		private CardType tradingIn;
		private CardType gettingBack;
		
		SelfTrade(CardType tradingIn, CardType gettingBack) {
			this.tradingIn = tradingIn;
			this.gettingBack = gettingBack;
		}
		
		@Override
		public float execute(Board b, Player p, boolean isFake) {
			p.doTradeWithSelf(tradingIn, gettingBack);
			return 0;
		}
	}
	
	protected static class State implements Comparable<State> {
		private LinkedList<Action> actions;
		private Board boardState;
		private Player boardPlayer;
		// values are approximate, and we can save some space by storing them as floats
		private float value = 0;
		private float expectedExtraCards = 0;
		
		State(Board b, Player p) {
			this.boardState = Board.makeFakeBoard(b);
			this.boardPlayer = this.boardState.getPlayerByColor(p.getPlayerColor());
			this.actions = new LinkedList<Action>();
		}
		
		State(State state) {
			this.actions = new LinkedList<Action>(state.actions);
			this.boardState = Board.makeFakeBoard(state.boardState);
			this.boardPlayer = this.boardState.getPlayerByColor(state.boardPlayer.getPlayerColor());
		}
		
		State newStateFromAction(Action a) {
			State newState = new State(this);
			this.expectedExtraCards += a.execute(newState.boardState, newState.boardPlayer, true);
			newState.actions.add(a);
			return newState;
		}
		
		LinkedList<Action> getActions() {
			return this.actions;
		}
		
		StateAwareCPUPlayer getPlayer() {
			return (StateAwareCPUPlayer) this.boardPlayer;
		}
		
		Board getBoard() {
			return this.boardState;
		}
		
		float getExpectedExtraCards() {
			return this.expectedExtraCards;
		}
		
		// Players can broadly choose how to value their states.
		
		float getValue() {
			return this.value;
		}
		
		void setValue(float v) {
			this.value = v;
		}
		
		void setValue(double v) {
			this.value = (float) v;
		}

		@Override
		public int compareTo(State arg0) {
			return (int) Math.signum(this.getValue() - arg0.getValue());
		}
	}
	
	/* Infrastructure for operating on states */
	
	protected interface StateTransform {
		Collection<State> transform(State state);
	}
	
	protected class MultiThreadedStateReducer {
		/* Generic object that takes a list of states and maps the 
		 * provided StateTransform's computeState method over the elements */
		private StateTransform transform;
		
		MultiThreadedStateReducer(StateTransform t) {
			this.transform = t;
		}
		
		List<State> reduce(List<State> states) {
			int totalThreadsToCreate = Math.min(NUM_CPUS_TO_USE, states.size());
			List<Collection<State>> results = new ArrayList<Collection<State>>(totalThreadsToCreate);
			List<Thread> threads = new ArrayList<Thread>();
			Lock lock = new ReentrantLock();
			for (int i=0; i<totalThreadsToCreate; i++) {
				results.add(null);
				int startIndex = i * (states.size() / totalThreadsToCreate);
				int endIndex = Math.min((i + 1) * (states.size() / totalThreadsToCreate), states.size()); 
				Thread thread = new Thread(new ReduceRunner(this.transform, states, startIndex, endIndex, results, lock, i));
				threads.add(thread);
				thread.start();
			}
			for (Thread t : threads) {
				try {
					t.join();
				} catch (InterruptedException e) {
					System.out.println("Interrupted in MultiThreadedStateReducer while waiting to join!");
					return null;
				}
			}
			List<State> ret = new ArrayList<State>();
			for (int i=0; i<totalThreadsToCreate; i++)
				ret.addAll(results.get(i));
			return ret;
		}
		
		class ReduceRunner implements Runnable {
			
			private StateTransform transform;
			private List<State> states;
			private int startIndex;
			private int endIndex; 
			private List<Collection<State>> results;
			private Lock lock;
			private int resIndex;

			ReduceRunner(StateTransform transform, List<State> states, int startIndex, int endIndex, 
					List<Collection<State>> results, Lock lock, int resIndex) {
				this.transform = transform;
				this.states = states;
				this.startIndex = startIndex;
				this.endIndex = endIndex;
				this.results = results;
				this.lock = lock;
				this.resIndex = resIndex;
			}
			
			@Override
			public void run() {
				List<State> theseResults = new ArrayList<State>();
				for (int i=this.startIndex; i<endIndex; i++) {
					theseResults.addAll(this.transform.transform(this.states.get(i)));
				}
				this.lock.lock();
				this.results.set(this.resIndex, theseResults);
				this.lock.unlock();
			}
			
		}
	}
	
	public ComputerPlayer fakeCopy(Board fake) {
		return new StateAwareCPUPlayer(fake, this);
	}

}
