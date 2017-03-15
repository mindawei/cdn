package com.cacheserverdeploy.deploy;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 启发式搜素：当前全局的服务费用最下
 * 
 * @author mindw
 * @date 2017年3月12日
 */
public class HeuristicOptimizer extends Optimizer {

	private final Optimizer previousOptimizer;
	
	public HeuristicOptimizer(Optimizer previousOptimizer){
		this.previousOptimizer = previousOptimizer;
	}
	
	/** 存活周期 ,单位：推进次数  */	
	private static final int LIVE_TIME = 1;
	
	// private static int nearestK = Global.nodes.size();
	
	private static final Random random = new Random(47);
	
	/**
	 * 简单的思路：只是在边界合并
	 */
	@Override
	void optimize() {
		
		if(previousOptimizer!=null){
			previousOptimizer.optimize();
		}
		
		Map<String,Integer> visitedNodes = new HashMap<String,Integer>();
		for(Server server : Global.servers){
			visitedNodes.put(server.nodeId,LIVE_TIME);
		}
		
		final int RANDOM_RATE = 10;
		int randomRate = 10; // 1/10随机
		
		// 计算最优解重复次数
		int sameBestCostNum = 0;
		int MAX_SAME_BEST_COST_NUM = 100;
	
		while(true) {
			
			if(Global.isTimeOut()){
				break;
			}
			
			// 可选方案
			List<MoveAction> pairs = new LinkedList<MoveAction>();
			for(Server server : Global.servers){
				// 获得可以移动的点
				// Set<String> toNodeIds = Router.getNearestK(Router.getToNodeCost(server.nodeId), nearestK);
				
				for(String toNodeId : Global.nodes){
					// 排除移动
					if(!visitedNodes.containsKey(toNodeId)){
						pairs.add(new MoveAction(server.nodeId, toNodeId));
					}
				}
			}
			// 无可选方案
			if(pairs.size()==0){
				break;
			}
			

			MoveAction bestNextPair = null;
			int minCost = Global.INFINITY;
			
			randomRate--;
			// 增加扰动性
			if(randomRate>0){
				for (MoveAction nextPair : pairs) {
					Global.save();
					// 启发函数： 花费 + 这个点的移动频率
					move(nextPair);
					int cost = Global.getTotalCost();
					if (cost < minCost) {
						minCost = cost;
						bestNextPair = nextPair;
					}
					Global.goBack();
				}
			}else{
				randomRate = RANDOM_RATE;
				int randomIndex = random.nextInt(pairs.size());
				bestNextPair = pairs.get(randomIndex);
			}
			
			if (bestNextPair != null) {
				// 移动
				move(bestNextPair);
				boolean better = Global.updateSolution();
				if(better){
					sameBestCostNum = 0;
				}else{
					sameBestCostNum++;
					if(sameBestCostNum==MAX_SAME_BEST_COST_NUM){
						
					}
				}
				visitedNodes.put(bestNextPair.newServerNodeId,LIVE_TIME);
			} else {
				break;
			}
			
			List<String> needRemovedVisitedNodeIds = new LinkedList<String>(); 
			for(Map.Entry<String, Integer> entry : visitedNodes.entrySet()){
				String nodeId = entry.getKey();
				int liveTime = entry.getValue();
				liveTime-=1;
				if(liveTime<=0){
					needRemovedVisitedNodeIds.add(nodeId);
				}else{
					visitedNodes.put(nodeId, liveTime);
				}
			}
			
			for(String nodeId : needRemovedVisitedNodeIds){
				visitedNodes.remove(nodeId);
			}
			
		}
		
	}
}
