package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.HashMap;
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
	static final boolean IS_DEBUG = false;

	/** 何时超时 */
	static final long TIME_OUT = System.currentTimeMillis() + 80 * 1000L;

	/** 是否超时 */
	static boolean isTimeOut() {
		return System.currentTimeMillis() > TIME_OUT;
	}
	
	/** 是否非常难 */
	static boolean isNpHardest;
	
	/** 是否困难 */
	static boolean isNpHard;
	
	private static final int NP_HARD_THRESHOLD    = 1000000;
	
	private static final int NP_HARDEST_THRESHOLD = 10000000;

	/** 无穷大 */
	static final int INFINITY = Integer.MAX_VALUE;

	/** 最小费用 */
	private static int minCost = INFINITY;
	private static int initCost;
	
	/** 初始解是否陷入局部最优了 */
	static boolean isDropInInit(){
		return initCost == minCost;
	}
	
	/** 解决方案 */
	private static String[] bsetSolution;

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
			
	/** 放置的服务器 */
	private static ArrayList<Server> bestServers;

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
		
		initCost = minCost;
		
		// 判断任务难易 
		int On = nodeNum * nodeNum * consumerNum;
		isNpHardest = On>NP_HARDEST_THRESHOLD;
		isNpHard = On > NP_HARD_THRESHOLD;
		if(IS_DEBUG){
			System.out.println("initCost:"+initCost);
			System.out.println("On:"+On+" isNpHard:"+isNpHard);
		}
	}
	
	public static ArrayList<Server> getBestServers() {
		return bestServers;
	}
	
	public static void setBestServers(ArrayList<Server> nextGlobalServers) {
		bestServers = nextGlobalServers;
	}
	
	public static String[] getBsetSolution() {
		return bsetSolution;
	}
	
	static Server[] getConsumerServer(){
		Server[] servers = new Server[consumerNum];
		for(int i=0;i<consumerNum;++i){
			servers[i] = new Server(i,consumerNodes[i],consumerDemands[i]);
		}
		return servers;
	}
	
	/** 重置edge的带宽值 */
	public static void resetEdgeBandWidth() {
		for (Edge edge : edges) {
			edge.reset();
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
			bsetSolution = getSolution(bestServers);
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
		for (String line : bsetSolution) {
			System.out.println(line);
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
	
}
