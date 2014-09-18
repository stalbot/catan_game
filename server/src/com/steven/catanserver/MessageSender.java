package com.steven.catanserver;

public interface MessageSender {
	
	interface Waiter {
		void onMessageTermination();
	}

	void send(String message);
	
	void registerWaiter(Waiter w);
}
