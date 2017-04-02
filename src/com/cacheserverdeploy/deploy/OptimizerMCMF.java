package com.cacheserverdeploy.deploy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * 利用最大最小费用流进行优化
 *
 * @author mindw
 * @date 2017年3月23日
 */
public final class OptimizerMCMF {

	private final class McmfEdge {
		int u;
		int v;
		int cap;
		int cost;
		int flow;
		int next;
		@Override
		public String toString() {
			return "Edge [u=" + u + ",v=" + v + ", cap=" + cap + ", cost=" + cost + ",flow=" + flow + ", next=" + next
					+ "]";
		}

	}

	//// 最小费用最大流模版.求最大费用最大流建图时把费用取负即可。
	//// 无向边转换成有向边时需要拆分成两条有向边。即两次加边。
	// 最多节点数
	private final int maxn;
	private final int inf = 1000000000;
	private int edgeIndex = 0;
	private final McmfEdge[] p;
	private int sumFlow;
	private int[] head;
	private int[] dis;
	private int[] pre;
	private boolean[] vis;

	private int nodeNum;
	private int sourceNode;
	private int endNode;

	public OptimizerMCMF(String[] graphContent) {

		// 多少个网络节点，多少条链路，多少个消费节点
		String[] line0 = graphContent[0].split(" ");
		/** 网络节点数：不超过1000个 */
		nodeNum = Integer.parseInt(line0[0]);

		// 多源多汇问题，定义一个超级源点s,一个超级汇点e
		sourceNode = nodeNum;
		endNode = nodeNum + 1;
		maxn = nodeNum + 2;

		/** 链路数：每个节点的链路数量不超过20条，推算出总共不超过20000 */
		int edgeNum = Integer.parseInt(line0[1]);

		/** 消费节点数：不超过500个 */
		int consumerNum = Integer.parseInt(line0[2]);

		// 多个边加上超级汇点
		// 最多多少条边
		int maxm = edgeNum * 2 + consumerNum + nodeNum;

		p = new McmfEdge[maxm << 1];
		dis = new int[maxn];
		pre = new int[maxn];
		vis = new boolean[maxn];
		head = new int[maxn << 1];
		Arrays.fill(head, -1);

		/* dfs */
		visDFS = new boolean[maxm << 1];
		visEdge = new int[maxm << 1];

		int lineIndex = 4;
		String line = null;
		// 每行：链路起始节点ID 链路终止节点ID 总带宽大小 单位网络租用费
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
			addEdge(fromNode, toNode, bandwidth, cost);
			addEdge(toNode, fromNode, bandwidth, cost);
		}

		// 消费节点，相连网络节点ID，视频带宽消耗需求
		for (int index = lineIndex; index < graphContent.length; ++index) {
			line = graphContent[index];
			String[] strs = line.split(" ");
			int node = Integer.parseInt(strs[1]);
			int demand = Integer.parseInt(strs[2]);
			// 定义消费节点到超级汇点的带宽 消费节点的需求带宽
			addEdge(node, endNode, demand, 0);
		}

