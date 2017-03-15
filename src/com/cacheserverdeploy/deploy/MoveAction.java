package com.cacheserverdeploy.deploy;

/**
 * 一个移动方案
 *
 * @author mindw
 * @date 2017年3月15日
 */
public final class MoveAction {
	
	final String oldServerNodeId;
	final String newServerNodeId;

	public MoveAction(String oldServerNodeId, String newServerNodeId) {
		super();
		this.oldServerNodeId = oldServerNodeId;
		this.newServerNodeId = newServerNodeId;
	}

	@Override
	public String toString() {
		return "Pair [oldServerNodeId=" + oldServerNodeId + ", newServerNodeId=" + newServerNodeId + "]";
	}
}
