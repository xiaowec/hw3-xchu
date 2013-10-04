package edu.cmu.deiis.annotators;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;

import edu.cmu.deiis.types.Answer;
import edu.cmu.deiis.types.Question;
import edu.cmu.deiis.types.Token;

/**
 * TokenAnnotator will annotate tokens for each document
 * @author cxw
 *
 */

public class TokenAnnotator extends JCasAnnotator_ImplBase {
  
  //These parameters must be set in the analysis engine descriptor
  private static String TOKEN_PATTERN_NAME = "token_pattern";
  private static String PROCESSID_NAME = "componetId";
  
  private static double confidence;
  private static String componentId;
  
  private Pattern tokenPattern;
  
  /**
   * (non-Javadoc)
   * 
   * @see 
   * org.apache.uima.analysis_component.AnalysisComponent_ImplBase#initialize(
   * org.apache.uima.UimaContext)
   */
  @Override
  public void initialize(UimaContext aContext)
  {
    /* get parameters value from Uima context */
    tokenPattern = Pattern.compile((String)aContext.getConfigParameterValue(TOKEN_PATTERN_NAME));
    componentId = (String)aContext.getConfigParameterValue(PROCESSID_NAME);
    
    /* set confidence */
    confidence = 1.0;
  }

  /**
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.analysis_component.JCasAnnotator_ImplBase#process(org.apache.uima.jcas.JCas
   */
  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    
    /* get question and answer index */
    FSIndex question_index = aJCas.getAnnotationIndex(Question.type);
    FSIndex answer_index = aJCas.getAnnotationIndex(Answer.type);
    
    /* Annotate tokens in question */
    FSIterator<Question> qiterator = question_index.iterator();
    while(qiterator.hasNext())
    {
      /* get features for each question */
      Question q = qiterator.next();
      int begin = q.getBegin();
      String text = q.getCoveredText();
      
      Matcher match = tokenPattern.matcher(text);
      while (match.find()) {
        Token token = new Token(aJCas, begin+match.start(), begin+match.end());
        token.setCasProcessorId(componentId);
        token.setConfidence(confidence);
        token.addToIndexes();
      }
    }
    
    /* Annotate tokens in answer */
    FSIterator<Answer> aiterator = answer_index.iterator();
    while(aiterator.hasNext())
    {
      /* get features for each answer */
      Answer a = aiterator.next();
      int begin = a.getBegin();
      int end = a.getEnd();
      String text = a.getCoveredText();
      
      Matcher matcher = tokenPattern.matcher(text);
      while(matcher.find()){
        Token token = new Token(aJCas, begin+matcher.start(), begin+matcher.end());
        token.setCasProcessorId(componentId);
        token.setConfidence(confidence);
        token.addToIndexes();
        
      }
    }
  }

}