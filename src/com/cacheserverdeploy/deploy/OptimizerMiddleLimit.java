package com.cacheserverdeploy.deploy;

/**
 * 基于 middle 优化
 * @author mindw
 * @date 2017年4月1日
 */
public final class GreedyOptimizerLeve3{

	/** 频率大于0的点 */
	private int[] nodes;

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
	
	private void selcetServers() {
		serverNodesSize = 0;
		for (Server server : Global.getBestServers()) {
			if (server == null) {
				break;
			}
			serverNodes[serverNodesSize++] = server.node;
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

		selcetServers();

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
				totalCost+= Global.depolyCostPerServer;
				isNewServerInstalled[Global.consumerNodes[consumerId]] = true;
				continue;
			}
			
			while(transferCost(consumerId));
			
			if (consumerDemands[consumerId] > 0) {
				totalCost+= Global.depolyCostPerServer;
				isNewServerInstalled[Global.consumerNodes[consumerId]] = true;
			}
			
		}
		
		for(int i=0;i<lsNewServersSize;++i){
			int newServerNode = lsNewServers[i];
			if(isNewServerInstalled[newServerNode]){
				totalCost += Global.depolyCostPerServer;
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
