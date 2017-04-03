package com.cacheserverdeploy.deploy;

import java.util.Arrays;

/**
 * 利用最大最小费用流进行优化
 *
 * @author mindw
 * @date 2017年3月23日
 */
public class OptimizerComplex extends Optimizer{
	
	
	/** 新服务器是否已经安装 */
	protected final boolean[] isNewServerInstalled = new boolean[Global.nodeNum];
	/** 是否是新服务器 */
	protected final boolean[] isNewServer = new boolean[Global.nodeNum];
	
	
	protected void reset(int fromServerNode, int toServerNode){
		for (int i = 0; i < Global.nodeNum; ++i) {
			isNewServer[i] = false;
			isNewServerInstalled[i] = false;
		}
		
		// 1 与超级源点相连的重置 
		for (McmfEdge edge : edges) {
			edge.leftBandWidth = edge.initBandWidth;
		}

		// 2 设置与超级源点相连的节点
		for (int i = 0; i < serverNodesSize; ++i) {
			int serverNode = serverNodes[i];
			if (serverNode != fromServerNode) {
				isNewServer[serverNode] = true;
				// 重置
				serverEdges[serverNode].leftBandWidth  = inf;		
			}
		}
		
		isNewServer[toServerNode] = true;
		// 重置
		serverEdges[toServerNode].leftBandWidth  = inf;
		
	}
	
	@Override
	protected int getCostAfterMove(int fromServerNode, int toServerNode) {
		
		reset(fromServerNode, toServerNode);
		
		int cost = 0;
		int flow = 0; // 总流量
	
		while (spfa()) {
			
			int minflow = Global.INFINITY;		
			for (int i = pre[endNode]; i != -1; i = pre[edges[i ^ 1].toNode ]){
				if (edges[i].leftBandWidth  < minflow){
					minflow = edges[i].leftBandWidth ;
				}
			}
			flow += minflow;
			
			int serverNode = -1;
			for (int i = pre[endNode]; i != -1; i = pre[edges[i ^ 1].toNode ]) {
				edges[i].leftBandWidth  -= minflow;
				edges[i ^ 1].leftBandWidth  += minflow;
				serverNode = edges[i].toNode ; 
			}
			cost += dis[endNode] * minflow;
			
			if(!isNewServerInstalled[serverNode]){
				isNewServerInstalled[serverNode] = true;
				cost+=Global.depolyCostPerServer;
			}
			
			if(flow==Global.consumerTotalDemnad){
				break;
			}
		}
		
		if(flow<Global.consumerTotalDemnad){
			cost = Global.INFINITY;
		}
		
		if (Global.IS_DEBUG) {
			System.out.println("round:"+(round++)+" cost:"+cost);
		}
	
		return cost;
	}
	
	int round;

	

	@Override
	protected void moveBest(int fromServerNode, int toServerNode) {
		
		reset(fromServerNode, toServerNode);
		
		int flow = 0; // 总流量
	
		while (spfa()) {
			
			int minflow = Global.INFINITY;		
			for (int i = pre[endNode]; i != -1; i = pre[edges[i ^ 1].toNode]){
				if (edges[i].leftBandWidth < minflow){
					minflow = edges[i].leftBandWidth;
				}
			}
			flow += minflow;
			
			int serverNode = -1;
			for (int i = pre[endNode]; i != -1; i = pre[edges[i ^ 1].toNode]) {
				edges[i].leftBandWidth  -= minflow;
				edges[i ^ 1].leftBandWidth  += minflow;
				serverNode = edges[i].toNode; 
			}
	
			if(!isNewServerInstalled[serverNode]){
				isNewServerInstalled[serverNode] = true;
			}
			
			if(flow==Global.consumerTotalDemnad){
				break;
			}
			
		}
		
		serverNodesSize = 0;
		for (int node = 0; node < Global.nodeNum; ++node) {
			if (isNewServerInstalled[node]) {
				serverNodes[serverNodesSize++] = node;
			}
		}

		if (Global.IS_DEBUG) {
			System.out.println("移动成功");
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
		que = new int[maxn];
		head = new int[maxn << 1];
		
		Arrays.fill(head, -1);

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

	protected final class McmfEdge {
	
		final int toNode;
		final int initBandWidth;
		final int next;
		final int cost;
		int leftBandWidth;
		
		public McmfEdge(int toNode, int initBandWidth, int cost, int next) {
			super();
			this.toNode = toNode;
			this.initBandWidth = initBandWidth;
			this.cost = cost;
			this.next = next;
			this.leftBandWidth = initBandWidth;
		}

	}

	protected final int maxn;
	protected final int inf = 1000000000;
	protected int edgeIndex = 0;
	protected final McmfEdge[] edges;
	protected int[] head;
	protected int[] dis;
	protected int[] pre;
	protected boolean[] vis;

	protected int sourceNode;
	protected int endNode;


	protected final McmfEdge[] serverEdges = new McmfEdge[Global.nodeNum];
	protected void resetSourceEdge(int v) {
		edges[edgeIndex] = new McmfEdge(v, 0, 0, head[sourceNode]);
		serverEdges[v] = edges[edgeIndex];
		head[sourceNode] = edgeIndex++;
		
		edges[edgeIndex] = new McmfEdge(sourceNode,0,0,head[v]);
		head[v] = edgeIndex++;
	}
	
	protected void addEdge(int u, int v, int cap, int cost) {
		edges[edgeIndex] = new McmfEdge(v,cap,cost,head[u]);
		head[u] = edgeIndex++;	
		edges[edgeIndex] = new McmfEdge(u,0,-cost,head[v]);
		head[v] = edgeIndex++;
	}
	
	// 数组模拟循环队列，当前后指针在同一个位置上的时候才算结束（开始不包括）
	protected final int[] que;
	protected int qHead;  // 指向队首位置
	protected int qTail; // 指向下一个插入的位置
	
	protected final boolean spfa() {
		int u, v,newCost,insertNode;
		// que.clear()
		qHead = 0;
		qTail = 0;
		
		for(int i=0;i<maxn;++i){
			vis[i] = false;
			dis[i] = inf;
		}
	
		vis[sourceNode] = true;
		dis[sourceNode] = 0;
		pre[sourceNode] = -1;

		// que.offer(sourceNode);
		que[qTail++] = sourceNode;
		
		
		while (qHead!=qTail) {
			// u = que.poll();
			u = que[qHead++];
			if(qHead==que.length){
				qHead = 0;
			}
			
			vis[u] = false;
			for (int i = head[u]; i != -1; i = edges[i].next) {
				if (edges[i].leftBandWidth == 0) {
					continue;
				}
				v = edges[i].toNode;
				newCost = dis[u] + edges[i].cost; 
				if (newCost < dis[v] && newCost < dis[endNode]) { // 费用比目的地低才考虑
					dis[v] = newCost;
					pre[v] = i;
					if (!vis[v]) {
						// que.offer(v);
						insertNode = v;
						
						// 队伍不空，比头部小
						if(qHead!=qTail && dis[v]<dis[que[qHead]]){
							insertNode = que[qHead]; 
							que[qHead] = v;
						}
		
						que[qTail++] = insertNode;
						if(qTail==que.length){
							qTail = 0;
						}
					
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
}
