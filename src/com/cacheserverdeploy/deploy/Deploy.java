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
    	
    	new GreedyOptimizerSimple().optimize();
		new GreedyOptimizerMiddle().optimize();
		// new GreedyOptimizerComplex().optimize();
		Global.optimize(); 
    		
//    	if(Global.isNpHardest){
//    		new GreedyOptimizerMiddle().optimize();
//    	}else if(Global.isNpHard){
//    		new GreedyOptimizerSimple().optimize();
//    		// 当中一个过度解
//    		new GreedyOptimizerMiddle().optimize();
//    		Global.optimize();
//    	}else{
//    		new GreedyOptimizerMiddle().optimize();
//    		new GreedyOptimizerComplex().optimize();
//    		Global.optimize(); 
//    	}
    	
    	if(Global.IS_DEBUG){
    		Global.printBestSolution();
      	}
    	
    	String[] solution = Global.bsetSolution;
    	return solution;    
    }
}
