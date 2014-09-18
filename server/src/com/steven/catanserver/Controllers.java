package com.steven.catanserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import redis.clients.jedis.Jedis;

import fi.iki.elonen.IWebSocketFactory;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.WebSocket;

public class Controllers {
	
	private static final HashMap<String, String> mimeTypes = new HashMap<String, String>();
	
	static {
		mimeTypes.put("js", "application/javascript");
	}
	
	private static String getMimeType(String fileExtension) {
		String mimeType = mimeTypes.get(fileExtension);
		return (mimeType == null) ? "text/" + fileExtension : mimeType;
	}
	
	private static Response makeFileResponse(String fileName) {
		// Another Java 7 improvement to be had below
		String fileContents;
		try {
			fileContents = new Scanner(new File(fileName)).useDelimiter("\\Z").next();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			return makeResponse(makeErrorJSON("404"), Response.Status.NOT_FOUND);
		}
		String[] temp = fileName.split("\\.");
		String fileExtension = temp[temp.length - 1];
		return new Response(Response.Status.OK, getMimeType(fileExtension), fileContents);
	}
	
	private static Response makeResponse(String responseString) {
		return makeResponse(responseString, Response.Status.OK);
	}
	
	private static Response makeResponse(String responseString, Response.Status status) {
		return new Response(status, "application/json", responseString);
	}
	
	public static String makeErrorJSON(String msg) {
		return "{\"error\": \"" + msg + "\"}";
	}

	public static Response handle(String uri, Method method, IHTTPSession session) {
		Map<String, String> parms = session.getParms();
		// TODO: use string switching in 1.7
//		System.out.println(uri);
		if (uri.contentEquals("/register")) {
			String wait = parms.get("wait");
			String boardId = parms.get("board_id");
			String userId = parms.get("user_id");
			if (boardId == null || userId == null)
				return makeResponse(makeErrorJSON("400"), Response.Status.BAD_REQUEST);
			Response ws = TurnHandlingWebSocket.getHandler().serve(session);
			if (ws != null) {
				System.out.println("Created websocket!");
				return ws;
			}
			// Wasn't a websocket request, better have been a GET
			if (method != Method.GET)
				return makeResponse(makeErrorJSON("405"), Response.Status.METHOD_NOT_ALLOWED);
			String responseJSON = BoardService.getBoardState(boardId, userId);
			return makeResponse(responseJSON);
		}
//		else if (uri.contentEquals("/action")) {
//			if (method != Method.POST)
//				return makeResponse(makeErrorJSON("405"), Response.Status.METHOD_NOT_ALLOWED);
//		}
//		else if (uri.contentEquals("/trade")) {
//			if (method != Method.POST)
//				return makeResponse(makeErrorJSON("405"), Response.Status.METHOD_NOT_ALLOWED);
//		}
		else if (uri.contentEquals("/new_game")) {
			// TODO: definitely uncomment this
//			if (method != Method.POST)
//				return makeResponse(makeErrorJSON("405"), Response.Status.METHOD_NOT_ALLOWED);
			int numPlayers = 4;
			try {
				numPlayers = Integer.parseInt(parms.get("num_players"));
			}
			catch(NumberFormatException e) {}
			return makeResponse(BoardService.makeNewBoard(parms.get("user_id"), numPlayers));
		}
		else if (uri.contentEquals("/start_game")) {
			if (method != Method.POST)
				return makeResponse(makeErrorJSON("405"), Response.Status.METHOD_NOT_ALLOWED);
			String boardId = parms.get("board_id");
			String userId = parms.get("user_id");
			BoardService.startGame(boardId, userId);
			return makeResponse("{\"acknowledged\": true}");
		}
		else if (uri.contentEquals("/test")) {
			if (method != Method.GET)
				return makeResponse(makeErrorJSON("405"), Response.Status.METHOD_NOT_ALLOWED);
			return makeResponse("OK");
		}
		else {
			String filename = "/Users/Steven/catan/static/js" + uri; 
			System.out.println(filename);
			return makeFileResponse(filename);
		}
	}
}
