package com.steven.catanserver;

public enum CardType {
	SHEEP, WHEAT, WOOD, BRICK, ORE;

	String getName() {
		return this.toString().toLowerCase();
	}
	
	HarborType getHarborType() {
		return HarborType.valueOf(this.toString());
	}
}
