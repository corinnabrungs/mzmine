/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.types.fx.DataTypeCellFactory;
import io.github.mzmine.datamodel.features.types.fx.DataTypeCellValueFactory;
import io.github.mzmine.datamodel.features.types.fx.EditComboCellFactory;
import io.github.mzmine.datamodel.features.types.fx.EditableDataTypeCellFactory;
import io.github.mzmine.datamodel.features.types.fx.ModularDataTypeCellValueFactory;
import io.github.mzmine.datamodel.features.types.modifiers.AddElementDialog;
import io.github.mzmine.datamodel.features.types.modifiers.EditableColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.StringParser;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ListProperty;
import javafx.beans.property.Property;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class of data types: Provides formatters. Should be added to one {@link ModularDataModel}
 *
 * @param <T>
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public abstract class DataType<T extends Property<?>> {

  protected Logger logger = Logger.getLogger(this.getClass().getName());

  public DataType() {
  }

  /**
   * A formatted string representation of the value
   *
   * @return the formatted representation of the value (or an empty String)
   */
  @Nonnull
  public String getFormattedString(T property) {
    return property == null ? "" : getFormattedString(property.getValue());
  }

  /**
   * A formatted string representation of the value
   *
   * @return the formatted representation of the value (or an empty String)
   */
  @Nonnull
  public String getFormattedString(@Nullable Object value) {
    if (value != null) {
      return value.toString();
    } else {
      return "";
    }
  }

  /**
   * The header string (name) of this data type
   *
   * @return
   */
  @Nonnull
  public abstract String getHeaderString();

  /**
   * Creates a TreeTableColumn or null if the value is not represented in a column. A {@link
   * SubColumnsFactory} DataType can also add multiple sub columns to the main column generated by
   * this class.
   *
   * @param raw               null if this is a FeatureListRow column. For Feature columns: the raw
   *                          data file specifies the feature.
   * @param modularParentType if this type is a sub type of modularParentType (or null): Changes the
   *                          CellFactory for editable cells and the CellValueFactory
   * @return the TreeTableColumn or null if this DataType.value is not represented in a column
   */
  @Nullable
  public TreeTableColumn<ModularFeatureListRow, Object> createColumn(
      final @Nullable RawDataFile raw,
      final @Nullable ModularType modularParentType) {
    if (this instanceof NullColumnType) {
      return null;
    }
    // create column
    TreeTableColumn<ModularFeatureListRow, Object> col = new TreeTableColumn<>(getHeaderString());
    col.setUserData(this);

    if (this instanceof SubColumnsFactory) {
      col.setSortable(false);
      // add sub columns
      List<TreeTableColumn<ModularFeatureListRow, ?>> children =
          ((SubColumnsFactory) this).createSubColumns(raw);
      col.getColumns().addAll(children);
      return col;
    } else {
      col.setSortable(true);

      // is sub column of modularParentType?
      // define observable
      if (modularParentType != null) {
        col.setCellValueFactory(new ModularDataTypeCellValueFactory(raw, modularParentType, this));
      } else {
        col.setCellValueFactory(new DataTypeCellValueFactory(raw, this));
      }
      // value representation
      if (this instanceof EditableColumnType) {
        col.setCellFactory(getEditableCellFactory(col, raw, modularParentType));
        col.setEditable(true);
        col.setOnEditCommit(event -> {
          Object data = event.getNewValue();
          if (data != null) {
            ModularDataModel model;
            if (raw == null) {
              model = event.getRowValue().getValue();
            } else {
              model = event.getRowValue().getValue().getFilesFeatures().get(raw);
            }
            // set value
            if (modularParentType != null) {
              model = model.get(modularParentType);
            }
            if (this instanceof ListDataType) {
              if (this instanceof AddElementDialog && data instanceof String &&
                  AddElementDialog.BUTTON_TEXT.equals(data)) {
                ((AddElementDialog) this).createNewElementDialog(model, this);
              } else {
                try {
                  ((ListProperty) model.get(this)).remove(data);
                  ((ListProperty) model.get(this)).add(0, data);
                } catch (Exception ex) {
                  logger.log(Level.SEVERE, "Cannot set value from table cell to data type: "
                      + this.getHeaderString());
                  logger.log(Level.SEVERE, ex.getMessage(), ex);
                }
              }
            } else {
              model.set(this, data);
            }
          }
        });
      } else {
        col.setCellFactory(new DataTypeCellFactory(raw, this));
      }
    }
    return col;
  }

  protected Callback<TreeTableColumn<ModularFeatureListRow, Object>,
      TreeTableCell<ModularFeatureListRow, Object>> getEditableCellFactory(
      TreeTableColumn<ModularFeatureListRow, Object> col,
      RawDataFile raw, ModularType modularParentType) {
    if (this instanceof ListDataType) {
      return new EditComboCellFactory(raw, this, modularParentType);
    } else if (this instanceof StringParser<?>) {
      return new EditableDataTypeCellFactory(raw, this);
    } else {
      throw new UnsupportedOperationException("Programming error: No edit CellFactory for "
          + "data type: " + this.getHeaderString() + " class " + this.getClass().toString());
    }
  }

  // TODO dirty hack to make this a "singleton"
  @Override
  public boolean equals(Object obj) {
    return obj != null && obj.getClass().equals(this.getClass());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  /**
   * Creating a property which is used in a {@link ModularDataModel}
   *
   * @return
   */
  public abstract T createProperty();


  /**
   * In case this DataType is added to a {@link ModularFeature}, these row bindings are added to the
   * {@link ModularFeatureList} to automatically calculate or visualize summary datatypes in a row
   *
   * @return
   */
  @Nonnull
  public List<RowBinding> createDefaultRowBindings() {
    return List.of();
  }

  @Nullable
  public Runnable getDoubleClickAction(@Nonnull ModularFeatureListRow row,
      @Nonnull List<RawDataFile> file) {
    return null;
  }

}