package com.cacheserverdeploy.deploy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * 利用最大最小费用流进行优化
 *
 * @author mindw
 * @date 2017年3月23日
 */
public final class OptimizerComplex extends Optimizer{
	
	
	/** 新服务器是否已经安装 */
	private final boolean[] isNewServerInstalled = new boolean[Global.nodeNum];
	/** 是否是新服务器 */
	private final boolean[] isNewServer = new boolean[Global.nodeNum];
	
	@Override
	protected int getCostAfterMove(int fromServerNode, int toServerNode) {
		
		for (int i = 0; i < Global.nodeNum; ++i) {
			isNewServer[i] = false;
			isNewServerInstalled[i] = false;
		}
		
		// 1 与超级源点相连的重置 
		for (McmfEdge edge : serverEdges) {
			edge.cap = 0;
			edge.cost = inf;
		}

		// 2 设置与超级源点相连的节点
		for (int i = 0; i < serverNodesSize; ++i) {
			int serverNode = serverNodes[i];
			if (serverNode != fromServerNode) {
				isNewServer[serverNode] = true;
				// 重置
				serverEdges[serverNode].cap = inf;
				serverEdges[serverNode].cost = 0;
			}
		}
		
		isNewServer[toServerNode] = true;
		// 重置
		serverEdges[toServerNode].cap = inf;
		serverEdges[toServerNode].cost = 0;
		
	
		// 3 重置的流量边 
		for (int i = 0; i < edgeIndex; i++) {
			edges[i].flow = 0;
		}
		
		return mcmfCost();
	}
	
	int round;

	private int mcmfCost() {
		
		int cost = 0;
		int flow = 0; // 总流量
	
		while (spfa()) {
			
			int minflow = Global.INFINITY;		
			for (int i = pre[endNode]; i != -1; i = pre[edges[i ^ 1].v]){
				int leftFlow = edges[i].cap - edges[i].flow;
				if (leftFlow < minflow){
					minflow = leftFlow;
				}
			}
			flow += minflow;
			
			for (int i = pre[endNode]; i != -1; i = pre[edges[i ^ 1].v]) {
				edges[i].flow += minflow;
				edges[i ^ 1].flow -= minflow;
			}
			cost += dis[endNode] * minflow;
			
			int serverNode = edges[pre[endNode]].u;
			if(!isNewServerInstalled[serverNode]){
				isNewServerInstalled[serverNode] = true;
				cost+=Global.depolyCostPerServer;
			}
			
			if(flow==Global.consumerTotalDemnad){
				break;
			}
		}
		
		if(flow==Global.consumerTotalDemnad){
			if (Global.IS_DEBUG) {
				System.out.println("round:"+(round++)+" cost:"+cost);
			}
			return cost;
		}else{
			if (Global.IS_DEBUG) {
				System.out.println("mcmf 无法找到一个满足的解！");
			}
			return Global.INFINITY;
		}
		
	}
	
	
	//////////////////////////////////////////////////////
	@Override
	protected void moveBest(int bestFromNode, int bestToNode) {
		
		mcmfServersSize = 0;
		mcmfServers[mcmfServersSize++] = bestToNode;
		for (int i=0;i<serverNodesSize;++i) {
			int serverNode = serverNodes[i];
			if(serverNode!=bestFromNode){
				mcmfServers[mcmfServersSize++] = serverNode;
			}
		}
		
		mcmfMove(mcmfServers, mcmfServersSize);
	
	}
	
	/** 优化一个位置的 */
	final void mcmfMove(int[] lsNewServers, int lsNewServersSize) {
	
		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}
		
		// 与超级源点相连的重置 
		for (int i = head[sourceNode]; i != -1; i = edges[i].next) {
			edges[i].cap = 0;
			edges[i].cost = inf;
		}
		
		for (int index =0;index <lsNewServersSize;++index) {
			int serverNode = lsNewServers[index];
			serverEdges[serverNode].cap = inf;
			serverEdges[serverNode].cost = 0;
		}
		
		for (int i = 0; i < edgeIndex; i++) {
			edges[i].flow = 0;
		}

		int totalCost = minCostMaxFlow();

