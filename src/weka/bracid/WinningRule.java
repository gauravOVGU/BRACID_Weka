/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weka.bracid;

/**
 *
 * @author gaurav sharma
 */
public class WinningRule {
	public int nearestRuleIndex;
	public boolean sameClass;  /* sign: whether nearestRule's class is same as ex's */
	public float distance;         /* distance to nstrule */
	
	public void set(int rule, boolean same, float dist){
		this.nearestRuleIndex = rule;
		this.sameClass = same;
		this.distance = dist;
	}
}

