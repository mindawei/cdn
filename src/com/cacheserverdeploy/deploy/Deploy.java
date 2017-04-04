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
     
    		int maxMovePerRound = 900;
    		int maxUpdateNum = 30;
    		int minUpdateNum = 20;
    		new OptimizerSimpleLimit(nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();

    		maxMovePerRound = 200;
    		maxUpdateNum = 6;
     		minUpdateNum = 3;
     		new OptimizerMiddleLimit(nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
    		
    	}else if(Global.isNpHard){
    		
    		int nearestK = 2 ;
    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
    		
    		int maxMovePerRound = 1000;
    		int maxUpdateNum = 200;
    		int minUpdateNum = 100;
    		new OptimizerMiddleLimit(nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
    		
    		optimizerMCMF.optimizeGlobalBest();
    		
    		maxUpdateNum = 6;
    		minUpdateNum = 3;
			new OptimizerComplexLimit(graphContent,nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
		
    	}else{

//			new OptimizerMiddle().optimize();
//			optimizerMCMF.optimizeGlobalBest();	
//			int nearestK = 5;
//    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
//    		int maxMovePerRound = 500*1000;
//    		int maxUpdateNum = 1000;
//    		int minUpdateNum = 1000;
//			new OptimizerComplexLimit(graphContent,nodes,maxMovePerRound,maxUpdateNum,minUpdateNum).optimize();
		
//    		int nearestK = 1; 
//    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);
//
//    		int simpleMaxMovePerRound = 2000;
//    		int simpleMaxUpdateNum = 1;
//    		
//    		int middleMaxMovePerRound = 1000;
//    		int middleMaxUpdateNum = 1;
//    		int selectedNum = Global.consumerNum/4 ;
    		
    		// k = 1 30706
    		// k = 2 30899
    		// k = 3 30890
    		
    		int nearestK = Global.consumerNum/4; 
    		int[] nodes = NodesSelector.selectMoveNodes(nearestK);

    		int simpleMaxMovePerRound = 1000;
    		int simpleMaxUpdateNum = 3;
    		
    		int middleMaxMovePerRound = 1000;
    		int middleMaxUpdateNum = 1;
    		int selectedNum = Global.consumerNum/4 ;
    		
    		new OptimizerRandomLimit(optimizerMCMF,nodes, 
    				selectedNum,
    				simpleMaxMovePerRound,simpleMaxUpdateNum,
    				middleMaxMovePerRound,middleMaxUpdateNum).optimize();

    	}
    	
    	
    	optimizerMCMF.optimizeGlobalBest();
    	
    	
    	
//    	if(Global.IS_DEBUG){
//    		Global.printBestSolution();
//      }
    
    	return Global.getBsetSolution();    
    }
}
