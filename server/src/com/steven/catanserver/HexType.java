package com.steven.catanserver;

public enum HexType {
	OCEAN(false), HILL, MOUNTAIN, FIELD, PASTURE, FOREST, DESERT(false);
	
	private Boolean hasNum = true;
	
	HexType() {}

	HexType(Boolean hasNum) {
		this.hasNum = hasNum;
	}
	
	Boolean hasRollNumber() {
		return this.hasNum;
	}
	
}
