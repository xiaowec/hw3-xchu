package edu.cmu.deiis.annotators;

import java.util.ArrayList;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.deiis.types.Answer;
import edu.cmu.deiis.types.NGram;
import edu.cmu.deiis.types.Question;
import edu.cmu.deiis.types.Token;

/**
 * NGramAnnotator will annotate n-grams(1,2,3-grams) based on tokens
 * @author cxw
 *
 */

public class NGramAnnotator extends JCasAnnotator_ImplBase
{
  
  // These parameters must be set in the analysis engine descriptor
  private static String N_PARAMETER_NAME = "n";
  private static String COMPONENT_NAME = "componetId";
  private static String TYPE_NAME = "typeName";
  
  private Integer n;
  private String componentId, typeName;
  private double confidence;
  
  /**
   * @see org.apache.uima.analysis_component.AnalysisComponent_ImplBase#initialize(
   * org.apache.uima.UimaContext)
   */
  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException
  {
    super.initialize(aContext);
    
    /* get parameters value from Uima context */
    n = (Integer) aContext.getConfigParameterValue(N_PARAMETER_NAME);
    componentId = (String) aContext.getConfigParameterValue(COMPONENT_NAME);
    typeName = (String)aContext.getConfigParameterValue(TYPE_NAME);
    
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
  public void process(JCas jcas) throws AnalysisEngineProcessException {
    
    /* get question answer and token index */
    FSIndex qIndex = jcas.getAnnotationIndex(Question.type);
    FSIndex aIndex = jcas.getAnnotationIndex(Answer.type);
    FSIndex index = jcas.getAnnotationIndex(Token.type);
    
    FSIterator<Question> qIterator = qIndex.iterator();
    FSIterator<Answer> aIterator = aIndex.iterator();
    
    /* get each question and features from question index */
    for (int i=0; i < qIndex.size(); i++)
    {
      Question q = qIterator.next();
      int begin = q.getBegin();
      int end = q.getEnd();
      
      ArrayList<Token> array = new ArrayList<Token>();
      FSIterator<Token> tIterator = index.iterator();
      
      /* extract tokens for each question */
      while(tIterator.hasNext())
      {
        Token token = tIterator.next();
        /* Judge whether the token is in question */
        if (token.getBegin() >= begin && token.getEnd() <= end)
        {
          array.add(token);
        }
      }
      
      /* extract ad-hoc ngrams from tokens */
      int lastNGramStart = array.size() -n +1;
      for(int offset =0 ; offset < lastNGramStart ; offset++)
      {
        FSArray elementsArray = new FSArray(jcas, n);
        for (int j=0 ; j<n ; j++)
        {
          elementsArray.set(j, array.get(offset+j));
        }
        NGram ng = new NGram(jcas , array.get(offset).getBegin(), array.get(offset+n-1).getEnd());
        ng.setElementType(typeName);
        ng.setElements(elementsArray);
        ng.setCasProcessorId(componentId);
        ng.setConfidence(confidence);
        ng.addToIndexes();
      }
      
    }
    
    /* get each answer and features from answer index */    
    for (int i=0; i < aIndex.size(); i++)
    {
      Answer a = aIterator.next();
      int begin = a.getBegin();
      int end = a.getEnd();
      
      ArrayList<Token> array = new ArrayList<Token>();
      FSIterator<Token> tIterator = index.iterator();
      
      /* extract tokens for each answer */
      while(tIterator.hasNext())
      {
        Token token = tIterator.next();
        /* Judge whether the token is in answer */
        if (token.getBegin() >= begin && token.getEnd() <= end)
        {
          array.add(token);
        }
      }
      
      /* extract ad-hoc ngrams from tokens */
      int lastNGramStart = array.size() -n +1;
      for(int offset =0 ; offset < lastNGramStart ; offset++)
      {
        FSArray elementsArray = new FSArray(jcas, n);
        for (int j=0 ; j<n ; j++)
        {
          elementsArray.set(j, array.get(offset+j));
        }
        NGram ng = new NGram(jcas , array.get(offset).getBegin(), array.get(offset+n-1).getEnd());
        ng.setElementType(typeName);
        ng.setElements(elementsArray);
        ng.setCasProcessorId(componentId);
        ng.setConfidence(confidence);
        ng.addToIndexes();
      }
      
    }
  }
  
}
