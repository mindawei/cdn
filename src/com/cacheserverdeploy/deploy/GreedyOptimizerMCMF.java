package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * 利用最大最小费用流进行优化
 *
 * @author mindw
 * @date 2017年3月23日
 */
public final class GreedyOptimizerMCMF extends GreedyOptimizer{

	public GreedyOptimizerMCMF(boolean isOptimizeOnce){
		super(isOptimizeOnce);
	}
	
	@Override
	protected ArrayList<Server> transferServers(Server[] newServers) {
		
		// 源头
		Arrays.fill(Global.graph[Global.sourceNode], null);
		for(int fromNode=0;fromNode<Global.mcmfNodeNum;++fromNode){
			Global.graph[fromNode][Global.sourceNode] = null;
		}
		
		for(int node =0;node<Global.nodeNum;++node){
			if(newServers[node]==null){
				continue;
			}
			Global.graph[Global.sourceNode][node] = new Edge(Global.INFINITY, 0);
			Global.graph[node][Global.sourceNode] = new Edge(0, 0);
		}
		
		List<ServerInfo> serverInfos = mcmf();

		if(serverInfos==null){ // 无解
			ArrayList<Server> nextGlobalServers = new ArrayList<Server>(Global.consumerNum);
			for(Server consumerServer : Global.getConsumerServer()){
				nextGlobalServers.add(consumerServer);
			}
			return nextGlobalServers;
			
		}else{ // 有解
			
			ArrayList<Server> nextGlobalServers = new ArrayList<Server>();
			for(ServerInfo serverInfo : serverInfos){
				int serverNode = serverInfo.viaNodes[serverInfo.viaNodes.length-1];
				Server newServer = newServers[serverNode];
				newServer.serverInfos.add(serverInfo);
			}
			for(int node =0;node<Global.nodeNum;++node){
				if(newServers[node]==null){
					continue;
				}
				Server newServer = newServers[node];
				if(newServer.getDemand()>0){
					nextGlobalServers.add(newServer);
				}
			}
			return nextGlobalServers;

		}		
	
	}
	

	/**
	 * @return 返回路由,不存在解决方案则为无穷大
	 */
	private List<ServerInfo> mcmf(){
		
		List<ServerInfo> serverInfos = new LinkedList<ServerInfo>();
		
		int[] mcmfPre = new int[Global.mcmfNodeNum];
		
		int minFlow;
		int sumFlow = 0;
		int s = Global.sourceNode;
		int t = Global.endNode;
	
		while(spfa(s,t,mcmfPre)){
					
			minFlow = Integer.MAX_VALUE;
			for(int i=t;i!=s;i=mcmfPre[i]){
				if(Global.graph[mcmfPre[i]][i].leftBandWidth<minFlow){
					minFlow = Global.graph[mcmfPre[i]][i].leftBandWidth;
				}
			}
			
			sumFlow+=minFlow;
			for(int i=t;i!=s;i=mcmfPre[i]){
				Global.graph[mcmfPre[i]][i].leftBandWidth-=minFlow;
				if(Global.graph[i][mcmfPre[i]]==null){
					System.out.println(i+" "+mcmfPre[i]);
				}
				Global.graph[i][mcmfPre[i]].leftBandWidth+=minFlow;
				// minCost += cost[mcmfPre[i]][i]*minFlow;
			}
			//minCost += dist[t]*minFlow;
			
			// 保存服务信息,从消费者开始的 -> 服务器
			LinkedList<Integer> nodes = new LinkedList<Integer>();
			for(int i=mcmfPre[t];i!=s;i=mcmfPre[i]){
				nodes.add(i);
			}
			int consumerId = Global.nodeToConsumerId.get(nodes.getFirst());
			int[] viaNodes = new int[nodes.size()];
			int index = 0;
			for(int node : nodes){
				viaNodes[index++] = node;
			}
			ServerInfo serverInfo = new ServerInfo(consumerId, minFlow, viaNodes);
			serverInfos.add(serverInfo);
		}
		
		if(sumFlow>=Global.consumerTotalDemnad){
			return serverInfos;
		}else{
			return null;
		}
	}

	private boolean spfa(int s, int t,int[] mcmfPre) {
		
		int[] dist = new int[Global.mcmfNodeNum];
		boolean vis[] = new boolean[Global.mcmfNodeNum];
		Queue<Integer> q = new LinkedList<Integer>();
		for (int i = 0; i <= t; i++) {
			vis[i] = false;
			dist[i] = Global.INFINITY;
		}
		vis[s] = true;
		dist[s] = 0;
		q.offer(s);
		while (!q.isEmpty()) {
			int u = q.poll();
			vis[u] = false;
			for (int v = 0; v <= t; v++) {
				if (Global.graph[u][v]!=null&&
						Global.graph[u][v].leftBandWidth > 0
						&& dist[v] > dist[u] + Global.graph[u][v].cost) {
					dist[v] = dist[u] + Global.graph[u][v].cost;
					mcmfPre[v] = u;
					if (!vis[v]) {
						q.offer(v);
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
