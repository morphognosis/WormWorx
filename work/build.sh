#!/bin/bash
javac -cp h2o-genmodel-head.jar wormworx_head_model.java -d wormworx_head_model_dir
javac -cp h2o-genmodel-body.jar wormworx_body_model.java -d wormworx_body_model_dir
javac -classpath "../lib/morphognosis.jar:../lib/weka.jar:./h2o-genmodel-head.jar:./h2o-genmodel-body.jar" -d . ../src/java/openworm/morphognosis/wormworx/*.java ../src/java/hex/genmodel/tools/WormWorxHeadPredict.java ../src/java/hex/genmodel/tools/WormWorxBodyPredict.java
jar cvfm ../bin/wormworx.jar wormworx.mf openworm hex