package com.cacheserverdeploy.deploy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 利用最大最小费用流进行优化
 *
 * @author mindw
 * @date 2017年3月23日
 */
public final class OptMCMF {

	private final class McmfEdge {
		int v;
		int cap;
		int cost;
		int flow;
		int next;
	}

	private final int maxn;
	private final int inf = 1000000000;
	private int edgeIndex = 0;
	private final McmfEdge[] edges;
	private int sumFlow;
	private final int[] head;
	private final int[] dis;
	private final int[] pre;
	private final boolean[] vis;

	private final int sourceNode;
	private final int endNode;

	public OptMCMF() {
		
		int nodeNum = Global.nodeNum;

		// 多源多汇问题，定义一个超级源点s,一个超级汇点e
		sourceNode = nodeNum;
		endNode = nodeNum + 1;
		maxn = nodeNum + 2;

		// 多个边加上超级汇点
		// 最多多少条边
		int maxm = Global.edges.length + Global.consumerNum + nodeNum;

		edges = new McmfEdge[maxm << 1];
		dis = new int[maxn];
		pre = new int[maxn];
		vis = new boolean[maxn];
		head = new int[maxn << 1];
		que = new int[maxn];
		Arrays.fill(head, -1);

		/* dfs */
		visDFS = new boolean[maxm << 1];
		visEdge = new int[maxm << 1];

		// 空行
		for (Edge edge : Global.edges) {
			addEdge(edge.from, edge.to, edge.initBandWidth, edge.cost);
		}

		// 消费节点，相连网络节点ID，视频带宽消耗需求
		for (int i = 0; i < Global.consumerNum; ++i) {
			// 定义消费节点到超级汇点的带宽 消费节点的需求带宽
			addEdge(Global.consumerNodes[i], endNode, Global.consumerDemands[i], 0);
		}

		for (int node = 0; node < Global.nodeNum; node++) {
			resetSourceEdge(node);
		}
	}
	
	private List<ServerInfo> serverInfos = new LinkedList<ServerInfo>();

