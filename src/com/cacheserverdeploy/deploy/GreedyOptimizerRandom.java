package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * 防止在局部最优中出不来
 * 
 * @author mindw
 * @date 2017年3月23日
 */
public final class GreedyOptimizerRandom extends GreedyOptimizer{
	
	/** 频率大于0的点 */
	private int[] nodes;
	
	/** 候选节点是否被选中 */
	private final boolean[] selected;
	
	/** 每次最多随机选多少个*/
	private final int selectNum;
	/** 下一轮的服务器*/
	private ArrayList<Server> nextRoundServers;
	
	private final Random random = new Random(47);
	
	/**
	 * 构造函数
	 * @param nearestK 初始化的时候选每个消费者几个最近领
	 * @param selectNum 随机生成的时候服务器个数
	 */
	public GreedyOptimizerRandom(int nearestK,int selectNum){
		this.selectNum = selectNum;
		initNodes(nearestK);
		selected = new boolean[nodes.length];
		nextRoundServers = new ArrayList<Server>(Global.consumerNum);
	}
	
	/** 随机选择服务器 ,改变{@link #nextRoundServers}*/
	private void randomSelectServers() {
		
		Arrays.fill(selected, false);
		nextRoundServers.clear();
		
		int leftNum = selectNum;
		// 肯定是服务器的
		for(int node : Global.mustServerNodes){
			nextRoundServers.add(new Server(node));
		}
		leftNum -= Global.mustServerNodes.length;
		// 随机选择
		while(leftNum>0){
			int index = random.nextInt(nodes.length);
			// 没有被选过
			if(!selected[index]){
				int node = nodes[index];
				if(Global.isMustServerNode[node]){
					//  是服务器，服务器上面已经添加过了
					selected[index] = true;
				}else{ 
					selected[index] = true;
					nextRoundServers.add(new Server(node));
					leftNum--;
				}
			}
		}
	}

	@Override
	void optimize() {

		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}

		if (Global.isTimeOut()) {
			return;
		}

		long t = System.currentTimeMillis();
			
		randomSelectServers();
		
		int lastCsot = Global.INFINITY;
		
