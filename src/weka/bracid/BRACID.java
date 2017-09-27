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
import java.util.Enumeration;
import java.util.Random;
import weka.classifiers.Classifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;

public class BRACID implements Classifier,OptionHandler {
    
    protected float[] coverages;
    protected static final int None = -1;
    int numExamples;
    int numAttributes;
    int numClasses;
    int numVals;
    int classIndex;
    protected Attribute[][] examples;
    protected WinningRule[] winningRules;
    protected WinningRule[] newWinningRules;
    protected ArrayList<Rule> rules;
    protected AttDef[] attdefs;
    float[][][] distances;
    int[] classfreqs;
    protected float avgDist = 0;
    protected float avgDist2 = 0;
    protected int classificationStrategy = 0;
    int minClassIndex;
    int minClassExamples;
    private static final int SAFE = 0;
    private static final int BORDER = 1;
    private static final int NOISE = 2;
    private static final int SAME_NEIGHBOURS = 0;
    private static final int ALL_NEIGHBOURS = 1;
    private static final int OTHER_NEIGHBOURS = 2;
   // int remove_noisy = AFTER;
   // private static final int BEFORE = 0;
   // private static final int AFTER = 1;
    //private boolean extend_against = true;
    private int qualityMeasure = ConfusionMatrix.FMEASURE;
   // private boolean ReplaceInMajority = false;
   // private int tmpOption = 2;
    int[] labels;
    int k = 5;
    //private boolean GenerateTopDown = false;
    int countSingleMin = 0;

    @Override
    public String toString() {
        //Utils.prls(rules, attdefs, examples);
        String ret = toStringHelper();
        return /*"iRISE\t"+*/ ret;
    }

    @Override
    public void buildClassifier(Instances arg0) throws Exception {
        classificationStrategy = 5;
        
        init(arg0);
        minClassExamples =arg0.size();
        for(int i=0;i<classfreqs.length;i++)
        {
        	if(minClassExamples>classfreqs[i])
        	{
        		minClassExamples=classfreqs[i];
        		minClassIndex=i;
        	}
        }
        minClassExamples = arg0.attributeStats(classIndex).nominalCounts[minClassIndex];
        labels = new int[numExamples];
        generateLabels(k);
        
        rise();
        Utils.prls(rules, attdefs, examples);

        System.out.println("built");

        coverages = new float[rules.size()];
        coverages = Utils.calculateCoverages(rules, examples, coverages);
        for (int i = 0; i < coverages.length; i++)
        {
        //System.out.println(coverages[i]);
        }
    }

    private void generateLabels(int k) {
        int[] neighbours = new int[k];
        int index;
        int sameCount = 0;
        int otherCount = 0;
        int clas = 0;
        for (int i = 0; i < numExamples; i++) {
            clas = examples[i][classIndex].symb;
            sameCount = 0;
            otherCount = 0;
            neighbours = findNearestExamples(i, k, neighbours, ALL_NEIGHBOURS);
            for (int j = 0; j < k; j++) {
                index = neighbours[j];
                if (index == -1) {
                    continue;
                }
                if (examples[index][classIndex].symb == clas) {
                    sameCount++;
                } else {
                    otherCount++;
                }
            }
            // if(sameCount == k)
            if (sameCount > otherCount) {
                labels[i] = SAFE;
            } else if (otherCount == k) {
                labels[i] = NOISE;
            } else {
                labels[i] = BORDER;
            }
        }
    }

    private void updateCM(ConfusionMatrix cm, boolean sameClassNew, boolean sameClassOld, boolean minorityClass) {

        if (sameClassNew != sameClassOld) {
            if (minorityClass) {
                if (sameClassOld) {
                    cm.FN++;
                    cm.TP--;
                } else { //sameClassNew=true
                    cm.FN--;
                    cm.TP++;
                }
            } else { //majorityClass
                if (sameClassOld) {
                    cm.FP++;
                    cm.TN--;
                } else { //sameClassNew=true
                    cm.FP--;
                    cm.TN++;
                }
            }
        }
    }

    private void calculateCurrentCM(ConfusionMatrix cm) {
        cm.reset();
        boolean sameClass;
        boolean minorityClass;
        for (int e = 0; e < numExamples; e++) {
            if (examples[e][classIndex].symb == None) //do not take into account noisy examples
            {
                continue;
            }
            sameClass = (winningRules[e].sameClass == true);
            minorityClass = (examples[e][classIndex].symb == minClassIndex);
            if (sameClass) {
                if (minorityClass) {
                    cm.TP++;
                } else {
                    cm.TN++;
                }
            } else {
                if (minorityClass) {
                    cm.FN++;
                } else {
                    cm.FP++;
                }
            }
        }
    }

