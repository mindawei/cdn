package com.cacheserverdeploy.deploy;

/**
 * 优化器应该实现的接口 
 * 
 * @author mindw
 * @date 2017年3月12日
 */
public interface Optimizer {
	
	/** 优化，优化过后会改变全局状态 */
	void optimize();
	
}
