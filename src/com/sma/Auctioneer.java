package com.sma;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

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

	private boolean acceptinJoins = true;

	private Auction auction;
	private ScheduledExecutorService executer = Executors.newSingleThreadScheduledExecutor();
	private AuctioneerBehavior behavior = new AuctioneerBehavior();
	private LinkedList<AID> bidders = new LinkedList<>();
	private List<AuctionItem> auctionItems;
	private Map<AID, String> priorities = new HashMap<AID, String>();

	public Auctioneer() {
		auction = new Auction();
	}

	@Override
	protected void setup() {
		try {
			regist();
			addBehaviour(behavior); 
			startJoinPhase(20);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	private void startJoinPhase(int time) {
		executer.schedule(() -> {
			acceptinJoins = false;
			startAuction();
		}, time, TimeUnit.SECONDS);
	}

	private void startAuction() {
		auction.setFase(AuctionFase.onGoing);
		sendStartingMessage();
	}

	private void sendStartingMessage() {
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		auctionItems = auction.nextRound();
		Gson gson = new Gson();
		String msgAux = gson.toJson(auctionItems);
		for (AID bidder : bidders) {
			msg.addReceiver(bidder);
		}
		msg.setContent(MessageType.START_ROUND.toString() + "\n" + msgAux);
		System.out.println("Sent message: " + msg);
		send(msg);
		
		executer.schedule(() -> {
			startBiddingPhase();
		}, 10, TimeUnit.SECONDS);
		
	}


	private void startBiddingPhase() {
		
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

	private class AuctioneerBehavior extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			ACLMessage msg = myAgent.receive();
			if (msg != null) {	 
				MessageType type = MessageType.valueOf(msg.getContent().split("\n")[0]);
				switch (type) {
				case JOIN:{
					processJoin(msg);
					break;
				}case BIDDING: {
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
			}else {
				this.block();
			} 
		}
		
		private void processPriorities(ACLMessage msg) {
			String prioritiesMsg = msg.getContent().split("\n")[1];
			priorities.put(msg.getSender(), prioritiesMsg);
			System.out.println(prioritiesMsg);
		} 	

		private void processBidding(ACLMessage msg) {
			
		}

		private void processJoin(ACLMessage msg) {
			AID sender = msg.getSender();
			if (auction.getFase() == AuctionFase.Open && acceptinJoins) {
				if (msg.getPerformative() == ACLMessage.REQUEST && !bidders.contains(sender)) {
					ACLMessage response = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					response.setContent(MessageType.JOIN.toString() + "\n");
					response.addReceiver(sender);
					bidders.add(sender);
					send(response);
				}
			} else {
				ACLMessage response = new ACLMessage(ACLMessage.REFUSE);
				response.setContent(MessageType.JOIN.toString() + "\n");
				response.addReceiver(sender);
				send(response);
			}
			if (msg.getPerformative() == ACLMessage.REFUSE) {
				bidders.remove(sender);
			}
		}
	}

	public static void main(String[] args) {
		//new Auctioneer().getItemsJson();
	}

}
