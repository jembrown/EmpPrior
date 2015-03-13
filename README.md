![EmpPrior Logo](https://tigerbytes2.lsu.edu/users/jembrown/web/images/EmpPriorLogo.jpg "EmpPrior Logo")

# EmpPrior
Using empirical data to inform branch-length priors for Bayesian phylogenetics

## Overview

The proper approach to setting priors for Bayesian analyses has always been a point of contention [Efron 2013](http://www.sciencemag.org/content/340/6137/1177.summary) and phylogenetics is no exception. Prior choice has been shown to be particularly important for certain kinds of phylogenetic inferences, such as branch-length (e.g., [Brown et al. 2010](http://sysbio.oxfordjournals.org/content/59/2/145.short), [Marshall 2010](http://sysbio.oxfordjournals.org/content/59/1/108.short) and divergence-time estimation (e.g., [Nowak et al. 2013](http://www.plosone.org/article/info%3Adoi%2F10.1371%2Fjournal.pone.0066245#pone-0066245-g005). While much ongoing work is focusing on how to specify more robust default prior distributions (e.g., [Rannala et al. 2012](http://mbe.oxfordjournals.org/content/29/1/325.short)), the information contained in outside empirical datasets can also be leveraged to help set appropriate priors for new analyses.

`EmpPrior` is a software package that will quickly and efficiently query the [TreeBASE](http://www.treebase.org/) database to find datasets that are similar to a focal dataset (e.g., those that sample the same genes at roughly the same taxonomic level) and use the returned datasets to parameterize priors for future analyses. The portion of the program that searches [TreeBASE](http://www.treebase.org/) is written in Java and known as `EmpPrior-search`. To facilitate the use of these outside datasets for informing branch-length priors, `EmpPrior` includes accompanying R code (`EmpPrior-fit`) to fit popular branch-length prior distributions and return maximum-likelihood (ML) parameter estimates. These outside parameter estimates may then be used to specify informed priors for the focal dataset. The use of informed branch-length priors frequently results in improved branch-length estimates for datasets that have previously been shown to produce unreasonable estimates under common default priors ([Nelson et al. 2015](http://sysbio.oxfordjournals.org/content/early/2015/01/14/sysbio.syv003.abstract)).

While we have been motivated to develop `EmpPrior` for improving branch-length prior specification, the outside datasets that it returns can be used to inform priors for a wide variety of other parameters. It can also be used more broadly as a tool for efficient [TreeBASE](http://www.treebase.org/) searches.

## References

Brown J.M., Hedtke S.M., Lemmon A.R., Lemmon E.M. 2010. [When trees grow too long: Investigating the causes of highly inaccurate Bayesian branch-length estimates](http://sysbio.oxfordjournals.org/content/59/2/145.short). _Syst. Biol._ 59:145-161.

Efron B. 2013. [Bayes' theorem in the 21st century](http://www.sciencemag.org/content/340/6137/1177.summary). _Science_ 340:1177-1178.

Marshall D.C. 2010. [Cryptic failure of partitioned Bayesian phylogenetic analyses: Lost in the land of long trees](http://sysbio.oxfordjournals.org/content/59/1/108.short). _Syst. Biol._ 59:108-117.

Nelson B.J., Andersen J.J., Brown J.M. 2015. [Deflating trees: Improving Bayesian branch-length estimates using informed priors](http://sysbio.oxfordjournals.org/content/early/2015/01/14/sysbio.syv003.abstract). _Syst. Biol._ In Press.

Nowak M.D., Smith A.B., Simpson C., Zwickl D.J. 2013. [A simple method for estimating informative node age priors for the fossil calibration of molecular divergence time analyses](http://www.plosone.org/article/info%3Adoi%2F10.1371%2Fjournal.pone.0066245#pone-0066245-g005). _PLoS ONE_ 8:e66245.

Rannala B., Zhu T., Yang Z. 2012. [Tail paradox, partial identifiability, and influential priors in Bayesian branch length inference](http://mbe.oxfordjournals.org/content/29/1/325.short). _Mol. Biol. Evol._ 29:325-335.
