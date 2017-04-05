package com.cacheserverdeploy.deploy;

public class OptimizerMiddle2 extends Optimizer{

	private int[] lsNewServers = new int[Global.nodeNum];
	private int lsNewServersSize = 0;

	/** 新服务器是否已经安装 */
	private final boolean[] isNewServerInstalled = new boolean[Global.nodeNum];
	/** 是否是新服务器 */
	private final boolean[] isNewServer = new boolean[Global.nodeNum];
	private final int[] consumerDemands = new int[Global.consumerNum];
	
	private int totalCost;
	
	
	private void prepare(int fromServerNode, int toServerNode){
		for (int i = 0; i < Global.nodeNum; ++i) {
			isNewServer[i] = false;
			isNewServerInstalled[i] = false;
		}
		
		lsNewServersSize = 0;
		for (int i=0;i<serverNodesSize;++i) {
			int serverNode = serverNodes[i];
			if (serverNode != fromServerNode) {
				isNewServer[serverNode] = true;
				lsNewServers[lsNewServersSize++] = serverNode;
			}
		}
		isNewServer[toServerNode] = true;
		lsNewServers[lsNewServersSize++] = toServerNode;
		
		// 复制需求
		System.arraycopy(Global.consumerDemands, 0, consumerDemands, 0, Global.consumerNum);
	
		Global.resetEdgeBandWidth();
	}
	
	
	/** 进行一步移动 ,不要改变传进来的Server,结果缓存在 nextGlobalServers */
	protected int getCostAfterMove(int fromServerNode, int toServerNode) {
		
		prepare(fromServerNode, toServerNode);

		totalCost = 0;	
		for(int consumerId=0;consumerId<Global.consumerNum;++consumerId){	
			// 减枝概率不大
			// 简单减枝计算，转移额最小费用
			int minCost = Global.INFINITY;
			for(int i=0;i<lsNewServersSize;++i){
				int newServerNode = lsNewServers[i];
				if(Global.allCost[consumerId][newServerNode]<minCost){
					minCost = Global.allCost[consumerId][newServerNode];
				}
			}
			if(minCost*consumerDemands[consumerId]>=Global.depolyCostPerServer){
				totalCost += Global.depolyCostPerServer*2;
				isNewServerInstalled[Global.consumerNodes[consumerId]] = true;
				continue;
			}
			
			while(transferCost(consumerId));
			
			if (consumerDemands[consumerId] > 0) {
				totalCost += Global.depolyCostPerServer*2;
				isNewServerInstalled[Global.consumerNodes[consumerId]] = true;
			}
			
		}
		
		for(int i=0;i<lsNewServersSize;++i){
			int newServerNode = lsNewServers[i];
			if(isNewServerInstalled[newServerNode]){
				totalCost += Global.depolyCostPerServer*2;
				isNewServerInstalled[newServerNode]=false;
			}
		}
		return totalCost;
		
	}
	
	protected void moveBest(int fromServerNode, int toServerNode) {
		
		prepare(fromServerNode, toServerNode);
		
		for(int consumerId=0;consumerId<Global.consumerNum;++consumerId){	
			// 减枝概率不大
			// 简单减枝计算，转移额最小费用
			int minCost = Global.INFINITY;
			for(int i=0;i<lsNewServersSize;++i){
				int newServerNode = lsNewServers[i];
				if(Global.allCost[consumerId][newServerNode]<minCost){
					minCost = Global.allCost[consumerId][newServerNode];
				}
			}
			if(minCost*consumerDemands[consumerId]>=Global.depolyCostPerServer){
				isNewServerInstalled[Global.consumerNodes[consumerId]] = true;
				continue;
			}
			
			while(transferCost(consumerId));
			
			if (consumerDemands[consumerId] > 0) {
				isNewServerInstalled[Global.consumerNodes[consumerId]] = true;
			}
			
		}
		
		serverNodesSize = 0;
		for(int i=0;i<lsNewServersSize;++i){
			int newServerNode = lsNewServers[i];
			if(isNewServerInstalled[newServerNode]){
				serverNodes[serverNodesSize++] = newServerNode;
				isNewServerInstalled[newServerNode]=false;
			}
		}
	}
	
	private final boolean[] visited = new boolean[Global.nodeNum];
	private final int[] costs = new int[Global.nodeNum]; 
	private final int[] preNodes = new int[Global.nodeNum];
	
	// 13436
	/**  
	 * 将起始点需求分发到目的地点中，会改变边的流量<br>
	 * @return 是否需要继续转移 
	 */
	private boolean transferCost(int consumerId) {

		for(int node=0;node<Global.nodeNum;++node){
			visited[node] = false;
			costs[node] =  Global.INFINITY;
			preNodes[node] = -1;
		}

		int fromNode = Global.consumerNodes[consumerId];

		// 还剩多少个服务节点未使用
		int leftServerNodeNum = lsNewServersSize;
	
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
			if (isNewServer[minCostNode]) {
				int usedDemand = Global.useBandWidthByPreNode(consumerDemands[consumerId], minCostNode, preNodes);
				
				// 可以消耗
				if (usedDemand > 0) {		
					
					// 适配： 指针 -> 数组
					int node1 = minCostNode;
					int node0 = preNodes[node1];
					while(node0!=-1){
						Edge edge = Global.graph[node1][node0];
						totalCost+=edge.cost * usedDemand;
						node1 = node0;
						node0 = preNodes[node0];
					}
					
					consumerDemands[consumerId] -= usedDemand;
					isNewServerInstalled[minCostNode] = true;
					
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
