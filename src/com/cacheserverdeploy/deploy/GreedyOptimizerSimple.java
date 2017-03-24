package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/** 
 * 简单移动比较快 
 */
public class GreedyOptimizerSimple extends GreedyOptimizer{

	/** 消费者到所有节点的费用 */
	private int[][] allCost;
	private int[][] allPreNodes;
	
	public GreedyOptimizerSimple(){
		// 初始化本地缓存
		allCost = new int[Global.consumerNum][Global.nodeNum];
		allPreNodes = new int[Global.consumerNum][Global.nodeNum];
		
		for(int i=0;i<Global.consumerNum;++i){
			Arrays.fill(allPreNodes[i], -1);
			initCost(i);
		}
	}
	
	@Override
	protected ArrayList<Server> transferServers(Server[] consumerServers, Map<Integer, Server> newServers) {
		
		ArrayList<Server> nextGlobalServers = new ArrayList<Server>();
		for(int consumerId=0;consumerId<consumerServers.length;++consumerId){	
			Server consumerServer = consumerServers[consumerId];
			if(Global.isMustServerNode[consumerServer.node]){
				// 肯定是服务器不用转移
				nextGlobalServers.add(consumerServer);
			}else{
				transfer(consumerId,consumerServer,newServers);
				if (consumerServer.getDemand()>0) {
					nextGlobalServers.add(consumerServer);
				}
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

		int[] costs = allCost[consumerId];
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
			int usedDemand = useBandWidthByPreNode(fromDemand, consumerId,serverNode);
			
			// 可以消耗
			if (usedDemand > 0) {
				fromDemand -= usedDemand;
				totalCost+=usedDemand*minCost;
		
				// 适配
				int[] preNodes = allPreNodes[consumerId];
				
				// 计算长度
				int len = 0;
				int pre = serverNode;
				while(pre!=-1){
					len++;
					pre = preNodes[pre];
				}
	
				// 逐个添加
				int[] viaNodes = new int[len];
				pre = serverNode;
				while(pre!=-1){
					viaNodes[--len] = pre;	
					pre = preNodes[pre];
				}
				
				transferTo(fromServer, toServers.get(serverNode), usedDemand,viaNodes);
				
				if (fromDemand == 0) {
					break;
				}
			}
		}
	
	}
	
	/**
	 * 消耗带宽最大带宽
	 * @return 消耗掉的带宽
	 */
	private int useBandWidthByPreNode(int demand, int consumerId, int serverNode) {
		int[] preNodes = allPreNodes[consumerId];
		int node1 = serverNode;
		int node0 = preNodes[node1];
		
		int minBindWidth = Global.INFINITY;
		while(node0!=-1){
			Edge edge = Global.graph[node1][node0];
			minBindWidth = Math.min(edge.leftBandWidth, minBindWidth);
			
			node1 = node0;
			node0 = preNodes[node0];
		}
		if (minBindWidth == 0) {
			return 0;
		}
		
		int usedBindWidth = Math.min(minBindWidth, demand);
		
		node1 = serverNode;
		node0 = preNodes[node1];
		while(node0!=-1){
			Edge edge = Global.graph[node1][node0];
			edge.leftBandWidth -= usedBindWidth;
			
			node1 = node0;
			node0 = preNodes[node0];
		}
		return usedBindWidth;
	}
	
	private void initCost(int consumerId) {

		int[] costs = allCost[consumerId];
		Arrays.fill(costs, Global.INFINITY);
		
		int[] visited = new int[Global.nodeNum];

		int[] preNodes = allPreNodes[consumerId];
		
		int startNode = Global.consumerNodes[consumerId];
		costs[startNode] = 0;
	
		while (true) {

			// 寻找下一个最近点
			int minCost = Global.INFINITY;
			int fromNode = -1;
			for (int node =0;node<Global.nodeNum;++node) {
				// 1 访问过了 或者 2 还没信息（cost 无穷大）
				if(visited[node]==1){
					continue;
				}
				if (costs[node] < minCost) {
					minCost = costs[node];
					fromNode = node;
				}
			}

			// 其余都不可达
			if (fromNode == -1) {
				break;
			}

			// 访问过了
			visited[fromNode] = 1;

			// 更新
			for (int toNode : Global.connections[fromNode]) {
				Edge edge = Global.graph[fromNode][toNode];
				int newCost = minCost + edge.cost;
				if (newCost < costs[toNode]) {
					costs[toNode] = newCost;
					// 添加路径
					preNodes[toNode] = fromNode;
				}
			}
			
		}
	}

}
