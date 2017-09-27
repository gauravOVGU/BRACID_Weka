/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weka.bracid;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.SubsetByExpression;

/**
 *
 * @author gaurav sharma
 */
public class BracidDriver {
    
    
    static Instances train;
    public static void main(String args[])
    {
    BRACID bracid = new BRACID();    
    File datafile = new File("C:\\Users\\djhaw\\netbeans\\SHIPMiner\\datasets\\diabetes.arff");
        try {
            inizialize(datafile,"");
            System.out.println("Training data set initiallized with "+ train.size()+" number of instances");
            bracid.buildClassifier(train);
            System.out.println("number of rules generated="+bracid.newWinningRules.length);
            
        } catch (Exception ex) {
            Logger.getLogger(BracidDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    static public void inizialize(File file, String subsetOptions) throws FileNotFoundException, IOException, Exception {
        ArrayList classLabels = new ArrayList();
        Instances dataInput;
        BufferedReader reader = null;
        System.out.println("Reader Start");
        reader = new BufferedReader(new FileReader(file));
        //reader = new BufferedReader(new FileReader("SHIP-2.arff"));
        Instances dataWithMissing = new Instances(reader);
        reader.close();
        System.out.println("Reader End");
        dataWithMissing.setClassIndex(dataWithMissing.numAttributes() - 1);
        classLabels.clear();
        //AttributeStats astats = dataAll.attributeStats(dataAll.classIndex());
        for (int i = 0; i < dataWithMissing.attribute(dataWithMissing.classIndex()).numValues(); i++) {
            classLabels.add(dataWithMissing.attribute(dataWithMissing.classIndex()).value(i));
        }

        // Apply user-defined subpopulation filter if specified
        if (!subsetOptions.isEmpty()) {
            SubsetByExpression subset = new SubsetByExpression();
            String[] subsetOptionsArray = {"-E", subsetOptions};
            subset.setOptions(subsetOptionsArray);
            subset.setInputFormat(dataWithMissing);
            dataInput = Filter.useFilter(dataWithMissing, subset);
        } else {
            dataInput = dataWithMissing;
        }
        dataInput.setClassIndex(dataInput.numAttributes() - 1);
        train = dataInput;
    }
}
