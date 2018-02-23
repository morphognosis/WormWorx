javac -classpath "../lib/morphognosis.jar;../lib/weka.jar" -d . ../src/java/openworm/morphognosis/wormworx/*.java
jar cvfm ../bin/wormworx.jar wormworx.mf openworm
