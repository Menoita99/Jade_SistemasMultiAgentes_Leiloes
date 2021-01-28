package com.sma;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
	private LinkedList<AuctionItem> auctionItems;
	private Map<AID, String> priorities = new HashMap<AID, String>();
	private OneSchedualTimer biddingTimer;

	private Pair<AID, Double> highestOffer = null;

	public Auctioneer() {
		auction = new Auction();
	}

	@Override
	protected void setup() {
		try {
			regist();
			addBehaviour(behavior);
			startJoinPhase(15);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	private void startJoinPhase(int time) {
		executer.schedule(() -> {
			System.out.println("Joining phase started");
			acceptinJoins = false;
			startAuction();
		}, time, TimeUnit.SECONDS);
	}

	private void startAuction() {
		System.out.println("Auction started");
		auction.setFase(AuctionFase.onGoing);
		startNewRound();
	}

	private void startNewRound() {
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		try {
			auctionItems = auction.nextRound();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Gson gson = new Gson();
		String msgAux = gson.toJson(auctionItems);
		for (AID bidder : bidders) {
			msg.addReceiver(bidder);
		}
		msg.setContent(MessageType.START_ROUND.toString() + "\n" + msgAux);
		System.out.println("Sent message: " + msg);
		send(msg);

		executer.schedule(() -> {
			biddingPhase();
		}, 10, TimeUnit.SECONDS);

	}

	private void biddingPhase() {
		if (!auctionItems.isEmpty()) {
			Gson gson = new Gson();
			ACLMessage biddingMessage = new ACLMessage(ACLMessage.INFORM);
			biddingMessage.setContent(MessageType.START_BIDDING.toString() + "\n" + gson.toJson(auctionItems.pop()));
			for (AID bidder : bidders)
				biddingMessage.addReceiver(bidder);
			send(biddingMessage);
			biddingTimer = new OneSchedualTimer(() -> {
				biddingPhase();
				announceWinner();
			}, 5000);
		} else {
			startNewRound();
		}
	}

	private void announceWinner() {
		ACLMessage winnerMessage = new ACLMessage(ACLMessage.INFORM);
		String jsonHighestOffer = new Gson().toJson(highestOffer, Pair.class);
		winnerMessage.setContent(MessageType.WINNER.toString() + "\n" + jsonHighestOffer);
		sendMessageToAllBiders(winnerMessage);
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
			} else {
				this.block();
			}
		}

		private void processPriorities(ACLMessage msg) {
			String json = msg.getContent().substring(msg.getContent().indexOf("\n"));
			System.out.println("--------------------------");
			System.out.println(json);
			Type listType = new TypeToken<List<Pair<AuctionItem, Integer>>>() {
			}.getType();
			List<Pair<AuctionItem, Integer>> itens = new Gson().fromJson(json, listType);
			System.out.println("Bidder:" + msg.getSender().getLocalName());
			itens.forEach((pair) -> System.out.println("Key: " + pair.getKey() + "| Priority: " + pair.getValue()));
		}

		private void processBidding(ACLMessage msg) {
			biddingTimer.restart(5000);
			double offer = Double.parseDouble(msg.getContent().split("\n")[1]);
			ACLMessage biddingResult = new ACLMessage(ACLMessage.INFORM);
			if (highestOffer == null) {
				highestOffer = new Pair<AID, Double>(msg.getSender(), offer);
				String jsonHighestOffer = new Gson().toJson(highestOffer, Pair.class);
				biddingResult.setContent(MessageType.BIDDING.toString() + "\n" + jsonHighestOffer);
				sendMessageToAllBiders(biddingResult);
			}
			if (offer > highestOffer.getValue()) {
				highestOffer = new Pair<AID, Double>(msg.getSender(), offer);
				String jsonHighestOffer = new Gson().toJson(highestOffer, Pair.class);
				biddingResult.setContent(MessageType.BIDDING.toString() + "\n" + jsonHighestOffer);
				sendMessageToAllBiders(biddingResult);
			} else {
				ACLMessage biddingRefused = new ACLMessage(ACLMessage.REFUSE);
				String jsonHighestOffer = new Gson().toJson(highestOffer, Pair.class);
				biddingResult.setContent(MessageType.BIDDING.toString() + "\n" + jsonHighestOffer);
				biddingRefused.addReceiver(msg.getSender());
				send(biddingRefused);
			}
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

	private void sendMessageToAllBiders(ACLMessage msg) {
		for (AID bidder : bidders) {
			msg.addReceiver(bidder);
		}
		send(msg);
	}

	public static void main(String[] args) {
		// new Auctioneer().getItemsJson();
	}

}
