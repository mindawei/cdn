package com.cacheserverdeploy.deploy;

import java.util.Arrays;
import java.util.HashMap;
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


}
