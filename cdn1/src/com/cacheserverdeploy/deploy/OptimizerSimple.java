package com.cacheserverdeploy.deploy;

import java.util.Arrays;

// simple部分
public class OptimizerSimple extends Optimizer{
	/** 新服务器是否已经安装 */
	private final boolean[] isNewServerInstalled = new boolean[Global.nodeNum];
	/** 是否是新服务器 */
	private final boolean[] isNewServer = new boolean[Global.nodeNum];
	
	/** 进行一步移动 */
	protected int getCostAfterMove(int fromServerNode, int toServerNode) {
		
		Global.resetEdgeBandWidth();
		
		Arrays.fill(isNewServer, false);
		
		for (int i=0;i<serverNodesSize;++i) {
			int serverNode = serverNodes[i];
			isNewServer[serverNode] = true;
			isNewServerInstalled[serverNode] = false;
		}
		isNewServer[fromServerNode] = false;
		isNewServer[toServerNode] = true;
		isNewServerInstalled[toServerNode] = false;
		
		int cost = 0;

		for(int consumerId=0;consumerId<Global.consumerNum;++consumerId){	
		
			if(Global.isConsumerServer[consumerId]){
				cost += Global.depolyCostPerServer;
				continue;
			}
			
			int consumerDemand = Global.consumerDemands[consumerId];	
			// 将起始点需求分发到目的地点中，会改变边的流量	
			for(int node : Global.allPriorityCost[consumerId]){
				
				// 不是服务器
				if(!isNewServer[node]){
					continue;
				}
				
				int usedDemand = Global.useBandWidthByPreNode(consumerDemand, node, Global.allPreNodes[consumerId]);
				// 可以消耗
				if (usedDemand > 0) {	
					if(!isNewServerInstalled[node]){
						cost+=Global.depolyCostPerServer;
						isNewServerInstalled[node] = true;
					}
					int[] preNodes = Global.allPreNodes[consumerId];
					int node1 = node;
					int node0 = preNodes[node1];
					while(node0!=-1){
						Edge edge = Global.graph[node1][node0];
						cost +=  edge.cost * usedDemand;
						node1 = node0;
						node0 = preNodes[node0];
					}
					consumerDemand -= usedDemand;
					if(consumerDemand==0){
						break;
					}
				}
			}
			
			if(consumerDemand>0){
				cost+=Global.depolyCostPerServer;
			}
		}
		return cost;
	}
	
	/** 进行一步真正的移动  */
	protected void moveBest(int fromServerNode, int toServerNode) {
		
		Global.resetEdgeBandWidth();
		
		for(int i=0;i<Global.nodeNum;++i){
			isNewServer[i] = false;
			isNewServerInstalled[i] = false;
		}
	
		for (int i=0;i<serverNodesSize;++i) {
			int serverNode = serverNodes[i];
			isNewServer[serverNode] = true;
		}
		isNewServer[fromServerNode] = false;
		isNewServer[toServerNode] = true;
	
		for(int consumerId=0;consumerId<Global.consumerNum;++consumerId){	
		
			if(Global.isConsumerServer[consumerId]){
				isNewServerInstalled[Global.consumerNodes[consumerId]] = true;
				continue;
			}
			
			int consumerDemand = Global.consumerDemands[consumerId];	
			// 将起始点需求分发到目的地点中，会改变边的流量	
			for(int node : Global.allPriorityCost[consumerId]){
				// 不是服务器
				if(!isNewServer[node]){
					continue;
				}
				int usedDemand = Global.useBandWidthByPreNode(consumerDemand, node, Global.allPreNodes[consumerId]);
				// 可以消耗
				if (usedDemand > 0) {	
					isNewServerInstalled[node] = true;
					consumerDemand -= usedDemand;
					if(consumerDemand==0){
						break;
					}
				}
			}
			
			if(consumerDemand>0){
				isNewServerInstalled[Global.consumerNodes[consumerId]] = true;		
			}
		}
		
		serverNodesSize = 0;
		for(int node=0;node<Global.nodeNum;++node){
			if(isNewServerInstalled[node]){
				serverNodes[serverNodesSize++] = node;
			}
		}
	}
}
