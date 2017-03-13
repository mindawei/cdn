package com.cacheserverdeploy.deploy;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 启发式搜素：当前全局的服务费用最下
 * 
 * @author mindw
 * @date 2017年3月12日
 */
public class HeuristicOptimizer implements Optimizer {

	// 接下来可以走的地方
	class Pair{
		String oldServerNodeId;
		String newServerNodeId;
		public Pair(String oldServerNodeId, String newServerNodeId) {
			super();
			this.oldServerNodeId = oldServerNodeId;
			this.newServerNodeId = newServerNodeId;
		}
		@Override
		public String toString() {
			return "Pair [oldServerNodeId=" + oldServerNodeId
					+ ", newServerNodeId=" + newServerNodeId + "]";
		}
		
	}
	
	/** 存活周期 ,单位：推进次数  */	
	private static final int LIVE_TIME = 10;
	
	/**
	 * 简单的思路：只是在边界合并
	 */
	public void optimize() {
		
		Map<String,Integer> visitedNodes = new HashMap<String,Integer>();
		for(Server server : Global.servers){
			visitedNodes.put(server.nodeId,LIVE_TIME);
		}
		
		Map<String,Integer> moveNumbers = new HashMap<String,Integer>();
		for(String nodeId : Global.nodes){
			moveNumbers.put(nodeId, 0);
		}
		
		final long TIME_OUT = 5 * 1000;
		long startT = System.currentTimeMillis();
		while(true) {
			// 可选方案
			List<Pair> pairs = new LinkedList<Pair>();
			for(Server server : Global.servers){
				// 获得可以移动的点
				Set<String> toNodeIds = Global.getToNodeIds(server.nodeId);
				for(String toNodeId : toNodeIds){
					// 排除移动
					if(!visitedNodes.containsKey(toNodeId)){
						pairs.add(new Pair(server.nodeId, toNodeId));
					}
				}
			}
			// 无可选方案
			if(pairs.size()==0){
				break;
			}

			Pair bestNextPair = null;
			int minCost = Global.INFINITY;
			
			for (Pair nextPair : pairs) {
				Global.save();
				// 启发函数： 花费 + 这个点的移动频率
				int cost = move(nextPair) + moveNumbers.get(nextPair.oldServerNodeId) * 10;
				if (cost < minCost) {
					minCost = cost;
					bestNextPair = nextPair;
				}
				Global.goBack();
			}
			

			if (bestNextPair != null) {
				// 移动
				move(bestNextPair);
				// 更新该点的移动值
				// int moveNumber = moveNumbers.get(bestNextPair.oldServerNodeId);
				// moveNumbers.put(bestNextPair.oldServerNodeId,moveNumber+1);
				Global.updateSolution();
				visitedNodes.put(bestNextPair.newServerNodeId,LIVE_TIME);
			} else {
				break;
			}
		
			long endT = System.currentTimeMillis();
			if(endT-startT>=TIME_OUT){
				break;
			}
			
			List<String> needRemovedVisitedNodeIds = new LinkedList<String>(); 
			for(Map.Entry<String, Integer> entry : visitedNodes.entrySet()){
				String nodeId = entry.getKey();
				int liveTime = entry.getValue();
				liveTime-=1;
				if(liveTime<=0){
					needRemovedVisitedNodeIds.add(nodeId);
				}else{
					visitedNodes.put(nodeId, liveTime);
				}
			}
			
			for(String nodeId : needRemovedVisitedNodeIds){
				visitedNodes.remove(nodeId);
			}
			
		}
		
	}

	/**
	 * 尝试合并
	 * 
	 * @return
	 */
	private static int move(Pair pair) {
		
		Server oldServer= null;
		Server newServer = null;

		// 替换旧的Server
		Map<String, Server> newServers = new HashMap<String, Server>();
		for (Server server : Global.servers){
			newServers.put(server.nodeId, server);
			if(server.nodeId.equals(pair.oldServerNodeId)){
				oldServer = server;
			}
			if(server.nodeId.equals(pair.newServerNodeId)){
				newServer = server;
			}
		}
		newServers.remove(pair.oldServerNodeId);
		
		if(newServer==null){
			newServer= new Server(pair.newServerNodeId);
			newServers.put(pair.newServerNodeId,newServer);
		}
		// 拆一台装一台没有费用
		int mergeCost = 0;	

		mergeCost += transfer(oldServer, newServers);
		if (oldServer.getDemand() == 0) {			// 真正拆除
			Global.servers.remove(oldServer);
		}else{
			// 部署一台
			mergeCost += Global.depolyCostPerServer;
		}
		
		if(newServer.getDemand()==0){
			// 拆掉不需要
			mergeCost -= Global.depolyCostPerServer;
		}else{ // > 0
			// 真正安装
			Global.servers.add(newServer);
		}

		if (Global.IS_DEBUG) {
			System.out.println("当前数目："+Global.servers.size());
			System.out.println("mergeCost:" + mergeCost);
		}
		return mergeCost;
	}

	/**
	 * 尽可能地转移需求 <br>
	 * 转移后会改变fromServer 和 toServer的状态<br>
	 * 
	 * @return 转移部分的网络花费，不成功或者就在本地时返回0
	 */
	private static int transfer(Server fromServer, Map<String, Server> toServers) {

		int bandWidthCost = 0;

		String fromNode = fromServer.nodeId;

		Map<String, TransferInfo> toServerCost = Router.getToServerCost(
				fromNode, fromServer.getDemand(), toServers.keySet());

		for (Map.Entry<String, TransferInfo> entry : toServerCost.entrySet()) {
			String nodeId = entry.getKey();
			Server server = toServers.get(nodeId);
			TransferInfo transferInfo = entry.getValue();
			bandWidthCost += fromServer.transferTo(server, transferInfo);
		}

		return bandWidthCost;
	}

}