    private int[] findNearestExamples(int r, int k, int[] nearestExamples, int flag) {
        float minDist = Utils.Infinity;
        for (int i = 0; i < k; i++) {
            nearestExamples[i] = None;
        }

        int n;
        if (flag == ALL_NEIGHBOURS) {
            n = numExamples;
        } else {
            n = minClassExamples;
            if ((flag == SAME_NEIGHBOURS && rules.get(r).ants[classIndex].symb != minClassIndex)
                    || (flag == OTHER_NEIGHBOURS && rules.get(r).ants[classIndex].symb == minClassIndex)) {
                n = numExamples - n;
            }
        }

        float[] dist = new float[n];
        int[] indices = new int[n];

        int index = 0;
        int minIndex = 0;

        for (int e = 0; e < numExamples; e++) {
            if (examples[e][classIndex].symb == None) //do not consider noisy examples
            {
                continue;
            }
            if ((flag == SAME_NEIGHBOURS && rules.get(r).ants[classIndex].symb == examples[e][classIndex].symb)
                    || (flag == OTHER_NEIGHBOURS && rules.get(r).ants[classIndex].symb != examples[e][classIndex].symb)
                    || (flag == ALL_NEIGHBOURS)) {
                dist[index] = distance(rules.get(r), examples[e], Utils.Infinity);
                indices[index] = e;
                //search for first minimum
                if (dist[index] > 0 && dist[index] < minDist) {
                    nearestExamples[0] = indices[index];
                    minDist = dist[index];
                    minIndex = index;
                }
                index++;
            }
        }

        //swap found minimum with first element
        dist[minIndex] = dist[0];
        indices[minIndex] = indices[0];

        //search for remaining minima
        for (int i = 1; i < k; i++) {
            minDist = Utils.Infinity;
            for (int j = i; j < dist.length; j++) {
                if (dist[j] > 0 && dist[j] < minDist) {
                    nearestExamples[i] = indices[j];
                    minDist = dist[j];
                    minIndex = j;
                }
            }
            //swap
            dist[minIndex] = dist[i];
            indices[minIndex] = indices[i];
        }

        return nearestExamples;
    }

