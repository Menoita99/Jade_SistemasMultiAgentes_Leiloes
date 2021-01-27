package com.sma;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuctionItem {

	private double price;
	
	public AuctionItem() {
		price = Math.random() * 50;
	}
}
