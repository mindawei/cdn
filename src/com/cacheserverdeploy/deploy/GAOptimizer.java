package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

/**
 * 遗传算法
 * 
 * @author mindw
 * @date 2017年3月16日
 */
public final class GAOptimizer extends Optimizer {
	
	/** 最多多少个 */
	private int maxNum = 500;
	private int initNum = 1000;
	
	private Random random = new Random(47);
	
	private final class Info implements Comparable<Info>{
		
		final String key;
		final int cost;
	
		public Info(String key,int cost) {
			super();
			this.key = key;
			this.cost = cost;
		}
		
		@Override
		public int compareTo(Info o) {
			return cost-o.cost;
		}

		@Override
		public String toString() {
			return "Info [key=" + key + ", cost=" + cost + "]";
		}
		
		
	}
	
	@Override
	void optimize() {
		
		// a. 选择适当表示模式，生成初始群体
		Map<String,int[]> group = new HashMap<String,int[]>();
		initGroup(group);
		
		// b. 通过计算群体中各个体的适应度对群体进行评价；
		Map<String,Integer> costs = new HashMap<String,Integer>();
		for(Map.Entry<String, int[]> entry : group.entrySet()){
			String key = entry.getKey();
			int[] gene = entry.getValue();
			if(Global.isTimeOut()){
				return;
			}
			move(gene);
			Global.updateSolution();
			int cost = Global.getTotalCost();
			costs.put(key, cost);
			Global.reset();
		}
		
		
		// While 未达到要求的目标 do
		while(!Global.isTimeOut()){
			
			// a. 选择作为下一代群体的各个体；
			// top k
			PriorityQueue<Info> priorityQueue = new PriorityQueue<Info>();
			for(Map.Entry<String, Integer> entry : costs.entrySet()){
				if(entry.getValue()<=Global.MAX_COST){
					priorityQueue.add(new Info(entry.getKey(), entry.getValue()));
				}
			}
			
			ArrayList<String> liveKeys = new ArrayList<String>();
			// 存活1/2
			int i = 0;
			while(!priorityQueue.isEmpty()&&i++<maxNum){
				liveKeys.add(priorityQueue.poll().key);
			}
			// System.out.println("group.size():"+group.size());
			// b. 执行交换操作；
			Map<String,int[]> swapedGroup = swap(liveKeys,group);
			
			// System.out.println("swapedGroup.size():"+swapedGroup.size());
			
			// c. 执行突变操作；
			Map<String,int[]> mutatedGroup = mutate(swapedGroup);
				
			// System.out.println("mutatedGroup.size():"+mutatedGroup.size());
			
			// d. 对群体进行评价
			Map<String,Integer> newCosts = new HashMap<String,Integer>();
			for(Map.Entry<String, int[]> entry : mutatedGroup.entrySet()){
				String key = entry.getKey();
				int[] gene = entry.getValue();
				if(costs.containsKey(key)){
					newCosts.put(key, costs.get(key));
				}else{
					if(Global.isTimeOut()){
						return;
					}
					move(gene);
					Global.updateSolution();
					int cost = Global.getTotalCost();
					costs.put(key, cost);
					Global.reset();
				}
			
			}
			costs = newCosts;
			group = mutatedGroup;
		}
		
	}

	private Map<String, int[]> mutate(Map<String, int[]> group) {
		Map<String,int[]> newGroup = new HashMap<String,int[]>(); 
		for (Map.Entry<String, int[]> entry : group.entrySet()) {
			int[] gene = entry.getValue();
			int index = random.nextInt(gene.length);
			int[] newGene = Arrays.copyOf(gene, gene.length);
			if(gene[index]==1){
				newGene[index] = 0;
				newGroup.put(getKey(newGene), newGene);
			}else{
				newGene[index] = 1;
				newGroup.put(getKey(newGene), newGene);
			}
		}
		newGroup.putAll(group);
		return newGroup;
	}



