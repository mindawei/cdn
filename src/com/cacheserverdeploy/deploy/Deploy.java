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
    	
    	if(Global.isNpHardest){
    		int nearestK = 1;
    		int selectedNum = Global.consumerNum / 4;
    		int maxMovePerRound = 2000;
    		int maxUpdateNum = 9;
    		new GreedyOptimizerRandom(nearestK,selectedNum,maxMovePerRound,maxUpdateNum).optimize();
    	}else if(Global.isNpHard){
//    		int nearestK = Global.consumerNum -1;
//    		int selectedNum = Global.consumerNum / 4;
//    		int maxMovePerRound = 5000;
//    		new GreedyOptimizerRandom(nearestK,selectedNum,maxMovePerRound).optimize();
    		int maxUpdateNum = 9;
    		new GreedyOptimizerSimple(maxUpdateNum).optimize();
		}else{
			int maxUpdateNum = 20;
			new GreedyOptimizerMiddle(maxUpdateNum).optimize();
		}
    	
    	new GreedyOptimizerMCMF(GreedyOptimizer.OPTIMIZE_ONCE).optimize();	
	
//    	if(Global.IS_DEBUG){
//    		Global.printBestSolution();
//      }
    
    	return Global.getBsetSolution();    
    }
}
