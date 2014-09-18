package com.steven.catanserver;

public enum HarborType {
	WHEAT, SHEEP, BRICK, WOOD, ORE, GENERIC;
	
	HarborType() {
	}
	
	public CardType getCardType() {
		if (this == GENERIC)
			return null;
		return CardType.valueOf(this.toString());
	}
	
	public int getTradeRatio() {
		return (this == GENERIC) ? 3 : 2;
	}
}
