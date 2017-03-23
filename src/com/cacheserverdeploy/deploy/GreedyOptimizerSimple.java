package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

/** 
 * 简单移动比较快 
 */
public final class GreedyOptimizerSimple extends GreedyOptimizer{
	
	@Override
	protected ArrayList<Server> transferServers(Server[] consumerServers, Map<Integer, Server> newServers) {
		
		ArrayList<Server> nextGlobalServers = new ArrayList<Server>();
		for(int consumerId=0;consumerId<consumerServers.length;++consumerId){	
			Server consumerServer = consumerServers[consumerId];
			transfer(consumerId,consumerServer,newServers);
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
	
	
	/** 将起始点需求分发到目的地点中，会改变边的流量<br> */
	private void transfer(int consumerId,Server fromServer, Map<Integer, Server> toServers) {
		// 0 未访问  1访问过
		int[] visited = new int[Global.nodeNum];

		int[] costs = Global.allCost[consumerId];
		int fromDemand = Global.consumerDemands[consumerId];
		int totalCost = 0;
		
		while (true) {

			// 寻找下一个最近点
			int minCost = Global.INFINITY;
			int serverNode = -1;
			for (int node : toServers.keySet()) {
				// 1 访问过了 或者 2 还没信息（cost 无穷大）
				if(visited[node]==1){
					continue;
				}
				int cost = costs[node];
				if (cost < minCost) {
					minCost = cost;
					serverNode = node;
				}
			}

			// 其余都不可达
			if (serverNode == -1) {
				break;
			}
			// 访问过了
			visited[serverNode] = 1;
			
			// 减枝
			if(totalCost+fromDemand*minCost>=Global.depolyCostPerServer){
				break;
			}
			
			// 是服务器
			//int usedDemand = Global.useBandWidth(fromDemand, viaNodes);
			int usedDemand = Global.useBandWidthByPreNode(fromDemand, consumerId,serverNode);
			
			// 可以消耗
			if (usedDemand > 0) {
				fromDemand -= usedDemand;
				totalCost+=usedDemand*minCost;
		
				// 适配
				LinkedList<Integer> lsNodes = new LinkedList<Integer>();
				int[] preNodes = Global.allPreNodes[consumerId];
				int pre = serverNode;
				while(pre!=-1){
					lsNodes.addFirst(pre);
					pre = preNodes[pre];
				}
				int[] viaNodes = new int[lsNodes.size()];
				int index = 0;
				for(int node : lsNodes){
					viaNodes[index++] = node;
				}
				Global.transferTo(fromServer, toServers.get(serverNode), usedDemand,viaNodes);
				
				// Global.transferTo(fromServer, toServers.get(serverNode), usedDemand,consumerId,serverNode);
				
				if (fromDemand == 0) {
					break;
				}
			}
		}
	
	}
}
