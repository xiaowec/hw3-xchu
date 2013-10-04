package edu.cmu.deiis.annotators;

import java.awt.Component;
import java.util.ArrayList;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.deiis.types.Answer;
import edu.cmu.deiis.types.AnswerScore;
import edu.cmu.deiis.types.Question;
import edu.cmu.deiis.types.Token;

/**
 * TokenOverlapScoreAnnotator will give each answer a score based on 
 * token overlap calculation.
 * @author cxw
 *
 */
public class TokenOverlapScoreAnnotator extends JCasAnnotator_ImplBase {
  
  //These parameters must be set in the analysis engine descriptor
  private static String COMPONENT_NAME = "componetId";
  
  private String componentId;
  private double confidence;
  
  private int totalnum; /* total number of tokens in each answer */
  private int matchnum; /* number of questions token occur in each answer */
  
  /**
   * @see org.apache.uima.analysis_component.AnalysisComponent_ImplBase#initialize(
   * org.apache.uima.UimaContext)
   */
  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException
  {
    super.initialize(aContext);
    
    componentId = (String) aContext.getConfigParameterValue(COMPONENT_NAME);
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
    
    /* get question answer and token index */
    FSIndex qIndex = aJCas.getAnnotationIndex(Question.type);
    FSIndex aIndex = aJCas.getAnnotationIndex(Answer.type);
    FSIndex index = aJCas.getAnnotationIndex(Token.type);
    
    FSIterator<Question> qIterator = qIndex.iterator();
    FSIterator<Answer> aIterator = aIndex.iterator();
    FSIterator<Token> tIterator = index.iterator();
    
    /* get features from question */
    Question q = qIterator.next();
    int begin = q.getBegin();
    int end = q.getEnd();
    
    ArrayList<String> qArray = new ArrayList<String>();
    while(tIterator.hasNext())
    {
      /* get tokens for each question */
      Token token = tIterator.next();
      if (token.getBegin() >= begin && token.getEnd() <= end)
      {
        qArray.add(token.getCoveredText());
      }
    }
    
    /* get answer from answer index */
    while(aIterator.hasNext())
    {
      tIterator = index.iterator();
      Answer a = aIterator.next();
      
      matchnum = 0;
      totalnum = 0;
      
      begin = a.getBegin();
      end = a.getEnd();
      
      while(tIterator.hasNext())
      {
        Token token = tIterator.next();
        if (token.getBegin() >= begin && token.getEnd() <= end)
        {
          String tokenString = token.getCoveredText();
          if (qArray.contains(tokenString)) {
            matchnum++;
          }
          totalnum++;
        }
        
       }
      
      /* set answerscore features */
      AnswerScore score = new AnswerScore(aJCas,a.getBegin(), a.getEnd());
      score.setAnswer(a);
      score.setScore((double)matchnum/totalnum);
      score.setCasProcessorId(componentId);
      score.setConfidence(confidence);
      score.addToIndexes();
    }
  }

}