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

import io.annot8.api.components.Processor;
import io.annot8.api.components.responses.ProcessorResponse;
import io.annot8.common.data.bounds.SpanBounds;
import io.annot8.conventions.AnnotationTypes;
import io.annot8.conventions.PropertyKeys;
import io.annot8.testing.testimpl.TestItem;
import io.annot8.testing.testimpl.content.TestStringContent;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class POSTest {
  @Test
  public void test(){
    TestItem testItem = new TestItem();
    TestStringContent content = testItem.createContent(TestStringContent.class)
        .withData("The quick brown fox jumps over the lazy dog.")
        .save();

    content.getAnnotations().create().withBounds(new SpanBounds(0, 44)).withType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).save();
    content.getAnnotations().create().withBounds(new SpanBounds(0, 3)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();
    content.getAnnotations().create().withBounds(new SpanBounds(4, 9)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();
    content.getAnnotations().create().withBounds(new SpanBounds(10, 15)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();
    content.getAnnotations().create().withBounds(new SpanBounds(16, 19)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();
    content.getAnnotations().create().withBounds(new SpanBounds(20, 25)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();
    content.getAnnotations().create().withBounds(new SpanBounds(26, 30)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();
    content.getAnnotations().create().withBounds(new SpanBounds(31, 34)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();
    content.getAnnotations().create().withBounds(new SpanBounds(35, 39)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();
    content.getAnnotations().create().withBounds(new SpanBounds(40, 43)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();
    content.getAnnotations().create().withBounds(new SpanBounds(43, 44)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();

    POS pos = new POS();
    Processor p = pos.createComponent(null, new CoreNLPSettings());

    ProcessorResponse response = p.process(testItem);
    assertEquals(ProcessorResponse.Status.OK, response.getStatus());

    Map<String, String> posTags = new HashMap<>();
    posTags.put("the", "DT");
    posTags.put("quick", "JJ");
    posTags.put("brown", "JJ");
    posTags.put("fox", "NN");
    posTags.put("jumps", "VBZ");
    posTags.put("over", "IN");
    posTags.put("lazy", "JJ");
    posTags.put("dog", "NN");
    posTags.put(".", ".");

    assertEquals(10, content.getAnnotations().getByType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).count());
    assertTrue(content.getAnnotations().getByBoundsAndType(SpanBounds.class, AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).allMatch(a -> a.getProperties().has(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, String.class)));
    content.getAnnotations().getByBoundsAndType(SpanBounds.class, AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN)
        .forEach(a -> {
          String posTag = a.getProperties().get(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, String.class).get();
          assertEquals(posTags.get(content.getText(a).get().toLowerCase()), posTag);
        });
  }

  @Test
  public void testTwoSentences(){
    TestItem testItem = new TestItem();
    TestStringContent content = testItem.createContent(TestStringContent.class)
        .withData("John went to Oxford. Mary went to Cambridge.")
        .save();

    content.getAnnotations().create().withBounds(new SpanBounds(0, 20)).withType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).save();
    content.getAnnotations().create().withBounds(new SpanBounds(0, 4)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();
    content.getAnnotations().create().withBounds(new SpanBounds(5, 9)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();
    content.getAnnotations().create().withBounds(new SpanBounds(10, 12)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();
    content.getAnnotations().create().withBounds(new SpanBounds(13, 19)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();
    content.getAnnotations().create().withBounds(new SpanBounds(19, 20)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();

    content.getAnnotations().create().withBounds(new SpanBounds(21, 44)).withType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).save();
    content.getAnnotations().create().withBounds(new SpanBounds(21, 25)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();
    content.getAnnotations().create().withBounds(new SpanBounds(26, 30)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();
    content.getAnnotations().create().withBounds(new SpanBounds(31, 33)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();
    content.getAnnotations().create().withBounds(new SpanBounds(34, 43)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();
    content.getAnnotations().create().withBounds(new SpanBounds(43, 44)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).save();

    POS pos = new POS();
    Processor p = pos.createComponent(null, new CoreNLPSettings());

    p.process(testItem);

    assertEquals(10, content.getAnnotations().getByType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).count());
    assertTrue(content.getAnnotations().getByBoundsAndType(SpanBounds.class, AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).allMatch(a -> a.getProperties().has(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, String.class)));
  }
}
