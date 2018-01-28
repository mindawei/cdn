package com.cacheserverdeploy.deploy;

public final class OptReduce {

	/** 下一轮的服务器 */
	private final int[] serverNodes;
	/** 节点的输出 */
	private final int[] serverOutputs;
	/** 节点的数目 */
	private int serverNodesSize;

	private final int maxOutput;
	private final int[] nodes;
	private final int nodeNum;
	private int minCost;

	public OptReduce(int[] nodes) {
		this.nodes = nodes;
		this.maxOutput = Global.maxServerOutput;
		;
		this.nodeNum = Global.nodeNum;
		this.serverNodes = new int[nodeNum];
		this.serverOutputs = new int[nodeNum];
	}

	void optimize() {

		long t;
		if (Global.IS_DEBUG) {
			t = System.currentTimeMillis();
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}

		// 初始化服务器
		serverNodesSize = 0;
		for (Server server : Global.getBestServers()) {
			if (server == null) {
				break;
			}
			serverNodes[serverNodesSize] = server.node;
			serverNodesSize++;
		}

		minCost = Global.minCost;

		boolean find = true;
		int cost;

		while (find) {
			find = false;
			for (int i = 0; i < serverNodesSize; ++i) {
				cost = costAfterReduce(serverNodes[i]);
				if (cost < minCost) {
					minCost = cost;
					if (Global.IS_DEBUG) {
						System.out.println(minCost);
					}
					find = true;
					break;
				}
			}
		}

		if (minCost >= Global.minCost) {
			return;
		}

		// 设置服务器
		Global.minCost = minCost;
		Server[] servers = new Server[serverNodesSize];
		for (int i = 0; i < serverNodesSize; ++i) {
			servers[i] = new Server(serverNodes[i]);
			servers[i].ouput = serverOutputs[i];
		}
		Global.setBestServers(servers);

		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: " + (System.currentTimeMillis() - t));
		}

	}

	/** 安装费用 */
	private final int[] installedCost = new int[Global.nodeNum];
	/** 剩余的输出 */
	private final int[] leftOutput = new int[Global.nodeNum];
	private final int[] consumerDemands = new int[Global.consumerNum];
	/** 是否是新服务器 */
	private final boolean[] isNewServer = new boolean[Global.nodeNum];

	private int totalCost;

	/** 进行一步移动 ,不要改变传进来的Server,结果缓存在 nextGlobalServers */
	private final int costAfterReduce(int fromServerNode) {

		// 复制需求
		System.arraycopy(Global.consumerDemands, 0, consumerDemands, 0, Global.consumerNum);
		Global.resetEdgeBandWidth();

		for (int i = 0; i < nodeNum; ++i) {
			isNewServer[i] = false;
			leftOutput[i] = maxOutput;
			installedCost[i] = 0;
		}

		for (int i = 0; i < serverNodesSize; ++i) {
			isNewServer[serverNodes[i]] = true;
		}
		isNewServer[fromServerNode] = false;

		// 先用simple做
		totalCost = 0;

		for (int consumerId : Global.consumerIds) {

			int consumerNode = Global.consumerNodes[consumerId];

			// 将起始点需求分发到目的地点中，会改变边的流量
			for (int node : Global.allPriorityCost[consumerId]) {

				if (isNewServer[node] && leftOutput[node] > 0) {

					// 申请的流量 = 本身需求和服务器还能够提供输出的最小值
					int applyDemand = Math.min(consumerDemands[consumerId], leftOutput[node]);

					// 就是本地
					if (node == consumerNode) {
						leftOutput[node] -= applyDemand;
						consumerDemands[consumerId] -= applyDemand;
						if (consumerDemands[consumerId] == 0) {
							break;
						}
						continue;
					}

					int usedDemand = Global.useBandWidthByPreNode(applyDemand, node, Global.allPreNodes[consumerId]);
					// 可以消耗
					if (usedDemand > 0) {
						totalCost += Global.allCost[consumerId][node] * usedDemand;

						leftOutput[node] -= usedDemand;
						consumerDemands[consumerId] -= usedDemand;
						if (consumerDemands[consumerId] == 0) {
							break;
						}
					}

				}
			}
		}

		// 计算预分配服务器部署费用,i 为node
		for (int i = 0; i < nodeNum; ++i) {
			if (leftOutput[i] < maxOutput) {
				installedCost[i] = Global.deployServerCost(i, maxOutput - leftOutput[i]);
				totalCost += installedCost[i];
			}
		}

		for (int consumerId : Global.consumerIds) {
			if (consumerDemands[consumerId] == 0) {
				continue;
			}

			while (totalCost < minCost && transferCost(consumerId))
				;

			if (totalCost >= minCost) {
				return inf;
			}

			if (consumerDemands[consumerId] > 0) {

				int consumerNode = Global.consumerNodes[consumerId];
				leftOutput[consumerNode] -= consumerDemands[consumerId];

				// 无法满足需求
				if (leftOutput[consumerNode] < 0) {
					return inf;
				}

				int deployCost = Global.deployServerCost(consumerNode, maxOutput - leftOutput[consumerNode]);
				if (deployCost > installedCost[consumerNode]) {
					totalCost += deployCost - installedCost[consumerNode];
					installedCost[consumerNode] = deployCost;
					if (totalCost >= minCost) {
						return inf;
					}
				}
			}
		}

		// 小于则移动
		if (totalCost < minCost) {
			serverNodesSize = 0;
			for (int i = Global.nodeNum - 1; i >= 0; --i) {
				if (leftOutput[nodes[i]] < maxOutput) {
					serverNodes[serverNodesSize] = nodes[i];
					serverOutputs[serverNodesSize] = maxOutput - leftOutput[nodes[i]];
					serverNodesSize++;
				}
			}
			return totalCost;
		} else {
			return inf;
		}
	}

	private final int inf = Global.INFINITY;

	/**
	 * 将起始点需求分发到目的地点中，会改变边的流量<br>
	 * 
	 * @return 是否需要继续转移
	 */
	private boolean transferCost(int consumerId) {

		for (int i = 0; i < nodeNum; ++i) {
			vis[i] = false;
			costs[i] = inf;
		}

		int fromNode = Global.consumerNodes[consumerId];

		// 还剩多少个服务节点未使用
		int leftServerNodeNum = serverNodesSize - 1;

		// queue
		qSize = 0;

		// 自己到自己的距离为0
		costs[fromNode] = 0;
		preNodes[fromNode] = -1;

		queAdd(fromNode);

		// 是否找的一条减少需求的路
		boolean fromDemandSmaller = false;

		int minCostNode;
		int applyDemand;

		while (leftServerNodeNum > 0 && qSize > 0) {

			// 寻找下一个最近点
			minCostNode = quePoll();

			// 访问过了
			vis[minCostNode] = false;

			// 是服务器 && 申请的流量 = 本身需求和服务器还能够提供输出的最小值
			if (isNewServer[minCostNode]
					&& (applyDemand = Math.min(consumerDemands[consumerId], leftOutput[minCostNode])) > 0) {
				int usedDemand = Global.useBandWidthByPreNode(applyDemand, minCostNode, preNodes);
				// 可以消耗
				if (usedDemand > 0) {

					totalCost += costs[minCostNode] * usedDemand;

					consumerDemands[consumerId] -= usedDemand;
					leftOutput[minCostNode] -= usedDemand;

					int deployCost = Global.deployServerCost(minCostNode, maxOutput - leftOutput[minCostNode]);
					if (deployCost > installedCost[minCostNode]) {
						totalCost += deployCost - installedCost[minCostNode];
						installedCost[minCostNode] = deployCost;
						if (totalCost >= minCost) {
							return false;
						}
					}

					fromDemandSmaller = true;
					leftServerNodeNum--;
					if (consumerDemands[consumerId] == 0 || leftServerNodeNum == 0) {
						break;
					}
				}

			}

			// 更新
			for (int toNode : Global.connections[minCostNode]) {
				// 反向流量
				Edge edge = Global.graph[toNode][minCostNode];
				if (edge.leftBandWidth == 0) {
					continue;
				}
				int newCost = costs[minCostNode] + edge.cost;
				if (newCost < costs[toNode]) {
					costs[toNode] = newCost;
					// 添加路径
					preNodes[toNode] = minCostNode;
					if (!vis[toNode]) {
						vis[toNode] = true;
						queAdd(toNode);
					}
				}
			}
		}

		if (consumerDemands[consumerId] > 0 && fromDemandSmaller && leftServerNodeNum > 0) {
			return true;
		} else {
			return false;
		}
	}

	private final boolean[] vis = new boolean[Global.nodeNum];
	private final int[] costs = new int[Global.nodeNum];
	private final int[] preNodes = new int[Global.nodeNum];

	// 实现最小堆
	private final int[] queue = new int[Global.nodeNum];
	private int qSize = 0;

	/** 添加节点 */
	private final void queAdd(int x) {
		int k = qSize;
		qSize = k + 1;
		while (k > 0) {
			int parent = (k - 1) >>> 1;
			int e = queue[parent];
			if (costs[x] > costs[e])
				break;
			queue[k] = e;
			k = parent;
		}
		queue[k] = x;
	}

	/** 弹出节点 */
	private final int quePoll() {
		int s = --qSize;
		int result = queue[0];
		int x = queue[s];
		if (s != 0) {
			// 下沉操作
			int k = 0;
			int half = qSize >>> 1;
			while (k < half) {
				int child = (k << 1) + 1;
				int c = queue[child];
				int right = child + 1;
				if (right < qSize && costs[c] > costs[queue[right]])
					c = queue[child = right];
				if (costs[x] <= costs[c])
					break;
				queue[k] = c;
				k = child;
			}
			queue[k] = x;
		}
		return result;
	}

}
