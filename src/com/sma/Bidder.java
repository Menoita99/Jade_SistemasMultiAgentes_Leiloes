package com.sma;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import lombok.Getter;
import lombok.Setter;
import jade.lang.acl.ACLMessage;

@Getter
@Setter
public class Bidder extends Agent{

	private static final long serialVersionUID = 1L;

	private BidderBehavior behavior = new BidderBehavior();
	private AID auctionner;

	@Override
	protected void setup() {
		try {
			regist();
			joinAuction();
		} catch (FIPAException | InterruptedException e) {
			e.printStackTrace();
		}
	}





	private void joinAuction() throws InterruptedException, FIPAException {
		while(auctionner == null) {
			
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("auctionner");
			template.addServices(sd);
			DFAgentDescription[] result = DFService.search(this, template);
			for (int i = 0; i < result.length; i++) {
				ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST); 
				newMsg.setContent("join");
				newMsg.addReceiver(result[i].getName());
			}
			
			if(auctionner == null)
				Thread.sleep(1000);
		}
	}






	private void regist() throws FIPAException {
		addBehaviour(behavior);
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		dfd.setName(getAID());
		sd.setType("bidder");
		sd.setName(getLocalName()+" bidder");
		dfd.addServices(sd);
		DFService.register(this, dfd);
		System.out.println("Bidder "+getName()+" with aid "+getAID()+" ready");
	}


	private class BidderBehavior extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			ACLMessage msg = myAgent.receive();
			switch (msg.getPerformative()) {
			case ACLMessage.ACCEPT_PROPOSAL: {
				
				break;
			}
			case ACLMessage.REFUSE: {
				
				break;
			}
			default:
				throw new IllegalArgumentException("Unexpected value: " + msg.getPerformative());
			}
			
		}
	}
}