    private boolean addBestNeighbour(int r, ConfusionMatrix oldCM, ConfusionMatrix newCM, int[] nearestExamples, int k, Attribute[] neighbour) {
        boolean improving = false;
        int newon, newcorr;
        float newDist;
        Rule originalRule = new Rule(numAttributes + 1);
        Rule rule = rules.get(r);
        Utils.copyrule(originalRule, rule);


        float bestFmeasure = oldCM.getQuality(qualityMeasure);
        float tmpFmeasure;
        int bestNeighbour = -1;
        Rule bestRule = new Rule(numAttributes + 1);
        ConfusionMatrix bestCM = new ConfusionMatrix();

        for (int i = 0; i < k; i++) {

            if (nearestExamples[i] == -1) {
                return improving;
            }

            if (nearestExamples[i] == -2) { //use the one given in parameter
                msg(originalRule, neighbour, rule, true);

            } else {

                /* generalize to cover nearest example of same class */
                msg(originalRule, examples[nearestExamples[i]], rule, true);
            }

            /* accept if there's an improvement on newly won examples */
            for (int e = 0; e < numExamples; e++) {
                boolean minorityExample = (examples[e][classIndex].symb == minClassIndex);

                if (winningRules[e].nearestRuleIndex != r //not already won by this rule
                        && (rule.fromExample != e/*r != e*/ || rule.nmerged > 1) //rule is not equal to this example
                        && examples[e][classIndex].symb != None //example was not removed (as noisy)
                        && ((newDist = distance(rule, examples[e],
                        winningRules[e].distance)) <= winningRules[e].distance)) //is closer than previous rule
                {                    
                    updateCM(newCM, (rule.ants[classIndex].symb == examples[e][classIndex].symb)/*(newWinningRules[e].sameClass == true)*/, (winningRules[e].sameClass == true), minorityExample);
                }
            }
            tmpFmeasure = newCM.getQuality(qualityMeasure);

            if ((neighbour != null || tmpFmeasure > bestFmeasure /* if = : Occam's razor */
                    || tmpFmeasure == bestFmeasure)
                    && !Utils.eqrule(originalRule, rule)) {               
                bestFmeasure = tmpFmeasure;
                bestNeighbour = i;
                newCM.copyTo(bestCM);
                Utils.copyrule(bestRule, rule);
            }
            oldCM.copyTo(newCM);
        }

        if (bestNeighbour == -1) {
            Utils.copyrule(rule, originalRule);
            oldCM.copyTo(newCM);
            return false;
        }

        Utils.copyrule(rule, bestRule);
        bestCM.copyTo(newCM);

        newon = 0;
        newcorr = 0;
        for (int e = 0; e < numExamples; e++) {
            if (winningRules[e].nearestRuleIndex != r //not already won by this rule
                    && (rule.fromExample != e/*r != e*/ || rule.nmerged > 1) //rule is not equal to this example
                    && examples[e][classIndex].symb != None //example was not removed (as noisy)
                    && ((newDist = distance(rule, examples[e],
                    winningRules[e].distance)) <= winningRules[e].distance)) //is closer than previous rule
            {
                newWinningRules[e].set(r, (rule.ants[classIndex].symb == examples[e][classIndex].symb), newDist);
                newon++;
                if (newWinningRules[e].sameClass == true) {
                    newcorr++;
                }
            } else {
                newWinningRules[e].nearestRuleIndex = None;
            }
        }
        //corect new rule statistics
        rule.nwon += newon;
        rule.ncorr += newcorr;
        rule.acc = LaplaceAcc(r);

        for (int e = 0; e < numExamples; e++) {
            if (newWinningRules[e].nearestRuleIndex == r) {
                //correct statistics for rule that lost their examples in favor of r
                rules.get(winningRules[e].nearestRuleIndex).nwon--;

                if (winningRules[e].sameClass == true) {
                    rules.get(winningRules[e].nearestRuleIndex).ncorr--;
                }
                rules.get(winningRules[e].nearestRuleIndex).acc = LaplaceAcc(winningRules[e].nearestRuleIndex);
                //correct statistics for examples won by r
                winningRules[e].set(r, newWinningRules[e].sameClass, newWinningRules[e].distance);
            }
        }

        delduplics(r);

        newCM.copyTo(oldCM);

        return true;
    }
    protected void delduplics(int r) {
        boolean same;

        for (int q = 0; q < rules.size(); q++) {
            if (rules.get(q).nmerged == -1) {
                continue;
            }
            same = true;
            for (int a = 0; a <= numAttributes; a++) /* a=natts: class should also be the same */ {
                if (!(rules.get(r).ants[a].type == Utils.Any
                        && rules.get(q).ants[a].type == Utils.Any
                        || rules.get(r).ants[a].type == Utils.Symbolic
                        && rules.get(q).ants[a].type == Utils.Symbolic
                        && rules.get(r).ants[a].symb == rules.get(q).ants[a].symb || rules.get(r).ants[a].type == Utils.Numeric
                        && rules.get(q).ants[a].type == Utils.Numeric
                        && rules.get(r).ants[a].lower == rules.get(q).ants[a].lower
                        && rules.get(r).ants[a].upper == rules.get(q).ants[a].upper)) {
                    same = false;
                    break;
                }
            }
            if (same && r != q) {
                rules.get(r).nmerged += rules.get(q).nmerged;
                rules.get(q).nmerged = -1; //rules with nmerged = -1 are not taken into account in any procedures
                rules.get(r).nwon += rules.get(q).nwon;
                rules.get(r).ncorr += rules.get(q).ncorr;
                rules.get(r).acc = LaplaceAcc(r);
                for (int e = 0; e < numExamples; e++) {
                    if (winningRules[e].nearestRuleIndex == q) {
                        winningRules[e].nearestRuleIndex = r;
                    }
                }
            }
        }


    }

    protected float LaplaceAcc(int r) {
        return ((float) rules.get(r).ncorr + 1) / (rules.get(r).nwon + numClasses);
    }

