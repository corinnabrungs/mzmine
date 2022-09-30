/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.featdet_manual;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureConvertors;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.bouncycastle.math.raw.Mod;

class ManualPickerTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private int processedFiles, totalFiles;

  private final MZmineProject project;
  private final ModularFeatureList featureList;
  private FeatureListRow featureListRow;
  private RawDataFile dataFiles[];
  private Range<Double> mzRange;
  private Range<Float> rtRange;
  private final ParameterSet parameterSet;

  ManualPickerTask(MZmineProject project, FeatureListRow featureListRow, RawDataFile dataFiles[],
      ParameterSet parameters, FeatureList featureList) {
    super(null, Instant.now()); // we get passed a flist, so it should contain a storage

    this.project = project;
    this.featureListRow = featureListRow;
    this.dataFiles = dataFiles;
    this.featureList = (ModularFeatureList) featureList;

    // TODO: FloatRangeParameter
    rtRange = RangeUtils.toFloatRange(
        parameters.getParameter(ManualPickerParameters.retentionTimeRange).getValue());
    mzRange = parameters.getParameter(ManualPickerParameters.mzRange).getValue();
    this.parameterSet = parameters;
    totalFiles = dataFiles.length;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalFiles == 0) {
      return 0;
    }
    return (double) processedFiles / totalFiles;
  }

  @Override
  public String getTaskDescription() {
    return "Manually picking features from " + Arrays.toString(dataFiles);
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    logger.finest("Starting manual feature picker, RT: " + rtRange + ", m/z: " + mzRange);

    final ScanSelection selection = new ScanSelection(rtRange, 1);
    for (RawDataFile file : dataFiles) {
      final IonTimeSeries<?> series = IonTimeSeriesUtils.extractIonTimeSeries(file, selection,
          mzRange, featureList.getMemoryMapStorage());

      final Feature feature = featureListRow.getFeature(file);
      ModularFeature f = (ModularFeature)feature;
      f.set(FeatureDataType.class, series);
      FeatureDataUtils.recalculateIonSeriesDependingTypes(f);
      processedFiles++;
    }

    // Notify the GUI that feature list contents have changed
    if (featureList != null) {
      // Check if the feature list row has been added to the feature list,
      // and
      // if it has not, add it
      List<FeatureListRow> rows = new ArrayList<>(featureList.getRows());
      if (!rows.contains(featureListRow)) {
        featureList.addRow(featureListRow);
      }
    }

    featureList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(ManualFeaturePickerModule.class, parameterSet,
            getModuleCallDate()));

    logger.finest("Finished manual feature picker, " + processedFiles + " files processed");

    setStatus(TaskStatus.FINISHED);

  }

}
