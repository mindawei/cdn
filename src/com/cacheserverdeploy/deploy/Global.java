package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 全局参数，方便访问
 * 
 * @author mindw
 * @date 2017年3月11日
 */
public final class Global {

	/** 是否是调试 */
	static final boolean IS_DEBUG = true;

	/** 何时超时 */
	static final long TIME_OUT = System.currentTimeMillis() + 80 * 1000L;

	/** 是否超时 */
	static boolean isTimeOut() {
		return System.currentTimeMillis() > TIME_OUT;
	}
	
	/** 是否困难 */
	static boolean isNpHard;
	
	private static final int NP_HARD_THRESHOLD = 1000000;

	/** 无穷大 */
	static final int INFINITY = Integer.MAX_VALUE;

	/** 最小费用 */
	static int minCost = INFINITY;
	/** 解决方案 */
	static String[] bsetSoluttion;
	
	/** 每台部署的成本：[0,5000]的整数 */
	public static int depolyCostPerServer;

	/** 节点数:不超过1000个 ,从0开始 */
	static int nodeNum;
	/** 消费者数 */
	static int consumerNum;
	/** 消费者所在的节点ID下标 */
	static int[] consumerNodes;
	static int[] consumerDemands;

	/** 地图 */
	public static Edge[][] graph;
	/** 地图上的边 */
	public static Edge[] edges;
	/** 连接关系：下游节点 */
	static int[][] connections; 
	
	/** 消费者到所有节点的费用 */
	static int[][] allCost;
	static int[][][] allViaNode;
		
	/** 放置的服务器 */
	private static ArrayList<Server> bestServers;
	
	public static ArrayList<Server> getBestServers() {
		return bestServers;
	}
	
	static Server[] getConsumerServer(){
		Server[] servers = new Server[consumerNum];
		for(int i=0;i<consumerNum;++i){
			servers[i] = new Server(i,consumerNodes[i],consumerDemands[i]);
		}
		return servers;
	}
	
	/** 重置 */
	public static void resetEdgeBandWidth() {
		// 恢复edge的带宽值
		for (Edge edge : edges) {
			edge.reset();
		}
	}

	public static void saveBandWidth() {
		// 保存edge的带宽值
		for (Edge edge : edges) {
			edge.saveBandWidth();
		}
	}
	
	/** 恢复之前的保存状态 */
	public static void goBackBandWidth() {
		// 恢复edge的带宽值
		for (Edge edge : edges) {
			edge.goBackBandWidth();
		}
	}
		
	/** 初始化解：将服务器直接放在消费节点上 */
	public static void init() {
		
		// 初始连接关系
		connections = new int[nodeNum][];
		for(int fromNode=0;fromNode<nodeNum;++fromNode){
			ArrayList<Integer> toNodeIds = new ArrayList<Integer>();
			for (int toNodeId = 0; toNodeId < nodeNum; ++toNodeId) {
				if (graph[fromNode][toNodeId] != null) {
					toNodeIds.add(toNodeId);
				}
			}
			connections[fromNode] = new int[toNodeIds.size()];
			for(int i=0;i<toNodeIds.size();++i){
				connections[fromNode][i] = toNodeIds.get(i);
			}
		}
		
		// 初始解
		ArrayList<Server> nextGlobalServers = new ArrayList<Server>(consumerNum);
		for (int i = 0; i < consumerNum; ++i) {
			nextGlobalServers.add(new Server(i, consumerNodes[i], consumerDemands[i]));
		}
		updateSolution(nextGlobalServers);				

		// 判断任务难易 
		int On = nodeNum * nodeNum * consumerNum;
		isNpHard = On > NP_HARD_THRESHOLD;
		if (isNpHard){
			// 初始费用缓存
			initAllCost();
		}
		
		if(IS_DEBUG){
			System.out.println("On:"+On+" isNpHard:"+isNpHard);
		}
	}
	
	

