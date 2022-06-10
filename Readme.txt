C. elegans worm movement, including locomotion and foraging, with Morphognosis learning and control.

Setup:

1. Clone or download the project.
2. Import into Eclipse.
3. Build the wormsim_jni library in the jni directory.
4. Run .sh or .bat commands in the work directory

Notes on using H2O (https://www.h2o.ai) model:
1. Learn metamorphs using wormsim driver.
2. Use worm dashboard to export head and body training csv files.
3. Train wormworx_head_model using H2O.
4. Export h2o-genmodel.jar and wormworx_head_model.java.
5. Rename h2o-genmodel.jar to h2o-genmodel-head.jar.
6. Repeat 3-5 for body model. 
7. Copy jar and java files to work directory.
8. Build.
9. Run.
10. Select metamorphH2ONN driver on worm dashboard.

References:

Boyle, Berri and Cohen, "Gait modulation in C. elegans: an integrated neuromechanical model"
http://www.frontiersin.org/Computational_Neuroscience/10.3389/fncom.2012.00010/abstract
https://github.com/OpenSourceBrain/CelegansNeuromechanicalGaitModulation.git

Eduardo J. Izquierdo and Randall D. Beer, "An Integrated Neuromechanical Model of Steering in C. elegans", ECAL15

Portegys, T., "Morphognosis: the shape of knowledge in space and time" 
http://ceur-ws.org/Vol-1964/CS2.pdf 
https://github.com/portegys/Morphognosis
