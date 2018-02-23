// Wormsim interface.

package openworm.morphognosis.wormworx;

public class Wormsim
{
   static
   {
      System.loadLibrary("wormsim_jni");
   }

   // Initialize.
   public static native void init();

   // Set steering neuron synapse weights.
   public static native void setSteeringSynapseWeights(double[] weights);

   // Step simulation with salt sensor stimulus.
   public static native void step(double salt_stimulus);

   // Get neuron activations.
   public static native void getSteeringActivations(double[] activations);
   public static native void getDorsalMotorActivations(double[] activations);
   public static native void getVentralMotorActivations(double[] activations);

   // Get muscle activations.
   public static native void getDorsalMuscleActivations(double[] activations);
   public static native void getVentralMuscleActivations(double[] activations);

   // Get body structure.
   public static native void getBody(double[] body);
   public static native void getSegmentAngles(double[] angles);

   // Terminate.
   public static native void terminate();

   // Override SMB muscle amplifiers.
   public static native void overrideSMBmuscleAmplifiers(double dorsal, double ventral);
}
