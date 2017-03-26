package com.cacheserverdeploy.deploy;

import java.util.Arrays;


/**
 * 贪心搜索，寻找一个最优解，达到具备最优解后就马上退出
 * 
 * @author mindw
 * @date 2017年3月12日
 */
public abstract class GreedyOptimizer {
	
	/** 只优化一次 */
	public static final boolean  OPTIMIZE_ONCE = true;
	
	private boolean isOptimizeOnce = false;

	public GreedyOptimizer(){}

	public GreedyOptimizer(boolean isOptimizeOnce){
		this.isOptimizeOnce = isOptimizeOnce;
	}
	
	/** 为了复用，为null的地方不放置服务器 */
	protected Server[] newServers = new Server[Global.nodeNum];
	/** 为了复用，下一轮的服务器，模拟队列，当遇到null时表示结束*/
	protected Server[] lsNewServers = new Server[Global.nodeNum];
	
	/** 为了复用，下一轮的服务器，模拟队列，当遇到null时表示结束*/
	protected Server[] nextGlobalServers = new Server[Global.nodeNum];
	
	void optimize() {
		
		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}

		long t = System.currentTimeMillis();

		// 本地移动一步,各个结果之间过渡的时候回漏掉一步，故添加该方法  
		moveLocal(Global.getBestServers());
		Global.updateSolution(nextGlobalServers);
		
		// 只优化一次
		if(isOptimizeOnce){
			if (Global.IS_DEBUG) {
				System.out.println(this.getClass().getSimpleName() + " 结束，耗时: " + (System.currentTimeMillis() - t));
			}
			return;
		}
		
		while (true) {
			
			// 可选方案
			int minCost = Global.INFINITY;
			int bestFromNode = -1;
			int bestToNode = -1;

			for (Server server : Global.getBestServers()) {
				if(server==null){
					break;
				}
				
				int fromNode = server.node;
				// 服务器不移动
				if(Global.isMustServerNode[fromNode]){
					continue;
				}
				
				for (int toNode = 0; toNode < Global.nodeNum; ++toNode) {
					
					if (Global.isTimeOut()) {
						return;
					}
					
					// 防止自己到自己
					if (fromNode == toNode) {
						continue;
					}
					
					move(Global.getBestServers(),fromNode, toNode);
					int cost = Global.getTotalCost(nextGlobalServers);
					if (cost < minCost) {
						minCost = cost;
						bestFromNode = fromNode;
						bestToNode = toNode;
					}
				}
			}

			if (minCost == Global.INFINITY) {
				break;
			}

			if (Global.isTimeOut()) {
				return;
			}
			
			// 移动
			move(Global.getBestServers(),bestFromNode, bestToNode);
			boolean better = Global.updateSolution(nextGlobalServers);

			if (!better) { // better
				break;
			}
			
		}

		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: " + (System.currentTimeMillis() - t));
		}
	}

	/** 进行一步移动 ,不要改变传进来的Server,结果缓存在 nextGlobalServers */
	protected void move(Server[] oldServers,int fromServerNode, int toServerNode) {
		Arrays.fill(newServers, null);
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
		transferServers(newServers,lsNewServers,lsSize);
	}
	
	/** 本地移动一步,各个结果之间过渡的时候回漏掉一步，故添加该方法,结果缓存在 nextGlobalServers   */
	protected void moveLocal(Server[] oldGlobalServers) {
		Arrays.fill(newServers, null);
		int lsSize = 0;
		for (Server server : oldGlobalServers) {
			if(server==null){
				break;
			}
			Server newServer = new Server(server.node);
			newServers[server.node] = newServer;
			lsNewServers[lsSize++] = newServer;
		}
		Global.resetEdgeBandWidth();
		transferServers(newServers,lsNewServers,lsSize);
	}
	
	/** 
	 * 不同的搜索策略需要提供此方法 :总共 nodeNum个位置，结果缓存在 nextGlobalServers
	 * @param newServers 为null的表示没有服务器
	 * @param lsServers 全部往前移动，为了减少遍历
	 * @param lsSize lsServer的长度
	 */
	protected abstract void transferServers(Server[] newServers,Server[] lsServers,int lsSize);

	/** 
	 * 供子类调用：
	 * 转移到另一个服务器，并返回价格<br>
	 * 注意：可能cost会改变(又返回之前的点)
	 */
	protected void transferTo(int consumerId,Server toServer,int transferBandWidth,int serverNode,int[] preNodes) {
		
		///////////////////////
		// 适配： 指针 -> 数组
		// 计算长度
		int len = 0;
		int pre = serverNode;
		while(pre!=-1){
			len++;
			pre = preNodes[pre];
		}

		// 逐个添加
		int[] viaNodes = new int[len];
		pre = serverNode;
		while(pre!=-1){
			viaNodes[--len] = pre;	
			pre = preNodes[pre];
		}
		
		/////////////////////////
		
		// 剩余要传的的和本地的最小值
		ServerInfo toServerInfo = new ServerInfo(consumerId,transferBandWidth,viaNodes);
		toServer.addServerInfo(toServerInfo);
	}
	
	/**
	 * 消耗带宽最大带宽
	 * @return 消耗掉的带宽
	 */
	protected int useBandWidthByPreNode(int demand,int serverNode,int[] preNodes ) {
		int node1 = serverNode;
		int node0 = preNodes[node1];
		
		int minBindWidth = Global.INFINITY;
		while(node0!=-1){
			Edge edge = Global.graph[node1][node0];
			if(edge.leftBandWidth<minBindWidth){				
				minBindWidth = edge.leftBandWidth;
				if(minBindWidth==0){
					break;
				}
			}
			node1 = node0;
			node0 = preNodes[node0];
		}
		
//		if(minBindWidth==0){
//			System.out.println("0");
//		}else{
//			System.out.println("not 0");
//		}
		
		if (minBindWidth == 0) {
			return 0;
		}
		
		int usedBindWidth = Math.min(minBindWidth, demand);
		
		node1 = serverNode;
		node0 = preNodes[node1];
		while(node0!=-1){
			Edge edge = Global.graph[node1][node0];
			edge.leftBandWidth -= usedBindWidth;
			
			node1 = node0;
			node0 = preNodes[node0];
		}
		return usedBindWidth;
	}
}
