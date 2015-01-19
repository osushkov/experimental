package com.experimental.documentmodel.thirdparty;

import com.experimental.documentmodel.DocumentNameGenerator;
import com.experimental.documentmodel.SentenceProcessor;

/**
 * Created by sushkov on 14/01/15.
 */
public interface ThirdPartyDocumentParserFactory {

  boolean isValidDocumentFile(String filePath);

  ThirdPartyDocumentParser create(DocumentNameGenerator documentNameGenerator,
                                  SentenceProcessor sentenceProcessor);

}
