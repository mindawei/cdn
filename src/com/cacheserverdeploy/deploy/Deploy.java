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

    	GreedyOptimizer.optimize();
    	Global.optimize();
    	GreedyOptimizerComplex.optimize();
    	
    	if(Global.IS_DEBUG){
    		Global.printBestSolution();
      	}
    	
    	String[] solution = Global.bsetSoluttion;
    	return solution;    
    }

}
