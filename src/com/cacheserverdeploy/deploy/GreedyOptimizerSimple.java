package com.cacheserverdeploy.deploy;

import java.util.Arrays;

// simple部分
public class GreedyOptimizerSimple extends GreedyOptimizer{
	/** 新服务器是否已经安装 */
	private final boolean[] isNewServerInstalled = new boolean[Global.nodeNum];
	/** 是否是新服务器 */
	private final boolean[] isNewServer = new boolean[Global.nodeNum];
	
	/** 进行一步移动 ,不要改变传进来的Server,结果缓存在 nextGlobalServers */
	protected int getCostAfterMove(Server[] oldServers,int fromServerNode, int toServerNode) {
		
		Global.resetEdgeBandWidth();
		
		Arrays.fill(isNewServer, false);
		
		for (Server server : oldServers) {
			if(server==null){
				break;
			}
			if (server.node != fromServerNode) {
				isNewServer[server.node] = true;
				isNewServerInstalled[server.node] = false;
			}
		}
		isNewServer[toServerNode] = true;
		isNewServerInstalled[toServerNode] = false;
		
		int cost = 0;

		for(int consumerId=0;consumerId<Global.consumerNum;++consumerId){	
			
			int consumerDemand = Global.consumerDemands[consumerId];
			
			int consumerNode = Global.consumerNodes[consumerId];
			
			if(Global.isMustServerNode[consumerNode]){
				cost+=Global.depolyCostPerServer;
				continue;
			}
			
			// 将起始点需求分发到目的地点中，会改变边的流量	
			for(int node : Global.allPriorityCost[consumerId]){
				
				if(node==-1){
					break;
				}
				
				// 不是服务器
				if(!isNewServer[node]){
					continue;
				}
				
				int usedDemand = Global.useBandWidthByPreNode(consumerDemand, node,Global.allPreNodes[consumerId]);
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

	@Override
	protected void transferServers(Server[] nextGlobalServers,Server[] newServers,Server[] lsServers,int lsSize) {
		
		int size = 0;
		
		for(int consumerId=0;consumerId<Global.consumerNum;++consumerId){	
			
			int consumerNode = Global.consumerNodes[consumerId];
			int consumerDemand = Global.consumerDemands[consumerId];
			
			if(Global.isMustServerNode[consumerNode]){
				// 肯定是服务器不用转移
				nextGlobalServers[size++] = new Server(consumerId,consumerNode,consumerDemand);
				continue;
			}
			
			// 将起始点需求分发到目的地点中，会改变边的流量	
			for(int node : Global.allPriorityCost[consumerId]){
				// 不是服务器
				if(newServers[node]==null){
					continue;
				}
				
				int usedDemand = Global.useBandWidthByPreNode(consumerDemand, node,Global.allPreNodes[consumerId]);
				// 可以消耗
				if (usedDemand > 0) {	
					Global.transferTo(consumerId, newServers[node], usedDemand,node, Global.allPreNodes[consumerId]);
					consumerDemand -= usedDemand;
					if(consumerDemand==0){
						break;
					}
				}
			}
			
			if (consumerDemand>0) {
				nextGlobalServers[size++] = new Server(consumerId,consumerNode,consumerDemand);
			}
			
		}
		
		for(int i=0;i<lsSize;++i){
			Server newServer = lsServers[i];
			if(newServer.getDemand()>0){
				nextGlobalServers[size++] = newServer;
			}
		}
		
		// 尾部设置null表示结束
		if(size<nextGlobalServers.length){
			nextGlobalServers[size] = null;
		}
	}
	
	
}
