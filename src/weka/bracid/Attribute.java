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
public class Attribute {
	public int type, symb;      /* symbolic values are represented by integers */
	/* type: Symbolic, Numeric or Any (unknown value) */
	public float lower, upper;  /* upper and lower bounds for numeric value */
}

