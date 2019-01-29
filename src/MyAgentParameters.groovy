import org.arl.unet.Parameter

@com.google.gson.annotations.JsonAdapter(org.arl.unet.JsonTypeAdapter.class)
enum MyAgentParameters implements Parameter {
  dummyValue
}