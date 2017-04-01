package com.cacheserverdeploy.deploy;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * 统一选点避免重复
 */
public final class NodesSelector {
	
	private static final class Info {
		/** 花费 */
		final int cost;
		/** 前向指针 */
		final int[] preNodes;
		/** 目标节点 */
		final int toNode;

		public Info(int cost, int[] preNodes, int toNode) {
			super();
			this.cost = cost;
			this.preNodes = preNodes;
			this.toNode = toNode;
		}
	}
	
	
	private static final class NodeFreq {
		final int node;
		int freqs;

		public NodeFreq(int node, int freqs) {
			super();
			this.node = node;
			this.freqs = freqs;
		}
	}
	

	/** 寻找可以移动的那些点 */
	static int[] selectMoveNodes(int nearestK) {

		// 不会超过消费者数
		nearestK = Math.min(nearestK, Global.nodeNum - 1);

		// 节点频率
		NodeFreq[] nodeFreqs = new NodeFreq[Global.nodeNum];
		for (int node = 0; node < Global.nodeNum; ++node) {
			nodeFreqs[node] = new NodeFreq(node, 0);
		}

		for (int consumerId = 0; consumerId < Global.consumerNum; ++consumerId) {

			int fromConsumerNode = Global.consumerNodes[consumerId];

			// 最大堆
			PriorityQueue<Info> priorityQueue = new PriorityQueue<Info>(
					nearestK + 1, new Comparator<Info>() {
						@Override
						public int compare(Info o1, Info o2) {
							return o2.cost - o1.cost;
						}
					});

			for (int toConsumerNode : Global.consumerNodes) {
				// 自己不到自己
				if (toConsumerNode == fromConsumerNode) {
					continue;
				}
				int cost = Global.allCost[consumerId][toConsumerNode];
				int[] preNodes = Global.allPreNodes[consumerId];
				Info info = new Info(cost, preNodes, toConsumerNode);
				priorityQueue.add(info);
				if (priorityQueue.size() > nearestK) {
					// 去掉最大的，保留 k个最小的
					priorityQueue.poll();
				}
			}

			// 统计频率
			Info info = null;
			while ((info = priorityQueue.poll()) != null) {
				int[] preNodes = info.preNodes;
				for (int node = info.toNode; node != -1; node = preNodes[node]) {
					nodeFreqs[node].freqs++;
				}
			}
		}

		// 按频率从大到小排
		Arrays.sort(nodeFreqs, new Comparator<NodeFreq>() {
			@Override
			public int compare(NodeFreq o1, NodeFreq o2) {
				return o2.freqs - o1.freqs;
			}
		});

		
		int[] selectedNodes = new int[Global.nodeNum];
		int len = 0;
		for (NodeFreq nodeFreq : nodeFreqs) {
			if (nodeFreq.freqs > 0) {
				selectedNodes[len++] = nodeFreq.node;
			}else{
				break;
			}
		}
		int[] nodes = new int[len];
		System.arraycopy(selectedNodes, 0, nodes, 0, len);

		if(Global.IS_DEBUG){
			System.out.println("选出数目："+nodes.length+" 总共数目："+Global.nodeNum);
		}
		return nodes;
	}

	
}
