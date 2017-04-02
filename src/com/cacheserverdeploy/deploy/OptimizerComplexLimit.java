package com.cacheserverdeploy.deploy;

/**
 * 基于 middle 优化
 * @author mindw
 * @date 2017年4月1日
 */
public final class OptimizerComplexLimit extends OptimizerComplex{ 

	public OptimizerComplexLimit(String[] graphContent,int[] nodes, int maxMovePerRound,int maxUpdateNum, int minUpdateNum){
			super(graphContent);
			this.MAX_UPDATE_NUM = maxUpdateNum;
			this.MIN_UPDATE_NUM = minUpdateNum;
			this.nodes = nodes;
			this.maxMovePerRound = maxMovePerRound;
	}

	/** 频率大于0的点 */
	private int[] nodes;

	/** 每轮最多移动多少次 */
	private final int maxMovePerRound;

	private final int MAX_UPDATE_NUM;
	private final int MIN_UPDATE_NUM;

	
	private void selcetServers() {
		serverNodesSize = 0;
		for (Server server : Global.getBestServers()) {
			if (server == null) {
				break;
			}
			serverNodes[serverNodesSize++] = server.node;
		}
	}

	void optimize() {

		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}

		if (Global.isTimeOut()) {
			return;
		}

		long t = System.currentTimeMillis();

		selcetServers();
		
		int minCost = Global.minCost;
		int maxUpdateNum = MAX_UPDATE_NUM;
	
		while (!Global.isTimeOut()) {
			
			if (serverNodesSize == 0) {
				break;
			}
			
			// 可选方案
			int bestFromNode = -1;
			int bestToNode = -1;
			int leftMoveRound = maxMovePerRound / serverNodesSize;
			int updateNum = 0;
			boolean found = false;
			
			for (int i = 0; i < serverNodesSize; ++i) {
				int fromNode = serverNodes[i];

				// 服务器不移动
				if (Global.isMustServerNode[fromNode]) {
					continue;
				}

				for (int j=0;j<leftMoveRound;++j) {
					int toNode = nodes[j];
					// 防止自己到自己
					if (fromNode == toNode) {
						continue;
					}
					
					if (Global.isTimeOut()) {
						if(bestFromNode!=-1){
							moveBest(bestFromNode, bestToNode);
						}
						if(minCost<Global.minCost){
							updateBeforeReturn();
						}
						return;
					}
					
					int cost = getCostAfterMove(fromNode,toNode);
					if (cost < minCost) {
						minCost = cost;
						bestFromNode = fromNode;
						bestToNode = toNode;
						updateNum++;
						if (updateNum == maxUpdateNum) {
							found = true;
							break;
						}
					}
				}

				if (found) {
					break;
				}

			}
			
			// not better
			if (bestFromNode == -1) {
				if (Global.IS_DEBUG) {
					System.out.println("not better");
				}
				break;
			} else { // 移动
				moveBest(bestFromNode, bestToNode);
				if (Global.IS_DEBUG) {
					System.out.println("better : " + minCost);
				}
			}

			if (maxUpdateNum <= updateNum) {
				maxUpdateNum++;
				if (maxUpdateNum > MAX_UPDATE_NUM) {
					maxUpdateNum = MAX_UPDATE_NUM;
				}
			} else { // > updateNum
				maxUpdateNum--;
				if (maxUpdateNum < MIN_UPDATE_NUM) {
					maxUpdateNum = MIN_UPDATE_NUM;
				}
			}
			
		}
		
		if(minCost<Global.minCost){
			updateBeforeReturn();
		}
		
		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: "+ (System.currentTimeMillis() - t));
		}

	}
}
