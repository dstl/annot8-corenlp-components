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
import io.annot8.conventions.AnnotationTypes;
import io.annot8.testing.testimpl.TestItem;
import io.annot8.testing.testimpl.content.TestStringContent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TokenizeTest {
  @Test
  public void test(){
    TestItem testItem = new TestItem();
    TestStringContent content = testItem.createContent(TestStringContent.class)
        .withData("I had biscuits for breakfast, and cookies mid-morning. By lunch, I wasn't hungry. I wonder why?!")
        .save();

    Tokenize tokenize = new Tokenize();
    Processor p = tokenize.createComponent(null, new CoreNLPSettings());

    p.process(testItem);

    assertEquals(3, content.getAnnotations().getByType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).count());
    assertEquals(22, content.getAnnotations().getByType(AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN).count());
  }
}
