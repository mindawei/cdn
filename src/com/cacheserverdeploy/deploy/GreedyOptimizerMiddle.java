package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * 一个一个转移
 *
 * @author mindw
 * @date 2017年3月23日
 */
public final class GreedyOptimizerMiddle extends GreedyOptimizer{

	@Override
	protected ArrayList<Server> transferServers(Server[] consumerServers, Map<Integer, Server> newServers) {
		
		ArrayList<Server> nextGlobalServers = new ArrayList<Server>();
		for(int consumerId=0;consumerId<consumerServers.length;++consumerId){	
			Server consumerServer = consumerServers[consumerId];
			transfer(consumerServer,newServers,0);
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
	private void transfer(Server fromServer,Map<Integer, Server> toServers,int totalCost) {

		int fromNode = fromServer.node;
		int fromDemand = fromServer.getDemand();
		
		// 使用了多少个服务节点
		int leftServerNodeNum = toServers.size();
	
		int notVisitNodeNum = Global.nodeNum;
		// 0 未访问  1访问过
		int[] visited = new int[Global.nodeNum];
		int[] costs = new int[Global.nodeNum]; 
		Arrays.fill(costs, Global.INFINITY);
		int[][] allViaNodes = new int[Global.nodeNum][];
		
		// 自己到自己的距离为0
		costs[fromNode] = 0;
		allViaNodes[fromNode] = new int[]{fromNode};

		boolean fromDemandSmaller = false;
		
		while (notVisitNodeNum > 0) {

			// 寻找下一个最近点
			int minCost = Global.INFINITY;
			int minCostNode = -1;
			for (int node =0;node<Global.nodeNum;++node) {
				// 1 访问过了 或者 2 还没信息（cost 无穷大）
				if(visited[node]==1){
					continue;
				}
				int cost = costs[node];
				if (cost < minCost) {
					minCost = cost;
					minCostNode = node;
				}
			}

			// 其余都不可达
			if (minCostNode == -1) {
				break;
			}

			// 访问过了
			visited[minCostNode] = 1;
			notVisitNodeNum--;

			// 减枝
			if(fromDemand*minCost+totalCost>=Global.depolyCostPerServer){	
				fromDemand=0;
				fromDemandSmaller = true;
				break;
			}
						
			// 是服务器
			if (toServers.containsKey(minCostNode)) {
				int usedDemand = Global.useBandWidth(fromDemand,allViaNodes[minCostNode]);
				// 可以消耗
				if (usedDemand > 0) {
					Global.transferTo(fromServer, toServers.get(minCostNode), usedDemand, allViaNodes[minCostNode]);
					totalCost+= usedDemand * minCost;
					fromDemand -= usedDemand;
					fromDemandSmaller = true;
					leftServerNodeNum--;
					break;
				}
			}

			// 更新
			for (int toNode : Global.connections[minCostNode]) {
				// 访问过
				if (visited[toNode] == 1) { 
					continue;
				}

				Edge edge = Global.graph[toNode][minCostNode];
				if(edge.leftBandWidth==0){
					continue;
				}
				int newCost = costs[minCostNode] + edge.cost;
				if (newCost < costs[toNode]) {
					costs[toNode] = newCost;
					// 添加路径
					int nodeSize = allViaNodes[minCostNode].length;
					allViaNodes[toNode] = Arrays.copyOf(allViaNodes[minCostNode],nodeSize+1);
					allViaNodes[toNode][nodeSize] = toNode;
				}

			}
		}
		
		if(fromDemandSmaller&&fromDemand>0&&leftServerNodeNum>0){
			transfer(fromServer, toServers,totalCost);
		}
	
	}
}
