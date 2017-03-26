package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


/**
 * 贪心搜索，寻找一个最优解，达到具备最优解后就马上退出
 * 
 * @author mindw
 * @date 2017年3月12日
 */
public abstract class GreedyOptimizer {
	
	/** 只优化一次 */
	public static final boolean  OPTIMIZE_ONCE = true;
	
	private boolean isOptimizeOnce = false;

	public GreedyOptimizer(boolean isOptimizeOnce){
		this.isOptimizeOnce = isOptimizeOnce;
	}
	
	protected boolean USE_UPDATE_NUM_LIMIT = false;
	protected int MAX_UPDATE_NUM = 3;
	protected int MIN_UPDATE_NUM = 9;
	
	public GreedyOptimizer(int maxUpdateNum,int minUpdateNum){
		this.MAX_UPDATE_NUM = maxUpdateNum;
		this.MIN_UPDATE_NUM = minUpdateNum;
	}
	
	/** 为了复用，为null的地方不放置服务器 */
	protected Server[] newServers = new Server[Global.nodeNum];
	/** 为了复用，下一轮的服务器，模拟队列，当遇到null时表示结束*/
	protected Server[] lsNewServers = new Server[Global.nodeNum];
	
	/** 为了复用，下一轮的服务器，模拟队列，当遇到null时表示结束*/
	protected Server[] nextGlobalServers = new Server[Global.nodeNum];
	
	void optimize() {
		
		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}

		long t = System.currentTimeMillis();

		// 本地移动一步,各个结果之间过渡的时候回漏掉一步，故添加该方法  
		moveLocal(Global.getBestServers());
		Global.updateSolution(nextGlobalServers);
		
		// 只优化一次
		if(isOptimizeOnce){
			if (Global.IS_DEBUG) {
				System.out.println(this.getClass().getSimpleName() + " 结束，耗时: " + (System.currentTimeMillis() - t));
			}
			return;
		}
		
		int maxUpdateNum = Global.INFINITY;
		
