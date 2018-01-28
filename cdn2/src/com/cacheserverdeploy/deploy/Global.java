package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * 全局参数，方便访问
 * 
 * @author mindw
 * @date 2017年3月11日
 */
public final class Global {

	/** 是否是调试 */
	static final boolean IS_DEBUG = true;

	/** 何时超时 */
	static final long TIME_OUT = System.currentTimeMillis() + 88 * 1000L;

	/** 是否困难 */
	static boolean isLarge;
	private static final int LARGE_THRESHOLD = 1000;

	/** 无穷大 */
	static final int INFINITY = Integer.MAX_VALUE;
	static final int SMALL_INFINITY = 1000000000;

	/** 最小费用 */
	static int minCost = INFINITY;

	/** 解决方案 */
	private static String[] bestSolution;
	private static int bestSolutionLength;

	public static String[] getBsetSolution() {
		String[] solution = new String[bestSolutionLength];
		System.arraycopy(bestSolution, 0, solution, 0, bestSolutionLength);
		return solution;
	}

	/** 服务器硬件档次ID */
	public static int serverLevelNum = 10;

	/** 最大输出能力 */
	public static int[] serverMaxOutputs = new int[10];

	/** 硬件成本 */
	public static int[] serverDeployCosts = new int[10];

	/** 一台服务器最大的输出能力 */
	static int maxServerOutput;

	/** 根据需求确定服务器的等级，如果无法满足，则返回-1等级 */
	static int decideServerLevel(int output) {
		for (int level = 0; level < serverLevelNum; ++level) {
			if (serverMaxOutputs[level] >= output) {
				return level;
			}
		}
		// error
		return -1;
	}

	/** 根据需求确定服务器的费用，如果无法满足，则返回较小的无穷大，防止增加溢出 */
	public static int deployServerCost(int serverNode, int output) {
		for (int level = 0; level < serverLevelNum; ++level) {
			if (serverMaxOutputs[level] >= output) {
				// 硬件费用 + 节点费用
				return serverDeployCosts[level] + nodeDeployCosts[serverNode];
			}
		}
		// error
		return SMALL_INFINITY;
	}

	/** 节点数:不超过10000个 ,从0开始 */
	static int nodeNum;

	/** 每个节点上的部署费用 */
	public static int[] nodeDeployCosts;

	/** 消费者数 */
	public static int consumerNum;
	/** 消费者所在的节点ID下标 */
	static int[] consumerNodes;
	public static int[] consumerDemands;
	/** 需求从大到小排序 */
	static int[] consumerIds;

	/** 消费者需求总和 */
	static int consumerTotalDemnad = 0;
	/** 网络节点ID - > 消费节点ID */
	static int[] nodeToConsumerId;

	/** 地图 */
	public static Edge[][] graph;
	/** 地图上的边 */
	public static Edge[] edges;
	/** 连接关系：下游节点 */
	static int[][] connections;

	/** 放置的服务器 ,List 用 数组替代，为空时表示数组结束 */
	private static Server[] bestServers;

	public static Server[] getBestServers() {
		return bestServers;
	}

	static void setBestServers(Server[] nextGlobalServers) {
		int size = 0;
		for (Server server : nextGlobalServers) {
			if (server == null) {
				break;
			}
			bestServers[size++] = server;
		}
		if (size < bestServers.length) {
			bestServers[size] = null;
		}
	}

