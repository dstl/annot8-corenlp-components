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

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotator;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.DependencyParseAnnotator;
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
import io.annot8.conventions.PropertyKeys;
import org.apache.commons.lang3.text.WordUtils;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@ComponentName("CoreNLP OpenIE Relation")
@ComponentDescription("Extract relations between existing annotations using CoreNLP's Dependency Parser (depparse), Natural Logic annotator (natlog), and OpenIE Relation annotator (openie)")
@SettingsClass(CoreNLPSettings.class)
public class OpenIE extends AbstractProcessorDescriptor<OpenIE.Processor, CoreNLPSettings> {

  @Override
  protected Processor createComponent(Context context, CoreNLPSettings settings) {
    return new Processor(settings.getProperties());
  }

  @Override
  public Capabilities capabilities() {
    SimpleCapabilities.Builder builder = new SimpleCapabilities.Builder()
        .withProcessesAnnotations(AnnotationTypes.ANNOTATION_TYPE_SENTENCE, SpanBounds.class)
        .withProcessesAnnotations(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN, SpanBounds.class)
        .withCreatesGroups("*");

    CoreNLPUtils.ANNOT8_TO_CORENLP.keySet().forEach(a -> builder.withProcessesAnnotations(a, SpanBounds.class));
    builder.withProcessesAnnotations(CoreNLPUtils.UNDEFINED_ENTITY, SpanBounds.class);

    return builder.build();
  }

  public static class Processor extends AbstractTextProcessor {

    private final DependencyParseAnnotator parserAnnotator;
    private final NaturalLogicAnnotator logicAnnotator;
    private final edu.stanford.nlp.naturalli.OpenIE openIE;

    public Processor(Properties properties){
      parserAnnotator = new DependencyParseAnnotator(properties);
      logicAnnotator = new NaturalLogicAnnotator(properties);
      openIE = new edu.stanford.nlp.naturalli.OpenIE(properties);
    }

    @Override
    protected void process(Text content) {
      Annotation document = CoreNLPUtils.createCoreNLPDocument(content);
      parserAnnotator.annotate(document);
      logicAnnotator.annotate(document);
      openIE.annotate(document);

      for(CoreMap sentence: document.get(CoreAnnotations.SentencesAnnotation.class)) {
        Collection<RelationTriple> relations = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
        if(relations == null)
          continue;

        for (RelationTriple relation : relations) {
          String relationType = GroupTypes.RELATION_PREFIX + normalizeRelation(relation.relationLemmaGloss());

          Group.Builder builder = content.getItem().getGroups().create()
              .withType(relationType)
              .withProperty(PropertyKeys.PROPERTY_KEY_PROBABILITY, relation.confidence);

          //Subjects
          if(relation.subject.isEmpty())
            continue;

          List<String> subjectTypes = relation.subject.stream().map(cl -> CoreNLPUtils.CORENLP_TO_ANNOT8.getOrDefault(cl.ner(), "_")).distinct().collect(Collectors.toList());
          if(subjectTypes.size() > 1)
            continue; // If it's greater than one, then it exceeds annotation bounds and/or spans multiple annotations

          String subjectType = subjectTypes.get(0);
          if("_".equals(subjectType))
            continue; // We'd need to create a new entity

          content.getAnnotations().getByBoundsAndType(SpanBounds.class, subjectType)
              .filter(a -> a.getBounds(SpanBounds.class).get().equals(getBounds(relation.subject)))
              .forEach(a -> builder.withAnnotation(GroupRoles.GROUP_ROLE_SOURCE, a));

          //Objects
          if(relation.object.isEmpty())
            continue;

          List<String> objectTypes = relation.object.stream().map(cl -> CoreNLPUtils.CORENLP_TO_ANNOT8.getOrDefault(cl.ner(), "_")).distinct().collect(Collectors.toList());
          if(objectTypes.size() > 1)
            continue; // If it's greater than one, then it exceeds annotation bounds and/or spans multiple annotations

          String objectType = objectTypes.get(0);
          if("_".equals(objectType))
            continue; // We'd need to create a new entity

          content.getAnnotations().getByBoundsAndType(SpanBounds.class, objectType)
              .filter(a -> a.getBounds(SpanBounds.class).get().equals(getBounds(relation.object)))
              .forEach(a -> builder.withAnnotation(GroupRoles.GROUP_ROLE_TARGET, a));

          builder.save();
        }
      }
    }

    private static SpanBounds getBounds(Collection<CoreLabel> coreLabels){
      int min = Integer.MAX_VALUE;
      int max = Integer.MIN_VALUE;

      for(CoreLabel label : coreLabels){
        if(label.beginPosition() < min)
          min = label.beginPosition();

        if(label.endPosition() > max)
          max = label.endPosition();
      }

      return new SpanBounds(min, max);
    }

    private static String normalizeRelation(String s){
      String fully = WordUtils.capitalizeFully(s, ' ').replaceAll(" ", "");
      if(fully.length() <= 1)
        return fully.toLowerCase();

      return fully.substring(0, 1).toLowerCase() + fully.substring(1);
    }
  }
}
