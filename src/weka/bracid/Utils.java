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
import java.util.ArrayList;
import weka.core.Instance;


public final class Utils {
	
	public static final int Symbolic = 0;
	public static final int Numeric = 1;
	public static final int Any = 2;
	public static final int Class = 3;	
	public static final float Infinity = Float.MAX_VALUE;
	
	private static int Missing(int a, AttDef[] attdefs){
		return attdefs[a].nvals;
	}
	
	public static Attribute[] convertInstanceToExample(Instance instance, AttDef[] attdefs){
		Attribute[] example = new Attribute[attdefs.length];
		
		
		for (int j = 0; j < attdefs.length; j++){
			example[j] = new Attribute();
			if ((attdefs[j].type == Utils.Symbolic) || (attdefs[j].type == Utils.Class)){
				example[j].type = Utils.Symbolic;
				if (instance.isMissing(j))
					example[j].symb = attdefs[j].nvals;				
				else
					example[j].symb = (int)instance.value(j);
			}
			else if (attdefs[j].type == Utils.Numeric){				
				if (instance.isMissing(j))
					example[j].type = Utils.Any;
				else {
					example[j].type = Utils.Numeric;
					example[j].lower = (float)instance.value(j);
					example[j].upper = example[j].lower;
				}
			}
		}	
		
		return example;
	}
	
	public static void copyrule(Rule nr, Rule or)
	{
	int a;

	for (a = 0; a < nr.ants.length; a++)
            copyatt(nr.ants[a], or.ants[a]);
	nr.nmerged = or.nmerged;
	nr.nwon = or.nwon;
	nr.ncorr = or.ncorr;
	nr.acc = or.acc;
        nr.fromExample = or.fromExample;
	}

        public static void copyex(Attribute[] na, Attribute[] oa){
            for (int a = 0; a < na.length; a++)
                copyatt(na[a],oa[a]);
        }

	public static void copyatt(Attribute na, Attribute oa)
	{
	na.type = oa.type;
	na.symb = oa.symb;
	na.lower = oa.lower;
	na.upper = oa.upper;
	}

	public static boolean eqrule(Rule r1, Rule r2)
	{
	int a;

	for (a = 0; a < r1.ants.length; a++)
	if (!eqattr(r1.ants[a], r2.ants[a]))
	  return(false);
	return(true);
	}

	private static boolean eqattr(Attribute a1, Attribute a2)
	{
	if (a1.type != a2.type
	  || a1.type == Utils.Symbolic && a1.symb != a2.symb
	  || a1.type == Utils.Numeric
	     && (a1.lower != a2.lower || a1.upper != a2.upper))
	return(false);
	else
	return(true);
	}

      public static boolean covers(Rule rule, Attribute[] example) {
        for (int a = 0; a < example.length - 1; a++) {
            if (!covers(example[a], rule.ants[a])) {
                return false;
            }
        }
        return true;
    }

          /* checks if condition on the attribute in the rule (rule_ant) covers attribute example_att */
    public static boolean covers(Attribute example_att, Attribute rule_ant) {
        if (rule_ant.type == Utils.Any) {
            return true;
        }
        if (example_att.type == Utils.Symbolic && example_att.symb == rule_ant.symb) {
            return true;
        }
        if (example_att.type == Utils.Numeric
                && example_att.lower <= rule_ant.upper
                && example_att.lower >= rule_ant.lower) {
            return true;
        }
        if (example_att.type == Utils.Any) {
            return true;
        }
        return false;
    }

    public static int getStrength(Rule r, Attribute[][] examples){
        int counter = 0;
        for(int i = 0; i < examples.length; i++)
            if(covers(r,examples[i]))
                counter++;
        return counter;
    }

    public static float[] calculateCoverages(ArrayList<Rule> rules,  Attribute[][] examples, float[] coverages){
        for (int i = 0; i < rules.size(); i++)
            coverages[i] = getCoverage(rules.get(i), examples);
        return coverages;
    }

