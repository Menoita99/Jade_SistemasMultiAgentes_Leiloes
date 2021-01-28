package com.sma;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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

	private int money = 0;


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
				send(newMsg);
				System.out.println("Sending join request to: "+result[i].getName());
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







	public void chooseItemPriorities(List<AuctionItem> itens) {
		// TODO Auto-generated method stub

	}






	private class BidderBehavior extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			ACLMessage msg = myAgent.receive();
			if(msg != null) {
				MessageType type = MessageType.valueOf(msg.getContent().split("\n")[0]);
				switch (type) {
				case JOIN: {
					processJoin(msg);
					break;
				}
				case BIDDING: {
					processBidding(msg);
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
		}



		private void processBidding(ACLMessage msg) {
			// TODO Auto-generated method stub
		}



		private void processJoin(ACLMessage msg) {
			System.out.println("Received Join message from "+msg.getSender());
			System.out.println(msg.getContent());
			if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
				if(auctionner == null)
					auctionner = msg.getSender();
				else {
					ACLMessage newMsg = new ACLMessage(ACLMessage.REFUSE); 
					newMsg.setContent(MessageType.JOIN.toString()+"\n");
					newMsg.addReceiver(msg.getSender());
					send(newMsg);
				}
			}
		}



		private void processStartRound(ACLMessage msg) {
			money += 100;
			String json = msg.getContent().substring(msg.getContent().indexOf("\n"));
			Type listType = new TypeToken<List<AuctionItem>>() {}.getType();
			List<AuctionItem> itens = new Gson().fromJson(json, listType);
			chooseItemPriorities(itens);
		}



		private void processWinner(ACLMessage msg) {
			// TODO Auto-generated method stub

		}



		private void processOver(ACLMessage msg) {
			// TODO Auto-generated method stub

		}
	}

}
