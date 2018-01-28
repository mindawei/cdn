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

	/** 开始时间 */
	static long startTime;
	/** 何时超时 */
	static long TIME_OUT_OF_NORMAL;
	static long TIME_OUT_OF_MCMF;

	/** 无穷大 */
	static final int INFINITY = Integer.MAX_VALUE;
	static final int SMALL_INFINITY = 1000000000;

	/** 解决方案 */
	private static String[] bestSolution;
	private static int bestSolutionLength;

	public static String[] getBsetSolution() {
		String[] solution = new String[bestSolutionLength];
		System.arraycopy(bestSolution, 0, solution, 0, bestSolutionLength);
		return solution;
	}

	/** 服务器硬件档次ID */
	public static int serverLevelNum;
	/** 最大档次 */
	public static int maxServerLevel;
	
	/** 最大输出能力 */
	public static int[] serverMaxOutputs;

	/** 硬件成本 */
	public static int[] serverDeployCosts;

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

	/** 排序后的服务器位置 */
	static int[] nodes;
	
	/** 消费者能流入的最大流量 */
	static int[] nodesMaxInputBandWidth;

	/** 消费者数 */
	public static int consumerNum;
	/** 消费者所在的节点ID下标 */
	static int[] consumerNodes;
	public static int[] consumerDemands;
	
	/** 消费者能流入的最大流量 */
	static int[] consumerMaxInputBandWidth;
	
	/** 消费者节点提供消费者的队伍ID */
	static int[] consumerTeamIds;

	/** 网络节点ID - > 消费节点ID */
	static int[] nodeToConsumerId;

	/** 地图 */
	public static Edge[][] graph;
	/** 地图上的边 */
	public static Edge[] edges;
	/** 连接关系：下游节点 */
	static int[][] connections;

	/** 本队伍的ID */
	static int teamID;

	/** 没有队伍的ID */
	static final int emptyTeamID = 0;

	/** 剩余金额 */
	static int leftMoney;

	/** 消费节点每轮视频服务费 */
	static int consumerPayPerRound;

	/** 是否是第一次初始化 */
	private static boolean isFirst = true;

	/** 10s一轮，共60轮 */
	static int round = 0;
	
	static boolean isFirstRound(){
		return round == 1;
	}
	
	static int  leftRound(){
		return 60 - round + 1;
	}
	
	/** 对手赚的钱比我多多少 */
	static int moneyOff = 0; 

	private static StringBuilder sb = new StringBuilder();

	public static void deal(String[] graphContent) {
		if (isFirst) {
			init(graphContent);
			// 第一轮多输出2个单位
//			for(int i=0;i<consumerNum;++i){
//				consumerDemands[i]+=2;
//			}			
			// leftMoney /= 2; // 第一次保留1/2
			moneyOff+=leftMoney; // 目标超过初始资金
			isFirst = false;
		} else {
			update(graphContent);
		}
	}
	
	static boolean betterThanHe = false;

	/** 这一轮多少个选择我 */
	static int selectMe = 0;
	/** 这一轮多少个选择他 */
	static int selectHe = 0;
	
	/** 初始化解：将服务器直接放在消费节点上 */
	public static void update(String[] graphContent) {
		
		sb.setLength(0);
		sb.append("selectInfo:\n");
		
		String line = null;

		selectMe = 0;
		selectHe = 0;
		// 消费节点ID 直接相连的网络节点ID 最低带宽消耗需求 该节点选择的队伍ID
		for (int index = consumerLineIndex; index < graphContent.length - 2; ++index) {
			line = graphContent[index];
			String[] strs = line.split(" ");
			int consumerId = Integer.parseInt(strs[0]);
			int selectedTeamId = Integer.parseInt(strs[3]);
			sb.append(consumerId + " selected " + selectedTeamId+"\n");
			
			if(selectedTeamId==teamID){
				selectMe++;
			}else if(selectedTeamId!=emptyTeamID){
				selectHe++;
			}
			
			consumerTeamIds[consumerId] = selectedTeamId;
		}
		
		sb.append("round:"+round+" me:"+selectMe+" he:"+selectHe);
		
		/** 差距 */
		moneyOff += (selectHe-selectMe)*consumerPayPerRound;
		
		// 40 轮之后
		if(round>=40 && leftRound() *(selectMe-selectHe)*consumerPayPerRound > moneyOff){
			betterThanHe = true;
		}else{
			betterThanHe = false;
		}
		
//		LogUtil.printLog("better: "+betterThanHe+" moneyOff:"+moneyOff);
//		LogUtil.printLog(sb.toString());

		line = graphContent[graphContent.length - 1];
		String[] strs = line.split(" ");
		leftMoney = Integer.parseInt(strs[1]);
	}

	private static int consumerLineIndex;

	/** 初始化解：将服务器直接放在消费节点上 */
	public static void init(String[] graphContent) {
		
//		sb.setLength(0);
//		for(String str : graphContent){
//			sb.append(str+"\n");
//		}
//		sb.append("round:"+1);
//		LogUtil.printLog(sb.toString());
		
		String[] line0 = graphContent[0].split(" ");
		nodeNum = Integer.parseInt(line0[0]);
		nodeDeployCosts = new int[nodeNum];
		nodeToConsumerId = new int[nodeNum];
		graph = new Edge[nodeNum][nodeNum];
		nodesMaxInputBandWidth = new int[nodeNum];
		
		// 局部变量初始化
		visited = new boolean[nodeNum];
		costs = new int[nodeNum];
		queue = new int[nodeNum];

		int edgeNum = Integer.parseInt(line0[1]);
		edges = new Edge[edgeNum << 1];

		// 消费节点数
		consumerNum = Integer.parseInt(line0[2]);
		consumerNodes = new int[consumerNum];
		consumerDemands = new int[consumerNum];
		consumerTeamIds = new int[consumerNum];
		consumerMaxInputBandWidth = new int[consumerNum];

		/** 参数队伍数量 */
		// teamNum = Integer.parseInt(line0[3]);

		// 空行

		// 0 70 50 服务器硬件档次ID为0，最大输出能力为70，硬件成本为50
		// 10级
		serverLevelNum = 10;
		maxServerLevel = 9;
		serverMaxOutputs = new int[serverLevelNum];
		serverDeployCosts = new int[serverLevelNum];

		// 消费节点每轮视频服务费
		consumerPayPerRound = Integer.parseInt(graphContent[2]);

		// 视频内容服务器档次ID 输出能力 硬件成本
		int lineIndex = 4;
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

		// 网络节点ID 部署成本
		line = null;
		while (!(line = graphContent[lineIndex++]).isEmpty()) {
			String[] strs = line.split(" ");
			int node = Integer.parseInt(strs[0]);
			int nodeDeployCost = Integer.parseInt(strs[1]);
			nodeDeployCosts[node] = nodeDeployCost;

		}

		// 链路起始节点ID 链路终止节点ID 总带宽大小 每轮单位网络租用费
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

		// 消费节点ID 直接相连的网络节点ID 最低带宽消耗需求 该节点选择的队伍ID
		consumerLineIndex = lineIndex;
		for (int index = lineIndex; index < graphContent.length - 2; ++index) {
			line = graphContent[index];
			String[] strs = line.split(" ");
			int consumerId = Integer.parseInt(strs[0]);
			int node = Integer.parseInt(strs[1]);
			int demand = Integer.parseInt(strs[2]);
			consumerNodes[consumerId] = node;
			consumerDemands[consumerId] = demand;
			nodeToConsumerId[node] = consumerId;

		}

		line = graphContent[graphContent.length - 1];
		String[] strs = line.split(" ");
		teamID = Integer.parseInt(strs[0]);
		leftMoney = Integer.parseInt(strs[1]);
	
		maxServerOutput = serverMaxOutputs[serverLevelNum - 1];

		bestSolution = new String[300002];// 最多300000条
		// 默认无解
		bestSolutionLength = 0;
		bestSolution[bestSolutionLength++] = "NA";

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

		// 节点最大输出能力
		for(int node=0;node<nodeNum;++node){
			nodesMaxInputBandWidth[node] =0;
			for(int toNode : connections[node]){
				nodesMaxInputBandWidth[node] += graph[toNode][node].initBandWidth;
			}
		}
		
		// 消费者的最大流入流量
		for(int consumerId=0;consumerId<consumerNum;++consumerId){
			int consumerNode = consumerNodes[consumerId];
			consumerMaxInputBandWidth[consumerId] = nodesMaxInputBandWidth[consumerNode];	
		}
		
//		long t = System.currentTimeMillis();
		/** 初始化缓存 */
		initAllCostAndPreNodes();
//		LogUtil.printLog("initAllCostAndPreNodes use time:"+(System.currentTimeMillis()-t));
		
		nodes = new NodesSelector().select();
	}

	/** 重置edge的带宽值 */
	static void resetEdgeBandWidth() {
		for (Edge edge : edges) {
			edge.leftBandWidth = edge.initBandWidth;
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
	static void updateBestSolution(Server[] servers) {
		lines.clear();
		for (Server server : servers) {
			if (server == null) {
				continue;
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

	static void useBandWidthDirectly(int minBindWidth, int serverNode, int[] preNodes) {
		int node1 = serverNode;
		int node0 = preNodes[node1];

		node1 = serverNode;
		node0 = preNodes[node1];
		while (node0 != -1) {
			Edge edge = graph[node1][node0];
			edge.leftBandWidth -= minBindWidth;

			node1 = node0;
			node0 = preNodes[node0];
		}
	}

	static int getBandWidthCanbeUsed(int demand, int serverNode, int[] preNodes) {
		int node1 = serverNode;
		int node0 = preNodes[node1];

		int minBindWidth = demand;
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
		return minBindWidth;
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
