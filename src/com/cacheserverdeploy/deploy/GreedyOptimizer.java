package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElementDecl.GLOBAL;

/**
 * 启发式搜素：当前全局的服务费用最下
 * 
 * @author mindw
 * @date 2017年3月12日
 */
public class GreedyOptimizer implements Optimizer {

	private final Optimizer previousOptimizer;
	
	public GreedyOptimizer(Optimizer previousOptimizer){
		this.previousOptimizer = previousOptimizer;
	}
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
	private static int LIVE_TIME = 1;
	
	// private static int nearestK = Global.nodes.size();
	
	/**
	 * 简单的思路：只是在边界合并
	 */
	public void optimize() {
		
		if(previousOptimizer!=null){
			previousOptimizer.optimize();
		}
		
		Map<String,Integer> visitedNodes = new HashMap<String,Integer>();
		for(Server server : Global.servers){
			visitedNodes.put(server.nodeId,LIVE_TIME);
		}
	
	
		while(true) {
			// 可选方案
			List<Pair> pairs = new LinkedList<Pair>();
			for(Server server : Global.servers){
				// 获得可以移动的点
				// Set<String> toNodeIds = Router.getNearestK(Router.getToNodeCost(server.nodeId), nearestK);
				
				for(String toNodeId : Global.nodes){
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
				int cost = move(nextPair);
				if (cost < minCost) {
					minCost = cost;
					bestNextPair = nextPair;
				}
				Global.goBack();
			}
			

			if (bestNextPair != null) {
				// 移动
				move(bestNextPair);
				Global.updateSolution();
				visitedNodes.put(bestNextPair.newServerNodeId,LIVE_TIME);
			} else {
				break;
			}
	
			if(Global.isTimeOut()){
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
		
		// 替换旧的Server
		Map<String, Server> newServers = new HashMap<String, Server>();
		
		for (Server server : Global.servers){
			if(!server.nodeId.equals(pair.oldServerNodeId)){
				newServers.put(server.nodeId, new Server(server.nodeId));
			}	
		}	
		newServers.put(pair.newServerNodeId,new Server(pair.newServerNodeId));
	
		// 拆一台装一台没有费用
		int mergeCost = 0;	
		
		List<Server> oldServers = new ArrayList<Server>(Global.servers);
		for (Server oldServer : oldServers) {
			mergeCost += transfer(oldServer, newServers);
			if (oldServer.getDemand() == 0) { // 真正拆除
				Global.servers.remove(oldServer);
			} else {
				// 部署一台
				mergeCost += Global.depolyCostPerServer;
			}
		}
		
		for(Server newServer : newServers.values()){
			if (newServer.getDemand() == 0) {
				// 拆掉不需要
				mergeCost -= Global.depolyCostPerServer;
			} else { // > 0
				// 真正安装
				Global.servers.add(newServer);
			}
		}
		if (Global.IS_DEBUG) {
			System.out.println("当前数目：" + Global.servers.size());
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
