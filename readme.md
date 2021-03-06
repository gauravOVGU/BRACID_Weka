### BRACID Algorithm
The BRACID name is the acronym of Bottom-up induction of Rules And Cases for Imbalanced Data. It uses an integrated representation of rules and single instances and  using a less greedy bottom-up induction of rules from single examples with the specific generalization by looking for nearest examples to the rule. A new classification strategy is proposed with the nearest rule and a special treatment of the borderline or noisy examples.

### Algorithm description
A pseudo-code of the main procedure of BRACID is presented in Algorithm 1. Let us remark that its general loop, final hybrid representation and the idea of starting the search from a rule set corresponding to a set of training examples are inspired by RISE algorithm (Domingos 1996). Bracid Algorithm uses bottom-up induction technique, which considers each training instance as an individual rule and then each rule is generalized using nearby rules considering algorithm performance(F-measure). 
BRACID Algorithm
In each iteration, a rule is generalized using nearby rules and then F-measure is calculated for model with generalized rule. If f-measure value is improved or even if it is equal to initial F-measure value, generalized rule is added to the rule set. Main loop ends if rules can not be generalized furthermore. The output from the algorithm contains rules which includes set of generalized rules and instances which can be used to classify unseen instances.
### Classification
Classification can be done using hybrid rule set generated by BRACID algorithm using rule coverage and nearest rule functionality. To classify an unseen instance, set of rules are extracted from rule set which covers the instance and if the instance is not covered by any rule, then k nearest rules are extracted from rule set. 

*if rule covers the instance, 1/distance(rule, instance) = 1 
Then weighted vote is evaluated for each class label using extracted rules and the instance is classified with the class label having maximum votes.  Similar classification strategy is used while performing classification using rules generated by Hot Spot algorithm, but only rule is considered while calculating  the weighted vote for each class label.

### Results and evaluation
To evaluate the algorithms performance we are performing K-Fold validation using weka evaluation class. User can change various parameters in Bracid algorithm, e.g. classification neighborhood and number of folds. We are evaluating the models performance using selectivity and F-measure. Using selectivity we can examine the models performance in identifying minority class instances and selectivity provide overall insight on classifiers performance. Similar evaluation strategy is used to evaluate Hot spot and  Bayes Network performance.
