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
public final class GreedyOptimizerRandom extends GreedyOptimizerSimple{
	
	/** 频率大于0的点 */
	private int[] nodes;
	
	private final Random random = new Random(47);
	
	public GreedyOptimizerRandom(int topK){
		initNodes(topK);
	}
	
	/** 随机选择服务器 */
	private ArrayList<Server> randomSelectServers() {
		
		ArrayList<Server> nextServerNodes = new ArrayList<Server>(Global.consumerNum);
		
		boolean[] selected = new boolean[nodes.length];
		Arrays.fill(selected, false);
		int leftNum = Global.consumerNum / 4;
		for(int node : Global.mustServerNodes){
			nextServerNodes.add(new Server(node));
		}
		leftNum -= Global.mustServerNodes.length;
		
		while(leftNum>0){
			int index = random.nextInt(nodes.length);
			// 没有被选过
			if(!selected[index]){
				int node = nodes[index];
				if(Global.isMustServerNode[node]){
					selected[index] = true;
				}else{ //  是服务器，服务器上面已经添加过了
					selected[index] = true;
					nextServerNodes.add(new Server(node));
					leftNum--;
				}
			}
		}

		return nextServerNodes;
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

			
		ArrayList<Server> oldGlobalServers = randomSelectServers();
		
		int lastCsot = Global.INFINITY;
		
		while (true) {
				
			if (Global.IS_DEBUG) {
				for(Server server : oldGlobalServers){
					System.out.print(server.node+" ");
				}
				System.out.println();
			}

			// 可选方案
			int minCost = Global.INFINITY;
			int bestFromNode = -1;
			int bestToNode = -1;

			// System.out.println(oldGlobalServers.size()*oldGlobalServers.size());
			
			int toNums = 2000 / oldGlobalServers.size();
			
			for (Server oldServer : oldGlobalServers) {
				
				int fromNode = oldServer.node;
				
				// 服务器不移动
				if(Global.isMustServerNode[fromNode]){
					continue;
				}
				int leftNum = toNums;
				for (Server toServer : oldGlobalServers) {
					
					int toNode = toServer.node;
					
					// 防止自己到自己
					if (fromNode == toNode) {
						continue;
					}

					if (Global.isTimeOut()) {
						return;
					}

					ArrayList<Server> nextGlobalServers = move(oldGlobalServers, fromNode, toNode);
					int cost = Global.getTotalCost(nextGlobalServers);
					if (cost < minCost) {
						minCost = cost;
						bestFromNode = fromNode;
						bestToNode = toNode;
					}
					

					leftNum--;
					if(leftNum==0){
						break;
					}
				}
			}

			if (minCost == Global.INFINITY) {
				break;
			}

			if (Global.isTimeOut()) {
				return;
			}
			
			// 移动
			ArrayList<Server> nextGlobalServers = move(oldGlobalServers, bestFromNode, bestToNode);
			int cost = Global.getTotalCost(nextGlobalServers);
			//boolean better = Global.updateSolution(nextGlobalServers);

			if (cost<lastCsot) {
				Global.updateSolution(nextGlobalServers);
				oldGlobalServers = nextGlobalServers;
				lastCsot = cost;
			}else{ // not better
				lastCsot = Global.INFINITY;
				oldGlobalServers = randomSelectServers();
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

	private void initNodes(int topK) {
		
		// 不会超过消费者数
		topK = Math.min(topK, Global.nodeNum-1);
		
		// 节点频率
		int[] nodeFreqs = new int[Global.nodeNum];
			
		for(int consumerId =0 ;consumerId<Global.consumerNum;++consumerId){
			
			int fromConsumerNode = Global.consumerNodes[consumerId];
			
			// 最大堆
			PriorityQueue<Info> priorityQueue = new PriorityQueue<Info>(topK+1,new Comparator<Info>() {
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
				if(priorityQueue.size()>topK){
					// 去掉最大的，保留 k个最小的
					priorityQueue.poll();
				}
			}
			
			// 统计频率
			Info info  = null;
			while((info=priorityQueue.poll())!=null){
				int[] preNodes = info.preNodes;
				for(int node=info.toNode;node!=-1;node=preNodes[node]){
					nodeFreqs[node]++;
				}
			}
		}
		
		int[] selectedNodes = new int[Global.nodeNum];
		int len = 0;
		for(int node =0;node<Global.nodeNum;++node){
			if(nodeFreqs[node]>0){
				selectedNodes[len++] = node;
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
}