	/** 初始化解：将服务器直接放在消费节点上 */
	public static void init(String[] graphContent) {

		// 多少个网络节点，多少条链路，多少个消费节点
		String[] line0 = graphContent[0].split(" ");
		/** 网络节点数：不超过1000个 */
		nodeNum = Integer.parseInt(line0[0]);
		nodeDeployCosts = new int[nodeNum];
		graph = new Edge[nodeNum][nodeNum];

		// 判断任务难易
		isLarge = (nodeNum >= LARGE_THRESHOLD);

		// 局部变量初始化
		visited = new boolean[nodeNum];
		costs = new int[nodeNum];
		queue = new int[nodeNum];

		/** 链路数：每个节点的链路数量不超过20条，推算出总共不超过20000 */
		int edgeNum = Integer.parseInt(line0[1]);

		/** 消费节点数：不超过500个 */
		consumerNum = Integer.parseInt(line0[2]);
		consumerNodes = new int[consumerNum];
		consumerDemands = new int[consumerNum];

		// 多个边加上超级汇点
		edges = new Edge[edgeNum << 1];

		// 空行

		// 0 70 50 服务器硬件档次ID为0，最大输出能力为70，硬件成本为50
		// 10级
		serverLevelNum = 10;
		serverMaxOutputs = new int[serverLevelNum];
		serverDeployCosts = new int[serverLevelNum];

		int lineIndex = 2;
		String line = null;
		while (!(line = graphContent[lineIndex++]).isEmpty()) {
			String[] strs = line.split(" ");
			int serverLevel = Integer.parseInt(strs[0]);
			int maxOutput = Integer.parseInt(strs[1]);
			int deployCost = Integer.parseInt(strs[2]);
			serverMaxOutputs[serverLevel] = maxOutput;
			serverDeployCosts[serverLevel] = deployCost;
		}

		// 空行

		// 网络节点ID为0，部署成本为50
		line = null;
		while (!(line = graphContent[lineIndex++]).isEmpty()) {
			String[] strs = line.split(" ");
			int node = Integer.parseInt(strs[0]);
			int nodeDeployCost = Integer.parseInt(strs[1]);
			nodeDeployCosts[node] = nodeDeployCost;
		}

		// 消费节点ID为0，相连网络节点ID为8，视频带宽消耗需求为40
		line = null;
		int edgeIndex = 0;
		while (!(line = graphContent[lineIndex++]).isEmpty()) {
			String[] strs = line.split(" ");

			// 链路起始节点
			int fromNode = Integer.parseInt(strs[0]);
			// 链路终止节点
			int toNode = Integer.parseInt(strs[1]);
			// 总带宽大小
			int bandwidth = Integer.parseInt(strs[2]);
			// 单位网络租用费
			int cost = Integer.parseInt(strs[3]);

			// 为每个方向上都建立一条边，创建过程中负责相关连接
			Edge goEdege = new Edge(bandwidth, cost, fromNode, toNode);
			edges[edgeIndex++] = goEdege;
			graph[fromNode][toNode] = goEdege;

			Edge backEdge = new Edge(bandwidth, cost, toNode, fromNode);
			edges[edgeIndex++] = backEdge;
			graph[toNode][fromNode] = backEdge;
		}

		// 空行

		// 对消费者进行排序
		final class ConsumerCost implements Comparable<ConsumerCost>{
			int consumerId;
			int demand;

			public ConsumerCost(int consumerId, int demand) {
				super();
				this.consumerId = consumerId;
				this.demand = demand;
			}

			@Override
			public int compareTo(ConsumerCost o) {
				// // 需求从大到小排序
				return o.demand - demand;
			}
		}
		ConsumerCost[] consumerCosts = new ConsumerCost[consumerNum];

		nodeToConsumerId = new int[nodeNum];
		// 消费节点，相连网络节点ID，视频带宽消耗需求
		for (int index = lineIndex; index < graphContent.length; ++index) {
			line = graphContent[index];
			String[] strs = line.split(" ");
			int consumerId = Integer.parseInt(strs[0]);
			int node = Integer.parseInt(strs[1]);
			int demand = Integer.parseInt(strs[2]);
			consumerNodes[consumerId] = node;
			consumerDemands[consumerId] = demand;
			consumerTotalDemnad += demand;

			nodeToConsumerId[node] = consumerId;

			consumerCosts[consumerId] = new ConsumerCost(consumerId, demand);
		}

		
		Arrays.sort(consumerCosts);
		consumerIds = new int[consumerNum];
		for (int i = 0; i < consumerNum; ++i) {
			consumerIds[i] = consumerCosts[i].consumerId;
		}

		 if(isLarge){
			 serverLevelNum = 10;
		 }else{
			 serverLevelNum = 10;
		 }
		maxServerOutput = serverMaxOutputs[serverLevelNum - 1];

		bestSolution = new String[300002];// 最多300000条
		// 默认无解
		bestSolutionLength = 0;
		bestSolution[bestSolutionLength++] = "NA";
		bestServers = new Server[nodeNum];

		// 初始连接关系
		connections = new int[nodeNum][];
		ArrayList<Integer> toNodes = new ArrayList<Integer>();
		for (int fromNode = 0; fromNode < nodeNum; ++fromNode) {
			toNodes.clear();
			for (int toNode = 0; toNode < nodeNum; ++toNode) {
				if (graph[fromNode][toNode] != null) {
					toNodes.add(toNode);
				}
			}
			connections[fromNode] = new int[toNodes.size()];
			for (int i = 0; i < toNodes.size(); ++i) {
				connections[fromNode][i] = toNodes.get(i);
			}
		}

		/** 初始化缓存 */
		initAllCostAndPreNodes();

		/** 对Connection 进行排序 */
		class NodeInfo implements Comparable<NodeInfo> {
			int node;
			int degree;

			public NodeInfo(int node, int degree) {
				super();
				this.node = node;
				this.degree = degree;
			}

			@Override
			public int compareTo(NodeInfo o) {
				return o.degree - degree;
			}
		}
		for (int node = 0; node < nodeNum; ++node) {
			int len = connections[node].length;
			NodeInfo[] infos = new NodeInfo[len];
			for (int i = 0; i < len; ++i) {
				int toNode = connections[node][i];
				infos[i] = new NodeInfo(toNode, connections[toNode].length);
			}
			Arrays.sort(infos);
			for (int i = 0; i < len; ++i) {
				connections[node][i] = infos[i].node;
			}
		}

		// if(IS_DEBUG){
		// System.out.println("总的需求："+consumerTotalDemnad+" 至少需要服务器数："+
		// consumerTotalDemnad / maxServerOutput);
		// }
	}

