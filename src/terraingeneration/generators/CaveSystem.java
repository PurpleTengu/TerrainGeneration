package terraingeneration.generators;

import java.util.ArrayList;
import java.util.LinkedList;

import terraingeneration.World;

public class CaveSystem extends Generator{

	double rarity, startDepth, endDepth;
	int resolution, carve;
	int jaggedness, size;
	Cave cave;
	
	public CaveSystem(int carveblock,  double rarity, int resolution, int size, double startDepth, double endDepth){
		this(carveblock, rarity, resolution, size, startDepth, endDepth, new Cave());
	}
	
	public CaveSystem(int carveblock,  double rarity, int resolution, int size, double startDepth, double endDepth, Cave cave){
		this.rarity=rarity;
		this.resolution=resolution;
		this.jaggedness=1;
		this.size = size;
		this.startDepth=startDepth;
		this.endDepth=endDepth;
		this.cave=cave;
		this.carve=carveblock;
	}
	
	
	
	@Override
	public void applyToWorld(World w) {
		CaveNode.carveblock=carve;
		for(int x=0; x<w.getWidth(); x+=resolution){
			for(int y=(int) (w.getHeight()*startDepth); y<w.getHeight()*endDepth; y+=resolution){	
				if(w.getSeed().nextBoolean()){
					cave.setOrigin( new CaveNode(w, x, y, size));
					cave.populateCave(w);
					cave.fillCave(w);
				}
			}
		}
	}
}

class Cave{
	
	CaveNode originNode;
	static final int maxbranches = 2;
	
	public void setOrigin(CaveNode node){
		this.originNode = node;
	}
	
	public void populateCave(World w){
		populateCave(w,originNode);
	}
	
	public void populateCave(World w, CaveNode node){
		for(int i=0; i<maxbranches; i++){
			if(node.width>3 || w.getSeed().nextFloat()<=0.1){
				int si = node.width;
				if(w.getSeed().nextFloat()<=0.75){
					si = (int)(node.width*w.getSeed().nextFloat()/2-1);
				}
				if(si>=0){
					node.children.add(new CaveNode(w,node, si));
				}
			}
		}
		
		for(int i=0; i<node.children.size(); i++){
			populateCave(w,node.children.get(i));
		}
	}
	
	public void fillCave(World w){
		fillCave(w,originNode);
	}
	
	public static void fillCave(World w, CaveNode node) {
		for(int i=0; i<node.getChildren().size(); i++){
			CaveNode subnode = node.getChildren().get(i);
			float heightline=1;//OPTIMIZATION
			
			//establish direction to iterate
			int xd=1, yd=1;
			if(subnode.x!=node.x){
				heightline = (float)(subnode.y-node.y)/(subnode.x-node.x);
				xd = (subnode.x-node.x)/Math.abs(subnode.x-node.x);
			}
			if(subnode.y!=node.y){
				yd = (subnode.y-node.y)/Math.abs(subnode.y-node.y);
			}
			
			//iterates x, then y to draw line 
			int lasty=node.y;
			for(int x=node.x; (subnode.x-x)/xd >=0; x+= xd){
				int a=lasty;
				for(int y=lasty; y!=Math.round(node.y+(x-node.x)*heightline); y+=yd){
					//draw perpendicular to path: thickness
					//slope of line perpendicular to the line between the caves
					drawLine(w,x,y,-(float)(subnode.x-node.x)/(subnode.y-node.y),
							(int)(node.width-(float)(subnode.x-x)/(subnode.x-node.x)*(subnode.width-node.width))+w.getSeed().nextInt(1)-w.getSeed().nextInt(1)
							,CaveNode.carveblock);
					a=y;
				}
				if(lasty!=a){
					lasty=a;
				} else{
					drawLine(w,x,lasty,-(float)(subnode.x-node.x)/(subnode.y-node.y),
							(int)(node.width+(float)(subnode.x-x)/(subnode.x-node.x)*(subnode.width-node.width))+w.getSeed().nextInt(1)-w.getSeed().nextInt(1)//width of cave at this point
							,CaveNode.carveblock);
				}
			}
			if(node.x==node.getChildren().get(i).x){
				for(int y=node.y; y<subnode.y; y++){
					w.setTerrainAt(node.x, y, CaveNode.carveblock);
				}
			}
		}
		
		//call on all subunits
		for(int i=0; i<node.children.size(); i++){
			fillCave(w,node.children.get(i));
		}
	}
	
	public static void drawLine(World w, int x, int y, float slope, int cavethick, int cavekind){
		/*
		double angle = Math.atan(slope);
		double yoff = Math.sin(angle)*cavethick/2;
		double xoff = Math.cos(angle)*cavethick/2;
		//System.out.println(cavethick);
		
		float f =w.getSeed().nextFloat();
		
		int lasty=(int) (y-yoff);
		for(int tx=(int)(x-xoff); tx<x+xoff; tx+=xoff/Math.abs(xoff)){
			int nexty = (int) ((tx-x+xoff)/(x+xoff)*2*yoff - yoff);
			for(int ty=lasty; ty<=nexty; ty++){
				if(tx>=0 && ty>=0 && ty<w.getHeight() && tx<w.getWidth()){
					w.setTerrainAt(tx,ty,cavekind);
					System.out.println(f+" "+lasty+" "+ty+" "+nexty+" ");
				}
			}
			lasty=nexty;
		}
		*/
		
		if(Math.abs(slope)>1){
			for(int i=0; i<cavethick; i++){
				if(y+i-cavethick/2>=0 && y+i-cavethick/2<w.getHeight()){
					w.setTerrainAt(x,y+i-cavethick/2,cavekind);
				}
			}
		}else{
			for(int i=0; i<cavethick; i++){
				if(x+i-cavethick/2>=0 && x+i-cavethick/2<w.getWidth()){
					w.setTerrainAt(x+i-cavethick/2,y,cavekind);
				}
			}
		}
		
	}
	
}


class CaveNode{
	
	int x,y,width;
	CaveNode parentNode;
	
	static int carveblock;
	
	ArrayList<CaveNode> children = new ArrayList<CaveNode>(0);

	static final int resolution = 20;
	static final int cutoffbig = 5;
	
	public CaveNode(World w, int x, int y, int width){
		this.x=x;
		this.y=y;
		this.width=width;
		this.parentNode=null;
		limitToWorld(w);
	}
	
	public CaveNode(World w, CaveNode parentNode, int x, int y, int width){
		this.x=x;
		this.y=y;
		this.width=width;
		limitToWorld(w);
	}
	
	public CaveNode(World w,CaveNode parentNode, int width){
		this.parentNode=parentNode;
		this.width=width;
		this.x=parentNode.x+w.getSeed().nextInt(resolution)-w.getSeed().nextInt(resolution);
		if(width<cutoffbig){
			this.y=parentNode.y+w.getSeed().nextInt(resolution)-w.getSeed().nextInt(resolution);
		}
		else{
			this.y=parentNode.y+w.getSeed().nextInt(resolution);
		}
		this.children = new ArrayList<CaveNode>(0);
		limitToWorld(w);
	}
	
	void limitToWorld(World w){
		if(this.x<0){ this.x=0; }
		if(this.x>=w.getWidth()){ this.x=w.getWidth()-1; }
		if(this.y<0){ this.y=0; }
		if(this.y>=w.getHeight()){ this.y=w.getHeight()-1; }
		
	}
	
	public ArrayList<CaveNode> getChildren(){
		return this.children;
	}
	
	public boolean hasChildren(){
		return !this.children.isEmpty();
	}
	
	public String toString(){
		return "<Node "+this.x+", "+this.y+">";
	}
	
}
