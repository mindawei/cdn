package com.cacheserverdeploy.deploy;

import java.util.HashMap;
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
		Global.reset(); // 开始的服务器
		Server[] oldServers = new Server[Global.servers.size()];
		for(int i=0;i<Global.servers.size();++i){
			oldServers[i] = Global.servers.get(i);
		}
		
		Router.transfer(oldServers, newServers);
		
		for (Server oldServer : oldServers) {
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
	
	
	


	
}
