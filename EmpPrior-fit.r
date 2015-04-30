#!/usr/bin/Rscript --vanilla --slave

# Usage: ./EmpPrior-fit.r [path/to/nexus1.nex path/to/nexus2.nex || folder=path/to/files folder=path/to/other/files] 
#                         [outfile=path/to/EmpPrior-fit.out]

# Filenames cannot contain "="
# Multiple files or folders can be specified
# Treefiles must be in nexus format

# Updates: 
# v0.1.1 -> v0.2: The compound Dirichlet negative log-likelihood function has been updated
#					to exactly match equation 36 from Rannala et al. (2011, MBE, 29:325-335).
#					The 0.1.1 version of this function returned reasonable parameter estimates
#					but incorrect likelihoods. The likelihoods returned by 0.2 should be reliable
#					and are now reported as part of the program output. 

# Please send any bug reports to jembrown@lsu.edu.

### Begin function definitions ###
# Parse command line arguments

parseArgs = function(){
    args = commandArgs(trailingOnly=T)
    args = sapply(args, function(x) as.character(x))
    
    # Return usage/help if no input or --help flag is set
    index.help = grep("--help", args, ignore.case=TRUE)
    if(length(index.help) > 0 || length(args) == 0){
        print("EmpPrior-fit: fit branch length distributions to trees or sequence data")
        print("Usage: ./EmpPrior-fit.r [path/to/nexus1.nex path/to/nexus2.nex")
        print("|| infolder=path/to/files/ infolder=path/to/other/files] [outfile=path/to/EmpPrior-fit.out]")
        print("Trees and sequence data must be in nexus format")
        quit(save = "no", status = 0)
    }

    index.outfile = grep("outfile=", args, ignore.case=TRUE)
    if(length(index.outfile) == 1){
        outfile = as.character(unlist(strsplit(args[index.outfile], "="))[2])
        if(is.na(outfile)){
            print("Outfile must be coercible to a character string (e.g. outfile=outfile.out).")
            quit(save = "no", status = 1, runLast = FALSE)
        } else {
            args = args[-index.outfile]
        }
    } else if(length(index.outfile) == 0) {
        outfile = NULL
    } else stop("Outfile must not be specified multiple times.")
    
    index.folders = grep("folder=", args, ignore.case=TRUE)
    if(length(index.folders) > 0){
        folders = vector()
        files = vector()
        for(i in 1:length(index.folders)){
            folders[i] = as.character(unlist(strsplit(args[index.folders[i]], "="))[2])
            if(!is.na(folders[i])){
                args = args[-index.folders[i]]
                tempfiles = list.files(folders[i])
                tempfiles = sapply(tempfiles, function(x) paste(folders[i], x, sep="/"))
                files = append(files, tempfiles)
            } else {
                print(paste("Folder name", folders[i], "must be coercible to a character string (e.g. folder=path/to/folder)."))
                quit(save = "no", status = 1, runLast = FALSE)
            }
        }
    } else if(length(index.folders) == 0) {
        files = sapply(args, function(x) as.character(x))
    }

    # Check for DATA and TREES block in each infile
    file.inferTree = vector(length=length(files))
    if(length(files) > 0){
        for(i in 1:length(files)){
            if(!is.na(files[i])){
                text = readLines(con = files[i])
                text.tree = grep("begin trees;", text, ignore.case=TRUE)
                text.data = grep("begin data;", text, ignore.case=TRUE)
                if(length(text.tree) > 0){
                        file.inferTree[i] = FALSE
                } else if(length(text.data) > 0){
                    file.inferTree[i] = TRUE
                } else {
                    file.inferTree[i] = NA
                    print(paste("Data in file", files[i], "not recognized. Skipping..."))
                } 
            } else {
                print(paste("Name of infile", files[i], "must be coercible to a character string. Skipping..."))
                file.inferTree[i] = NA
            }
        }
    } 
    
    # Remove unrecognized files from list
    i = 1
    while(i <= length(files)){
        if(is.na(file.inferTree[i])){
            files = files[-i]
            file.inferTree = file.inferTree[-i]
        } else i = i + 1
    }
    
    # Make sure at least one file can be analyzed
    if(length(files) == 0) stop("No files selected for analysis")
    
    return(list(outfile = outfile, files = files, inferTree = file.inferTree))
}

# Negative log likelihood calculators
    
expon.NLL = function(rate = 0){
    -sum(dexp(dat$el, rate=exp(rate), log=TRUE))
}

gamma.NLL = function(shape = 0, rate = 0) {
    -sum(dgamma(dat$el, shape = exp(shape), rate = exp(rate), log=TRUE))
}

compDirichlet.NLL = function(shape=0, beta=0, concentration=0, c=0){
	len_tip = length(tip.length)
	len_internal = length(internal.length)
	len_T = sum(tip.length) + sum(internal.length)
	shp = exp(shape)
	bt = exp(beta)
	cnc = exp(concentration)
	expc = exp(c)
	logB = len_tip * lgamma(cnc) + len_internal * lgamma(cnc * expc) - lgamma(len_tip * cnc + len_internal * cnc * expc)
	logLike = shp * log(bt) - lgamma(shp) - bt * len_T + (shp - 1) * log(len_T) - logB +    
	sum(sapply(tip.length, function(x) log(x ^ (cnc - 1)))) +
	sum(sapply(internal.length, function(x) log(x ^ (cnc * expc - 1)))) +
        (-cnc * len_tip - cnc * expc * len_internal + 1) * log(len_T)
	return(-logLike)
}

