package com.steven.catanserver;

public enum PlayerColor {
	RED("red"), BLUE("blue"), WHITE("white"), ORANGE("orange");
	
	private String color;
	
	String getString() {
		return this.color;
	}
	
	private PlayerColor(String color) {
		this.color = color;
	}
	
}
