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
public final class GreedyOptimizerComplex {

	static void optimize() {
		
		long t = System.currentTimeMillis();
	
		while(true) {
				
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
					
					if(Global.isTimeOut()){
						return;
					}
					
					//Global.saveBandWidth();
					ArrayList<Server> nextGlobalServers = moveComplex(oldGlobalServers,fromNode,toNode);
					int cost = Global.getTotalCost(nextGlobalServers);
					Global.updateSolution(nextGlobalServers);
					
					if (cost < minCost) {
						minCost = cost;
						bestFromNode = fromNode;
						bestToNode = toNode;
					}
					//Global.goBackBandWidth();
					
				}
			}
			
			if (minCost == Global.INFINITY) {
				break;
			}
			
			// 移动
			if(Global.isTimeOut()){
				return;
			}
			ArrayList<Server> nextGlobalServers = moveComplex(oldGlobalServers,bestFromNode,bestToNode);
			boolean better = Global.updateSolution(nextGlobalServers);
			
			if(!better){ // better
				break;
			}
		}

		if(Global.IS_DEBUG){
			System.out.println("阶段2耗时: "+(System.currentTimeMillis()-t));
		}
	}
	
	/** 复杂移动比较费时间 */
	private static ArrayList<Server>  moveComplex(ArrayList<Server> oldGlobalServers,int fromServerNode,int toServerNode){
		
		Map<Integer, Server> newServers = new HashMap<Integer, Server>();
		for (Server server : oldGlobalServers) {
			if (server.node != fromServerNode) {
				newServers.put(server.node, new Server(server.node));
			}
		}
		newServers.put(toServerNode, new Server(toServerNode));
				
		Global.resetEdgeBandWidth();
	
		Server[] consumerServers = Global.getConsumerServer();
		
		RouterComplex.transfer(consumerServers, newServers);
		
		ArrayList<Server> nextGlobalServers = new ArrayList<Server>();
		for(Server consumerServer : consumerServers){
			if (consumerServer.getDemand() > 0) { // 真正安装
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
