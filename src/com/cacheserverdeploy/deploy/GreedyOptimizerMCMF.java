package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/** 
 * 复杂移动比较费时间
 */
public final class GreedyOptimizerMCMF extends GreedyOptimizer{

	@Override
	protected ArrayList<Server> transferServers(Server[] consumerServers, Map<Integer, Server> newServers) {


		List<ServerInfo> serverInfos = new LinkedList<ServerInfo>();
		// 源头
		Arrays.fill(Global.graph[Global.sourceNode], null);
		for(int toServerNode : newServers.keySet()){
			Global.graph[Global.sourceNode][toServerNode] = new Edge(Global.INFINITY, 0);
			Global.graph[toServerNode][Global.sourceNode] = new Edge(0, 0);
		}
		int mcmfMinCost = mcmf(serverInfos);

		if(mcmfMinCost<Global.INFINITY){
//			if(Global.IS_DEBUG){
//			System.out.println("better mcmfMinCost :"+mcmfMinCost+" minCost:"+Global.minCost);
//		}
//		String[] mcmfSoluttion = Global.getSolution(serverInfos);
//		Global.bsetSolution = mcmfSoluttion;
//		Global.printBestSolution(mcmfSoluttion);
			
			ArrayList<Server> nextGlobalServers = new ArrayList<Server>(newServers.size());
			for(ServerInfo serverInfo : serverInfos){
				int serverNode = serverInfo.viaNodes[serverInfo.viaNodes.length-1];
				Server newServer = newServers.get(serverNode);
				newServer.serverInfos.add(serverInfo);
			}
			for(Server newServer : newServers.values()){
				if(newServer.getDemand()>0){
					nextGlobalServers.add(newServer);
				}
			}
			
//			if(Global.IS_DEBUG){
//			System.out.println("better mcmfMinCost :"+(mcmfMinCost+nextGlobalServers.size()*Global.depolyCostPerServer)+" minCost:"+Global.minCost);
//		}
			return nextGlobalServers;
			
		}else{
//			if(Global.IS_DEBUG){
//				System.out.println("not better mcmfMinCost :"+mcmfMinCost+" minCost:"+Global.minCost);
//			}
			ArrayList<Server> nextGlobalServers = new ArrayList<Server>(consumerServers.length);
			for(Server consumerServer : consumerServers){
				nextGlobalServers.add(consumerServer);
			}
			return nextGlobalServers;
		}		
	
	}
	


	/**
	 * @return 返回费用,不存在解决方案则为无穷大
	 */
	private static int mcmf(List<ServerInfo> serverInfos){
		
		int[] dist = new int[Global.mcmfNodeNum];
		int[] mcmfPre = new int[Global.mcmfNodeNum];
		
		int minFlow;
		int sumFlow = 0;
		int minCost = 0;
		int s = Global.sourceNode;
		int t = Global.endNode;
	
		while(spfa(s,t,dist,mcmfPre)&&!Global.isTimeOut()){
					
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
			minCost += dist[t]*minFlow;
			
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
			return minCost;
		}else{
			return Global.INFINITY;
		}
	}

	private static boolean spfa(int s, int t,int[] dist,int[] mcmfPre) {
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