    protected Rule msg(Rule fromRule, Attribute[] example, Rule toRule, boolean cloneFirst) {
        if (cloneFirst) {
            Utils.copyrule(toRule, fromRule);
        }

        if (fromRule.ants[classIndex].symb == example[classIndex].symb) { //pokryj przyklad, jezeli z tej samej klasy
            for (int a = 0; a < numAttributes; a++) {
                if (fromRule.ants[a].type == Utils.Any //rule already dropped this attribute
                        || example[a].type == Utils.Any //example has a missing value
                        || (example[a].type == Utils.Symbolic
                        && example[a].symb == fromRule.ants[a].symb) //rule and example are the same on this attribute
                        || (example[a].type == Utils.Numeric //example's value is already covered by rule
                        && example[a].lower <= fromRule.ants[a].upper
                        && example[a].lower >= fromRule.ants[a].lower)) {
                    continue;
                } else if (example[a].type == Utils.Symbolic) //drop if different value on symbolic attribute
                {
                    toRule.ants[a].type = Utils.Any;
                } else if (example[a].type == Utils.Numeric) { //generalize to cover new numeric value
                    if (example[a].lower > fromRule.ants[a].upper) {
                        toRule.ants[a].upper = (float) (example[a].upper + (numExamples > 3000 ? 0.05 * (attdefs[a].max - attdefs[a].min)
                                : 0));
                    } else /* ie. if (example[a].lower < rule.ants[a].lower) */ {
                        toRule.ants[a].lower = (float) (example[a].lower - (numExamples > 3000 ? 0.05 * (attdefs[a].max - attdefs[a].min)
                                : 0));
                    }
                }
            }
        } else {//extend against
            for (int a = 0; a < numAttributes; a++) {
                if (fromRule.ants[a].type == Utils.Any //rule already dropped this attribute
                        || example[a].type == Utils.Any) //example has a missing value
                {
                    continue;
                } else if (example[a].type == Utils.Symbolic) {
                    if (example[a].symb != fromRule.ants[a].symb) //rule and example are different on this attribute
                    {
                        continue;
                    } else {
                        continue;
                    }
                } else if (example[a].type == Utils.Numeric) { //example's value is already covered by rule
                    if (example[a].upper <= fromRule.ants[a].upper
                            && example[a].lower >= fromRule.ants[a].lower) //rule covers this example
                    {
                        continue;
                    }
                    if (example[a].upper > fromRule.ants[a].upper) //extend right margin
                    {
                        toRule.ants[a].upper += 0.9 * (example[a].upper - fromRule.ants[a].upper);
                    }
                    if (example[a].lower < fromRule.ants[a].lower) //extend right margin //extend left margin
                    {
                        toRule.ants[a].lower -= 0.9 * (fromRule.ants[a].lower - example[a].lower);
                    }
                }
            }
        }
        return toRule;
    }

    
    private boolean addAllGoodNeighbours(int r, ConfusionMatrix oldCM, ConfusionMatrix newCM, int[] nearestExamples, int k) {
        boolean improving = false;
        int newon, newcorr;
        float newDist;
        Rule oldRule = new Rule(numAttributes + 1);
        Rule originalRule = new Rule(numAttributes + 1);
        Utils.copyrule(originalRule, rules.get(r));
        boolean replaced = false;

        for (int i = 0; i < k; i++) {

            if (nearestExamples[i] == -1) {
                return improving;
            }

            if (!replaced) {//replace first one
                Utils.copyrule(oldRule, rules.get(r));
                msg(originalRule, examples[nearestExamples[i]], rules.get(r), true);
            } else { //add remaining ones
                if (rules.size() > examples.length * k) {
                    return improving;
                }
                Rule newRule = new Rule(numAttributes + 1);
                msg(originalRule, examples[nearestExamples[i]], newRule, true);
                newRule.nwon = 0;
                newRule.ncorr = 0;
                newRule.acc = (float) classfreqs[newRule.ants[classIndex].symb]
                        / numExamples;
                newRule.nmerged = 1;

                rules.add(newRule);
                r = rules.size() - 1;
            }

            /* accept if there's an improvement on newly won examples */
            newon = 0;
            newcorr = 0;
            for (int e = 0; e < numExamples; e++) {
                boolean minorityExample = (examples[e][classIndex].symb == minClassIndex);

                if (winningRules[e].nearestRuleIndex != r //not already won by this rule
                        && (rules.get(r).fromExample != e/*r != e*/ || rules.get(r).nmerged > 1) //rule is not equal to this example
                        && examples[e][classIndex].symb != None //example was not removed (as noisy)
                        && ((newDist = distance(rules.get(r), examples[e],
                        winningRules[e].distance)) <= winningRules[e].distance)) //is closer than previous rule
                {
                    newWinningRules[e].set(r, (rules.get(r).ants[classIndex].symb == examples[e][classIndex].symb), newDist);
                    newon++;
                    if (newWinningRules[e].sameClass == true) {
                        newcorr++;
                    }
                    updateCM(newCM, (newWinningRules[e].sameClass == true), (winningRules[e].sameClass == true), minorityExample);
                } else {
                    newWinningRules[e].nearestRuleIndex = None;
                }
            }
            float newQuality = newCM.getQuality(qualityMeasure);
            float oldQuality = oldCM.getQuality(qualityMeasure);
            if (newQuality > oldQuality /* if = : Occam's razor */
                    || newQuality == oldQuality
                    && !Utils.eqrule(originalRule, rules.get(r))) {
                improving = true;
                //corect new rule statistics
                rules.get(r).nwon += newon;
                rules.get(r).ncorr += newcorr;
                rules.get(r).acc = LaplaceAcc(r);

                for (int e = 0; e < numExamples; e++) {
                    if (newWinningRules[e].nearestRuleIndex == r) {
                        //correct statistics for rule that lost their examples in favor of r
                        rules.get(winningRules[e].nearestRuleIndex).nwon--;
                        if (winningRules[e].sameClass == true) {
                            rules.get(winningRules[e].nearestRuleIndex).ncorr--;
                        }
                        rules.get(winningRules[e].nearestRuleIndex).acc = LaplaceAcc(winningRules[e].nearestRuleIndex);
                        //correct statistics for examples won by r
                        winningRules[e].set(r, newWinningRules[e].sameClass, newWinningRules[e].distance);
                    }
                }
                delduplics(r);
                newCM.copyTo(oldCM);
                replaced = true;
            } else {
                if (!replaced) //get back the previous rule in place of this one
                {
                    Utils.copyrule(rules.get(r), oldRule);
                } else //remove new rule
                {
                    rules.remove(r);
                }
                oldCM.copyTo(newCM);
            }
        }
        return improving;
    }
    
