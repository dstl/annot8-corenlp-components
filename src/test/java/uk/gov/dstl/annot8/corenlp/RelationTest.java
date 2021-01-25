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

import io.annot8.api.annotations.Annotation;
import io.annot8.api.annotations.Group;
import io.annot8.api.components.Processor;
import io.annot8.api.components.responses.ProcessorResponse;
import io.annot8.common.data.bounds.SpanBounds;
import io.annot8.conventions.AnnotationTypes;
import io.annot8.conventions.GroupRoles;
import io.annot8.conventions.GroupTypes;
import io.annot8.conventions.PropertyKeys;
import io.annot8.testing.testimpl.TestItem;
import io.annot8.testing.testimpl.content.TestStringContent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RelationTest {
  @Test
  public void test(){
    TestItem testItem = new TestItem();
    TestStringContent content = testItem.createContent(TestStringContent.class)
        .withData("Rachel lives in London.")
        .save();

    content.getAnnotations().create().withBounds(new SpanBounds(0, 23)).withType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).save();
    content.getAnnotations().create().withBounds(new SpanBounds(0, 6)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").save();
    content.getAnnotations().create().withBounds(new SpanBounds(7, 12)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "VBZ").save();
    content.getAnnotations().create().withBounds(new SpanBounds(13, 15)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "IN").save();
    content.getAnnotations().create().withBounds(new SpanBounds(16, 22)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").save();
    content.getAnnotations().create().withBounds(new SpanBounds(22, 23)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, ".").save();

    Annotation aRachel = content.getAnnotations().create().withBounds(new SpanBounds(0, 6)).withType(AnnotationTypes.ANNOTATION_TYPE_PERSON).save();
    Annotation aLondon = content.getAnnotations().create().withBounds(new SpanBounds(16, 22)).withType(AnnotationTypes.ANNOTATION_TYPE_LOCATION).withProperty(PropertyKeys.PROPERTY_KEY_SUBTYPE, "CITY").save();

    Relation relation = new Relation();
    Processor p = relation.createComponent(null, new CoreNLPSettings());

    ProcessorResponse pr = p.process(testItem);
    assertEquals(ProcessorResponse.Status.OK, pr.getStatus());

    assertEquals(1, testItem.getGroups().getAll().count());

    Group g = testItem.getGroups().getAll().findFirst().get();

    assertEquals(GroupTypes.RELATION_PREFIX + "locationOfResidence", g.getType());

    assertEquals(1, g.getAnnotations(GroupRoles.GROUP_ROLE_SOURCE).count());
    assertEquals(1, g.getAnnotations(GroupRoles.GROUP_ROLE_TARGET).count());
    assertEquals(0, g.getAnnotations(GroupRoles.GROUP_ROLE_OBJECT).count());

    List<Annotation> sources = g.getAnnotations(GroupRoles.GROUP_ROLE_SOURCE).collect(Collectors.toList());
    assertTrue(sources.contains(aRachel));

    List<Annotation> targets = g.getAnnotations(GroupRoles.GROUP_ROLE_TARGET).collect(Collectors.toList());
    assertTrue(targets.contains(aLondon));
  }

  @Test
  public void testSibling(){
    TestItem testItem = new TestItem();
    TestStringContent content = testItem.createContent(TestStringContent.class)
        .withData("Mary's brother is David.")
        .save();

    content.getAnnotations().create().withBounds(new SpanBounds(0, 24)).withType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).save();
    content.getAnnotations().create().withBounds(new SpanBounds(0, 4)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").save();
    content.getAnnotations().create().withBounds(new SpanBounds(4, 6)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "POS").save();
    content.getAnnotations().create().withBounds(new SpanBounds(7, 14)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NN").save();
    content.getAnnotations().create().withBounds(new SpanBounds(15, 17)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "VBZ").save();
    content.getAnnotations().create().withBounds(new SpanBounds(18, 23)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").save();
    content.getAnnotations().create().withBounds(new SpanBounds(23, 24)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, ".").save();

    Annotation aMary = content.getAnnotations().create().withBounds(new SpanBounds(0, 4)).withType(AnnotationTypes.ANNOTATION_TYPE_PERSON).save();
    Annotation aDavid = content.getAnnotations().create().withBounds(new SpanBounds(18, 23)).withType(AnnotationTypes.ANNOTATION_TYPE_PERSON).save();

    Relation relation = new Relation();
    Processor p = relation.createComponent(null, new CoreNLPSettings());

    ProcessorResponse pr = p.process(testItem);
    assertEquals(ProcessorResponse.Status.OK, pr.getStatus());

    assertEquals(1, testItem.getGroups().getAll().count());

    Group g = testItem.getGroups().getAll().findFirst().get();

    assertEquals(GroupTypes.RELATION_PREFIX + "sibling", g.getType());

    assertEquals(0, g.getAnnotations(GroupRoles.GROUP_ROLE_SOURCE).count());
    assertEquals(0, g.getAnnotations(GroupRoles.GROUP_ROLE_TARGET).count());
    assertEquals(2, g.getAnnotations(GroupRoles.GROUP_ROLE_OBJECT).count());

    List<Annotation> annotations = g.getAnnotations(GroupRoles.GROUP_ROLE_OBJECT).collect(Collectors.toList());

    assertTrue(annotations.contains(aMary));
    assertTrue(annotations.contains(aDavid));
  }

  @Test
  public void testMultipleTokenMentions(){
    TestItem testItem = new TestItem();
    TestStringContent content = testItem.createContent(TestStringContent.class)
        .withData("Barack Obama lives in America.")
        .save();

    content.getAnnotations().create().withBounds(new SpanBounds(0, 30)).withType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).save();
    content.getAnnotations().create().withBounds(new SpanBounds(0, 6)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").save();
    content.getAnnotations().create().withBounds(new SpanBounds(7, 12)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").save();
    content.getAnnotations().create().withBounds(new SpanBounds(13, 18)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "VBZ").save();
    content.getAnnotations().create().withBounds(new SpanBounds(19, 21)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "IN").save();
    content.getAnnotations().create().withBounds(new SpanBounds(22, 29)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").save();
    content.getAnnotations().create().withBounds(new SpanBounds(29, 30)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, ".").save();

    Annotation aBarackObama = content.getAnnotations().create().withBounds(new SpanBounds(0, 12)).withType(AnnotationTypes.ANNOTATION_TYPE_PERSON).save();
    Annotation aAmerica = content.getAnnotations().create().withBounds(new SpanBounds(22, 29)).withType(AnnotationTypes.ANNOTATION_TYPE_LOCATION).withProperty(PropertyKeys.PROPERTY_KEY_SUBTYPE, "CITY").save();

    Relation relation = new Relation();
    Processor p = relation.createComponent(null, new CoreNLPSettings());

    ProcessorResponse pr = p.process(testItem);
    assertEquals(ProcessorResponse.Status.OK, pr.getStatus());

    assertEquals(1, testItem.getGroups().getAll().count());

    Group g = testItem.getGroups().getAll().findFirst().get();

    assertEquals(GroupTypes.RELATION_PREFIX + "locationOfResidence", g.getType());

    assertEquals(1, g.getAnnotations(GroupRoles.GROUP_ROLE_SOURCE).count());
    assertEquals(1, g.getAnnotations(GroupRoles.GROUP_ROLE_TARGET).count());
    assertEquals(0, g.getAnnotations(GroupRoles.GROUP_ROLE_OBJECT).count());

    List<Annotation> sources = g.getAnnotations(GroupRoles.GROUP_ROLE_SOURCE).collect(Collectors.toList());
    assertTrue(sources.contains(aBarackObama));

    List<Annotation> targets = g.getAnnotations(GroupRoles.GROUP_ROLE_TARGET).collect(Collectors.toList());
    assertTrue(targets.contains(aAmerica));
  }

  @Test
  public void testMultipleSentences(){
    TestItem testItem = new TestItem();
    TestStringContent content = testItem.createContent(TestStringContent.class)
        .withData("Obama lives in America. Obama was the president.")
        .save();

    content.getAnnotations().create().withBounds(new SpanBounds(0, 23)).withType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).save();
    content.getAnnotations().create().withBounds(new SpanBounds(0, 5)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").save();
    content.getAnnotations().create().withBounds(new SpanBounds(6, 11)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "VBZ").save();
    content.getAnnotations().create().withBounds(new SpanBounds(12, 14)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "IN").save();
    content.getAnnotations().create().withBounds(new SpanBounds(15, 22)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").save();
    content.getAnnotations().create().withBounds(new SpanBounds(22, 23)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, ".").save();

    content.getAnnotations().create().withBounds(new SpanBounds(24, 48)).withType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).save();
    content.getAnnotations().create().withBounds(new SpanBounds(24, 29)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").save();
    content.getAnnotations().create().withBounds(new SpanBounds(30, 33)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "VBZ").save();
    content.getAnnotations().create().withBounds(new SpanBounds(34, 37)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "DT").save();
    content.getAnnotations().create().withBounds(new SpanBounds(38, 47)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NN").save();
    content.getAnnotations().create().withBounds(new SpanBounds(47, 48)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, ".").save();

    Annotation aObama1 = content.getAnnotations().create().withBounds(new SpanBounds(0, 5)).withType(AnnotationTypes.ANNOTATION_TYPE_PERSON).save();
    Annotation aObama2 = content.getAnnotations().create().withBounds(new SpanBounds(24, 29)).withType(AnnotationTypes.ANNOTATION_TYPE_PERSON).save();
    Annotation aAmerica = content.getAnnotations().create().withBounds(new SpanBounds(15, 22)).withType(AnnotationTypes.ANNOTATION_TYPE_LOCATION).withProperty(PropertyKeys.PROPERTY_KEY_SUBTYPE, "COUNTRY").save();
    Annotation aPresident = content.getAnnotations().create().withBounds(new SpanBounds(38, 47)).withType(CoreNLPUtils.UNDEFINED_ENTITY).withProperty(PropertyKeys.PROPERTY_KEY_SUBTYPE, "TITLE").save();

    Relation relation = new Relation();
    Processor p = relation.createComponent(null, new CoreNLPSettings());

    ProcessorResponse pr = p.process(testItem);
    if(pr.hasExceptions())
      pr.getExceptions().forEach(Exception::printStackTrace);
    assertEquals(ProcessorResponse.Status.OK, pr.getStatus());

    assertEquals(2, testItem.getGroups().getAll().count());

    Group g1 = testItem.getGroups().getByType(GroupTypes.RELATION_PREFIX + "locationOfResidence").findFirst().get();
    assertTrue(g1.containsAnnotation(aObama1));
    assertEquals(GroupRoles.GROUP_ROLE_SOURCE, g1.getRole(aObama1).get());
    assertTrue(g1.containsAnnotation(aAmerica));
    assertEquals(GroupRoles.GROUP_ROLE_TARGET, g1.getRole(aAmerica).get());

    Group g2 = testItem.getGroups().getByType(GroupTypes.RELATION_PREFIX + "title").findFirst().get();
    assertTrue(g2.containsAnnotation(aObama2));
    assertEquals(GroupRoles.GROUP_ROLE_SOURCE, g2.getRole(aObama2).get());
    assertTrue(g2.containsAnnotation(aPresident));
    assertEquals(GroupRoles.GROUP_ROLE_TARGET, g2.getRole(aPresident).get());
  }
}
