package com.cacheserverdeploy.deploy;

import java.util.LinkedList;
import java.util.List;

public class Zkw{
	
	static void init() {
		
		int nodeNum = Global.nodeNum;

		// 多源多汇问题，定义一个超级源点s,一个超级汇点e
		sourceNode = nodeNum;
		endNode = nodeNum + 1;
		maxn = nodeNum + 2;

		// 多个边加上超级汇点
		// 最多多少条边
		int maxm = Global.edges.length + Global.consumerNum + nodeNum;

		edges = new ZkwEdge[maxm << 1];
		viaEdges = new ZkwEdge[maxm << 1];
		vis = new boolean[maxn];
		head = new ZkwEdge[maxn];
		
		// 消费节点ID为0，相连网络节点ID为8，视频带宽消耗需求为40
		for (Edge edge : Global.edges) {
			addEdge(edge.from, edge.to, edge.initBandWidth, edge.cost);
		}

		// 消费节点，相连网络节点ID，视频带宽消耗需求
		for (int consumerId = 0; consumerId < Global.consumerNum; ++consumerId) {
			// 定义消费节点到超级汇点的带宽 消费节点的需求带宽
			addConsumerEdge(consumerId);
		}

		// 链接超级源
		for (int node = 0; node < Global.nodeNum; node++){
			addServerEdge(node);
		}
	}
	
	private static final Server[] servers = new Server[Global.nodeNum];
	
	/** 优化全局的 */
	static final Server[] optimize(int[] consumerOutputs, int[] outputServerLevel,int usedMoneyOfBandwidth) {
	
		// 消费者重置
		int consumerTotalDemnad = 0;
		for (int consumerId = 0; consumerId < Global.consumerNum; ++consumerId) {
			consumerEdges[consumerId].initCost = 0;
			consumerEdges[consumerId].initBandWidth = consumerOutputs[consumerId];
			consumerTotalDemnad += consumerOutputs[consumerId];
		}

		// 与超级源点相连的重置
		for (int node = 0; node < Global.nodeNum; ++node) {
			serverEdges[node].initCost = 0;
			serverEdges[node].initBandWidth = (outputServerLevel[node]==-1 ? 0 : Global.serverMaxOutputs[outputServerLevel[node]]);
		}
				
		for (ZkwEdge edge : edges) {
			edge.cost = edge.initCost;
			edge.leftBandWidth = edge.initBandWidth;
		}

		zkwCost = 0;
		pi1 = 0;
		flow = 0;
		
		do {
			do {
				for(int i=0;i<vis.length;++i){
					vis[i] = false;
				}
			} while (System.currentTimeMillis()<Global.TIME_OUT_OF_MCMF && aug(sourceNode, Global.INFINITY) > 0);
		} while (System.currentTimeMillis()<Global.TIME_OUT_OF_MCMF && modlabel());
		
		
		if(flow >= consumerTotalDemnad && zkwCost<usedMoneyOfBandwidth){
			
			for (ZkwEdge edge : edges) {
				edge.flow = edge.initBandWidth - edge.leftBandWidth;
				edge.visited = false;
			}
		
			serverInfos.clear();
			findPathByDfs(sourceNode,0);

			for(int i=0;i<Global.nodeNum;++i){
				servers[i] = null;
			}
			for (ServerInfo serverInfo : serverInfos) {
				int serverNode = serverInfo.viaNodes[serverInfo.viaNodes.length - 1];
				if (servers[serverNode] == null) {
					servers[serverNode] =  new Server(serverNode);
				}
				servers[serverNode].addServerInfo(serverInfo);
			}
			return servers;
			
		}else{
			return null;
		}
	
	}
	
	private static final List<ServerInfo> serverInfos = new LinkedList<ServerInfo>();
	private static int inf = 1000000000;
	private static ZkwEdge[] viaEdges;
	
