package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
	static final long TIME_OUT = System.currentTimeMillis() + 60 * 1000L;

	/** 是否超时 */
	static boolean isTimeOut() {
		return System.currentTimeMillis() > TIME_OUT;
	}

	/** 无穷大 */
	public static final int INFINITY = Integer.MAX_VALUE;

	/** 最小费用 */
	public static int minCost;
	/** 解决方案 */
	public static String[] soluttion;
	/** 最优多少种方案 */
	public static int bestServerNum;
	
	public static int[] bestGene;
	
	/** 最多费用 */
	public static int MAX_COST;

	/** 每台部署的成本：[0,5000]的整数 */
	public static int depolyCostPerServer;

	/** 节点数:不超过1000个 ,从0开始 */
	public static int nodeNum;
	/** 消费者数 */
	public static int consumerNum;

	/** 地图 */
	public static Edge[][] graph;
	/** 地图上的边 */
	public static Edge[] edges;
	/** 连接关系：下游节点 */
	public static int[][] connections; 
		
	/** 放置的服务器 */
	public static ArrayList<Server> servers = new ArrayList<Server>();

	/** 备份 */
	private static ArrayList<Server> copyServers;

	/** 备份 */
	private static ArrayList<Server> initServers;

	/** 初始化 */
	public static void initRest() {
		initServers = new ArrayList<Server>(servers.size());
		for (Server server : servers) {
			initServers.add(server.copy());
		}
	}

	/** 重置 */
	public static void reset() {
		// 恢复edge的带宽值
		for (Edge edge : edges) {
			edge.reset();
		}

		servers = new ArrayList<Server>(servers.size());
		for (Server server : initServers) {
			servers.add(server.copy());
		}
	}

	/** 保存当前状态 */
	public static void save() {
		// 保存edge的带宽值
		for (Edge edge : edges) {
			edge.saveCurrentBandWidth();
		}

		copyServers = new ArrayList<Server>(servers.size());
		for (Server server : servers) {
			copyServers.add(server.copy());
		}
	}

	/** 恢复之前的保存状态 */
	public static void goBack() {
		// 恢复edge的带宽值
		for (Edge edge : edges) {
			edge.goBackBandWidth();
		}
		servers = copyServers;
	}

	/** 打印网络信息 */
	public static void printNetworkInfo() {
		String buildInfo = String.format("节点数：%d,消费节点数：%d,每台部署成本：%d", nodeNum,
				servers.size(), Global.depolyCostPerServer);
		System.out.println(buildInfo);
	}

   public static int[] getGene(){
		int[] gene = new int[nodeNum];
		for(Server server : servers){
			gene[server.nodeId] = 1;
		}
		return gene;
	}
	
	/** 初始化解：将服务器直接放在消费节点上 */
	public static void initSolution() {
		minCost = getTotalCost();
		soluttion = getSolution();
		bestServerNum = servers.size();
		MAX_COST = Global.minCost;
		bestGene = getGene();
		
		System.out.println("MAX_COST：" + MAX_COST);
		
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
		
	}

	/** 更新值 ，是否更好 */
	public static boolean updateSolution() {
		int newMinCost = getTotalCost();
		String[] newSoluttion = getSolution();

		if (IS_DEBUG) {
			System.out.println("newMinCost:" + newMinCost);
			// System.out.println(newMinCost < Global.minCost ? "better" : "worse");
		}

		if (newMinCost < Global.minCost) {
			minCost = newMinCost;
			soluttion = newSoluttion;
			bestServerNum = servers.size();
			bestGene = getGene();
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
	private static String[] getSolution() {

		List<String> ls = new LinkedList<String>();
		for (Server server : Global.servers) {
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

	/** 获得最优解 */
	public static String[] getBestSolution() {
		return Global.soluttion;
	}

	/** 获得总的费用 */
	public static int getTotalCost() {
		int toatlCost = 0;
		for (Server server : Global.servers) {
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
		for (String line : Global.soluttion) {
			System.out.println(line);
		}
		System.out.println("---------------");
	}




}
