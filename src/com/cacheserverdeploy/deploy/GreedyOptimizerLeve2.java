package com.cacheserverdeploy.deploy;

/**
 * 防止在局部最优中出不来 第一轮不随机，之后就完全随机了
 * 
 * @author mindw
 * @date 2017年3月23日
 */
public final class GreedyOptimizerLeve2 extends GreedyOptimizerSimple {

	/** 频率大于0的点 */
	private int[] nodes;

	/** 每次最多随机选多少个 */
	private final int selectNum;
	/** 下一轮的服务器 */
	private Server[] serversInRandom;
	private int serverSize;
	
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
	public GreedyOptimizerLeve2(int[] nodes, int selectNum,
			int maxMovePerRound, int maxUpdateNum, int minUpdateNum) {
		this.nodes = nodes;
		this.MAX_UPDATE_NUM = maxUpdateNum;
		this.MIN_UPDATE_NUM = minUpdateNum;
		this.selectNum = selectNum;
		serversInRandom = new Server[Global.nodeNum];
		this.maxMovePerRound = maxMovePerRound;
	}

	private void selcetBestServers() {
		serverSize = 0;

		int leftNum = selectNum;
		// 肯定是服务器的
		for (int node : Global.mustServerNodes) {
			serversInRandom[serverSize++] = new Server(node);
		}
		leftNum -= Global.mustServerNodes.length;
		int index = 0;
		// 随机选择
		while (leftNum > 0 && index < nodes.length) {
			// 没有被选过
			int node = nodes[index++];
			// 服务器上面已经添加过了
			if (!Global.isMustServerNode[node]) {
				serversInRandom[serverSize++] = new Server(node);
				leftNum--;
			}
		}
		// 设置结束标志
		if (serverSize < serversInRandom.length) {
			serversInRandom[serverSize] = null;
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

		int lastCsot = Global.INFINITY;
		int maxUpdateNum = MAX_UPDATE_NUM;

		while (true) {

			// 可选方案
			int minCost = Global.INFINITY;
			int bestFromNode = -1;
			int bestToNode = -1;

			if (serverSize == 0) {
				break;
			}

			final int leftMoveRound = maxMovePerRound / serverSize;

			int updateNum = 0;
			boolean found = false;
			if (Global.IS_DEBUG) {
				System.out.println("maxUpdateNum:" + maxUpdateNum);
			}

			for (int i = 0; i < serverSize; ++i) {
				Server oldServer = serversInRandom[i];
				int fromNode = oldServer.node;

				// 服务器不移动
				if (Global.isMustServerNode[fromNode]) {
					continue;
				}

				int leftNum = leftMoveRound;
				for (int toNode : nodes) {
					// 防止自己到自己
					if (fromNode == toNode) {
						continue;
					}

					if (Global.isTimeOut()) {
						return;
					}

					int cost = getCostAfterMove(serversInRandom, fromNode,toNode);
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

					leftNum--;
					if (leftNum == 0) {
						break;
					}

				}

				if (found) {
					break;
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

			if (minCost == Global.INFINITY) {
				break;
			}

			if (Global.isTimeOut()) {
				return;
			}

			// 移动
			move(serversInRandom, bestFromNode, bestToNode);
			if (minCost < lastCsot) {
				serverSize = 0;
				for (Server server : nextGlobalServers) {
					if (server == null) {
						break;
					}
					serversInRandom[serverSize++] = server;
				}
				// 设置终止
				if (serverSize < serversInRandom.length) {
					serversInRandom[serverSize] = null;
				}

				lastCsot = minCost;
				Global.updateSolution(serversInRandom);
			} else { // not better
				break;
				// lastCsot = Global.INFINITY;
				// selectRandomServers();
				// maxUpdateNum = MAX_UPDATE_NUM;
			}

		}

		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: "
					+ (System.currentTimeMillis() - t));
		}

	}
	
}
