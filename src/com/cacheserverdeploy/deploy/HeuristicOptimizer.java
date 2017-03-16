package com.cacheserverdeploy.deploy;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * 启发式搜素
 * 
 * @author mindw
 * @date 2017年3月12日
 */
public class HeuristicOptimizer extends Optimizer {

	private final class Info implements Comparable<Info>{
		
		int cost;
		MoveAction action;
		
		public Info(int cost, MoveAction action) {
			super();
			this.cost = cost;
			this.action = action;
		}
		
		@Override
		public int compareTo(Info other) {
			return cost-other.cost;
		}
		
	}
	
	@Override
	void optimize() {

		// 禁忌
		final int LIVE_TIME = 1;
		// 随机种子
		final Random random = new Random(47);

		Map<String, Integer> visitedNodes = new HashMap<String, Integer>();
		for (Server server : Global.servers) {
			visitedNodes.put(server.nodeId, LIVE_TIME);
		}
		
		int lastCost = Global.getTotalCost();
		int topK = 2;

		while (true) {

			if (Global.isTimeOut()) {
				break;
			}

			// 可选方案
			List<MoveAction> pairs = new LinkedList<MoveAction>();
			for (Server server : Global.servers) {
				// 获得可以移动的点
				// Set<String> toNodeIds =
				// Router.getNearestK(Router.getToNodeCost(server.nodeId),
				// nearestK);
				for (String toNodeId : Global.nodes) {
					// 排除移动
					if (!visitedNodes.containsKey(toNodeId)) {
						pairs.add(new MoveAction(server.nodeId, toNodeId));
					}
				}
			}
			// 无可选方案
			if (pairs.size() == 0) {
				break;
			}
	
			PriorityQueue<Info> priorityQueue = new PriorityQueue<Info>();		
			for (MoveAction action : pairs) {
				Global.save();
				move(action);
				int cost = Global.getTotalCost();
				priorityQueue.add(new Info(cost, action));
				Global.goBack();
			}
			

			if (priorityQueue.size() == 0) {
				break;
			}
			
			
			int size = Math.min(topK, priorityQueue.size());
			int k = random.nextInt(size);
			MoveAction bestNextPair = null;
			while(k-->0){
				if(priorityQueue.poll().cost>Global.MAX_COST){
					break;
				}
			}
			bestNextPair = priorityQueue.poll().action;
			
			// 移动
			move(bestNextPair);
			int cost = Global.getTotalCost();
			lastCost = cost;
			Global.updateSolution();	
			visitedNodes.put(bestNextPair.newServerNodeId, LIVE_TIME);
			
			if(cost<lastCost){
				topK--;
				if(topK<0){
					topK=0;
				}
			}else{ // >= cost;
				topK++;
				if(topK>128){
					topK = 128;
				}
			}
		
			List<String> needRemovedVisitedNodeIds = new LinkedList<String>();
			for (Map.Entry<String, Integer> entry : visitedNodes.entrySet()) {
				String nodeId = entry.getKey();
				int liveTime = entry.getValue();
				liveTime -= 1;
				if (liveTime <= 0) {
					needRemovedVisitedNodeIds.add(nodeId);
				} else {
					visitedNodes.put(nodeId, liveTime);
				}
			}

			for (String nodeId : needRemovedVisitedNodeIds) {
				visitedNodes.remove(nodeId);
			}

		}

	}
}
