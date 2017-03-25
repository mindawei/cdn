package com.cacheserverdeploy.deploy;

import java.util.ArrayList;

/**
 * 一个一个转移
 *
 * @author mindw
 * @date 2017年3月23日
 */
public class GreedyOptimizerMiddle extends GreedyOptimizer{
	
	private int[] consumerDemands = new int[Global.consumerNum];
	
	@Override
	protected ArrayList<Server> transferServers(Server[] newServers) {
		
		// 复制需求
		System.arraycopy(Global.consumerDemands, 0, consumerDemands, 0, Global.consumerNum);
		
		ArrayList<Server> nextGlobalServers = new ArrayList<Server>();
		for(int consumerId=0;consumerId<Global.consumerNum;++consumerId){	
			// 肯定是服务器不用转移  ？？ 加上效果不好 case50
//			if (Global.isMustServerNode[consumerServer.node]) {
//				nextGlobalServers.add(consumerServer);
//				continue;
//			} 
			// 减枝概率不大
			// 简单减枝计算，转移额最小费用
			int minCost = Global.INFINITY;
			for(int serverNode =0;serverNode<Global.nodeNum;++serverNode){
				if(newServers[serverNode]==null){
					continue;
				}
				if(Global.allCost[consumerId][serverNode]<minCost){
					minCost = Global.allCost[consumerId][serverNode];
				}
			}
			if(minCost*consumerDemands[consumerId]>=Global.depolyCostPerServer){
				nextGlobalServers.add(new Server(consumerId, Global.consumerNodes[consumerId], consumerDemands[consumerId]));
				continue;
			}
			
			while(transfer(consumerId, newServers));
			
			if (consumerDemands[consumerId] > 0) {
				nextGlobalServers.add(new Server(consumerId, Global.consumerNodes[consumerId], consumerDemands[consumerId]));
			}
			
		}
		
		for(int node =0;node<Global.nodeNum;++node){
			if(newServers[node]==null){
				continue;
			}
			Server newServer = newServers[node];
			if(newServer.getDemand()>0){
				nextGlobalServers.add(newServer);
			}
		}
		
		return nextGlobalServers;
	}
	
	private final boolean[] visited = new boolean[Global.nodeNum];
	private final int[] costs = new int[Global.nodeNum]; 
	private final int[] preNodes = new int[Global.nodeNum];
	// 13436
	/**  
	 * 将起始点需求分发到目的地点中，会改变边的流量<br>
	 * @return 是否需要继续转移 
	 */
	private boolean transfer(int consumerId,Server[] newServers) {

		for(int node=0;node<Global.nodeNum;++node){
			visited[node] = false;
			costs[node] =  Global.INFINITY;
			preNodes[node] = -1;
		}

		int fromNode = Global.consumerNodes[consumerId];

		// 使用了多少个服务节点
		int leftServerNodeNum = 0;
		for(Server server : newServers){
			if(server!=null){
				leftServerNodeNum++;
			}
		}
	
		// 自己到自己的距离为0
		costs[fromNode] = 0;
		// 是否找的一条减少需求的路
		boolean fromDemandSmaller = false;
		
		int minCost;
		int minCostNode;
		
		while (leftServerNodeNum > 0) {
		
			// 寻找下一个最近点
			minCost = Global.INFINITY;
			minCostNode = -1;
				
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
			if (newServers[minCostNode]!=null) {
				int usedDemand =useBandWidthByPreNode(consumerDemands[consumerId], minCostNode, preNodes);
				
				// 可以消耗
				if (usedDemand > 0) {		
					consumerDemands[consumerId] -= usedDemand;
					
					transferTo(consumerId, newServers[minCostNode], usedDemand, minCostNode, preNodes);
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
		
		if(consumerDemands[consumerId]>0&&fromDemandSmaller&&leftServerNodeNum>0){
			return true;
		}else{
			return false;
		}
	}
	
}
