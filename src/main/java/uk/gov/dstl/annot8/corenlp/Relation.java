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
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.KBPAnnotator;
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

import java.util.*;
import java.util.stream.Collectors;

@ComponentName("CoreNLP KBP Relation")
@ComponentDescription("Extract relations between existing annotations using CoreNLP's KBP Relation annotator (kbp)")
@SettingsClass(CoreNLPSettings.class)
public class Relation extends AbstractProcessorDescriptor<Relation.Processor, CoreNLPSettings> {

  private static final Map<String, String> KBP_TO_GROUPS = new HashMap<>();
  private static final Set<String> OMNI_DIRECTIONAL_RELATIONS = new HashSet<>();

  static {
    // https://tac.nist.gov/2015/KBP/ColdStart/guidelines/TAC_KBP_2015_Slot_Descriptions_V1.0.pdf

    KBP_TO_GROUPS.put("org:alternate_names", GroupTypes.RELATION_PREFIX + "alsoKnownAs");
    KBP_TO_GROUPS.put("org:city_of_headquarters", GroupTypes.RELATION_PREFIX + "locationOfHeadquarters");
    KBP_TO_GROUPS.put("org:country_of_headquarters", GroupTypes.RELATION_PREFIX + "locationOfHeadquarters");
    KBP_TO_GROUPS.put("org:date_dissolved", GroupTypes.RELATION_PREFIX + "dateDissolved");
    KBP_TO_GROUPS.put("org:date_founded", GroupTypes.RELATION_PREFIX + "dateFounded");
    KBP_TO_GROUPS.put("org:founded_by", GroupTypes.RELATION_PREFIX + "foundedBy");
    KBP_TO_GROUPS.put("org:members", GroupTypes.RELATION_PREFIX + "members");
    KBP_TO_GROUPS.put("org:member_of", GroupTypes.RELATION_PREFIX + "memberOf");
    KBP_TO_GROUPS.put("org:number_of_employees_members", GroupTypes.RELATION_PREFIX + "numberOfMembers");
    KBP_TO_GROUPS.put("org:parents", GroupTypes.RELATION_PREFIX + "parent");
    KBP_TO_GROUPS.put("org:political_religious_affiliation", GroupTypes.RELATION_PREFIX + "affiliation");
    KBP_TO_GROUPS.put("org:shareholders", GroupTypes.RELATION_PREFIX + "shareholders");
    KBP_TO_GROUPS.put("org:stateorprovince_of_headquarters", GroupTypes.RELATION_PREFIX + "locationOfHeadquarters");
    KBP_TO_GROUPS.put("org:subsidiaries", GroupTypes.RELATION_PREFIX + "subsidiaries");
    KBP_TO_GROUPS.put("org:top_members_employees", GroupTypes.RELATION_PREFIX + "leaderOf");
    KBP_TO_GROUPS.put("org:website", GroupTypes.RELATION_PREFIX + "website");

    KBP_TO_GROUPS.put("per:age", GroupTypes.RELATION_PREFIX + "age");
    KBP_TO_GROUPS.put("per:alternate_names", GroupTypes.RELATION_PREFIX + "alsoKnownAs");
    KBP_TO_GROUPS.put("per:cause_of_death", GroupTypes.RELATION_PREFIX + "causeOfDeath");
    KBP_TO_GROUPS.put("per:charges", GroupTypes.RELATION_PREFIX + "criminalCharges");
    KBP_TO_GROUPS.put("per:children", GroupTypes.RELATION_PREFIX + "child");
    KBP_TO_GROUPS.put("per:cities_of_residence", GroupTypes.RELATION_PREFIX + "locationOfResidence");
    KBP_TO_GROUPS.put("per:city_of_birth", GroupTypes.RELATION_PREFIX + "locationOfBirth");
    KBP_TO_GROUPS.put("per:city_of_death", GroupTypes.RELATION_PREFIX + "locationOfDeath");
    KBP_TO_GROUPS.put("per:countries_of_residence", GroupTypes.RELATION_PREFIX + "locationOfResidence");
    KBP_TO_GROUPS.put("per:country_of_birth", GroupTypes.RELATION_PREFIX + "locationOfBirth");
    KBP_TO_GROUPS.put("per:country_of_death", GroupTypes.RELATION_PREFIX + "locationOfDeath");
    KBP_TO_GROUPS.put("per:date_of_birth", GroupTypes.RELATION_PREFIX + "dateOfBirth");
    KBP_TO_GROUPS.put("per:date_of_death", GroupTypes.RELATION_PREFIX + "dateOfDeath");
    KBP_TO_GROUPS.put("per:employee_or_member_of", GroupTypes.RELATION_PREFIX + "memberOf");
    KBP_TO_GROUPS.put("per:origin", GroupTypes.RELATION_PREFIX + "nationality");
    KBP_TO_GROUPS.put("per:other_family", GroupTypes.RELATION_PREFIX + "family");
    KBP_TO_GROUPS.put("per:parents", GroupTypes.RELATION_PREFIX + "parent");
    KBP_TO_GROUPS.put("per:religion", GroupTypes.RELATION_PREFIX + "religion");
    KBP_TO_GROUPS.put("per:schools_attended", GroupTypes.RELATION_PREFIX + "schoolAttended");
    KBP_TO_GROUPS.put("per:siblings", GroupTypes.RELATION_PREFIX + "sibling");
    KBP_TO_GROUPS.put("per:spouse", GroupTypes.RELATION_PREFIX + "spouse");
    KBP_TO_GROUPS.put("per:stateorprovince_of_birth", GroupTypes.RELATION_PREFIX + "locationOfBirth");
    KBP_TO_GROUPS.put("per:stateorprovince_of_death", GroupTypes.RELATION_PREFIX + "locationOfDeath");
    KBP_TO_GROUPS.put("per:statesorprovinces_of_residence", GroupTypes.RELATION_PREFIX + "locationOfResidence");
    KBP_TO_GROUPS.put("per:title", GroupTypes.RELATION_PREFIX + "title");

    OMNI_DIRECTIONAL_RELATIONS.add(GroupTypes.RELATION_PREFIX + "alsoKnownAs");
    OMNI_DIRECTIONAL_RELATIONS.add(GroupTypes.RELATION_PREFIX + "sibling");
    OMNI_DIRECTIONAL_RELATIONS.add(GroupTypes.RELATION_PREFIX + "spouse");
  }

