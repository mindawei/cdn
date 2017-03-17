package com.cacheserverdeploy.deploy;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/** 服务器 */
public final class Server {

	/** 放置的位置 */
	public final int nodeId;
	
	/** 服务的消费者 */
	public final List<ServerInfo> serverInfos = new LinkedList<ServerInfo>();

	/** 复制 */
	public Server copy() {
		Server newCopyServer = new Server(nodeId);
		for(ServerInfo serverInfo : serverInfos){
			newCopyServer.serverInfos.add(serverInfo.copy());
		}
		return newCopyServer;
	}

	/** 
	 * 转移到另一个服务器，并返回价格<br>
	 * 注意：可能cost会改变(又返回之前的点)
	 */
	public void transferTo(Server toServer, TransferInfo transferInfo) {
		
		int leftTransferBandWidth = transferInfo.avaliableBandWidth;

		LinkedList<ServerInfo> needRemoveServerInfos = new LinkedList<ServerInfo>();
		
		for(ServerInfo localServerInfo : serverInfos){
			
			// 剩余要传的的和本地的最小值
			int transferBandWidth = Math.min(leftTransferBandWidth, localServerInfo.provideBandWidth);
			
			// 转移路径节点数
			int transferNodesSize = transferInfo.viaNodes.length;
			// 当前服务路径节点数
			int nodeSize = localServerInfo.viaNodes.length;
			// 虽然分配了，但是新的部分目前为0
			int[] nodes = Arrays.copyOf(localServerInfo.viaNodes, nodeSize+transferNodesSize);
			
			// 去头
			for(int pos = 1;pos<transferNodesSize;++pos){
				int transferNode = transferInfo.viaNodes[pos];
				
				// 重复的下标
				int repeatIndex = -1;
				for(int index=0;index<nodeSize;++index){
					if(nodes[index]==transferNode){
						repeatIndex = index;
						break;
					}
				}
				if(repeatIndex==-1){ 
					// 不存在回路，直接添加	
					nodes[nodeSize++] = transferNode;
				}else{ // repeatIndex!=-1  
					// 存在回路
					
					// 添加节点,并改变大小
					nodes[nodeSize++] = transferNode;
					
					// 删除回路影响,恢复消耗的带宽,反方向
					for (int i = nodeSize-1; i >=repeatIndex+1; --i) {
						Edge edge = Global.graph[nodes[i]][nodes[i -1]];	
						edge.leftBandWidth += transferBandWidth;
					}
					
					// 删除回路,指针前移到重复位置吗，之后的不要 [0,repeatIndex]
					nodeSize = repeatIndex+1;
				}	
			}
		
			ServerInfo toServerInfo = new ServerInfo(localServerInfo.consumerId,transferBandWidth,Arrays.copyOf(nodes, nodeSize));
			toServer.serverInfos.add(toServerInfo);
			// 更新当前的
			localServerInfo.provideBandWidth -= transferBandWidth;
			if(localServerInfo.provideBandWidth==0){ // 已经全部转移
				needRemoveServerInfos.add(localServerInfo);
			}
			
			
			leftTransferBandWidth -= transferBandWidth;
			// 已经完成
			if(leftTransferBandWidth==0){
				break;
			}
			
		}
		serverInfos.removeAll(needRemoveServerInfos);
	}
	
	/** 获得服务器所有的需求 */
	public int getDemand() {
		int demand = 0;
		for(ServerInfo info : serverInfos){
			demand += info.provideBandWidth;
		}
		return demand;
	}
	
	public Server(int nodeId){
		super();
		this.nodeId = nodeId;
	}
	
	public Server(Integer consumerId,int nodeId,int demand) {
		super();
		this.nodeId = nodeId;
		serverInfos.add(new ServerInfo(consumerId,demand,new int[]{nodeId}));
	}

	/**
	 * 网络节点ID-01 网络节点ID-02 …… 网络节点ID-n 消费节点ID 占用带宽大小
	 */
	public List<String> getSolution() {
		List<String> solution = new LinkedList<String>();
		for(ServerInfo serverInfo : serverInfos){
			StringBuilder builder = new StringBuilder();
			for(int i=serverInfo.viaNodes.length-1;i>=0;--i){
				builder.append(serverInfo.viaNodes[i]);
				builder.append(" ");
			}
			builder.append(serverInfo.consumerId);
			builder.append(" ");
			builder.append(serverInfo.provideBandWidth);

			solution.add(builder.toString());
		}
		return solution;
	}

	/** 获得总的带宽费用费用数 */
	public int getCost() {
		int toatlCost = Global.depolyCostPerServer;
		for(ServerInfo serverInfo : serverInfos){
			toatlCost += serverInfo.getBandWidthCost();
		}
		return toatlCost;
	}

	@Override
	public boolean equals(Object obj) {
		Server other = (Server)obj;
		return nodeId == other.nodeId;
	}
}
