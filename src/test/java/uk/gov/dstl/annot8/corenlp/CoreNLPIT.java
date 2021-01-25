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
import io.annot8.api.data.Item;
import io.annot8.api.settings.NoSettings;
import io.annot8.api.stores.AnnotationStore;
import io.annot8.common.data.bounds.SpanBounds;
import io.annot8.common.data.content.Text;
import io.annot8.conventions.AnnotationTypes;
import io.annot8.testing.testimpl.TestItem;
import io.annot8.testing.testimpl.content.TestStringContent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CoreNLPIT {
  @Test
  public void test(){
    TestItem item = annotateText("Barack Obama lives in America. Obama was the president.");

    AnnotationStore annotationStore = item.getContents(Text.class).findFirst().get().getAnnotations();
    assertTrue(annotationStore.getAll().count() > 0);
    assertTrue(annotationStore.getAll().anyMatch(a -> a.getType().startsWith(AnnotationTypes.ENTITY_PREFIX)));
    assertTrue(annotationStore.getAll().anyMatch(a -> a.getType().startsWith(AnnotationTypes.GRAMMAR_PREFIX)));
    assertTrue(item.getGroups().getAll().count() > 0);

    item.getContents(Text.class).forEach(CoreNLPIT::printContent);
    printGroups(item);
  }

  public static void printContent(Text content){
    System.out.println("Text: "+content.getData());

    if (content.getProperties().keys().count() > 0) {
      System.out.println("Properties:");
      content.getProperties().getAll().forEach((k, v) -> System.out.println("\t" + k + ": " + v));
    }

    if(content.getAnnotations().getAll().count() > 0) {
      System.out.println("Entities: ");
      content.getAnnotations().getByBounds(SpanBounds.class).forEach(a -> {
        System.out.println("\tText: " + content.getText(a).get());
        System.out.println("\tType: " + a.getType());
        if (a.getProperties().keys().count() > 0) {
          System.out.println("\tProperties:");
          a.getProperties().getAll().forEach((k, v) -> System.out.println("\t\t" + k + ": " + v));
        }
        System.out.println();
      });
    }

    System.out.println();
  }

  public static void printGroups(Item item){
    if(item.getGroups().getAll().count() > 0) {
      System.out.println("Groups: ");

      item.getGroups().getAll().forEach(g -> {
        System.out.println("\tType: " + g.getType());
        if (g.getProperties().keys().count() > 0) {
          System.out.println("\tProperties:");
          g.getProperties().getAll().forEach((k, v) -> System.out.println("\t\t" + k + ": " + v));
        }
        System.out.println("\tMembers: ");
        g.getAnnotations().forEach((k, a) -> {
          System.out.println("\t\t"+k+": ");
          a.forEach(an -> {
            System.out.println("\t\t\t"+((Text)item.getContent(an.getContentId()).get()).getText(an).get());
          });
        });
        System.out.println();
      });
    }

    System.out.println();
  }

  public static TestItem annotateText(String text){
    TestItem testItem = new TestItem();
    testItem.createContent(TestStringContent.class)
        .withData(text)
        .save();

    Tokenize tokenize = new Tokenize();
    POS pos = new POS();
    Lemma lemma = new Lemma();
    NER ner = new NER();
    Coreference coref = new Coreference();
    Relation relation = new Relation();
    OpenIE openIE = new OpenIE();

    Processor pTokenize = tokenize.createComponent(null, new CoreNLPSettings());
    Processor pPos = pos.createComponent(null, new CoreNLPSettings());
    Processor pLemma = lemma.createComponent(null, NoSettings.getInstance());
    Processor pNer = ner.createComponent(null, new NER.Settings());
    Processor pCoref = coref.createComponent(null, new NER.Settings());
    Processor pRelation = relation.createComponent(null, new CoreNLPSettings());
    Processor pOpenIE = openIE.createComponent(null, new CoreNLPSettings());

    ProcessorResponse prTokenize = pTokenize.process(testItem);
    if(prTokenize.hasExceptions())
      prTokenize.getExceptions().forEach(Exception::printStackTrace);
    assertEquals(ProcessorResponse.Status.OK, prTokenize.getStatus());

    ProcessorResponse prPos = pPos.process(testItem);
    if(prPos.hasExceptions())
      prPos.getExceptions().forEach(Exception::printStackTrace);
    assertEquals(ProcessorResponse.Status.OK, prPos.getStatus());

    ProcessorResponse prLemma = pLemma.process(testItem);
    if(prLemma.hasExceptions())
      prLemma.getExceptions().forEach(Exception::printStackTrace);
    assertEquals(ProcessorResponse.Status.OK, prLemma.getStatus());

    ProcessorResponse prNer = pNer.process(testItem);
    if(prNer.hasExceptions())
      prNer.getExceptions().forEach(Exception::printStackTrace);
    assertEquals(ProcessorResponse.Status.OK, prNer.getStatus());

    ProcessorResponse prCoref = pCoref.process(testItem);
    if(prCoref.hasExceptions())
      prCoref.getExceptions().forEach(Exception::printStackTrace);
    assertEquals(ProcessorResponse.Status.OK, prCoref.getStatus());

    ProcessorResponse prRelation = pRelation.process(testItem);
    if(prRelation.hasExceptions())
      prRelation.getExceptions().forEach(Exception::printStackTrace);
    assertEquals(ProcessorResponse.Status.OK, prRelation.getStatus());

    ProcessorResponse prOpenIE = pOpenIE.process(testItem);
    if(prOpenIE.hasExceptions())
      prOpenIE.getExceptions().forEach(Exception::printStackTrace);
    assertEquals(ProcessorResponse.Status.OK, prOpenIE.getStatus());

    return testItem;
  }
}
