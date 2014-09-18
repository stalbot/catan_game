package com.steven.catanserver;

import redis.clients.jedis.Jedis;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.ServerRunner;

public class CatanServer extends NanoHTTPD {
	public CatanServer() {
		super(8081);
	}
	
	public Response serve(IHTTPSession session) {
		Method method = session.getMethod();
        String uri = session.getUri();
		return Controllers.handle(uri, method, session);
	}
	
    public static void main(String[] args) {
//    	System.out.println(HexData.generateHexes());
		ServerRunner.run(CatanServer.class);
    }
    
    public static Jedis getRedisClient() {
    	return new Jedis("0.0.0.0", 6379);
    }
}
