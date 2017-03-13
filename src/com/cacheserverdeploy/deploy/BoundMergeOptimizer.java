package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;


/**
 * 在边界进行优化
 * @author mindw
 * @date 2017年3月11日
 */
public final class BoundMergeOptimizer implements Optimizer{
	
	private final Optimizer previousOptimizer;
	public BoundMergeOptimizer(Optimizer previousOptimizer){
		this.previousOptimizer = previousOptimizer;
	}
	
	/** 
	 * 简单的思路：只是在边界合并 
	 */
	public void optimize() {
		
		if(previousOptimizer!=null){
			previousOptimizer.optimize();
		}
		
		while (true) {
			int bestIndex = -1;
			int minCost = 0;
			for (int startIndex = 0; startIndex < Global.servers.size(); ++startIndex) {
				Global.save();
				int cost = merge(startIndex);
				if (cost < minCost) {
					minCost = cost;
					bestIndex = startIndex;
				}
				Global.goBack();
			}

			if (bestIndex != -1) {
				merge(bestIndex);
				Global.updateSolution();
			} else {
				break;
			}
		}
	}
	
	/**
	 *  尝试合并
	 *  @return 
	 */
	private static int merge(int startIndex) {
		
		// 安装Server的NodeId -> Server
		Map<String,Server> newServers = new HashMap<String, Server>();
		
		// 需要移除的服务器
		List<Server> needRemoveOldServers = new ArrayList<Server>();
		
		// 安装一台机器的费用
		int mergeCost = 0;
		
		for(int i=0;i<Global.servers.size();++i){
			
			int index = (startIndex+i)%Global.servers.size();
			Server oldServer = Global.servers.get(index);
			
			// 分发流量
			// 服务器
			mergeCost += transfer(oldServer,newServers);
			if(oldServer.getDemand()==0){
				mergeCost -= Global.depolyCostPerServer;
				needRemoveOldServers.add(oldServer);
				break;
			}
				
			// 增加本地服务器解决需求
			if(oldServer.getDemand()>0){
				// 将本地列为服务器，没有费用：增加和减少的费用抵消，转移也没有费用
				needRemoveOldServers.add(oldServer);
				newServers.put(oldServer.nodeId, oldServer);
			}
			
		}
		
		Global.servers.removeAll(needRemoveOldServers);
		Global.servers.addAll(newServers.values());
		
		if(Global.IS_DEBUG){
			System.out.println("mergeCost:"+mergeCost);
		}
		return mergeCost;
	}
		
	/** 
	 * 尽可能地转移需求 <br>
	 * 转移后会改变fromServer 和 toServer的状态<br>
	 * @return 转移部分的网络花费，不成功或者就在本地时返回0
	 */
	private static int transfer(Server fromServer,Map<String,Server> toServers){
		
		int bandWidthCost = 0;
		
		String fromNode = fromServer.nodeId;
		
		Map<String,TransferInfo> toServerCost = Router.getToServerCost(fromNode,
				fromServer.getDemand(),
				toServers.keySet());
		
		for(Map.Entry<String, TransferInfo> entry : toServerCost.entrySet()){
			String nodeId = entry.getKey();
			Server server = toServers.get(nodeId);
			TransferInfo transferInfo = entry.getValue();
			bandWidthCost += fromServer.transferTo(server,transferInfo);
		}

		return bandWidthCost;
	}

}
