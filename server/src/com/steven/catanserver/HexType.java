package com.steven.catanserver;

public enum HexType {
	OCEAN(false), HILL(CardType.BRICK), MOUNTAIN(CardType.ORE), FIELD(CardType.WHEAT), PASTURE(CardType.SHEEP), FOREST(CardType.WOOD), DESERT(false);
	
	private Boolean hasNum = true;
	private CardType yield = null;
	
	HexType() {}

	HexType(Boolean hasNum) {
		this.hasNum = hasNum;
	}
	
	HexType(CardType yield) {
		this.yield = yield;
	}
	
	Boolean hasRollNumber() {
		return this.hasNum;
	}
	
	CardType getCardType() {
		return this.yield;
	}
	
}
