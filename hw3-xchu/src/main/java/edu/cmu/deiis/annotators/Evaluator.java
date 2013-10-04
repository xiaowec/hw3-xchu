package edu.cmu.deiis.annotators;

import java.awt.List;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.deiis.types.Answer;
import edu.cmu.deiis.types.AnswerScore;
import edu.cmu.deiis.types.Question;

/**
 * Evaluator will evaluate the precision score for pipeline 
 * @author cxw
 *
 */
public class Evaluator extends JCasAnnotator_ImplBase {
  
  private int predictnum; //number of correct prediction for a single document
  private int truenum; //number of top n sentences for a single document
  private int correctnum; //number of correctly predicted answers
  private int totalnum; //total number of top n sentences in training dataset
  
  /**
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.analysis_component.JCasAnnotator_ImplBase#process(org.apache.uima.jcas.JCas
   */
  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    
    /* get question and answerscore index */
    FSIndex qIndex = aJCas.getAnnotationIndex(Question.type);
    FSIndex aIndex = aJCas.getAnnotationIndex(AnswerScore.type);
    
    /* Print question */
    Iterator<Question> qIterator = qIndex.iterator();
    if (qIterator.hasNext()) {
      String qString = qIterator.next().getCoveredText();
      System.out.println("Question: "+qString);
    }
    
    Iterator<AnswerScore> aIterator = aIndex.iterator();
    ArrayList<AnswerScore> scoreList = new ArrayList<AnswerScore>();
    
    predictnum = 0;
    truenum = 0;
    
    /*get the number of correct sentences in documents */
    while(aIterator.hasNext())
    {
      AnswerScore score = aIterator.next();
      if(score.getAnswer().getIsCorrect())
      {
        truenum++;
        totalnum++;
      }
      scoreList.add(score);
    }
    Collections.sort(scoreList, comparator);
    
    /* get the number of correctly predicted answers */
    for (int i=0 ; i< scoreList.size(); i++)
    {
      AnswerScore aScore = scoreList.get(i);
      Answer a = aScore.getAnswer();
      char sign;
      if(a.getIsCorrect())
      {
        if (i < truenum) {
          predictnum++;
          correctnum++;
        }
        sign = '+';
      }
      else {
        sign = '-';
      }
      System.out.println(sign+" "+aScore.getScore()+" "+aScore.getAnswer().getCoveredText());
     }
    double precision = (double)predictnum / truenum;
    System.out.println("Precision at "+truenum+": "+precision);
    
    }
  
  /**
   * @see org.apache.uima.analysis_component.AnalysisComponent_ImplBase#destroy()
   */
  public void destroy()
  {
    System.out.println("Average Precision: "+(double)correctnum/totalnum);
  }
  
  public static Comparator<AnswerScore> comparator = new Comparator<AnswerScore>() {
    
    public int compare(AnswerScore a, AnswerScore b)
    {
      if (a.getScore() < b.getScore()) {
        return 1;
      }
      if (a.getScore() > b.getScore()) {
        return -1;
      }
      return 0;
    }
  };
    
}

