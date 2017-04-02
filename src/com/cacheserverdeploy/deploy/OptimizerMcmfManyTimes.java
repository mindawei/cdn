package com.cacheserverdeploy.deploy;

/**
 * 最后阶段全移动：mcmf
 */
public final class OptimizerMcmfManyTimes{

	private final OptimizerMCMF optimizerMCMF;
	private final int[] nodes;

	public OptimizerMcmfManyTimes(OptimizerMCMF optimizerMCMF, int[] nodes) {
		this.optimizerMCMF = optimizerMCMF;
		this.nodes = nodes;
	}

	void optimizeMCMF() {

		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}

		if (Global.isTimeOut()) {
			return;
		}

		long t = System.currentTimeMillis();
		// 下一轮位置
		int[] newServers = new int[Global.nodeNum];
		int newServersSize = 0;
		// 临时转移的位置
		int[] servers = new int[Global.nodeNum];
		int serverSize = 0;
		
		int lastCost = Global.INFINITY;
		
		while (true) {

			if(Global.minCost>=lastCost){
				if(Global.IS_DEBUG){
					System.out.println("结束：最优解没有更新！");
				}
				break;
			}else{
				if(Global.IS_DEBUG){
					System.out.println("最优解更新,进入下一轮！");
				}
			}
			lastCost = Global.minCost;
			
			newServersSize = 0;
			for (Server server : Global.getBestServers()) {
				if (server == null) {
					break;
				}
				newServers[newServersSize++] = server.node;
			}

			if (newServersSize == 0) {
				break;
			}
			
			for (int i = 0; i < newServersSize; ++i) {
				int fromNode = newServers[i];

				// 服务器不移动
				if (Global.isMustServerNode[fromNode]) {
					continue;
				}

				for (int toNode : nodes) {
					// 防止自己到自己
					if (fromNode == toNode) {
						continue;
					}

					if (Global.isTimeOut()) {
						return;
					}
					
					// 移动
					serverSize = 0;
					for(int idx =0;idx<newServersSize;++idx){
						if(newServers[idx]!=fromNode){
							servers[serverSize++] = newServers[idx];
						}
					}
					servers[serverSize++] = toNode;
					optimizerMCMF.optimizeCASE(servers,serverSize);
					
				}

			}

			if (Global.isTimeOut()) {
				return;
			}

		}

		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: " + (System.currentTimeMillis() - t));
		}

	}

}
