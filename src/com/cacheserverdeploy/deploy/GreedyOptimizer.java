package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 贪心搜索，寻找一个最优解，达到具备最优解后就马上退出
 * 
 * @author mindw
 * @date 2017年3月12日
 */
public abstract class GreedyOptimizer {

	void optimize() {
		
		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}

		long t = System.currentTimeMillis();

		if (!Global.isTimeOut()) {
			// 本地移动一步,各个结果之间过渡的时候回漏掉一步，故添加该方法  
			ArrayList<Server> nextGlobalServers = moveLocal(Global.getBestServers());
			Global.updateSolution(nextGlobalServers);
		}
		
		while (true) {

			if (Global.isTimeOut()) {
				break;
			}

			// 可选方案
			int minCost = Global.INFINITY;
			int bestFromNode = -1;
			int bestToNode = -1;

			ArrayList<Server> oldGlobalServers = Global.getBestServers();
			for (Server server : oldGlobalServers) {
				int fromNode = server.node;
				for (int toNode = 0; toNode < Global.nodeNum; ++toNode) {
					// 防止自己到自己
					if (fromNode == toNode) {
						continue;
					}

					if (Global.isTimeOut()) {
						return;
					}

					ArrayList<Server> nextGlobalServers = move(oldGlobalServers, fromNode, toNode);
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

			// 移动
			ArrayList<Server> nextGlobalServers = move(oldGlobalServers, bestFromNode, bestToNode);
			boolean better = Global.updateSolution(nextGlobalServers);

			if (!better) { // better
				break;
			}
		}

		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: " + (System.currentTimeMillis() - t));
		}
	}

	/** 进行一步移动 */
	private ArrayList<Server> move(ArrayList<Server> oldGlobalServers, int fromServerNode, int toServerNode) {
		Map<Integer, Server> newServers = new HashMap<Integer, Server>();
		for (Server server : oldGlobalServers) {
			if (server.node != fromServerNode) {
				newServers.put(server.node, new Server(server.node));
			}
		}
		newServers.put(toServerNode, new Server(toServerNode));

		Server[] consumerServers = Global.getConsumerServer();

		Global.resetEdgeBandWidth();

		return transferServers(consumerServers, newServers);
	}

	/** 本地移动一步,各个结果之间过渡的时候回漏掉一步，故添加该方法   */
	private ArrayList<Server> moveLocal(ArrayList<Server> oldGlobalServers) {
		Map<Integer, Server> newServers = new HashMap<Integer, Server>();
		for (Server server : oldGlobalServers) {
			newServers.put(server.node, new Server(server.node));
		}
		
		Server[] consumerServers = Global.getConsumerServer();

		Global.resetEdgeBandWidth();

		return transferServers(consumerServers, newServers);
	}
	
	/** 不同的搜索策略需要提供此方法 */
	protected abstract ArrayList<Server> transferServers(Server[] consumerServers, Map<Integer, Server> newServers);

}