		if (sumFlow >= Global.consumerTotalDemnad) {
			
			Arrays.fill(visEdge, -3);
			serverInfos.clear();
			DFS(sourceNode, endNode, maxn);

			Set<Integer> newServers = new HashSet<Integer>();
			for (ServerInfo serverInfo : serverInfos) {
				int serverNode = serverInfo.viaNodes[serverInfo.viaNodes.length - 1];
				newServers.add(serverNode);
			}

			serverNodesSize = 0;
			for (int node : newServers) {
				serverNodes[serverNodesSize++] = node;
			}
			
			totalCost += newServers.size() * Global.depolyCostPerServer;
			
			if (Global.IS_DEBUG) {
				System.out.println("totalCost:" + totalCost+" sumFlow:" + sumFlow + " consumerTotalDemnad:" + Global.consumerTotalDemnad);
			}

		} else {
			
			totalCost = Global.INFINITY;
			
			if (Global.IS_DEBUG) {
				System.out.println("mcmf 无法找到一个满足的解！");
			}

		}
	}
	
	////////////////////////
	
	public OptimizerComplex(String[] graphContent) {

		// 多少个网络节点，多少条链路，多少个消费节点
		String[] line0 = graphContent[0].split(" ");
		/** 网络节点数：不超过1000个 */
		int nodeNum = Integer.parseInt(line0[0]);

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

		edges = new McmfEdge[maxm << 1];
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
			resetSourceEdge(index);
		}
	}

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

	private final int maxn;
	private final int inf = 1000000000;
	private int edgeIndex = 0;
	private final McmfEdge[] edges;
	private int sumFlow;
	private int[] head;
	private int[] dis;
	private int[] pre;
	private boolean[] vis;

	private int sourceNode;
	private int endNode;

	private final int[] mcmfServers = new int[Global.nodeNum];
	private int mcmfServersSize;
	
	
	private List<ServerInfo> serverInfos = new LinkedList<ServerInfo>();

	private final McmfEdge[] serverEdges = new McmfEdge[Global.nodeNum];
	private void resetSourceEdge(int v) {
		edges[edgeIndex] = new McmfEdge();
		serverEdges[v] = edges[edgeIndex];
		
		edges[edgeIndex].u = sourceNode;
		edges[edgeIndex].v = v;
		edges[edgeIndex].cap = 0;
		edges[edgeIndex].cost = inf;
		edges[edgeIndex].flow = 0;
		edges[edgeIndex].next = head[sourceNode];
		head[sourceNode] = edgeIndex++;
		
		edges[edgeIndex] = new McmfEdge();
		edges[edgeIndex].u = v;
		edges[edgeIndex].v = sourceNode;
		edges[edgeIndex].cap = 0;
		edges[edgeIndex].flow = 0;
		edges[edgeIndex].cost = 0;
		edges[edgeIndex].next = head[v];
		head[v] = edgeIndex++;
	}


	private void addEdge(int u, int v, int cap, int cost) {
		edges[edgeIndex] = new McmfEdge();
		edges[edgeIndex].u = u;
		edges[edgeIndex].v = v;
		edges[edgeIndex].cap = cap;
		edges[edgeIndex].cost = cost;
		edges[edgeIndex].flow = 0;
		edges[edgeIndex].next = head[u];
		head[u] = edgeIndex++;
		edges[edgeIndex] = new McmfEdge();
		edges[edgeIndex].u = v;
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

	private static final int[] nodes = new int[Global.nodeNum];
	private void DFS(int s, int t, int n) {

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
			int consumerId = Global.nodeToConsumerId.get(viaNodes[0]);
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

	
	private final Queue<Integer> que = new LinkedList<Integer>();
	
	private final boolean spfa() {
		int u, v;
		que.clear();
		
		for(int i=0;i<maxn;++i){
			vis[i] = false;
			dis[i] = inf;
		}
	
		vis[sourceNode] = true;
		dis[sourceNode] = 0;
		pre[sourceNode] = -1;

		que.offer(sourceNode);
		while (!que.isEmpty()) {
			u = que.poll();
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
						que.offer(v);
						vis[v] = true;
					}
				}
			}
		}
		if (dis[endNode] == inf){
			return false;
		}else{
			return true;
		}
	}

	private int minCostMaxFlow() {
		int flow = 0; // 总流量
		int minflow, mincost;
		mincost = 0;
		while (spfa()) {
			minflow = inf + 1;
			for (int i = pre[endNode]; i != -1; i = pre[edges[i ^ 1].v])
				if ((edges[i].cap - edges[i].flow) < minflow)
					minflow = (edges[i].cap - edges[i].flow);
			flow += minflow;
			for (int i = pre[endNode]; i != -1; i = pre[edges[i ^ 1].v]) {
				edges[i].flow += minflow;
				edges[i ^ 1].flow -= minflow;
			}
			mincost += dis[endNode] * minflow;
			
		}
		sumFlow = flow; // 最大流
		return mincost;
	}


	
	
}
