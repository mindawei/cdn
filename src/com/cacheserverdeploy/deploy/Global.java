package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 全局参数，方便访问
 * 
 * @author mindw
 * @date 2017年3月11日
 */
public final class Global {

	public static final boolean IS_DEBUG = true;

	/** 无穷大 */
	public static final int INFINITY = Integer.MAX_VALUE;

	/** 最小费用 */
	public static int minCost;
	/** 解决方案 */
	public static String[] soluttion;

	/** 每台部署的成本：[0,5000]的整数 */
	public static int depolyCostPerServer;

	/** 节点缓存 */
	public static final Set<String> nodes = new HashSet<String>();

	/** 边 */
	public static final Map<String, Map<String, Edge>> edges = new HashMap<String, Map<String, Edge>>();

	/** 放置的服务器 */
	public static ArrayList<Server> servers = new ArrayList<Server>();

	/** 备份 */
	private static ArrayList<Server> copyServers;

	/** 保存当前状态 */
	public static void save() {
		// 保存edge的带宽值
		for (Map<String, Edge> map : edges.values()) {
			for (Edge edge : map.values()) {
				edge.saveCurrentBandWidth();
			}
		}
		copyServers = new ArrayList<Server>(servers.size());
		for (Server server : servers) {
			copyServers.add(server.copy());
		}
	}

	/** 恢复之前的保存状态 */
	public static void goBack() {
		// 恢复edge的带宽值
		for (Map<String, Edge> map : edges.values()) {
			for (Edge edge : map.values()) {
				edge.goBackBandWidth();
			}
		}
		servers = copyServers;
	}

	/** 打印网络信息 */
	public static void printNetworkInfo() {
		String buildInfo = String.format("节点数：%d,消费节点数：%d,每台部署成本：%d",
				nodes.size(), servers.size(), Global.depolyCostPerServer);
		System.out.println(buildInfo);
	}

	/** 初始化解：将服务器直接放在消费节点上 */
	public static void initSolution() {
		Global.minCost = getTotalCost();
		Global.soluttion = getSolution();
	}

	/** 更新值 */
	public static void updateSolution() {
		int newMinCost = getTotalCost();
		String[] newSoluttion = getSolution();

		if (newMinCost < Global.minCost) {
			minCost = newMinCost;
			soluttion = newSoluttion;
			if (IS_DEBUG) {
				System.out.println("better!");
			}
		} else {
			if (IS_DEBUG) {
				System.out.println("worse!");
			}
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
	private static int getTotalCost() {
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
		System.out.println("总的服务器数：" + Global.servers.size());
		System.out.println();
		for (String line : Global.soluttion) {
			System.out.println(line);
		}
		System.out.println("---------------");
	}

	/** 添加一条边 */
	public static void addEdge(String fromNodeId, String toNodeId, Edge edge) {
		if (!edges.containsKey(fromNodeId)) {
			edges.put(fromNodeId, new HashMap<String, Edge>());
		}

		Map<String, Edge> outEdges = edges.get(fromNodeId);
		outEdges.put(toNodeId, edge);
	}

	/** 获得一条边,不存在则返回null */
	public static Edge getEdge(String fromNodeId, String toNodeId) {
		if (!edges.containsKey(fromNodeId)) {
			return null;
		}
		return edges.get(fromNodeId).get(toNodeId);
	}

	/** 获得下游Node */
	@SuppressWarnings("unchecked")
	public static Set<String> getToNodeIds(String fromNodeId) {
		if (!edges.containsKey(fromNodeId)) {
			return Collections.EMPTY_SET;
		}
		return edges.get(fromNodeId).keySet();
	}

	/**
	 * 消耗带宽最大带宽
	 * 
	 * @return 消耗掉的带宽
	 */
	public static int useBandWidth(int demand, ArrayList<String> nodes) {
		if (demand == 0) {
			return 0;
		}
		int minBindWidth = Global.INFINITY;
		for (int i = 0; i < nodes.size() - 1; ++i) {
			Edge edge = getEdge(nodes.get(i), nodes.get(i + 1));
			minBindWidth = Math.min(edge.bandWidth, minBindWidth);
		}
		if (minBindWidth == 0) {
			return 0;
		}
		int usedBindWidth = Math.min(minBindWidth, demand);
		for (int i = 0; i < nodes.size() - 1; ++i) {
			Edge edge = getEdge(nodes.get(i), nodes.get(i + 1));
			edge.bandWidth -= usedBindWidth;
		}
		return usedBindWidth;
	}

}