	/** 执行交换操作 */
	private Map<String, int[]> swap(ArrayList<String> liveKeys,Map<String, int[]> group) {
		
		Map<String,int[]> newGroup = new HashMap<String,int[]>(); 
			
		Set<String> swapedKeys = new HashSet<String>();
		
		for (String keyA : liveKeys) {

			// 保存优秀的
			newGroup.put(keyA, group.get(keyA));
			
			// 进行交换
			String keyB = liveKeys.get(random.nextInt(liveKeys.size()));
			if (keyB.equals(keyA) || swapedKeys.contains(keyB)) {
				continue;
			}

			int[] geneA = group.get(keyA);
			int[] geneB = group.get(keyB);

			// [1,geneA.length-1]
			int len = 1 + random.nextInt(geneA.length - 1);

			int[] tmpA = Arrays.copyOf(geneA, geneA.length);
			System.arraycopy(geneB, len, tmpA, len, geneA.length - len);

			int[] tmpB = Arrays.copyOf(geneB, geneB.length);
			System.arraycopy(geneA, len, tmpB, len, geneA.length - len);

			//if (check(tmpA)) {
				newGroup.put(getKey(tmpA), tmpA);
			//}
			//if (check(tmpB)) {
				newGroup.put(getKey(tmpB), tmpB);
			//}
			
		}
		
		return newGroup;
	}
	
	/** 检查是否合法 */
	private boolean check(int[] gene) {
		int sum = 0;
		for(int v : gene){
			sum+=v;
		}
		return sum<= Global.consumerNum - 1 && sum>0;
	}

	/** 生成初始群体 */
	private void initGroup(Map<String,int[]> group) {
		
		class Node implements Comparable<Node>{
			String key;
			int cost;
			int[] gene;
			public Node(String key, int cost, int[] gene) {
				super();
				this.key = key;
				this.cost = cost;
				this.gene = gene;
			}
			
			@Override
			public int compareTo(Node o) {
				return o.cost - cost;
			}
			
		}
		
		PriorityQueue<Node> priorityQueue = new PriorityQueue<Node>(initNum);
		
		// 一个初始解
		int[] initGene = Global.getGene();
		priorityQueue.add(new Node(getKey(initGene),Global.getTotalCost(),initGene));
		
		
		// 一个贪心解决
		while(true) {
			
			if(Global.isTimeOut()){
				break;
			}
			
			// 可选方案
			List<MoveAction> moveActions = new LinkedList<MoveAction>();
			for(Server server : Global.servers){		
				for(int toNodeId =0;toNodeId<Global.nodeNum;++toNodeId){
					moveActions.add(new MoveAction(server.nodeId, toNodeId));
				}
			}
			
			MoveAction bestMoveAction = null;
			int minCost = Global.INFINITY;
			
			for (MoveAction moveAction : moveActions) {
				Global.save();
				// 启发函数： 花费 + 这个点的移动频率
				move(moveAction);
				int cost = Global.getTotalCost();		
				if (cost < minCost) {
					minCost = cost;
					bestMoveAction = moveAction;
				}
				
				int[] gene = Global.getGene();
				priorityQueue.add(new Node(getKey(gene),cost,gene));
				if(priorityQueue.size()>initNum){
					priorityQueue.poll();
				}
				
				Global.goBack();
			}

			if (bestMoveAction == null) {
				break;
			}
			
			// 移动
			move(bestMoveAction);
			boolean better = Global.updateSolution();
			if(!better){
				break; // 返回
			}
					
		}
		int[] greedyGene = Global.bestGene;
		group.put(getKey(greedyGene),greedyGene);
		while(!priorityQueue.isEmpty()){
			Node node = priorityQueue.poll();
			group.put(node.key,node.gene);
		}
	}
	
	/** 生成 key */
	private String getKey(int[] gene){
		StringBuilder builder = new StringBuilder();
		// 那些位置为1 3,4,5,  
		for(int i=0;i<gene.length;++i){
			if(gene[i]==1){
				builder.append(i);
				builder.append(",");
			}
		}
		String key = builder.toString();
		return key;
	}

}
