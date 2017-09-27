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
public class Rule {
        public	Attribute[] ants;
	public int nmerged;      /* no. instances merged into rule; nmerged = 0 : deleted */
	public int nwon, ncorr;  /* no. exs won, no. exs correct */
	public float acc;        /* Laplace accuracy */
        public int fromExample;
	
	public Rule(int maxAtts){
		ants = new Attribute[maxAtts];
		for(int i = 0; i < maxAtts; i++)
			ants[i] = new Attribute();
	}
}

