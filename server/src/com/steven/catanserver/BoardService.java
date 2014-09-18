package com.steven.catanserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.sun.javadoc.Type;

public class BoardService {
	
	private static class HexSerializer implements JsonSerializer<HexData> {
		@Override
		public JsonElement serialize(HexData arg0, java.lang.reflect.Type arg1,
				JsonSerializationContext arg2) {
			return arg0.toJson();
		}
	}
	
	private static String serializeBoard(BoardModel board) {
		GsonBuilder gson = new GsonBuilder();
		gson.registerTypeAdapter(HexData.class, new HexSerializer());
		return gson.create().toJson(new BoardContainer(board));
	}
	
	private static class BoardContainer {
		private BoardModel board;
		BoardContainer(BoardModel board) {
			this.board = board;
		}
	}
	
	public static void startGame(String boardId, String userId) {
		BoardModel board = BoardModel.getFromDB(boardId);
		board.startSetup();
	}

	public static String getBoardState(String boardId, String userId) {
		BoardModel board = BoardModel.getFromDB(boardId);
		return serializeBoard(board);
	}
	
	public static String makeNewBoard(String userId, int numPlayers) {
		BoardModel board = BoardModel.makeNewBoard(numPlayers, userId);
		return serializeBoard(board);
	}

}
