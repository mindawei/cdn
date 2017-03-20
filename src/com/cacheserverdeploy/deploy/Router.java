package com.cacheserverdeploy.deploy;

import java.util.Map;

/**
 * 路由
 * 
 * @author mindw
 * @date 2017年3月11日
 */
public final class Router {
	
	/** 将起始点需求分发到目的地点中，会改变边的流量<br> */
	public static void transfer(int consumerId,Server fromServer, Map<Integer, Server> toServers) {
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
			
			// 中庸的减枝
			if(totalCost+fromDemand*minCost>=Global.depolyCostPerServer){
				break;
			}
			
			// 是服务器
			int[] viaNodes = Global.allViaNode[consumerId][serverNode];
			int usedDemand = Global.useBandWidth(fromDemand, viaNodes);
			// 可以消耗
			if (usedDemand > 0) {
				fromDemand -= usedDemand;
				totalCost+=usedDemand*minCost;
				Global.transferTo(fromServer, toServers.get(serverNode), usedDemand,viaNodes);
				if (fromDemand == 0) {
					break;
				}
			}
		}
	
	}
	
	

}
