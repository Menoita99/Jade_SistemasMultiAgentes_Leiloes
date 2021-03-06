package com.sma;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

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
public class Bidder extends Agent {

	private static final long serialVersionUID = 1L;
	private final int itemsWanted = 6;
	private final int maxPoints = 60;

	private BidderBehavior behavior = new BidderBehavior();
	private AID auctionner;

	private double money = 0;

	private HashMap<AuctionItem, Integer> itemsWon = new HashMap<>();
	private HashMap<AuctionItem, Integer> priorities = new HashMap<>();
	private AuctionItem currentItemInAuction;
	private int currentItemPriority = 0;

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
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("auctionner");
		template.addServices(sd);
		DFAgentDescription[] result = DFService.search(this, template);
		for (int i = 0; i < result.length; i++) {
			ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
			newMsg.setContent(MessageType.JOIN.toString() + "\n");
			newMsg.addReceiver(result[i].getName());
			send(newMsg);
		}
	}

	private void regist() throws FIPAException {
		addBehaviour(behavior);
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		dfd.setName(getAID());
		sd.setType("bidder");
		sd.setName(getLocalName() + " bidder");
		dfd.addServices(sd);
		DFService.register(this, dfd);
	}

	public void defineItemsPriorities(List<AuctionItem> items) {
		Random r = new Random();
		priorities.clear();
		LinkedList<AuctionItem> wanted = new LinkedList<>();
		while (wanted.size() < itemsWanted && items.size() > 0) {
			AuctionItem item = items.get(r.nextInt(items.size()));
			wanted.add(item);
			items.remove(item);
		}

		int totalMax = maxPoints;
		int min = totalMax / wanted.size();
		int sum = 0;
		int prio = 0;
		int max = 0;
		for (int i = wanted.size() - 1; i >= 0; i--) {
			if (i != 0) {
				max = totalMax - (i + 1) * 5;
				prio = r.nextInt(Math.max(min, Math.min(max, 15) - 5)) + 5;
				totalMax -= prio;
				min = (int) Math.ceil(totalMax / Math.max(1, i + 1));
			} else
				prio = maxPoints - sum;
			sum += prio;
			priorities.put(wanted.get(i), prio);
		}
	}

	private class BidderBehavior extends CyclicBehaviour {

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
				case START_BIDDING: {
					processStartBidding(msg);
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
			} else {
				this.block();
			}
		}

		private void processStartBidding(ACLMessage msg) {
			System.out.println("BIDDER " + getAID().getLocalName() + " | Current money: " + money);
			String json = msg.getContent().substring(msg.getContent().indexOf("\n"));
			currentItemInAuction = new Gson().fromJson(json, AuctionItem.class);
			currentItemPriority = priorities.getOrDefault(currentItemInAuction, 0);
			bid();
		}

		private void bid() {
			Random r = new Random();
			double price = currentItemInAuction.getPrice();
			int priceProb = (5 - (int) ((money - price) / 20));
			int prioProb = 10 + currentItemPriority * 5;
			int bidProb = prioProb + priceProb;

			int nextInt = r.nextInt(100);
			if (price >= money || nextInt >= bidProb) {
				System.out.println("BIDDER " + getAID().getLocalName() + " | I'm not going to bid " + bidProb);
				return;
			}

			double complement = Math.max(Math.min((money - price), 10) * r.nextDouble(), 0.01);
			double offer = price + complement;
			offer = new BigDecimal(offer).setScale(2, RoundingMode.HALF_UP).doubleValue();

			ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
			newMsg.setContent(MessageType.BIDDING.toString() + "\n" + new Gson().toJson(offer));
			newMsg.addReceiver(auctionner);
			System.out.println("BIDDER " + getAID().getLocalName() + " | Bidding on " + currentItemInAuction.getId()
					+ " for: " + offer);
			send(newMsg);
		}

		private void processBidding(ACLMessage msg) {
			String json = msg.getContent().substring(msg.getContent().indexOf("\n"));
			Type type = new TypeToken<Pair<String, Double>>() {
			}.getType();
			Pair<String, Double> item = new Gson().fromJson(json, type);
			currentItemInAuction.setPrice(item.getValue());
			if (!getAID().getName().equals(item.getKey()) || msg.getPerformative() == ACLMessage.REFUSE) {
				bid();
			}
		}

		private void processJoin(ACLMessage msg) {
			if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
				if (auctionner == null)
					auctionner = msg.getSender();
				else {
					ACLMessage newMsg = new ACLMessage(ACLMessage.REFUSE);
					newMsg.setContent(MessageType.JOIN.toString() + "\n");
					newMsg.addReceiver(msg.getSender());
					send(newMsg);
				}
			}
		}

		private void processStartRound(ACLMessage msg) {
			money += 100;
			String json = msg.getContent().substring(msg.getContent().indexOf("\n"));
			Type listType = new TypeToken<List<AuctionItem>>() {
			}.getType();
			List<AuctionItem> itens = new Gson().fromJson(json, listType);
			defineItemsPriorities(itens);

			List<Pair<AuctionItem, Integer>> l = new LinkedList<>();
			priorities.forEach((k, v) -> l.add(new Pair<AuctionItem, Integer>(k, v)));
			String itemsJson = new Gson().toJson(l);

			ACLMessage newMsg = new ACLMessage(ACLMessage.INFORM);
			newMsg.setContent(MessageType.PRIORITIES.toString() + "\n" + itemsJson);
			newMsg.addReceiver(msg.getSender());
			send(newMsg);
		}

		private void processWinner(ACLMessage msg) {
			String json = msg.getContent().substring(msg.getContent().indexOf("\n"));
			Type listType = new TypeToken<Pair<String, AuctionItem>>() {
			}.getType();
			Pair<String, AuctionItem> winner = new Gson().fromJson(json, listType);
			if (winner != null && getAID().getName().equals(winner.getKey())) {
				money = money - winner.getValue().getPrice();
				money = new BigDecimal(money).setScale(2, RoundingMode.HALF_UP).doubleValue();
				for (AuctionItem ai : priorities.keySet()) {
					if (winner.getValue().getId().equals(ai.getId())) {
						itemsWon.put(winner.getValue(), priorities.get(ai));
					}
				}
			}
		}

		private void processOver(ACLMessage msg) {
			ACLMessage newMsg = new ACLMessage(ACLMessage.INFORM);
			int sum = 0;
			for (AuctionItem ai : itemsWon.keySet()) {
				sum += itemsWon.get(ai);
			}
			String itemsJson = new Gson().toJson(new double[] { sum, money });
			newMsg.setContent(MessageType.RESULTS.toString() + "\n" + itemsJson);
			newMsg.addReceiver(msg.getSender());
			send(newMsg);
			
			finish();
		}
		
		
		public void finish() {
			done();
			takeDown();
			myAgent.doDelete();
		}
	}
}
