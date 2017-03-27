package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 利用最大最小费用流进行优化s
 *
 * @author mindw
 * @date 2017年3月23日
 */
public final class MCMF{

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
			System.out.println( "MCMF 开始接管 ");
		}
		long t = System.currentTimeMillis();

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
		Global.resetEdgeBandWidth();
		transferServers(nextGlobalServers,newServers,lsNewServers,lsSize);
		Global.updateSolution(nextGlobalServers);
		
		if (Global.IS_DEBUG) {
			System.out.println("MCMF 结束，耗时: " + (System.currentTimeMillis() - t));
		}
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
	private static List<ServerInfo> mcmf(){
		
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
		
		if(sumFlow>=Global.consumerTotalDemnad){
			return serverInfos;
		}else{
			return null;
		}
	}

	private final static int[] mcmfFlow = new int[Global.mcmfNodeNum];
	private final static int[] mcmfPre = new int[Global.mcmfNodeNum];
	private final static boolean vis[] = new boolean[Global.mcmfNodeNum];
	private final static int[] dist = new int[Global.mcmfNodeNum];
	private final static int[] que = new int[Global.mcmfNodeNum];
	
	
	private static boolean spfa(int s, int t) {
		
		for (int i = 0; i <= t; i++) {
			vis[i] = false;
			mcmfFlow[i] = Global.INFINITY;
			dist[i] = Global.INFINITY;
		}
		
		// 左边指针 == 右边指针 时候队列为空
		// 左边指针，指向队首
		int queL = 0;
		// 右边指针，指向下一个插入的地方
		int queR = 0;
		vis[s] = true;
		dist[s] = 0;
		que[queR++] = s;

		while (queL!=queR) {
			int u = que[queL++];
			if(queL>= Global.mcmfNodeNum){
				queL = queL % Global.mcmfNodeNum;
			}
			
			vis[u] = false;
			// u -> v的边
			for (int v : Global.mfmcConnections[u]) {
				if (Global.graph[u][v].leftBandWidth > 0 && dist[v] > dist[u] + Global.graph[u][v].cost) {
					dist[v] = dist[u] + Global.graph[u][v].cost;
					mcmfFlow[v] = Math.min(mcmfFlow[u], Global.graph[u][v].leftBandWidth);
					mcmfPre[v] = u;
					if (!vis[v]) {
						que[queR++] = v;
						if(queR>= Global.mcmfNodeNum){
							queR = queR % Global.mcmfNodeNum;
						}
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