    boolean processMajorityRule(int r, ConfusionMatrix oldCM, ConfusionMatrix newCM, int[] nearestExamples, int k, boolean first_it) {

        boolean improved;

        improved = addBestNeighbour(r, oldCM, newCM, nearestExamples, k, null);

        if (!improved) {
            if (first_it) {//processNoisy;
                rules.get(r).nmerged = -1;
                examples[r][classIndex].symb = None;
                correctWinningRules(r, newCM, oldCM);
                // counterNoise++;
            } else {
                rules.get(r).nmerged = 0;
            }
        }

        return improved;
    }

    private void correctWinningRules(int r, ConfusionMatrix newCM, ConfusionMatrix oldCM) {

        for (int e = 0; e < examples.length; e++) {

            if (winningRules[e].nearestRuleIndex != r) {
                continue;
            }

            if (examples[e][classIndex].symb == None) {
                continue;
            }

            boolean sameClassOld = (rules.get(r).ants[classIndex].symb == examples[e][classIndex].symb);

            NearestRule rule = new NearestRule();

            boolean minorityExample = (examples[e][classIndex].symb == minClassIndex);

            findNearestRule(examples[e], rule, e);
            winningRules[e].set(rule.ruleIndex, (examples[e][classIndex].symb == rule.symb), rule.dist);
            rules.get(rule.ruleIndex).nwon++;
            if (winningRules[e].sameClass == true) {
                rules.get(rule.ruleIndex).ncorr++;
            }
            rules.get(rule.ruleIndex).acc = LaplaceAcc(rule.ruleIndex);

            updateCM(newCM, (rules.get(rule.ruleIndex).ants[classIndex].symb == examples[e][classIndex].symb), sameClassOld, minorityExample);
        }

        newCM.copyTo(oldCM);
    }

    private boolean isCloser(NearestRule newRule, NearestRule currRule, int strategy) {
        int fr = 0, fn = 0;
        float ac = 0, an = 0;
        Random rand = new Random();

        if (newRule.dist > currRule.dist) {
            return false;
        }
        if (newRule.dist < currRule.dist) {
            return true;
        }

        //standard RISE procedure
            //newRule.lwstdist == currRule.lwstdist
            if ((ac = rules.get(newRule.ruleIndex).acc) > (an = rules.get(currRule.ruleIndex).acc)) {
                return true;
            }

            if (ac == an && (fr = classfreqs[newRule.symb]) > (fn = classfreqs[currRule.symb])) //jesli rowna odleglosc i trafnosc, to wybierz klase wiekszosciowa!!!
            {
                return true;
            }

            if (ac == an && fr == fn && ((float) rand.nextFloat() > 0.5)) {
                return true;
            }
           
        return false;
    }

    
    protected NearestRule findNearestRule(Attribute[] ex, NearestRule nearestRule, int leftout) {
        NearestRule tmpRule = new NearestRule();

        nearestRule.ruleIndex = None;
        nearestRule.dist = Utils.Infinity;
        nearestRule.symb = rules.get(0).ants[classIndex].symb;
        for (int r = 0; r < rules.size(); r++) {
            if (r != leftout && rules.get(r).nmerged != -1) {
                tmpRule.set(r, distance(rules.get(r), ex, nearestRule.dist), rules.get(r).ants[classIndex].symb);
                if (isCloser(tmpRule, nearestRule, classificationStrategy)) {
                    nearestRule.set(tmpRule.ruleIndex, tmpRule.dist, tmpRule.symb);
                }
            }
        }
        return nearestRule;
    }

