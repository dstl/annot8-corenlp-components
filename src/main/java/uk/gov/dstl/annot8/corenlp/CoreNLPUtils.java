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

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;
import io.annot8.api.annotations.Annotation;
import io.annot8.common.data.bounds.SpanBounds;
import io.annot8.common.data.content.Text;
import io.annot8.common.data.utils.SortUtils;
import io.annot8.conventions.AnnotationTypes;
import io.annot8.conventions.PropertyKeys;

import java.util.*;
import java.util.stream.Collectors;

public class CoreNLPUtils {

  public static final Map<String, String> CORENLP_TO_ANNOT8;
  public static final Map<String, String> ANNOT8_TO_CORENLP;

  public static final String UNDEFINED_ENTITY = "entity";

  static {
    CORENLP_TO_ANNOT8 = new HashMap<>();
    //Standard types - not included: MISC, SET
    CORENLP_TO_ANNOT8.put("PERSON", AnnotationTypes.ANNOTATION_TYPE_PERSON);
    CORENLP_TO_ANNOT8.put("LOCATION", AnnotationTypes.ANNOTATION_TYPE_LOCATION);
    CORENLP_TO_ANNOT8.put("ORGANIZATION", AnnotationTypes.ANNOTATION_TYPE_ORGANISATION);
    CORENLP_TO_ANNOT8.put("MONEY", AnnotationTypes.ANNOTATION_TYPE_MONEY);
    CORENLP_TO_ANNOT8.put("NUMBER", AnnotationTypes.ENTITY_PREFIX + "number");
    CORENLP_TO_ANNOT8.put("ORDINAL", AnnotationTypes.ENTITY_PREFIX + "ordinal");
    CORENLP_TO_ANNOT8.put("PERCENT", AnnotationTypes.ENTITY_PREFIX + "percent");
    CORENLP_TO_ANNOT8.put("DATE", AnnotationTypes.ANNOTATION_TYPE_TEMPORAL);
    CORENLP_TO_ANNOT8.put("TIME", AnnotationTypes.ANNOTATION_TYPE_TEMPORAL);
    CORENLP_TO_ANNOT8.put("DURATION", AnnotationTypes.ANNOTATION_TYPE_TEMPORAL);

    //Fine-grained types
    CORENLP_TO_ANNOT8.put("CITY", AnnotationTypes.ANNOTATION_TYPE_LOCATION);
    CORENLP_TO_ANNOT8.put("COUNTRY", AnnotationTypes.ANNOTATION_TYPE_LOCATION);
    CORENLP_TO_ANNOT8.put("STATE_OR_PROVINCE", AnnotationTypes.ANNOTATION_TYPE_LOCATION);
    CORENLP_TO_ANNOT8.put("EMAIL", AnnotationTypes.ANNOTATION_TYPE_EMAIL);
    CORENLP_TO_ANNOT8.put("URL", AnnotationTypes.ANNOTATION_TYPE_URL);
    CORENLP_TO_ANNOT8.put("NATIONALITY", AnnotationTypes.ANNOTATION_TYPE_NATIONALITY);
    CORENLP_TO_ANNOT8.put("RELIGION", AnnotationTypes.ENTITY_PREFIX + "religion");
    CORENLP_TO_ANNOT8.put("IDEOLOGY", AnnotationTypes.ENTITY_PREFIX + "ideology");
    CORENLP_TO_ANNOT8.put("TITLE", UNDEFINED_ENTITY);
    CORENLP_TO_ANNOT8.put("CAUSE_OF_DEATH", UNDEFINED_ENTITY);
    CORENLP_TO_ANNOT8.put("CRIMINAL_CHARGE", UNDEFINED_ENTITY);

    ANNOT8_TO_CORENLP = new HashMap<>();
    ANNOT8_TO_CORENLP.put(AnnotationTypes.ANNOTATION_TYPE_PERSON, "PERSON");
    ANNOT8_TO_CORENLP.put(AnnotationTypes.ANNOTATION_TYPE_LOCATION, "LOCATION");
    ANNOT8_TO_CORENLP.put(AnnotationTypes.ANNOTATION_TYPE_ORGANISATION, "ORGANIZATION");
    ANNOT8_TO_CORENLP.put(AnnotationTypes.ANNOTATION_TYPE_MONEY, "MONEY");
    ANNOT8_TO_CORENLP.put(AnnotationTypes.ENTITY_PREFIX + "number", "NUMBER");
    ANNOT8_TO_CORENLP.put(AnnotationTypes.ENTITY_PREFIX + "ordinal", "ORDINAL");
    ANNOT8_TO_CORENLP.put(AnnotationTypes.ENTITY_PREFIX + "percent", "PERCENT");
    ANNOT8_TO_CORENLP.put(AnnotationTypes.ANNOTATION_TYPE_EMAIL, "EMAIL");
    ANNOT8_TO_CORENLP.put(AnnotationTypes.ANNOTATION_TYPE_URL, "URL");
    ANNOT8_TO_CORENLP.put(AnnotationTypes.ANNOTATION_TYPE_NATIONALITY, "NATIONALITY");
    ANNOT8_TO_CORENLP.put(AnnotationTypes.ENTITY_PREFIX + "religion", "RELIGION");
    ANNOT8_TO_CORENLP.put(AnnotationTypes.ENTITY_PREFIX + "ideology", "IDEOLOGY");
    //TODO: How does temporal stuff map?

  }

