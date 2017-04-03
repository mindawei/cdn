package com.cacheserverdeploy.deploy;

public class Deploy{
    /**
     * 你需要完成的入口
     * <功能详细描述>
     * @param graphContent 用例信息文件
     * @return [参数说明] 输出结果信息
     * @see [类、类#方法、类#成员]
     */
    public static String[] deployServer(String[] graphContent)
    {
        /**do your work here**/
    	Parser.buildNetwork(graphContent);
    
    	Global.init();
    	OptimizerMCMF optimizerMCMF = new OptimizerMCMF(graphContent);
    	if(Global.isNpHardest){
    		
    		int nearestK = Global.consumerNum ;
    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
     
    		int selectedNum = Global.consumerNum / 4 + 5;
    		int maxMovePerRound = 1800;
    		int maxUpdateNum = 30;
    		int minUpdateNum = 20;
    		new OptimizerSimpleLimit(nodes,selectedNum,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();

    		maxMovePerRound = 400;
    		maxUpdateNum = 6;
     		minUpdateNum = 3;
     		new OptimizerMiddleLimit(nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
    		
    	}else if(Global.isNpHard){
    		
    		int nearestK = 2 ;
    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
    		int maxMovePerRound = 1600;
    		int maxUpdateNum = 200;
    		int minUpdateNum = 100;
    		new OptimizerMiddleLimit(nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
    		
    		optimizerMCMF.optimizeGlobalBest();
		
    		maxUpdateNum = 6;
    		minUpdateNum = 3;
			new OptimizerComplexLimit(graphContent,nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
			
    	}else{
//
//			new OptimizerMiddle().optimize();
//			optimizerMCMF.optimizeGlobalBest();		
//			
//			int nearestK = 5;
//    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
//    	
//    		int maxMovePerRound = 3000;
//    		int maxUpdateNum = 1000;
//    		int minUpdateNum = 1000;
//			new OptimizerComplexLimit(graphContent,nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();

    		// 
			new OptimizerMiddle().optimize();
			optimizerMCMF.optimizeGlobalBest();		
	
			// k = 1
			// k = 3
			// k = 5
			// k = 7
			// k - 9
			
			int nearestK = Global.consumerNum; // 5
    		// int[] nodes = NodesSelector.selectMoveNodes(nearestK);
    		// int[] nodes = NodesSelector.selectAllNodes(nearestK);
    		int[] nodes = NodesSelector.selectAllNodesInReverse(nearestK);
    		
    		
    		int maxMovePerRound = 500*1000;
    		int maxUpdateNum = 1000;
    		int minUpdateNum = 1000;
			new OptimizerComplexLimit(graphContent,nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
		
    		// 22276
//    		int nearestK = 5; /// 4
//    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
//    		int maxMovePerRound = 3000;
//    		int maxUpdateNum = 20;
//    		int minUpdateNum = 12;
//    		int selectedNum = Global.consumerNum / 4;
//    		new OptimizerRandomLimit(optimizerMCMF,nodes, selectedNum, maxMovePerRound, maxUpdateNum, minUpdateNum).optimize();
	
    		
    	}
    	
    	
    	optimizerMCMF.optimizeGlobalBest();
    	
//    	if(Global.IS_DEBUG){
//    		Global.printBestSolution();
//      }
    
    	return Global.getBsetSolution();    
    }
}
