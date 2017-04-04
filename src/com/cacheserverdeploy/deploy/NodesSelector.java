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

	static final class NodeFreq {
		final int node;
		int freqs;
		int degree;
		int bandwidth = 0;
		int rank;
		int dist = 0;

		public NodeFreq(int node, int freqs) {
			super();
			this.node = node;
			this.freqs = freqs;
			this.degree = Global.connections[node].length;
			for (int toNdoe : Global.connections[node]) {
				bandwidth += Global.graph[node][toNdoe].initBandWidth;
			}

			PriorityQueue<Integer> dists = new PriorityQueue<Integer>();
			for (int i = 0; i < Global.consumerNum; ++i) {
				dists.add( Global.allCost[i][node]);
			}			
			for(int i=0;i<2;++i){
				dist+=dists.poll();			
			}
		}

		@Override
		public String toString() {
			return "NodeFreq [node=" + node + ", freqs=" + freqs + ", degree=" + degree + ", bandwidth=" + bandwidth
					+ ", rank=" + rank + ", dist=" + dist + "]";
		}

	}
	
	/** 寻找可以移动的那些点 */
	static int[] selectMoveNodes(int nearestK) {

		// 不会超过消费者数
		nearestK = Math.min(nearestK, Global.nodeNum - 1);


		NodeFreq[] nodeFreqs = new NodeFreq[Global.nodeNum];
		for (int node = 0; node < Global.nodeNum; ++node) {
			nodeFreqs[node] = new NodeFreq(node, 0);
		}

		for (int consumerId = 0; consumerId < Global.consumerNum; ++consumerId) {

			int fromConsumerNode = Global.consumerNodes[consumerId];

			// 最大堆
			PriorityQueue<Info> priorityQueue = new PriorityQueue<Info>(nearestK + 1, new Comparator<Info>() {
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
			} else {
				break;
			}
		}
		int[] nodes = new int[len];
		System.arraycopy(selectedNodes, 0, nodes, 0, len);
		return nodes;
	}

	static NodeFreq[] nodeFreqs2 = new NodeFreq[Global.nodeNum];

	/** 寻找可以移动的那些点 */
	static int[] selectMoveNodesByRank(int nearestK) {
		
		// 不会超过消费者数
		nearestK = Math.min(nearestK, Global.nodeNum - 1);

		NodeFreq[] nodeFreqs = new NodeFreq[Global.nodeNum];
		for (int node = 0; node < Global.nodeNum; ++node) {
			nodeFreqs[node] = new NodeFreq(node, 0);
		}

		for (int consumerId = 0; consumerId < Global.consumerNum; ++consumerId) {

			int fromConsumerNode = Global.consumerNodes[consumerId];

			// 最大堆
			PriorityQueue<Info> priorityQueue = new PriorityQueue<Info>(nearestK + 1, new Comparator<Info>() {
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
				// minRank:2 maxRAnk:180 42180
				// if(o2.freqs == o1.freqs){
				// return o2.degree - o1.degree;
				// }
				// return o2.freqs - o1.freqs;

				// minRank:26 maxRAnk:290 42328
				// return o2.bandwidth - o1.bandwidth;
				
				// minRank:14 maxRAnk:299 42180
				
				// minRank:2 maxRAnk:263
				// minRank:0 maxRAnk:279
				// minRank:0 maxRAnk:197
				return o1.dist/(o1.freqs+o1.degree+o1.bandwidth+1) - o2.dist/(o2.freqs+o2.degree+o2.bandwidth+1);

			}
		});

		for (NodeFreq nodeFreq : nodeFreqs) {
			nodeFreqs2[nodeFreq.node] = nodeFreq;
		}

		int[] selectedNodes = new int[Global.nodeNum];
		int len = 0;
		int rank = 0;
		for (NodeFreq nodeFreq : nodeFreqs) {
			nodeFreq.rank = rank++;
			if (nodeFreq.freqs > 0) {
				// if(nodeFreq.degree>=4||Global.nodeToConsumerId.containsKey(nodeFreq.node)){
				selectedNodes[len++] = nodeFreq.node;

				// 】System.out.print(nodeFreq.degree+",");
				// }
			}
		}
		System.out.println();
		int[] nodes = new int[len];
		System.arraycopy(selectedNodes, 0, nodes, 0, len);

		return nodes;
	}

	public static int[] selectAllNodes(int nearestK) {
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
			PriorityQueue<Info> priorityQueue = new PriorityQueue<Info>(nearestK + 1, new Comparator<Info>() {
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
			selectedNodes[len++] = nodeFreq.node;
		}
		
		return selectedNodes;
	}

	public static int[] selectAllNodesInReverse(int nearestK) {
		// 不会超过消费者数
		nearestK = Math.min(nearestK, Global.nodeNum - 1);


		NodeFreq[] nodeFreqs = new NodeFreq[Global.nodeNum];
		for (int node = 0; node < Global.nodeNum; ++node) {
			nodeFreqs[node] = new NodeFreq(node, 0);
		}

		for (int consumerId = 0; consumerId < Global.consumerNum; ++consumerId) {

			int fromConsumerNode = Global.consumerNodes[consumerId];

			// 最大堆
			PriorityQueue<Info> priorityQueue = new PriorityQueue<Info>(nearestK + 1, new Comparator<Info>() {
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

		// 按频率从小到大排，频率相同按度从大到小排
		Arrays.sort(nodeFreqs, new Comparator<NodeFreq>() {
			@Override
			public int compare(NodeFreq o1, NodeFreq o2) {
				if (o1.freqs == o2.freqs) {
					// 度从大到小排
					return o2.degree - o1.degree;
				} else {
					// 频率从小到大排
					return o1.freqs - o2.freqs;
				}
			}
		});

		int[] selectedNodes = new int[Global.nodeNum];
		int len = 0;
		for (NodeFreq nodeFreq : nodeFreqs) {
			selectedNodes[len++] = nodeFreq.node;
		}
	
		return selectedNodes;
	}

}
