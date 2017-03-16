package com.cacheserverdeploy.deploy;

/**
 * 一个移动方案
 *
 * @author mindw
 * @date 2017年3月15日
 */
public final class MoveAction {
	
	final int oldServerNodeId;
	final int newServerNodeId;

	public MoveAction(int oldServerNodeId, int newServerNodeId) {
		super();
		this.oldServerNodeId = oldServerNodeId;
		this.newServerNodeId = newServerNodeId;
	}

	@Override
	public String toString() {
		return "Pair [oldServerNodeId=" + oldServerNodeId + ", newServerNodeId=" + newServerNodeId + "]";
	}
}