	/** 优化全局的 */
	final void optimizeGlobalBest() {

		if (Global.IS_DEBUG) {
			System.out.println("\n"+this.getClass().getSimpleName() + " 开始接管 ");
		}
		
		long t = System.currentTimeMillis();
		
		// 与超级源点相连的重置
		for (int i = head[sourceNode]; i != -1; i = edges[i].next) {
			edges[i].cap = 0;
			edges[i].cost = inf;
		}
	
		for (Server server : Global.getBestServers()) {
			if (server == null) {
				break;
			}
			serverEdges[server.node].cap = server.ouput;
			serverEdges[server.node].cost = 0;
		}

		// 重置边的流
		for (int i = 0; i < edgeIndex; i++) {
			edges[i].flow = 0;
		}

		minCostMaxFlow(sourceNode, endNode, maxn);

		if (sumFlow >= Global.consumerTotalDemnad) {

			Arrays.fill(visEdge, -3);
			serverInfos.clear();
			DFS(sourceNode, endNode, maxn);

			Map<Integer, Server> newServers = new HashMap<Integer, Server>();
			for (ServerInfo serverInfo : serverInfos) {
				int serverNode = serverInfo.viaNodes[serverInfo.viaNodes.length - 1];
				if (!newServers.containsKey(serverNode)) {
					newServers.put(serverNode, new Server(serverNode));
				}
				newServers.get(serverNode).addServerInfo(serverInfo);
				newServers.get(serverNode).ouput += serverInfo.provideBandWidth;
			}
			Server[] nextGlobalServers = new Server[newServers.size()];
			int size = 0;
			for (Server newServer : newServers.values()) {
				nextGlobalServers[size++] = newServer;
			}
			Global.updateSolutionForce(nextGlobalServers);

		} else {
			if (Global.IS_DEBUG) {
				System.out.println("mcmf 无法找到一个满足的解！");
			}
		}

		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: " + (System.currentTimeMillis() - t));
		}

	}

	private final McmfEdge[] serverEdges = new McmfEdge[Global.nodeNum];

	private final void resetSourceEdge(int v) {
		edges[edgeIndex] = new McmfEdge();
		serverEdges[v] = edges[edgeIndex];

		edges[edgeIndex].v = v;
		edges[edgeIndex].cap = 0;
		edges[edgeIndex].cost = inf;
		edges[edgeIndex].flow = 0;
		edges[edgeIndex].next = head[sourceNode];
		head[sourceNode] = edgeIndex++;

		edges[edgeIndex] = new McmfEdge();
		edges[edgeIndex].v = sourceNode;
		edges[edgeIndex].cap = 0;
		edges[edgeIndex].flow = 0;
		edges[edgeIndex].cost = 0;
		edges[edgeIndex].next = head[v];
		head[v] = edgeIndex++;
	}
	
	private void addEdge(int u, int v, int cap, int cost) {
		edges[edgeIndex] = new McmfEdge();
		edges[edgeIndex].v = v;
		edges[edgeIndex].cap = cap;
		edges[edgeIndex].cost = cost;
		edges[edgeIndex].flow = 0;
		edges[edgeIndex].next = head[u];
		head[u] = edgeIndex++;
		edges[edgeIndex] = new McmfEdge();
		edges[edgeIndex].v = u;
		edges[edgeIndex].cap = 0;
		edges[edgeIndex].flow = 0;
		edges[edgeIndex].cost = -cost;
		edges[edgeIndex].next = head[v];
		head[v] = edgeIndex++;
	}

	private boolean[] visDFS;
	private int[] visEdge;
	private int visCount = 0;
	private int MINFLOW = inf;

	private final int[] nodes = new int[Global.nodeNum];

	private final void DFS(int s, int t, int n) {

		if (s == t) {
			MINFLOW = inf;
			for (int visedge : visEdge) {
				if (visedge < 0) {
					break;
				}
				MINFLOW = Math.min(MINFLOW, edges[visedge].flow);
			}
			if (MINFLOW <= 0) {
				return;
			}
			int nodeSize = 0;
			for (int visedge : visEdge) {
				if (visedge < 0) {
					break;
				}
				edges[visedge].flow -= MINFLOW;
				edges[visedge ^ 1].flow += MINFLOW;
				nodes[nodeSize++] = edges[visedge].v;
			}
			// 保存服务信息,从消费者开始的 -> 服务器

			// 去头 end,node0,node1,....
			nodeSize--;
			int k = 0;
			int[] viaNodes = new int[nodeSize];
			// nodeSize--;
			for (int j = nodeSize - 1; j >= 0; j--) {
				viaNodes[k++] = nodes[j];
			}
			int consumerId = Global.nodeToConsumerId[viaNodes[0]];
			ServerInfo serverInfo = new ServerInfo(consumerId, MINFLOW, viaNodes);
			serverInfos.add(serverInfo);

			return;
		}
		for (int i = head[s]; i != -1; i = edges[i].next) {
			if (edges[i].v != sourceNode && edges[i].flow > 0 && !visDFS[i]) {
				visDFS[i] = true;
				visEdge[visCount++] = i;
				DFS(edges[i].v, t, n);
				visEdge[--visCount] = -3;
				visDFS[i] = false;
			}
		}

	}

	private final int minCostMaxFlow(int s, int t, int n) {
		sumFlow = 0; // 总流量
		int minflow;
		int mincost = 0;
		while (spfa(s, t, n)) {
			minflow = inf + 1;
			for (int i = pre[t]; i != -1; i = pre[edges[i ^ 1].v])
				if ((edges[i].cap - edges[i].flow) < minflow)
					minflow = (edges[i].cap - edges[i].flow);
			sumFlow += minflow;
			for (int i = pre[t]; i != -1; i = pre[edges[i ^ 1].v]) {
				edges[i].flow += minflow;
				edges[i ^ 1].flow -= minflow;
			}
			mincost += dis[t] * minflow;

		}
		return mincost;
	}

	// 数组模拟循环队列，当前后指针在同一个位置上的时候才算结束（开始不包括）
	private final int[] que;
	private int qHead; // 指向队首位置
	private int qTail; // 指向下一个插入的位置

	private final boolean spfa(int s, int t, int n) {
		int u, v;

		// que.clear()
		qHead = 0;
		qTail = 0;

		for (int i = 0; i < vis.length; ++i) {
			vis[i] = false;
		}
		pre[s] = -1;

		for (int i = 0; i < n; ++i) {
			dis[i] = inf;
		}
		vis[s] = true;
		dis[s] = 0;

		que[qTail++] = sourceNode;

		while (qHead != qTail) {
			// u = que.poll();
			u = que[qHead++];
			if (qHead == que.length) {
				qHead = 0;
			}

			vis[u] = false;
			for (int i = head[u]; i != -1; i = edges[i].next) {
				if (edges[i].cap <= edges[i].flow) {
					continue;
				}
				v = edges[i].v;
				if (dis[v] > dis[u] + edges[i].cost) {
					dis[v] = dis[u] + edges[i].cost;
					pre[v] = i;
					if (!vis[v]) {
						int insertNode = v;

						// 队伍不空，比头部小
						if (qHead != qTail && dis[v] < dis[que[qHead]]) {
							insertNode = que[qHead];
							que[qHead] = v;
						}

						que[qTail++] = insertNode;
						if (qTail == que.length) {
							qTail = 0;
						}

						vis[v] = true;
					}
				}
			}
		}
		if (dis[t] == inf)
			return false;
		return true;
	}

}
