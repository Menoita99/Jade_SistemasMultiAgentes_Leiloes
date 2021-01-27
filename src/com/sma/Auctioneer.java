package com.sma;

import java.util.LinkedList;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Auctioneer extends Agent {

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
			regist();
			searchForBidders();
			addBehaviour(behavior);

		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	private void regist() throws FIPAException {

		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		dfd.setName(getAID());
		sd.setType("auctionner");
		sd.setName(getLocalName() + " auctioneer");
		dfd.addServices(sd);
		DFService.register(this, dfd);
		System.out.println("Auctionner " + getName() + " with aid " + getAID() + " ready");
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
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	private class AuctioneerBehavior extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			ACLMessage msg = myAgent.receive();
			if (msg != null) {
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
				case PRIORITIES: {
					processPriorities(msg);
					break;
				}
				case WINNER: {
					// processWinner(msg);
					break;
				}
				default:
					throw new IllegalArgumentException("Unexpected value: " + type);
				}
			}
		}

		private void processPriorities(ACLMessage msg) {
			// TODO Auto-generated method stub

		}

		private void processBidding(ACLMessage msg) {
			// TODO Auto-generated method stub

		}

		private void processJoin(ACLMessage msg) {
			ACLMessage response = new ACLMessage(ACLMessage.INFORM);
			if (auction.getFase() == AuctionFase.Open) {
				if (msg.getPerformative() == ACLMessage.REQUEST) {
					bidders.add(msg.getSender());
					response.setContent(MessageType.JOIN.toString() + "\n");
					response.addReceiver(msg.getSender());
					System.out.println("Auctioneer sent ACCEPT to :" + msg.getSender());
				}
			} else if (msg.getPerformative() == ACLMessage.REFUSE) {
				bidders.remove(msg.getSender());
			}
		}
	}
}
