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
import io.annot8.api.settings.NoSettings;
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

public class LemmaTest {
  @Test
  public void test(){
    TestItem testItem = new TestItem();
    TestStringContent content = testItem.createContent(TestStringContent.class)
        .withData("John went to the shops.")
        .save();

    content.getAnnotations().create().withBounds(new SpanBounds(0, 23)).withType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).save();
    content.getAnnotations().create().withBounds(new SpanBounds(0, 4)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNP").save();
    content.getAnnotations().create().withBounds(new SpanBounds(5, 9)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "VBD").save();
    content.getAnnotations().create().withBounds(new SpanBounds(10, 12)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "TO").save();
    content.getAnnotations().create().withBounds(new SpanBounds(13, 16)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "DT").save();
    content.getAnnotations().create().withBounds(new SpanBounds(17, 22)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, "NNS").save();
    content.getAnnotations().create().withBounds(new SpanBounds(22, 23)).withType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).withProperty(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, ".").save();

    Lemma lemma = new Lemma();
    Processor p = lemma.createComponent(null, NoSettings.getInstance());

    ProcessorResponse response = p.process(testItem);
    assertEquals(ProcessorResponse.Status.OK, response.getStatus());

    Map<String, String> lemmas = new HashMap<>();
    lemmas.put("John", "John");
    lemmas.put("went", "go");
    lemmas.put("to", "to");
    lemmas.put("the", "the");
    lemmas.put("shops", "shop");
    lemmas.put(".", ".");

    assertEquals(6, content.getAnnotations().getByType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).count());
    assertTrue(content.getAnnotations().getByBoundsAndType(SpanBounds.class, AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).allMatch(a -> a.getProperties().has(PropertyKeys.PROPERTY_KEY_LEMMA, String.class)));
    content.getAnnotations().getByBoundsAndType(SpanBounds.class, AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN)
        .forEach(a -> {
          String l = a.getProperties().get(PropertyKeys.PROPERTY_KEY_LEMMA, String.class).get();
          assertEquals(lemmas.get(content.getText(a).get()), l);
        });
  }
}
