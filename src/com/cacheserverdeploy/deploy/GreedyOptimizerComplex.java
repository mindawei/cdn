package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/** 
 * 复杂移动比较费时间
 */
public final class GreedyOptimizerComplex extends GreedyOptimizer{

	@Override
	protected ArrayList<Server> transferServers(Server[] consumerServers, Map<Integer, Server> newServers) {
		transfer(consumerServers, newServers);
		
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
	
	private class CostInfo {
		
		Server fromServer;
		
		/** 到达需要的费用：单位花费 */
		int cost; 
		
		/** 经过的节点ID,包括了首尾,从消费者开始，服务器路径应该逆向 */
		int[] viaNodes;
		
		public CostInfo(Server fromServer,int cost,int[] viaNodes) {
			super();
			this.fromServer = fromServer;
			this.cost = cost;
			this.viaNodes = viaNodes;
		}
			
	}
	
	/** 将起始点需求分发到目的地点中，会改变边的流量<br> */
	public void transfer(Server[] fromServers, Map<Integer, Server> toServers) {
		// 0 未访问  1访问过
		int layerNum = fromServers.length;
		int totalFromDemand = 0;
		int[] fromDemands = new int[layerNum];
		int[] notVisitedServerNum = new int[layerNum];
		int[] fromCosts = new int[layerNum];
		
		int serverNum = toServers.size();
		for (int layer = 0; layer < layerNum; ++layer) {
			Server fromServer = fromServers[layer];	
			// 需求
			int fromDemand = fromServer.getDemand();
			notVisitedServerNum[layer] = serverNum;
			fromCosts[layer] =0;
			fromDemands[layer] = fromDemand;
			totalFromDemand += fromDemand;
		}
		
		_transfer(fromServers, toServers, fromDemands, totalFromDemand,notVisitedServerNum,fromCosts);
	}

	/** 将起始点需求分发到目的地点中，会改变边的流量<br> */
	private  void _transfer(Server[] fromServers, Map<Integer, Server> toServers,int[] fromDemands,int totalFromDemand,int[] notVisitedServerNum,int[] fromCosts) {

		// 0 未访问  1访问过
		int layerNum = fromServers.length;
		int[][] visited = new int[layerNum][Global.nodeNum];
		
		CostInfo[][] transferInfos = new CostInfo[layerNum][Global.nodeNum];	
		for (int layer = 0; layer < layerNum; ++layer) {

			// 初始化
			transferInfos[layer] = new CostInfo[Global.nodeNum];
			// 自己到自己的距离为0
			Server fromServer = fromServers[layer];
			int fromNode = fromServer.node;
			transferInfos[layer][fromNode] = new CostInfo(fromServer, 0 , new int[] { fromNode });
		}
		
		boolean fromDemandSmaller = false;
		
		while (true) {

			// 寻找下一个最近点
			int minCost = Global.INFINITY;
			int minFromLayer = -1;
			int minCostNode = -1;
			
			for (int layer =0;layer<layerNum;++layer) {
				for (int node = 0; node < Global.nodeNum; ++node) {
					// 1 访问过了 或者 2 还没信息（cost 无穷大）
					if (visited[layer][node] == 1 || notVisitedServerNum[layer]==0|| fromDemands[layer]==0) {
						continue;
					}
					CostInfo transferInfo = transferInfos[layer][node];
					if(transferInfo==null){
						continue;
					}
					int cost = transferInfos[layer][node].cost;				
					if (cost < minCost){
						minCost = cost;
						minFromLayer = layer;
						minCostNode = node;
					}
				}
			}

			// 其余都不可达
			if (minCostNode == -1) {
				break;
			}
			
			// 访问过了
			visited[minFromLayer][minCostNode] = 1;
			
			CostInfo minCostInfo = transferInfos[minFromLayer][minCostNode];
			// 减枝
			if(fromDemands[minFromLayer]*minCost+fromCosts[minFromLayer]>=Global.depolyCostPerServer){
				totalFromDemand -= fromDemands[minFromLayer];
				fromDemands[minFromLayer]=0;
				fromDemandSmaller = true;
				break;
			}

			// 是服务器
			if (toServers.containsKey(minCostNode)) {
				int usedDemand = useBandWidth(fromDemands[minFromLayer],minCostInfo.viaNodes);
				// 可以消耗
				if (usedDemand > 0) {
					fromCosts[minFromLayer]+= usedDemand * minCost;
					notVisitedServerNum[minFromLayer]--;
					transferTo(minCostInfo.fromServer,toServers.get(minCostNode),usedDemand,minCostInfo.viaNodes);
					totalFromDemand -= usedDemand;
					fromDemands[minFromLayer]-=usedDemand;
					fromDemandSmaller = true;
					break;
				}
			}
			
			// 更新
			for (int toNodeId : Global.connections[minCostNode]) {
				// 访问过
				if (visited[minFromLayer][toNodeId] == 1) { 
					continue;
				}
									
				Edge edge = Global.graph[toNodeId][minCostNode];
				if(edge.leftBandWidth==0){
					continue;
				}

				if (transferInfos[minFromLayer][toNodeId] == null) {
					transferInfos[minFromLayer][toNodeId] = new CostInfo(transferInfos[minFromLayer][minCostNode].fromServer,Global.INFINITY,null);
				}				
				CostInfo costInfo = transferInfos[minFromLayer][toNodeId];
				int oldCost = costInfo.cost;
				int newCost = minCost + edge.cost;
				if (newCost < oldCost) {
					costInfo.cost = newCost;
					// 添加路径
					int nodeSize = minCostInfo.viaNodes.length;
					costInfo.viaNodes = Arrays.copyOf(minCostInfo.viaNodes,nodeSize+1);
					costInfo.viaNodes[nodeSize] = toNodeId;
				}
			}
		}
		
		if(fromDemandSmaller&&totalFromDemand>0){
			_transfer(fromServers, toServers, fromDemands, totalFromDemand,notVisitedServerNum,fromCosts);
		}
	}
}