  private CoreNLPUtils(){
    //Private constructor for utility class
  }

  public static edu.stanford.nlp.pipeline.Annotation createCoreNLPDocument(Text content){
    edu.stanford.nlp.pipeline.Annotation document = new edu.stanford.nlp.pipeline.Annotation(content.getData());

    List<Annotation> sentences = content.getAnnotations().getByBoundsAndType(SpanBounds.class, AnnotationTypes.ANNOTATION_TYPE_SENTENCE)
        .sorted(SortUtils.SORT_BY_SPANBOUNDS)
        .collect(Collectors.toList());

    List<Annotation> tokens = content.getAnnotations().getByBoundsAndType(SpanBounds.class, AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN)
        .sorted(SortUtils.SORT_BY_SPANBOUNDS)
        .collect(Collectors.toList());

    List<Annotation> entities = content.getAnnotations().getByBounds(SpanBounds.class)
        .filter(CoreNLPUtils::isCoreNLPType)
        .sorted(SortUtils.SORT_BY_SPANBOUNDS)
        .collect(Collectors.toList());


    //Create tokens - additional information will be added later
    List<CoreLabel> cTokens = new ArrayList<>();
    for (Annotation token : tokens) {
      Optional<SpanBounds> opt = token.getBounds(SpanBounds.class);
      if(opt.isEmpty())
        continue;
      SpanBounds tokenBounds = opt.get();

      CoreLabel cToken = new CoreLabel();

      cToken.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, tokenBounds.getBegin());
      cToken.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, tokenBounds.getEnd());

      content.getText(token).ifPresent(s -> {
        cToken.set(CoreAnnotations.TextAnnotation.class, s);
        cToken.set(CoreAnnotations.ValueAnnotation.class, s);
      });
      token.getProperties().get(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH, String.class).ifPresent(s -> cToken.set(CoreAnnotations.PartOfSpeechAnnotation.class, s));
      token.getProperties().get(PropertyKeys.PROPERTY_KEY_LEMMA, String.class).ifPresent(s -> cToken.set(CoreAnnotations.LemmaAnnotation.class, s));

      cToken.set(CoreAnnotations.NamedEntityTagAnnotation.class, "O");

