package com.cacheserverdeploy.deploy;

/**
 * 基于 middle 优化
 * @author mindw
 * @date 2017年4月1日
 */
public final class OptimizerMiddleLimit2 extends OptimizerMiddle{ 

	/** 频率大于0的点 */
	private int[] nodes;

	/** 每轮最多移动多少次 */
	private final int maxMovePerRound;

	private final int MAX_UPDATE_NUM;
	private final int MIN_UPDATE_NUM;
	private final int minOffset;

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
	public OptimizerMiddleLimit2(int[] nodes, int maxMovePerRound,int maxUpdateNum, int minUpdateNum
			,int minOffset) {
		this.MAX_UPDATE_NUM = maxUpdateNum;
		this.MIN_UPDATE_NUM = minUpdateNum;
		this.nodes = nodes;
		this.maxMovePerRound = maxMovePerRound;
		this.minOffset = minOffset;
	}
	
	private void selcetServers() {
		serverNodesSize = 0;
		for (Server server : Global.getBestServers()) {
			if (server == null) {
				break;
			}
			serverNodes[serverNodesSize++] = server.node;
		}
	}


	int maxLiveNum = 10;
	int[][] allServerNodes = new int[maxLiveNum][];
	int[] costs = new int[maxLiveNum];

	public void initAllServerNodes(){
		allServerNodes[0] = new int[Global.consumerNum];
		for(int i=0;i<Global.consumerNum;++i){
			allServerNodes[0][i] = Global.consumerNodes[i];
		}
		
		for(int i=1;i<maxLiveNum;++i){
			allServerNodes[i] = new int[Global.consumerNum];
			
			
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

		int minCost = Global.INFINITY;
		int maxUpdateNum = MAX_UPDATE_NUM;
		int lastCost = minCost;
		
		int bestFromNode = -1;
		int bestToNode = -1;
		int lastToNode = -1;
		
	
		
		
		
		while (!Global.isTimeOut()) {
			
			if (serverNodesSize == 0) {
				break;
			}
			
			
			// 可选方案
			lastToNode = bestToNode;
			bestFromNode = -1;
			bestToNode = -1;
			int leftMoveRound = Math.min(nodes.length, maxMovePerRound / serverNodesSize);
			int updateNum = 0;
			boolean found = false;
			
			
			for (int j=0;j<leftMoveRound;++j) {
				int toNode = nodes[j];
				
				for (int i = 0; i < serverNodesSize; ++i) {
					int fromNode = serverNodes[i];

					if(fromNode==lastToNode){
						continue;
					}
					
					// 服务器不移动
					if (Global.isMustServerNode[fromNode]) {
						continue;
					}

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
			if(Math.abs(minCost-lastCost)<minOffset){
				if (Global.IS_DEBUG) {
					System.out.println("收敛太慢");
				}
				break;
			}else if (bestFromNode == -1) {
				if (Global.IS_DEBUG) {
					System.out.println("not better");
				}
				break;
			} else { // 移动
				moveBest(bestFromNode, bestToNode);
				if (Global.IS_DEBUG) {
					System.out.println("better : " + minCost);
				}
				lastCost = minCost;
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
