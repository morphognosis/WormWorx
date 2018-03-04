javac -cp ../lib/h2o-genmodel.jar wormworx_model.java -d wormworx_model_dir
javac -classpath "../lib/morphognosis.jar;../lib/weka.jar;../lib/h2o-genmodel.jar" -d . ../src/java/openworm/morphognosis/wormworx/*.java ../src/java/hex/genmodel/tools/WormWorxPredict.java
jar cvfm ../bin/wormworx.jar wormworx.mf openworm hex
