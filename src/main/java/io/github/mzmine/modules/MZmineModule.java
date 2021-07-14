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

package io.github.mzmine.modules;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.mzmine.parameters.ParameterSet;

/**
 * This interface represents any component of MZmine that has a ParameterSet, and therefore can
 * store its settings.
 */
public interface MZmineModule {

  /**
   * Returns module name
   * 
   * @return Module name
   */
  @NotNull
  public String getName();

  /**
   * Returns module's parameter class. If the module has no parameters, it can return null. The
   * returned class must provide a public constructor without parameters.
   */
  public @Nullable Class<? extends ParameterSet> getParameterSetClass();

}
