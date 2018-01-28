package com.cacheserverdeploy.deploy;

public class Deploy {
	/**
	 * 你需要完成的入口 <功能详细描述>
	 * 
	 * @param graphContent
	 *            用例信息文件
	 * @return [参数说明] 输出结果信息
	 * @see [类、类#方法、类#成员]
	 */
	public static String[] deployServer(String[] graphContent) {
		/** do your work here **/

		Global.init(graphContent);
		OptMCMF optimizerMCMF = new OptMCMF();

		if (Global.isLarge) {

			// 选点
			int[] nodes = new NodesSelector().select();

			Global.minCost = Global.INFINITY;
			int serverNodesSize = 240;
			Server[] servers = new Server[serverNodesSize];
			for (int i = 0; i < serverNodesSize; ++i) {
				servers[i] = new Server(nodes[i]);
				servers[i].ouput = Global.maxServerOutput;
			}
			Global.setBestServers(servers);

			// 筛选
			new OptReduce(nodes).optimize();

			// 调整
			int maxUpdateNum = 12;
			new OptLarge(nodes, maxUpdateNum).optimize();

		} else {

			// 选点
			int[] nodes = new NodesSelector().select();

			// 设置服务器
			Global.minCost = Global.INFINITY;
			int serverNodesSize = 120;
			Server[] servers = new Server[serverNodesSize];
			for (int i = 0; i < serverNodesSize; ++i) {
				servers[i] = new Server(nodes[i]);
				servers[i].ouput = Global.maxServerOutput;
			}
			Global.setBestServers(servers);

			// 筛选
			new OptReduce(nodes).optimize();

			// 调整
			int maxUpdateNum = 12;
			new OptMiddle(nodes, maxUpdateNum).optimize();

		}

		optimizerMCMF.optimizeGlobalBest();

		return Global.getBsetSolution();
	}
}
