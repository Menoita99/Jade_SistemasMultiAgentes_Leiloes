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
				newMsg.setContent(MessageType.JOIN.toString()+"\n");
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
			MessageType type = MessageType.valueOf(msg.getContent().split("\n")[0]);
			switch (type) {
			case ACCEPT: {
				processAccept(msg);
				break;
			}
			case REFUSE: {
				processRefuse(msg);
				break;
			}
			case START_ROUND: {
				processStartRound(msg);
				break;
			}
			case WINNER: {
				processWinner(msg);
				break;
			}
			case OVER: {
				processOver(msg);
				break;
			}
			default:
				throw new IllegalArgumentException("Unexpected value: " + type);
			}
		}

		
		
		private void processRefuse(ACLMessage msg) {
			// TODO Auto-generated method stub
			
		}



		private void processStartRound(ACLMessage msg) {
			// TODO Auto-generated method stub
			
		}



		private void processWinner(ACLMessage msg) {
			// TODO Auto-generated method stub
			
		}



		private void processOver(ACLMessage msg) {
			// TODO Auto-generated method stub
			
		}



		private void processAccept(ACLMessage msg) {
			// TODO Auto-generated method stub
			
		}
	}
}
