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
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.POSTaggerAnnotator;
import io.annot8.api.capabilities.Capabilities;
import io.annot8.api.components.annotations.ComponentDescription;
import io.annot8.api.components.annotations.ComponentName;
import io.annot8.api.components.annotations.SettingsClass;
import io.annot8.api.context.Context;
import io.annot8.common.components.AbstractProcessorDescriptor;
import io.annot8.common.components.capabilities.SimpleCapabilities;
import io.annot8.common.data.bounds.SpanBounds;
import io.annot8.common.data.content.Text;
import io.annot8.components.base.text.processors.AbstractTextProcessor;
import io.annot8.conventions.AnnotationTypes;
import io.annot8.conventions.PropertyKeys;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@ComponentName("CoreNLP POS")
@ComponentDescription("Add Parts of Speech information to tokens")
@SettingsClass(CoreNLPSettings.class)
public class POS extends AbstractProcessorDescriptor<POS.Processor, CoreNLPSettings> {

  @Override
  protected Processor createComponent(Context context, CoreNLPSettings settings) {
    return new Processor(settings.getProperties());
  }

  @Override
  public Capabilities capabilities() {
    return new SimpleCapabilities.Builder()
        .withProcessesAnnotations(AnnotationTypes.ANNOTATION_TYPE_SENTENCE, SpanBounds.class)
        .withProcessesAnnotations(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN, SpanBounds.class)
        .build();
  }

  public static class Processor extends AbstractTextProcessor {

    private final POSTaggerAnnotator tagger;

    public Processor(Properties properties){
      tagger = new POSTaggerAnnotator(POSTaggerAnnotator.STANFORD_POS, properties);
    }

    @Override
    protected void process(Text content) {
      Annotation document = CoreNLPUtils.createCoreNLPDocument(content);
      tagger.annotate(document);

      Map<Integer, io.annot8.api.annotations.Annotation> annotPos = content.getAnnotations()
          .getByBoundsAndType(SpanBounds.class, AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN)
          .collect(Collectors.toMap(a -> a.getBounds(SpanBounds.class).get().getBegin(), a -> a));

      Map<Integer, CoreLabel> tokenPos = document.get(CoreAnnotations.TokensAnnotation.class).stream()
          .collect(Collectors.toMap(l -> l.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class), l -> l));

      for(int i : tokenPos.keySet()){
        if(!annotPos.containsKey(i)) {
          log().warn("Can't find original annotation from CoreNLP Token");
          continue;
        }

        content.getAnnotations().create().from(annotPos.get(i))
            .withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, tokenPos.get(i).get(CoreAnnotations.PartOfSpeechAnnotation.class))
            .save();
      }

    }
  }
}
