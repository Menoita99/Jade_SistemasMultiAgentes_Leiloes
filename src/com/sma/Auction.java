package com.sma;

import java.util.LinkedList;
import java.util.List;

import lombok.Data;

@Data
public class Auction {

	
	private final int initialNumberOfItems = 50;
	private final int numberOfItemsPerRound = 10;
	
	private int round = 0;
	private AuctionFase fase = AuctionFase.Open;
	private LinkedList<AuctionItem> items = new LinkedList<>();
 	
	
	
	public Auction() {
		for (int i = 0; i < initialNumberOfItems; i++) 
			items.add(new AuctionItem());
	}
	
	
	public List<AuctionItem> nextRound(){
		if((round+1)*numberOfItemsPerRound < items.size()) {
			round++;
			return items.subList((round-1)*numberOfItemsPerRound, (round)*numberOfItemsPerRound);
		}
		return null;
	}
}
