package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 贪心搜索，寻找一个最优解，达到具备最优解后就马上退出
 * 
 * @author mindw
 * @date 2017年3月12日
 */
public final class GreedyOptimizerSimplest {

	static void optimize() {
		
		long t = System.currentTimeMillis();
	
		while(true) {
				
			// 可选方案
			int minCost = Global.INFINITY;
			int bestFromNode =-1;
			int bestToNode =-1;
			
			ArrayList<Server> oldGlobalServers = Global.getBestServers();
			for(Server fromServer : oldGlobalServers){		
				int fromNode = fromServer.node;
				for(int toNode : Global.connections[fromNode]){		
				
					if(Global.isTimeOut()){
						return;
					}
					
					Global.saveBandWidth();
					ArrayList<Server> nextGlobalServers = moveMiddle(oldGlobalServers,fromNode,toNode);
					int cost = Global.getTotalCost(nextGlobalServers);
					if (cost < minCost) {
						minCost = cost;
						bestFromNode = fromNode;
						bestToNode = toNode;
					}
					Global.goBackBandWidth();
					
				}
			}
			
			if (minCost == Global.INFINITY) {
				break;
			}
			
			// 移动
			if(Global.isTimeOut()){
				return;
			}
			ArrayList<Server> nextGlobalServers = moveMiddle(oldGlobalServers,bestFromNode,bestToNode);
			boolean better = Global.updateSolution(nextGlobalServers);
			
			if(!better){ // better
				break;
			}
		}

		if(Global.IS_DEBUG){
			System.out.println("阶段2耗时: "+(System.currentTimeMillis()-t));
		}
	}
	
	/** 简单移动比较快 */
	private static ArrayList<Server> moveMiddle(ArrayList<Server> oldGlobalServers,int fromServerNode,int toServerNode) {
		
		Map<Integer, Server> newServers = new HashMap<Integer, Server>();
		for (Server server : oldGlobalServers) {
			if (server.node != fromServerNode) {
				newServers.put(server.node, new Server(server.node));
			}
		}
		newServers.put(toServerNode, new Server(toServerNode));
				
		Global.resetEdgeBandWidth();
	
		Server[] consumerServers = Global.getConsumerServer();
		
		ArrayList<Server> nextGlobalServers = new ArrayList<Server>();
		for(int consumerId=0;consumerId<consumerServers.length;++consumerId){	
			Server consumerServer = consumerServers[consumerId];
			RouterMiddle.transfer(consumerServer,newServers,0);
			if (consumerServer.getDemand()>0) {
				nextGlobalServers.add(consumerServer);
			}
		}
		
		for(Server newServer : newServers.values()){
			if (newServer.getDemand() > 0) { // 真正安装
				nextGlobalServers.add(newServer);
			}
		}
		
		return nextGlobalServers;
	}
	
}
