package com.cacheserverdeploy.deploy;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;


/**
 * 防止在局部最优中出不来 第一轮不随机，之后就完全随机了
 * 
 * @author mindw
 * @date 2017年3月23日
 */
public final class GreedyOptimizerLeve3 extends GreedyOptimizerMiddle {

	/** 频率大于0的点 */
	private int[] nodes;

	/** 候选节点是否被选中 */
	private final boolean[] selected;

	/** 每次最多随机选多少个 */
	private final int selectNum;
	/** 下一轮的服务器 */
	private Server[] serversInRandom;
	private int serverSize;

	private final Random random = new Random(47);

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
	public GreedyOptimizerLeve3(int nearestK, int selectNum,
			int maxMovePerRound, int maxUpdateNum, int minUpdateNum,
			int topK,double freqsRatio,double degreeRatio,double bandWidthRatio) {
		this.MAX_UPDATE_NUM = maxUpdateNum;
		this.MIN_UPDATE_NUM = minUpdateNum;
		this.selectNum = selectNum;
		nodes = initNodes(nearestK,topK, freqsRatio, degreeRatio, bandWidthRatio);
		if (Global.IS_DEBUG) {
			System.out.println("选出数目：" + nodes.length + " 总共数目："
					+ Global.nodeNum);
		}
		selected = new boolean[nodes.length];
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
		// selectRandomServers();

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
				// for (int toNode : nodes) {
				for (int toNode : nodes) {
					// 防止自己到自己
					if (fromNode == toNode) {
						continue;
					}

					if (Global.isTimeOut()) {
						return;
					}

					move(serversInRandom, fromNode, toNode);
					int cost = Global.getTotalCost(nextGlobalServers);
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
			int cost = Global.getTotalCost(nextGlobalServers);

			if (cost < lastCsot) {
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

				lastCsot = cost;
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

	final class Info {
		/** 花费 */
		final int cost;
		/** 前向指针 */
		final int[] preNodes;
		/** 目标节点 */
		final int toNode;

		public Info(int cost, int[] preNodes, int toNode) {
			super();
			this.cost = cost;
			this.preNodes = preNodes;
			this.toNode = toNode;
		}
	}



	final class NodeInfo {

		final int node;
		
		int freqs = 0;     // 在最短路径上的频率
		
		int bandWidth = 0; // 周围带宽
		
		int degree = 0;    // 度
		
		double rank = 0;

		public NodeInfo(int node) {
			super();
			this.node = node;
		}

		@Override
		public String toString() {
			return "NodeInfo [node=" + node + ", freqs=" + freqs
					+ ", bandWidth=" + bandWidth + ", degree=" + degree
					+ ", rank=" + rank + "]";
		}
		
	}

	int[] initNodes(int nearestK,int topK,double freqsRatio,double degreeRatio,double bandWidthRatio) {

		NodeInfo[] nodeInfos = new NodeInfo[Global.nodeNum];

		for (int fromNode = 0; fromNode < Global.nodeNum; ++fromNode) {
			nodeInfos[fromNode] = new NodeInfo(fromNode);
			for (int toNode = 0; toNode < Global.nodeNum; ++toNode) {
				if (Global.graph[fromNode][toNode] != null) {
					nodeInfos[fromNode].bandWidth += Global.graph[fromNode][toNode].initBandWidth;
					nodeInfos[fromNode].degree++;
				}
			}
		}

		// 不会超过消费者数
		nearestK = Math.min(nearestK, Global.nodeNum - 1);

		// 节点频率

		for (int consumerId = 0; consumerId < Global.consumerNum; ++consumerId) {

			int fromConsumerNode = Global.consumerNodes[consumerId];

			// 最大堆
			PriorityQueue<Info> priorityQueue = new PriorityQueue<Info>(
					nearestK + 1, new Comparator<Info>() {
						@Override
						public int compare(Info o1, Info o2) {
							return o2.cost - o1.cost;
						}
					});

			for (int toConsumerNode : Global.consumerNodes) {
				// 自己不到自己
				if (toConsumerNode == fromConsumerNode) {
					continue;
				}
				int cost = Global.allCost[consumerId][toConsumerNode];
				int[] preNodes = Global.allPreNodes[consumerId];
				Info info = new Info(cost, preNodes, toConsumerNode);
				priorityQueue.add(info);
				if (priorityQueue.size() > nearestK) {
					// 去掉最大的，保留 k个最小的
					priorityQueue.poll();
				}
			}

			// 统计频率
			Info info = null;
			while ((info = priorityQueue.poll()) != null) {
				int[] preNodes = info.preNodes;
				for (int node = info.toNode; node != -1; node = preNodes[node]) {
					nodeInfos[node].freqs++;
				}
			}
		}


		// 按频率从大到小排
		Arrays.sort(nodeInfos, new Comparator<NodeInfo>() {
			@Override
			public int compare(NodeInfo o1, NodeInfo o2) {
				return o2.freqs - o1.freqs;
			}
		});
		for (int i = 0; i < Global.nodeNum; ++i) {
			nodeInfos[i].rank +=freqsRatio*i; 
		}
		
		
		// 按带宽从大到小排
		Arrays.sort(nodeInfos, new Comparator<NodeInfo>() {
			@Override
			public int compare(NodeInfo o1, NodeInfo o2) {
				return o2.bandWidth - o1.bandWidth;
			}
		});
		for (int i = 0; i < Global.nodeNum; ++i) {
			nodeInfos[i].rank +=bandWidthRatio*i; 
		}
		
		// 按度从大到小排
		Arrays.sort(nodeInfos, new Comparator<NodeInfo>() {
			@Override
			public int compare(NodeInfo o1, NodeInfo o2) {
				return o2.degree - o1.degree;
			}
		});
		for (int i = 0; i < Global.nodeNum; ++i) {
			nodeInfos[i].rank += degreeRatio*i; 
		}
		
		// 按 rank从小到大排名
		Arrays.sort(nodeInfos, new Comparator<NodeInfo>() {
			@Override
			public int compare(NodeInfo o1, NodeInfo o2) {
				if(o1.rank - o2.rank<=0){
					return -1;
				}else{
					return 1;
				}
			}
		});
//		for (int i = 0; i < Global.nodeNum; ++i) {
//			nodeInfos[i].rank +=i; 
//		}
		
		System.out.println(Arrays.toString(nodeInfos));

		int[] selectedNodes = new int[Global.nodeNum];
		int len = 0;
		for (NodeInfo nodeFreq : nodeInfos) {
			selectedNodes[len++] = nodeFreq.node;
			if(len==topK){
				break;
			}
		}
		int[] nodes = new int[len];
		System.arraycopy(selectedNodes, 0, nodes, 0, len);

		if (Global.IS_DEBUG) {
			System.out.println("频率大于0的初始节点有" + nodes.length + "个");
		}
		return nodes;
	}

}
