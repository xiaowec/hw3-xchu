package edu.cmu.deiis.annotators;

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
import edu.cmu.deiis.types.NGram;
import edu.cmu.deiis.types.Question;

/**
 * NGramOverlapScoreAnnotator will give each answer a score based on
 * NGram(1,2,3-grams) Overlap calculation
 * @author cxw
 *
 */
public class NGramOverlapScoreAnnotator extends JCasAnnotator_ImplBase {
  
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
    FSIndex index = aJCas.getAnnotationIndex(NGram.type);
    
    FSIterator<Question> qIterator = qIndex.iterator();
    FSIterator<Answer> aIterator = aIndex.iterator();
    FSIterator<NGram> nIterator = index.iterator();
    
    /* get features from question */
    Question q = qIterator.next();
    int begin = q.getBegin();
    int end = q.getEnd();
    
    ArrayList<String> qArray = new ArrayList<String>();
    while(nIterator.hasNext())
    {
      /* get ngrams for each question */
      NGram ngram = nIterator.next();
      if (ngram.getBegin() >= begin && ngram.getEnd() <= end)
      {
        qArray.add(ngram.getCoveredText());
      }
    }
    
    /* get answer from answer index */
    while(aIterator.hasNext())
    {
      nIterator = index.iterator();
      Answer a = aIterator.next();
      
      matchnum = 0;
      totalnum = 0;
      
      begin = a.getBegin();
      end = a.getEnd();
      
      while(nIterator.hasNext())
      {
        NGram ngram = nIterator.next();
        if (ngram.getBegin() >= begin && ngram.getEnd() <= end)
        {
          String tokenString = ngram.getCoveredText();
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

  public NGramOverlapScoreAnnotator() {
  }

}