		for (int index = 0; index < Global.nodeNum; index++) {
			resetSourceEdge(sourceNode, index);
		}
	}

	final void optimizeCASE(int[] lsNewServers, int lsNewServersSize) {
		
		Global.resetEdgeBandWidth();
	
		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}

		long t = System.currentTimeMillis();

		for (int i = head[sourceNode]; i != -1; i = p[i].next) {
			p[i].cap = 0;
			p[i].cost = inf;
		}
		int serverSize = 0;
		for (int index =0;index <lsNewServersSize;++index) {
			int serverNode = lsNewServers[index];
			
			serverSize++;
			for (int i = head[sourceNode]; i != -1; i = p[i].next) {
				if (serverNode == p[i].v) {
					p[i].cap = inf;
					p[i].cost = 0;
					break;
				}
			}
		}
		resetEdge();

		int cost = MCMF(sourceNode, endNode, maxn) + serverSize * Global.depolyCostPerServer;

		if (Global.IS_DEBUG) {
			System.out.println("cost:" + cost);
			System.out.println("sumFlow:" + sumFlow + " consumerTotalDemnad:" + Global.consumerTotalDemnad);
		}

		Arrays.fill(visEdge, -3);
		serverInfos.clear();
		DFS(sourceNode, endNode, maxn);

		if (sumFlow >= Global.consumerTotalDemnad) {

			Map<Integer, Server> newServers = new HashMap<Integer, Server>();
			for (ServerInfo serverInfo : serverInfos) {
				int serverNode = serverInfo.viaNodes[serverInfo.viaNodes.length - 1];
				if (!newServers.containsKey(serverNode)) {
					newServers.put(serverNode, new Server(serverNode));
				}
				newServers.get(serverNode).addServerInfo(serverInfo);
			}
			cost = cost + newServers.size() * Global.depolyCostPerServer;
			Server[] nextGlobalServers = new Server[newServers.size()];
			int size = 0;
			for (Server newServer : newServers.values()) {
				nextGlobalServers[size++] = newServer;
			}
			Global.updateSolution(nextGlobalServers);

		} else {

			if (Global.IS_DEBUG) {
				System.out.println("mcmf 无法找到一个满足的解！");
			}

		}

		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: " + (System.currentTimeMillis() - t));
		}

	}

	void optimize() {

		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}

		long t = System.currentTimeMillis();

		for (int i = head[sourceNode]; i != -1; i = p[i].next) {
			p[i].cap = 0;
			p[i].cost = inf;
		}

		for (Server server : Global.getBestServers()) {
			if (server == null) {
				break;
			}
			for (int i = head[sourceNode]; i != -1; i = p[i].next) {
				if (server.node == p[i].v) {
					p[i].cap = inf;
					p[i].cost = 0;
					break;
				}
			}
		}
		resetEdge();
		
		int cost = MCMF(sourceNode, endNode, maxn);

		if (Global.IS_DEBUG) {
			System.out.println("cost:" + cost);
			System.out.println("sumFlow:" + sumFlow + " consumerTotalDemnad:" + Global.consumerTotalDemnad);
		}

		Arrays.fill(visEdge, -3);
		serverInfos.clear();
		DFS(sourceNode, endNode, maxn);
	

		if (sumFlow >= Global.consumerTotalDemnad) {

			Map<Integer, Server> newServers = new HashMap<Integer, Server>();
			for (ServerInfo serverInfo : serverInfos) {
				int serverNode = serverInfo.viaNodes[serverInfo.viaNodes.length - 1];
				if (!newServers.containsKey(serverNode)) {
					newServers.put(serverNode, new Server(serverNode));
				}
				newServers.get(serverNode).addServerInfo(serverInfo);
			}
			cost = cost + newServers.size() * Global.depolyCostPerServer;
			Server[] nextGlobalServers = new Server[newServers.size()];
			int size = 0;
			for (Server newServer : newServers.values()) {
				nextGlobalServers[size++] = newServer;
			}
			Global.updateSolution(nextGlobalServers);
			
		} else {

			if (Global.IS_DEBUG) {
				System.out.println("mcmf 无法找到一个满足的解！");
			}

		}

		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: " + (System.currentTimeMillis() - t));
		}

	}


	private void resetSourceEdge(int u, int v) {
		p[edgeIndex] = new McmfEdge();
		p[edgeIndex].u = u;
		p[edgeIndex].v = v;
		p[edgeIndex].cap = 0;
		p[edgeIndex].cost = inf;
		p[edgeIndex].flow = 0;
		p[edgeIndex].next = head[u];
		head[u] = edgeIndex++;
		p[edgeIndex] = new McmfEdge();
		p[edgeIndex].u = v;
		p[edgeIndex].v = u;
		p[edgeIndex].cap = 0;
		p[edgeIndex].flow = 0;
		p[edgeIndex].cost = 0;
		p[edgeIndex].next = head[v];
		head[v] = edgeIndex++;
	}

	private void resetEdge() {
		for (int i = 0; i < edgeIndex; i++) {
			p[i].flow = 0;
		}
	}

	private void addEdge(int u, int v, int cap, int cost) {
		p[edgeIndex] = new McmfEdge();
		p[edgeIndex].u = u;
		p[edgeIndex].v = v;
		p[edgeIndex].cap = cap;
		p[edgeIndex].cost = cost;
		p[edgeIndex].flow = 0;
		p[edgeIndex].next = head[u];
		head[u] = edgeIndex++;
		p[edgeIndex] = new McmfEdge();
		p[edgeIndex].u = v;
		p[edgeIndex].v = u;
		p[edgeIndex].cap = 0;
		p[edgeIndex].flow = 0;
		p[edgeIndex].cost = -cost;
		p[edgeIndex].next = head[v];
		head[v] = edgeIndex++;
	}

	private boolean[] visDFS;
	private int[] visEdge;
	private int visCount = 0;
	private int MINFLOW = inf;

	private static final int[] nodes = new int[Global.nodeNum];
	private void DFS(int s, int t, int n) {

		if (s == t) {
			MINFLOW = inf;
			for (int visedge : visEdge) {
				if (visedge < 0) {
					break;
				}
				MINFLOW = Math.min(MINFLOW, p[visedge].flow);
			}
			if (MINFLOW <= 0) {
				return;
			}
			int nodeSize = 0;
			for (int visedge : visEdge) {
				if (visedge < 0) {
					break;
				}
				p[visedge].flow -= MINFLOW;
				p[visedge ^ 1].flow += MINFLOW;
				nodes[nodeSize++] = p[visedge].v;
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
			int consumerId = Global.nodeToConsumerId.get(viaNodes[0]);
			ServerInfo serverInfo = new ServerInfo(consumerId, MINFLOW, viaNodes);
			serverInfos.add(serverInfo);

			return;
		}
		for (int i = head[s]; i != -1; i = p[i].next) {
			if (p[i].v != sourceNode && p[i].flow > 0 && !visDFS[i]) {
				visDFS[i] = true;
				visEdge[visCount++] = i;
				DFS(p[i].v, t, n);
				visEdge[--visCount] = -3;
				visDFS[i] = false;
			}
		}

	}


	private final static int[] len = new int[Global.mcmfNodeNum];
	private final boolean spfa(int s, int t, int n) {
		int u, v;
		Queue<Integer> q = new LinkedList<Integer>();
		Arrays.fill(vis, false);
		Arrays.fill(pre, -1);
		for (int i = 0; i < n; ++i) {
			dis[i] = inf;
			len[i] = 0;
		}
		vis[s] = true;
		dis[s] = 0;

		q.offer(s);
		while (!q.isEmpty()) {
			u = q.peek();
			q.poll();
			vis[u] = false;
			for (int i = head[u]; i != -1; i = p[i].next) {
				if (p[i].cap <= p[i].flow) {
					continue;
				}
				v = p[i].v;
				if (dis[v] > dis[u] + p[i].cost) {
					dis[v] = dis[u] + p[i].cost;
					pre[v] = i;
					len[v] = len[u] + 1;
					if (!vis[v]) {
						q.offer(v);
						vis[v] = true;
					}
				} else if (dis[v] == dis[u] + p[i].cost && len[v] > len[u] + 1) {
					dis[v] = dis[u] + p[i].cost;
					pre[v] = i;
					len[v] = len[u] + 1;
					if (!vis[v]) {
						q.offer(v);
						vis[v] = true;
					}
				}
			}
		}
		if (dis[t] == inf)
			return false;
		return true;
	}

	private List<ServerInfo> serverInfos = new LinkedList<ServerInfo>();

	private int MCMF(int s, int t, int n) {
		
		int flow = 0; // 总流量
		int minflow, mincost;
		mincost = 0;
		while (spfa(s, t, n)) {
			minflow = inf + 1;
			for (int i = pre[t]; i != -1; i = pre[p[i ^ 1].v])
				if ((p[i].cap - p[i].flow) < minflow)
					minflow = (p[i].cap - p[i].flow);
			flow += minflow;
			for (int i = pre[t]; i != -1; i = pre[p[i ^ 1].v]) {
				p[i].flow += minflow;
				p[i ^ 1].flow -= minflow;
			}
			mincost += dis[t] * minflow;
			
		}
		sumFlow = flow; // 最大流
		return mincost;
	}
	
}
