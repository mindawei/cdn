package com.cacheserverdeploy.deploy;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 利用最大最小费用流进行优化
 *
 * @author mindw
 * @date 2017年3月23日
 */
public final class MCMF2 {
	
	class Edge {
		int v, cap, cost, next;
	}

	//// 最小费用最大流模版.求最大费用最大流建图时把费用取负即可。
	//// 无向边转换成有向边时需要拆分成两条有向边。即两次加边。
	// 最多节点数
	int maxn ;
	// 最多边数
	int maxm;
	int inf = 1000000000;
	int edgeIndex = 0;
	Edge[] p;
	int sumFlow, n, m, st, en;
	int[] head ;
	int[] dis ;
	int[] pre;
	boolean[] vis;
	
	int nodeNum;
	int sourceNode;
	int endNode;
	public MCMF2(String[] graphContent){
		 
		// 多少个网络节点，多少条链路，多少个消费节点
		String[] line0 = graphContent[0].split(" ");
		/** 网络节点数：不超过1000个 */
		nodeNum = Integer.parseInt(line0[0]);
		
		
		// 多源多汇问题，定义一个超级源点s,一个超级汇点e
		sourceNode  = nodeNum;
		endNode = nodeNum+1;
		maxn = nodeNum+2;
	
		/** 链路数：每个节点的链路数量不超过20条，推算出总共不超过20000 */
		int edgeNum = Integer.parseInt(line0[1]);
		
		/** 消费节点数：不超过500个 */
		int consumerNum=  Integer.parseInt(line0[2]);
	
		// 多个边加上超级汇点
		// 最多多少条边
		int maxm = edgeNum*2+consumerNum+nodeNum;
		
		p = new Edge[maxm << 1];
		dis = new int[maxn];
		pre = new int[maxn];
		vis = new boolean[maxn];
		head = new int[maxn];
		Arrays.fill(head, -1);

		int lineIndex  = 4;
		String line = null;
		// 每行：链路起始节点ID 链路终止节点ID 总带宽大小 单位网络租用费
		while(!(line=graphContent[lineIndex++]).isEmpty()){	
			String[] strs = line.split(" ");
			// 链路起始节点
			int fromNode = Integer.parseInt(strs[0]);
			//  链路终止节点
			int toNode = Integer.parseInt(strs[1]);
			// 总带宽大小 
			int bandwidth = Integer.parseInt(strs[2]);
			// 单位网络租用费
			int cost = Integer.parseInt(strs[3]);
			addEdge(fromNode, toNode, bandwidth, cost);
			addEdge( toNode, fromNode,bandwidth, cost);
		}
	
		// 消费节点，相连网络节点ID，视频带宽消耗需求
		for (int index = lineIndex; index < graphContent.length; ++index) {
			line = graphContent[index];
			String[] strs = line.split(" ");
			// int consumerId = Integer.parseInt(strs[0]);
			int node = Integer.parseInt(strs[1]);
			int demand = Integer.parseInt(strs[2]);
			// 定义消费节点到超级汇点的带宽 消费节点的需求带宽
			addEdge(node, endNode, demand, 0);
	
		}
		
		if(Global.IS_DEBUG){
			String info = String.format("初始化完成");
			System.out.println(info);
		}
		
	}
	
	public void optimize(){
		
		int n = 0;
		for(Server server : Global.getBestServers()){
			if(server==null){
				break;
			}
			addEdge(sourceNode, server.node, inf, 0);
			n++;
		}
		System.out.println(
				
				
				"mcmf2: "+(MCMF(sourceNode, endNode,maxn)+n*Global.depolyCostPerServer));
		
	}


	void addEdge(int u, int v, int cap, int cost) {
		p[edgeIndex] = new Edge();
		p[edgeIndex].v = v;
		p[edgeIndex].cap = cap;
		p[edgeIndex].cost = cost;
		p[edgeIndex].next = head[u];
		head[u] = edgeIndex++;
		p[edgeIndex] = new Edge();
		p[edgeIndex].v = u;
		p[edgeIndex].cap = 0;
		p[edgeIndex].cost = -cost;
		p[edgeIndex].next = head[v];
		head[v] = edgeIndex++;
	}

	boolean spfa(int s, int t, int n) {
		int u, v;
		Queue<Integer> q = new LinkedList<Integer>();
		Arrays.fill(vis, false);
		Arrays.fill(pre, -1);
		for (int i = 0; i < n; ++i)
			dis[i] = inf;
		vis[s] = true;
		dis[s] = 0;
		q.offer(s);
		while (!q.isEmpty()) {
			u = q.peek();
			q.poll();
			vis[u] = false;
			for (int i = head[u]; i != -1; i = p[i].next) {
				v = p[i].v;
				if (p[i].cap>0 && dis[v] > dis[u] + p[i].cost) {
					dis[v] = dis[u] + p[i].cost;
					pre[v] = i;
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

	int MCMF(int s, int t, int n) {
		int flow = 0; // 总流量
		int minflow, mincost;
		mincost = 0;
		while (spfa(s, t, n)) {
			minflow = inf + 1;
			for (int i = pre[t]; i != -1; i = pre[p[i ^ 1].v])
				if (p[i].cap < minflow)
					minflow = p[i].cap;
			flow += minflow;
			for (int i = pre[t]; i != -1; i = pre[p[i ^ 1].v]) {
				p[i].cap -= minflow;
				p[i ^ 1].cap += minflow;
			}
			mincost += dis[t] * minflow;
		}
		sumFlow = flow; // 最大流
		return mincost;
	}
	
	
	
	
	
}
