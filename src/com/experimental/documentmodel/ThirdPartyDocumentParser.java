package com.experimental.documentmodel;

import java.io.IOException;

/**
 * Created by sushkov on 14/01/15.
 */
public interface ThirdPartyDocumentParser {

  public void parseThirdPartyDocument(String documentFilePath) throws IOException;

}
