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
public class ConfusionMatrix {
    public int TP;
    public int TN;
    public int FP;
    public int FN;

    public static final int GMEAN = 0;
    public static final int ACCURACY = 1;
    public static final int FMEASURE = 2;

    public ConfusionMatrix(){
        this.TP = this.TN = this.FP = this.FN = 0;
    }

    public void reset(){
        this.TP = this.TN = this.FP = this.FN = 0;
    }

    public void copyTo(ConfusionMatrix dest){
        dest.TP = this.TP;
        dest.TN = this.TN;
        dest.FP = this.FP;
        dest.FN = this.FN;
    }

    public int getCorrect(){
        return this.TP + this.TN;
    }
    
    public float getPrecision(){
        return (float)this.TP/(this.TP + this.FP);
    }

    public float getSpecificity(){
        return (float)this.TN/(this.TN + this.FN);
    }

    public float getRecall(){
        return (float)this.TP/(this.TP + this.FN);
    }

    private float getFMeasure(){
        float precision = getPrecision();
        float recall = getRecall();
        return 2 * precision * recall / (precision + recall);   
       // return getGmean();
        //return getCorrect();
    }

    private float getGmean(){
         return getSpecificity() * getPrecision();
    }

    private float getAccuracy(){
        return (float)(this.TP + this.TN) / (this.TP + this.FP + this.TN + this.FN);
    }

    public float getQuality(int measure){
        switch(measure){
            case FMEASURE: return getFMeasure();
            case ACCURACY: return getAccuracy();
            case GMEAN: return getGmean();
        }
        return -1;
    }
}