		while (true) {
				
			if (Global.IS_DEBUG) {
				for(Server server : nextRoundServers){
					System.out.print(server.node+" ");
				}
				System.out.println();
			}

			// 可选方案
			int minCost = Global.INFINITY;
			int bestFromNode = -1;
			int bestToNode = -1;

			// System.out.println(oldGlobalServers.size()*oldGlobalServers.size());
			
			 int toNums = 2000 / nextRoundServers.size();
			
			//boolean findBetter = false;
			for (Server oldServer : nextRoundServers) {
				
				int fromNode = oldServer.node;
				
				// 服务器不移动
				if(Global.isMustServerNode[fromNode]){
					continue;
				}
				 int leftNum = toNums;
				// for (Server toServer : nextRoundServers) {
				// int toNode = toServer.node;
				for (int toNode : nodes) {
					
					// 防止自己到自己
					if (fromNode == toNode) {
						continue;
					}

					if (Global.isTimeOut()) {
						return;
					}

					ArrayList<Server> newServers = move(nextRoundServers, fromNode, toNode);
					int cost = Global.getTotalCost(newServers);
					if (cost < minCost) {
						minCost = cost;
						bestFromNode = fromNode;
						bestToNode = toNode;
//						findBetter = true;
//						break;
					}
					
					leftNum--;
					if(leftNum==0){
						break;
					}
				}
				
//				if(findBetter){
//					break;
//				}
			}

			if (minCost == Global.INFINITY) {
				break;
			}

			if (Global.isTimeOut()) {
				return;
			}
			
			// 移动
			nextRoundServers = move(nextRoundServers, bestFromNode, bestToNode);
			int cost = Global.getTotalCost(nextRoundServers);
		
			if (cost<lastCsot) {
				lastCsot = cost;
				Global.updateSolution(nextRoundServers);
			}else{ // not better
				lastCsot = Global.INFINITY;
				randomSelectServers();
			}
			
		}

		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 结束，耗时: " + (System.currentTimeMillis() - t));
		}
		
	}
	
	/////////////////////////////
	
	private final class Info{
		/** 花费 */
		final int cost;
		/** 前向指针 */
		final int[] preNodes;
		/** 目标节点 */
		final int toNode;
		
		public Info(int cost, int[] preNodes,int toNode) {
			super();
			this.cost = cost;
			this.preNodes = preNodes;
			this.toNode = toNode;
		}	
	}
	
	private final class NodeFreq{
		final int node;
		int freqs;
		
		public NodeFreq(int node, int freqs) {
			super();
			this.node = node;
			this.freqs = freqs;
		}
	}
	
	private void initNodes(int nearestK) {
		
		// 不会超过消费者数
		nearestK = Math.min(nearestK, Global.nodeNum-1);
		
		// 节点频率
		NodeFreq[] nodeFreqs = new NodeFreq[Global.nodeNum];
		for(int node=0;node<Global.nodeNum;++node){
			nodeFreqs[node] = new NodeFreq(node,0);
		}
		
		for(int consumerId =0 ;consumerId<Global.consumerNum;++consumerId){
			
			int fromConsumerNode = Global.consumerNodes[consumerId];
			
			// 最大堆
			PriorityQueue<Info> priorityQueue = new PriorityQueue<Info>(nearestK+1,new Comparator<Info>() {
				@Override
				public int compare(Info o1, Info o2) {
					return o2.cost-o1.cost;
				}
			});
			
			for(int toConsumerNode : Global.consumerNodes){
				// 自己不到自己
				if(toConsumerNode==fromConsumerNode){
					continue;
				}
				int cost = Global.allCost[consumerId][toConsumerNode];
				int[] preNodes = Global.allPreNodes[consumerId];
				Info info = new Info(cost, preNodes, toConsumerNode);
				priorityQueue.add(info);
				if(priorityQueue.size()>nearestK){
					// 去掉最大的，保留 k个最小的
					priorityQueue.poll();
				}
			}
			
			// 统计频率
			Info info  = null;
			while((info=priorityQueue.poll())!=null){
				int[] preNodes = info.preNodes;
				for(int node=info.toNode;node!=-1;node=preNodes[node]){
					nodeFreqs[node].freqs++;
				}
			}
		}
		
		// 按频率从大到小排
		Arrays.sort(nodeFreqs,new Comparator<NodeFreq>() {
			@Override
			public int compare(NodeFreq o1, NodeFreq o2) {	
				return o2.freqs - o1.freqs;
			}
		});
		
		int[] selectedNodes = new int[Global.nodeNum];
		int len = 0;
		for(NodeFreq nodeFreq : nodeFreqs ){
			if(nodeFreq.freqs>0){
				selectedNodes[len++] = nodeFreq.node;
			}else{
				break;
			}
		}
		this.nodes = new int[len];
		System.arraycopy(selectedNodes, 0, this.nodes, 0, len);
		
		if (Global.IS_DEBUG) {
			System.out.println("频率大于0的初始节点有"+nodes.length+"个");
//			System.out.println("频率大于0的初始节点有"+nodes.length+"个，具体如下：");
//			for(int node : nodes){
//				System.out.print(node+" ");
//			}
//			System.out.println();
		}
	}
	
	/// 原来Simple部分
	
	
	@Override
	protected ArrayList<Server> transferServers(Server[] newServers) {
	
		ArrayList<Server> nextGlobalServers = new ArrayList<Server>();
		for(int consumerId=0;consumerId<Global.consumerNum;++consumerId){	
			
			int consumerNode = Global.consumerNodes[consumerId];
			int consumerDemand = Global.consumerDemands[consumerId];
			
			if(Global.isMustServerNode[consumerNode]){
				// 肯定是服务器不用转移
				nextGlobalServers.add(new Server(consumerId,consumerNode,consumerDemand));
				continue;
			}
			
			// 将起始点需求分发到目的地点中，会改变边的流量	
			for(int node : Global.allPriorityCost[consumerId]){
				// 不是服务器
				if(newServers[node]==null){
					continue;
				}
				
				int usedDemand = useBandWidthByPreNode(consumerDemand, node,Global.allPreNodes[consumerId]);
				// 可以消耗
				if (usedDemand > 0) {	
					transferTo(consumerId, newServers[node], usedDemand,node, Global.allPreNodes[consumerId]);
					consumerDemand -= usedDemand;
					if(consumerDemand==0){
						break;
					}
				}
			}
			
			if (consumerDemand>0) {
				nextGlobalServers.add(new Server(consumerId,consumerNode,consumerDemand));
			}
			
		}
		
		for(int node =0;node<Global.nodeNum;++node){
			if(newServers[node]==null){
				continue;
			}
			Server newServer = newServers[node];
			if(newServer.getDemand()>0){
				nextGlobalServers.add(newServer);
			}
		}
		return nextGlobalServers;
	}

}
