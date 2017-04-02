package com.cacheserverdeploy.deploy;

/**
 * 改进版 simple
 * @author mindw
 * @date 2017年4月1日
 */
public final class OptimizerSimpleLimit extends OptimizerSimple{

	/** 频率大于0的点 */
	private final int[] nodes;
	/** 每次最多随机选多少个 */
	private final int selectNum;
	/** 每轮最多移动多少次 */
	private final int maxMovePerRound;
	private final int MAX_UPDATE_NUM;
	private final int MIN_UPDATE_NUM;

	/**
	 * 构造函数
	 * 
	 * @param nearestK
	 *            初始化的时候选每个消费者几个最近领
	 * @param selectNum
	 *            随机生成的时候服务器个数
	 * @param maxMovePerRound
	 *            每轮最多移动多少次
	 */
	public OptimizerSimpleLimit(int[] nodes, int selectNum,
			int maxMovePerRound, int maxUpdateNum, int minUpdateNum) {
		this.nodes = nodes;
		this.selectNum = selectNum;
		this.MAX_UPDATE_NUM = maxUpdateNum;
		this.MIN_UPDATE_NUM = minUpdateNum;
		this.maxMovePerRound = maxMovePerRound;
	}
	
	private final void selcetBestServers() {
		serverNodesSize = 0;

		int leftNum = selectNum;
		// 肯定是服务器的
		for (int node : Global.mustServerNodes) {
			serverNodes[serverNodesSize++] = node;
		}
		leftNum -= Global.mustServerNodes.length;
		int index = 0;
		// 随机选择
		while (leftNum > 0 && index < nodes.length) {
			// 没有被选过
			int node = nodes[index++];
			// 服务器上面已经添加过了
			if (!Global.isMustServerNode[node]) {
				serverNodes[serverNodesSize++] = node;
				leftNum--;
			}
		}
	}
	
	@Override
	void optimize() {

		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}

		if (Global.isTimeOut()) {
			return;
		}

		long t = System.currentTimeMillis();

		selcetBestServers();

		int minCost = Global.INFINITY;
		int maxUpdateNum = MAX_UPDATE_NUM;
	
		while (!Global.isTimeOut()) {
			
			if (serverNodesSize == 0) {
				break;
			}
			
			// 可选方案
			int bestFromNode = -1;
			int bestToNode = -1;
			int leftMoveRound = Math.min(nodes.length,maxMovePerRound / serverNodesSize);
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
