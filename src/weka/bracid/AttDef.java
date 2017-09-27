/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weka.bracid;

/**
 *
 * @author Gaurav Sharma
 */
public class AttDef {
	public int type, nvals, nmiss;
	public float min, max, avg;
	public String name = "";
	public String[] vals;
	
	public AttDef(int maxVals){
		vals = new String[maxVals];
	max = -Utils.Infinity;
	min = Utils.Infinity;
	avg = 0;
	nvals = nmiss = 0;
	name = "";
	for (int v = 0; v < maxVals; v++)
	vals[v] = "";
	}
}
