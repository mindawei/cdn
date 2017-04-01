package com.cacheserverdeploy.deploy;

import java.util.Arrays;

/**
 * 基于 middle 优化
 * @author mindw
 * @date 2017年4月1日
 */
public final class GreedyOptimizerLeve3 extends GreedyOptimizerMiddle {

	/** 频率大于0的点 */
	private int[] nodes;

	/** 下一轮的服务器 */
	private Server[] serversInRandom;
	private int serverSize;
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
	public GreedyOptimizerLeve3(int[] nodes, int maxMovePerRound,int maxUpdateNum, int minUpdateNum) {
		this.MAX_UPDATE_NUM = maxUpdateNum;
		this.MIN_UPDATE_NUM = minUpdateNum;
		this.nodes = nodes;
		serversInRandom = new Server[Global.nodeNum];
		this.maxMovePerRound = maxMovePerRound;
	}

	private void selcetServers() {
		serverSize = 0;

		for (Server server : Global.getBestServers()) {
			if (server == null) {
				break;
			}
			serversInRandom[serverSize++] = new Server(server.node);

		}
		// 设置结束标志
		if (serverSize < serversInRandom.length) {
			serversInRandom[serverSize] = null;
		}

	}

	@Override
	void optimize() {

		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}

		if (Global.isTimeOut()) {
			return;
		}

		long t = System.currentTimeMillis();

		selcetServers();

		int lastCsot = Global.INFINITY;
		int maxUpdateNum = MIN_UPDATE_NUM;

		while (true) {

			// 可选方案
			int minCost = Global.INFINITY;
			int bestFromNode = -1;
			int bestToNode = -1;

			if (serverSize == 0) {
				break;
			}

			final int leftMoveRound = maxMovePerRound / serverSize;

			int updateNum = 0;
			boolean found = false;
			if (Global.IS_DEBUG) {
				System.out.println("maxUpdateNum:" + maxUpdateNum);
			}

			for (int i = 0; i < serverSize; ++i) {
				Server oldServer = serversInRandom[i];
				int fromNode = oldServer.node;

				// 服务器不移动
				if (Global.isMustServerNode[fromNode]) {
					continue;
				}

				int leftNum = leftMoveRound;
				for (int toNode : nodes) {
					// 防止自己到自己
					if (fromNode == toNode) {
						continue;
					}

					if (Global.isTimeOut()) {
						return;
					}

					int cost = moveCost(serversInRandom, fromNode, toNode);
					
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

					leftNum--;
					if (leftNum == 0) {
						break;
					}

				}

				if (found) {
					break;
				}

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

			if (minCost == Global.INFINITY) {
				break;
			}

			if (Global.isTimeOut()) {
				return;
			}

			// 移动
			move(serversInRandom, bestFromNode, bestToNode);

			int cost = Global.getTotalCost(nextGlobalServers);
			System.out.println("cost:"+cost+" minCost:"+minCost);
			
			if (cost < lastCsot) {
				serverSize = 0;
				for (Server server : nextGlobalServers) {
					if (server == null) {
						break;
					}
					serversInRandom[serverSize++] = server;
				}
				// 设置终止
				if (serverSize < serversInRandom.length) {
					serversInRandom[serverSize] = null;
				}

				lastCsot = cost;
				Global.updateSolution(serversInRandom);
			} else { // not better
				break;
				// lastCsot = Global.INFINITY;
				// selectRandomServers();
				// maxUpdateNum = MAX_UPDATE_NUM;
			}

		}

		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: "
					+ (System.currentTimeMillis() - t));
		}

	}
	
	///
	private int totalCost;
	
	private final int[] consumerDemands = new int[Global.consumerNum];
	
	private final int[] serverProvides = new int[Global.nodeNum];
	
	/** 进行一步移动 ,不要改变传进来的Server,结果缓存在 nextGlobalServers */
	protected int moveCost(Server[] oldServers,int fromServerNode, int toServerNode) {
		totalCost = 0;
		Arrays.fill(newServers, null);
		Arrays.fill(serverProvides, 0);
		
		int lsSize = 0;
		for (Server server : oldServers) {
			if(server==null){
				break;
			}
			if (server.node != fromServerNode) {
				Server newServer = new Server(server.node);
				newServers[server.node] = newServer;
				lsNewServers[lsSize++] = newServer;
			}
		}
		Server newServer = new Server(toServerNode);
		newServers[toServerNode] = newServer;
		lsNewServers[lsSize++] = newServer;
		
		Global.resetEdgeBandWidth();
		transferServersCost(nextGlobalServers,newServers,lsNewServers,lsSize);
		return totalCost;
	}
	
	protected void transferServersCost(Server[] nextGlobalServers,Server[] newServers,Server[] lsServers,int lsSize) {
		
		totalCost = 0;
		
		// 复制需求
		System.arraycopy(Global.consumerDemands, 0, consumerDemands, 0, Global.consumerNum);
		
		int size = 0;
		
		for(int consumerId=0;consumerId<Global.consumerNum;++consumerId){	
			// 肯定是服务器不用转移
			if (Global.isMustServerNode[Global.consumerNodes[consumerId]]) {
				totalCost+= Global.depolyCostPerServer;
				nextGlobalServers[size++] = new Server(consumerId, Global.consumerNodes[consumerId], consumerDemands[consumerId]);
				continue;
			} 
			// 减枝概率不大
			// 简单减枝计算，转移额最小费用
			int minCost = Global.INFINITY;
			for(int i=0;i<lsSize;++i){
				Server newServer = lsServers[i];
				if(Global.allCost[consumerId][newServer.node]<minCost){
					minCost = Global.allCost[consumerId][newServer.node];
				}
			}
			if(minCost*consumerDemands[consumerId]>=Global.depolyCostPerServer){
				totalCost+= Global.depolyCostPerServer;
				nextGlobalServers[size++] = new Server(consumerId, Global.consumerNodes[consumerId], consumerDemands[consumerId]);
				continue;
			}
			
			while(transferCost(consumerId, newServers));
			
			if (consumerDemands[consumerId] > 0) {
				totalCost+= Global.depolyCostPerServer;
				nextGlobalServers[size++] = new Server(consumerId, Global.consumerNodes[consumerId], consumerDemands[consumerId]);
			}
			
		}
		
		for(int i=0;i<lsSize;++i){
			Server newServer = lsServers[i];
			if(serverProvides[newServer.node]>0){
				totalCost+= Global.depolyCostPerServer;
				nextGlobalServers[size++] = newServer;
			}
		}
		
		// 尾部设置null表示结束
		if(size<nextGlobalServers.length){
			nextGlobalServers[size] = null;
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
	private boolean transferCost(int consumerId,Server[] newServers) {

		for(int node=0;node<Global.nodeNum;++node){
			visited[node] = false;
			costs[node] =  Global.INFINITY;
			preNodes[node] = -1;
		}

		int fromNode = Global.consumerNodes[consumerId];

		// 使用了多少个服务节点
		int leftServerNodeNum = 0;
		for(Server server : newServers){
			if(server!=null){
				leftServerNodeNum++;
			}
		}
	
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
			if (newServers[minCostNode]!=null) {
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
					serverProvides[minCostNode]+=usedDemand;
					
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
