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
public final class GreedyOptimizerSimple {

	static void optimize() {
		
		long t = System.currentTimeMillis();
	
		while(true) {
			
			if(Global.isTimeOut()){
				break;
			}
			
			// 可选方案
			int minCost = Global.INFINITY;
			int bestFromNode =-1;
			int bestToNode =-1;
			
			ArrayList<Server> oldGlobalServers = Global.getBestServers();
			for(Server server : oldGlobalServers){		
				int fromNode = server.node;
				for(int toNode =0;toNode<Global.nodeNum;++toNode){
					// 防止自己到自己
					if(fromNode==toNode){
						continue;
					}
					
					Global.saveBandWidth();
					ArrayList<Server> nextGlobalServers = moveSimple(oldGlobalServers,fromNode,toNode);
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
			ArrayList<Server> nextGlobalServers = moveSimple(oldGlobalServers,bestFromNode,bestToNode);
			boolean better = Global.updateSolution(nextGlobalServers);
			 
			if(!better){ // better
				break;
			}
		}

		if(Global.IS_DEBUG){
			System.out.println("moveSimple 耗时: "+(System.currentTimeMillis()-t));
		}
	}

	/** 简单移动比较快 */
	private static ArrayList<Server> moveSimple(ArrayList<Server> oldGlobalServers,int fromServerNode,int toServerNode) {
		
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
			RouterSimple.transfer(consumerId,consumerServer,newServers);
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
