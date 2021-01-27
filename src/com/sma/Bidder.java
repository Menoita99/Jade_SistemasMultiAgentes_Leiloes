package com.sma;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Bidder extends Agent{

	private static final long serialVersionUID = 1L;
	
	private BidderBehavior behavior = new BidderBehavior();
	
	
	
	
	private class BidderBehavior extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			// TODO Auto-generated method stub
			
		}
	}
}
