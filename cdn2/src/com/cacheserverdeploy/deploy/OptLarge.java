package com.cacheserverdeploy.deploy;

public final class OptLarge{
	
	/** 下一轮的服务器 */
	private final int[] serverNodes = new int[Global.nodeNum];
	/** 节点的输出 */
	private final int[] serverOutputs = new int[Global.nodeNum];
	/** 节点的等级 */
	private final int[] serverLevels = new int[Global.nodeNum];
	/** 节点的数目 */
	private int serverNodesSize;

	/** 在返回之前更新全局的服务器信息 */
	private void updateBeforeReturn(int cost) {
		if(cost>=Global.minCost){
			return; 
		}
		
		Global.minCost = cost;
		Server[] servers = new Server[serverNodesSize];
		for (int i = 0; i < serverNodesSize; ++i) {
			servers[i] = new Server(serverNodes[i]);
			servers[i].ouput = serverOutputs[i];
		}
		Global.setBestServers(servers);
	}
	
	private final int maxServerOutput = Global.maxServerOutput;;

	private final int MAX_UPDATE_NUM;
	
	private int minCost;
	
	void optimize() {
		
		long t;
		if (Global.IS_DEBUG) {
			t = System.currentTimeMillis();
			System.out.println("\n"+this.getClass().getSimpleName() + " 开始接管 ");
		}
		
		// 设置服务器
		serverNodesSize = 0;
		for (Server server : Global.getBestServers()) {
			if (server == null) {
				break;
			}
			serverNodes[serverNodesSize] = server.node;
			serverLevels[serverNodesSize] = Global.decideServerLevel(server.ouput);
			serverNodesSize++;
		}

		minCost = Global.minCost;
		
		int lastToNode = -1;
		int updateNum;
		boolean found;
		int fromNode;
		int cost;
		int serverLevel;
		
		while (System.currentTimeMillis() < Global.TIME_OUT) {

			// 可选方案
			updateNum = 0;
			found = false;

			for (serverLevel = Global.serverLevelNum-1; serverLevel >= 0; --serverLevel) {
				
				for (int i = 0; i < serverNodesSize; ++i) {
				
					fromNode = serverNodes[i];
					if (fromNode == lastToNode) {
						continue;
					}
					
					for(int toNode : Global.connections[fromNode]){	

						if (System.currentTimeMillis() >= Global.TIME_OUT) {
							updateBeforeReturn(minCost);
							if (Global.IS_DEBUG) {
								System.out.println(this.getClass().getSimpleName() + " 结束，耗时: " + (System.currentTimeMillis() - t));
							}
							return;
						}

						cost = costAfterMove(fromNode, toNode, serverLevel);
						if(cost!=Global.INFINITY){ // 表示成功
							minCost = cost;
							lastToNode = toNode;
							if (Global.IS_DEBUG) {
								System.out.println(minCost);
							}
							updateNum++;
							if (updateNum == MAX_UPDATE_NUM) {
								found = true;
							}
							break;
						}
						
					}
					
					if (found) {
						break;
					}
	
				}
		
				if (found) {
					break;
				}
			}
			
		}

		updateBeforeReturn(minCost);
		
		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: " + (System.currentTimeMillis() - t));
		}

	}

	
	/** 剩余的输出 */
	private final int[] leftOutput = new int[Global.nodeNum];

	private final int[] initOutput = new int[Global.nodeNum];

	
	private final int costAfterMove(int fromNode, int toNode, int toLevel) {
		
		for (int i = 0; i < Global.nodeNum; ++i) {
			initOutput[i] = maxServerOutput;
			leftOutput[i] = maxServerOutput;
		}

		// 1 与超级源点相连的重置
		for (ZkwEdge edge : edges) {
			edge.cost = edge.initCost;
			edge.leftBandWidth = edge.initBandWidth;
		}

		// 2 设置与超级源点相连的节点
		for (int i = 0; i < serverNodesSize; ++i) {
			int serverNode = serverNodes[i];
			if (serverNode != fromNode) {
				// 重置
				int maxOutput = Global.serverMaxOutputs[serverLevels[i]];
				
				serverEdges[serverNode].leftBandWidth = maxOutput;
				initOutput[serverNode] = maxOutput;
				leftOutput[serverNode] = maxOutput;
			}
		}

		// 重置
		int maxOutput = Global.serverMaxOutputs[toLevel];
		serverEdges[toNode].leftBandWidth = maxOutput;
		initOutput[toNode] = maxOutput;
		leftOutput[toNode] = maxOutput;

		cost = 0;
		pi1 = 0;
		flow = 0;
		
		do {
			do {
				for(int i=0;i<vis.length;++i){
					vis[i] = false;
				}
			} while (aug(sourceNode, Global.INFINITY) > 0);
		} while (modlabel());
		
		

		if (flow < Global.consumerTotalDemnad) {
			cost = Global.INFINITY;
		}else{
			
			for (int node = 0; node < Global.nodeNum; ++node) {
				if (leftOutput[node] < initOutput[node]) {
					cost += Global.deployServerCost(node, initOutput[node] - leftOutput[node]);
				}
			}
			
			if(cost<minCost){
				// 合理移动
				serverNodesSize = 0;
				int output,node;
				for (int i = Global.nodeNum-1; i >=0 ; --i) {
					node = nodes[i];
					if (leftOutput[node] < initOutput[node]) {
						output = initOutput[node] - leftOutput[node];
						serverNodes[serverNodesSize] = node;
						serverOutputs[serverNodesSize] = output;
						serverLevels[serverNodesSize] = Global.decideServerLevel(output);
						serverNodesSize++;
					}
				}
			}else{
				cost = Global.INFINITY;
			}
		
		}
		return cost;
	}

	
	////////////////////////

	private final int[] nodes;
	public OptLarge(int[] nodes, int maxUpdateNum) {
		
		this.MAX_UPDATE_NUM = maxUpdateNum;
		this.nodes = nodes;

		int nodeNum = Global.nodeNum;

		// 多源多汇问题，定义一个超级源点s,一个超级汇点e
		sourceNode = nodeNum;
		endNode = nodeNum + 1;
		maxn = nodeNum + 2;

		// 多个边加上超级汇点
		// 最多多少条边
		int maxm = Global.edges.length + Global.consumerNum + nodeNum;

		edges = new ZkwEdge[maxm << 1];
		vis = new boolean[maxn];
		head = new ZkwEdge[maxn];

		// 消费节点ID为0，相连网络节点ID为8，视频带宽消耗需求为40
		for (Edge edge : Global.edges) {
			addEdge(edge.from, edge.to, edge.initBandWidth, edge.cost);
		}

		// 消费节点，相连网络节点ID，视频带宽消耗需求
		for (int i = 0; i < Global.consumerNum; ++i) {
			// 定义消费节点到超级汇点的带宽 消费节点的需求带宽
			addEdge(Global.consumerNodes[i], endNode, Global.consumerDemands[i], 0);
		}

		// 链接超级源
		for (int node = 0; node < Global.nodeNum; node++) {
			int bandwidth = 0;
			int cost = 0;
			addSourceEdge(node, bandwidth, cost);
		}
	}

	private final ZkwEdge[] head;
	private final ZkwEdge[] edges;

	private final class ZkwEdge {

		final int toNode;

		private final int initCost;
		int cost;

		private final int initBandWidth;
		int leftBandWidth;

		ZkwEdge next;
		ZkwEdge pair;

		public ZkwEdge(int toNode, int cost, int bandwidth) {
			super();
			this.toNode = toNode;
			this.initCost = cost;
			this.cost = cost;
			this.initBandWidth = bandwidth;
			this.leftBandWidth = bandwidth;
		}

	}

	private final int maxn;
	private final boolean[] vis;
	private int edgeIndex = 0;

	private final int sourceNode;
	private final int endNode;

	private final ZkwEdge[] serverEdges = new ZkwEdge[Global.nodeNum];

	private void addSourceEdge(int toNode, int bandwidth, int cost) {

		ZkwEdge toEdge = new ZkwEdge(toNode, cost, bandwidth);
		serverEdges[toNode] = toEdge;

		ZkwEdge backEdge = new ZkwEdge(sourceNode, -cost, 0);

		edges[edgeIndex++] = toEdge;
		edges[edgeIndex++] = backEdge;

		toEdge.next = head[sourceNode];
		head[sourceNode] = toEdge;

		backEdge.next = head[toNode];
		head[toNode] = backEdge;

		toEdge.pair = backEdge;
		backEdge.pair = toEdge;
	}

	private void addEdge(int fromNode, int toNode, int bandwidth, int cost) {

		ZkwEdge toEdge = new ZkwEdge(toNode, cost, bandwidth);
		ZkwEdge backEdge = new ZkwEdge(fromNode, -cost, 0);

		edges[edgeIndex++] = toEdge;
		edges[edgeIndex++] = backEdge;

		toEdge.next = head[fromNode];
		head[fromNode] = toEdge;

		backEdge.next = head[toNode];
		head[toNode] = backEdge;

		toEdge.pair = backEdge;
		backEdge.pair = toEdge;
	}

	private int cost;
	private int pi1;
	private int flow;
	private int serverNode;

	private int aug(int node, int bandwidth) {

		if (node == endNode) {
			leftOutput[serverNode] -= bandwidth;
			cost += pi1 * bandwidth;
			flow += bandwidth;
			return bandwidth;
		}

		vis[node] = true;
		int leftBandwidth = bandwidth;

		for (ZkwEdge edge = head[node]; edge != null; edge = edge.next) {
			if (edge.leftBandWidth != 0 && edge.cost == 0 && !vis[edge.toNode]) {

				if (node == sourceNode) {
					serverNode = edge.toNode;
				}

				int d = aug(edge.toNode, leftBandwidth < edge.leftBandWidth ? leftBandwidth : edge.leftBandWidth);
				
				edge.leftBandWidth -= d;
				edge.pair.leftBandWidth += d;
				leftBandwidth -= d;
				if (leftBandwidth == 0) {
					return bandwidth;
				}
			}
		}
		return bandwidth - leftBandwidth;
	}

	private boolean modlabel() {
		int minCost = Global.INFINITY;
		for (int node = 0; node < maxn; ++node) {
			if (vis[node]) {
				for (ZkwEdge edge = head[node]; edge != null; edge = edge.next) {
					if (edge.leftBandWidth != 0 && !vis[edge.toNode] && edge.cost < minCost) {
						minCost = edge.cost;
					}
				}
			}
		}

		if (minCost == Global.INFINITY) {
			return false;
		}

		for (int node = 0; node < maxn; ++node) {
			if (vis[node]) {
				for (ZkwEdge edge = head[node]; edge != null; edge = edge.next) {
					edge.cost -= minCost;
					edge.pair.cost += minCost;
				}
			}
		}
		pi1 += minCost;
		return true;
	}

}
