package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;


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
	static final long TIME_OUT = System.currentTimeMillis() + 85 * 1000L;

	/** 是否超时 */
	static boolean isTimeOut() {
		return System.currentTimeMillis() > TIME_OUT;
	}
	
	/** 是否非常难 */
	static boolean isNpHardest;
	
	/** 是否困难 */
	static boolean isNpHard;
	

	private static final int NP_HARDEST_THRESHOLD = 100000000;
	private static final int NP_HARD_THRESHOLD    = 10000000;
	
	// 初级                1638400
	// 中级                10800000
	// 高级                204800000
	// case 50 4500000
	// case 99 500000000
	
	/** 无穷大 */
	static final int INFINITY = Integer.MAX_VALUE;
	//static final int INFINITY = 1000000000;

	/** 最小费用 */
	static int minCost = INFINITY;
	private static int initCost;
	
	/** 初始解是否陷入局部最优了 */
	static boolean isDropInInit(){
		return initCost == minCost;
	}
	
	/** 解决方案 */
	private static String[] bestSolution;
	private static int bestSolutionLength;
	
	public static String[] getBsetSolution() {
		String[] solution = new String[bestSolutionLength];
		System.arraycopy(bestSolution, 0, solution, 0, bestSolutionLength);
		return solution;
	}

	/** 每台部署的成本：[0,5000]的整数 */
	public static int depolyCostPerServer;

	/** 增加两个超级会点*/
	static int mcmfNodeNum;
	static int sourceNode;
	static int endNode;
	
	/** 节点数:不超过1000个 ,从0开始 */
	static int nodeNum;
	/** 消费者数 */
	static int consumerNum;
	/** 消费者所在的节点ID下标 */
	static int[] consumerNodes;
	static int[] consumerDemands;
	/** 消费者需求总和*/
	static int consumerTotalDemnad = 0;
	/** 网络节点ID - >  消费节点ID */
	static Map<Integer,Integer> nodeToConsumerId = new HashMap<Integer,Integer>();
		
	/** 地图 */
	public static Edge[][] graph;
	/** 地图上的边 */
	public static Edge[] edges;
	/** 连接关系：下游节点 */
	static int[][] connections; 
	/** 连接关系：下游节点 */
	static int[][] mfmcConnections; 
	
	/** 是否确肯定定是服务节点 */
	static boolean[] isMustServerNode;
	/** 一定是服务节点的点 */
	static int[] mustServerNodes;
	
	/** 放置的服务器 ,List 用 数组替代，为空时表示数组结束  */
	private static Server[] bestServers;
	
	public static Server[] getBestServers() {
		return bestServers;
	}

	private static void setBestServers(Server[] nextGlobalServers) {
		int size = 0;
		for(Server server : nextGlobalServers){
			if(server==null){
				break;
			}
			bestServers[size++] = server;
		}
		if(size<bestServers.length){
			bestServers[size] = null;
		}
	}

	/** 初始化解：将服务器直接放在消费节点上 */
	public static void init() {
		
		bestSolution = new String[50002];// 最多5万条
		bestServers = new Server[nodeNum]; 
		
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
		
		mfmcConnections = new int[mcmfNodeNum][];
		for(int fromNode=0;fromNode<mcmfNodeNum;++fromNode){
			ArrayList<Integer> toNodeIds = new ArrayList<Integer>();
			for (int toNodeId = 0; toNodeId < mcmfNodeNum; ++toNodeId) {
				if (graph[fromNode][toNodeId] != null) {
					toNodeIds.add(toNodeId);
				}
			}
			mfmcConnections[fromNode] = new int[toNodeIds.size()];
			for(int i=0;i<toNodeIds.size();++i){
				mfmcConnections[fromNode][i] = toNodeIds.get(i);
			}
		}
		
		// 如果需求大于供应，则这个消费节点必须建立服务器
		isMustServerNode = new boolean[nodeNum];
		Arrays.fill(isMustServerNode, false);
		ArrayList<Integer> lsMustServerNodes = new ArrayList<Integer>();
		for(int consumerId = 0;consumerId<consumerNum;++consumerId){
			// 自己的需求
			int cunsumerDemand = consumerDemands[consumerId];
			// 消费者节点
			int consumerNode = consumerNodes[consumerId];
			// 能够提供的需求
			int supplyDemand = 0;
			// 来自的边
			ArrayList<Edge> fromEdges = new ArrayList<Edge>();
			for(int fromNode =0;fromNode<nodeNum;++fromNode){
				Edge edge = graph[fromNode][consumerNode];
				if(edge!=null){
					supplyDemand+=edge.initBandWidth;
					fromEdges.add(edge);
				}
			}
			boolean isServerNode =false;
			if(supplyDemand<cunsumerDemand){ // 不能满足
				isServerNode = true;
			}else{ // supplyDemand>= cunsumerDemand 可以满足
				// 按费用从小到大
				Collections.sort(fromEdges,new Comparator<Edge>() {
					@Override
					public int compare(Edge o1, Edge o2) {
						return o1.cost-o2.cost;
					}
				});
				int totalCost = 0;
				for(Edge fromEdge :fromEdges){
					int useDemand = Math.min(cunsumerDemand, fromEdge.initBandWidth);
					totalCost+= useDemand * fromEdge.cost;
					cunsumerDemand-=useDemand;
					if(cunsumerDemand==0){
						break;
					}
				}
				if(totalCost>=depolyCostPerServer){
					isServerNode = true;
				}
			}
			if(isServerNode){
				lsMustServerNodes.add(consumerNode);
				isMustServerNode[consumerNode]= true;
			}
		}
		mustServerNodes = new int[lsMustServerNodes.size()];
		for(int i=0;i<lsMustServerNodes.size();++i){
			mustServerNodes[i] = lsMustServerNodes.get(i);
		}
		if(IS_DEBUG){
			System.out.println("服务器节点："+Arrays.toString(mustServerNodes));
		}
				
		// 初始解
		Server[] nextGlobalServers = new Server[consumerNum];
		for (int i = 0; i < consumerNum; ++i) {
			nextGlobalServers[i] = new Server(i, consumerNodes[i], consumerDemands[i]);
		}
		updateSolution(nextGlobalServers);	
		
		initCost = minCost;
		
		// 判断任务难易 
		int On = nodeNum * nodeNum * consumerNum;
		isNpHardest = On>=NP_HARDEST_THRESHOLD;
		isNpHard = On >= NP_HARD_THRESHOLD;
		if(IS_DEBUG){
			System.out.println("initCost:"+initCost);
			System.out.println("On:"+On+" isNpHardest:"+isNpHardest+" isNpHard:"+isNpHard);
		}
		
		/** 初始化缓存 */
		initAllCostAndPreNodes();
	}
	

	
