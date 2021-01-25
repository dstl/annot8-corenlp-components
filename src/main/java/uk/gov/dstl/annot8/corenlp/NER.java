/*
 * Crown Copyright (C) 2021 Dstl
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package uk.gov.dstl.annot8.corenlp;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.NERCombinerAnnotator;
import edu.stanford.nlp.util.CoreMap;
import io.annot8.api.capabilities.Capabilities;
import io.annot8.api.components.annotations.ComponentDescription;
import io.annot8.api.components.annotations.ComponentName;
import io.annot8.api.components.annotations.SettingsClass;
import io.annot8.api.context.Context;
import io.annot8.api.exceptions.Annot8Exception;
import io.annot8.api.settings.Description;
import io.annot8.common.components.AbstractProcessorDescriptor;
import io.annot8.common.components.capabilities.SimpleCapabilities;
import io.annot8.common.data.bounds.SpanBounds;
import io.annot8.common.data.content.Text;
import io.annot8.components.base.text.processors.AbstractTextProcessor;
import io.annot8.conventions.AnnotationTypes;
import io.annot8.conventions.PropertyKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

@ComponentName("CoreNLP NER")
@ComponentDescription("Extract entities using CoreNLP's NER annotator")
@SettingsClass(NER.Settings.class)
public class NER extends AbstractProcessorDescriptor<NER.Processor, NER.Settings> {

  private static final Logger LOGGER = LoggerFactory.getLogger(NER.class);

  @Override
  protected Processor createComponent(Context context, Settings settings) {
    try {
      return new Processor(settings.getTypeMapping(), settings.getProperties(), settings.getProbabilityThreshold());
    } catch (Annot8Exception e) {
      LOGGER.error("Unable to create processor", e);
      return null;
    }
  }

  @Override
  public Capabilities capabilities() {
    SimpleCapabilities.Builder builder = new SimpleCapabilities.Builder()
        .withProcessesAnnotations(AnnotationTypes.ANNOTATION_TYPE_SENTENCE, SpanBounds.class)
        .withProcessesAnnotations(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN, SpanBounds.class)
        .withCreatesAnnotations("entity", SpanBounds.class);

    for(String creates : new HashSet<>(getSettings().getTypeMapping().values())){
      builder = builder.withCreatesAnnotations(creates, SpanBounds.class);
    }

    return builder.build();
  }

  public static class Processor extends AbstractTextProcessor {

    private final NERCombinerAnnotator annotator;
    private final Map<String, String> typeMapping;
    private final double probThreshold;

    public Processor(Map<String, String> typeMapping, Properties properties, double probThreshold) throws Annot8Exception {
      this.typeMapping = typeMapping;
      this.probThreshold = probThreshold;

      try {
        annotator = new NERCombinerAnnotator(properties);
      }catch (IOException e){
        throw new Annot8Exception("Unable to create CoreNLP NERCombinerAnnotator", e);
      }
    }

    @Override
    protected void process(Text content) {
      Annotation document = CoreNLPUtils.createCoreNLPDocument(content);
      annotator.annotate(document);

      for(CoreMap mention : document.get(CoreAnnotations.MentionsAnnotation.class)){
        SpanBounds sb = new SpanBounds(mention.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class), mention.get(CoreAnnotations.CharacterOffsetEndAnnotation.class));

        String type = mention.get(CoreAnnotations.NamedEntityTagAnnotation.class);
        String coreNLPType = typeMapping.getOrDefault(type, "entity");

        //Assume if there are multiple probabilities then the highest is the one we're using,
        // as the Map key doesn't always match the type
        Map<String, Double> probs = mention.get(CoreAnnotations.NamedEntityTagProbsAnnotation.class);
        OptionalDouble optProb = probs.values().stream().mapToDouble(Double::doubleValue).max();
        Double prob = null;
        if(optProb.isPresent())
          prob = optProb.getAsDouble();

        if(prob != null && prob < probThreshold)
          continue;

        content.getAnnotations().create()
            .withBounds(sb)
            .withType(coreNLPType)
            .withProperty(PropertyKeys.PROPERTY_KEY_PROBABILITY, prob)
            .withProperty(PropertyKeys.PROPERTY_KEY_SUBTYPE, type)
            .save();

        //TODO: Add information from edu.stanford.nlp.time.TimeAnnotations$TimexAnnotation
      }
    }
  }


  public static class Settings extends CoreNLPSettings {

    private Map<String, String> typeMapping;
    private double probabilityThreshold = 0.0;

    public Settings(){
      typeMapping = CoreNLPUtils.CORENLP_TO_ANNOT8;
      properties = new Properties();
    }

    public Settings(Map<String, String> typeMapping, Properties properties){
      this.typeMapping = typeMapping;
      this.properties = properties;
    }

    @Description("Mapping of CoreNLP types to Annot8 types")
    public Map<String, String> getTypeMapping() {
      return typeMapping;
    }
    public void setTypeMapping(Map<String, String> typeMapping) {
      this.typeMapping = typeMapping;
    }
    public void addTypeMapping(String source, String target){
      if(typeMapping == null)
        typeMapping = new HashMap<>();

      typeMapping.put(source, target);
    }

    @Description(value = "Reject any annotations with a probability below this threshold", defaultValue = "0.0")
    public double getProbabilityThreshold() {
      return probabilityThreshold;
    }
    public void setProbabilityThreshold(double probabilityThreshold) {
      this.probabilityThreshold = probabilityThreshold;
    }

    @Override
    public boolean validate() {
      return typeMapping != null && !typeMapping.isEmpty() &&
          probabilityThreshold >= 0.0 && probabilityThreshold <= 1.0 &&
          properties != null;
    }
  }
}
