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
		Map<String, Server> newServers = new HashMap<String, Server>();
		for (int i =0;i<arr.length;++i){
			if(arr[i]==1){
				String nodeId = String.valueOf(i);
				newServers.put(nodeId, new Server(nodeId));
			}	
		}	
		move(newServers);
	}
	
	/** 移动 */
	final void move(MoveAction moveAction) {
		
		// 替换旧的Server
		Map<String, Server> newServers = new HashMap<String, Server>();
		
		for (Server server : Global.servers){
			if(!server.nodeId.equals(moveAction.oldServerNodeId)){
				newServers.put(server.nodeId, new Server(server.nodeId));
			}	
		}	
		newServers.put(moveAction.newServerNodeId,new Server(moveAction.newServerNodeId));
	
		move(newServers);
	}
	
	private void move(Map<String, Server> newServers){
		// 拆一台装一台没有费用
		// int mergeCost = 0;	
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

	/**
	 * 尽可能地转移需求 <br>
	 * 转移后会改变fromServer 和 toServer的状态<br>
	 * 
	 * @return 转移部分的网络花费，不成功或者就在本地时返回0
	 */
	final void transfer(Server fromServer, Map<String, Server> toServers) {
		
		String fromNode = fromServer.nodeId;

		Map<String, TransferInfo> toServerCost = Router.getToServerCost(
				fromNode, fromServer.getDemand(), toServers.keySet());

		for (Map.Entry<String, TransferInfo> entry : toServerCost.entrySet()) {
			String nodeId = entry.getKey();
			Server server = toServers.get(nodeId);
			TransferInfo transferInfo = entry.getValue();
			fromServer.transferTo(server, transferInfo);
		}
		
	}
	
}