//	static Server[] getConsumerServer(){
//		Server[] servers = new Server[consumerNum];
//		for(int i=0;i<consumerNum;++i){
//			servers[i] = new Server(i,consumerNodes[i],consumerDemands[i]);
//		}
//		return servers;
//	}
	
	/** 重置edge的带宽值 */
	public static void resetEdgeBandWidth() {
		for (Edge edge : edges) {
			edge.reset();
		}
	}
	

	/** 更新值 ，是否更好 */
	public static boolean updateSolution(Server[] nextGlobalServers) {
		
		int newMinCost = getTotalCost(nextGlobalServers);
		
		if (IS_DEBUG) {
			System.out.println("newMinCost:" + newMinCost);
			System.out.println(newMinCost < Global.minCost ? "better" : "worse");
		}
		
		if (newMinCost < Global.minCost) {
			minCost = newMinCost;
			updateBestSolution(nextGlobalServers);
			setBestServers(nextGlobalServers);
			return true;
		} else {
			return false;
		}
	}
	
	
	/** 更新值 ，是否更好 */
	public static boolean updateSolutionWithOutDebugInfo(Server[] nextGlobalServers) {
		
		int newMinCost = getTotalCost(nextGlobalServers);
		if (newMinCost < Global.minCost) {
			minCost = newMinCost;
			updateBestSolution(nextGlobalServers);
			setBestServers(nextGlobalServers);
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
	// qua
	private static StringBuilder stringBuilder = new StringBuilder();
	private static List<String> lines = new LinkedList<String>();
	private static void updateBestSolution(Server[] servers) {	
		lines.clear();
		for (Server server : servers) {
			if(server==null){
				break;
			}
			server.getSolution(lines,stringBuilder);
		}

		bestSolutionLength = 0;
		bestSolution[bestSolutionLength++] = String.valueOf(lines.size());
		bestSolution[bestSolutionLength++] = "";
		for (String line : lines) {
			bestSolution[bestSolutionLength++] = line;
		}
	}

	/** 获得总的费用 */
	public static int getTotalCost(Server[] servers) {
		int toatlCost = 0;
		for (Server server : servers) {
			if(server==null){
				break;
			}
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
		for (int i=0;i<bestSolutionLength;++i) {
			System.out.println(bestSolution[i]);
		}
		System.out.println("---------------");
	}
	
	/** 打印解决方案细节 */
	public static void printBestSolution(String[] bsetSoluttion) {
		System.out.println("---------------");
		System.out.println();
		for (String line : bsetSoluttion) {
			System.out.println(line);
		}
		System.out.println("---------------");
	}
	
	
	///////////////////////////////////
	// 预先计算
	//////////////////////////////////
	
	/** 消费者到所有节点的费用 */
	/** 最小消耗 */
	static int[][] allCost;
	/** 最小消耗的前向指针 */
	static int[][] allPreNodes;
	/** 按节点的费用进行排序，从低到高，包括自己 */
	static int[][] allPriorityCost;
	
	private static void initAllCostAndPreNodes() {
		// 初始化本地缓存
		allCost = new int[Global.consumerNum][Global.nodeNum];
		allPreNodes = new int[Global.consumerNum][Global.nodeNum];
		allPriorityCost = new int[Global.consumerNum][Global.nodeNum];
		
		final class Node implements Comparable<Node>{
			int node;
			int cost;
			public Node(int node, int cost) {
				super();
				this.node = node;
				this.cost = cost;
			}
			
			@Override
			public int compareTo(Node o) {
				// 费用从小到大排
				return cost-o.cost;
			}
		}
		
		for (int consumerId = 0; consumerId < Global.consumerNum; ++consumerId) {
			Arrays.fill(allPreNodes[consumerId], -1);
			initOneCostAndPreNodes(consumerId);
			
			// 保存费用优先级
			int[] costs = allCost[consumerId];
			Node[] nodes = new Node[Global.nodeNum];
			for(int node =0;node<Global.nodeNum;++node){
				nodes[node] = new Node(node,costs[node]);
			}
			Arrays.sort(nodes);
			int index = 0;
			for(Node node : nodes){
				allPriorityCost[consumerId][index++] = node.node;
			}
			
		}
		
		if(Global.IS_DEBUG){
			System.out.println("预计算完成：消费者到所有节点的费用 ");
		}
	}
	
	private static void initOneCostAndPreNodes(int consumerId) {

		int[] costs = allCost[consumerId];
		Arrays.fill(costs, Global.INFINITY);
		
		int[] visited = new int[Global.nodeNum];

		int[] preNodes = allPreNodes[consumerId];
		
		int startNode = Global.consumerNodes[consumerId];
		costs[startNode] = 0;
	
		while (true) {

			// 寻找下一个最近点
			int minCost = Global.INFINITY;
			int fromNode = -1;
			for (int node =0;node<Global.nodeNum;++node) {
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
			for (int toNode : Global.connections[fromNode]) {
				Edge edge = Global.graph[fromNode][toNode];
				int newCost = minCost + edge.cost;
				if (newCost < costs[toNode]) {
					costs[toNode] = newCost;
					// 添加路径
					preNodes[toNode] = fromNode;
				}
			}
			
		}
	}
	
	
	// >> 通用方法
	/** 
	 * 供子类调用：
	 * 转移到另一个服务器，并返回价格<br>
	 * 注意：可能cost会改变(又返回之前的点)
	 */
	static void transferTo(int consumerId,Server toServer,int transferBandWidth,int serverNode,int[] preNodes) {
		
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
	static int useBandWidthByPreNode(int demand,int serverNode,int[] preNodes ) {
		int node1 = serverNode;
		int node0 = preNodes[node1];
		
		int minBindWidth = INFINITY;
		while(node0!=-1){
			Edge edge = graph[node1][node0];
			if(edge.leftBandWidth<minBindWidth){				
				minBindWidth = edge.leftBandWidth;
				if(minBindWidth==0){
					break;
				}
			}
			node1 = node0;
			node0 = preNodes[node0];
		}
		
		if (minBindWidth == 0) {
			return 0;
		}
		
		int usedBindWidth = Math.min(minBindWidth, demand);
		
		node1 = serverNode;
		node0 = preNodes[node1];
		while(node0!=-1){
			Edge edge = graph[node1][node0];
			edge.leftBandWidth -= usedBindWidth;
			
			node1 = node0;
			node0 = preNodes[node0];
		}
		return usedBindWidth;
	}
	
}
