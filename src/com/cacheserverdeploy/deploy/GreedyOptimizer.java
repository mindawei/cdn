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
	
	@Override
	void optimize() {
	
		while(true) {
			
			if(Global.isTimeOut()){
				break;
			}
			
			// 可选方案
			List<MoveAction> moveActions = new LinkedList<MoveAction>();
			for(Server server : Global.servers){		
				for(String toNodeId : Global.nodes){
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
			boolean better = Global.updateSolution();
			if(!better){
				break; // 返回
			}
					
		}
		
	}

}
