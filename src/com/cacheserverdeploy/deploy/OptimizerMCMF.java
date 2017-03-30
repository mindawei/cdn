package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
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
	
	class McmfEdge {
		int u, v, 
		cap,
		cost,
		flow,
		next;
		
		@Override
		public String toString() {
			return "Edge [u="+u+",v=" + v + ", cap=" + cap + ", cost=" + cost+",flow="+flow
					+ ", next=" + next + "]";
		}
		
	}

	//// 最小费用最大流模版.求最大费用最大流建图时把费用取负即可。
	//// 无向边转换成有向边时需要拆分成两条有向边。即两次加边。
	// 最多节点数
	int maxn ;
	// 最多边数
	int maxm;
	int inf = 1000000000;
	int edgeIndex = 0;
	McmfEdge[] p;
	int sumFlow, n, m, st, en;
	int[] head ;
	int[] dis ;
	int[] pre;
	boolean[] vis;
	
	int nodeNum;
	int sourceNode;
	int endNode;
	public OptimizerMCMF(String[] graphContent){
		 
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
		
		p = new McmfEdge[maxm << 1];
		dis = new int[maxn];
		pre = new int[maxn];
		vis = new boolean[maxn];
		head = new int[maxn<<1];
		Arrays.fill(head, -1);

		
		/* dfs*/
		visDFS = new boolean[maxm << 1];
		visEdge = new int[maxm << 1];
		
		
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
	}
	
	public void optimize(){
		
		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}

		long t = System.currentTimeMillis();
		
		for(Server server : Global.getBestServers()){
			if(server==null){
				break;
			}
			addEdge(sourceNode, server.node, inf, 0);
		}
			
//		addEdge(sourceNode, 3, inf, 0);
//		addEdge(sourceNode, 0, inf, 0);
//		addEdge(sourceNode, 22, inf, 0);
//		addEdge(sourceNode, 5, inf, 0);
//		addEdge(sourceNode, 11, inf, 0);
//		addEdge(sourceNode, 23, inf, 0);
//		addEdge(sourceNode, 45, inf, 0);
//		addEdge(sourceNode, 52, inf, 0);
//		addEdge(sourceNode, 56, inf, 0);
//		addEdge(sourceNode, 58, inf, 0);
//		addEdge(sourceNode, 61, inf, 0);
//		addEdge(sourceNode, 62, inf, 0);
//		addEdge(sourceNode, 65, inf, 0);
//		addEdge(sourceNode, 75, inf, 0);
//		addEdge(sourceNode, 89, inf, 0);
//		addEdge(sourceNode, 98, inf, 0);
//		addEdge(sourceNode, 113, inf, 0);
//		addEdge(sourceNode, 118, inf, 0);
//		addEdge(sourceNode, 136, inf, 0);
//		addEdge(sourceNode, 180, inf, 0);
//		addEdge(sourceNode, 192, inf, 0);
//		addEdge(sourceNode, 288, inf, 0);
	
//		for(int i=0;i<p.length;i++){
//			if(p[i]!=null){
//				System.out.println(p[i].toString());
//			}
//			
//		}
		int cost = 
		MCMF(sourceNode, endNode,maxn);
		
		//System.out.println("cost:"+cost);
		//System.out.println("sumFlow:"+sumFlow+" consumerTotalDemnad:"+Global.consumerTotalDemnad);
		Arrays.fill(visEdge, -3);
		serverInfos.clear();
		DFS(sourceNode,endNode,maxn);
