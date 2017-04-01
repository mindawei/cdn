package com.cacheserverdeploy.deploy;

import java.util.Arrays;

/**
 * 改进版 simple
 * @author mindw
 * @date 2017年4月1日
 */
public final class GreedyOptimizerLeve2{

	/** 频率大于0的点 */
	private final int[] nodes;
	/** 每次最多随机选多少个 */
	private final int selectNum;
	/** 每轮最多移动多少次 */
	private final int maxMovePerRound;
	private final int MAX_UPDATE_NUM;
	private final int MIN_UPDATE_NUM;

	/**
	 * 构造函数
	 * 
	 * @param nearestK
	 *            初始化的时候选每个消费者几个最近领
	 * @param selectNum
	 *            随机生成的时候服务器个数
	 * @param maxMovePerRound
	 *            每轮最多移动多少次
	 */
	public GreedyOptimizerLeve2(int[] nodes, int selectNum,
			int maxMovePerRound, int maxUpdateNum, int minUpdateNum) {
		this.nodes = nodes;
		this.selectNum = selectNum;
		this.MAX_UPDATE_NUM = maxUpdateNum;
		this.MIN_UPDATE_NUM = minUpdateNum;
		this.maxMovePerRound = maxMovePerRound;
	}

	/** 下一轮的服务器 */
	private final int[] serverNodes = new int[Global.nodeNum];
	private int serverNodesSize = 0;

	public void updateBeforeReturn(){
		Server[] servers = new Server[serverNodesSize];
		for(int i=0;i<serverNodesSize;++i){
			servers[i] = new Server(serverNodes[i]);
		}
		Global.setBestServers(servers);
		if(Global.IS_DEBUG){
			System.out.println("服务器设置完成");
		}
	}
	
	private final void selcetBestServers() {
		serverNodesSize = 0;

		int leftNum = selectNum;
		// 肯定是服务器的
		for (int node : Global.mustServerNodes) {
			serverNodes[serverNodesSize++] = node;
		}
		leftNum -= Global.mustServerNodes.length;
		int index = 0;
		// 随机选择
		while (leftNum > 0 && index < nodes.length) {
			// 没有被选过
			int node = nodes[index++];
			// 服务器上面已经添加过了
			if (!Global.isMustServerNode[node]) {
				serverNodes[serverNodesSize++] = node;
				leftNum--;
			}
		}
	}
	

	void optimize() {

		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}

		if (Global.isTimeOut()) {
			return;
		}

		long t = System.currentTimeMillis();

		selcetBestServers();

		int lastCsot = Global.INFINITY;
		int maxUpdateNum = MAX_UPDATE_NUM;
	
		while (!Global.isTimeOut()) {
			
			if (serverNodesSize == 0) {
				break;
			}
			
			// 可选方案
			int minCost = Global.INFINITY;
			int bestFromNode = -1;
			int bestToNode = -1;
			int leftMoveRound = maxMovePerRound / serverNodesSize;
			int updateNum = 0;
			boolean found = false;
			
			for (int i = 0; i < serverNodesSize; ++i) {
				int fromNode = serverNodes[i];

				// 服务器不移动
				if (Global.isMustServerNode[fromNode]) {
					continue;
				}

				for (int j=0;j<leftMoveRound;++j) {
					int toNode = nodes[j];
					// 防止自己到自己
					if (fromNode == toNode) {
						continue;
					}
					
					if(Global.isTimeOut()){
						updateBeforeReturn();
						return;
					}
					
					int cost = getCostAfterMove(fromNode,toNode);
					if (cost < minCost) {
						minCost = cost;
						bestFromNode = fromNode;
						bestToNode = toNode;
						updateNum++;
						if (updateNum == maxUpdateNum) {
							found = true;
							break;
						}
					}
				}

				if (found) {
					break;
				}

			}
			
			if (minCost == Global.INFINITY) {
				break;
			}

			if (maxUpdateNum <= updateNum) {
				maxUpdateNum++;
				if (maxUpdateNum > MAX_UPDATE_NUM) {
					maxUpdateNum = MAX_UPDATE_NUM;
				}
			} else { // > updateNum
				maxUpdateNum--;
				if (maxUpdateNum < MIN_UPDATE_NUM) {
					maxUpdateNum = MIN_UPDATE_NUM;
				}
			}
			
			// 移动
			if (minCost < lastCsot) {
				lastCsot = minCost;
				moveBest(bestFromNode, bestToNode);
				if(Global.IS_DEBUG){
					System.out.println("better : "+minCost);
					System.out.println("maxUpdateNum:" + maxUpdateNum);
				}
			} else { // not better
				if(Global.IS_DEBUG){
					System.out.println("worse : "+minCost);
				}
				break;
			}
		}
		
		updateBeforeReturn();
		
		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: "+ (System.currentTimeMillis() - t));
		}

	}
	
	
	/** 新服务器是否已经安装 */
	private final boolean[] isNewServerInstalled = new boolean[Global.nodeNum];
	/** 是否是新服务器 */
	private final boolean[] isNewServer = new boolean[Global.nodeNum];
	
	/** 进行一步移动 */
	private final int getCostAfterMove(int fromServerNode, int toServerNode) {
		
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
	private final void moveBest(int fromServerNode, int toServerNode) {
		
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
