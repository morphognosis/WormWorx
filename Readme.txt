C. elegans worm movement, including locomotion and foraging, with Morphognosis learning and control.

Setup:

1. Clone or download the project.
2. Import into Eclipse.
3. Build the wormsim_jni library in the jni directory.
4. Run .sh or .bat commands in the work directory

Notes on using H2O (https://www.h2o.ai) model:
1. Use worm dashboard to export training csv file after creating metamorphs to learn.
2. Train wormworx_model using H2O and export h2o-genmodel.jar.
3. Copy h2o-genmodel.jar to lib directory.
4. Copy wormworx_model.java to work directory.
5. Build.
6. Select metamorphH2ONN as driver on worm dashboard.
7. Run.

References:

Boyle, Berri and Cohen, "Gait modulation in C. elegans: an integrated neuromechanical model"
http://www.frontiersin.org/Computational_Neuroscience/10.3389/fncom.2012.00010/abstract
https://github.com/OpenSourceBrain/CelegansNeuromechanicalGaitModulation.git

Eduardo J. Izquierdo and Randall D. Beer, "An Integrated Neuromechanical Model of Steering in C. elegans", ECAL15

Portegys, T., "Morphognosis: the shape of knowledge in space and time" 
http://ceur-ws.org/Vol-1964/CS2.pdf 
https://github.com/portegys/Morphognosis
