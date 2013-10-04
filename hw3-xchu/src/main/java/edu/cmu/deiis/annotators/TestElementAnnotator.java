package edu.cmu.deiis.annotators;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import edu.cmu.deiis.types.Answer;
import edu.cmu.deiis.types.Question;

/**
 * TestElementAnnotator will annotate both the question and answers for a document
 * @author cxw
 *
 */
public class TestElementAnnotator extends JCasAnnotator_ImplBase {
  
  //These parameters must be set in the analysis engine descriptor
  private static String QUESTION_PATTERN_NAME = "question_pattern";
  private static String ANSWER_PATTERN_NAME = "answer_pattern";
  private static String PROCESSID_NAME = "componetId";
  
  private static double confidence;
  private static String componentId;
  
  private Pattern questionPattern;
  private Pattern answerPattern;
  
  /**
   * @see org.apache.uima.analysis_component.AnalysisComponent_ImplBase#initialize(
   * org.apache.uima.UimaContext)
   */
  @Override
  public void initialize(UimaContext aContext)
  {
    /* get parameters value from Uima context and compile patterns */
    questionPattern = Pattern.compile((String)aContext.getConfigParameterValue(QUESTION_PATTERN_NAME));
    answerPattern = Pattern.compile((String)aContext.getConfigParameterValue(ANSWER_PATTERN_NAME));
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
    
    /* read training document from input files */
    String docs = aJCas.getDocumentText();
    
    /* Annotate questions */
    Matcher match = questionPattern.matcher(docs);
    while (match.find())
    {
      try {
        Question q = new Question(aJCas, match.start(1), match.end(1));
        q.setCasProcessorId(componentId);
        q.setConfidence(confidence);
        q.addToIndexes();
      } catch (Exception e) {
        throw new AnalysisEngineProcessException(e);
      }
    }
    
    /* Annotate answers */
    match = answerPattern.matcher(docs);
    while (match.find()) 
    {
      try {
        Answer a = new Answer(aJCas, match.start(2), match.end(2));
        a.setCasProcessorId(componentId);
        a.setConfidence(confidence);
        
        /* Judge whether the answer is correct or not */
        if (Integer.parseInt(match.group(1)) == 0) 
        {
          a.setIsCorrect(false);
        }
        else 
        {
          a.setIsCorrect(true);
        }
        a.addToIndexes();
      } catch (Exception e) {
        throw new AnalysisEngineProcessException(e);
      } 
    }

  }

}