  @Override
  protected Processor createComponent(Context context, CoreNLPSettings settings) {
    return new Processor(settings.getProperties());
  }

  @Override
  public Capabilities capabilities() {
    SimpleCapabilities.Builder builder = new SimpleCapabilities.Builder()
        .withProcessesAnnotations(AnnotationTypes.ANNOTATION_TYPE_SENTENCE, SpanBounds.class)
        .withProcessesAnnotations(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN, SpanBounds.class);

    CoreNLPUtils.ANNOT8_TO_CORENLP.keySet().forEach(a -> builder.withProcessesAnnotations(a, SpanBounds.class));
    builder.withProcessesAnnotations(CoreNLPUtils.UNDEFINED_ENTITY, SpanBounds.class);

    Set<String> groups = new HashSet<>(KBP_TO_GROUPS.values());
    groups.forEach(builder::withCreatesGroups);

    return builder.build();
  }

  public static class Processor extends AbstractTextProcessor {

    private final KBPAnnotator kbp;

    public Processor(Properties properties){
      kbp = new KBPAnnotator(properties);
    }

    @Override
    protected void process(Text content) {
      Annotation document = CoreNLPUtils.createCoreNLPDocument(content);
      kbp.annotate(document);

      for(CoreMap sentence: document.get(CoreAnnotations.SentencesAnnotation.class)) {
        Collection<RelationTriple> relations = sentence.get(CoreAnnotations.KBPTriplesAnnotation.class);
        if(relations == null)
          continue;

        Set<String> omniDirectionalRelations = new HashSet<>();
        for (RelationTriple relation : relations) {
          String relationType = KBP_TO_GROUPS.getOrDefault(relation.relationHead().word(), GroupTypes.RELATION_PREFIX + relation.relationHead().word());
          boolean omniDirectional = OMNI_DIRECTIONAL_RELATIONS.contains(relationType);

          if(omniDirectional){
            Set<String> tokenPos = new TreeSet<>();
            relation.subject.forEach(r -> tokenPos.add(r.beginPosition() + "-" + r.endPosition()));
            relation.object.forEach(r -> tokenPos.add(r.beginPosition() + "-" + r.endPosition()));

            if(!omniDirectionalRelations.add(relation.relationHead() + ":" + String.join("/", tokenPos))){
              continue;
            }
          }

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
              .forEach(a -> builder.withAnnotation(omniDirectional ? GroupRoles.GROUP_ROLE_OBJECT : GroupRoles.GROUP_ROLE_SOURCE, a));

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
            .forEach(a -> builder.withAnnotation(omniDirectional ? GroupRoles.GROUP_ROLE_OBJECT : GroupRoles.GROUP_ROLE_TARGET, a));

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
  }
}