    private static float getCoverage(Rule r, Attribute[][] examples){
        int counter = 0;
        int counterOk = 0;
        int clas = r.ants.length-1;
        for(int i = 0; i < examples.length; i++){
            if (r.ants[clas].symb == examples[i][clas].symb)
                counter++;
            if(covers(r,examples[i]))
                counterOk++;
            }

        return (float)counterOk/counter;
    }

    public static float getAccuracy(Rule r, Attribute[][] examples){
        int counter = 0;
        int counterOk = 0;
        int clas = r.ants.length-1;
        for(int i = 0; i < examples.length; i++)
            if(covers(r,examples[i])){
                counter++;
                if (r.ants[clas].symb == examples[i][clas].symb)
                    counterOk++;
            }

        return (float)counterOk/counter;
    }

    public static int getLength(Attribute[] ants){
        int counter = 0;
        for (int i =0; i < ants.length - 1; i++)
            if (ants[i].type != Utils.Any)
                counter++;
        return counter;

    }
	
	public static void prls(ArrayList<Rule> rules, AttDef[] attdefs, Attribute[][] examples)  /* print all active rules */
	{
	int r,counter=0;
	
	for (r = 0; r < rules.size(); r++)            
	if (rules.get(r).nmerged != -1){
          System.out.print(counter+".[ "+rules.get(r).fromExample+"] ");
          System.out.print("strength="+ getStrength(rules.get(r),examples) + " Accuracy="+getAccuracy(rules.get(r), examples)+" ");
          counter++;
	  prl(rules.get(r), attdefs);
          
            }
	System.out.println("total number of rules="+counter);
	}
	
	public static void prl(Rule r, AttDef[] attdefs)  /* print one rule */
	{
	int a;
	
	int natts = r.ants.length-1;
	int clas = attdefs.length-1;

	System.out.print(r.acc +" "+ r.nwon + " "+ r.ncorr + " " + r.nmerged + "  |  ");
	for (a = 0; a < natts; a++) {
		System.out.print(attdefs[a].name);
	if (r.ants[a].type == Any)
	  System.out.print("* ");
	else if (r.ants[a].type == Symbolic
	     && r.ants[a].symb == Missing(a, attdefs))
	  System.out.print("? ");
	else if (r.ants[a].type == Symbolic)
	/*      printf("%d ", r.ants[a].symb); */
	  System.out.print(attdefs[a].vals[r.ants[a].symb]+" ");
	else if (r.ants[a].type == Numeric) {
	  if (r.ants[a].lower < -0.9 * Infinity
	  && r.ants[a].upper > 0.9 * Infinity)
		  System.out.print("* ");
	  else if (r.ants[a].lower < -0.9 * Infinity)
	System.out.print("< "+ r.ants[a].upper);
	  else if (r.ants[a].upper > 0.9 * Infinity)
	System.out.print("> " + r.ants[a].lower);
	  else
	System.out.print(r.ants[a].lower+":"+r.ants[a].upper+" ");
	}
	}
	/*  printf("-> %d", r.ants[class].symb); */
	System.out.println("-> "+ attdefs[clas].vals[r.ants[clas].symb]);
	}
	
	private static void prx(Attribute[] ex, AttDef[] attdefs)  /* print one example, given by pointer */
	{
	int a;
	int clas = ex.length-1;

	for (a = 0; a < ex.length; a++) {
	if (ex[a].type == Any
	|| ex[a].type == Symbolic && ex[a].symb == Missing(a,attdefs))
		System.out.print("* ");
	else if (ex[a].type == Symbolic)
	/*      printf("%d ", ex[a].symb); */
		System.out.print(" "+ attdefs[a].vals[ex[a].symb]);
	else if (ex[a].type == Numeric)
		System.out.print(" "+ ex[a].lower+":"+ex[a].upper);
	}
	/* printf("-> %d", ex[class].symb); */
	System.out.println("-> "+ attdefs[clas].vals[ex[clas].symb]);
	}

	public static void pre(Attribute[] example, AttDef[] attdefs)  /* print one example, given by index */
	{
	prx(example, attdefs);
	}

	public static void pres(Attribute[][] examples,  AttDef[] attdefs)  /* print all examples */
	{
	for (int i = 0; i < examples.length; i++)
	pre(examples[i], attdefs);
	System.out.println("");
	}


}

