package com.sma;

import java.util.LinkedList;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
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
	private LinkedList<AID> bidders = new LinkedList<>();
	
	public Auctioneer() {
		auction = new Auction();
	}
	
	@Override
	protected void setup() {
		try {
			searchForBidders();
			regist();
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}
	
	private void regist() throws FIPAException {
		addBehaviour(behavior);
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		dfd.setName(getAID());
		sd.setType("auctionner");
		sd.setName(getLocalName()+" auctioneer");
		dfd.addServices(sd);
		DFService.register(this, dfd);
		System.out.println("Auctionner "+getName()+" with aid "+getAID()+" ready");
	}
	
	protected void searchForBidders() {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("bidders");
		template.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			bidders.clear();
			for (int i = 0; i < result.length; ++i)
				bidders.add(result[i].getName());
		}
		catch (FIPAException fe) { fe.printStackTrace(); }
	} 

	
	
	
	
	private class AuctioneerBehavior extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			// TODO Auto-generated method stub
			
		}
	}
}
