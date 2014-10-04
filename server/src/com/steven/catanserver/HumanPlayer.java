package com.steven.catanserver;

import redis.clients.jedis.JedisPubSub;

public class HumanPlayer extends Player {
	public final transient Boolean isSync = false;
	private BoardModel boardModel;
	
	HumanPlayer(PlayerColor pc, BoardModel board, String userId, int turnOrder) {
		super(pc, board, turnOrder, userId);
		this.boardModel = board;
	}
	
	public HumanPlayer(Board fake, HumanPlayer hp) {
		// TODO: there might be a bug here with not setting board here
		super(fake, hp);
	}

	class MyListener extends JedisPubSub implements MessageSender.Waiter {
		
		private MessageSender messageSender;
		
		public JedisPubSub setSender(MessageSender messageSender) {
			this.messageSender = messageSender;
			this.messageSender.registerWaiter(this);
			return this;
		}
		
        public void onMessage(String channel, String message) {
        	this.messageSender.send(message);
        }
        
		@Override
		public void onMessageTermination() {
			this.unsubscribe();
		}
		
		/* Default stuff have to implement */

        public void onSubscribe(String channel, int subscribedChannels) {
        }

        public void onUnsubscribe(String channel, int subscribedChannels) {
        }

        public void onPSubscribe(String pattern, int subscribedChannels) {
        }

        public void onPUnsubscribe(String pattern, int subscribedChannels) {
        }

        public void onPMessage(String pattern, String channel,
            String message) {
        	
        }
	}
	
	BoardModel getBoardModel() {
		return this.boardModel;
	}

	public Boolean doTurn() {
//		this.getBoard().notifyTurnStart(this.getId());
		return false;
	}

	public void registerChannelHandler(MessageSender messageSender) {
		this.getBoardModel().registerChannelListener(new MyListener().setSender(messageSender));
	}

	public String handleMessage(String message) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean doSetupTurn() {
		System.out.println(this.getPlayerColor() + " (human) doing setup.");
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public TradeResponse repondToTrade(CardCollection askedFor, CardCollection offered, Boolean allowCounter) {
		// TODO Auto-generated method stub
		return new TradeResponse(false);
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
	public boolean chooseAndPlaceRoad() {
		// TODO Auto-generated method stub
		return false;
	}

}