	/** 重置edge的带宽值 */
	static void resetEdgeBandWidth() {
		for (Edge edge : edges) {
			edge.leftBandWidth = edge.initBandWidth;
		}
	}

	/** 更新值 ，是否更好 */
	static boolean updateSolutionForce(Server[] nextGlobalServers) {

		int newMinCost = getTotalCost(nextGlobalServers);

		if (IS_DEBUG) {
			System.out.println("newMinCost:" + newMinCost);
		}

		if (newMinCost <= Global.minCost) {
			minCost = newMinCost;
			updateBestSolution(nextGlobalServers);
			setBestServers(nextGlobalServers);
			return true;
		} else {
			return false;
		}

	}

	private static StringBuilder stringBuilder = new StringBuilder();
	private static LinkedList<String> lines = new LinkedList<String>();

	/**
	 * 获得解决方案，格式如下:<br>
	 * <blockquote> 网络路径数量<br>
	 * （空行）<br>
	 * 网络节点ID-01 网络节点ID-02 …… 网络节点ID-n 消费节点ID 占用带宽大小<br>
	 * ……………. （文件结束）<br>
	 * <br>
	 * 每条网络路径由若干网络节点构成，路径的起始节点ID-01表示该节点部署了视频内容服务器，终止节点为某个消费节点<br>
	 * </blockquote>
	 */
	private static void updateBestSolution(Server[] servers) {
		lines.clear();
		for (Server server : servers) {
			if (server == null) {
				break;
			}
			server.getSolution(lines, stringBuilder);
		}

		bestSolutionLength = 0;
		bestSolution[bestSolutionLength++] = String.valueOf(lines.size());
		bestSolution[bestSolutionLength++] = "";
		for (String line : lines) {
			bestSolution[bestSolutionLength++] = line;
		}
	}

	/** 获得总的费用 */
	private static int getTotalCost(Server[] servers) {
		int toatlCost = 0;
		for (Server server : servers) {
			if (server == null) {
				break;
			}
			toatlCost += server.getCost();
		}
		return toatlCost;
	}