	/** 更新值 ，是否更好 */
	public static boolean updateSolution(ArrayList<Server> nextGlobalServers) {
		
		int newMinCost = getTotalCost(nextGlobalServers);
		
		if (IS_DEBUG) {
			System.out.println("newMinCost:" + newMinCost);
			System.out.println(newMinCost < Global.minCost ? "better" : "worse");
		}
		
		if (newMinCost < Global.minCost) {
			bestServers = nextGlobalServers;
			minCost = newMinCost;
			bsetSoluttion = getSolution(bestServers);
			return true;
		} else {
			return false;
		}
	}


	/**
	 * 获得解决方案，格式如下:<br>
	 * <blockquote> 网络路径数量<br>
	 * （空行）<br>
	 * 网络节点ID-01 网络节点ID-02 …… 网络节点ID-n 消费节点ID 占用带宽大小<br>
	 * ……………. （文件结束）<br>
	 * <br>
	 * 每条网络路径由若干网络节点构成，路径的起始节点ID-01表示该节点部署了视频内容服务器，终止节点为某个消费节点<br>
	 * </blockquote>
	 */
	private static String[] getSolution(ArrayList<Server> servers) {
		List<String> ls = new LinkedList<String>();
		for (Server server : servers) {
			ls.addAll(server.getSolution());
		}

		String[] solution = new String[ls.size() + 2];
		solution[0] = String.valueOf(ls.size());
		solution[1] = "";
		int index = 2;
		for (String line : ls) {
			solution[index++] = line;
		}
		return solution;
	}

	/** 获得总的费用 */
	public static int getTotalCost(ArrayList<Server> servers) {
		int toatlCost = 0;
		for (Server server : servers) {
			toatlCost += server.getCost();
		}
		return toatlCost;
	}

	/**
	 * 打印解决方案细节
	 */
	public static void printBestSolution() {
		System.out.println("---------------");
		System.out.println("最优解：");
		System.out.println("总的费用：" + Global.minCost);
		System.out.println();
		for (String line : bsetSoluttion) {
			System.out.println(line);
		}
		System.out.println("---------------");
	}
	
	/**
	 * 消耗带宽最大带宽
	 * 
	 * @return 消耗掉的带宽,路由器到服务器，反方向消耗要
	 */
	static int useBandWidth(int demand, int[] nodeIds) {
		if (demand == 0) {
			return 0;
		}
		int minBindWidth = Global.INFINITY;
		for (int i = nodeIds.length - 1; i >=1; --i) {
			Edge edge = Global.graph[nodeIds[i]][nodeIds[i -1]];
			minBindWidth = Math.min(edge.leftBandWidth, minBindWidth);
		}
		if (minBindWidth == 0) {
			return 0;
		}
		int usedBindWidth = Math.min(minBindWidth, demand);
		for (int i = nodeIds.length - 1; i >=1; --i) {
			Edge edge = Global.graph[nodeIds[i]][nodeIds[i -1]];
			edge.leftBandWidth -= usedBindWidth;
		}
		return usedBindWidth;
	}
	
	
	public static void returnBandWidth(int bindWidth, int[] nodeIds) {
		if (bindWidth == 0) {
			return;
		}
		for (int i = nodeIds.length - 1; i >=1; --i) {
			Edge edge = Global.graph[nodeIds[i]][nodeIds[i -1]];
			edge.leftBandWidth += bindWidth;
		}
	}
	
	/**
	 * 消耗带宽最大带宽
	 * 
	 * @return 消耗掉的带宽,路由器到服务器，反方向消耗要
	 */
	static int getBandWidthCanUsed(int demand, int[] nodeIds) {
		if (demand == 0) {
			return 0;
		}
		int minBindWidth = Global.INFINITY;
		for (int i = nodeIds.length - 1; i >=1; --i) {
			Edge edge = Global.graph[nodeIds[i]][nodeIds[i -1]];
			minBindWidth = Math.min(edge.leftBandWidth, minBindWidth);
		}
		return minBindWidth;
	}

	
	private static void initAllCost(){
		allCost = new int[consumerNum][nodeNum];
		allViaNode = new int[consumerNum][nodeNum][];
		
		for(int i=0;i<consumerNum;++i){
			initCost(i);
		}
	}
	
