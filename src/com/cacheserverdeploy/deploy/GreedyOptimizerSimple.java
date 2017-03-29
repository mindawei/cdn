package com.cacheserverdeploy.deploy;

// simple部分
public class GreedyOptimizerSimple extends GreedyOptimizer{

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
