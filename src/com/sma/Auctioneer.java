package com.sma;

import java.util.LinkedList;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Auctioneer extends Agent{
	
	private static final long serialVersionUID = 1L;
	
	private Auction auction;
	private AuctioneerBehavior behavior = new AuctioneerBehavior();
	private LinkedList<Bidder> bidders = new LinkedList<>();
	
	public Auctioneer() {
		auction = new Auction();
	}
	
	

	
	
	
	
	private class AuctioneerBehavior extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			// TODO Auto-generated method stub
			
		}
	}
}