    @Override
    public double classifyInstance(Instance instnc) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double[] distributionForInstance(Instance instnc) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Capabilities getCapabilities() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    private boolean processMinorityRule(int r, ConfusionMatrix oldCM, ConfusionMatrix newCM, int[] nearestExamples, int k, boolean first_it) {

        boolean improved = false;        

            boolean safe = false;
            if (labels[rules.get(r).fromExample] == SAFE) {
                safe = true;
            }

            if (safe) {
                improved = addBestNeighbour(r, oldCM, newCM, nearestExamples, k, null);
            } else {
                improved = addAllGoodNeighbours(r, oldCM, newCM, nearestExamples, k);
            }
        

        if (!improved && !first_it) //probably noise...do not try in the next iterations.
        {
                int[] nearestOther = new int[k];
                nearestOther = findNearestExamples(r, k, nearestOther, OTHER_NEIGHBOURS);

                if (nearestOther[0] != -1) {
                    int[] dummy = new int[1];
                    dummy[0] = -2;
                    Attribute[] maxNeighbour = new Attribute[numAttributes + 1];
                    getMaxNeighbour(nearestOther, maxNeighbour, rules.get(r));
                    addBestNeighbour(r, oldCM, newCM, dummy, 1, maxNeighbour);                  
                }
                
            rules.get(r).nmerged = 0;
        }
        return improved;
    }

    private void getMaxNeighbour(int[] otherNeighbours, Attribute[] maxNeighbour, Rule rule) {

        for (int j = 0; j <= numAttributes; j++) {
            maxNeighbour[j] = new Attribute();
        }

        Utils.copyatt(maxNeighbour[classIndex], examples[otherNeighbours[0]][classIndex]);
        for (int a = 0; a < numAttributes; a++) {
            maxNeighbour[a].type = examples[otherNeighbours[0]][a].type;
            maxNeighbour[a].lower = -Utils.Infinity;
            maxNeighbour[a].upper = Utils.Infinity;
            if(maxNeighbour[a].type == Utils.Symbolic)
                maxNeighbour[a].symb = rule.ants[a].symb;            
        }

        for (int i = 0; i < otherNeighbours.length; i++) {

            if (otherNeighbours[i] == -1) {
                return;
            }

            Attribute[] example = examples[otherNeighbours[i]];

            for (int a = 0; a < numAttributes; a++) {
                if (example[a].type == Utils.Numeric) {
                    if (example[a].lower < rule.ants[a].lower) {//extend to the left
                        if (example[a].lower > maxNeighbour[a].lower) {
                            maxNeighbour[a].lower = example[a].lower;
                        }
                    }
                    if (example[a].lower > rule.ants[a].upper) {//extend to the right
                        if (example[a].lower < maxNeighbour[a].upper) {
                            maxNeighbour[a].upper = example[a].lower;
                        }
                    }
                }
            }
        }
        //replace missing by the rule values
        for (int a = 0; a < numAttributes; a++) {
            if (maxNeighbour[a].type == Utils.Numeric) {
                if (maxNeighbour[a].lower == -Utils.Infinity) {
                    maxNeighbour[a].lower = rule.ants[a].lower;
                }
                if (maxNeighbour[a].upper == Utils.Infinity) {
                    maxNeighbour[a].upper = rule.ants[a].upper;
                }
            }
        }
    }

    private void removeNoisyUsingLabels() {

        for (int r = 0; r < rules.size(); r++) {

            if (rules.get(r).nmerged > 0 && labels[r] == NOISE) {
                if (rules.get(r).ants[classIndex].symb == minClassIndex) {
                    rules.get(r).nmerged = 0;
                } else {
                    rules.get(r).nmerged = -1;
                    examples[r][classIndex].symb = None;
                }
            }
        }
    }
    protected void ibl() {
        NearestRule rule = new NearestRule();

        for (int e = 0; e < numExamples; e++) {
            findNearestRule(examples[e], rule, e);
            winningRules[e].set(rule.ruleIndex, (examples[e][classIndex].symb == rule.symb), rule.dist);
            rules.get(rule.ruleIndex).nwon++;
            if (winningRules[e].sameClass == true) {
                rules.get(rule.ruleIndex).ncorr++;
            }
        }
        for (int i = 0; i < numExamples; i++) {
            rules.get(i).acc = LaplaceAcc(i);
        }
    }

