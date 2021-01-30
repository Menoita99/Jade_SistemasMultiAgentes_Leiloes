# Jade_SistemasMultiAgentes_Leiloes

To run:

Start by running Jade, you can use this command:
java -classpath jade/lib/jade.jar jade.Boot -GUI

When the interface shows up, select the main container.
</br>
Now you should create the agents.
First create an Auctioneer, (com.sma.Auctioneer)
When the auctioneer is created you will have 20 seconds to create the bidders.
Note: 20 seconds correspond to the time for the join auction phase.
Now create the Bidders (com.sma.Bidders)
Once the bidders are created they will exchange messages to Auctioneer to join the Auction.
Now you just need to wait for the Auction to start and see who is the best Bidder.
