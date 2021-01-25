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

public class CoreferenceTest {
  @Test
  public void testOneSentence() {
    TestItem testItem = new TestItem();
    TestStringContent content = testItem.createContent(TestStringContent.class)
        .withData("I voted for Smith because he promised fewer cuts.")
        .save();

    content.getAnnotations().create().withBounds(new SpanBounds(0, 50)).withType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).save();
    content.getAnnotations().create().withBounds(new SpanBounds(0, 1)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "PRP").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "I").save();
    content.getAnnotations().create().withBounds(new SpanBounds(2, 7)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "VBD").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "vote").save();
    content.getAnnotations().create().withBounds(new SpanBounds(8, 11)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "IN").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "for").save();
    content.getAnnotations().create().withBounds(new SpanBounds(12, 17)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "Smith").save();
    content.getAnnotations().create().withBounds(new SpanBounds(18, 25)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "IN").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "because").save();
    content.getAnnotations().create().withBounds(new SpanBounds(26, 28)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "PRP").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "he").save();
    content.getAnnotations().create().withBounds(new SpanBounds(29, 37)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "VBD").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "promise").save();
    content.getAnnotations().create().withBounds(new SpanBounds(38, 43)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "JJR").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "fewer").save();
    content.getAnnotations().create().withBounds(new SpanBounds(45, 49)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNS").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "cut").save();
    content.getAnnotations().create().withBounds(new SpanBounds(49, 50)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, ".").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, ".").save();

    Annotation aSmith = content.getAnnotations().create().withBounds(new SpanBounds(12, 17)).withType(AnnotationTypes.ANNOTATION_TYPE_PERSON).save();
    Annotation aHe = content.getAnnotations().create().withBounds(new SpanBounds(26, 28)).withType(AnnotationTypes.ANNOTATION_TYPE_PERSON).save();

    Coreference coref = new Coreference();
    Processor p = coref.createComponent(null, new CoreNLPSettings());

    ProcessorResponse response = p.process(testItem);
    response.getExceptions().forEach(Exception::printStackTrace);
    assertEquals(ProcessorResponse.Status.OK, response.getStatus());

    assertEquals(1, testItem.getGroups().getAll().count());
    Group g = testItem.getGroups().getAll().findFirst().get();

    assertEquals(GroupTypes.GROUP_TYPE_GRAMMAR_COREFERENCE, g.getType());
    assertEquals(2, g.getAnnotationsForContent(content).count());

    List<Annotation> annotations = g.getAnnotations(GroupRoles.GROUP_ROLE_MENTION).collect(Collectors.toList());
    assertEquals(2, annotations.size());
    assertTrue(annotations.contains(aSmith));
    assertTrue(annotations.contains(aHe));
  }

  @Test
  public void testCrossSentence() {
    TestItem testItem = new TestItem();
    TestStringContent content = testItem.createContent(TestStringContent.class)
        .withData("Barack Obama lives in America. Obama was the president.")
        .save();

    content.getAnnotations().create().withBounds(new SpanBounds(0, 30)).withType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).save();
    content.getAnnotations().create().withBounds(new SpanBounds(0, 6)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "Barack").save();
    content.getAnnotations().create().withBounds(new SpanBounds(7, 12)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "Obama").save();
    content.getAnnotations().create().withBounds(new SpanBounds(13, 18)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "VBZ").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "live").save();
    content.getAnnotations().create().withBounds(new SpanBounds(19, 21)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "IN").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "in").save();
    content.getAnnotations().create().withBounds(new SpanBounds(22, 29)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "America").save();
    content.getAnnotations().create().withBounds(new SpanBounds(29, 30)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, ".").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, ".").save();

    content.getAnnotations().create().withBounds(new SpanBounds(31, 55)).withType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).save();
    content.getAnnotations().create().withBounds(new SpanBounds(31, 36)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "Obama").save();
    content.getAnnotations().create().withBounds(new SpanBounds(37, 40)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "VBZ").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "be").save();
    content.getAnnotations().create().withBounds(new SpanBounds(41, 44)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "DT").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "the").save();
    content.getAnnotations().create().withBounds(new SpanBounds(45, 54)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NN").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "president").save();
    content.getAnnotations().create().withBounds(new SpanBounds(54, 55)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, ".").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, ".").save();

    Annotation aObama1 = content.getAnnotations().create().withBounds(new SpanBounds(0, 12)).withType(AnnotationTypes.ANNOTATION_TYPE_PERSON).save();
    Annotation aObama2 = content.getAnnotations().create().withBounds(new SpanBounds(31, 36)).withType(AnnotationTypes.ANNOTATION_TYPE_PERSON).save();
    Annotation aAmerica = content.getAnnotations().create().withBounds(new SpanBounds(22, 27)).withType(AnnotationTypes.ANNOTATION_TYPE_LOCATION).withProperty(PropertyKeys.PROPERTY_KEY_SUBTYPE, "COUNTRY").save();
    Annotation aPresident = content.getAnnotations().create().withBounds(new SpanBounds(45, 54)).withType(CoreNLPUtils.UNDEFINED_ENTITY).withProperty(PropertyKeys.PROPERTY_KEY_SUBTYPE, "TITLE").save();

    Coreference coref = new Coreference();
    Processor p = coref.createComponent(null, new CoreNLPSettings());

    ProcessorResponse response = p.process(testItem);
    response.getExceptions().forEach(Exception::printStackTrace);
    assertEquals(ProcessorResponse.Status.OK, response.getStatus());

    assertEquals(1, testItem.getGroups().getAll().count());
    Group g = testItem.getGroups().getAll().findFirst().get();

    assertEquals(GroupTypes.GROUP_TYPE_GRAMMAR_COREFERENCE, g.getType());
    assertEquals(2, g.getAnnotationsForContent(content).count());

    List<Annotation> annotations = g.getAnnotations(GroupRoles.GROUP_ROLE_MENTION).collect(Collectors.toList());
    assertEquals(2, annotations.size());
    assertTrue(annotations.contains(aObama1));
    assertTrue(annotations.contains(aObama2));
  }

  @Test
  public void testTwoGroups() {
    TestItem testItem = new TestItem();
    TestStringContent content = testItem.createContent(TestStringContent.class)
        .withData("Alice and Bob are married, but she likes cats and he likes dogs.")
        .save();

    content.getAnnotations().create().withBounds(new SpanBounds(0, 64)).withType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).save();
    content.getAnnotations().create().withBounds(new SpanBounds(0, 5)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "Alice").save();
    content.getAnnotations().create().withBounds(new SpanBounds(6, 9)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "CC").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "and").save();
    content.getAnnotations().create().withBounds(new SpanBounds(10, 13)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "Bob").save();
    content.getAnnotations().create().withBounds(new SpanBounds(14, 17)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "VBP").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "be").save();
    content.getAnnotations().create().withBounds(new SpanBounds(18, 25)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "VBN").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "marry").save();
    content.getAnnotations().create().withBounds(new SpanBounds(25, 26)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, ",").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, ",").save();
    content.getAnnotations().create().withBounds(new SpanBounds(27, 30)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "CC").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "but").save();
    content.getAnnotations().create().withBounds(new SpanBounds(31, 34)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "PRP").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "she").save();
    content.getAnnotations().create().withBounds(new SpanBounds(35, 40)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "VBZ").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "like").save();
    content.getAnnotations().create().withBounds(new SpanBounds(41, 45)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNS").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "cat").save();
    content.getAnnotations().create().withBounds(new SpanBounds(46, 49)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "CC").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "and").save();
    content.getAnnotations().create().withBounds(new SpanBounds(50, 52)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "PRP").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "he").save();
    content.getAnnotations().create().withBounds(new SpanBounds(53, 58)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "VBZ").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "like").save();
    content.getAnnotations().create().withBounds(new SpanBounds(59, 63)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNS").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, "dog").save();
    content.getAnnotations().create().withBounds(new SpanBounds(63, 64)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, ".").withProperty(PropertyKeys.PROPERTY_KEY_LEMMA, ".").save();

    Annotation aAlice = content.getAnnotations().create().withBounds(new SpanBounds(0, 5)).withType(AnnotationTypes.ANNOTATION_TYPE_PERSON).save();
    Annotation aBob = content.getAnnotations().create().withBounds(new SpanBounds(10, 13)).withType(AnnotationTypes.ANNOTATION_TYPE_PERSON).save();
    Annotation aShe = content.getAnnotations().create().withBounds(new SpanBounds(31, 34)).withType(AnnotationTypes.ANNOTATION_TYPE_PERSON).save();
    Annotation aHe = content.getAnnotations().create().withBounds(new SpanBounds(50, 52)).withType(AnnotationTypes.ANNOTATION_TYPE_PERSON).save();

    Coreference coref = new Coreference();
    Processor p = coref.createComponent(null, new CoreNLPSettings());

    ProcessorResponse response = p.process(testItem);
    response.getExceptions().forEach(Exception::printStackTrace);
    assertEquals(ProcessorResponse.Status.OK, response.getStatus());

    assertEquals(2, testItem.getGroups().getAll().count());
    assertTrue(testItem.getGroups().getAll().allMatch(g -> GroupTypes.GROUP_TYPE_GRAMMAR_COREFERENCE.equals(g.getType())));
    assertTrue(testItem.getGroups().getAll().allMatch(g -> g.getAnnotations(GroupRoles.GROUP_ROLE_MENTION).count() == 2));

    testItem.getGroups().getAll().forEach(g -> {
      List<Annotation> annotations = g.getAnnotations(GroupRoles.GROUP_ROLE_MENTION).collect(Collectors.toList());

      assertTrue((annotations.contains(aAlice) && annotations.contains(aShe)) || (annotations.contains(aBob) && annotations.contains(aHe)));
    });
  }
}
