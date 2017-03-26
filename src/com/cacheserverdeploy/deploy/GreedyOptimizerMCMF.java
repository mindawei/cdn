package com.cacheserverdeploy.deploy;

import java.util.LinkedList;
import java.util.List;

/**
 * 利用最大最小费用流进行优化s
 *
 * @author mindw
 * @date 2017年3月23日
 */
public final class GreedyOptimizerMCMF extends GreedyOptimizer{

	public GreedyOptimizerMCMF(){}
	
	public GreedyOptimizerMCMF(boolean isOptimizeOnce){
		super(isOptimizeOnce);
	}
	
	@Override
	protected void transferServers(Server[] newServers) {
		
		for(int node =0;node<Global.nodeNum;++node){
			// 没有服务器
			if(newServers[node]==null){
				Global.graph[node][Global.sourceNode] = null;
				Global.graph[Global.sourceNode][node] = null;
			}else{
				Global.graph[Global.sourceNode][node] = new Edge(Global.INFINITY, 0);
				Global.graph[node][Global.sourceNode] = new Edge(0, 0);
			}
		}
		
		List<ServerInfo> serverInfos = mcmf();

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
			for(int node =0;node<Global.nodeNum;++node){
				if(newServers[node]==null){
					continue;
				}
				Server newServer = newServers[node];
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
	private final int[] nodes = new int[Global.nodeNum];
	private int nodeSize = 0;		
			
	/**
	 * @return 返回路由,不存在解决方案则为无穷大
	 */
	private List<ServerInfo> mcmf(){
		
		List<ServerInfo> serverInfos = new LinkedList<ServerInfo>();
		
		
		int minFlow;
		int sumFlow = 0;
		int s = Global.sourceNode;
		int t = Global.endNode;
		
		while(spfa(s,t)){
					
			minFlow = Integer.MAX_VALUE;
			for(int i=t;i!=s;i=mcmfPre[i]){
				if(Global.graph[mcmfPre[i]][i].leftBandWidth<minFlow){
					minFlow = Global.graph[mcmfPre[i]][i].leftBandWidth;
				}
			}
			
			sumFlow+=minFlow;
			for(int i=t;i!=s;i=mcmfPre[i]){
				Global.graph[mcmfPre[i]][i].leftBandWidth-=minFlow;
				Global.graph[i][mcmfPre[i]].leftBandWidth+=minFlow;
				// minCost += cost[mcmfPre[i]][i]*minFlow;
			}
			//minCost += dist[t]*minFlow;
			
			// 保存服务信息,从消费者开始的 -> 服务器
			nodeSize = 0;
			for(int i=mcmfPre[t];i!=s;i=mcmfPre[i]){
				nodes[nodeSize++] = i;
			}
		
			int consumerId = Global.nodeToConsumerId.get(nodes[0]);
			int[] viaNodes = new int[nodeSize];
			System.arraycopy(nodes, 0, viaNodes, 0, nodeSize);
			ServerInfo serverInfo = new ServerInfo(consumerId, minFlow, viaNodes);
			serverInfos.add(serverInfo);
		}
		
		if(sumFlow>=Global.consumerTotalDemnad){
			return serverInfos;
		}else{
			return null;
		}
	}

	private final int[] mcmfPre = new int[Global.mcmfNodeNum];
	private final boolean vis[] = new boolean[Global.mcmfNodeNum];
	private final int[] dist = new int[Global.mcmfNodeNum];
	//private final Queue<Integer> que = new LinkedList<Integer>();
	private final int[] que = new int[Global.mcmfNodeNum];
	// 左边指针 == 右边指针 时候队列为空
	// 左边指针，指向队首
	private int queL;
	// 右边指针，指向下一个插入的地方
	private int queR;
	
	private boolean spfa(int s, int t) {
		
		for (int i = 0; i <= t; i++) {
			vis[i] = false;
			dist[i] = Global.INFINITY;
		}
		
		// que.clear();
		queL = 0;
		queR = 0;
		
		vis[s] = true;
		dist[s] = 0;
		//que.offer(s);
		que[queR++] = s;
		//while (!que.isEmpty()) {
		while (queL!=queR) {
			// int u = que.poll();
			int u = que[queL++];
			queL = queL % Global.mcmfNodeNum;
			
			vis[u] = false;
			for (int v = 0; v <= t; v++) {
				if (Global.graph[u][v]!=null&&
						Global.graph[u][v].leftBandWidth > 0
						&& dist[v] > dist[u] + Global.graph[u][v].cost) {
					dist[v] = dist[u] + Global.graph[u][v].cost;
					mcmfPre[v] = u;
					if (!vis[v]) {
						que[queR++] = v;
						queR = queR % Global.mcmfNodeNum;
						vis[v] = true;
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
