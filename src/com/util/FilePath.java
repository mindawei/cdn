package com.util;

public class FilePath {

//	private static final String baseFilePath = "case_example/0 初级/";
	// 2 12595
	//
	
//	private static final String baseFilePath = "case_example/1 中级/";
	
	// 2 27385
	// 27464
	
	private static final String baseFilePath = "case_example/2 高级/";
	// 0 40214
	// 1 36623
	// 2 43389
	// 
	
	
	// 总的费用：59121  1/4
//	预计算完成：消费者到所有节点的费用 
//	edge.bandWidth<0:671 781 552 564 272 6
	
	// 总的费用：50311  1/7
	// 43008
//	public static final String graphFilePath = baseFilePath+"0.txt";
	// k=1 n=1/4 r=2000  36337
	// k=2 n=1/4 r=2000  36337
	// k=counsumer-1 n=1/4 r=2000  36337
	// 37617
//	public static final String graphFilePath = baseFilePath+"1.txt";
//	public static final String graphFilePath = baseFilePath+"2.txt";
	// 40645
//	public static final String graphFilePath = baseFilePath+"3.txt";
	// 36965
//	public static final String graphFilePath = baseFilePath+"4.txt";
	// 37267
//	public static final String graphFilePath = baseFilePath+"5.txt";
//	public static final String graphFilePath = baseFilePath+"6.txt";
//	public static final String graphFilePath = baseFilePath+"7.txt";
	
	// 高级  37251
	// 中级  28717
	// 初级
	public static final String graphFilePath = baseFilePath+"8.txt";

	//	public static final String graphFilePath = "case_example/官网案例.txt";
//	public static final String graphFilePath = "case_example/case0.txt";
//	public static final String graphFilePath = "case_example/case1.txt";
//	public static final String graphFilePath = "case_example/case2.txt";
//	public static final String graphFilePath = "case_example/case3.txt";
//	public static final String graphFilePath = "case_example/case4.txt";
//	public static final String graphFilePath = "case_example/case50.txt";
//  public static final String graphFilePath = "case_example/case99.txt";

	public static final String resultFilePath = "result/0.txt";
	
	// 官网案例     :  783
	// case 0 : 2042
	// case 1 : 2136
	// case 2 : 1692
	// case 3 : 2111
	// case 4 : 1967 
	
	// ? case 50 : 13209 / 13706
	// 最小费用：12884 
	// 服务器结点: 5, 11, 23, 45, 52, 56, 58, 61, 62, 65, 75, 89, 98, 113, 118, 136, 180, 192, 288, 
	//          5, 11, 23, 45, 52, 56, 58, 61, 62, 65, 75, 89, 98, 113, 118, 136, 180, 192, 288, 
	
	// use:16319
	
	// case 99
	// 最优解：总的费用：610563
	
	// 80s
	// 总的费用：571604
	
	// TODO
	// MCMF优化？
	// 1 加上Complex?
	// 2 其他用例测试
	// 3  寻路过程中也可以利用缓存提前结束
	
	// 21920 41574 9万以下
	
//	分割子网（不建议，类似误差累积）
//	实现细节（可以再抠一下）
//	运筹学（不知道啥东东，说不定可以借鉴一下）
//	数学归纳（从初始解类推）
//	动态规划（复用思想）
//	在一些假设下（利用网络本身信息）
	
	// TODO 明天跑测试
	
}
