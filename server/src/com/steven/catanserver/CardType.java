package com.steven.catanserver;

import java.util.HashMap;

public enum CardType {
	WOOL, GRAIN, WOOD, BRICK, ORE;

	String getName() {
		return this.toString().toLowerCase();
	}
	
	HarborType getHarborType() {
		return HarborType.valueOf(this.toString());
	}
}
