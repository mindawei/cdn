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
     
    		int selectedNum = Global.consumerNum / 4;
    		int maxMovePerRound = 2000;
    		int maxUpdateNum = 9;
    		int minUpdateNum = 6;
    		new OptimizerSimpleLimit(nodes,selectedNum,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();

    		maxMovePerRound = 1000;
    		maxUpdateNum = 6;
     		minUpdateNum = 3;
     		new OptimizerMiddleLimit(nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
    		
    	}else if(Global.isNpHard){
    		
    		int nearestK = 2 ;
    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
    		int maxMovePerRound = 2000;
    		int maxUpdateNum = 1000;
    		int minUpdateNum = 1000;
    		new OptimizerMiddleLimit(nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
    		
    		optimizerMCMF.optimizeGlobalBest();
		
    		maxUpdateNum = 9;
    		minUpdateNum = 6;
			new OptimizerComplexLimit(graphContent,nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
			
    	}else{

//			new OptimizerMiddle().optimize();
//			optimizerMCMF.optimizeGlobalBest();		
			
			int nearestK = 2;
    		//int[] nodes = NodesSelector.selectMoveNodes(nearestK);
    		int[] nodes = NodesSelector.selectAllNodes();
    		
    		
    		int maxMovePerRound = 500*1000;
    		int maxUpdateNum = 1000;
    		int minUpdateNum = 1000;
			new OptimizerComplexLimit(graphContent,nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
		
//    		int nearestK = Global.consumerNum / 4;
//    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
//    		int maxMovePerRound = 2000;
//    		int maxUpdateNum = 12;
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