    protected void rise() {
        
        countSingleMin = 0;
        int countSingleMaj = 0;

        ibl();

        ConfusionMatrix oldCM = new ConfusionMatrix();
        ConfusionMatrix newCM = new ConfusionMatrix();

        calculateCurrentCM(oldCM);
        oldCM.copyTo(newCM);

        boolean improving;
        boolean imp = false;
        int iterations = 0;

        int[] nearestExamples = new int[k];

        int k_maj = 1;       

        do {
            improving = false;

            int size = rules.size();

            //   System.out.println("iteration "+iterations+" size "+size);

            for (int r = 0; r < size; r++) {

                if (rules.get(r).nmerged > 0) { //do not process =-1 (merged) and =0 (marked not to process in further iterations)

                    /* find nearest examples*/
                    if (rules.get(r).ants[classIndex].symb == minClassIndex) {
                        nearestExamples = findNearestExamples(r, k, nearestExamples, SAME_NEIGHBOURS);
                        imp = processMinorityRule(r, oldCM, newCM, nearestExamples, k, iterations == 0);
                    } else {
                        // if (classificationStrategy == 5){
                        if (iterations == 0 || labels[rules.get(r).fromExample] == SAFE) {
                            k_maj = 1;
                        } else {
                            k_maj = k;
                        }
                        //  }
                        nearestExamples = findNearestExamples(r, k_maj, nearestExamples, SAME_NEIGHBOURS);
                        imp = processMajorityRule(r, oldCM, newCM, nearestExamples, k_maj, iterations == 0);
                    }

                    if (imp) {
                        improving = true;
                    } else {
                        if(iterations == 0){
                        if (rules.get(r).ants[classIndex].symb == minClassIndex)
                            countSingleMin++;
                         else
                            countSingleMaj++;
                        }
                    }
                }
            }

            iterations++;
        } while (improving);

        //  printRuleStats(minClassIndex);
       // System.out.print(countSingleMaj + "\t" + countSingleMin + "\t");
    }

    //######################### parameters (not used) ###############################################################

