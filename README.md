# Jade_SistemasMultiAgentes_Leiloes

To run:

Start by running Jade, you can use this command:</br>
java -classpath jade/lib/jade.jar jade.Boot -GUI</br>
</br>
When the interface shows up, select the main container.</br>
Now you should create the agents.</br>
First create an Auctioneer, (com.sma.Auctioneer)</br>
When the auctioneer is created you will have 20 seconds to create the bidders.</br>
Note: 20 seconds correspond to the time for the join auction phase.</br>
Now create the Bidders (com.sma.Bidders)</br>
Once the bidders are created they will exchange messages to Auctioneer to join the Auction.</br>
Now you just need to wait for the Auction to start and see who is the best Bidder.</br>
