package com.cacheserverdeploy.deploy;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 路由
 * 
 * @author mindw
 * @date 2017年3月11日
 */
public final class Router {
	
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
			int fromNode = fromServer.nodeId;
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
					int bandWidthCanUsed = getBandWidthCanUsed(fromDemand, transferInfo.viaNodes);
			
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
				int usedDemand = useBandWidth(fromDemands[minFromLayer],minCostInfo.viaNodes);
				// 可以消耗
				if (usedDemand > 0) {
					minCostInfo.avaliableBandWidth = usedDemand;
					Server oldServer = minCostInfo.fromServer;
					Server newServer = toServers.get(minCostNode);
					oldServer.transferTo(newServer, minCostInfo);
					
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
	
	
	/**
	 * 消耗带宽最大带宽
	 * 
	 * @return 消耗掉的带宽,路由器到服务器，反方向消耗要
	 */
	public static int useBandWidth(int demand, int[] nodeIds) {
		if (demand == 0) {
			return 0;
		}
		int minBindWidth = Global.INFINITY;
		for (int i = nodeIds.length - 1; i >=1; --i) {
			Edge edge = Global.graph[nodeIds[i]][nodeIds[i -1]];
			minBindWidth = Math.min(edge.leftBandWidth, minBindWidth);
		}
		if (minBindWidth == 0) {
			return 0;
		}
		int usedBindWidth = Math.min(minBindWidth, demand);
		for (int i = nodeIds.length - 1; i >=1; --i) {
			Edge edge = Global.graph[nodeIds[i]][nodeIds[i -1]];
			edge.leftBandWidth -= usedBindWidth;
		}
		return usedBindWidth;
	}

	
	/**
	 * 消耗带宽最大带宽
	 * 
	 * @return 消耗掉的带宽,路由器到服务器，反方向消耗要
	 */
	public static int getBandWidthCanUsed(int demand, int[] nodeIds) {
		if (demand == 0) {
			return 0;
		}
		int minBindWidth = Global.INFINITY;
		for (int i = nodeIds.length - 1; i >=1; --i) {
			Edge edge = Global.graph[nodeIds[i]][nodeIds[i -1]];
			minBindWidth = Math.min(edge.leftBandWidth, minBindWidth);
		}
		return minBindWidth;
	}
	
	
	/** 将起始点需求分发到目的地点中，会改变边的流量<br> */
	public static List<TransferInfo> getToServerCost(int fromNode,int fromDemand, Set<Integer> toNodes) {

		// 使用了多少个服务节点
		int usedToNodeNum = 0;

		List<TransferInfo> result = new LinkedList<TransferInfo>();
		
		int notVisitNodeNum = Global.nodeNum;
		// 0 未访问  1访问过
		int[] visited = new int[Global.nodeNum];
		Arrays.fill(visited, 0);
		
		TransferInfo[] transferInfos = new TransferInfo[Global.nodeNum];
	
		// 自己到自己的距离为0
		transferInfos[fromNode] = new TransferInfo(0, new int[]{fromNode});

		boolean fromDemandSmaller = false;
		
		while (notVisitNodeNum > 0) {

			// 寻找下一个最近点
			int minCost = Global.INFINITY;
			int minCostNodeID = -1;
			for (int nodeId =0;nodeId<Global.nodeNum;++nodeId) {
				// 1 访问过了 或者 2 还没信息（cost 无穷大）
				if(visited[nodeId]==1||transferInfos[nodeId]==null){
					continue;
				}
				int cost = transferInfos[nodeId].cost;
				if (cost < minCost) {
					minCost = cost;
					minCostNodeID = nodeId;
				}
			}

			// 其余都不可达
			if (minCostNodeID == -1) {
				break;
			}

			// 访问过了
			visited[minCostNodeID] = 1;
			notVisitNodeNum--;

			TransferInfo minCostInfo = transferInfos[minCostNodeID];

			// 是服务器
			if (toNodes.contains(minCostNodeID)) {
				int usedDemand = useBandWidth(fromDemand,minCostInfo.viaNodes);
				// 可以消耗
				if (usedDemand > 0) {
					usedToNodeNum++;
					minCostInfo.avaliableBandWidth = usedDemand;
					minCostInfo.serverNode = minCostNodeID;					
					result.add(minCostInfo);
					fromDemand -= usedDemand;
					fromDemandSmaller = true;
					if (fromDemand == 0 || toNodes.size() == usedToNodeNum) {
						return result;
					}else{
						break;
					}
				}
			}

			// 更新
			for (int toNodeId : Global.connections[minCostNodeID]) {
				// 访问过
				if (visited[toNodeId] == 1) { 
					continue;
				}
				int[] viaNodes = transferInfos[minCostNodeID].viaNodes;
				int minBindWidth = Global.INFINITY;
				for (int i = viaNodes.length - 1; i >=1; --i) {
					Edge edge = Global.graph[viaNodes[i]][viaNodes[i -1]];
					minBindWidth = Math.min(edge.leftBandWidth, minBindWidth);
				}
				if(minBindWidth==0){
					continue;
				}
				
				if (transferInfos[toNodeId] == null) {
					transferInfos[toNodeId] = new TransferInfo(Global.INFINITY,null);
				}
				

				Edge edge = Global.graph[minCostNodeID][toNodeId];
				TransferInfo costInfo = transferInfos[toNodeId];
				int oldCost = costInfo.cost;
				//Edge edge = Global.graph[minCostNodeID][toNodeId];
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
		
		if(fromDemandSmaller&&fromDemand>0){
			result.addAll(getToServerCost(fromNode, fromDemand, toNodes));
		}
		return result;
	}



}