    protected String toStringHelper() {
        int counterMin = 0;
        int counterMaj = 0;
        float counterLengthMaj = 0;
        float counterLengthMin = 0;
        float strengthMin = 0;
        float strengthMaj = 0;
        int singleCases = 0;

        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).nmerged != -1) {
                if (rules.get(i).ants[classIndex].symb == 0) {
                    counterMin++;
                    counterLengthMin += Utils.getLength(rules.get(i).ants);
                    int strength  = Utils.getStrength(rules.get(i), examples);
                    strengthMin += strength;
                    if (strength == 1){
                        singleCases++;
                    }
                } else {
                    counterMaj++;
                    counterLengthMaj += Utils.getLength(rules.get(i).ants);
                    strengthMaj += Utils.getStrength(rules.get(i), examples);
                }
            }
        }

        counterLengthMaj /= counterMaj;
        counterLengthMin /= counterMin;
        strengthMaj /= counterMaj;
        strengthMin /= counterMin;


        return String.format("%d\t%d\t%.2f\t%.2f\t%.2f\t%.2f\t%d",
                counterMin,
                counterMaj,
                strengthMin,
                strengthMaj,
                counterLengthMin,
                counterLengthMaj,
                singleCases
                );
    }
    protected void init(Instances instances) {
        alocateTables(instances);
        initAttDefs(instances);
        initExamples(instances);
        initClassFreqs(instances);
        // initDistances
        compvdm();
        initrules();
    }
    private void alocateTables(Instances instances) {
        classIndex = instances.numAttributes() - 1;
        numExamples = instances.numInstances();
        numAttributes = instances.numAttributes() - 1;
        numClasses = instances.classAttribute().numValues();
        numVals = getMaxVals(instances);

        examples = new Attribute[numExamples][numAttributes + 1];
        winningRules = new WinningRule[numExamples];
        newWinningRules = new WinningRule[numExamples];
        rules = new ArrayList<Rule>();
        attdefs = new AttDef[numAttributes + 1];
        distances = new float[numAttributes][numVals][numVals];

        for (int i = 0; i < numExamples; i++) {
            winningRules[i] = new WinningRule();
            newWinningRules[i] = new WinningRule();
            rules.add(new Rule(numAttributes + 1));

            for (int j = 0; j <= numAttributes; j++) {
                examples[i][j] = new Attribute();
            }
        }
        for (int i = 0; i <= numAttributes; i++) {
            attdefs[i] = new AttDef(numVals);
        }
    }

    private int getMaxVals(Instances instances) {
        int max = 0;
        for (int i = 0; i < instances.numAttributes(); i++) {
            if (instances.attribute(i).isNominal()) {
                max = Math.max(instances.attribute(i).numValues(), max);
            }
        }
        return max + 1; // 1 for missing value
    }
    
    private void initAttDefs(Instances instances) {
        // przepisz definicje atrybutow
        for (int i = 0; i < instances.numAttributes(); i++) {
            if (instances.attribute(i).isNominal()) {
                attdefs[i].type = Utils.Symbolic;
                attdefs[i].name=instances.attribute(i).name();
                attdefs[i].nvals = instances.attribute(i).numValues();
                attdefs[i].nmiss = instances.attributeStats(i).missingCount;
                for (int j = 0; j < attdefs[i].nvals; j++) {
                    attdefs[i].vals[j] = instances.attribute(i).value(j);
                }
            } else {
                attdefs[i].type = Utils.Numeric;
                attdefs[i].name=instances.attribute(i).name();
                attdefs[i].min = (float) instances.attributeStats(i).numericStats.min;
                attdefs[i].max = (float) instances.attributeStats(i).numericStats.max;
                attdefs[i].avg = (float) instances.attributeStats(i).numericStats.mean;
            }
        }
    }

    private void initExamples(Instances instances) { 
        for (int i = 0; i < numExamples; i++) {
            int k = i;
            for (int j = 0; j < instances.numAttributes(); j++) {
                if ((attdefs[j].type == Utils.Symbolic)
                        || (attdefs[j].type == Utils.Class)) {
                    examples[i][j].type = Utils.Symbolic;
                    if (instances.instance(k).isMissing(j)) {
                        examples[i][j].symb = attdefs[j].nvals;
                    } else {
                        examples[i][j].symb = (int) instances.instance(k).value(j);
                    }
                } else if (attdefs[j].type == Utils.Numeric) {
                    if (instances.instance(k).isMissing(j)) {
                        examples[i][j].type = Utils.Any;
                    } else {
                        examples[i][j].type = Utils.Numeric;
                        examples[i][j].lower = (float) instances.instance(k).value(j);
                        examples[i][j].upper = examples[i][j].lower;
                    }
                }
            }
        }
    }
    private void initClassFreqs(Instances instances) {
        classfreqs = instances.attributeStats(instances.classIndex()).nominalCounts;
    }
    
    private void compvdm() {
        int nvals;
        int[] valfreqs = new int[numVals];
        float[][] classvalfreqs = new float[numClasses][numVals];

        for (int a = 0; a < numAttributes; a++) {
            if (attdefs[a].type == Utils.Symbolic) {
                nvals = attdefs[a].nvals;
                for (int v = 0; v <= nvals; v++) {
                    valfreqs[v] = 0;
                    for (int c = 0; c < numClasses; c++) {
                        classvalfreqs[c][v] = 0;
                    }
                }
                for (int e = 0; e < numExamples; e++) {
                    valfreqs[examples[e][a].symb]++;
                    classvalfreqs[examples[e][classIndex].symb][examples[e][a].symb] += 1;
                }
                for (int v = 0; v <= nvals; v++) {
                    for (int c = 0; c < numClasses; c++) {
                        if (valfreqs[v] != 0) {
                            classvalfreqs[c][v] = classvalfreqs[c][v]
                                    / valfreqs[v];
                        }
                    }
                }
                for (int v = 0; v <= nvals; v++) {
                    for (int w = 0; w <= nvals; w++) {
                        if (valfreqs[v] == 0 || valfreqs[w] == 0) {
                            distances[a][v][w] = 1;
                        } else {
                            distances[a][v][w] = 0;
                            for (int c = 0; c < numClasses; c++) {
                                distances[a][v][w] += Math.abs(classvalfreqs[c][v]
                                        - classvalfreqs[c][w]);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void initrules() {
        int a, e;

        /* initialize rules */
        for (e = 0; e < numExamples; e++) {
            for (a = 0; a <= numAttributes; a++) {
                Utils.copyatt(rules.get(e).ants[a], examples[e][a]);
            }
            rules.get(e).nmerged = 1;
            rules.get(e).nwon = rules.get(e).ncorr = 0;
            rules.get(e).acc = (float) classfreqs[rules.get(e).ants[classIndex].symb]
                    / numExamples;
            rules.get(e).fromExample = e;
        }
    }
    protected float distance(Rule rule, Attribute[] example, float mindist) {
        float dist = 0, di;
        Attribute example_att, rule_ant;

        for (int a = 0; a < numAttributes; a++) {
            if (dist > mindist) {
                break;
            } else {
                example_att = example[a];
                rule_ant = rule.ants[a];
                if (Utils.covers(example_att, rule_ant)) {
                    continue; //add zero to a distance
                } else if (example_att.type == Utils.Symbolic) {
                    dist += Math.pow(distances[a][rule_ant.symb][example_att.symb], 2);
                } else if (example_att.type == Utils.Numeric) {
                    di = Math.min(Math.abs(rule_ant.lower - example_att.lower),
                            Math.abs(rule_ant.upper - example_att.lower))
                            / (attdefs[a].max - attdefs[a].min);
                    dist += Math.pow(di, 2);
                }
            }
        }
        return dist;
    }

	@Override
	public String[] getOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<Option> listOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setOptions(String[] arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
}


