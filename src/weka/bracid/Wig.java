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
import java.io.Serializable;
import java.util.BitSet;

import weka.core.Utils;

/**
 *<p>
 * Measure for evaluation of conditions and rules - Weighted Information Gain
 *</p>
 * 
 * @author gaurav sharma
 * 
 */
public class Wig {

	private static double WORST_VALUE = Double.MAX_VALUE * (-1);
	
	private static double BEST_VALUE = Double.MAX_VALUE;
		
	/**
	 *
	 * @see evaluationMeasures.EvaluationMeasure#isBetter(double, double)
	 */
	public static boolean isBetter(double what, double fromWhat){
		return what > fromWhat;
	}

	/**
	 * Calculates evaluation of the condition according to the formula:<br>
	 * (S1+ / S+)*(log2(S1+ / S1) - log2(S+ / S))<br>
	 * where:<br>
	 * S1 - instances covered in setS <br>
	 * S1+ - positive instances covered in setS <br>
	 * S - all instances in setS <br>
	 * S+ - all positive instances in setS <br>
	 *
	 * @see evaluationMeasures.EvaluationMeasure#getEvaluation(java.util.BitSet, java.util.BitSet, java.util.BitSet, int, boolean)
	 */
	public static double getEvaluation(BitSet instancesCovered,
			BitSet setS, BitSet positives, int size){
		
		BitSet S1 = (BitSet)instancesCovered.clone();
		S1.and(setS);
				
		//regula nie pokrywa zadnych przykladow - kiepska
		if (S1.cardinality() == 0)
			return WORST_VALUE;

		//regula wczesniej nie pokrywala zadnych przykladow - kiepska
		if (setS.cardinality() == 0)
			return WORST_VALUE;
				
		BitSet posS1 = (BitSet)S1.clone();
		posS1.and(positives);
		
		BitSet posS = (BitSet)setS.clone();
		posS.and(positives);
		
//		regula wczesniej nie pokrywala zadnych przykladow pozytywnych - kiepska
		if (posS.cardinality() == 0)
			return WORST_VALUE;		
		
		
//		regula nie pokrywa zadnych przykladow pozytywnych - kiepska
		if (posS1.cardinality() == 0)
			return WORST_VALUE;	
		
		double nPosS1 = (double)posS1.cardinality();
		double nPosS = (double)posS.cardinality();
		
		double nS1 = (double)S1.cardinality();
		double nS = (double)setS.cardinality();		
		
		return (nPosS1 / nPosS)*(Utils.log2(nPosS1 / nS1) - Utils.log2(nPosS / nS));		
	}
	
	/**
	 *
	 * @see evaluationMeasures.EvaluationMeasure#getBestValue()
	 */
	public static double getBestValue() {
		return BEST_VALUE;
	}

	/**
	 *
	 * @see evaluationMeasures.EvaluationMeasure#getWorstValue()
	 */
	public static double getWorstValue() {
		return WORST_VALUE;
	}
}


