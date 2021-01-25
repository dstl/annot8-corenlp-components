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
import io.annot8.api.components.Processor;
import io.annot8.api.components.responses.ProcessorResponse;
import io.annot8.common.data.bounds.SpanBounds;
import io.annot8.conventions.AnnotationTypes;
import io.annot8.conventions.PropertyKeys;
import io.annot8.testing.testimpl.TestItem;
import io.annot8.testing.testimpl.content.TestStringContent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NERTest {
  @Test
  public void test(){
    TestItem testItem = new TestItem();
    TestStringContent content = testItem.createContent(TestStringContent.class)
        .withData("Martin visited Peter, Joe and Louise in London last week.")
        .save();

    content.getAnnotations().create().withBounds(new SpanBounds(0, 57)).withType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).save();
    content.getAnnotations().create().withBounds(new SpanBounds(0, 6)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").save();
    content.getAnnotations().create().withBounds(new SpanBounds(7, 14)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "VBD").save();
    content.getAnnotations().create().withBounds(new SpanBounds(15, 20)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").save();
    content.getAnnotations().create().withBounds(new SpanBounds(20, 21)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, ".").save();
    content.getAnnotations().create().withBounds(new SpanBounds(22, 25)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").save();
    content.getAnnotations().create().withBounds(new SpanBounds(26, 29)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "CC").save();
    content.getAnnotations().create().withBounds(new SpanBounds(30, 36)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").save();
    content.getAnnotations().create().withBounds(new SpanBounds(37, 39)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "IN").save();
    content.getAnnotations().create().withBounds(new SpanBounds(40, 46)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").save();
    content.getAnnotations().create().withBounds(new SpanBounds(47, 51)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "JJ").save();
    content.getAnnotations().create().withBounds(new SpanBounds(52, 56)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NN").save();
    content.getAnnotations().create().withBounds(new SpanBounds(56, 57)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, ".").save();

    NER ner = new NER();
    Processor p = ner.createComponent(null, new NER.Settings());

    ProcessorResponse pr = p.process(testItem);
    assertEquals(ProcessorResponse.Status.OK, pr.getStatus());

    assertEquals(4, content.getAnnotations().getByBoundsAndType(SpanBounds.class, AnnotationTypes.ANNOTATION_TYPE_PERSON).count());
    assertEquals(1, content.getAnnotations().getByBoundsAndType(SpanBounds.class, AnnotationTypes.ANNOTATION_TYPE_LOCATION).count());
    assertEquals(1, content.getAnnotations().getByBoundsAndType(SpanBounds.class, AnnotationTypes.ANNOTATION_TYPE_TEMPORAL).count());

    Annotation l = content.getAnnotations().getByBoundsAndType(SpanBounds.class, AnnotationTypes.ANNOTATION_TYPE_LOCATION).findFirst().get();
    assertEquals("London", content.getText(l).get());
    assertEquals("CITY", l.getProperties().get(PropertyKeys.PROPERTY_KEY_SUBTYPE).get());
    assertTrue(l.getProperties().has(PropertyKeys.PROPERTY_KEY_PROBABILITY));
    assertEquals(Double.class, l.getProperties().get(PropertyKeys.PROPERTY_KEY_PROBABILITY).get().getClass());
  }

  @Test
  public void testNoEntities(){
    TestItem testItem = new TestItem();
    TestStringContent content = testItem.createContent(TestStringContent.class)
        .withData("What is this?")
        .save();

    content.getAnnotations().create().withBounds(new SpanBounds(0, 13)).withType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).save();
    content.getAnnotations().create().withBounds(new SpanBounds(0, 4)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "WP").save();
    content.getAnnotations().create().withBounds(new SpanBounds(5, 7)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "VBZ").save();
    content.getAnnotations().create().withBounds(new SpanBounds(8, 12)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "DT").save();
    content.getAnnotations().create().withBounds(new SpanBounds(12, 13)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, ".").save();

    NER ner = new NER();
    Processor p = ner.createComponent(null, new NER.Settings());

    ProcessorResponse pr = p.process(testItem);
    assertEquals(ProcessorResponse.Status.OK, pr.getStatus());

    assertEquals(5, content.getAnnotations().getByBounds(SpanBounds.class).count());
    assertEquals(1, content.getAnnotations().getByBoundsAndType(SpanBounds.class, AnnotationTypes.ANNOTATION_TYPE_SENTENCE).count());
    assertEquals(4, content.getAnnotations().getByBoundsAndType(SpanBounds.class, AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).count());
  }
}
