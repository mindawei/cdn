package com.cacheserverdeploy.deploy;

/**
 * 网络构建
 * @author mindw
 * @date 2017年3月10日
 */
public final class Parser {
	
	public static int edgeIndex = 0;
	
	/** 构建图 */
	public static void buildNetwork(String[] graphContent){
		 
		// 多少个网络节点，多少条链路，多少个消费节点
		String[] line0 = graphContent[0].split(" ");
		/** 网络节点数：不超过1000个 */
		int nodeNum = Integer.parseInt(line0[0]);
		int mcmfNodeNum = nodeNum+2;
		Global.nodeNum = nodeNum;
		Global.mcmfNodeNum = mcmfNodeNum;
		Global.graph = new Edge[mcmfNodeNum][mcmfNodeNum];
	
		// 多源多汇问题，定义一个超级源点s,一个超级汇点e
		Global.sourceNode =nodeNum;
		Global.endNode = nodeNum+1;
	
		/** 链路数：每个节点的链路数量不超过20条，推算出总共不超过20000 */
		int edgeNum = Integer.parseInt(line0[1]);
		
		/** 消费节点数：不超过500个 */
		int consumerNum=  Integer.parseInt(line0[2]);
		Global.consumerNum = consumerNum;
		Global.consumerNodes = new int[consumerNum];
		Global.consumerDemands = new int[consumerNum];
		
		// 多个边加上超级汇点
		Global.edges = new Edge[edgeNum*2+consumerNum*2];
		
		// 空行
		
		// 每台部署的成本
		String line2 = graphContent[2];
		int depolyCostPerServer = Integer.parseInt(line2);
		Global.depolyCostPerServer = depolyCostPerServer;
		
		// 空行
		int lineIndex  = 4;
		String line = null;
		while(!(line=graphContent[lineIndex++]).isEmpty()){	
			buildEdge(line);
		}
		
		// 空行
		
		// 消费节点，相连网络节点ID，视频带宽消耗需求
		for (int index = lineIndex; index < graphContent.length; ++index) {
			line = graphContent[index];
			buildConsumer(line);
		}
		
		//if(Global.IS_DEBUG){
		//	String info = String.format("节点数：%d,消费节点数：%d,每台部署成本：%d", nodeNum,consumerNum, depolyCostPerServer);
		//	System.out.println(info);
		//}
		
	}

	/** 
	 * 构建条边 <br>
	 * @param line 每行：链路起始节点ID 链路终止节点ID 总带宽大小 单位网络租用费
	 */
	private static void buildEdge(String line){
		
		String[] strs = line.split(" ");
		
		// 链路起始节点
		int fromNode = Integer.parseInt(strs[0]);
		//  链路终止节点
		int toNode = Integer.parseInt(strs[1]);
		
		// 总带宽大小 
		int bandwidth = Integer.parseInt(strs[2]);
		// 单位网络租用费
		int cost = Integer.parseInt(strs[3]);
		
		// 为每个方向上都建立一条边，创建过程中负责相关连接
		Edge goEdege = new Edge(bandwidth, cost);
		Global.edges[edgeIndex++] = goEdege;
		Global.graph[fromNode][toNode] = goEdege;
		
		Edge backEdge = new Edge(bandwidth, cost);
		Global.edges[edgeIndex++] = backEdge;
		Global.graph[toNode][fromNode] = backEdge;
		
		// 最小费用最大流数据部分
//		Global.mcmfBandWidth[fromNode][toNode] = bandwidth;
//		Global.mcmfBandWidth[toNode][fromNode] = bandwidth;
//		
//		Global.mcmfCost[fromNode][toNode] = cost;
//		Global.mcmfCost[toNode][fromNode] = cost;
	
	}
	
	/**
	 * 构建消费节点
	 * @param line 消费节点 相连网络节点ID 视频带宽消耗需求 
	 */
	private static void buildConsumer(String line){
		String[] strs = line.split(" ");
		int consumerId = Integer.parseInt(strs[0]);
		int node = Integer.parseInt(strs[1]);
		int demand = Integer.parseInt(strs[2]);
		Global.consumerNodes[consumerId] = node;
		Global.consumerDemands[consumerId] = demand;
		Global.consumerTotalDemnad += demand;
		
		Global.nodeToConsumerId.put(node, consumerId);

		// 定义消费节点到超级汇点的带宽 消费节点的需求带宽
		Edge goEdege = new Edge(demand, 0);
		Global.edges[edgeIndex++] = goEdege;
		Global.graph[node][Global.endNode] = goEdege;
		
		Edge backEdge = new Edge(0, 0);
		Global.edges[edgeIndex++] = backEdge;
		Global.graph[Global.endNode][node] = backEdge;
		
		
	}
}
