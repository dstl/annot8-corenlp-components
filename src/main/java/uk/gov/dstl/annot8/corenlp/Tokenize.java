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

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
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

import java.util.Properties;

@ComponentName("CoreNLP Tokenize")
@ComponentDescription("Tokenize the document into sentences and word tokens, using tokenize and ssplit")
@SettingsClass(CoreNLPSettings.class)
public class Tokenize extends AbstractProcessorDescriptor<Tokenize.Processor, CoreNLPSettings> {

  @Override
  protected Processor createComponent(Context context, CoreNLPSettings settings) {
    return new Processor(settings.getProperties());
  }

  @Override
  public Capabilities capabilities() {
    return new SimpleCapabilities.Builder()
        .withCreatesAnnotations(AnnotationTypes.ANNOTATION_TYPE_SENTENCE, SpanBounds.class)
        .withCreatesAnnotations(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN, SpanBounds.class)
        .build();
  }

  public static class Processor extends AbstractTextProcessor {

    private final StanfordCoreNLP pipeline;

    public Processor(Properties properties){
      //Explicitly set the annotators property
      properties.put("annotators", "tokenize,ssplit");

      pipeline = new StanfordCoreNLP(properties);
    }

    @Override
    protected void process(Text content) {
      CoreDocument document = new CoreDocument(content.getData());
      pipeline.annotate(document);

      document.sentences().forEach(sentence -> {
        content.getAnnotations().create()
            .withType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE)
            .withBounds(new SpanBounds(sentence.charOffsets().first(), sentence.charOffsets().second()))
            .save();

        sentence.tokens().forEach(token -> content.getAnnotations().create()
            .withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN)
            .withBounds(new SpanBounds(token.beginPosition(), token.endPosition()))
            .save());
      });
    }
  }
}
