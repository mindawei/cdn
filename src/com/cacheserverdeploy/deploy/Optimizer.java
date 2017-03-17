package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 优化器应该实现的接口 
 * 
 * @author mindw
 * @date 2017年3月12日
 */
public abstract class Optimizer {
	
	/** 优化，优化过后会改变全局状态 */
	abstract void optimize();
	
	final void move(int[] arr) {
		Map<Integer, Server> newServers = new HashMap<Integer, Server>();
		for (int nodeId =0;nodeId<arr.length;++nodeId){
			if(arr[nodeId]==1){
				newServers.put(nodeId, new Server(nodeId));
			}	
		}	
		move(newServers);
	}
	
	/** 移动 */
	final void move(MoveAction moveAction) {
		// 替换旧的Server
		Map<Integer, Server> newServers = new HashMap<Integer, Server>();
		
		for (Server server : Global.servers){
			if(server.nodeId != moveAction.oldServerNodeId){
				newServers.put(server.nodeId, new Server(server.nodeId));
			}	
		}	
		newServers.put(moveAction.newServerNodeId,new Server(moveAction.newServerNodeId));
	
		move(newServers);
	}

	
	private void move(Map<Integer, Server> newServers){
		// 拆一台装一台没有费用
		// int mergeCost = 0;	
		Global.reset(); // 开始的服务器
		List<Server> oldServers = new ArrayList<Server>(Global.servers);
		for (Server oldServer : oldServers) {
			transfer(oldServer, newServers);
			if (oldServer.getDemand() == 0) { // 真正拆除
				Global.servers.remove(oldServer);
			} 
		}
		
		for(Server newServer : newServers.values()){
			if (newServer.getDemand() > 0) { // 真正安装
				Global.servers.add(newServer);
			}
		}
	}
	
//	private void move(Map<Integer, Server> newServers){
//		// 拆一台装一台没有费用
//		// int mergeCost = 0;	
//		List<Server> oldServers = new ArrayList<Server>(Global.servers);
//		for (Server oldServer : oldServers) {
//			transfer(oldServer, newServers);
//			if (oldServer.getDemand() == 0) { // 真正拆除
//				Global.servers.remove(oldServer);
//			} 
//		}
//		
//		for(Server newServer : newServers.values()){
//			if (newServer.getDemand() > 0) { // 真正安装
//				Global.servers.add(newServer);
//			}
//		}
//	}

	/**
	 * 尽可能地转移需求 <br>
	 * 转移后会改变fromServer 和 toServer的状态<br>
	 * 
	 * @return 转移部分的网络花费，不成功或者就在本地时返回0
	 */
	final void transfer(Server fromServer, Map<Integer, Server> toServers) {
		
		int fromNode = fromServer.nodeId;

		Map<Integer, TransferInfo> toServerCost = Router.getToServerCost(
				fromNode, fromServer.getDemand(), toServers.keySet());

		for (Map.Entry<Integer, TransferInfo> entry : toServerCost.entrySet()) {
			int nodeId = entry.getKey();
			Server server = toServers.get(nodeId);
			TransferInfo transferInfo = entry.getValue();
			fromServer.transferTo(server, transferInfo);
		}
		
	}
	
}
