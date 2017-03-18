package com.cacheserverdeploy.deploy;

import java.util.LinkedList;
import java.util.List;

/**
 * 贪心搜索，寻找一个最优解，达到具备最优解后就马上退出
 * 
 * @author mindw
 * @date 2017年3月12日
 */
public final class GreedyOptimizer extends Optimizer {
		
	int lastCost = Global.INFINITY;
	
	 // private final Random random = new Random(47);

	
	@Override
	void optimize() {
	
		while(true) {
			
			if(Global.isTimeOut()){
				break;
			}
			
			// 可选方案
			List<MoveAction> moveActions = new LinkedList<MoveAction>();
			for(Server server : Global.servers){		
				for(int toNodeId =0;toNodeId<Global.nodeNum;++toNodeId){
					moveActions.add(new MoveAction(server.nodeId, toNodeId));
				}
			}
			
			MoveAction bestMoveAction = null;
			int minCost = Global.INFINITY;
			
			for (MoveAction moveAction : moveActions) {
				Global.save();
				// 启发函数： 花费 + 这个点的移动频率
				move(moveAction);
				int cost = Global.getTotalCost();
				if (cost < minCost) {
					minCost = cost;
					bestMoveAction = moveAction;
				}
				Global.goBack();
			}

			if (bestMoveAction == null) {
				break;
			}
			
			// 移动
			move(bestMoveAction);
//			boolean better = Global.updateSolution();
//			if(!better){ 
//				break;
//			}
			
			int cost = Global.updateSolution();
			if(cost<lastCost){ // better
				lastCost = cost;
				// System.out.println("best cost:"+lastCost);
			}else{
				break;
				
//				Global.reset();
//				int maxRound = 10000;
//				int[] gene = new int[Global.nodeNum];
//				int num = Global.consumerNum;
//				
//				for(Server server :Global.servers){
//					gene[server.nodeId] = 1;
//				}
//				while(maxRound-->0&&num>0){
//					int node = random.nextInt(Global.nodeNum);
//					if(gene[node]==0){
//						gene[node] = 1;
//						num--;
//					}
//				}
//				for(Server server :Global.servers){
//					gene[server.nodeId] = 0;
//				}
//				move(gene);
//				lastCost = Global.updateSolution();
				
			}
					
		}
		
	}
	
	
	

}
