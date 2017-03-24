package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 防止在局部最优中出不来
 * 
 * @author mindw
 * @date 2017年3月23日
 */
public class GreedyOptimizerRandom extends GreedyOptimizerSimple{
	
	private final class NodeInfo implements Comparable<NodeInfo>{
    	int id;
    	int freq;
		@Override
		public int compareTo(NodeInfo o) {
			return o.freq-freq;
		}
    }
	
	private final ArrayList<Integer> nodes = new ArrayList<Integer>();
	
	private final Random random = new Random(47);
	
	public GreedyOptimizerRandom(){
		init();
	}
	
	private ArrayList<Integer> randomSelectServers() {
		
		ArrayList<Integer> nextServerNodes = new ArrayList<Integer>(Global.consumerNum);
		
		boolean[] selected = new boolean[nodes.size()];
		Arrays.fill(selected, false);
		int leftNum = Global.consumerNum / 4;
		for(int node : Global.mustServerNodes){
			nextServerNodes.add(node);
		}
		leftNum -= Global.mustServerNodes.length;
		
		while(leftNum>0){
			int index = random.nextInt(nodes.size());
			// 没有被选过
			if(!selected[index]){
				Integer node = nodes.get(index);
				if(Global.isMustServerNode[node]){
					selected[index] = true;
				}else{ //  是服务器，服务器上面已经添加过了
					selected[index] = true;
					nextServerNodes.add(node);
					leftNum--;
				}
			}
		}

		return nextServerNodes;
	}

	
	@Override
	void optimize() {

		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}

		if (Global.isTimeOut()) {
			return;
		}

		long t = System.currentTimeMillis();

			
		ArrayList<Integer> oldGlobalServers = randomSelectServers();
		
		int lastCsot = Global.INFINITY;
		
		while (true) {
				
			if (Global.IS_DEBUG) {
				for(int server : oldGlobalServers){
					System.out.print(server+" ");
				}
				System.out.println();
			}

			
			// 可选方案
			int minCost = Global.INFINITY;
			int bestFromNode = -1;
			int bestToNode = -1;

			// System.out.println(oldGlobalServers.size()*oldGlobalServers.size());
			
			int toNums = 2000 / oldGlobalServers.size();
			
			for (int fromNode : oldGlobalServers) {
				// 服务器不移动
				if(Global.isMustServerNode[fromNode]){
					continue;
				}
				int leftNum = toNums;
				for (int toNode : oldGlobalServers) {
					// 防止自己到自己
					if (fromNode == toNode) {
						continue;
					}

					if (Global.isTimeOut()) {
						return;
					}

					ArrayList<Server> nextGlobalServers = moveInRandom(oldGlobalServers, fromNode, toNode);
					int cost = Global.getTotalCost(nextGlobalServers);
					if (cost < minCost) {
						minCost = cost;
						bestFromNode = fromNode;
						bestToNode = toNode;
					}
					

					leftNum--;
					if(leftNum==0){
						break;
					}
				}
			}

			if (minCost == Global.INFINITY) {
				break;
			}

			if (Global.isTimeOut()) {
				return;
			}
			
			// 移动
			ArrayList<Server> nextGlobalServers = moveInRandom(oldGlobalServers, bestFromNode, bestToNode);
			int cost = Global.getTotalCost(nextGlobalServers);
			//boolean better = Global.updateSolution(nextGlobalServers);

			if (cost<lastCsot) {
				Global.updateSolution(nextGlobalServers);
				oldGlobalServers.clear();
				for(Server server : nextGlobalServers){
					oldGlobalServers.add(server.node);
				}
				lastCsot = cost;
			}else{ // not better
				lastCsot = Global.INFINITY;
				oldGlobalServers = randomSelectServers();
			}
			
		}

		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: " + (System.currentTimeMillis() - t));
		}
		
	}
	
	
	/** 进行一步移动 */
	private ArrayList<Server> moveInRandom(ArrayList<Integer> oldGlobalServers, int fromServerNode, int toServerNode) {
		
		Map<Integer, Server> newServers = new HashMap<Integer, Server>();
		for (int serverNode : oldGlobalServers) {
			if (serverNode != fromServerNode) {
				newServers.put(serverNode, new Server(serverNode));
			}
		}
		newServers.put(toServerNode, new Server(toServerNode));

		Server[] consumerServers = Global.getConsumerServer();

		Global.resetEdgeBandWidth();

		return transferServers(consumerServers, newServers);
	}

	/////////////////////////////

	//private final int N =1010;
	private int nearNodeId;
	private int[] limitBandWidth;
	
	private void init() {
		limitBandWidth = new int[Global.nodeNum];
		NodeInfo[] nodeFreq = new NodeInfo[Global.nodeNum];

		for(int i=0;i<Global.nodeNum;i++){
			nodeFreq[i]= new NodeInfo();
		}
		List<Integer> clientList = new ArrayList<Integer>(Global.consumerNodes.length);
		for(int client: Global.consumerNodes){
			clientList.add(client);
		}
		
		for(int client: Global.consumerNodes){
			nearNodeId=-1;
			int[] pre = dijkstra(client,clientList);
			for(int i=nearNodeId;i!=client;i=pre[i]){
				nodeFreq[i].id=i;
				nodeFreq[i].freq++;
			}
			nodeFreq[client].id=client;
			nodeFreq[client].freq++;
		}
		
		//Arrays.sort(nodeFreq);
		
		
		for(NodeInfo node : nodeFreq){
			if(node.freq>0){
				nodes.add(node.id);
			}
		}
		
	}

	private  int[] dijkstra(int s,List<Integer> clientList){
		boolean [] vis = new boolean[Global.nodeNum];
		int[] dis = new int[Global.nodeNum];
		int[] pre = new int[Global.nodeNum];
		Arrays.fill(pre, -1);
		for(int i=0;i<Global.nodeNum;i++){
			dis[i]=Integer.MAX_VALUE;
		}
		dis[s]=0;
		limitBandWidth[s]=Integer.MAX_VALUE;
		for(int i=0;i<Global.nodeNum;i++){
			int u=-1,Min=Integer.MAX_VALUE,limit=-1;
			for(int j=0;j<Global.nodeNum;j++){
				if(vis[j]==false&&(dis[j]<Min||dis[j]==Min&&limitBandWidth[j]>limit)){
					u=j;
					Min=dis[j];
					limit=limitBandWidth[j];
				}
			}
			if(u==-1){
				break;
			}
			if(u!=s&&clientList.contains(u)){
				nearNodeId=u;
				return pre;
			}
			vis[u]=true;
			for(int v=0;v<Global.nodeNum;v++){
				if(vis[v]==false&&Global.graph[u][v]!=null&&
						dis[v]>dis[u]+Global.graph[u][v].cost){
					dis[v]=dis[u]+Global.graph[u][v].cost;
					pre[v]=u;
					limitBandWidth[v]=Math.min(limitBandWidth[u], Global.graph[u][v].initBandWidth);
				}
			}
		}
		return pre;
	}

}
