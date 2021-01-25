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

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CorefAnnotator;
import edu.stanford.nlp.pipeline.ParserAnnotator;
import edu.stanford.nlp.util.CoreMap;
import io.annot8.api.annotations.Group;
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
import io.annot8.conventions.GroupRoles;
import io.annot8.conventions.GroupTypes;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

@ComponentName("CoreNLP Coreference")
@ComponentDescription("Coreference entities using CoreNLP's Parser (parse) and Coreference (coref)")
@SettingsClass(CoreNLPSettings.class)
public class Coreference extends AbstractProcessorDescriptor<Coreference.Processor, CoreNLPSettings> {

  @Override
  protected Processor createComponent(Context context, CoreNLPSettings settings) {
    return new Processor(settings.getProperties());
  }

  @Override
  public Capabilities capabilities() {
    SimpleCapabilities.Builder builder = new SimpleCapabilities.Builder()
        .withProcessesAnnotations(AnnotationTypes.ANNOTATION_TYPE_SENTENCE, SpanBounds.class)
        .withProcessesAnnotations(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN, SpanBounds.class)
        .withCreatesGroups(GroupTypes.GROUP_TYPE_GRAMMAR_COREFERENCE);

    CoreNLPUtils.ANNOT8_TO_CORENLP.keySet().forEach(a -> builder.withProcessesAnnotations(a, SpanBounds.class));
    builder.withProcessesAnnotations(CoreNLPUtils.UNDEFINED_ENTITY, SpanBounds.class);

    return builder.build();
  }

  public static class Processor extends AbstractTextProcessor {

    private final ParserAnnotator parser;
    private final CorefAnnotator coref;

    public Processor(Properties properties){
      parser = new ParserAnnotator(ParserAnnotator.STANFORD_PARSE, properties);
      coref = new CorefAnnotator(properties);
    }

    @Override
    protected void process(Text content) {
      Annotation document = CoreNLPUtils.createCoreNLPDocument(content);
      parser.annotate(document);
      coref.annotate(document);

      List<CoreLabel> tokens = document.get(CoreAnnotations.TokensAnnotation.class);
      List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

      for (CorefChain cc : document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
        Group.Builder builder = content.getItem().getGroups().create()
            .withType(GroupTypes.GROUP_TYPE_GRAMMAR_COREFERENCE);

        AtomicInteger count = new AtomicInteger(0);
        cc.getMentionsInTextualOrder().forEach(cm -> {
          int sentenceOffset = sentences.get(cm.sentNum - 1).get(CoreAnnotations.TokenBeginAnnotation.class);

          SpanBounds sbMention = new SpanBounds(tokens.get(sentenceOffset + cm.startIndex - 1).beginPosition(), tokens.get(sentenceOffset + cm.endIndex - 2).endPosition());
          String type = CoreNLPUtils.CORENLP_TO_ANNOT8.getOrDefault(tokens.get(cm.headIndex - 1).ner(), "_");

          content.getAnnotations().getByBoundsAndType(SpanBounds.class, type)
              .filter(a -> a.getBounds(SpanBounds.class).get().isSame(sbMention))
              .forEach(a -> {
                builder.withAnnotation(GroupRoles.GROUP_ROLE_MENTION, a);
                count.incrementAndGet();
              });
        });

        if(count.get() < 2)
          continue;

        builder.save();
      }
    }
  }
}