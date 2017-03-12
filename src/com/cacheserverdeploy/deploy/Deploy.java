package com.cacheserverdeploy.deploy;

public class Deploy
{
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
       
    	if(Global.IS_DEBUG){
        	Global.printNetworkInfo();
        }
    	
    	Global.initSolution();
    
    	// 1 边界合并
    	Optimizer optimizer = new BoundMergeOptimizer();
    	optimizer.optimize();
    	// 2 启发
    	optimizer = new HeuristicOptimizer();
    	optimizer.optimize();	

 //    	Map<String,CostInfo> map = Router.getUnitCost("7");
//    	for(Map.Entry<String,CostInfo> entry : map.entrySet()){
//    		System.out.println("7 - > "+entry.getKey()+" : "+entry.getValue().cost);
//    		for(String node :entry.getValue().nodes){
//    			System.out.print(node+" ");
//    		}
//    		System.out.println();
//    	}
    	if(Global.IS_DEBUG){
    		Global.printBestSolution();
		}	
    	String[] solution = Global.getBestSolution();
    	return solution;    
    }

}
