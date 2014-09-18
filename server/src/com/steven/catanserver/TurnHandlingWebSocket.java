package com.steven.catanserver;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

import fi.iki.elonen.IWebSocketFactory;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.WebSocket;
import fi.iki.elonen.WebSocketFrame;
import fi.iki.elonen.WebSocketFrame.CloseCode;
import fi.iki.elonen.WebSocketResponseHandler;

public class TurnHandlingWebSocket extends WebSocket implements MessageSender {
	
	private String userId;
	private String boardId;
	private HumanPlayer clientPlayer;
	private LinkedList<Waiter> waitingChannels = new LinkedList<Waiter>();

	public TurnHandlingWebSocket(IHTTPSession handshake) {
		super(handshake);
		System.out.println("Opening new websocket");
		// Small amount of code duping here with controllers.
		Map<String, String> parms = handshake.getParms();
		this.userId = parms.get("user_id");
		this.boardId = parms.get("board_id");
		
		this.clientPlayer = this.getClientPlayer();
		this.clientPlayer.registerChannelHandler(this);
	}
	
	/* Websocket event handling things */

	@Override
	protected void onPong(WebSocketFrame pongFrame) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void send(String message) {
		// TODO: possibly stick some retry logic in here
		try {
			super.send(message);
		} catch (IOException e) {
			System.out.println("Failed to send message '" + message + "', reason was: " + e.toString());
		}
	}

	@Override
	protected void onMessage(WebSocketFrame messageFrame) {
		String message = messageFrame.getTextPayload();
//		System.out.println("Got message: " + message);
		if (message.contentEquals("ping!")) {
			this.send("pong!");
			return;
		}
		
		String response = this.clientPlayer.handleMessage(message);
		if (response != null)
			this.send(response);
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onClose(CloseCode code, String reason,
			boolean initiatedByRemote) {
		System.out.println(String.format("Websocket closed, reason: %s, code: %s, by_client: %s!", reason, code, initiatedByRemote));
		// TODO: this seems like it is being memory leaky... how can we clean up?
		// Try to remove this ref... but doesn't seem to be working
		this.clientPlayer = null;
		for (Waiter w : this.waitingChannels)
			w.onMessageTermination();
	}

	@Override
	protected void onException(IOException e) {
		System.out.println(String.format("Exception in Websocket! ... %s", e));
	}
	
	/* Catan specific turn handling logic */
	
	BoardModel getBoard() {
		return BoardModel.getFromDB(this.boardId);
	}
	
	HumanPlayer getClientPlayer() {
		return getBoard().getHumanPlayer(this.userId);
	}
	
	/* Websocket creation boilerplate */

	private static IWebSocketFactory webSocketFactory = new IWebSocketFactory() {

		@Override
		public WebSocket openWebSocket(IHTTPSession handshake) {
			return new TurnHandlingWebSocket(handshake);
		}
	};

	private static WebSocketResponseHandler wsr = new WebSocketResponseHandler(webSocketFactory);

	public static WebSocketResponseHandler getHandler() {
		return wsr;
	}

	@Override
	public void registerWaiter(Waiter w) {
		this.waitingChannels.add(w);
	}
	
	// This is for debugging if instances are getting cleaned up.
//	public void finalize() {
//		System.out.println("GC successful on a websocket.");
//		
//		try {super.finalize();}
//		catch(Throwable t) {
//			
//		}
//	}
}