//		for(int i=0;i<p.length;i++){
//			if(p[i]!=null){
//				System.out.println(p[i].toString());
//			}
//			
//		}
		
		if(sumFlow>=Global.consumerTotalDemnad){
			
			Map<Integer,Server> newServers = new HashMap<Integer,Server>();
			for(ServerInfo serverInfo : serverInfos){
				int serverNode = serverInfo.viaNodes[serverInfo.viaNodes.length-1];
				if(!newServers.containsKey(serverNode)){
					newServers.put(serverNode, new Server(serverNode));
				}
				newServers.get(serverNode).addServerInfo(serverInfo);
			}
			cost = cost + newServers.size() * Global.depolyCostPerServer;
			System.out.println("cost:"+cost);
			Server[] nextGlobalServers = new Server[newServers.size()];
			int size = 0;
			for(Server newServer : newServers.values()){
				nextGlobalServers[size++] = newServer;
			}
			Global.updateSolution(nextGlobalServers);
			
		/*	
			Global.resetEdgeBandWidth();
			optimize(nextGlobalServers);
			
			
			Global.resetEdgeBandWidth();
			for(int fromNode =0;fromNode<nodeNum;++fromNode){
				for (int i = head[fromNode]; i != -1; i = p[i].next) {
					int toNode = p[i].v;
					if(toNode>=nodeNum){
						continue;
					}
					// 反向边的流量
					if(p[i].cost<0){ // 只考虑一条边
						continue;
					}
					// 反向边的流量
					//if(p[i^1].cap==0){
					Global.graph[fromNode][toNode].leftBandWidth = p[i^1].cap;
					//}
				}
			}
			
			// 重新更新流量后利用 complex进行寻路
			optimize(nextGlobalServers);
			// optimize(Global.getBestServers());
			*/

			
		}else{
			
			if(Global.IS_DEBUG){
				System.out.println("mcmf 无法找到一个满足的解！");
			}
			
		}

		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() +" 结束，耗时: " + (System.currentTimeMillis() - t));
		}
		
		
		
	}

	static int[][] priority = new int[Global.graph.length][Global.graph[0].length];
	

	void addEdge(int u, int v, int cap, int cost) {
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
	


	boolean []visDFS ;
	int []visEdge ;
	int visCount =0;
	int MINFLOW =inf;
	void DFS(int s,int t,int n){

		if(s==t){
			MINFLOW =inf;
			for(int visedge:visEdge){
				if(visedge<0){
					break;
				}
				MINFLOW = Math.min(MINFLOW, p[visedge].flow);
			}
			if(MINFLOW<=0){
				return;
			}
			int nodeSize = 0;
			for(int visedge:visEdge){
				if(visedge<0){
					break;
				}
				p[visedge].flow-=MINFLOW;
				p[visedge ^ 1].flow +=MINFLOW;
				nodes[nodeSize++]=p[visedge].v;
				//System.out.print(p[visedge].v+" ");
			}
			//System.out.println(MINFLOW);
			
			// 保存服务信息,从消费者开始的 -> 服务器
			
			
			// 去头  end,node0,node1,....
			nodeSize--; 
			int k =0;
			int[] viaNodes = new int[nodeSize];
			//nodeSize--;
			for(int j=nodeSize-1;j>=0;j--){
				viaNodes[k++]=nodes[j];
			}
			
			
		//	System.arraycopy(nodes, 1, viaNodes, 0, nodeSize);
			
			int consumerId = Global.nodeToConsumerId.get(viaNodes[0]);
			ServerInfo serverInfo = new ServerInfo(consumerId, MINFLOW, viaNodes);
			serverInfos.add(serverInfo);
			
			return;
		}
		for(int i=head[s];i!=-1;i =p[i].next){
			if(p[i].v!=sourceNode&&p[i].flow>0&&!visDFS[i]){
				visDFS[i]=true;
				visEdge[visCount++]=i;
				DFS(p[i].v,t,n);
				visEdge[--visCount]=-3;
				visDFS[i]=false;
			}
		}
		
	}
	
	boolean spfa(int s, int t, int n) {
		int u, v;
		Queue<Integer> q = new LinkedList<Integer>();
		Arrays.fill(vis, false);
		Arrays.fill(pre, -1);
		for (int i = 0; i < n; ++i){
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
				if(p[i].cap<=p[i].flow){
					continue;
				}
				v = p[i].v;
				if (dis[v] > dis[u] + p[i].cost) {
					dis[v] = dis[u] + p[i].cost;
					pre[v] = i;
					len[v] = len[u]+1;
					if (!vis[v]) {
						q.offer(v);
						vis[v] = true;
					}
				}else if(dis[v] == dis[u] + p[i].cost
						&& len[v] > len[u]+1){
					dis[v] = dis[u] + p[i].cost;
					pre[v] = i;
					len[v] = len[u]+1;
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

	// int[] nodes = new int[Global.nodeNum];
	List<ServerInfo> serverInfos = new LinkedList<ServerInfo>();
	
	int MCMF(int s, int t, int n) {
		
		//serverInfos.clear();
		
		int flow = 0; // 总流量
		int minflow, mincost;
		mincost = 0;
		while (spfa(s, t, n)) {
			minflow = inf + 1;
			for (int i = pre[t]; i != -1; i = pre[p[i ^ 1].v])
				if ((p[i].cap-p[i].flow) < minflow)
					minflow = (p[i].cap-p[i].flow);
			flow += minflow;
			for (int i = pre[t]; i != -1; i = pre[p[i ^ 1].v]) {
				p[i].flow += minflow;
				p[i ^ 1].flow -= minflow;
			}
			mincost += dis[t] * minflow;
			
//			// 保存服务信息,从消费者开始的 -> 服务器
//			int nodeSize = 0;
//			for (int i = pre[t]; i != -1; i = pre[p[i ^ 1].v]) {
//				nodes[nodeSize++] = p[i].v;
//			}
//			
//			// 去头  end,node0,node1,....
//			nodeSize--; 
//			
//			
//			int[] viaNodes = new int[nodeSize];
//			System.arraycopy(nodes, 1, viaNodes, 0, nodeSize);
//			
//			int consumerId = Global.nodeToConsumerId.get(viaNodes[0]);
//			ServerInfo serverInfo = new ServerInfo(consumerId, minflow, viaNodes);
//			serverInfos.add(serverInfo);
			
		}
		sumFlow = flow; // 最大流
		return mincost;
	}
	
	
	/// 复杂djkstra
	/** 为了复用，为null的地方不放置服务器 */
	private static Server[] newServers = new Server[Global.nodeNum];
	/** 为了复用，下一轮的服务器，模拟队列，当遇到null时表示结束*/
	private static Server[] lsNewServers = new Server[Global.nodeNum];
	
	/** 为了复用，下一轮的服务器，模拟队列，当遇到null时表示结束*/
	private static Server[] nextGlobalServers = new Server[Global.nodeNum];

	/** 优化全局最优解 */
	static void optimizeBestServers() {
		optimize(Global.getBestServers());
	}

	static void optimize(Server[] oldServers) {
		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println( "做一次Complex");
		}
		Arrays.fill(newServers, null);
		int lsSize = 0;
		for (Server server : oldServers) {
			if(server==null){
				break;
			}
			Server newServer = new Server(server.node);
			newServers[server.node] = newServer;
			lsNewServers[lsSize++] = newServer;
		}
		transferServers(nextGlobalServers,newServers,lsNewServers,lsSize);
		Global.updateSolution(nextGlobalServers);

	}
	
	static void transferServers(Server[] nextGlobalServers,Server[] newServers,Server[] lsServers,int lsSize) {
		
		List<Integer> sourceToNodes = new ArrayList<Integer>();
		for(int node =0;node<Global.nodeNum;++node){
			// 没有服务器
			if(newServers[node]==null){
				Global.graph[node][Global.sourceNode] = null;
				Global.graph[Global.sourceNode][node] = null;
			}else{
				sourceToNodes.add(node);
				Global.graph[Global.sourceNode][node] = new Edge(Global.INFINITY, 0);
				Global.graph[node][Global.sourceNode] = new Edge(0, 0);
			}
		}
		
		Global.mfmcConnections[Global.sourceNode] = new int[sourceToNodes.size()];
		for(int i=0;i<sourceToNodes.size();++i){
			Global.mfmcConnections[Global.sourceNode][i] = sourceToNodes.get(i);
		}
		
		List<ServerInfo> serverInfos = complex();

		if(serverInfos==null){ // 无解
			int size = 0;
			for(int consumerId=0;consumerId<Global.consumerNum;++consumerId){
				nextGlobalServers[size++] = new Server(consumerId,Global.consumerNodes[consumerId],Global.consumerDemands[consumerId]);
			}
			// 尾部设置null表示结束
			if(size<nextGlobalServers.length){
				nextGlobalServers[size] = null;
			}
		}else{ // 有解
			int size = 0;
			for(ServerInfo serverInfo : serverInfos){
				int serverNode = serverInfo.viaNodes[serverInfo.viaNodes.length-1];
				Server newServer = newServers[serverNode];
				newServer.addServerInfo(serverInfo);
			}
			for(int i=0;i<lsSize;++i){
				Server newServer = lsServers[i];
				if(newServer.getDemand()>0){
					nextGlobalServers[size++] = newServer;
				}
			}
			// 尾部设置null表示结束
			if(size<nextGlobalServers.length){
				nextGlobalServers[size] = null;
			}
		}		
	}
	
	// 复用
	private static final int[] nodes = new int[Global.nodeNum];
	private static int nodeSize = 0;		
			
	/**
	 * @return 返回路由,不存在解决方案则为无穷大
	 */
	private static List<ServerInfo> complex(){
		
		List<ServerInfo> serverInfos = new LinkedList<ServerInfo>();
		
		int sumFlow = 0;
		int s = Global.sourceNode;
		int t = Global.endNode;
		
		while(spfa(s,t)){
					
			for(int i=t;i!=s;i=mcmfPre[i]){
				Global.graph[mcmfPre[i]][i].leftBandWidth-= mcmfFlow[t];
				//Global.graph[i][mcmfPre[i]].leftBandWidth+= mcmfFlow[t];
			}
			
			// 保存服务信息,从消费者开始的 -> 服务器
			nodeSize = 0;
			for(int i=mcmfPre[t];i!=s;i=mcmfPre[i]){
				nodes[nodeSize++] = i;
			}
			int[] viaNodes = new int[nodeSize];
			System.arraycopy(nodes, 0, viaNodes, 0, nodeSize);
			
			int consumerId = Global.nodeToConsumerId.get(viaNodes[0]);
			ServerInfo serverInfo = new ServerInfo(consumerId, mcmfFlow[t], viaNodes);
			serverInfos.add(serverInfo);
			
			sumFlow+= mcmfFlow[t];
		}
		System.out.println("sumFlow:"+sumFlow);
		if(sumFlow>=Global.consumerTotalDemnad){
			return serverInfos;
		}else{
			return null;
		}
	}

	private final static int[] mcmfFlow = new int[Global.mcmfNodeNum];
	private final static int[] mcmfPre = new int[Global.mcmfNodeNum];
	private final static int[] len = new int[Global.mcmfNodeNum];
	private final static boolean visist[] = new boolean[Global.mcmfNodeNum];
	private final static int[] dist = new int[Global.mcmfNodeNum];
	private final static int[] que = new int[Global.mcmfNodeNum];
	
	
	private static boolean spfa(int s, int t) {
		
		for (int i = 0; i <= t; i++) {
			visist[i] = false;
			mcmfFlow[i] = Global.INFINITY;
			dist[i] = Global.INFINITY;
			len[i] = 0;
		}
		
		// 左边指针 == 右边指针 时候队列为空
		// 左边指针，指向队首
		int queL = 0;
		// 右边指针，指向下一个插入的地方
		int queR = 0;
		visist[s] = true;
		dist[s] = 0;
		que[queR++] = s;

		while (queL!=queR) {
			int u = que[queL++];
			if(queL>= Global.mcmfNodeNum){
				queL = queL % Global.mcmfNodeNum;
			}
			
			visist[u] = false;
			// u -> v的边
			for (int v : Global.mfmcConnections[u]) {
				if(Global.graph[u][v].leftBandWidth ==0){
					continue;
				}
				
				if (dist[v] > dist[u] + Global.graph[u][v].cost) {
					
					dist[v] = dist[u] + Global.graph[u][v].cost;
					mcmfFlow[v] = Math.min(mcmfFlow[u], Global.graph[u][v].leftBandWidth);
					mcmfPre[v] = u;
					len[v] = len[u]+1;
					if (!visist[v]) {
						que[queR++] = v;
						if(queR>= Global.mcmfNodeNum){
							queR = queR % Global.mcmfNodeNum;
						}
						visist[v] = true;
					}
				}else if(dist[v] == dist[u] + Global.graph[u][v].cost
						&& len[v] > len[u]+1){
					
					mcmfFlow[v] = Math.min(mcmfFlow[u], Global.graph[u][v].leftBandWidth);
					mcmfPre[v] = u;
					len[v] = len[u]+1;
					if (!visist[v]) {
						que[queR++] = v;
						if(queR>= Global.mcmfNodeNum){
							queR = queR % Global.mcmfNodeNum;
						}
						visist[v] = true;
					}
					
				}
				
			}
		}
		if (dist[t] == Global.INFINITY) {
			return false;
		} else {
			return true;
		}
	}
}