	private static final void findPathByDfs(int node,int viaEdgeSize) {

		if (node == endNode) {
			
			int provideBandWidth = inf;
			for (int i=0;i<viaEdgeSize;++i) {
				if(viaEdges[i].flow<provideBandWidth){
					provideBandWidth = viaEdges[i].flow;
				}
			}
			if(provideBandWidth==0){
				return;
			}
			
			for (int i=0;i<viaEdgeSize;++i) {
				viaEdges[i].flow -= provideBandWidth;
				viaEdges[i].pair.flow += provideBandWidth;
			}
			
			// 去掉最后一个点，超级源点
			int lsNodesSize = viaEdgeSize-1;
			int[] viaNodes = new int[lsNodesSize];
			for (int i = 0;i<lsNodesSize;++i) {
				viaNodes[lsNodesSize-1-i] = viaEdges[i].toNode;
			}
			int consumerId = Global.nodeToConsumerId[viaNodes[0]];
			ServerInfo serverInfo = new ServerInfo(consumerId, provideBandWidth, viaNodes);
			serverInfos.add(serverInfo);
			return;
		}
		
		for (ZkwEdge edge = head[node]; edge != null; edge = edge.next) {
			if (edge.toNode != sourceNode && edge.flow > 0 && !edge.visited) {
				edge.visited = true;
				viaEdges[viaEdgeSize] = edge;
				findPathByDfs(edge.toNode,viaEdgeSize+1);
				edge.visited = false;
			}
		}

	}
	
	
	///////////////////
	
	private static  ZkwEdge[] head;
	private static  ZkwEdge[] edges;

	private static  class ZkwEdge {

		int toNode;

		int initCost;
		int cost;

		int initBandWidth;
		int leftBandWidth;
		
		int flow;
		boolean visited;

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

	private static  int maxn;
	private static  boolean[] vis;
	private static int edgeIndex = 0;

	private static  int sourceNode;
	private static  int endNode;

	private static  ZkwEdge[] serverEdges = new ZkwEdge[Global.nodeNum];

	private static void addServerEdge(int toNode) {

		ZkwEdge toEdge = new ZkwEdge(toNode, 0, 0);
		serverEdges[toNode] = toEdge;

		ZkwEdge backEdge = new ZkwEdge(sourceNode,0, 0);

		edges[edgeIndex++] = toEdge;
		edges[edgeIndex++] = backEdge;

		toEdge.next = head[sourceNode];
		head[sourceNode] = toEdge;

		backEdge.next = head[toNode];
		head[toNode] = backEdge;

		toEdge.pair = backEdge;
		backEdge.pair = toEdge;
	}

	
	private static void addEdge(int fromNode, int toNode, int bandwidth, int cost) {

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
	
	private static final ZkwEdge[] consumerEdges = new ZkwEdge[Global.consumerNum];

	private static final void addConsumerEdge(int consumerId) {
		
		int fromNode = Global.consumerNodes[consumerId];
		int toNode = endNode;
		
		ZkwEdge toEdge = new ZkwEdge(toNode, 0, 0);
		consumerEdges[consumerId] = toEdge;
		
		ZkwEdge backEdge = new ZkwEdge(fromNode, 0, 0);

		edges[edgeIndex++] = toEdge;
		edges[edgeIndex++] = backEdge;

		toEdge.next = head[fromNode];
		head[fromNode] = toEdge;

		backEdge.next = head[toNode];
		head[toNode] = backEdge;

		toEdge.pair = backEdge;
		backEdge.pair = toEdge;
	}

	static int zkwCost;
	private static int pi1;
	private static int flow;
	private static int aug(int fromNode, int bandwidth) {

		if (fromNode == endNode) {			
			zkwCost += pi1 * bandwidth;
			flow += bandwidth;
			return bandwidth;
		}

		vis[fromNode] = true;
		int leftBandwidth = bandwidth;

		for (ZkwEdge edge = head[fromNode]; edge != null; edge = edge.next) {
			if (edge.leftBandWidth != 0 && edge.cost == 0 && !vis[edge.toNode]) {

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

	private static boolean modlabel() {
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