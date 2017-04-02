package com.cacheserverdeploy.deploy;

/**
 * 贪心搜索，寻找一个最优解，达到具备最优解后就马上退出
 * 
 * @author mindw
 * @date 2017年3月12日
 */
public abstract class Optimizer {

	/** 下一轮的服务器 */
	protected final int[] serverNodes = new int[Global.nodeNum];
	protected int serverNodesSize = 0;

	public void updateBeforeReturn() {
		Server[] servers = new Server[serverNodesSize];
		for (int i = 0; i < serverNodesSize; ++i) {
			servers[i] = new Server(serverNodes[i]);
		}
		Global.setBestServers(servers);
		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + "服务器设置完成");
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

		// 选择服务器
		serverNodesSize = 0;
		for (Server server : Global.getBestServers()) {
			if(server==null){
				break;
			}
			serverNodes[serverNodesSize++] = server.node;
		}
		
		int minCost = Global.INFINITY;
		
		while (!Global.isTimeOut()) {

			if (serverNodesSize == 0) {
				break;
			}

			// 可选方案
			int bestFromNode = -1;
			int bestToNode = -1;

			for (int i = 0; i < serverNodesSize; ++i) {
				int fromNode = serverNodes[i];

				// 服务器不移动
				if (Global.isMustServerNode[fromNode]) {
					continue;
				}

				for (int toNode = 0; toNode < Global.nodeNum; ++toNode) {

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

					int cost = getCostAfterMove(fromNode, toNode);
					if (cost < minCost) {
						minCost = cost;
						bestFromNode = fromNode;
						bestToNode = toNode;
					}
				}
			}

			// not better
			if (bestFromNode==-1) {
				if (Global.IS_DEBUG) {
					System.out.println("not better" );
				}
				break;
			}else{ // 移动
				moveBest(bestFromNode, bestToNode);
				if (Global.IS_DEBUG) {
					System.out.println("better : " + minCost);
				}
			}
		}

		if(minCost<Global.minCost){
			updateBeforeReturn();
		}

		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: " + (System.currentTimeMillis() - t));
		}

	}

	protected abstract int getCostAfterMove(int fromNode, int toNode);

	protected abstract void moveBest(int bestFromNode, int bestToNode);

}