      cTokens.add(cToken);
    }

    //Create mentions (entities), and add additional information to tokens
    List<CoreMap> cMentions = new ArrayList<>();
    for(int entityIndex = 0; entityIndex < entities.size(); entityIndex++){
      Annotation entity = entities.get(entityIndex);

      Optional<SpanBounds> opt = entity.getBounds(SpanBounds.class);
      if(opt.isEmpty())
        continue;
      SpanBounds entityBounds = opt.get();

      CoreMap cMention = new edu.stanford.nlp.pipeline.Annotation(content.getText(entity).orElse(""));

      cMention.set(CoreAnnotations.EntityMentionIndexAnnotation.class, entityIndex);

      cMention.set(CoreAnnotations.NamedEntityTagAnnotation.class, getCoreNLPType(entity));

      cMention.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, entityBounds.getBegin());
      cMention.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, entityBounds.getEnd());

      content.getText(entity).ifPresent(s -> {
        cMention.set(CoreAnnotations.TextAnnotation.class, s);
        cMention.set(CoreAnnotations.ValueAnnotation.class, s);
      });

      int firstToken = Integer.MAX_VALUE;
      int lastToken = Integer.MIN_VALUE;

      List<CoreLabel> cMentionTokens = new ArrayList<>();
      for(int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++){
        CoreLabel cToken = cTokens.get(tokenIndex);
        if(cToken.beginPosition() < entityBounds.getBegin() || cToken.endPosition() > entityBounds.getEnd())
          continue;

        if(tokenIndex < firstToken)
          firstToken = tokenIndex;

        if(tokenIndex >= lastToken)
          lastToken = tokenIndex + 1;

        cToken.set(CoreAnnotations.NamedEntityTagAnnotation.class, getCoreNLPType(entity));
        cToken.set(CoreAnnotations.NamedEntityTagProbsAnnotation.class,
            Map.of(getCoreNLPType(entity), entity.getProperties().get(PropertyKeys.PROPERTY_KEY_PROBABILITY, Double.class)
                .orElse(1.0d)));

        cMentionTokens.add(cToken);
      }

      cMention.set(CoreAnnotations.TokenBeginAnnotation.class, firstToken);
      cMention.set(CoreAnnotations.TokenEndAnnotation.class, lastToken);
      cMention.set(CoreAnnotations.TokensAnnotation.class, cMentionTokens);

      cMentions.add(cMention);
    }

    //Create sentences, and add additional information to tokens and mentions
    List<CoreMap> cSentences = new ArrayList<>();
    for(int sentenceIndex = 0; sentenceIndex < sentences.size(); sentenceIndex++){
      Annotation sentence = sentences.get(sentenceIndex);

      Optional<SpanBounds> opt = sentence.getBounds(SpanBounds.class);
      if(opt.isEmpty())
        continue;
      SpanBounds sentenceBounds = opt.get();

      CoreMap cSentence = new edu.stanford.nlp.pipeline.Annotation(content.getText(sentence).orElse(""));

      cSentence.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, sentenceBounds.getBegin());
      cSentence.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, sentenceBounds.getEnd());
      cSentence.set(CoreAnnotations.SentenceIndexAnnotation.class, sentenceIndex);

      //Get tokens
      int firstToken = Integer.MAX_VALUE;
      int lastToken = Integer.MIN_VALUE;
      List<CoreLabel> cSentenceTokens = new ArrayList<>();

      //Get tokens and add sentence index
      int sentenceToken = 1;
      for(int tokenIndex = 0; tokenIndex < cTokens.size(); tokenIndex++){
        CoreLabel cToken = cTokens.get(tokenIndex);
        if(cToken.beginPosition() < sentenceBounds.getBegin() || cToken.endPosition() > sentenceBounds.getEnd())
          continue;

        if(tokenIndex < firstToken)
          firstToken = tokenIndex;

        if(tokenIndex >= lastToken)
          lastToken = tokenIndex + 1;

        cToken.set(CoreAnnotations.SentenceIndexAnnotation.class, sentenceIndex);
        cToken.set(CoreAnnotations.IndexAnnotation.class, sentenceToken++);

        cSentenceTokens.add(cToken);
      }

      cSentence.set(CoreAnnotations.TokenBeginAnnotation.class, firstToken);
      cSentence.set(CoreAnnotations.TokenEndAnnotation.class, lastToken);
      cSentence.set(CoreAnnotations.TokensAnnotation.class, cSentenceTokens);

      //Get mentions and add sentence index
      List<CoreMap> cSentenceMentions = new ArrayList<>();
      for (CoreMap cMention : cMentions) {
        if (cMention.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class) < sentenceBounds.getBegin() || cMention.get(CoreAnnotations.CharacterOffsetEndAnnotation.class) > sentenceBounds.getEnd())
          continue;

        cMention.set(CoreAnnotations.SentenceIndexAnnotation.class, sentenceIndex);

        cSentenceMentions.add(cMention);
      }

      cSentence.set(CoreAnnotations.MentionsAnnotation.class, cSentenceMentions);

      cSentences.add(cSentence);
    }

    document.set(CoreAnnotations.TokensAnnotation.class, cTokens);
    document.set(CoreAnnotations.MentionsAnnotation.class, cMentions);
    document.set(CoreAnnotations.SentencesAnnotation.class, cSentences);

    return document;
  }

  public static String getCoreNLPType(Annotation entity){
    String type = entity.getType();
    Optional<String> subtype = entity.getProperties().get(PropertyKeys.PROPERTY_KEY_SUBTYPE, String.class);
    if(subtype.isPresent() && CORENLP_TO_ANNOT8.containsKey(subtype.get()))
      //If the subtype is a valid CoreNLP type that we support, then use that
      return subtype.get();

    return ANNOT8_TO_CORENLP.getOrDefault(type, "");
  }

  public static boolean isCoreNLPType(Annotation entity){
    if(ANNOT8_TO_CORENLP.containsKey(entity.getType()))
        return true;

    Optional<String> subtype = entity.getProperties().get(PropertyKeys.PROPERTY_KEY_SUBTYPE, String.class);
    return subtype.isPresent() && CORENLP_TO_ANNOT8.containsKey(subtype.get());
  }
}
