package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * 防止在局部最优中出不来
 * 
 * @author mindw
 * @date 2017年3月23日
 */
public class GreedyOptimizerTabuSearch extends GreedyOptimizerMiddle{

	@Override
	void optimize() {
	
		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 禁忌搜索开始接管 ");
		}

		if (Global.isTimeOut()) {
			return;
		}

		long t = System.currentTimeMillis();

		// 本地移动一步,各个结果之间过渡的时候回漏掉一步，故添加该方法  
		ArrayList<Server> nextGlobalServers = moveLocal(Global.getBestServers());
		Global.updateSolution(nextGlobalServers);
		
		int round = 100;
		
		int banedNum = Global.getBestServers().size();
		LinkedList<Integer> tabuNodes = new LinkedList<Integer>();
		
		while (true) {
			
			round--;
			if(round==0){
				Global.setBestServers(nextGlobalServers);
				break;
			}
			
			for(Server server : nextGlobalServers){
				System.out.print(server.node+" ");
			}
			System.out.println();

			
			// 可选方案
			int minCost = Global.INFINITY;
			int bestFromNode = -1;
			int bestToNode = -1;

			ArrayList<Server> oldGlobalServers = new ArrayList<Server>(nextGlobalServers);
			for (Server server : oldGlobalServers) {
				int fromNode = server.node;
				for (int toNode = 0; toNode < Global.nodeNum; ++toNode) {
					// 防止自己到自己
					if (fromNode == toNode||tabuNodes.contains(fromNode)) {
						continue;
					}

					if (Global.isTimeOut()) {
						return;
					}

					nextGlobalServers = move(oldGlobalServers, fromNode, toNode);
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
			nextGlobalServers = move(oldGlobalServers, bestFromNode, bestToNode);
			boolean better = Global.updateSolution(nextGlobalServers);

			if (!better) { 
				System.out.println(minCost);
				tabuNodes.add(bestFromNode);
				if(tabuNodes.size()>banedNum){
					tabuNodes.removeFirst();
				}
				
			}
			
		}

		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 禁忌搜索结束，耗时: " + (System.currentTimeMillis() - t));
		}
		
	}
}
