package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 路由
 * 
 * @author mindw
 * @date 2017年3月11日
 */
public final class Router {

	/**
	 * 将起始点需求分发到目的地点中，会改变边的流量<br>
	 * 
	 * @param fromNode
	 * @param toNodes
	 */
	public static Map<Integer, TransferInfo> getToServerCost(int fromNode,int fromDemand, Set<Integer> toNodes) {

		// 使用了多少个服务节点
		int usedToNodeNum = 0;

		Map<Integer, TransferInfo> returnMap = new HashMap<Integer, TransferInfo>(toNodes.size());
		
		int notVisitNodeNum = Global.nodeNum;
		// 0 未访问  1访问过
		int[] visited = new int[Global.nodeNum];
		Arrays.fill(visited, 0);
		
		TransferInfo[] transferInfos = new TransferInfo[Global.nodeNum];
	
		// 自己到自己的距离为0
		ArrayList<Integer> nodes = new ArrayList<Integer>();
		nodes.add(fromNode);
		transferInfos[fromNode] = new TransferInfo(0, nodes);

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
				int usedDemand = Global.useBandWidth(fromDemand,minCostInfo.nodes);
				// 可以消耗
				if (usedDemand > 0) {
					usedToNodeNum++;
					minCostInfo.avaliableBandWidth = usedDemand;
					returnMap.put(minCostNodeID, minCostInfo);
					fromDemand -= usedDemand;
					if (fromDemand == 0 || toNodes.size() == usedToNodeNum) {
						return returnMap;
					}
				}
			}

			// 更新
			for (int toNodeId : Global.connections[minCostNodeID]) {
				// 访问过
				if (visited[toNodeId] == 1) { 
					continue;
				}
				
				if (transferInfos[toNodeId] == null) {
					transferInfos[toNodeId] = new TransferInfo(Global.INFINITY,null);
				}

				TransferInfo costInfo = transferInfos[toNodeId];
				int oldCost = costInfo.cost;
				Edge edge = Global.graph[minCostNodeID][toNodeId];
				int newCost = minCost + edge.cost;
				if (newCost < oldCost) {
					costInfo.cost = newCost;
					costInfo.nodes = new ArrayList<Integer>(minCostInfo.nodes);
					costInfo.nodes.add(toNodeId);
				}

			}
			
		}
		return returnMap;
	}

}
