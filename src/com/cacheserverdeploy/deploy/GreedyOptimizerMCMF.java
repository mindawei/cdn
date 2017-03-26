package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 利用最大最小费用流进行优化s
 *
 * @author mindw
 * @date 2017年3月23日
 */
public final class GreedyOptimizerMCMF extends GreedyOptimizer{

	public GreedyOptimizerMCMF(int maxUpdateNum,int minUpdateNum){
		super(maxUpdateNum,minUpdateNum);
	}
	
	public GreedyOptimizerMCMF(boolean isOptimizeOnce){
		super(isOptimizeOnce);
	}
	
	@Override
	protected void transferServers(Server[] newServers,Server[] lsServers,int lsSize) {
		
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
	private final int[] nodes = new int[Global.nodeNum];
	private int nodeSize = 0;		
			
	/**
	 * @return 返回路由,不存在解决方案则为无穷大
	 */
	private List<ServerInfo> mcmf(){
		
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

	private final int[] mcmfFlow = new int[Global.mcmfNodeNum];
	private final int[] mcmfPre = new int[Global.mcmfNodeNum];
	private final boolean vis[] = new boolean[Global.mcmfNodeNum];
	private final int[] dist = new int[Global.mcmfNodeNum];
	private final int[] que = new int[Global.mcmfNodeNum];
	// 左边指针 == 右边指针 时候队列为空
	// 左边指针，指向队首
	private int queL;
	// 右边指针，指向下一个插入的地方
	private int queR;
	
	private boolean spfa(int s, int t) {
		
		for (int i = 0; i <= t; i++) {
			vis[i] = false;
			mcmfFlow[i] = Global.INFINITY;
			dist[i] = Global.INFINITY;
		}
		
		queL = 0;
		queR = 0;
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
