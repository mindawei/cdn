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
public class GreedyOptimizerMiddle extends GreedyOptimizer{
	
	@Override
	protected ArrayList<Server> transferServers(Server[] consumerServers, Map<Integer, Server> newServers) {
		
		ArrayList<Server> nextGlobalServers = new ArrayList<Server>();
		for(int consumerId=0;consumerId<consumerServers.length;++consumerId){	
			Server consumerServer = consumerServers[consumerId];
			
			// 肯定是服务器不用转移  ？？ 加上效果不好 case50
//			if (Global.isMustServerNode[consumerServer.node]) {
//				nextGlobalServers.add(consumerServer);
//				continue;
//			} 
			
			// 简单减枝计算，转移额最小费用
			int minCost = Global.INFINITY;
			for(int serverNode : newServers.keySet()){
				if(Global.allCost[consumerId][serverNode]<minCost){
					minCost = Global.allCost[consumerId][serverNode];
				}
			}
			if(minCost*consumerServer.getDemand()>=Global.depolyCostPerServer){
				nextGlobalServers.add(consumerServer);
				continue;
			}
			
			while(transfer(consumerServer, newServers));
			
			if (consumerServer.getDemand() > 0) {
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
	
	private final boolean[] visited = new boolean[Global.nodeNum];
	private final int[] costs = new int[Global.nodeNum]; 
	private final int[] preNodes = new int[Global.nodeNum];
	
	/**  
	 * 将起始点需求分发到目的地点中，会改变边的流量<br>
	 * @return 是否需要继续转移 
	 */
	private boolean transfer(Server fromServer,Map<Integer, Server> toServers) {

		Arrays.fill(visited, false);
		Arrays.fill(costs, Global.INFINITY);
		Arrays.fill(preNodes, -1);

		int fromNode = fromServer.node;
		int fromDemand = fromServer.getDemand();
		// 使用了多少个服务节点
		int leftServerNodeNum = toServers.size();
	
		// 自己到自己的距离为0
		costs[fromNode] = 0;
		// 是否找的一条减少需求的路
		boolean fromDemandSmaller = false;
		
		while (leftServerNodeNum > 0) {

			// 寻找下一个最近点
			int minCost = Global.INFINITY;
			int minCostNode = -1;
			for (int node =0;node<Global.nodeNum;++node) {
				// 1 访问过了 或者 2 还没信息（cost 无穷大）
				if(visited[node]){
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
			}else{
				// 访问过了
				visited[minCostNode] = true;
			}
						
			// 是服务器
			if (toServers.containsKey(minCostNode)) {
				int usedDemand =useBandWidthByPreNode(fromDemand, minCostNode, preNodes);
				// 可以消耗
				if (usedDemand > 0) {			
					transferTo(fromServer, toServers.get(minCostNode), usedDemand, minCostNode, preNodes);
					fromDemand -= usedDemand;
					fromDemandSmaller = true;
					leftServerNodeNum--;
					break;
				}
			}

			// 更新
			for (int toNode : Global.connections[minCostNode]) {
				// 访问过
				if (visited[toNode]) { 
					continue;
				}
				// 反向流量
				Edge edge = Global.graph[toNode][minCostNode];
				if(edge.leftBandWidth==0){
					continue;
				}
				int newCost = costs[minCostNode] + edge.cost;
				if (newCost < costs[toNode]) {
					costs[toNode] = newCost;
					// 添加路径
					preNodes[toNode] = minCostNode;
				}
			}
		}
		
		if(fromDemand>0&&fromDemandSmaller&&leftServerNodeNum>0){
			return true;
		}else{
			return false;
		}
	}
	
}
