package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 路由
 * @author mindw
 * @date 2017年3月11日
 */
public final class Router {
	/**
	 * 获得点到各个点的单位消耗<br>
	 * 算法：dijkstra<br>
	 */
	private static Map<String,TransferInfo> getToNodeCost(String fromNodeId){

		Map<String,TransferInfo> costMap = new HashMap<String, TransferInfo>();

		// 未访问过的节点ID
		Set<String> notVisitNodeIds = new HashSet<String>();
		notVisitNodeIds.addAll(Global.nodes);
		
		// 初始化
		for(String nodeId : Global.nodes){
			costMap.put(nodeId, new TransferInfo(Global.INFINITY,null));
		}
		// 自己到自己的距离为0
		ArrayList<String> nodes = new ArrayList<String>();
		nodes.add(fromNodeId);
		costMap.put(fromNodeId, new TransferInfo(0,nodes));
		
		while(notVisitNodeIds.size()>0){
			
			// 寻找下一个最近点
			int minCost = Global.INFINITY;
			String minCostNodeID = null;
			for(String nodeId : notVisitNodeIds){
				int cost = costMap.get(nodeId).cost;
				if(cost<minCost){
					minCost = cost;
					minCostNodeID = nodeId;
				}
			}
			
			// 其余都不可达
			if(minCostNodeID==null){ 
				break;
			}
			
			// 移除
			notVisitNodeIds.remove(minCostNodeID);
			
			TransferInfo minCostInfo = costMap.get(minCostNodeID);
			
			// 更新
			for (String toNodeId : Global.getToNodeIds(minCostNodeID)) {
				if (notVisitNodeIds.contains(toNodeId)) {
					TransferInfo costInfo = costMap.get(toNodeId);
					
					int oldCost = costInfo.cost;
					
					Edge edge = Global.getEdge(minCostNodeID, toNodeId);
					int newCost = minCost + edge.cost;
					
					if(newCost<oldCost){
						costInfo.cost = newCost;
						costInfo.nodes = new ArrayList<String>(minCostInfo.nodes);
						costInfo.nodes.add(toNodeId);
					}
				}
			}
			
		}
		return costMap;
	}

	/**
	 * 将起始点需求分发到目的地点中，会改变边的流量<br>
	 * @param fromNode
	 * @param toNodes
	 */
	public static Map<String, TransferInfo> getToServerCost(String fromNode,int fromDemand,Set<String> toNodes) {
		
		// 使用了多少个服务节点
		int usedToNodeNum = 0;
		
		Map<String,TransferInfo> costMap = new HashMap<String, TransferInfo>();

		// 未访问过的节点ID
		Set<String> notVisitNodeIds = new HashSet<String>();
		notVisitNodeIds.addAll(Global.nodes);
		
		// 初始化
		for(String nodeId : Global.nodes){
			costMap.put(nodeId, new TransferInfo(Global.INFINITY,null));
		}
		// 自己到自己的距离为0
		ArrayList<String> nodes = new ArrayList<String>();
		nodes.add(fromNode);
		costMap.put(fromNode, new TransferInfo(0,nodes));
		
		while(notVisitNodeIds.size()>0){
			
			// 寻找下一个最近点
			int minCost = Global.INFINITY;
			String minCostNodeID = null;
			for(String nodeId : notVisitNodeIds){
				int cost = costMap.get(nodeId).cost;
				if(cost<minCost){
					minCost = cost;
					minCostNodeID = nodeId;
				}
			}
			
			// 其余都不可达
			if(minCostNodeID==null){ 
				break;
			}
			
			// 移除
			notVisitNodeIds.remove(minCostNodeID);
			
			TransferInfo minCostInfo = costMap.get(minCostNodeID);
				
			// 是服务器
			if (toNodes.contains(minCostNodeID)) {
				int usedDemand = Global.useBandWidth(fromDemand,minCostInfo.nodes);
				// 可以消耗
				if (usedDemand > 0) {
					usedToNodeNum++;
					minCostInfo.avaliableBandWidth = usedDemand;
					fromDemand -= usedDemand;
					
					
					
					if (fromDemand == 0 || toNodes.size() == usedToNodeNum) {
						break;
					}
				}
			}

			// 更新
			for (String toNodeId : Global.getToNodeIds(minCostNodeID)) {
				if (notVisitNodeIds.contains(toNodeId)) {
					TransferInfo toCostInfo = costMap.get(toNodeId);
					
					int oldCost = toCostInfo.cost;
					
					Edge edge = Global.getEdge(minCostNodeID, toNodeId);
					int newCost = minCost + edge.cost;
					
					if(newCost<oldCost){
						toCostInfo.cost = newCost;
						toCostInfo.nodes = new ArrayList<String>(minCostInfo.nodes);
						toCostInfo.nodes.add(toNodeId);
					}
				}
			}
			
		}
		
		Map<String,TransferInfo> returnMap = new HashMap<String, TransferInfo>();
		for(String toNode : toNodes){
			if(!costMap.containsKey(toNode)){
				continue;
			}
			if(costMap.get(toNode).avaliableBandWidth>0){
				returnMap.put(toNode, costMap.get(toNode));
			}
			
		
		}
		return returnMap;
	}
	
}
