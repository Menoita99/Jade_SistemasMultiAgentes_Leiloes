package com.sma;

import java.util.Random;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuctionItem {

	private int price;
	private String id;

	private final static String[] objects = "carrots,face wash,balloon,keys,piano,leg warmers,key chain,milk,sharpie,toothpaste,tv,nail clippers,white out,button,bananas,sandal,ice cube tray,seat belt,puddle,playing card,teddies,conditioner,shoe lace,USB drive,shovel,door,coasters,clothes,thermometer,clamp,pool stick,outlet,cinder block,sun glasses,magnet,eraser,lace,street lights,paint brush,CD,packing peanuts,tomato,bookmark,sponge,doll,ipod,scotch tape,canvas,mouse pad,video games".split(",");
	private final static String[] adjectives = "able,bad,best,better,big,black,certain,clear,different,early,easy,economic,federal,free,full,good,great,hard,high,human,important,international,large,late,little,local,long,low,major,military,national,new,old,only,other,political,possible,public,real,recent,right,small,social,special,strong,sure,true,white,whole,young".split(",");

	public AuctionItem() {
		price = (int) (Math.random() * 50);
		id = objects[new Random().nextInt(objects.length)] + adjectives[new Random().nextInt(adjectives.length)];
	}
}
