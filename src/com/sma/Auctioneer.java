package com.sma;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
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
	private OneSchedualTimer biddingTimer;

	private Pair<String, Double> highestOffer = null;

	public Auctioneer() {
		auction = new Auction();
	}

	@Override
	protected void setup() {
		try {
			regist();
			addBehaviour(behavior);
			startJoinPhase(25);
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
		startNewRound();
	}

	private void startNewRound() {
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		try {
			auctionItems = auction.nextRound();
			System.out.println("AUCTIONEER| Starting round " + auction.getRound());
		} catch (Exception e) {
			e.printStackTrace();
		}
		Gson gson = new Gson();
		String msgAux = gson.toJson(auctionItems);
		for (AID bidder : bidders) {
			msg.addReceiver(bidder);
		}
		msg.setContent(MessageType.START_ROUND.toString() + "\n" + msgAux);
		send(msg);

		executer.schedule(() -> {
			biddingPhase();
		}, 10, TimeUnit.SECONDS);

	}

	private void biddingPhase() {
		System.out.println("========================================================================");
		if (!auctionItems.isEmpty()) {
			AuctionItem item = auctionItems.pop();
			System.out.println("AUCTIONEER| Round " + auction.getRound() + ",auctioning item: " + item.getId());
			highestOffer = new Pair<String, Double>("no biddings yet", item.getPrice());
			Gson gson = new Gson();
			ACLMessage biddingMessage = new ACLMessage(ACLMessage.INFORM);
			biddingMessage.setContent(MessageType.START_BIDDING.toString() + "\n" + gson.toJson(item));
			for (AID bidder : bidders)
				biddingMessage.addReceiver(bidder);
			biddingTimer = new OneSchedualTimer(() -> {
				announceWinner(item);
				biddingPhase();
			}, 5000);
			send(biddingMessage);
		} else {
			startNewRound();
		}
	}

	private void announceWinner(AuctionItem item) {
		ACLMessage winnerMessage = new ACLMessage(ACLMessage.INFORM);
		item.setPrice(highestOffer.getValue());
		String jsonHighestOffer = new Gson().toJson(new Pair<String, AuctionItem>(highestOffer.getKey(), item), Pair.class);
		winnerMessage.setContent(MessageType.WINNER.toString() + "\n" + jsonHighestOffer);
		System.out.println("AUCTIONEER| Item: " + item.getId() + ", winner is: "
		+ (highestOffer.getKey().equals("no biddings yet") ? "Nobody" : highestOffer.getKey())); 
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
					processWinner(msg);
					break;
				}
				default:
					throw new IllegalArgumentException("Unexpected value: " + type);
				}
			} else {
				this.block();
			}
		}

		private void processWinner(ACLMessage msg) {
			// TODO Auto-generated method stub
			
		}

		private void processPriorities(ACLMessage msg) {
			String json = msg.getContent().substring(msg.getContent().indexOf("\n"));
			Type listType = new TypeToken<List<Pair<AuctionItem, Integer>>>() {
			}.getType();
			List<Pair<AuctionItem, Integer>> itens = new Gson().fromJson(json, listType);
			System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			System.out.println("AUCTIONEER| Bidder " + msg.getSender().getLocalName() + " priorities");
			itens.forEach((pair) -> System.out.println("Key: " + pair.getKey() + "| Priority: " + pair.getValue()));
		}

		private void processBidding(ACLMessage msg) {
			biddingTimer.restart(5000);
			double offer = Double.parseDouble(msg.getContent().split("\n")[1]);
			System.out.println("AUCTIONEER| Bidding msg received: " + msg.getSender().getLocalName() + "of: " + offer);
			System.out.println("AUCTIONEER| Actual highest offer: " + highestOffer.getValue() + " from: " + highestOffer.getKey());
			ACLMessage biddingResult = new ACLMessage(ACLMessage.INFORM);
			if (highestOffer == null) {
				highestOffer = new Pair<String, Double>(msg.getSender().getLocalName(), offer);
				String jsonHighestOffer = new Gson().toJson(highestOffer);
				biddingResult.setContent(MessageType.BIDDING.toString() + "\n" + jsonHighestOffer);
				System.out.println("AUCTIONEER| New highest offer: " + highestOffer.getValue() + " from: " + highestOffer.getKey());
				sendMessageToAllBiders(biddingResult);
			}
			if (offer > highestOffer.getValue()) {
				highestOffer = new Pair<String, Double>(msg.getSender().getLocalName(), offer);
				String jsonHighestOffer = new Gson().toJson(highestOffer);
				biddingResult.setContent(MessageType.BIDDING.toString() + "\n" + jsonHighestOffer);
				System.out.println("AUCTIONEER| New highest offer: " + highestOffer.getValue() + " from: " + highestOffer.getKey());
				sendMessageToAllBiders(biddingResult);
			} else {
				ACLMessage biddingRefused = new ACLMessage(ACLMessage.REFUSE);
				String jsonHighestOffer = new Gson().toJson(highestOffer);
				biddingRefused.setContent(MessageType.BIDDING.toString() + "\n" + jsonHighestOffer);
				biddingRefused.addReceiver(msg.getSender());
				System.out.println("AUCTIONEER| Refused offer, current highest: " + highestOffer.getValue() + " from: " + highestOffer.getKey());
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
