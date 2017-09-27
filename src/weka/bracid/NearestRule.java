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
class NearestRule {

        int ruleIndex;
        float dist;
        int symb;

        void set(int r, float dist, int symb) {
            this.dist = dist;
            this.ruleIndex = r;
            this.symb = symb;
        }
    }
