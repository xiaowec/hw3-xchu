/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package edu.cmu.deiis.cpe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.SAXException;

import edu.cmu.deiis.types.Answer;
import edu.cmu.deiis.types.AnswerScore;
import edu.cmu.deiis.types.Question;

/**
 * A simple CAS consumer that writes the CAS to XMI format.
 * <p>
 * This CAS Consumer takes one parameter:
 * <ul>
 * <li><code>OutputDirectory</code> - path to directory into which output files will be written</li>
 * </ul>
 */
public class XmiWriterCasConsumer extends CasConsumer_ImplBase {
  /**
   * Name of configuration parameter that must be set to the path of a directory into which the
   * output files will be written.
   */
  public static final String PARAM_OUTPUTDIR = "OutputDirectory";

  private File mOutputDir;

  private int mDocNum;
  
  private int predictnum; //number of correct prediction for a single document
  private int truenum; //number of top n sentences for a single document
  private int correctnum; //number of correctly predicted answers
  private int totalnum; //total number of top n sentences in training dataset

  public void initialize() throws ResourceInitializationException {
    mDocNum = 0;
    mOutputDir = new File((String) getConfigParameterValue(PARAM_OUTPUTDIR));
    if (!mOutputDir.exists()) {
      mOutputDir.mkdirs();
    }
  }

  /**
   * Processes the CAS which was populated by the TextAnalysisEngines. <br>
   * In this case, the CAS is converted to XMI and written into the output file .
   * 
   * @param aCAS
   *          a CAS which has been populated by the TAEs
   * 
   * @throws ResourceProcessException
   *           if there is an error in processing the Resource
   * 
   * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#processCas(org.apache.uima.cas.CAS)
   */
  public void processCas(CAS aCAS) throws ResourceProcessException {
    String modelFileName = null;

    JCas jcas;
    try {
      jcas = aCAS.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }
    
    
    FSIndex qIndex = jcas.getAnnotationIndex(Question.type);
    FSIndex aIndex = jcas.getAnnotationIndex(AnswerScore.type);
    
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
    System.out.println("\n");
    

    // retreive the filename of the input file from the CAS
    FSIterator it = jcas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
    File outFile = null;
    if (it.hasNext()) {
      SourceDocumentInformation fileLoc = (SourceDocumentInformation) it.next();
      File inFile;
      try {
        inFile = new File(new URL(fileLoc.getUri()).getPath());
        String outFileName = inFile.getName();
        if (fileLoc.getOffsetInSource() > 0) {
          outFileName += ("_" + fileLoc.getOffsetInSource());
        }
        outFileName += ".xmi";
        outFile = new File(mOutputDir, outFileName);
        modelFileName = mOutputDir.getAbsolutePath() + "/" + inFile.getName() + ".ecore";
      } catch (MalformedURLException e1) {
        // invalid URL, use default processing below
      }
    }
    if (outFile == null) {
      outFile = new File(mOutputDir, "doc" + mDocNum++ + ".xmi");     
    }
    // serialize XCAS and write to output file
    try {
      writeXmi(jcas.getCas(), outFile, modelFileName);
    } catch (IOException e) {
      throw new ResourceProcessException(e);
    } catch (SAXException e) {
      throw new ResourceProcessException(e);
    }
  }

  /**
   * Serialize a CAS to a file in XMI format
   * 
   * @param aCas
   *          CAS to serialize
   * @param name
   *          output file
   * @throws SAXException
   * @throws Exception
   * 
   * @throws ResourceProcessException
   */
  private void writeXmi(CAS aCas, File name, String modelFileName) throws IOException, SAXException {
    FileOutputStream out = null;

    try {
      // write XMI
      out = new FileOutputStream(name);
      XmiCasSerializer ser = new XmiCasSerializer(aCas.getTypeSystem());
      XMLSerializer xmlSer = new XMLSerializer(out, false);
      ser.serialize(aCas, xmlSer.getContentHandler());
    } finally {
      if (out != null) {
        out.close();
      }
    }
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