		while (true) {
			
			// 可选方案
			int minCost = Global.INFINITY;
			int bestFromNode = -1;
			int bestToNode = -1;

			if(Global.IS_DEBUG){
				System.out.println("maxUpdateNum:"+maxUpdateNum);
			}
			
			int updateNum =0;	
			boolean found = false;
			for (Server server : Global.getBestServers()) {
				if(server==null){
					break;
				}
				
				int fromNode = server.node;
				// 服务器不移动
				if(Global.isMustServerNode[fromNode]){
					continue;
				}
				
				for (int toNode = 0; toNode < Global.nodeNum; ++toNode) {
					
					if (Global.isTimeOut()) {
						return;
					}
					
					// 防止自己到自己
					if (fromNode == toNode) {
						continue;
					}
					
					move(Global.getBestServers(),fromNode, toNode);
					int cost = Global.getTotalCost(nextGlobalServers);
					if (cost < minCost) {
						minCost = cost;
						bestFromNode = fromNode;
						bestToNode = toNode;
						updateNum++;
						if(updateNum == maxUpdateNum){
							found = true;
							break;
						}
					}
				}
				
				if(found){
					break;
				}
			}

			if(maxUpdateNum==Global.INFINITY){
				maxUpdateNum = updateNum;
			}else if(maxUpdateNum<=updateNum){
				maxUpdateNum++;
				if(maxUpdateNum>MAX_UPDATE_NUM){
					maxUpdateNum = MAX_UPDATE_NUM;
				}
			}else{ // > updateNum
				maxUpdateNum--;
				if(maxUpdateNum<MIN_UPDATE_NUM){
					maxUpdateNum = MIN_UPDATE_NUM;
				}
			}
			
			if (minCost == Global.INFINITY) {
				break;
			}

			if (Global.isTimeOut()) {
				return;
			}
			
			// 移动
			moveMcmf(Global.getBestServers(),bestFromNode, bestToNode);
			//moveMcmf
			boolean better = Global.updateSolution(nextGlobalServers);

			if (!better) { // better
				break;
			}
			
		}

		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: " + (System.currentTimeMillis() - t));
		}
	}

	/** 进行一步移动 ,不要改变传进来的Server,结果缓存在 nextGlobalServers */
	protected void move(Server[] oldServers,int fromServerNode, int toServerNode) {
		Arrays.fill(newServers, null);
		int lsSize = 0;
		for (Server server : oldServers) {
			if(server==null){
				break;
			}
			if (server.node != fromServerNode) {
				Server newServer = new Server(server.node);
				newServers[server.node] = newServer;
				lsNewServers[lsSize++] = newServer;
			}
		}
		Server newServer = new Server(toServerNode);
		newServers[toServerNode] = newServer;
		lsNewServers[lsSize++] = newServer;
		
		Global.resetEdgeBandWidth();
		transferServers(newServers,lsNewServers,lsSize);
	}
	
	/** 本地移动一步,各个结果之间过渡的时候回漏掉一步，故添加该方法,结果缓存在 nextGlobalServers   */
	protected void moveLocal(Server[] oldGlobalServers) {
		Arrays.fill(newServers, null);
		int lsSize = 0;
		for (Server server : oldGlobalServers) {
			if(server==null){
				break;
			}
			Server newServer = new Server(server.node);
			newServers[server.node] = newServer;
			lsNewServers[lsSize++] = newServer;
		}
		Global.resetEdgeBandWidth();
		transferServers(newServers,lsNewServers,lsSize);
	}
	
	
	/** 本地移动一步,各个结果之间过渡的时候回漏掉一步，故添加该方法,结果缓存在 nextGlobalServers   */
	protected void moveMcmf(Server[] oldServers,int fromServerNode, int toServerNode) {
		Arrays.fill(newServers, null);
		int lsSize = 0;
		for (Server server : oldServers) {
			if(server==null){
				break;
			}
			if (server.node != fromServerNode) {
				Server newServer = new Server(server.node);
				newServers[server.node] = newServer;
				lsNewServers[lsSize++] = newServer;
			}
		}
		Server newServer = new Server(toServerNode);
		newServers[toServerNode] = newServer;
		lsNewServers[lsSize++] = newServer;
		
		Global.resetEdgeBandWidth();
		mcmftransferServers(newServers,lsNewServers,lsSize);
	}
	
	/** 
	 * 不同的搜索策略需要提供此方法 :总共 nodeNum个位置，结果缓存在 nextGlobalServers
	 * @param newServers 为null的表示没有服务器
	 * @param lsServers 全部往前移动，为了减少遍历
	 * @param lsSize lsServer的长度
	 */
	protected abstract void transferServers(Server[] newServers,Server[] lsServers,int lsSize);

	/** 
	 * 供子类调用：
	 * 转移到另一个服务器，并返回价格<br>
	 * 注意：可能cost会改变(又返回之前的点)
	 */
	protected void transferTo(int consumerId,Server toServer,int transferBandWidth,int serverNode,int[] preNodes) {
		
		///////////////////////
		// 适配： 指针 -> 数组
		// 计算长度
		int len = 0;
		int pre = serverNode;
		while(pre!=-1){
			len++;
			pre = preNodes[pre];
		}

		// 逐个添加
		int[] viaNodes = new int[len];
		pre = serverNode;
		while(pre!=-1){
			viaNodes[--len] = pre;	
			pre = preNodes[pre];
		}
		
		/////////////////////////
		
		// 剩余要传的的和本地的最小值
		ServerInfo toServerInfo = new ServerInfo(consumerId,transferBandWidth,viaNodes);
		toServer.addServerInfo(toServerInfo);
	}
	
	/**
	 * 消耗带宽最大带宽
	 * @return 消耗掉的带宽
	 */
	protected int useBandWidthByPreNode(int demand,int serverNode,int[] preNodes ) {
		int node1 = serverNode;
		int node0 = preNodes[node1];
		
		int minBindWidth = Global.INFINITY;
		while(node0!=-1){
			Edge edge = Global.graph[node1][node0];
			if(edge.leftBandWidth<minBindWidth){				
				minBindWidth = edge.leftBandWidth;
				if(minBindWidth==0){
					break;
				}
			}
			node1 = node0;
			node0 = preNodes[node0];
		}
		
//		if(minBindWidth==0){
//			System.out.println("0");
//		}else{
//			System.out.println("not 0");
//		}
		
		if (minBindWidth == 0) {
			return 0;
		}
		
		int usedBindWidth = Math.min(minBindWidth, demand);
		
		node1 = serverNode;
		node0 = preNodes[node1];
		while(node0!=-1){
			Edge edge = Global.graph[node1][node0];
			edge.leftBandWidth -= usedBindWidth;
			
			node1 = node0;
			node0 = preNodes[node0];
		}
		return usedBindWidth;
	}
	
	///
	
void mcmftransferServers(Server[] newServers,Server[] lsServers,int lsSize) {
		
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
