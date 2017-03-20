package com.cacheserverdeploy.deploy;

import java.util.Arrays;
import java.util.Map;

/**
 * 路由
 * 
 * @author mindw
 * @date 2017年3月11日
 */
public final class RouterComplex {
	
	/** 将起始点需求分发到目的地点中，会改变边的流量<br> */
	public static void transfer(Server[] fromServers, Map<Integer, Server> toServers) {
		// 0 未访问  1访问过
		int layerNum = fromServers.length;
		int totalFromDemand = 0;
		int[] fromDemands = new int[layerNum];
		int[][] visited = new int[layerNum][Global.nodeNum];
		for (int layer = 0; layer < layerNum; ++layer) {
			visited[layer] = new int[Global.nodeNum];
			// 自己到自己的距离为0
			Server fromServer = fromServers[layer];	
			// 需求
			int fromDemand = fromServer.getDemand();
			fromDemands[layer] = fromDemand;
			totalFromDemand += fromDemand;
		}
		
		_transfer(fromServers, toServers, fromDemands, totalFromDemand,visited);
	}

	/** 将起始点需求分发到目的地点中，会改变边的流量<br> */
	private static void _transfer(Server[] fromServers, Map<Integer, Server> toServers,int[] fromDemands,int totalFromDemand,int[][] visited) {

		// 0 未访问  1访问过
		int layerNum = fromServers.length;
		
		TransferInfo[][] transferInfos = new TransferInfo[layerNum][Global.nodeNum];	
		for (int layer = 0; layer < layerNum; ++layer) {
			// 全部未访问过
			Arrays.fill(visited[layer], 0);
			
			// 初始化
			transferInfos[layer] = new TransferInfo[Global.nodeNum];
			// 自己到自己的距离为0
			Server fromServer = fromServers[layer];
			int fromNode = fromServer.node;
			transferInfos[layer][fromNode] = new TransferInfo(fromServer, 0 , new int[] { fromNode });
		}
		
		boolean fromDemandSmaller = false;
		
		while (true) {

			// 寻找下一个最近点
			int minCost = Global.INFINITY;
			int minFromLayer = -1;
			int minCostNode = -1;
			int maxBandWidthCanUsed = -1; 
			
			for (int layer =0;layer<layerNum;++layer) {
				for (int node = 0; node < Global.nodeNum; ++node) {
					// 1 访问过了 或者 2 还没信息（cost 无穷大）
					if (visited[layer][node] == 1) {
						continue;
					}
					TransferInfo transferInfo = transferInfos[layer][node];
					if(transferInfo==null){
						continue;
					}
					int cost = transferInfos[layer][node].cost;
					int fromDemand = fromDemands[layer];
					int bandWidthCanUsed = Global.getBandWidthCanUsed(fromDemand, transferInfo.viaNodes);
			
					if(bandWidthCanUsed==0){
						continue;
					}
					
					if (cost < minCost
							|| (cost==minCost&&bandWidthCanUsed>maxBandWidthCanUsed)) {
						minCost = cost;
						minFromLayer = layer;
						minCostNode = node;
						maxBandWidthCanUsed = bandWidthCanUsed;
					}
				}
			}

			// 其余都不可达
			if (minCostNode == -1) {
				break;
			}
			
			// 访问过了
			visited[minFromLayer][minCostNode] = 1;
			
			
			TransferInfo minCostInfo = transferInfos[minFromLayer][minCostNode];

			// 是服务器
			if (toServers.containsKey(minCostNode)) {
				int usedDemand = Global.useBandWidth(fromDemands[minFromLayer],minCostInfo.viaNodes);
				// 可以消耗
				if (usedDemand > 0) {
					minCostInfo.avaliableBandWidth = usedDemand;
					Server oldServer = minCostInfo.fromServer;
					Server newServer = toServers.get(minCostNode);
					
					Global.transferTo(oldServer,newServer,minCostInfo.avaliableBandWidth,minCostInfo.viaNodes);
							
					
					totalFromDemand -= usedDemand;
					fromDemands[minFromLayer]-=usedDemand;
					fromDemandSmaller = true;
					break;
				}
			}

			// 道路上是否还有流量
			int[] viaNodes = transferInfos[minFromLayer][minCostNode].viaNodes;
			int minBindWidth = Global.INFINITY;
			for (int i = viaNodes.length - 1; i >=1; --i) {
				Edge edge = Global.graph[viaNodes[i]][viaNodes[i -1]];
				minBindWidth = Math.min(edge.leftBandWidth, minBindWidth);
			}
			if(minBindWidth==0){
				continue;
			}
			
			// 更新
			for (int toNodeId : Global.connections[minCostNode]) {
				// 访问过
				if (visited[minFromLayer][toNodeId] == 1) { 
					continue;
				}
			
				if (transferInfos[minFromLayer][toNodeId] == null) {
					transferInfos[minFromLayer][toNodeId] = new TransferInfo(transferInfos[minFromLayer][minCostNode].fromServer,Global.INFINITY,null);
				}
				
				Edge edge = Global.graph[minCostNode][toNodeId];
				TransferInfo costInfo = transferInfos[minFromLayer][toNodeId];
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
			_transfer(fromServers, toServers, fromDemands, totalFromDemand,visited);
		}
	}
	
}
