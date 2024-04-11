/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_masscalibration.standardslist;

import com.opencsv.exceptions.CsvException;
import io.github.mzmine.util.CSVParsingUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


/**
 * UniversalCalibrantsListExtractor for csv files expects columns at fixed positions for storing
 * needed data first column is mz second column is optional name first row (column headers) is
 * skipped
 */
public class UniversalCalibrantsListCsvExtractor implements StandardsListExtractor {

  protected static final int mzRatioColumn = 0;
  protected static final int nameColumn = 1;

  protected Logger logger = Logger.getLogger(this.getClass().getName());

  protected String filename;

  protected ArrayList<StandardsListItem> extractedData;

  /**
   * Creates the extractor
   *
   * @param filename csv filename
   * @throws IOException exception thrown when issues opening given file occur
   */
  public UniversalCalibrantsListCsvExtractor(String filename) throws IOException {
    this.filename = filename;
  }

  /**
   * Creates the extractor
   *
   * @param filename    csv filename
   * @param inputStream input stream to use.
   * @throws IOException exception thrown when issues opening given file occur
   */
  public UniversalCalibrantsListCsvExtractor(String filename, InputStream inputStream)
      throws IOException {
    this.filename = filename;
  }

  /**
   * Extracts standards list caching underlying list data
   *
   * @return new standards list object
   * @throws IOException thrown when issues extracting from the file occur
   */
  public StandardsList extractStandardsList() throws IOException, CsvException {
    logger.fine("Extracting universal calibrants list " + filename);

    if (this.extractedData != null) {
      logger.fine("Using cached list");
      return new StandardsList(this.extractedData);
    }
    this.extractedData = new ArrayList<>();

    List<String[]> lines = CSVParsingUtils.readData(new File(filename), ";");
    for (String[] lineValues : lines) {
      try {
        String mzRatioString = lineValues[mzRatioColumn];
        String name = nameColumn < lineValues.length ? lineValues[nameColumn] : null;
        double mzRatio = Double.valueOf(mzRatioString);
        StandardsListItem calibrant = new StandardsListItem(mzRatio);
        if (name != null && name.trim().isEmpty() == false) {
          calibrant.setName(name);
        }
        extractedData.add(calibrant);
      } catch (Exception e) {
        logger.fine(STR."Exception occurred when reading row index \{lines.indexOf(lineValues)}");
        logger.fine(e.toString());
      }
    }

    logger.info(
        STR."Extracted \{extractedData.size()} standard molecules from \{lines.size()} rows");
    if (extractedData.size() < lines.size()) {
      logger.warning(STR."Skipped \{lines.size()
          - extractedData.size()} rows when reading standards list in csv file \{filename}");
    }

    return new StandardsList(extractedData);
  }
}