models.aicc = function(names = c("fit.expon", "fit.gamma", "fit.compDirichlet.T", "fit.compDirichlet.Ta", 
                                 "fit.compDirichlet.Tac", "fit.compDirichlet.Tc", "fit.compDirichlet.a", 
                                 "fit.compDirichlet.ac", "fit.compDirichlet.c"), 
                       file, digits = 3){
    fits = sapply(names, function(x) get(x))
    AICc = function(fit){
        2*fit@details$value + 2 * length(fit@coef) * fit@nobs / (fit@nobs - length(fit@coef) - 1)
    }

    dAICc = function(AICc.vec){
        AICc.vec - min(AICc.vec)
    }

    wAICc = function(dAICc.vec){
        exp(-0.5 * dAICc.vec)/sum(exp(-0.5 * dAICc.vec))
    }

    alphaT.calc = function(fit, name){
        if(name == "fit.expon") fit@nobs
        else if(name == "fit.gamma") 
            if(!is.na(fit@coef["shape"])) exp(fit@coef["shape"]) * fit@nobs else 1
        else if(!is.na(fit@coef["shape"])) exp(fit@coef["shape"]) else 1
    }

    betaT.calc = function(fit, name){
        if(name == "fit.expon") exp(fit@coef["rate"])
        else if(name == "fit.gamma") 
            if(!is.na(fit@coef["rate"])) exp(fit@coef["rate"]) else 1
        else if(!is.na(fit@coef["beta"])) exp(fit@coef["beta"]) else 1
    }

    concentration.calc = function(fit, name){
        if(name == "fit.expon" || name == "fit.gamma") 1
        else if(!is.na(fit@coef["concentration"])) exp(fit@coef["concentration"]) else 1
    }

    bl.ratio.calc = function(fit, name){
        if(name == "fit.expon" || name == "fit.gamma") 1
        else if(!is.na(fit@coef["c"])) exp(fit@coef["c"]) else 1
    }
    
    negLogL = sapply(fits, function(x) x@details$value)
    aicc = sapply(fits, function(x) AICc(x))
    df = sapply(fits, function(x) length(x@coef))
    daicc = dAICc(aicc)
    waicc = wAICc(daicc)
    
    # Store results in dataframe
    
    alphaT = betaT = concentration = bl.ratio = vector(length=length(names))
    file = rep(file, length(names))
    
    for(i in 1:length(names)){
        alphaT[i] = alphaT.calc(fits[[i]], names[[i]])
        betaT[i] = betaT.calc(fits[[i]], names[[i]])
        concentration[i] = concentration.calc(fits[[i]], names[[i]])
        bl.ratio[i] = bl.ratio.calc(fits[[i]], names[[i]])
    }
    aicc.dat = data.frame(file = file, negLogL = negLogL, df = df, dAICc = daicc, weight = waicc, TL.mean = alphaT/betaT, alphaT = alphaT, betaT = betaT,
    concentration = concentration, bl.ratio = bl.ratio, row.names = names)
    aicc.dat$weight = round(aicc.dat$weight, digits = digits)
    aicc.dat[order(aicc.dat$dAICc),]
}

### End function definitions ###

if(!interactive()){
    input = parseArgs()
    options(width=150)
}

library(ape, quietly=TRUE)
library(bbmle, quietly=TRUE)

for(i in 1:length(input$files)){
	print(paste("Reading tree for file", input$files[i]))
	tree = read.nexus(file = input$files[i])

    dat = data.frame(el = tree$edge.length)

	if(any(dat$el == 0)) {
		stop("Tree must not contain 0-length branches. Exiting...")
	}

    numEdges = length(tree$edge.length)
    numTips = length(tree$tip.label)
    tipIndex = match(1:numTips, tree$edge[,2])
    internalIndex = match((numTips+2):(numEdges+1), tree$edge[,2]) # Edge[numTips+1] is the root edge

    tip.length = tree$edge.length[tipIndex]
    internal.length = tree$edge.length[internalIndex]
    tree.length = sum(dat$el)

    fit.expon = suppressWarnings(mle2(expon.NLL, start = list(rate = 0), data = dat))
    fit.gamma = suppressWarnings(mle2(gamma.NLL, start = list(shape = 0, rate = 0), data = dat))
    fit.compDirichlet.T   = suppressWarnings(mle2(compDirichlet.NLL, start = list(beta = 0), trace=T, data = dat))
    fit.compDirichlet.Ta  = suppressWarnings(mle2(compDirichlet.NLL, start = list(beta = 0, concentration = 0), trace=T, data = dat))
    fit.compDirichlet.Tac = suppressWarnings(mle2(compDirichlet.NLL, start = list(beta = 0, concentration = 0, c = 0), trace=T, data = dat))
    fit.compDirichlet.Tc  = suppressWarnings(mle2(compDirichlet.NLL, start = list(beta = 0, c = 0), trace=T, data = dat))
    fit.compDirichlet.a   = suppressWarnings(mle2(compDirichlet.NLL, start = list(concentration = 0), trace=T, data = dat))
    fit.compDirichlet.ac  = suppressWarnings(mle2(compDirichlet.NLL, start = list(concentration = 0, c = 0), trace=T, data = dat))
    fit.compDirichlet.c   = suppressWarnings(mle2(compDirichlet.NLL, start = list(c = 0), trace=T, data = dat))


    splitfile = unlist(strsplit(input$files[i], split='/'))
    file = splitfile[length(splitfile)]
    if(i == 1) models.out = models.aicc(file = file)
    else models.out = rbind(models.out, models.aicc(file = file))
}

print(models.out)

if(!is.null(input$outfile)){
    write.table(models.out, file=input$outfile, sep='\t', quote=F)
} else print("No outfile specified.")
    