	private static void initCost(int consumerId) {

		int[] costs = allCost[consumerId];
		Arrays.fill(costs, INFINITY);
		
		int[] visited = new int[nodeNum];

		int[][] viaNodes = allViaNode[consumerId];
		
		int startNode = consumerNodes[consumerId];
		costs[startNode] = 0;
		viaNodes[startNode] = new int[]{startNode};
		
		while (true) {

			// 寻找下一个最近点
			int minCost = INFINITY;
			int fromNode = -1;
			for (int node =0;node<nodeNum;++node) {
				// 1 访问过了 或者 2 还没信息（cost 无穷大）
				if(visited[node]==1){
					continue;
				}
				if (costs[node] < minCost) {
					minCost = costs[node];
					fromNode = node;
				}
			}

			// 其余都不可达
			if (fromNode == -1) {
				break;
			}

			// 访问过了
			visited[fromNode] = 1;

			// 更新
			for (int toNode : connections[fromNode]) {
				Edge edge = Global.graph[fromNode][toNode];
				int newCost = minCost + edge.cost;
				if (newCost < costs[toNode]) {
					costs[toNode] = newCost;
					// 添加路径
					
					// 添加路径
					int nodeSize = viaNodes[fromNode].length;
					viaNodes[toNode] = Arrays.copyOf(viaNodes[fromNode],nodeSize+1);
					viaNodes[toNode][nodeSize] = toNode;
				}
			}
			
		}
		
	}

	/** 对接进行优化 */
	public static void optimize() {
		
//		if(!isNpHard){
//			return;
//		}

		Map<Integer, Server> newServers = new HashMap<Integer, Server>();
		for (Server server : bestServers) {
			newServers.put(server.node, new Server(server.node));
		}
					
		Global.resetEdgeBandWidth();
	
		Server[] consumerServers = Global.getConsumerServer();
		
		RouterComplex.transfer(consumerServers, newServers);
		
		ArrayList<Server> nextGlobalServers = new ArrayList<Server>();
		for(Server consumerServer : consumerServers){
			if (consumerServer.getDemand() > 0) { // 真正安装
				nextGlobalServers.add(consumerServer);
			}
		}
		
		for(Server newServer : newServers.values()){
			if (newServer.getDemand() > 0) { // 真正安装
				nextGlobalServers.add(newServer);
			}
		}
		updateSolution(nextGlobalServers);
	}
	
	/** 
	 * 转移到另一个服务器，并返回价格<br>
	 * 注意：可能cost会改变(又返回之前的点)
	 */
	static void transferTo(Server fromServer,Server toServer,int avaliableBandWidth,int[] viaNodes ) {
		
		Iterator<ServerInfo> iterator = fromServer.serverInfos.iterator();
		while(iterator.hasNext()){
			ServerInfo fromServerInfo = iterator.next();
			// 剩余要传的的和本地的最小值
			int transferBandWidth = Math.min(avaliableBandWidth, fromServerInfo.provideBandWidth);
			
			int[] fromNodes = fromServerInfo.viaNodes;
			
			// 虽然分配了，但是新的部分目前为0
			int[] nodes = new int[fromNodes.length+viaNodes.length-1];
				
			System.arraycopy(fromNodes, 0, nodes, 0, fromNodes.length);
			// 去头
			System.arraycopy(viaNodes, 1, nodes, fromNodes.length, viaNodes.length-1);
			
			ServerInfo toServerInfo = new ServerInfo(fromServerInfo.consumerId,transferBandWidth,nodes);
			toServer.serverInfos.add(toServerInfo);
			// 更新当前的
			fromServerInfo.provideBandWidth -= transferBandWidth;
			if(fromServerInfo.provideBandWidth==0){ // 已经全部转移
				iterator.remove();
			}
		}
		
	}
	
	
}
