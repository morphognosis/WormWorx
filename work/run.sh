java -Djava.library.path=../lib -cp "../bin/wormworx.jar:./h2o-genmodel-head.jar:./h2o-genmodel-body.jar:wormworx_head_model_dir:wormworx_body_model_dir" openworm.morphognosis.wormworx.Main -display -numNeighborhoods 10 -neighborhoodInitialDimension 17 -neighborhoodDimensionStride 0 -neighborhoodDimensionMultiplier 1 -epochIntervalStride 1 -epochIntervalMultiplier 1 $*