	///////////////////////////////////
	// 预先计算
	//////////////////////////////////

	/** 消费者到所有节点的费用 */
	/** 最小消耗 */
	static int[][] allCost;
	/** 最小消耗的前向指针 */
	static int[][] allPreNodes;
	/** 按节点的费用进行排序，从低到高，包括自己 */
	static int[][] allPriorityCost;

	private static void initAllCostAndPreNodes() {
		// 初始化本地缓存
		allCost = new int[consumerNum][nodeNum];
		allPreNodes = new int[consumerNum][nodeNum];
		allPriorityCost = new int[consumerNum][nodeNum];

		final class Node implements Comparable<Node> {
			int node;
			int cost;

			public Node(int node, int cost) {
				super();
				this.node = node;
				this.cost = cost;
			}

			@Override
			public int compareTo(Node o) {
				// 费用从小到大排
				return cost - o.cost;
			}
		}

		for (int consumerId = 0; consumerId < Global.consumerNum; ++consumerId) {
			Arrays.fill(allPreNodes[consumerId], -1);
			initOneCostAndPreNodes(consumerId);
			System.arraycopy(costs, 0, allCost[consumerId], 0, nodeNum);

			// 保存费用优先级
			int[] costs = allCost[consumerId];
			Node[] nodes = new Node[Global.nodeNum];
			for (int node = 0; node < Global.nodeNum; ++node) {
				nodes[node] = new Node(node, costs[node]);
				// 费用 = 转移费用 + 部署费用
				// (costs[node]*consumerDemands[consumerId]/4)+nodeDeployCosts[node]);
			}
			Arrays.sort(nodes);
			int index = 0;
			for (Node node : nodes) {
				allPriorityCost[consumerId][index++] = node.node;
			}

		}

	}

	private static boolean[] visited;

	private static void initOneCostAndPreNodes(int consumerId) {

		int[] preNodes = allPreNodes[consumerId];

		for (int i = 0; i < nodeNum; ++i) {
			visited[i] = false;
			costs[i] = SMALL_INFINITY;
		}

		int startNode = Global.consumerNodes[consumerId];
		costs[startNode] = 0;
		qSize = 0;
		queAdd(startNode);

		int minCostNode;
		while (qSize > 0) {

			// 寻找下一个最近点
			minCostNode = quePoll();
			visited[minCostNode] = false;

			// 更新
			for (int toNode : Global.connections[minCostNode]) {
				Edge edge = Global.graph[minCostNode][toNode];
				int newCost = costs[minCostNode] + edge.cost;
				if (newCost < costs[toNode]) {
					costs[toNode] = newCost;
					preNodes[toNode] = minCostNode;

					if (!visited[toNode]) {
						queAdd(toNode);
						visited[toNode] = true;
					}
				}
			}

		}
	}

	// >> 通用方法
	/**
	 * 消耗带宽最大带宽
	 * 
	 * @return 消耗掉的带宽
	 */
	static int useBandWidthByPreNode(int demand, int serverNode, int[] preNodes) {
		int node1 = serverNode;
		int node0 = preNodes[node1];

		int minBindWidth = INFINITY;
		while (node0 != -1) {
			Edge edge = graph[node1][node0];
			if (edge.leftBandWidth < minBindWidth) {
				minBindWidth = edge.leftBandWidth;
				if (minBindWidth == 0) {
					break;
				}
			}
			node1 = node0;
			node0 = preNodes[node0];
		}

		if (minBindWidth == 0) {
			return 0;
		}

		int usedBindWidth = Math.min(minBindWidth, demand);

		node1 = serverNode;
		node0 = preNodes[node1];
		while (node0 != -1) {
			Edge edge = graph[node1][node0];
			edge.leftBandWidth -= usedBindWidth;

			node1 = node0;
			node0 = preNodes[node0];
		}
		return usedBindWidth;
	}

	private static int[] costs;

	// 实现最小堆
	private static int[] queue;
	private static int qSize = 0;

	/** 添加节点 */
	private static final void queAdd(int x) {
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
	private static final int quePoll() {
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
