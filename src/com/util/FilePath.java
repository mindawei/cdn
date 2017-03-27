package com.util;

public class FilePath {
	static String resultFilePath = "result/0.txt";
	

//	private static final String baseFilePath = "case_example/0 初级/";
//	private static final String baseFilePath = "case_example/1 中级/";
	
	
	private static final String baseFilePath = "case_example/2 高级/";

	
	// middle 一般跑20s左右
	// 12151 
	// static String graphFilePath = "case_example/0 初级/0.txt";
	// 11472 
	// static String graphFilePath = "case_example/0 初级/1.txt";
	// 12595
	// static String graphFilePath = "case_example/0 初级/2.txt";
	// 11222 
	// static String graphFilePath = "case_example/0 初级/3.txt";
	// 11936
	// static String graphFilePath = "case_example/0 初级/4.txt";
	// 11976
	// static String graphFilePath = "case_example/0 初级/5.txt";
	// 11765
	// static String graphFilePath = "case_example/0 初级/6.txt";
	// 12186
	// static String graphFilePath = "case_example/0 初级/7.txt";
	// 14334
	// static String graphFilePath = "case_example/0 初级/8.txt";
	
	// random 跑 85s k=consumerNum updateNum = [100,100] maxMovePerRound = 100000000
	// 27297
	// static String graphFilePath = "case_example/1 中级/0.txt";
	// 26314
	//static String graphFilePath = "case_example/1 中级/1.txt";
	
	// 26780 27385
	static String graphFilePath = "case_example/1 中级/2.txt";
	// 27453
	// static String graphFilePath = "case_example/1 中级/3.txt";
	// 29034
	// static String graphFilePath = "case_example/1 中级/4.txt";
	// 28135
	// static String graphFilePath = "case_example/1 中级/5.txt";
	// 28484
	// static String graphFilePath = "case_example/1 中级/6.txt";
	// 28524
	//static String graphFilePath = "case_example/1 中级/7.txt";
	// 27934
	// static String graphFilePath = "case_example/1 中级/8.txt";

	
	// random 跑 85s k=1 updateNum = [3,6]
	// 38452
	// static String graphFilePath = "case_example/2 高级/0.txt";
	// 36233
	// static String graphFilePath = "case_example/2 高级/1.txt";
	// 38418
	// static String graphFilePath = "case_example/2 高级/2.txt";
	// 41737
	// static String graphFilePath = "case_example/2 高级/3.txt";
	// 37834
	// static String graphFilePath = "case_example/2 高级/4.txt";
	// 37861
	// static String graphFilePath = "case_example/2 高级/5.txt";
	// 35188
	// static String graphFilePath = "case_example/2 高级/6.txt";
	// 40032
	// static String graphFilePath = "case_example/2 高级/7.txt";
	// 37251
	// static String graphFilePath = "case_example/2 高级/8.txt";

//	static String graphFilePath = baseFilePath+"1.txt";
//	static String graphFilePath = baseFilePath+"2.txt";
//	static String graphFilePath = baseFilePath+"3.txt";
//	static String graphFilePath = baseFilePath+"4.txt";
//	static String graphFilePath = baseFilePath+"5.txt";
//	static String graphFilePath = baseFilePath+"6.txt";
//	static String graphFilePath = baseFilePath+"7.txt";
//	static String graphFilePath = baseFilePath+"8.txt";

//	static String graphFilePath = "case_example/官网案例.txt";
//	static String graphFilePath = "case_example/case0.txt";
//	static String graphFilePath = "case_example/case1.txt";
//	static String graphFilePath = "case_example/case2.txt";
//	static String graphFilePath = "case_example/case3.txt";
//	static String graphFilePath = "case_example/case4.txt";
//	static String graphFilePath = "case_example/case50.txt";
//  static String graphFilePath = "case_example/case99.txt";

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
